package com.stuart

import java.util.concurrent.CompletableFuture
import scala.jdk.FutureConverters._

import zio._

package object zcaffeine {

  private[zcaffeine] def zioToCompletableFuture[R, E <: Throwable, A](runtime: Runtime[R])(
      action: ZIO[R, E, A]
  ): CompletableFuture[A] =
    runtime.unsafeRunToFuture(action).future.asJava.toCompletableFuture
}
