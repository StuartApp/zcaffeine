# zcaffeine: ZIO-friendly Caffeine wrapper

## Setup

Add the library to your dependencies:

```scala
libraryDependencies += "com.stuart" %% "zcaffeine" % "x.x.x"
```

## Usage

The [ZCaffeine](src/main/scala/com/stuart/zcaffeine/ZCaffeine.scala) object is the entrypoint for configuring your cache, by using either:
* A [CaffeineSpec](https://www.javadoc.io/doc/com.github.ben-manes.caffeine/caffeine/latest/com.github.benmanes.caffeine/com/github/benmanes/caffeine/cache/CaffeineSpec.html),
  either in its representation or already loaded
* A fully unconfigured builder

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

## License

This project is published under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).
