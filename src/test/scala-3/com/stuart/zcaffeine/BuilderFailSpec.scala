package com.stuart.zcaffeine

import zio.*
import zio.test.Assertion.*
import zio.test.*
import zio.test.TestEnvironment

object BuilderFailSpec extends ZIOSpecDefault {

  // Used by the code in typeCheck
  import zio.*
  import com.stuart.zcaffeine.types.*
  import com.github.benmanes.caffeine.cache.stats.StatsCounter

  override def spec: Spec[TestEnvironment, TestFailure[Nothing]] =
    suite("BuilderFailSpec")(
      // /////////////////////
      // CONFIGURING TWICE //
      // /////////////////////
      test("cannot set eviction listener twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.evictionListener((_,_,_) => ZIO.unit)
                               .evictionListener((_,_,_) => ZIO.unit)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set expireAfter twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set expireAfterAccess twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.expireAfterAccess(1.millis)
                               .expireAfterAccess(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set expireAfterWrite twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.expireAfterWrite(1.millis)
                               .expireAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set initCapacity twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.initialCapacity(InitialCapacity(1))
                               .initialCapacity(InitialCapacity(1))
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set maximumSize twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.maximumSize(MaxSize(1))
                               .maximumSize(MaxSize(1))
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set maximumWeight twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.maximumWeight(MaximumWeight(3))
                               .maximumWeight(MaximumWeight(3))
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set recordStats twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.recordStats()
                               .recordStats()
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set refreshAfterWrite twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.refreshAfterWrite(1.millis)
                               .refreshAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot enable scheduling twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.enableScheduling()
                               .enableScheduling()
                               .build()
                            )"""))(isLeft(anything))
      },
      test("cannot set removalListener twice") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.removalListener((_,_,_) => ZIO.unit)
                               .removalListener((_,_,_) => ZIO.unit)
                               .build()
                            )"""))(isLeft(anything))
      },
      // /////////////////////
      // INCOMPATIBILITIES //
      // /////////////////////
      test("max size + max weight are incompatible") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.maximumSize(MaxSize(1))
                               .maximumWeight(MaximumWeight(3), (key, _) => key.length)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("max weight + max size are incompatible") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.maximumWeight(MaximumWeight(3), (key, _) => key.length)
                               .maximumSize(MaxSize(1))
                               .build()
                            )"""))(isLeft(anything))
      },
      test("expireAfter + expireAfterWrite are incompatible") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("expireAfter + expireAfterAccess are incompatible") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfterAccess(1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("expireAfter + expireAfterAccess are incompatible") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]().flatMap(
                              _.expireAfterAccess(1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(anything))
      },
      test("expireAfterWrite + expireAfter are incompatible") {
        assertZIO(typeCheck("""ZCaffeine[Any, String, Int]()
                              .flatMap(
                              _.expireAfterWrite(1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(anything))
      }
    )
}
