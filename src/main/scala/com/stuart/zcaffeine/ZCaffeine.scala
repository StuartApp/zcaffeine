package com.stuart.zcaffeine

import java.util
import java.util.concurrent._
import scala.annotation.{implicitAmbiguous, nowarn}
import scala.jdk.CollectionConverters._
import com.stuart.zcaffeine.ZCaffeine.State
import com.stuart.zcaffeine.ZCaffeine.State._
import com.stuart.zcaffeine.types._
import com.github.benmanes.caffeine.cache.{AsyncCacheLoader, Caffeine, CaffeineSpec, Expiry, RemovalCause}
import zio._
import zio.clock.Clock
import zio.duration._

object ZCaffeine {

  /**
   * Represents the current state of the [[com.stuart.zcaffeine.ZCaffeine]] cache builder.
   *
   * Used in combination with [[State.Conflict]] to restrict incompatible configurations of the underlying
   * [[com.github.benmanes.caffeine.cache.Caffeine]] builder, like configuring both max size and max weight.
   *
   * Note: using `ZCaffeine.fromCaffeineSpec` breaks those checks, as we don’t know for sure what has been already
   * configured.
   */
  sealed trait State

  // $COVERAGE-OFF$
  object State {
    private def unexpected = sys.error("Unexpected phantom type invocation")

    /**
     * Represents conflicts that can exists between different options of the [[com.stuart.zcaffeine.ZCaffeine]] cache
     * builder.
     *
     * Used in combination with [[State]] to restrict incompatible configurations of the underlying
     * [[com.github.benmanes.caffeine.cache.Caffeine]] builder, like configuring both max size and max weight.
     *
     * Note: using `ZCaffeine.fromCaffeineSpec` breaks those checks, as we don’t know for sure what has been already
     * configured.
     */
    sealed trait Conflict[S1 <: State, S2 <: State]
    implicit def conflict[S1 <: State, S2 <: State]: Conflict[S1, S2] = new Conflict[S1, S2] {}

    /** Nothing configured yet. */
    sealed trait Unconfigured extends State

    /** An eviction listener has been configured. */
    sealed trait EvictionListener extends State

    /** Expiry has been configured. */
    sealed trait ExpireAfter extends State

    /** Expiry after access has been configured. */
    sealed trait ExpireAfterAccess extends State

    /** Expiry after write has been configured. */
    sealed trait ExpireAfterWrite extends State

    /** An initial capacity has been set. */
    sealed trait InitCapacity extends State

    /** A maximum size has been set. */
    sealed trait MaxSize extends State

    /** A maximum weight has been set. */
    sealed trait MaxWeight extends State

    /** Stats recording has been enabled. */
    sealed trait RecordStats extends State

    /** Refresh after write behavior has been configured. */
    sealed trait RefreshAfterWrite extends State

    /** Scheduling has been enabled */
    sealed trait EnableScheduling extends State

    /** A removal listener has been configured */
    sealed trait RemovalListener extends State

    // BLOCK CONFIGURING TWICE START

    @implicitAmbiguous("evictionListener can only be configured once")
    implicit def evictionListener1[S <: State]: Conflict[EvictionListener, S with EvictionListener] = unexpected
    implicit def evictionListener2[S <: State]: Conflict[EvictionListener, S with EvictionListener] = unexpected

    @implicitAmbiguous("expireAfter can only be configured once")
    implicit def expireAfter1[S <: State]: Conflict[ExpireAfter, S with ExpireAfter] = unexpected
    implicit def expireAfter2[S <: State]: Conflict[ExpireAfter, S with ExpireAfter] = unexpected

    @implicitAmbiguous("expireAfterAccess can only be configured once")
    implicit def expireAfterAccess1[S <: State]: Conflict[ExpireAfterAccess, S with ExpireAfterAccess] = unexpected
    implicit def expireAfterAccess2[S <: State]: Conflict[ExpireAfterAccess, S with ExpireAfterAccess] = unexpected

    @implicitAmbiguous("expireAfterWrite can only be configured once")
    implicit def expireAfterWrite1[S <: State]: Conflict[ExpireAfterWrite, S with ExpireAfterWrite] = unexpected
    implicit def expireAfterWrite2[S <: State]: Conflict[ExpireAfterWrite, S with ExpireAfterWrite] = unexpected

    @implicitAmbiguous("initialCapacity can only be configured once")
    implicit def initCapacity1[S <: State]: Conflict[InitCapacity, S with InitCapacity] = unexpected
    implicit def initCapacity2[S <: State]: Conflict[InitCapacity, S with InitCapacity] = unexpected

    @implicitAmbiguous("maximumSize can only be configured once")
    implicit def maxSize1[S <: State]: Conflict[MaxSize, S with MaxSize] = unexpected
    implicit def maxSize2[S <: State]: Conflict[MaxSize, S with MaxSize] = unexpected

    @implicitAmbiguous("maximumWeight can only be configured once")
    implicit def maxWeight1[S <: State]: Conflict[MaxWeight, S with MaxWeight] = unexpected
    implicit def maxWeight2[S <: State]: Conflict[MaxWeight, S with MaxWeight] = unexpected

    @implicitAmbiguous("recordStats can only be configured once")
    implicit def recordStats1[S <: State]: Conflict[RecordStats, S with RecordStats] = unexpected
    implicit def recordStats2[S <: State]: Conflict[RecordStats, S with RecordStats] = unexpected

    @implicitAmbiguous("refreshAfterWrite can only be configured once")
    implicit def refreshAfterWrite1[S <: State]: Conflict[RefreshAfterWrite, S with RefreshAfterWrite] = unexpected
    implicit def refreshAfterWrite2[S <: State]: Conflict[RefreshAfterWrite, S with RefreshAfterWrite] = unexpected

    @implicitAmbiguous("enableScheduling can only be configured once")
    implicit def enableScheduling1[S <: State]: Conflict[EnableScheduling, S with EnableScheduling] = unexpected
    implicit def enableScheduling2[S <: State]: Conflict[EnableScheduling, S with EnableScheduling] = unexpected

    @implicitAmbiguous("removalListener can only be configured once")
    implicit def removalListener1[S <: State]: Conflict[RemovalListener, S with RemovalListener] = unexpected
    implicit def removalListener2[S <: State]: Conflict[RemovalListener, S with RemovalListener] = unexpected

    // BLOCK CONFIGURING  END

    // INCOMPATIBILITIES START
    @implicitAmbiguous("setting maxSize is incompatible with maxWeight")
    implicit def maxSizeAndWeight1[S <: State]: Conflict[MaxSize, S with MaxWeight] = unexpected
    implicit def maxSizeAndWeight2[S <: State]: Conflict[MaxSize, S with MaxWeight] = unexpected

    @implicitAmbiguous("setting maxWeight is incompatible with maxSize")
    implicit def maxWeightAndSize1[S <: State]: Conflict[MaxWeight, S with MaxSize] = unexpected
    implicit def maxWeightAndSize2[S <: State]: Conflict[MaxWeight, S with MaxSize] = unexpected

    @implicitAmbiguous("expireAfter is incompatible with expireAfterAccess")
    implicit def expireDynAndAccess1[S <: State]: Conflict[ExpireAfter, S with ExpireAfterAccess] = unexpected
    implicit def expireDynAndAccess2[S <: State]: Conflict[ExpireAfter, S with ExpireAfterAccess] = unexpected

    @implicitAmbiguous("expireAfter is incompatible with expireAfterWrite")
    implicit def expireDynAndWrite1[S <: State]: Conflict[ExpireAfter, S with ExpireAfterWrite] = unexpected
    implicit def expireDynAndWrite2[S <: State]: Conflict[ExpireAfter, S with ExpireAfterWrite] = unexpected

    @implicitAmbiguous("expireAfterAccess is incompatible with expireAfter")
    implicit def expireAccessAndDyn1[S <: State]: Conflict[ExpireAfterAccess, S with ExpireAfter] = unexpected
    implicit def expireAccessAndDyn2[S <: State]: Conflict[ExpireAfterAccess, S with ExpireAfter] = unexpected

    @implicitAmbiguous("expireAfterWrite is incompatible with expireAfter")
    implicit def expireWriteAndDyn1[S <: State]: Conflict[ExpireAfterWrite, S with ExpireAfter] = unexpected
    implicit def expireWriteAndDyn2[S <: State]: Conflict[ExpireAfterWrite, S with ExpireAfter] = unexpected

    // INCOMPATIBILITIES END
    // $COVERAGE-ON$
  }

  /**
   * Create a new [[ZCaffeine]] cache builder, preconfigured from the string representation of a `CaffeineSpec`.
   * Compared to other variants, this can fail if the spec string is invalid.
   *
   * Note: this variant breaks the checks offered by [[State]] & [[State.Conflict]], as we can’t know for sure what has
   * been already configured.
   *
   * @tparam R
   *   the ZIO environment used for effects interacting with the cache
   * @tparam Key
   *   Type of the cache keys
   * @tparam Value
   *   Type of the cache values
   * @return
   *   an preconfigured [[ZCaffeine]] cache builder
   */
  def fromCaffeineSpec[R <: Clock, Key, Value](caffeineSpec: String): RIO[R, ZCaffeine[R, Unconfigured, Key, Value]] =
    for {
      spec      <- ZIO.effect(CaffeineSpec.parse(caffeineSpec))
      zcaffeine <- fromCaffeineSpec[R, Key, Value](spec)
    } yield zcaffeine

  /**
   * Create a new [[ZCaffeine]] cache builder, preconfigured from a `CaffeineSpec`.
   *
   * Note: this variant breaks the checks offered by [[State]] & [[State.Conflict]], as we can’t know for sure what has
   * been already configured.
   * @tparam R
   *   the ZIO environment used for effects interacting with the cache
   * @tparam Key
   *   Type of the cache keys
   * @tparam Value
   *   Type of the cache values
   * @return
   *   an unconfigured [[ZCaffeine]] cache builder
   */
  def fromCaffeineSpec[R <: Clock, Key, Value](
      caffeineSpec: CaffeineSpec
  ): URIO[R, ZCaffeine[R, Unconfigured, Key, Value]] =
    apply(Caffeine.from(caffeineSpec).asInstanceOf[Caffeine[Key, Value]])

  /**
   * Create a new [[ZCaffeine]] cache builder, with all defaults settings from `Caffeine.newBuilder`.
   * @tparam R
   *   the ZIO environment used for effects interacting with the cache
   * @tparam Key
   *   Type of the cache keys
   * @tparam Value
   *   Type of the cache values
   * @return
   *   an unconfigured [[ZCaffeine]] cache builder
   */
  def apply[R <: Clock, Key, Value](): URIO[R, ZCaffeine[R, Unconfigured, Key, Value]] =
    apply(Caffeine.newBuilder().asInstanceOf[Caffeine[Key, Value]])

  private def apply[R <: Clock, Key, Value](
      caffeine: => Caffeine[Key, Value]
  ): URIO[R, ZCaffeine[R, Unconfigured, Key, Value]] =
    for {
      executor <- ZIO.executor
      runtime  <- ZIO.runtime[R]
      configured = ZIO.effect(caffeine.executor(executor.asJava).ticker(() => zioToValue(runtime)(zio.clock.nanoTime))
                   )
    } yield new ZCaffeine[R, Unconfigured, Key, Value](runtime, configured)
}

@nowarn("msg=parameter value ev in method .* is never used")
class ZCaffeine[R <: Clock, S <: State, Key, Value] private (
    runtime: Runtime[R],
    builder: RIO[R, Caffeine[Key, Value]]
) {

  /**
   * Builds a cache which does not automatically load values when keys are requested unless a mapping function is
   * provided. If the asynchronous computation fails value then the entry will be automatically removed. Note that
   * multiple threads can concurrently load values for distinct keys. <p> Consider `build(loadOne,loadAll,reloadOne)` if
   * possible. <p> This method does not alter the state of this builder instance, so it can be invoked again to create
   * multiple independent caches. <p>
   *
   * @return
   *   a cache having the requested features
   */
  def build(): RIO[R, Cache[R, Key, Value]] =
    builder.flatMap(b => ZIO.effect(new Cache(runtime, b.buildAsync())))

  /**
   * Builds a cache, which either returns the value already loaded or currently computing the value for a given key, or
   * computes the value asynchronously. If the asynchronous computation fails then the entry will be automatically
   * removed. Note that multiple threads can concurrently load values for distinct keys. <p> This method does not alter
   * the state of this builder instance, so it can be invoked again to create multiple independent caches. <p>
   *
   * @param loadOne
   *   a function defining how to compute a value for a given key
   * @param loadAll
   *   a function defining how to batch compute values for a set of keys
   * @param reloadOne
   *   a function defining to refresh an existing cache entry
   * @return
   *   a cache having the requested features
   */
  def build(
      loadOne: Key => RIO[R, Value],
      loadAll: Option[Set[Key] => RIO[R, Map[Key, Value]]] = None,
      reloadOne: Option[(Key, Value) => RIO[R, Value]] = None
  ): RIO[R, LoadingCache[R, Key, Value]] =
    builder.flatMap { b =>
      ZIO.effect {
        new LoadingCache(
          runtime,
          b.buildAsync(new AsyncCacheLoader[Key, Value] {

            override def asyncLoad(key: Key, executor: Executor): CompletableFuture[Value] =
              zioToCompletableFuture(runtime)(loadOne(key))

            override def asyncLoadAll(
                keys: util.Set[_ <: Key],
                executor: Executor
            ): CompletableFuture[util.Map[Key, Value]] = zioToCompletableFuture(runtime) {
              loadAll match {
                case Some(f) => f(keys.asScala.toSet).map(_.asJava)
                case None =>
                  ZIO
                    .foreach(keys.asScala.toSet)(key => loadOne(key).map(key -> _))
                    .map(_.toMap[Key, Value].asJava)
              }
            }

            override def asyncReload(key: Key, oldValue: Value, executor: Executor): CompletableFuture[Value] =
              zioToCompletableFuture(runtime) {
                reloadOne match {
                  case Some(f) => f(key, oldValue)
                  case None    => loadOne(key)
                }
              }
          })
        )
      }
    }

  /**
   * Specifies a listener instance that caches should notify each time an entry is evicted. The cache will invoke this
   * listener during the atomic operation to remove the entry. In the case of expiration or reference collection, the
   * entry may be pending removal and will be discarded as part of the routine maintenance described in the class
   * documentation above. Scheduling may be enabled for a prompt removal of expired entries. A `removalListener` may be
   * preferred when the listener should be invoked for any [[com.github.benmanes.caffeine.cache.RemovalCause]] and
   * performed outside of the atomic operation to remove the entry. <p>
   *
   * @param listener
   *   a listener instance that caches should notify each time an entry is being automatically removed due to eviction
   * @return
   *   this builder instance (for chaining)
   */
  def evictionListener(
      listener: (Key, Value, RemovalCause) => RIO[R, Unit]
  )(implicit ev: Conflict[EvictionListener, S]): ZCaffeine[R, S with EvictionListener, Key, Value] =
    withNewBuilder(
      _.evictionListener((key: Key, value: Value, cause: RemovalCause) =>
        zioToValue(runtime)(listener(key, value, cause))
      )
    )

  /**
   * Specifies that each entry should be automatically removed from the cache once a duration has elapsed after the
   * entry's creation, the most recent replacement of its value, or its last read. The expiration time is reset by all
   * cache read and write operations. <p> Expired entries may be counted in `estimatedSize`, but will never be visible
   * to read or write operations. Expired entries are cleaned up as part of the routine maintenance described in the
   * class javadoc. Scheduling may be enabled for a prompt removal of expired entries. <p>
   *
   * @param afterCreate
   *   the function to calculate expiration time after creating a new entry
   * @param afterUpdate
   *   the function to calculate expiration time after updating an entry
   * @param afterRead
   *   the function to calculate expiration time after reading an entry
   *
   * @return
   *   this builder instance (for chaining)
   */
  def expireAfter(
      afterCreate: (Key, Value, Duration) => Duration,
      afterUpdate: (Key, Value, Duration, Duration) => Duration,
      afterRead: (Key, Value, Duration, Duration) => Duration
  )(implicit ev: Conflict[ExpireAfter, S]): ZCaffeine[R, S with ExpireAfter, Key, Value] =
    withNewBuilder(_.expireAfter(new Expiry[Key, Value] {

      override def expireAfterCreate(key: Key, value: Value, currentTime: Long): Long =
        afterCreate(key, value, currentTime.nanos).toNanos

      override def expireAfterUpdate(key: Key, value: Value, currentTime: Long, currentDuration: Long): Long =
        afterUpdate(key, value, currentTime.nanos, currentDuration.nanos).toNanos

      override def expireAfterRead(key: Key, value: Value, currentTime: Long, currentDuration: Long): Long =
        afterRead(key, value, currentTime.nanos, currentDuration.nanos).toNanos
    }))

  /**
   * Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after
   * the entry's creation, the most recent replacement of its value, or its last access. Access time is reset by all
   * cache read and write operations. <p> Expired entries may be counted in `estimatedSize`, but will never be visible
   * to read or write operations. Expired entries are cleaned up as part of the routine maintenance described in the
   * class javadoc. Scheduling may be enabled for a prompt removal of expired entries.
   *
   * @param duration
   *   the length of time after an entry is last accessed that it should be automatically removed
   * @return
   *   this builder instance (for chaining)
   */
  def expireAfterAccess(duration: Duration)(implicit
      ev: Conflict[ExpireAfterAccess, S]
  ): ZCaffeine[R, S with ExpireAfterAccess, Key, Value] =
    withNewBuilder(_.expireAfterAccess(duration.toNanos, TimeUnit.NANOSECONDS))

  /**
   * Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after
   * the entry's creation, or the most recent replacement of its value. <p> Expired entries may be counted in
   * `estimatedSize`, but will never be visible to read or write operations. Expired entries are cleaned up as part of
   * the routine maintenance described in the class javadoc. Scheduling may be enabled for a prompt removal of expired
   * entries.
   *
   * @param duration
   *   the length of time after an entry is created that it should be automatically removed
   * @return
   *   this builder instance (for chaining)
   */
  def expireAfterWrite(duration: Duration)(implicit
      ev: Conflict[ExpireAfterWrite, S]
  ): ZCaffeine[R, S with ExpireAfterWrite, Key, Value] =
    withNewBuilder(_.expireAfterWrite(duration.toNanos, TimeUnit.NANOSECONDS))

  /**
   * Sets the minimum total size for the internal data structures. Providing a large enough estimate at construction
   * time avoids the need for expensive resizing operations later, but setting this value unnecessarily high wastes
   * memory.
   *
   * @param capacity
   *   minimum total size for the internal data structures
   * @return
   *   this builder instance (for chaining)
   */
  def initialCapacity(capacity: InitialCapacity)(implicit
      ev: Conflict[InitCapacity, S]
  ): ZCaffeine[R, S with InitCapacity, Key, Value] =
    withNewBuilder(_.initialCapacity(capacity))

  /**
   * Specifies the maximum number of entries the cache may contain. Note that the cache <b>may evict an entry before
   * this limit is exceeded or temporarily exceed the threshold while evicting</b>. As the cache size grows close to the
   * maximum, the cache evicts entries that are less likely to be used again. For example, the cache may evict an entry
   * because it hasn't been used recently or very often. <p> When <pre>maximumSize</pre> is zero, elements will be
   * evicted immediately after being loaded into the cache. This can be useful in testing, or to disable caching
   * temporarily without a code change. <p>
   *
   * @param maximumSize
   *   the maximum size of the cache
   * @return
   *   this builder instance (for chaining)
   */
  def maximumSize(maximumSize: MaximumSize)(implicit
      ev: Conflict[MaxSize, S]
  ): ZCaffeine[R, S with MaxSize, Key, Value] =
    withNewBuilder(_.maximumSize(maximumSize))

  /**
   * Specifies the maximum weight of entries the cache may contain. Weight is determined using the <pre>weigher</pre>,
   * mapping key/value pairs to their relative weight in the cache. <p> Note that the cache <b>may evict an entry before
   * this limit is exceeded or temporarily exceed the threshold while evicting</b>. As the cache size grows close to the
   * maximum, the cache evicts entries that are less likely to be used again. For example, the cache may evict an entry
   * because it hasn't been used recently or very often. <p> When <pre>maximumWeight</pre> is zero, elements will be
   * evicted immediately after being loaded into the cache. This can be useful in testing, or to disable caching
   * temporarily without a code change. <p> Note that weight is only used to determine whether the cache is over
   * capacity; it has no effect on selecting which entry should be evicted next. <p>
   *
   * @param maximumWeight
   *   the maximum total weight of entries the cache may contain
   * @return
   *   this builder instance (for chaining)
   */
  def maximumWeight(maximumWeight: MaximumWeight, weigher: (Key, Value) => Int = (_, _) => 1)(implicit
      ev: Conflict[MaxWeight, S]
  ): ZCaffeine[R, S with MaxWeight, Key, Value] =
    withNewBuilder(_.maximumWeight(maximumWeight).weigher(weigher(_, _)))

  /**
   * Enables the accumulation of [[com.github.benmanes.caffeine.cache.stats.CacheStats]] during the operation of the
   * cache. Without this `stats` will return zero for all statistics. Note that recording statistics requires
   * bookkeeping to be performed with each operation, and thus imposes a performance penalty on cache operation.
   *
   * @return
   *   this builder instance (for chaining)
   */
  def recordStats()(implicit ev: Conflict[RecordStats, S]): ZCaffeine[R, S with RecordStats, Key, Value] =
    withNewBuilder(_.recordStats())

  /**
   * Specifies that active entries are eligible for automatic refresh once a fixed duration has elapsed after the
   * entry's creation, or the most recent replacement of its value. The semantics of refreshes are specified in
   * `refresh`, and are performed by calling `reload`. <p> Automatic refreshes are performed when the first stale
   * request for an entry occurs. The request triggering refresh will make an asynchronous call to `reload` and
   * immediately return the old value.
   *
   * @param duration
   *   the length of time after an entry is created that it should be considered stale, and thus eligible for refresh
   * @return
   *   this builder instance (for chaining)
   */
  def refreshAfterWrite(duration: Duration)(implicit
      ev: Conflict[RefreshAfterWrite, S]
  ): ZCaffeine[R, S with RefreshAfterWrite, Key, Value] =
    withNewBuilder(_.refreshAfterWrite(duration.toNanos, TimeUnit.NANOSECONDS))

  /**
   * Specifies a listener instance that caches should notify each time an entry is removed for any
   * [[com.github.benmanes.caffeine.cache.RemovalCause]]. The cache will invoke this listener after the entry's removal
   * operation has completed. In the case of expiration or reference collection, the entry may be pending removal and
   * will be discarded as part of the routine maintenance described in the class documentation above. Scheduling may be
   * enabled for a prompt removal of expired entries. An `evictionListener` may be preferred when the listener should be
   * invoked as part of the atomic operation to remove the entry. <p>
   *
   * @param listener
   *   a listener instance that caches should notify each time an entry is removed
   * @return
   *   this builder instance (for chaining)
   */
  def removalListener(
      listener: (Key, Value, RemovalCause) => RIO[R, Unit]
  )(implicit ev: Conflict[RemovalListener, S]): ZCaffeine[R, S with RemovalListener, Key, Value] =
    withNewBuilder(
      _.removalListener((key: Key, value: Value, cause: RemovalCause) =>
        zioToValue(runtime)(listener(key, value, cause))
      )
    )

  /**
   * This augments the periodic maintenance that occurs during normal cache operations to allow for the prompt removal
   * of expired entries regardless of whether any cache activity is occurring at that time. By default, it is disabled.
   * <p> The scheduling between expiration events is paced to exploit batching and to minimize executions in short
   * succession. This minimum difference between the scheduled executions is implementation-specific, currently at ~1
   * second (2&#94;30 ns). The scheduling is best-effort and does not make any hard guarantees of when an expired entry
   * will be removed. <p>
   *
   * @return
   *   this builder instance (for chaining)
   */
  def enableScheduling()(implicit
      ev: Conflict[EnableScheduling, S]
  ): ZCaffeine[R, S with EnableScheduling, Key, Value] =
    withNewBuilder(
      _.scheduler((_, command, delay, timeUnit) =>
        zioToCompletableFuture(runtime)(
          ZIO.effect(command.run()).schedule(Schedule.duration(Duration(delay, timeUnit))).unit
        )
      )
    )

  private def withNewBuilder[NewState <: State](configurer: Caffeine[Key, Value] => Caffeine[Key, Value]) =
    new ZCaffeine[R, S with NewState, Key, Value](runtime, builder.flatMap(b => ZIO.effect(configurer(b))))

}
