# zcaffeine: ZIO-friendly Caffeine wrapper

zcaffeine is a ZIO-friendly wrapper for the [Caffeine cache library](https://github.com/ben-manes/caffeine).

* Built for ZIO 2
* Available for Scala 2.13 and Scala 3

## Setup

Add the library to your dependencies:

```scala
libraryDependencies += "com.stuart" %% "zcaffeine" % "x.x.x"
```

Snapshots are also published on [Sonatype’s snapshots repository](https://s01.oss.sonatype.org/content/repositories/snapshots/com/stuart/).

## Usage

The [ZCaffeine](src/main/scala/com/stuart/zcaffeine/ZCaffeine.scala) object is the entrypoint for configuring your cache, by using either:
* A [CaffeineSpec](https://www.javadoc.io/doc/com.github.ben-manes.caffeine/caffeine/latest/com.github.benmanes.caffeine/com/github/benmanes/caffeine/cache/CaffeineSpec.html),
  either in its string representation or already loaded
* An unconfigured builder

Please note that setting that can be configured by a [CaffeineSpec](https://www.javadoc.io/doc/com.github.ben-manes.caffeine/caffeine/latest/com.github.benmanes.caffeine/com/github/benmanes/caffeine/cache/CaffeineSpec.html)
can also be configured from the builder, with the builder supporting more options:
* Setting removal and/or evictions listeners
* Dynamically setting the expiration of cache entries based on the key & value
* Enabling scheduling of cache maintenance (rather than running maintenance on cache operations)
* Compute a cache entry weight dynamically, based on the key & value

After configuration, the cache can be constructed through either:
* `build()`: builds a "manually operated" `Cache` which requires the use of `get(key, mappingFunction)`/`getAll(keys, mappingFunction`)/`put(key,value)` 
             to load or replace values in the cache
* `build(loadOne,loadAll,reloadOne)`: builds an "automated" `LoadingCache` cache which offers, along with the other methods from `Cache`,
                                      `get(key)`/`getAll(keys)`/`refresh`/`refreshAll` that defers to `loadOne`/`loadAll`,`reloadAll` to compute the cache entries

> Note:
> `loadAll` and `reloadOne` are optional: both defer to `loadOne` if missing, but they can
> set if the behavior should differ (eg. parallel computation in `loadAll`)

## Examples

### Building a cache

```scala

// Configure a cache
val zcaffeine = ZCaffeine[Any, String, String]().map(
    _.initialCapacity(InitialCapacity(1))
      .maximumSize(MaxSize(10))
      .enableScheduling()
      .recordStats()
      .expireAfter(
        afterCreate = (key, _, _) => key.length.hour,
        afterUpdate = (key, _, _, _) => key.length.minutes,
        afterRead = (_, _, _, _) => Duration.Infinity
      )
  )
  
val getTime = ZIO.clockWith(_.instant)

// Create a Cache 
val cache = zcaffeine.flatMap(_.build())

// Create a LoadingCache
val loadingCache = zcaffeine.flatMap(
    _.build(
      loadOne = key => getTime.map(time => s"one::$key:$time"),
      loadAll = Some(keys => getTime.map(time => keys.map(key => (key, s"all::$key:$time")).toMap)),
      reloadOne = Some((key, oldValue) => getTime.map(time => s"reload::$key:$oldValue-->$time"))
    )
  )  
```

### Using a cache

You can also find additional examples from [zcaffeine’s tests](src/test/scala/com/stuart/zcaffeine).

```scala
/**********************************************/
/*               Using a Cache                */
/* Supposing cache = Cache[Any,String,String] */
/**********************************************/


// get or compute
cache.get("key")
// get all or compute all
cache.getAll(Set("key1","key2"), keys => ZIO.succeed(keys.map(key => (key, key + key)).toMap))
// get only if cached
cache.getIfPresent("key3")
// insert or replace
cache.put("key", ZIO.succeed("value1"))
// invalidate one
cache.invalidate("key")
// invalidate all
cache.invalidateAll
// invalidate all specified keys
cache.invalidateAll(Set("key1","key2"))


/*****************************************************/
/*               Using a LoadingCache                */
/* Supposing cache = LoadingCache[Any,String,String] */
/*****************************************************/

// get or compute using the functions set while building the cache
cache.get("key1")
// get all or compute all using the functions set while building the cache
cache.getAll(Set("key1","key2"))
// refresh using the functions set while building the cache and get
cache.refresh("key1")
// refresh all using the functions set while building the cache and get all
cache.refreshAll(Set("key1","key2")) 
```
## License

This project is published under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).
