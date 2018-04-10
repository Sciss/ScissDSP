/*
 * Threading.scala
 * (ScissDSP)
 *
 * Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.dsp

import edu.emory.mathcs.utils.ConcurrencyUtils
import de.sciss.serial.{DataOutput, DataInput, ImmutableSerializer}
import scala.annotation.switch

object Threading {
  private final val COOKIE    = 0x5468
  private final val ID_MULTI  = 0
  private final val ID_SINGLE = 1
  private final val ID_CUSTOM = 2

  implicit object Serializer extends ImmutableSerializer[Threading] {
    // don't worry about the exhaustiveness warning. seems to be SI-7298, to be fixed in Scala 2.10.2
    def write(v: Threading, out: DataOutput): Unit = {
      out.writeShort(COOKIE)
      v match  {
        case Multi      => out.writeByte(ID_MULTI)
        case Single     => out.writeByte(ID_SINGLE)
        case Custom(n)  => out.writeByte(ID_CUSTOM); out.writeShort(n)
      }
    }

    def read(in: DataInput): Threading = {
      val cookie = in.readShort()
      require(cookie == COOKIE, s"Unexpected cookie $cookie")
      (in.readByte(): @switch) match {
        case ID_MULTI   => Multi
        case ID_SINGLE  => Single
        case ID_CUSTOM  => val n = in.readShort(); Custom(n)
      }
    }
  }

  /** Use the optimal number of threads (equal to the number of cores reported for the CPU). */
  case object Multi extends Threading {
    private[dsp] def setJTransforms(): Unit =
      ConcurrencyUtils.setNumberOfThreads(ConcurrencyUtils.getNumberOfProcessors)
  }

  /** Use only single threaded processing. */
  case object Single extends Threading {
    private[dsp] def setJTransforms(): Unit =
      ConcurrencyUtils.setNumberOfThreads(1)
  }

  /** Use a custom number of threads. */
  final case class Custom(numThreads: Int) extends Threading {
    private[dsp] def setJTransforms(): Unit =
      ConcurrencyUtils.setNumberOfThreads(numThreads)
  }
}

sealed trait Threading {
  private[dsp] def setJTransforms(): Unit
}
