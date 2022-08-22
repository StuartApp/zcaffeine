package com.stuart.zcaffeine

import zio._
import zio.test.Assertion._
import zio.test._

object CacheSpec extends DefaultRunnableSpec {

  override def spec =
    suite("CacheSpec")(
      testM("get/getIfPresent") {
        for {
          zcaffeine <- ZCaffeine[ZEnv, String, Int]()
          cache     <- zcaffeine.build()
          missing   <- cache.getIfPresent("key")
          created   <- cache.get("key")(key => ZIO.succeed(key.length))
          unchanged <- cache.get("key")(key => ZIO.succeed(key.length + 1))
          present   <- cache.getIfPresent("key")
        } yield assert(missing)(isNone) &&
          assert(created)(equalTo(3)) &&
          assert(created)(equalTo(unchanged)) &&
          assert(present)(isSome(equalTo(created)))
      },
      testM("getAll") {
        for {
          zcaffeine <- ZCaffeine[ZEnv, String, Int]()
          cache     <- zcaffeine.build()
          missing1  <- cache.getIfPresent("key")
          missing2  <- cache.getIfPresent("key2")
          created   <- cache.getAll("key", "key2")(keys => ZIO.succeed(keys.map(key => key -> key.length).toMap))
          unchanged <-
            cache.getAll(Set("key", "key2"))(keys => ZIO.succeed(keys.map(key => key -> (key.length + 1)).toMap))
        } yield assert(missing1)(isNone) &&
          assert(missing2)(isNone) &&
          assert(created)(equalTo(Map("key" -> 3, "key2" -> 4))) &&
          assert(created)(equalTo(unchanged))
      },
      testM("put") {
        for {
          zcaffeine <- ZCaffeine[ZEnv, String, Int]()
          cache     <- zcaffeine.build()
          missing   <- cache.getIfPresent("key")
          _         <- cache.put("key", ZIO.succeed(1))
          created   <- cache.getIfPresent("key")
          _         <- cache.put("key", ZIO.succeed(3))
          updated   <- cache.getIfPresent("key")
        } yield assert(missing)(isNone) &&
          assert(created)(isSome(equalTo(1))) &&
          assert(updated)(isSome(equalTo(3)))
      },
      testM("invalidate/invalidateAll") {
        for {
          zcaffeine   <- ZCaffeine[ZEnv, String, Int]()
          cache       <- zcaffeine.build()
          _           <- cache.getAll("key", "key2")(keys => ZIO.succeed(keys.map(key => key -> key.length).toMap))
          _           <- cache.invalidate("key")
          keyMissing  <- cache.getIfPresent("key")
          key2Present <- cache.getIfPresent("key2")
          _           <- cache.invalidateAll
          key2Missing <- cache.getIfPresent("key2")
          _           <- cache.getAll(Set("key", "key2"))(keys => ZIO.succeed(keys.map(key => key -> key.length).toMap))
          _           <- cache.invalidateAll(Set("key", "key2"))
          keyMissingAgain  <- cache.getIfPresent("key")
          key2MissingAgain <- cache.getIfPresent("key2")
        } yield assert(keyMissing)(isNone) &&
          assert(key2Present)(isSome(equalTo(4))) &&
          assert(key2Missing)(isNone) &&
          assert(keyMissingAgain)(isNone) &&
          assert(key2MissingAgain)(isNone)
      }
    )
}
