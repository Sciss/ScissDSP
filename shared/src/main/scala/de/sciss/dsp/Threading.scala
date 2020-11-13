/*
 * Threading.scala
 * (ScissDSP)
 *
 * Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Affero General Public License v3+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.dsp

import de.sciss.serial.{ConstFormat, DataInput, DataOutput}
import de.sciss.transform4s.utils.ConcurrencyUtils

import scala.annotation.switch

object Threading {
  private final val COOKIE    = 0x5468
  private final val ID_MULTI  = 0
  private final val ID_SINGLE = 1
  private final val ID_CUSTOM = 2

  implicit object format extends ConstFormat[Threading] {
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

  /** Use the optimal number of threads (equal to the number of cores reported for the CPU).
    * This is ok for Scala.js, as reported number of processors is one.
    */
  case object Multi extends Threading {
    private[dsp] def confiureTransform4s(): Unit =
      ConcurrencyUtils.numThreads = ConcurrencyUtils.numProcessors
  }

  /** Use only single threaded processing. */
  case object Single extends Threading {
    private[dsp] def confiureTransform4s(): Unit =
      ConcurrencyUtils.numThreads = 1
  }

  /** Use a custom number of threads. */
  final case class Custom(numThreads: Int) extends Threading {
    private[dsp] def confiureTransform4s(): Unit =
      ConcurrencyUtils.numThreads = numThreads
  }
}

sealed trait Threading {
  private[dsp] def confiureTransform4s(): Unit
}
