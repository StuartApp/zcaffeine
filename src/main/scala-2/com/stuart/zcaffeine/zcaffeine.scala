package com.stuart

import java.util.concurrent.CompletableFuture
import scala.jdk.FutureConverters._

import zio.Unsafe._
import zio._

package object zcaffeine {

  private[zcaffeine] def zioToCompletableFuture[R, E <: Throwable, A](runtime: Runtime[R])(
      action: ZIO[R, E, A]
  ): CompletableFuture[A] =
    unsafe { implicit u =>
      runtime.unsafe.runToFuture(action).asJava.toCompletableFuture
    }

  private[zcaffeine] def zioToValue[R, E <: Throwable, A](runtime: Runtime[R])(action: ZIO[R, E, A]): A =
    unsafe { implicit u =>
      runtime.unsafe.run(action).getOrThrowFiberFailure()
    }

}
