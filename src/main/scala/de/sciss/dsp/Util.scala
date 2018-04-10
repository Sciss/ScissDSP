/*
 * Util.scala
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

/** This is a helper object containing utility functions
  * for common math operations and constants
  */
object Util {
  /** 2 * Pi (Outline of the unit circle) */
	val Pi2  = math.Pi * 2

	/** logarithmus naturalis of 2 */
	val Ln2  = math.log( 2 )
	/** logarithmus naturalis of 10 */
	val Ln10 = math.log( 10 )

  /** Decibel-to-Linear conversion.
    *
    * @param  dB  volume in decibels
    * @return volume linear, such that
    *         dBToLinear( -6 ) returns c. 0.5
    */
  def dbamp(dB: Double): Double = math.exp(dB / 20 * Ln10)

  /** Linear-to-Decibel conversion
    *
    * @param  linear  volume linear
    * @return volume in decibals, such that
    *         linearToDB( 2.0 ) returns c. +6
    */
  def ampdb(linear: Double): Double = math.log10(linear) * 20

  /** Calculates the logarithm with base 2.
    *
    * @param  value the input value
    * @return the log2 of the value
    */
  def log2(value: Double): Double = math.log(value) / Ln2

  /** Calculates an integer that is a power
    * of two and is equal or greater than a given integer
    *
    * @param	value the minimum value to return
    * @return		an integer 2^n which is equal or greater than x
    */
  def nextPowerOfTwo(value: Int): Int = {
    var y = 1
    while (y < value) y <<= 1
    y
  }

   /** Calculates energy of a signal (sum(x*x)). */
   def calcEnergy(a: Array[Float], off: Int, length: Int): Double = {
     var energy = 0.0
     val stop   = off + length

     var i = off
     while (i < stop) {
       val f   = a(i)
       energy += f * f
       i      += 1
     }

     energy
   }
}