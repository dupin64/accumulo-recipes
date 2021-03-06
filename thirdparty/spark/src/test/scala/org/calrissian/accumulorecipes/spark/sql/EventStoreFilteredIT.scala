/*
 * Copyright (C) 2015 The Calrissian Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.calrissian.accumulorecipes.spark.sql

import java.util.Collections

import org.calrissian.accumulorecipes.test.AccumuloMiniClusterDriver
import org.apache.spark.sql.{AnalysisException, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.calrissian.accumulorecipes.eventstore.impl.AccumuloEventStore
import org.calrissian.accumulorecipes.spark.sql.util.TableUtil
import org.calrissian.accumulorecipes.test.AccumuloTestUtils
import org.calrissian.mango.domain.Attribute
import org.calrissian.mango.domain.event.EventBuilder
import org.joda.time.DateTime
import org.junit._

object EventStoreFilteredIT {

  private val driver = new AccumuloMiniClusterDriver()
  private var eventStore:AccumuloEventStore = _
  private var sparkContext: SparkContext = _
  private implicit var sqlContext: SQLContext = _

  @BeforeClass
  def setup: Unit = {
    driver.start()
    driver.deleteAllTables()
    eventStore = new AccumuloEventStore(driver.getConnector)

    val sparkConf = new SparkConf().setMaster("local").setAppName("TestEventStoreSQL")
    sparkContext = new SparkContext(sparkConf)
    sqlContext = new SQLContext(sparkContext)
  }

  private def createTempTable = {

    TableUtil.registerEventFilteredTable("root",
                             "secret",
                             driver.getInstanceName,
                             driver.getZooKeepers,
                             new DateTime(System.currentTimeMillis() - 5000),
                             new DateTime(System.currentTimeMillis() + 5000),
                             "type",
                             "events")
  }

  private def persistEvents = {
    val event = EventBuilder.create("type", "id", System.currentTimeMillis())
      .attr(new Attribute("key1", "val1"))
      .attr(new Attribute("key2", 5))
      .build()

    val entity2 = EventBuilder.create("type2", "id", System.currentTimeMillis())
      .attr(new Attribute("key1", "val1"))
      .attr(new Attribute("key3", "val3"))
      .build()

    eventStore.save(Collections.singleton(event))
    eventStore.save(Collections.singleton(entity2))
    eventStore.flush()
  }

  @AfterClass
  def tearDown: Unit = {
    sparkContext.stop()
    driver.close()
  }

}
class EventStoreFilteredIT {

  import org.calrissian.accumulorecipes.spark.sql.EventStoreFilteredIT._

  @Before
  def setupTest: Unit = {
    persistEvents
    createTempTable
  }

  @After
  def teardownTest: Unit = {
    sqlContext.dropTempTable("events")
    AccumuloTestUtils.clearTable(driver.getConnector, "eventStore_shard")
    AccumuloTestUtils.clearTable(driver.getConnector, "eventStore_index")
  }

  @Test
  def testJoin(): Unit = {
    TableUtil.registerEventFilteredTable("root",
      "secret",
      driver.getInstanceName,
      driver.getZooKeepers,
      new DateTime(System.currentTimeMillis() - 5000),
      new DateTime(System.currentTimeMillis() + 5000),
      "type2",
      "events2")

    AccumuloTestUtils.dumpTable(driver.getConnector, "eventStore_shard")

    val rows = sqlContext.sql("SELECT e.key1,e.key2,t.key3 FROM events e INNER JOIN events2 t ON e.key1 = t.key1").collect

    System.out.println(rows.toList)
    Assert.assertEquals(1, rows.length)
    Assert.assertEquals("val1", rows(0).getAs[String](0))
    Assert.assertEquals(5, rows(0).getAs[Int](1))
    Assert.assertEquals("val3", rows(0).getAs[String](2))

    sqlContext.dropTempTable("events2")
  }

  @Test
  def testSelectionAndWhereWithAndOperator() {

    val rows = sqlContext.sql("SELECT key2,key1 FROM events WHERE (key1 = 'val1' and key2 >= 5)").collect

    Assert.assertEquals(1, rows.length)
    Assert.assertEquals(5, rows(0).getAs[Int](0))
    Assert.assertEquals("val1", rows(0).getAs(1))
  }

  @Test
  def testSelectAndWhereSingleOperator() {

    val rows = sqlContext.sql("SELECT key1, key2 FROM events WHERE (key1 = 'val1' and key2 = 5)").collect

    Assert.assertEquals(1, rows.length)
    Assert.assertEquals(5, rows(0).getAs[Int](1))
    Assert.assertEquals("val1", rows(0).getAs(0))
  }

  @Test
  def testSelectionOnlyMultipleFields() {

    val rows = sqlContext.sql("SELECT key1,key2 FROM events").collect

    Assert.assertEquals(1, rows.length)
    Assert.assertEquals(5, rows(0).getAs[Int](1))
    Assert.assertEquals("val1", rows(0).getAs(0))
  }

  @Test
  def testFieldInSchemaMissingFromEventIsNull: Unit = {
    val event = EventBuilder.create("type", "id2", System.currentTimeMillis())
      .attr(new Attribute("key1", "val2"))
      .attr(new Attribute("key2", 10))
      .attr(new Attribute("key3", 5))
      .build()

    eventStore.save(Collections.singleton(event))
    eventStore.flush

    // Reset temporary table to provide updated schema
    sqlContext.dropTempTable("events")
    createTempTable

    val rows = sqlContext.sql("SELECT * FROM events WHERE key2 >= 5 ORDER BY key2 ASC").collect

    Assert.assertEquals(2, rows.length)
    Assert.assertNull(rows(0).apply(2)) // the event missing key3 should sort first given the orderBy

  }

  @Test
  def testSelectionOnlySingleField = {

    val rows = sqlContext.sql("SELECT key1 FROM events").collect

    Assert.assertEquals(1, rows.length)
    Assert.assertEquals("val1", rows(0).getAs(0))
  }

  @Test
  def testNoSelectionAndNoWhereClause = {

    val rows = sqlContext.sql("SELECT * FROM events").collect

    Assert.assertEquals(1, rows.length)
    Assert.assertEquals(5, rows(0).getAs[Int](1))
    Assert.assertEquals("val1", rows(0).getAs(0))
  }

  @Test(expected = classOf[AnalysisException])
  def testSelectFieldNotInSchema: Unit = sqlContext.sql("SELECT doesntExist FROM events").collect

  @Test
  def testWhereClauseNoMatches: Unit = Assert.assertEquals(0, sqlContext.sql("SELECT * FROM events WHERE key2 > 5").collect.length)


}
