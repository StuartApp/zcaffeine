package com.stuart.zcaffeine

import zio._
import zio.test.Assertion._
import zio.test.{ TestEnvironment, _ }

object LoadingCacheSpec extends ZIOSpecDefault {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("LoadingCacheSpec")(
      test("get/getAll/refresh/refreshAll") {
        for {
          zcaffeine <- ZCaffeine[TestEnvironment, String, String]()
          cache <- zcaffeine.build(
                     loadOne = key => ZIO.succeed(s"loadOne: $key"),
                     loadAll = Some(keys => ZIO.succeed(keys.map(key => key -> s"loadAll: $key").toMap)),
                     reloadOne = Some((key, value) => ZIO.succeed(s"""reloadOne: $key (old: "$value")"""))
                   )
          loaded       <- cache.get("foo")
          allLoaded    <- cache.getAll(Set("foo1", "foo12"))
          refreshed    <- cache.refresh("foo")
          allRefreshed <- cache.refreshAll(Set("foo1", "foo12"))
        } yield assert(loaded)(equalTo("loadOne: foo")) &&
          assert(allLoaded)(equalTo(Map("foo1" -> "loadAll: foo1", "foo12" -> "loadAll: foo12"))) &&
          assert(refreshed)(equalTo("""reloadOne: foo (old: "loadOne: foo")""")) &&
          assert(allRefreshed)(
            equalTo(
              Map(
                "foo1"  -> """reloadOne: foo1 (old: "loadAll: foo1")""",
                "foo12" -> """reloadOne: foo12 (old: "loadAll: foo12")"""
              )
            )
          )
      }
    )
}
