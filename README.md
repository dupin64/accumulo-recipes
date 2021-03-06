[![Build Status](https://travis-ci.org/calrissian/accumulo-recipes.svg?branch=master)](https://travis-ci.org/calrissian/accumulo-recipes)


#What are Accumulo Recipes?

These recipes and stores have been created as a starting point for using Accumulo to implement various different use-cases. The projects contained in this repository could be used either directly or modified entirely to fit differing needs. The code is meant to provide different scenarios and exemplify:

- How effective Accumulo can be at munging data in parallel across a cluster of machines. 
- How great Accumulo can index and process data using lexicographically sorted keys making that data immediately available.
- How well Accumulo is integrated into the Hadoop stack where data partitioning across the cluster can be fine-tuned to take advantage of locality when doing bulk operations.

Be sure to check the README files at the root of each store's folder to get detailed instructions and explanations. If you've got a recipe that you don't see here, we'd love to host it and get it circulating through the community. 


## Versions

  * \<  2.0.0 uses Hadoop 0.20.2 and Accumulo 1.4
  * \>= 2.0.0 and above uses Hadoop 2.* and Accumulo 1.6.

API and table structure compatibility will not be guaranteed across minor versions. Major versions are reserved for significant updates (i.e. compeltely new versions of Accumulo and/or Hadoop). Bugfix versions will guarantee both API and table structure compatbility.

##Stores

- **[Blob Store][bs]**: This store demonstrates how to effectively stream bytes into and out of Accumulo tables. Large streams can be chunked up over several columns so that they don't need to fit into memory.
- **[Changelog Store][cs]**: This store is for distributed systems that need to be able to summarize data for determining how it may differ from other data. It uses [merkle trees](http://en.wikipedia.org/wiki/Merkle_tree) which get created on the server-side in parallel across a cluster.
- **[Entity Store][ens]**: This store is for common documents & objects that can be modelled using keys/values representing things in the real world (people, places, things, etc...). It takes full advantage of Accumulo's cell-level security and also provides a custom cell-level expiration policy. Rich query can be performed on the server side in parallel across the cloud. 
- **[Event Store][evs]**: This is a document/object store for time-based events that shards the data to make it very scalable to store and process. Like the Entity Store, it takes advantage of Accumulo's cell-level security and also provides a custom cell-level expiration policy. It also provides a query language for finding events of interest.
- **[Geospatial Store][ges]**: This store indexes events under geohashes. The data is partitioned in a way where even geopoints that are geographically close to each other can be spread across a cluster. The events can be queried back using rectangular "bounding boxes" representing a space on the earth.
- **[Graph Store][grs]**: This store indexes edges of a graph so that they can be easily traversed by their vertices. It allows for breadth-first traversal and filtering. An implementation of [Tinkerpop Blueprints](https://github.com/tinkerpop/blueprints/wiki) allows [Gremlin](https://github.com/tinkerpop/gremlin/wiki) queries to be performed to traverse the graph given a flexible groovy DSL.
- **[Last N Store][ls]**: This store is essentially a count expired window that allows events to be grouped together and queried back, keeping cell-level security in-tact for the events contained inside.
- **Feature Store**: This is a [feature vector](http://en.wikipedia.org/wiki/Feature_vector) store. That is, it stores mathematical summaries and models that can be continuously aggregated and enriched in different ways to aid in statistical analysis and machine learning algorithms. It allows for plugging in feature types like statistical metrics. A more simple extension of this store, the **[Metrics Store][ms]**, is useful for aggregating counts and other statistical algorithms that can be applied associatively over units of time (minutes, hours, days, months, etc...). Current metrics being aggregated are min, max, count, sum, average, and variance / standard deviation.
- **[Range Store][rs]**: Allows intervals (start and stop ranges) to be indexed so that overlapping intervals can be queried back easily.
- **[Temporal Last N Store][ts]**: Similar to the last-n store, this does't evict based on count, but rather allows a customizable window into a custom grouping for datasets for some point in time.

#Want to get involved?

These projects are all licensed under Apache 2.0. If you'd like to help out, you can start by looking at the github tickets. Pull requests are welcome as well. Check out the google group if you have any questions or want to start a discussion.

[bs]: store/blob-store/README.md
[cs]: store/changelog-store/README.md
[ens]: store/entity-store/README.md
[evs]: store/event-store/README.md
[ms]: store/feature-store/README.md
[ges]: store/geospatial-store/README.md
[grs]: store/graph-store/README.md
[ls]: store/lastn-store/README.md
[rs]: store/range-store/README.md
[ts]: store/temporal-last-n-store/README.md

