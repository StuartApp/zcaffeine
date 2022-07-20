package com.stuart

import java.util.concurrent.CompletableFuture
import scala.jdk.FutureConverters.*

import zio.*
import zio.Unsafe.*

package object zcaffeine {

  private[zcaffeine] def zioToCompletableFuture[R, E <: Throwable, A](runtime: Runtime[R])(
      action: ZIO[R, E, A]
  ): CompletableFuture[A] =
    unsafe(u ?=> runtime.unsafe.runToFuture(action).asJava.toCompletableFuture)

  private[zcaffeine] def zioToValue[R, E <: Throwable, A](runtime: Runtime[R])(action: ZIO[R, E, A]): A =
    unsafe(u ?=> runtime.unsafe.run(action).getOrThrowFiberFailure())

}
