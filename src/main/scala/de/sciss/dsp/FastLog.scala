/*
 * FastLog.scala
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

object FastLog {
  /** Create a new logarithm calculation instance. This will
    * hold the pre-calculated log values for a given base
    * and a table size depending on a given mantissa quantization.
    *
    * @param	base	the logarithm base (e.g. 2 for log duals, 10 for
    *               decibels calculations, Math.E for natural log)
    * @param	q		the quantization, the number of bits to remove
    *              from the mantissa. for q = 11, the table storage
    *              requires 32 KB.
    */
  def apply(base: Double, q: Int): FastLog = new Impl(base, q)

  private final class Impl(base0: Double, q0: Int)
    extends FastLog {

    override def toString = s"FastLog(base=$base, q=$q)"

    override def equals(that: Any) = that != null && that.isInstanceOf[FastLog] && {
      val f = that.asInstanceOf[FastLog]
      f.base == base && f.q == q
    }

    def base: Double = base0

    def q: Int = q0

    private val qM1   = q0 - 1
    private val korr  = (Util.Ln2 / math.log(base0)).toFloat

    private val data = {
      val tabSize = 1 << (24 - q0)
      val arr     = new Array[Float](tabSize)
      var i = 0; while (i < tabSize) {
        // note: the -150 is to avoid this addition in the calculation
        // of the exponent (see the floatToRawIntBits doc).
        arr(i) = (Util.log2(i << q0) - 150).toFloat
        i += 1
      }
      arr
    }

    def calc(arg: Float): Float = {
      //		final int raw	= Float.floatToRawIntBits( x );
      val raw       = java.lang.Float.floatToIntBits(arg)
      val exp       = (raw >> 23) & 0xFF
      val mantissa  = raw & 0x7FFFFF

      (exp + data(if (exp == 0) mantissa >> qM1 else (mantissa | 0x800000) >> q0)) * korr
    }
  }
}

/** Implementation of the ICSILog algorithm
  * as described in O. Vinyals, G. Friedland, N. Mirghafori
  * "Revisiting a basic function on current CPUs: A fast logarithm implementation
  * with adjustable accuracy" (2007).
  *
  * @see		java.lang.Float#floatToRawIntBits( float )
  */
sealed trait FastLog {
  def base: Double

  def q: Int

  /** Calculate the logarithm to the base given in the constructor.
    *
    * @param	arg	the argument. must be positive!
    * @return		log( x )
    */
  def calc(arg: Float): Float
}
