/*
 *  ConstQ.java
 *  (ScissDSP)
 *
 *  Copyright (c) 2001-2010 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *  	27-Jan-10	created from EisK, stripping prefs dependancies
 */

package de.sciss.dsp;

/**
 *	@version	0.71, 27-Jan-10
 *	@author		Hanns Holger Rutz
 */
public class ConstQ
{
	private Kernel[]	kernels;
	private int			numKernels;
	private int			fftSize;
	private float[]		fftBuf;

	// rather moderate defaults with 55 Hz, 8ms spacing, 4096 FFT...
	private float		minFreq		= 55f;	// 27.5f;
	private float		maxFreq		= 20000f;
	private float		maxTimeRes	= 8f;	// 5f;
	private int			bandsPerOct	= 24;	// 24
	private int			maxFFTSize	= 4096;	// 8192;
	
	private double		fs;
	
//	private double		LNKORR_ADD; // = -2 * Math.log( fftSize );

//	private static final double	TENBYLOG10				= 10 / MathUtil.LN10;

	public ConstQ() { /* empty */ }
	
	public int getNumKernels()
	{
		return numKernels;
	}

	public int getFFTSize()
	{
		return fftSize;
	}
	
	public float[] getFFTBuffer()
	{
		return fftBuf;
	}

	public void setSampleRate( double fs )
	{
		this.fs = fs;
	}
	
	public double getSampleRate()
	{
		return fs;
	}
	
	public void setMinFreq( float minFreq )
	{
		this.minFreq	= minFreq;
	}
	
	public float getMinFreq()
	{
		return minFreq;
	}
	
	public void setMaxFreq( float maxFreq )
	{
		this.maxFreq = maxFreq;
	}
	
	public float getMaxFreq()
	{
		return maxFreq;
	}
	
	public void setMaxTimeRes( float maxTimeRes )
	{
		this.maxTimeRes	= maxTimeRes;
	}
	
	public float getMaxTimeRes()
	{
		return maxTimeRes;
	}
	
	public void setMaxFFTSize( int maxFFTSize )
	{
		this.maxFFTSize	= maxFFTSize;
	}
	
	public int getMaxFFTSize()
	{
		return maxFFTSize;
	}
	
	public void setBandsPerOct( int bpo )
	{
		this.bandsPerOct = bpo;
	}
	
	public int getBandsPerOct()
	{
		return bandsPerOct;
	}
	
	public float getFrequency( int kernel )
	{
		return kernels[ kernel ].freq;
	}
	
	public static int getNumKernels( int bandsPerOct, float maxFreq, float minFreq )
	{
		return (int) Math.ceil( bandsPerOct * MathUtil.log2( maxFreq / minFreq ));
	}
	
	public void createKernels()
	{
		final float		threshSqr, q;
		final double	maxKernLen;
		final int		fftSizeC;

		int				kernelLen, kernelLenE, specStart, specStop;
		float[]			win;
		float			f1, f2;
		double			theorKernLen, centerFreq, centerFreqN, weight, d1, cos, sin;

//		System.out.println( "Calculating sparse kernel matrices" );
		
		maxFreq		= (float) Math.min( maxFreq, fs/2 );
		q			= (float) (1 / (Math.pow( 2, 1.0/bandsPerOct ) - 1));
		numKernels	= getNumKernels( bandsPerOct, maxFreq, minFreq );
		kernels		= new Kernel[ numKernels ];
//		cqKernels	= new float[ cqKernelNum ][];
//		cqKernelOffs= new int[ cqKernelNum ];
		maxKernLen	= q * fs / minFreq;
		
//		System.out.println( "ceil " + ((int) Math.ceil( maxKernLen )) + "; nextPower " + (MathUtil.nextPowerOfTwo( (int) Math.ceil( maxKernLen )))) ;
		
		fftSize		= Math.min( maxFFTSize,
		    MathUtil.nextPowerOfTwo( (int) Math.ceil( maxKernLen )));
//		LNKORR_ADD	= -2 * Math.log( fftSize );
		fftSizeC	= fftSize << 1;
		fftBuf		= new float[ fftSizeC ];
//		thresh		= 0.0054f / fftLen; // for Hamming window
		// weird observation : lowering the threshold will _increase_ the
		// spectral noise, not improve analysis! so the truncating of the
		// kernel is essential for a clean analysis (why??). the 0.0054
		// seems to be a good choice, so don't touch it.
//		threshSqr	= 2.916e-05f / (fftSize * fftSize); // for Hamming window (squared!)
//		threshSqr	= 2.916e-05f / fftSize; // for Hamming window (squared!)
		// (0.0054 * 3).squared
		threshSqr	= 2.6244e-4f / (fftSize * fftSize); // for Hamming window (squared!)
		// tempKernel= zeros(fftLen, 1); 
//		sparKernel= [];
		
//		System.out.println( "cqKernelNum = " + cqKernelNum + "; maxKernLen = " + maxKernLen + "; fftSize = " + fftSize + "; threshSqr " + threshSqr );
		
		for( int k = 0; k < numKernels; k++ ) {
			theorKernLen = maxKernLen * (float) Math.pow( 2, (double) -k / bandsPerOct );
			kernelLen	= Math.min( fftSize, (int) Math.ceil( theorKernLen ));
			kernelLenE	= kernelLen & ~1;
			win			= WindowedSincFilter.createFullWindow( kernelLen, WindowedSincFilter.WIN_HAMMING );
			
			centerFreq	= minFreq * Math.pow( 2, (float) k / bandsPerOct );
			centerFreqN	= centerFreq * -MathUtil.PI2 / fs;

//			weight		= 1.0f / (kernelLen * fftSize);
			// this is a good approximation in the kernel len truncation case
			// (tested with pink noise, where it will appear pretty much
			// with the same brightness over all frequencies)
			weight		= 6 / ((theorKernLen + kernelLen) * fftSize);
//			weight		= 2 / ((theorKernLen + kernelLen) * Math.sqrt( fftSize ));
			for( int m = kernelLenE, n = fftSizeC - kernelLenE; m < n; m++ ) {
				fftBuf[ m ] = 0f;
			}
									
			// note that we calculate the complex conjugation of
			// the temporalKernal and reverse its time, so the resulting
			// FFT can be immediately used for the convolution and does not
			// need to be conjugated; this is due to the Fourier property
			// h*(-x) <-> H*(f). time reversal is accomplished by
			// having iteration variable j run....

			// XXX NO! we don't take the reversed conjugate since
			// there seems to be a bug with Fourier.complexTransform
			// that already computes the conjugate spectrum (why??)
//			for( int i = kernelLen - 1, j = fftSizeC - kernelLenE; i >= 0; i-- ) { ... }
//			for( int i = 0, j = 0; /* fftSizeC - kernelLenE; */ i < kernelLen; i++ ) { ... }
			for( int i = 0, j = fftSizeC - kernelLenE; i < kernelLen; i++ ) {
				// complex exponential of a purely imaginary number
				// is cos( imag( n )) + i sin( imag( n ))
				d1			= centerFreqN * i;
//				d1			= centerFreqN * (i - kernelLenE);
				cos			= Math.cos( d1 );
				sin			= Math.sin( d1 ); // Math.sqrt( 1 - cos*cos );
				d1			= win[ i ] * weight;
				fftBuf[ j++ ] = (float) (d1 * cos);
//				fftBuf[ j++ ] = -f1 * sin;  // conj!
				fftBuf[ j++ ] = (float) (d1 * sin);  // NORM!
				if( j == fftSizeC ) j = 0;
			}
			
			// XXX to be honest, i don't get the point
			// of calculating the fft here, since we
			// have an analytic description of the kernel
			// function, it should be possible to calculate
			// the spectral coefficients directly
			// (the fft of a hamming is a gaussian,
			// isn't it?)
			
			Fourier.complexTransform( fftBuf, fftSize, Fourier.FORWARD );
			// with a "high" threshold like 0.0054, the
			// point it _not_ to create a sparse matrix by
			// gating the values. in fact we can locate
			// the kernal spectrally, so all we need to do
			// is to find the lower and upper frequency
			// of the transformed kernel! that makes things
			// a lot easier anyway since we don't need
			// to employ a special sparse matrix library.
			for( specStart = 0; specStart <= fftSize; specStart += 2 ) {
				f1 = fftBuf[ specStart ];
				f2 = fftBuf[ specStart+1 ];
				if( (f1 * f1 + f2 * f2) > threshSqr ) break;
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
			// high frequencies near nyquest blur accross the pi boundary,
			// and we will cut the overlap off by limiting specStop
			// to fftSize instead of fftSize<<1 ...).

			for( specStop = specStart; specStop <= fftSize; specStop += 2 ) {
				f1 = fftBuf[ specStop ];
				f2 = fftBuf[ specStop+1 ];
				if( (f1 * f1 + f2 * f2) <= threshSqr ) break;
			}

//System.out.println( "Kernel k : specStart " + specStart + "; specStop " + specStop + "; centerFreq " + centerFreq );
			kernels[ k ] = new Kernel( specStart, new float[ specStop - specStart ], (float) centerFreq );
			System.arraycopy( fftBuf, specStart, kernels[ k ].data, 0, specStop - specStart );
		}
	}

	/**
	 * 	Helper method for SwingOSC
	 */
	public float[] castToFloatArray( Object[] data )
	{
		final float[] buf = new float[ data.length ];
		for( int i = 0; i < buf.length; i++ ) {
			buf[ i ] = ((Number) data[ i ]).floatValue();
		}
		return buf;
	}
	
	/**
	 * 	Helper method for SwingOSC
	 */
	public float getArrayElement( float[] data, int idx )
	{
		return data[ idx ];
	}

	/**
	 * 	Assumes that the input was already successfully transformed
	 * 	into the Fourier domain (namely into fftBuf as returned by
	 * 	getFFTBuffer()). From this FFT, the method calculates the
	 * 	convolutions with the filter kernels, returning the magnitudes
	 * 	of the filter outputs.
	 * 
	 *	@param	output
	 *	@param	outOff
	 *	@return
	 */
	public float[] convolve( float[] output, int outOff )
	{
		if( output == null ) output = new float[ numKernels ];
		
		float[]	kern;
		float	f1, f2;
				
		for( int k = 0; k < numKernels; k++, outOff++ ) {
			kern	= kernels[ k ].data;
			f1		= 0f;
			f2		= 0f;
			for( int i = kernels[ k ].offset, j = 0; j < kern.length; i += 2, j += 2 ) {
				// complex mult: a * b =
				// (re(a)re(b)-im(a)im(b))+i(re(a)im(b)+im(a)re(b))
				// ; since we left out the conjugation of the kernel(!!)
				// this becomes (assume a is input and b is kernel):
				// (re(a)re(b)+im(a)im(b))+i(im(a)re(b)-re(a)im(b))
				// ; in fact this conjugation is unimportant for the
				// calculation of the magnitudes...
				f1 += fftBuf[ i ] * kern[ j ] - fftBuf[ i+1 ] * kern[ j+1 ];
				f2 += fftBuf[ i ] * kern[ j+1 ] + fftBuf[ i+1 ] * kern[ j ];
			}
//			cBuf[ k ] = ;  // squared magnitude
//			f1 = (float) ((Math.log( f1 * f1 + f2 * f2 ) + LNKORR_ADD) * gain);
//f1 = Math.max( -160, f1 + 90 );
			
			// since we use constQ to decimate spectra, we actually
			// are going to store a "mean square" of the amplitudes
			// so at the end of the decimation we'll have one squareroot,
			// or even easier, when calculating the logarithm, the
			// multiplicator of the log just has to be divided by two.
			// hence we don't calc abs( f ) but abs( f )^2 !
			
			output[ outOff ] = (f1 * f1 + f2 * f2);
		}
		
		return output;
	}
	
	public float[] transform( float[] input, int inOff, int inLen, float output[], int outOff )
	{
		if( output == null ) output = new float[ numKernels ];
		
		final int off, num, num2;

//		gain *= TENBYLOG10;
		off = fftSize >> 1;
		num = Math.min( fftSize - off, inLen );
		
//System.out.println( "inOff " + inOff + "; num  " + num + " ; inOff + num " + (inOff + num) + "; input.length " + input.length + "; fftBuf.length " + fftBuf.length );
		
		System.arraycopy( input, inOff, fftBuf, off, num );
		for( int i = off + num; i < fftSize; i++ ) {
			fftBuf[ i ] = 0f;
		}
		num2 = Math.min( fftSize - num, inLen - num );
		System.arraycopy( input, inOff + num, fftBuf, 0, num2 );
		for( int i = num2; i < off; i++ ) {
			fftBuf[ i ] = 0f;
		}
		
		// XXX evtl., wenn inpWin weggelassen werden kann,
		// optimierte overlap-add fft
		Fourier.realTransform( fftBuf, fftSize, Fourier.FORWARD );
		
		return convolve( output, outOff );
	}
		
	private static class Kernel
	{
		protected final int			offset;
		protected final float[]		data;
		protected final float		freq;
		
		protected Kernel( int offset, float[] data, float freq )
		{
			this.offset	= offset;
			this.data	= data;
			this.freq	= freq;
		}
	}
}