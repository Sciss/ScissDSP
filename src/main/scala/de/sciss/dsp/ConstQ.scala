/*
 * ConstQ.scala
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

import de.sciss.serial.{ConstFormat, DataInput, DataOutput}

import scala.language.implicitConversions

object ConstQ {

  sealed trait ConfigLike {
    /** Sampling rate of the audio material, in Hertz. */
    def sampleRate: Double

    /** Lowest frequency in the logarithmic spectral, in Hertz. */
    def minFreq: Float

    /** Highest frequency in the logarithmic spectral, in Hertz. */
    def maxFreq: Float

    /** Maximum temporal resolution in milliseconds. This resolution is achieved for high frequencies. */
    def maxTimeRes: Float

    /** Maximum size of FFTs calculated. This constraints the actual bandwidth of the minimum frequency
      * spectral resolution. Even if the bands per octave and minimum frequency would suggest a higher
      * FFT size for low frequencies, this setting prevents these, and therefore constraints processing
      * time.
      */
    def maxFFTSize: Int

    /** Number of frequency bands resolved per octave. */
    def bandsPerOct: Int

    /** Policy regarding parallelization of the calculation. */
    def threading: Threading
  }

  object Config {
    implicit def build(b: ConfigBuilder): Config = b.build

    def apply(): ConfigBuilder = new ConfigBuilderImpl

    private final val COOKIE  = 0x4351

    implicit object Serializer extends ConstFormat[Config] {
      def write(v: Config, out: DataOutput): Unit = {
        import v._
        out.writeShort(COOKIE)
        out.writeDouble(sampleRate)
        out.writeFloat(minFreq)
        out.writeFloat(maxFreq)
        out.writeFloat(maxTimeRes)
        out.writeShort(maxFFTSize)
        out.writeShort(bandsPerOct)
        Threading.Serializer.write(threading, out)
      }

      def read(in: DataInput): Config = {
        val cookie = in.readShort()
        require(cookie == COOKIE, s"Unexpected cookie $cookie")
        val sampleRate  = in.readDouble()
        val minFreq     = in.readFloat()
        val maxFreq     = in.readFloat()
        val maxTimeRes  = in.readFloat()
        val maxFFTSize  = in.readShort()
        val bandsPerOct = in.readShort()
        val threading   = Threading.Serializer.read(in)
        new ConfigImpl(sampleRate = sampleRate, minFreq = minFreq, maxFreq = maxFreq, maxTimeRes = maxTimeRes,
                       maxFFTSize = maxFFTSize, bandsPerOct = bandsPerOct, threading = threading)
      }
    }
  }

  sealed trait Config extends ConfigLike

  object ConfigBuilder {
    def apply(config: Config): ConfigBuilder = {
      import config._
      val b = new ConfigBuilderImpl
      b.sampleRate  = sampleRate
      b.minFreq     = minFreq
      b.maxFreq     = maxFreq
      b.maxTimeRes  = maxTimeRes
      b.maxFFTSize  = maxFFTSize
      b.bandsPerOct = bandsPerOct
      b.threading   = threading
      b
    }
  }

  sealed trait ConfigBuilder extends ConfigLike {
    var sampleRate: Double
    var minFreq: Float
    var maxFreq: Float
    var maxTimeRes: Float
    var maxFFTSize: Int
    var bandsPerOct: Int
    var threading: Threading

    def build: Config
  }

  private final class ConfigBuilderImpl extends ConfigBuilder {
    override def toString = s"ConstQ.ConfigBuilder@${hashCode.toHexString}"

    // rather moderate defaults with 55 Hz, 8ms spacing, 4096 FFT...
    var sampleRate    = 44100.0
    var minFreq       = 55f
    var maxFreq       = 20000f
    var maxTimeRes    = 8f
    var bandsPerOct   = 24
    var maxFFTSize    = 4096
    var threading     = Threading.Multi: Threading

    def build: Config = ConfigImpl(sampleRate, minFreq, maxFreq, maxTimeRes, maxFFTSize, bandsPerOct, threading)
  }

  private final case class ConfigImpl(sampleRate: Double, minFreq: Float, maxFreq: Float, maxTimeRes: Float,
                                      maxFFTSize: Int, bandsPerOct: Int, threading: Threading)
    extends Config {

    override def toString = s"ConstQ.Config@${hashCode.toHexString}"
  }

  /** Calculates the number of kernels resulting from a given setting. */
  def getNumKernels(bandsPerOct: Int, maxFreq: Float, minFreq: Float): Int =
    math.ceil(bandsPerOct * Util.log2(maxFreq / minFreq)).toInt

  def apply(config: Config = Config().build): ConstQ = createInstance(config)

  private final class Impl(val config: Config, kernels: Array[Kernel], val fft: Fourier, val fftBuffer: Array[Float])
    extends ConstQ {
    //      private Kernel[]	kernels;
    //     	private int			numKernels;
    //     	private int			fftSize;
    //     	private float[]		fftBuffer;
    //         private Fourier     fft;

    val numKernels  = kernels.length
    val fftSize     = fft.size

    override def toString = s"ConstQ@${hashCode.toHexString}"

    def getFrequency(kernel: Int): Float = kernels(kernel).freq

    def transform(input: Array[Float], inLen: Int, out0: Array[Float], inOff: Int, outOff: Int): Array[Float] = {
      val output = if (out0 == null) new Array[Float](numKernels) else out0

      //		gain *= TENBYLOG10;
      val off = fftSize >> 1
      val num = math.min(fftSize - off, inLen)

      //System.out.println( "inOff " + inOff + "; num  " + num + " ; inOff + num " + (inOff + num) + "; input.length " + input.length + "; fftBuffer.length " + fftBuffer.length );

      System.arraycopy(input, inOff, fftBuffer, off, num)
      var i = off + num
      while (i < fftSize) {
        fftBuffer(i) = 0f
        i += 1
      }
      val num2 = math.min(fftSize - num, inLen - num)
      System.arraycopy(input, inOff + num, fftBuffer, 0, num2)
      i = num2
      while (i < off) {
        fftBuffer(i) = 0f
        i += 1
      }

      // XXX evtl., wenn inpWin weggelassen werden kann,
      // optimierte overlap-add fft
      //   		Fourier.realTransform( fftBuffer, fftSize, Fourier.FORWARD );
      fft.realForward(fftBuffer)

      convolve(output, outOff)
    }

    def convolve(out0: Array[Float], outOff0: Int): Array[Float] = {
      val output = if (out0 == null) new Array[Float](numKernels) else out0

      var k = 0; var outOff = outOff0; while (k < numKernels) {
        val kern = kernels(k)
        val data = kern.data
        var f1 = 0f
        var f2 = 0f
        var i = kern.offset; var j = 0; while (j < data.length) {
          // complex mult: a * b =
          // (re(a)re(b)-im(a)im(b))+i(re(a)im(b)+im(a)re(b))
          // ; since we left out the conjugation of the kernel(!!)
          // this becomes (assume a is input and b is kernel):
          // (re(a)re(b)+im(a)im(b))+i(im(a)re(b)-re(a)im(b))
          // ; in fact this conjugation is unimportant for the
          // calculation of the magnitudes...
          val re1 = fftBuffer(i)
          val im1 = fftBuffer(i + 1)
          val re2 = data(j)
          val im2 = data(j + 1)
          f1 += re1 * re2 - im1 * im2
          f2 += re1 * im2 + im1 * re2
          i += 2; j += 2
        }
        //			cBuf[ k ] = ;  // squared magnitude
        //			f1 = (float) ((Math.log( f1 * f1 + f2 * f2 ) + LNKORR_ADD) * gain);
        //f1 = Math.max( -160, f1 + 90 );

        // since we use constQ to decimate spectra, we actually
        // are going to store a "mean square" of the amplitudes
        // so at the end of the decimation we'll have one square root,
        // or even easier, when calculating the logarithm, the
        // multiplicator of the log just has to be divided by two.
        // hence we don't calc abs( f ) but abs( f )^2 !

        output(outOff) = f1 * f1 + f2 * f2

        k += 1; outOff += 1
      }

      output
    }
  }

  private def createInstance(config0: Config): Impl = {
    val fs = config0.sampleRate
    val config = if (config0.maxFreq <= fs / 2) config0
    else {
      val c = ConfigBuilder(config0)
      c.maxFreq = (fs / 2).toFloat
      c.build
    }

    val q           = (1 / (math.pow(2, 1.0 / config.bandsPerOct) - 1)).toFloat
    val numKernels  = getNumKernels(config.bandsPerOct, config.maxFreq, config.minFreq)
    val kernels     = new Array[Kernel](numKernels)
    //		cqKernels	= new float[ cqKernelNum ][];
    //		cqKernelOffs= new int[ cqKernelNum ];
    val maxKernLen  = q * config.sampleRate / config.minFreq

    //		System.out.println( "ceil " + ((int) Math.ceil( maxKernLen )) + "; nextPower " + (Util$.nextPowerOfTwo( (int) Math.ceil( maxKernLen )))) ;

    val fftSize     = math.min(config.maxFFTSize, Util.nextPowerOfTwo(math.ceil(maxKernLen).toInt))
    //		LNKORR_ADD	= -2 * Math.log( fftSize );
    val fftSizeC    = fftSize << 1
    val fftBuf      = new Array[Float](fftSizeC)
    val fft         = Fourier(fftSize, config.threading)

    //		thresh		= 0.0054f / fftLen; // for Hamming window
    // weird observation : lowering the threshold will _increase_ the
    // spectral noise, not improve analysis! so the truncating of the
    // kernel is essential for a clean analysis (why??). the 0.0054
    // seems to be a good choice, so don't touch it.
    //		threshSqr	= 2.916e-05f / (fftSize * fftSize); // for Hamming window (squared!)
    //		threshSqr	= 2.916e-05f / fftSize; // for Hamming window (squared!)
    // (0.0054 * 3).squared
    val threshSqr = 2.6244e-4f / (fftSize * fftSize) // for Hamming window (squared!)
    // tempKernel= zeros(fftLen, 1);
    //		sparKernel= [];

    //		System.out.println( "cqKernelNum = " + cqKernelNum + "; maxKernLen = " + maxKernLen + "; fftSize = " + fftSize + "; threshSqr " + threshSqr );

    var k = 0
    while (k < numKernels) {
      val theorKernLen  = maxKernLen * math.pow(2, (-k).toDouble / config.bandsPerOct) // toFloat
      val kernelLen     = math.min(fftSize, math.ceil(theorKernLen).toInt)
      val kernelLenE    = kernelLen & ~1
      val win           = Window.Hamming.create(kernelLen)

      val centerFreq    = config.minFreq * math.pow(2, k.toDouble / config.bandsPerOct)
      val centerFreqN   = centerFreq * -Util.Pi2 / fs

      //			weight		= 1.0f / (kernelLen * fftSize);
      // this is a good approximation in the kernel len truncation case
      // (tested with pink noise, where it will appear pretty much
      // with the same brightness over all frequencies)
      val weight        = 6 / ((theorKernLen + kernelLen) * fftSize)
      //			weight = 2 / ((theorKernLen + kernelLen) * Math.sqrt( fftSize ));

      var m = kernelLenE
      val n = fftSizeC - kernelLenE
      while (m < n) {
        fftBuf(m) = 0f
        m += 1
      }

      // note that we calculate the complex conjugation of
      // the temporalKernal and reverse its time, so the resulting
      // FFT can be immediately used for the convolution and does not
      // need to be conjugated; this is due to the Fourier property
      // h*(-x) <-> H*(f). time reversal is accomplished by
      // having iteration variable j run....

      // note: in the old FFT algorithm, there was a mistake, whereby complex input
      // data was already time reversed in the transform. this is corrected now.

      //			for( int i = kernelLen - 1, j = fftSizeC - kernelLenE; i >= 0; i-- ) { ... }
      //			for( int i = 0, j = 0; /* fftSizeC - kernelLenE; */ i < kernelLen; i++ ) { ... }
      //         var i = 0; var j = fftSizeC - kernelLenE; while( i < kernelLen ) {
      var i = kernelLen - 1
      var j = fftSizeC - kernelLenE
      while (i >= 0) {
        // complex exponential of a purely imaginary number
        // is cos( imag( n )) + i sin( imag( n ))
        val d1    = centerFreqN * i
        //				d1			= centerFreqN * (i - kernelLenE);
        val cos   = math.cos(d1)
        val sin   = math.sin(d1) // Math.sqrt( 1 - cos*cos );
        val d2    = win(i) * weight
        fftBuf(j) = (d2 * cos).toFloat; j += 1
        //				fftBuf[ j++ ] = -f1 * sin;  // conj!
        fftBuf(j) = (d2 * sin).toFloat; j += 1 // NORM!
        if (j == fftSizeC) j = 0
        //            i += 1 }
        i -= 1
      }

      // XXX to be honest, i don't get the point
      // of calculating the fft here, since we
      // have an analytic description of the kernel
      // function, it should be possible to calculate
      // the spectral coefficients directly
      // (the fft of a hamming is a gaussian,
      // isn't it?)

      //			Fourier.complexTransform( fftBuf, fftSize, Fourier.FORWARD )

      //println( fftBuf.mkString( "[ ", ", ", " ]" ))
      fft.complexForward(fftBuf)
      //val _out  = fftBuf.clone()
      //Complex.rect2Polar( _out, 0, _out, 0, _out.length )
      //val _test = _out.zipWithIndex.collect { case (f, i) if i % 2 == 0 => f }
      //println( _test.mkString( "[ ", ", ", " ]" ))

      // with a "high" threshold like 0.0054, the
      // point is _not_ to create a sparse matrix by
      // gating the values. in fact we can locate
      // the kernel spectrally, so all we need to do
      // is to find the lower and upper frequency
      // of the transformed kernel! that makes things
      // a lot easier anyway since we don't need
      // to employ a special sparse matrix library.
      val specStart = {
        var i = 0; var break = false; while (!break && i <= fftSize) {
          val f1      = fftBuf(i)
          val f2      = fftBuf(i + 1)
          val magSqr  = f1 * f1 + f2 * f2
          if (magSqr > threshSqr) break = true else i += 2
        }
        i
      }
      // final matrix product:
      // input chunk (fft'ed) is a row vector with n = fftSize
      // kernel is a matrix mxn with m = fftSize, n = numKernels
      // result is a row vector with = numKernels
      // ; note that since the input is real and hence we
      // calculate only the positive frequencies (up to pi),
      // we might need to mirror the input spectrum to the
      // negative frequencies. however it can be observed that
      // for practically all kernels their frequency bounds
      // lie in the positive side of the spectrum (only the
      // high frequencies near nyquist blur across the pi boundary,
      // and we will cut the overlap off by limiting specStop
      // to fftSize instead of fftSize<<1 ...).

      val specStop = {
        var i = specStart; var break = false; while (!break && i <= fftSize) {
          val f1      = fftBuf(i)
          val f2      = fftBuf(i + 1)
          val magSqr  = f1 * f1 + f2 * f2
          if (magSqr <= threshSqr) break = true else i += 2
        }
        i
      }

      //System.out.println( "Kernel k : specStart " + specStart + "; specStop " + specStop + "; centerFreq " + centerFreq );
      kernels(k) = new Kernel(specStart, new Array[Float](specStop - specStart), centerFreq.toFloat)
      System.arraycopy(fftBuf, specStart, kernels(k).data, 0, specStop - specStart)
      k += 1
    }

    new Impl(config, kernels, fft, fftBuf)
  }

  private final class Kernel(val offset: Int, val data: Array[Float], val freq: Float)

}

trait ConstQ {
  def config: ConstQ.Config

  /** The number of kernels is the total number of frequency bands calculated. */
  def numKernels: Int

  /** The actual maximum FFT size used. */
  def fftSize: Int

  /** The buffer used to perform FFTs. */
  def fftBuffer: Array[Float]

  /** Queries a kernel frequency in Hertz.
    *
    * @param   kernel   the kernel index, from `0` up to and including `numKernels-1`
    */
  def getFrequency(kernel: Int): Float

  /** Transforms a time domain input signal to obtain the constant Q spectral coefficients.
    *
    * @param input   the time domain signal, which will be read from `inOff` for `inLen` samples
    * @param inOff   the offset into the input array
    * @param inLen   the number of samples to take from the input array
    * @param output  the target kernel buffer (size should be at leat `outOff` + `numKernels`. if `null` a new buffer is allocated
    * @param outOff  the offset into the output buffer
    * @return  the output buffer which is useful when the argument was `null`
    */
  def transform(input: Array[Float], inLen: Int, output: Array[Float], inOff: Int = 0, outOff: Int = 0): Array[Float]

  /** Assumes that the input was already successfully transformed
    * into the Fourier domain (namely into fftBuf as returned by
    * getFFTBuffer()). From this FFT, the method calculates the
    * convolutions with the filter kernels, returning the magnitudes
    * of the filter outputs.
    *
    * @param output  the target kernel buffer (size should be at least
    *                `outOff` + `numKernels`. if `null` a new buffer is allocated
    * @param outOff  the offset into the output buffer
    * @return  the output buffer which is useful when the argument was `null`
    */
  def convolve(output: Array[Float], outOff: Int = 0): Array[Float]
}