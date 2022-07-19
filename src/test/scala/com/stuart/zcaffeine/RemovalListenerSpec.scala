package com.stuart.zcaffeine

import zio._
import zio.test.Assertion._
import zio.test.{ TestEnvironment, _ }

object RemovalListenerSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment, Any] =
    suite("RemovalListenerSpec")(
      test("removalListener notifies on value evictions") {
        for {
          evictedKeys <- Ref.make(List.empty[String])
          zcaffeine   <- ZCaffeine[TestEnvironment, String, Int]()
          cache <- zcaffeine
                     .enableScheduling()
                     .removalListener((key, _, _) => evictedKeys.update(key :: _))
                     .build()
          _ <- cache.put("foo", ZIO.succeed(1))
          _ <- cache.put("bar", ZIO.succeed(3))
          _ <- cache.invalidateAll
          _ <- TestClock.adjust(1.second) // Waits for the scheduler to run maintenance
          keys <- evictedKeys.get
        } yield assert(keys)(hasSameElements(List("foo", "bar")))
      }
    )
}
