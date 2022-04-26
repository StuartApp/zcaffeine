package com.stuart.zcaffeine

import zio.test.Assertion._
import zio.test.{ TestEnvironment, _ }

object BuilderFailSpec extends ZIOSpecDefault {

  // Used by the code in typeCheck
  import zio._
  import com.stuart.zcaffeine.types._

  override def spec: ZSpec[TestEnvironment, TestFailure[Nothing]] =
    suite("BuilderFailSpec")(
      // /////////////////////
      // CONFIGURING TWICE //
      // /////////////////////
      test("cannot set eviction listener twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.evictionListener((_,_,_) => ZIO.unit)
                               .evictionListener((_,_,_) => ZIO.unit)
                               .build()
                            )"""))(isLeft(equalTo("evictionListener can only be configured once")))
      },
      test("cannot set expireAfter twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(equalTo("expireAfter can only be configured once")))
      },
      test("cannot set expireAfterAccess twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfterAccess(1.millis)
                               .expireAfterAccess(1.millis)
                               .build()
                            )"""))(isLeft(equalTo("expireAfterAccess can only be configured once")))
      },
      test("cannot set expireAfterWrite twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfterWrite(1.millis)
                               .expireAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(equalTo("expireAfterWrite can only be configured once")))
      },
      test("cannot set initCapacity twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.initialCapacity(InitialCapacity(1))
                               .initialCapacity(InitialCapacity(1))
                               .build()
                            )"""))(isLeft(equalTo("initialCapacity can only be configured once")))
      },
      test("cannot set maximumSize twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.maximumSize(MaxSize(1))
                               .maximumSize(MaxSize(1))
                               .build()
                            )"""))(isLeft(equalTo("maximumSize can only be configured once")))
      },
      test("cannot set maximumWeight twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.maximumWeight(MaximumWeight(3))
                               .maximumWeight(MaximumWeight(3))
                               .build()
                            )"""))(isLeft(equalTo("maximumWeight can only be configured once")))
      },
      test("cannot set recordStats twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.recordStats()
                               .recordStats()
                               .build()
                            )"""))(isLeft(equalTo("recordStats can only be configured once")))
      },
      test("cannot set refreshAfterWrite twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.refreshAfterWrite(1.millis)
                               .refreshAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(equalTo("refreshAfterWrite can only be configured once")))
      },
      test("cannot enable scheduling twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.enableScheduling()
                               .enableScheduling()
                               .build()
                            )"""))(isLeft(equalTo("enableScheduling can only be configured once")))
      },
      test("cannot set removalListener twice") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.removalListener((_,_,_) => ZIO.unit)
                               .removalListener((_,_,_) => ZIO.unit)
                               .build()
                            )"""))(isLeft(equalTo("removalListener can only be configured once")))
      },
      // /////////////////////
      // INCOMPATIBILITIES //
      // /////////////////////
      test("max size + max weight are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.maximumSize(MaxSize(1))
                               .maximumWeight(MaximumWeight(3), (key, _) => key.length)
                               .build()
                            )"""))(isLeft(equalTo("setting maxWeight is incompatible with maxSize")))
      },
      test("max weight + max size are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.maximumWeight(MaximumWeight(3), (key, _) => key.length)
                               .maximumSize(MaxSize(1))
                               .build()
                            )"""))(isLeft(equalTo("setting maxSize is incompatible with maxWeight")))
      },
      test("expireAfter + expireAfterWrite are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfterWrite(1.millis)
                               .build()
                            )"""))(isLeft(equalTo("expireAfterWrite is incompatible with expireAfter")))
      },
      test("expireAfter + expireAfterAccess are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .expireAfterAccess(1.millis)
                               .build()
                            )"""))(isLeft(equalTo("expireAfterAccess is incompatible with expireAfter")))
      },
      test("expireAfter + expireAfterAccess are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]().flatMap(
                              _.expireAfterAccess(1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(equalTo("expireAfter is incompatible with expireAfterAccess")))
      },
      test("expireAfterWrite + expireAfter are incompatible") {
        assertM(typeCheck("""ZCaffeine[ZEnv, String, Int]()
                              .flatMap(
                              _.expireAfterWrite(1.millis)
                               .expireAfter((_, _, _) => 1.millis, (_, _, _, _) => 1.millis, (_, _, _, _) => 1.millis)
                               .build()
                            )"""))(isLeft(equalTo("expireAfter is incompatible with expireAfterWrite")))
      }
    )
}
