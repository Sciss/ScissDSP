/*
 * Complex.scala
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

object Complex {
  import math.Pi
  import Util.Pi2

  //   private val Pi  = math.Pi
  //   private val Pi2 = math.Pi * 2

  /** Converts a complex data vector from cartesian to polar format
    *
    * @param	src		Array with interleaved real/ imag data
    * @param	dest	target array which can be the same object as source (in place operation)
    * @param	srcOff	array offset, physical (complex offset << 1)
    * @param	length	complex length << 1
    */
  def rect2Polar(src: Array[Double], srcOff: Int, dest: Array[Double], destOff: Int, length: Int): Unit = {
    if ((src eq dest) && (srcOff < destOff)) {
      var i = srcOff + length
      var j = destOff + length
      while (i > srcOff) {
        i -= 1; val d1 = src(i)
        i -= 1; val d2 = src(i)
        j -= 1; dest(j) = math.atan2(d1, d2)
        j -= 1; dest(j) = math.sqrt(d1 * d1 + d2 * d2)
      }
    } else {
      var i = srcOff
      var j = destOff
      val stop = srcOff + length
      while (i < stop) {
        val d2 = src(i); i += 1
        val d1 = src(i); i += 1
        dest(j) = math.sqrt(d1 * d1 + d2 * d2); j += 1
        dest(j) = math.atan2(d1, d2); j += 1
      }
    }
  }

  /** Converts a complex data vector from polar to cartesian format
    *
    * @param	src		Array with interleaved amplitude/ phase data
    * @param	dest	target array which can be the same object as source (in place operation)
    * @param	srcOff	array offset, physical (complex offset << 1)
    * @param	length	complex length << 1
    */
  def polar2Rect(src: Array[Double], srcOff: Int, dest: Array[Double], destOff: Int, length: Int): Unit = {
    if ((src eq dest) && (srcOff < destOff)) {
      var i = srcOff + length
      var j = destOff + length
      while (i > srcOff) {
        i -=1; val d1 = src(i)
        i -=1; val d2 = src(i)
        j -=1; dest(j) = d2 * math.sin(d1)
        j -=1; dest(j) = d2 * math.cos(d1)
      }
    } else {
      var i = srcOff
      var j = destOff
      val stop = srcOff + length
      while (i < stop) {
        val d2 = src(i); i += 1
        val d1 = src(i); i += 1
        dest(j) = d2 * math.cos(d1); j += 1
        dest(j) = d2 * math.sin(d1); j += 1
      }
    }
  }

  /** Multiplies two complex data vectors
    *
    * @param	src1	  Array with interleaved real/ imag data
    * @param	srcOff1	array offset, physical (complex offset << 1)
    * @param	src2	  Array with interleaved real/ imag data
    * @param	srcOff2	array offset, physical (complex offset << 1)
    * @param	dest	  kann identisch mit *einem* src sein (in-place)
    * @param	length	complex length << 1
    */
  def complexMult(src1: Array[Double], srcOff1: Int, src2: Array[Double], srcOff2: Int,
                  dest: Array[Double], destOff: Int, length: Int): Unit = {
    if (((src1 eq dest) && (srcOff1 < destOff)) || ((src2 eq dest) && (srcOff2 < destOff))) {
      var i = srcOff1 + length
      var j = srcOff2 + length
      var k = destOff + length
      while (i > srcOff1) {
        i -= 1; val im1 = src1(i)
        i -= 1; val re1 = src1(i)
        j -= 1; val im2 = src2(j)
        j -= 1; val re2 = src2(j)
        k -= 1; dest(k) = im1 * re2 + re1 * im2
        k -= 1; dest(k) = re1 * re2 - im1 * im2
      }
    } else {
      var i = srcOff1
      var j = srcOff2
      var k = destOff
      val stop = srcOff1 + length
      while (i < stop) {
        val re1 = src1(i); i += 1
        val im1 = src1(i); i += 1
        val re2 = src2(j); j += 1
        val im2 = src2(j); j += 1
        dest(k) = re1 * re2 - im1 * im2; k += 1
        dest(k) = im1 * re2 + re1 * im2; k += 1
      }
    }
  }

  /** Unwraps 2-Pi clipped phase data of a complex polar format data vector
    *
    * @param	src		Array with interleaved amplitude/ phase data
    * @param	dest	target array which can be the same object as source (in place operation);
    *               in this case, however, destOff must not be less than srcOff!
    * @param	srcOff	array offset, physical (complex offset << 1)
    * @param	length	complex length << 1
    */
  def unwrapPhases(src: Array[Double], srcOff: Int, dest: Array[Double], destOff: Int, length: Int): Unit = {
    var i     = srcOff + 1
    var j     = destOff + 1
    var k     = 0
    val stop  = srcOff + length
    var d1    = 0.0
    var d3    = 0.0
    while (i < stop) {
      val d2 = src(i)
      if (d2 - d1 > Pi) {
        k -= 1
        d3 = k * Pi2
      } else if (d1 - d2 > Pi) {
        k += 1
        d3 = k * Pi2
      }
      dest(j) = d2 + d3
      d1 = d2

      i += 2
      j += 2
    }
  }

  /** Wraps 2-Pi clipped phase data of a complex polar format data vector
    * such that all phases in the target buffer are between -PI and +PI.
    *
    * @param	src		Array with interleaved amplitude/ phase data
    * @param	dest	target array which can be the same object as source (in place operation);
    *               in this case, however, destOff must not be less than srcOff!
    * @param	srcOff	array offset, physical (complex offset << 1)
    * @param	length	complex length << 1
    */
  def wrapPhases(src: Array[Double], srcOff: Int, dest: Array[Double], destOff: Int, length: Int): Unit = {
    var i = srcOff + 1
    var j = destOff + 1
    var k = 0
    val stop = srcOff + length
    var d3 = 0.0
    while (i < stop) {
      val d2 = src(i)
      while (d2 - d3 > Pi) {
        k += 1
        d3 = k * Pi2
      }
      while (d3 - d2 > Pi) {
        k -= 1
        d3 = k * Pi2
      }
      dest(j) = d2 - d3

      i += 2
      j += 2
    }
  }
}