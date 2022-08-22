package com.stuart.zcaffeine

import zio.*
import zio.test.Assertion.*
import zio.test.*

object BuilderFailSpec extends DefaultRunnableSpec {

  // Used by the code in typeCheck
  import zio.*
  import com.stuart.zcaffeine.types.*
  import com.github.benmanes.caffeine.cache.stats.StatsCounter

  override def spec =
    suite("BuilderFailSpec")(
      // /////////////////////
      // CONFIGURING TWICE //
      // /////////////////////
      testM("cannot set eviction listener twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.evictionListener((_,_,_) => ZIO.unit)
                               .evictionListener((_,_,_) => ZIO.unit)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set expireAfter twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set expireAfterAccess twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfterAccess(1.millis)
                               .expireAfterAccess(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set expireAfterWrite twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfterWrite(1.millis)
                               .expireAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set initCapacity twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.initialCapacity(InitialCapacity(1))
                               .initialCapacity(InitialCapacity(1))
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set maximumSize twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.maximumSize(MaxSize(1))
                               .maximumSize(MaxSize(1))
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set maximumWeight twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.maximumWeight(MaximumWeight(3))
                               .maximumWeight(MaximumWeight(3))
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set recordStats twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.recordStats()
                               .recordStats()
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set refreshAfterWrite twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.refreshAfterWrite(1.millis)
                               .refreshAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot enable scheduling twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.enableScheduling()
                               .enableScheduling()
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("cannot set removalListener twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.removalListener((_,_,_) => ZIO.unit)
                               .removalListener((_,_,_) => ZIO.unit)
                               .build()
                            )"""))(isLeft(anything))
      },
      // /////////////////////
      // INCOMPATIBILITIES //
      // /////////////////////
      testM("max size + max weight are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.maximumSize(MaxSize(1))
                               .maximumWeight(MaximumWeight(3), (key, _) => key.length)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("max weight + max size are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.maximumWeight(MaximumWeight(3), (key, _) => key.length)
                               .maximumSize(MaxSize(1))
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("expireAfter + expireAfterWrite are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("expireAfter + expireAfterAccess are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfterAccess(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("expireAfter + expireAfterAccess are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfterAccess(1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      testM("expireAfterWrite + expireAfter are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]()
                              .flatMap(
                              _.expireAfterWrite(1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(anything))
      }
    )
}
