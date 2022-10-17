package com.stuart.zcaffeine

import scala.jdk.CollectionConverters._

import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.github.benmanes.caffeine.cache.{ AsyncCache, AsyncLoadingCache }
import zio._

sealed class Cache[R, Key, Value] private[zcaffeine] (runtime: Runtime[R], underlying: AsyncCache[Key, Value]) {

  /**
   * Returns the value associated to the given key in this cache, or asynchronously compute it if missing.
   * @param key
   *   the key whose associated value is to be returned
   * @param mappingFunction
   *   the function defining how to compute a new value for a given key
   * @return
   *   the current or newly computed value for the given key
   */
  def get(key: Key)(mappingFunction: Key => RIO[R, Value]): Task[Value] =
    ZIO.fromCompletableFuture(
      underlying.get(key, (key, _) => zioToCompletableFuture(runtime)(mappingFunction(key)))
    )

  /**
   * Returns the values associated with the given keys in this cache, or asynchronously compute them if missing. Keys
   * that are missing from the `mappingFunction` result won’t be saved in the cache.
   * @param keys
   *   the keys for which the associated values are to be returned
   * @param mappingFunction
   *   the function defining how to compute values for keys that are missing from the cache
   * @return
   *   the current or newly computed values for the given keys
   */
  def getAll(keys: Set[Key])(mappingFunction: Set[Key] => RIO[R, Map[Key, Value]]): Task[Map[Key, Value]] =
    getAllInternal(keys)(keys => mappingFunction(keys.toSet))

  /**
   * Returns the values associated with the given keys in this cache, or asynchronously compute them if missing. Keys
   * that are missing from the `mappingFunction` result won’t be saved in the cache.
   * @param keys
   *   the keys for which the associated values are to be returned
   * @param mappingFunction
   *   the function defining how to compute values for keys that are missing from the cache
   * @return
   *   the current or newly computed values for the given keys
   */
  def getAll(keys: Key*)(mappingFunction: Seq[Key] => RIO[R, Map[Key, Value]]): Task[Map[Key, Value]] =
    getAllInternal(keys)(keys => mappingFunction(keys.toSeq))

  /**
   * Returns the value associated with the given key if present in this cache, otherwise returns None.
   * @param key
   *   the key whose associated value is to be returned
   * @return
   *   the current value if present, otherwise None
   */
  def getIfPresent(key: Key): Task[Option[Value]] =
    Option(underlying.getIfPresent(key))
      .map(value => ZIO.fromCompletableFuture(value).asSome)
      .getOrElse(ZIO.none)

  /**
   * Creates or replaces the cached value for the given key in this cache. If the computation failed, remove the
   * existing entry.
   * @param key
   *   the key to associate with the value
   * @param value
   *   the value to associate with the key
   * @return
   */
  def put(key: Key, value: RIO[R, Value]): RIO[R, Unit] =
    value.toCompletableFuture.flatMap(v => ZIO.attempt(underlying.put(key, v)))

  /**
   * Removes the cached value for the given key in this cache if present.
   * @param key
   *   the key whose cached value is to be removed from the cache
   * @return
   */
  def invalidate(key: Key): Task[Unit] =
    ZIO.attemptBlocking(underlying.synchronous().invalidate(key))

  /**
   * Removes all cached values in this cache.
   * @return
   */
  def invalidateAll: Task[Unit] =
    ZIO.attemptBlocking(underlying.synchronous().invalidateAll())

  /**
   * Remove the cached values for the given keys in this cache if present.
   * @param keys
   *   the keys whose cached values are to be removed from the cache
   * @return
   */
  def invalidateAll(keys: Set[Key]): Task[Unit] =
    invalidateAllInternal(keys)

  /**
   * Remove the cached values for the given keys in this cache if present.
   * @param keys
   *   the keys whose cached values are to be removed from the cache
   * @return
   */
  def invalidateAll(keys: Key*): Task[Unit] =
    invalidateAllInternal(keys)

  /**
   * Returns the estimated size for this cache. It is only an estimation as it depends if there are pending
   * computations/evictions pending.
   * @return
   *   the estimated size of this cache
   */
  def estimatedSize: Task[Long] =
    ZIO.attemptBlocking(underlying.synchronous().estimatedSize())

  /**
   * Returns the cache statistics computed for this cache, if previously enabled with `recordStats` when building the
   * cache. To be used with caution as it induces a performance penalty.
   * @return
   *   the cache statistics for this cache
   */
  def stats: Task[CacheStats] =
    ZIO.attemptBlocking(underlying.synchronous().stats())

  /**
   * Returns an immutable view of the cache contents.
   * @return
   * the cache contents, as a Map
   */
  def asMap: Task[Map[Key,Value]] =
    ZIO.attemptBlocking(underlying.synchronous().asMap().asScala.toMap)

  /**
   * Immediately run any pending maintenance operations on this cache, such as evicting expired entries.
   * @return
   */
  def cleanUp: Task[Unit] =
    ZIO.attemptBlocking(underlying.synchronous().cleanUp())

  private def getAllInternal(
      keys: Iterable[Key]
  )(mappingFunction: Iterable[Key] => RIO[R, Map[Key, Value]]): Task[Map[Key, Value]] =
    ZIO
      .fromCompletableFuture(
        underlying
          .getAll(
            keys.asJava,
            (keys, _) => zioToCompletableFuture(runtime)(mappingFunction(keys.asScala).map(_.asJava))
          )
      )
      .map(_.asScala.toMap)

  private def invalidateAllInternal(keys: Iterable[Key]): Task[Unit] =
    ZIO.attemptBlocking(underlying.synchronous().invalidateAll(keys.asJava))
}

final class LoadingCache[R, Key, Value] private[zcaffeine] (
    runtime: Runtime[R],
    underlying: AsyncLoadingCache[Key, Value]
) extends Cache[R, Key, Value](runtime, underlying) {

  /**
   * Returns the value associated with the given key in this cache, or asynchronously computes it, based on the
   * `loadOne` function specified while building the cache
   * @param key
   *   the key whose associated value is to be returned
   * @return
   *   the current or newly computed value for the given key
   */
  def get(key: Key): Task[Value] =
    ZIO.fromCompletableFuture(underlying.get(key))

  /**
   * Returns the value associated with the given key in this cache, or asynchronously computes it, based on the
   * `loadAll` function specified while building the cache if defined, or falls back to `loadOne`.
   * @param keys
   *   the keys whose associated values are to be returned
   * @return
   *   the current or newly computed values for the given keys
   */
  def getAll(keys: Set[Key]): Task[Map[Key, Value]] =
    getAllInternal(keys)

  /**
   * Returns the value associated with the given key in this cache, or asynchronously computes it, based on the
   * `loadAll` function specified while building the cache if defined, or falls back to `loadOne`.
   * @param keys
   *   the keys whose associated values are to be returned
   * @return
   *   the current or newly computed values for the given keys
   */
  def getAll(keys: Key*): Task[Map[Key, Value]] =
    getAllInternal(keys)

  /**
   * Refreshes the value associated with the given key in this cache, based on the `reloadOne` function specified while
   * building the cache if defined, or falls back to `loadOne`.
   * @param key
   *   the key whose associated value are to be refreshed
   * @return
   *   the refreshed value for the given key
   */
  def refresh(key: Key): Task[Value] =
    ZIO.fromCompletableFuture(underlying.synchronous().refresh(key))

  /**
   * Refreshes the values associated with the given keys in this cache, based on the `reloadOne` function specified
   * while building the cache if defined, or falls back to `loadOne`.
   * @param keys
   *   the keys whose associated values are to be refreshed
   * @return
   *   the refreshed values for the given keys
   */
  def refreshAll(keys: Set[Key]): Task[Map[Key, Value]] =
    refreshAllInternal(keys)

  /**
   * Refreshes the values associated with the given keys in this cache, based on the `reloadOne` function specified
   * while building the cache if defined, or falls back to `loadOne`.
   * @param keys
   *   the keys whose associated values are to be refreshed
   * @return
   *   the refreshed values for the given keys
   */
  def refreshAll(keys: Key*): Task[Map[Key, Value]] =
    refreshAllInternal(keys)

  private def getAllInternal(keys: Iterable[Key]): Task[Map[Key, Value]] =
    ZIO.fromCompletableFuture(underlying.getAll(keys.asJava)).map(_.asScala.toMap)

  private def refreshAllInternal(keys: Iterable[Key]): Task[Map[Key, Value]] =
    ZIO.fromCompletableFuture(underlying.synchronous().refreshAll(keys.asJava)).map(_.asScala.toMap)
}
