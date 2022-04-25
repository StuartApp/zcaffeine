package com.stuart.zcaffeine

import zio.prelude.Assertion._
import zio.prelude._

object types {

  /**
   * A type representing the initial capacity for a ZCaffeine cache, must be positive.
   */
  type InitialCapacity = InitialCapacity.Type

  object InitialCapacity extends Subtype[Int] {

    override def assertion =
      assert(greaterThanOrEqualTo(0))
  }

  /**
   * A type representing the maximum size for a ZCaffeine cache, must be positive.
   */
  type MaximumSize = MaxSize.Type

  object MaxSize extends Subtype[Long] {

    override def assertion =
      assert(greaterThanOrEqualTo(0L))
  }

  /**
   * A type representing the maximum weight for a ZCaffeine cache, must be positive.
   */
  type MaximumWeight = MaximumWeight.Type

  object MaximumWeight extends Subtype[Long] {

    override def assertion =
      assert(greaterThanOrEqualTo(0L))
  }
}
