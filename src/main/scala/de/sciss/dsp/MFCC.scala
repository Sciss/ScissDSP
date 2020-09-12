/*
 * MFCC.scala
 * (ScissDSP)
 *
 * Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.dsp

import de.sciss.serial.{ImmutableSerializer, DataOutput, DataInput}

import scala.language.implicitConversions

/** Mel-Frequency Cepstrum Coefficients.
  *
  * @author Ganesh Tiwari
  * @author Hanns Holger Rutz
 */
object MFCC {
  sealed trait ConfigLike {
    /** Sampling rate of the audio material, in Hertz. */
    def sampleRate: Double

    /** Number of cepstral coefficients to calculate. */
    def numCoeff: Int

    /** Lowest frequency in the Mel filter bank, in Hertz. */
    def minFreq: Float

    /** Highest frequency in the Mel filter bank, in Hertz. */
    def maxFreq: Float

    /** Number of filters in the Mel filter bank. */
    def numFilters: Int

    def fftSize: Int

    /** If `true`, uses a fixed high-frequency boosting filter. */
    def preEmphasis: Boolean

    /** Policy regarding parallelization of the calculation. */
    def threading: Threading
  }

  object Config {
    implicit def build(b: ConfigBuilder): Config = b.build

    def apply(): ConfigBuilder = new ConfigBuilderImpl

    private final val COOKIE  = 0x4D45  // "ME"

    implicit object Serializer extends ImmutableSerializer[Config] {
      def write(v: Config, out: DataOutput): Unit = {
        import v._
        out.writeShort(COOKIE)
        out.writeDouble(sampleRate)
        out.writeShort(numCoeff)
        out.writeFloat(minFreq)
        out.writeFloat(maxFreq)
        out.writeShort(numFilters)
        out.writeInt(fftSize)
        out.writeBoolean(preEmphasis)
        Threading.Serializer.write(threading, out)
      }

      def read(in: DataInput): Config = {
        val cookie = in.readShort()
        require(cookie == COOKIE, s"Unexpected cookie $cookie")
        val sampleRate      = in.readDouble()
        val numCoeff        = in.readShort()
        val minFreq         = in.readFloat()
        val maxFreq         = in.readFloat()
        val numFilters      = in.readShort()
        val fftSize         = in.readInt()
        val preEmphasis     = in.readBoolean()
        val threading       = Threading.Serializer.read(in)

        new ConfigImpl(sampleRate = sampleRate, numCoeff = numCoeff,
          minFreq = minFreq, maxFreq = maxFreq, numFilters = numFilters,
          fftSize = fftSize, preEmphasis = preEmphasis, threading = threading)
      }
    }
  }

  sealed trait Config extends ConfigLike

  object ConfigBuilder {
    def apply(config: Config): ConfigBuilder = {
      import config._
      val b = new ConfigBuilderImpl
      b.sampleRate      = sampleRate
      b.numCoeff        = numCoeff
      b.minFreq         = minFreq
      b.maxFreq         = maxFreq
      b.numFilters      = numFilters
      b.fftSize         = fftSize
      b.preEmphasis     = preEmphasis
      b.threading       = threading
      b
    }
  }

  sealed trait ConfigBuilder extends ConfigLike {
    var sampleRate      : Double
    var numCoeff        : Int
    var minFreq         : Float
    var maxFreq         : Float
    var numFilters      : Int
    var fftSize         : Int
    var preEmphasis     : Boolean
    var threading       : Threading

    def build: Config
  }

  private final class ConfigBuilderImpl extends ConfigBuilder {
    override def toString = s"MFCC.ConfigBuilder@${hashCode.toHexString}"

    // rather moderate defaults with 55 Hz, 8ms spacing, 4096 FFT...
    var sampleRate      = 44100.0
    var numCoeff = 13
    var minFreq         = 55f
    var maxFreq         = 20000f
    var numFilters      = 42  // Tiwari used 30 for speech, we use SuperCollider's 42 default
    var fftSize         = 1024
    var preEmphasis     = false
    var threading       = Threading.Multi: Threading

    def build: Config = ConfigImpl(sampleRate, numCoeff = numCoeff,
      minFreq = minFreq, maxFreq = maxFreq, numFilters = numFilters,
      fftSize = fftSize, preEmphasis = preEmphasis, threading = threading)
  }

  private final case class ConfigImpl(sampleRate: Double, numCoeff: Int, minFreq: Float, maxFreq: Float,
                                      numFilters: Int, fftSize: Int, preEmphasis: Boolean, threading: Threading)
    extends Config {

    override def toString = s"MFCC.Config@${hashCode.toHexString}"
  }

  private def melToFreq(mel : Double): Double =  700 * (math.pow(10, mel / 2595) - 1)
  private def freqToMel(freq: Double): Double = 2595 * math.log10(1 + freq / 700)

  def apply(config: Config = Config().build): MFCC = {
    val fs = config.sampleRate
    val config1 = if (config.maxFreq <= fs / 2) config
    else {
      val c = ConfigBuilder(config)
      c.maxFreq = (fs / 2).toFloat
      c.build
    }
    val fft = Fourier(config1.fftSize, config1.threading)
    new Impl(config1, fft)
  }

  private class Impl(val config: Config, fft: Fourier) extends MFCC {
    import config._

    private[this] final val preEmphasisAlpha  = 0.95   // amount of high-pass filtering

    private[this] val melFLow     = freqToMel(minFreq)
    private[this] val melFHigh    = freqToMel(maxFreq)
    private[this] val cBin        = fftBinIndices()
    private[this] val fftBuf      = new Array[Float](fftSize + 2)

    /** Calculates the MFCC for the given input frame.
      *
      * @param in the input samples to process
      * @return the feature vector with `config.numCoeff` elements.
      */
    def process(in: Array[Float], off: Int, len: Int): Array[Double] = {
      val frame = if (preEmphasis) applyPreEmphasis(in, off, len) else in

      val bin   = magnitudeSpectrum(frame, if (preEmphasis) 0 else off, len)
      val fBank = melFilter(bin)
      val f     = nonLinearTransformation(fBank)

      dct(f)
    }

    private def dct(y: Array[Double]): Array[Double] = {
      val c = new Array[Double](numCoeff)
      var n = 1
      val r = math.Pi / numFilters
      while (n <= numCoeff) {
        var i = 1
        val s = r * (n - 1)
        while (i <= numFilters) {
          c(n - 1) += y(i - 1) * math.cos(s * (i - 0.5))
          i += 1
        }
        n += 1
      }
      c
    }

    private def magnitudeSpectrum(frame: Array[Float], off: Int, len: Int): Array[Float] = {
      System.arraycopy(frame, off, fftBuf, 0, len)
      var i = len
      while (i < fftSize) {
        fftBuf(i) = 0f
        i += 1
      }

      fft.realForward(fftBuf)

      val mag = new Array[Float]((fftSize + 2)/2)
      i = 0
      var j = 0
      while (j <= fftSize) {
        val re = fftBuf(j); j += 1
        val im = fftBuf(j); j += 1
        mag(i) = math.sqrt(re * re + im * im).toFloat
        i += 1
      }
      mag
    }

    /*
     * Emphasizes high freq signal
     */
    private def applyPreEmphasis(in: Array[Float], off: Int, len: Int): Array[Float] = {
      val out = new Array[Float](len)
      if (len == 0) return out

      // apply pre-emphasis to each sample
      var n = 1
      var x1 = in(off)
      while (n < len) {
        val x0 = in(off + n)
        out(n) = (x0 - preEmphasisAlpha * x1).toFloat
        x1 = x0
        n += 1
      }
      out
    }

    private def fftBinIndices(): Array[Int] = {
      val r         = fftSize / sampleRate
      val fftSizeH  = fftSize / 2
      val cBin      = new Array[Int](numFilters + 2)
      var i = 0
      while (i < cBin.length) {
        val fc  = centerFreq(i)
        val j   = math.round(fc * r).toInt
        if (j > fftSizeH) throw new IllegalArgumentException(s"Frequency $fc exceed Nyquist")
        cBin(i) = j
        i += 1
      }
      cBin
    }

    /**
     * Performs mel filter operation
     *
     * @param bin
     *            magnitude spectrum (| |) squared of fft
     * @return mel filtered coefficients --> filter bank coefficients.
     */
    private def melFilter(bin: Array[Float]): Array[Double] = {
      val temp = new Array[Double](numFilters + 2)
      var k = 1
      while (k <= numFilters) {
        val p = cBin(k - 1)
        val q = cBin(k)
        val r = cBin(k + 1)
        var i = p
        val s0 = (i - p + 1) / (q - p + 1) // should this be floating point?
        var num = 0.0
        while (i <= q) {
          num += s0 * bin(i)
          i += 1
        }

        i = q + 1
        val s1 = 1 - ((i - q) / (r - q + 1)) // should this be floating point?
        while (i <= r) {
          num += s1 * bin(i)
          i += 1
        }

        temp(k) = num
        k += 1
      }
      val fBank = new Array[Double](numFilters)
      System.arraycopy(temp, 1, fBank, 0, numFilters)
      fBank
    }

    /**
     * performs nonlinear transformation
     *
     * @param fBank filter bank coefficients
     * @return f log of filter bac
     */
    private def nonLinearTransformation(fBank: Array[Double]): Array[Double] = {
      val sz      = fBank.length
      val f       = new Array[Double](sz)
      val FLOOR   = -50
      var i = 0
      while (i < sz) {
        f(i) = math.max(FLOOR, math.log(fBank(i)))
        i += 1
      }
      f
    }

    private def centerFreq(i: Int): Double = {
      val temp = melFLow + ((melFHigh - melFLow) / (numFilters + 1)) * i
      melToFreq(temp)
    }
  }
}
trait MFCC {
  /** Calculate the coefficients for a given frame of an input signal.
    *
    * @param in   the frame to process
    * @param off  the offset into the `in` signal. Typically zero.
    * @param len  the number of sample frames in the `in` signal.
    *             Typically the same as `config.fftSize`.
    *             Must not be greater than the fft size.
    */
  def process(in: Array[Float], off: Int, len: Int): Array[Double]

  def config: MFCC.Config
}