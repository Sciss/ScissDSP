/*
 * Resample.scala
 * (ScissDSP)
 *
 * Copyright (c) 2001-2013 Hanns Holger Rutz. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.dsp

object Resample {
   object Quality {
      val Low     = Quality( 0.7,  6.5,  5 )
      val Medium  = Quality( 0.8,  7.0,  9 )
      val High    = Quality( 0.86, 7.5, 15 )
   }
   final case class Quality( rollOff: Double, kaiserBeta: Double, zeroCrossings: Int )

   def apply( quality: Quality = Quality.Medium ) : Resample = new Impl( quality )

   private final class Impl( val quality: Quality ) extends Resample {
      private val fltSmpPerCrossing = 4096
      private val fltLen            = ((fltSmpPerCrossing * quality.zeroCrossings) / quality.rollOff + 0.5).toInt
      private val flt               = new Array[ Float ]( fltLen )
      private val fltD: Array[ Float ] = null   // XXX TODO interpolation vector
      private val gain				   = WindowedSincFilter.createAntiAliasFilter(
            flt, fltD, halfWinSize = fltLen, samplesPerCrossing = fltSmpPerCrossing, rollOff = quality.rollOff,
            kaiserBeta = quality.kaiserBeta )

      def process( src: Array[ Float ], srcOff: Double, dest: Array[ Float ], destOff0: Int, length: Int, factor: Double ) {
         val smpIncr = 1.0 / factor
         var phase   = srcOff
         val srcLen	= src.length

         val (fltIncr, rsmpGain) = if( smpIncr > 1.0 ) {
            (fltSmpPerCrossing * factor, gain)
         } else {
            (fltSmpPerCrossing.toDouble, gain * smpIncr)
         }

         var srcOffI = 0
         var fltOff  = 0.0
         var fltOffI = 0
         var value   = 0.0
         var q       = 0.0
         var destOff = destOff0

         if( fltD == null ) {	// -------------------- without interpolation --------------------
            var i = 0; while( i < length ) {
               q        = phase % 1.0
               value    = 0.0
               srcOffI	= phase.toInt
               fltOff	= q * fltIncr
               fltOffI	= (fltOff + 0.5).toInt
               while( (fltOffI < fltLen) && (srcOffI >= 0) ) {
                  value   += src( srcOffI ).toDouble * flt( fltOffI )
                  srcOffI -= 1
                  fltOff  += fltIncr
                  fltOffI  = (fltOff + 0.5).toInt
               }

               srcOffI	= phase.toInt + 1
               fltOff	= (1.0 - q) * fltIncr
               fltOffI	= (fltOff + 0.5).toInt
               while( (fltOffI < fltLen) && (srcOffI < srcLen) ) {
                  value   += src( srcOffI ).toDouble * flt( fltOffI )
                  srcOffI += 1
                  fltOff  += fltIncr
                  fltOffI  = (fltOff + 0.5).toInt
               }

               dest( destOff ) = (value * rsmpGain).toFloat
               destOff += 1
               i += 1
               phase = srcOff + i * smpIncr
            }

         } else {	// -------------------- linear interpolation --------------------

            var i = 0; while( i < length ) {
               q		   = phase % 1.0
               value    = 0.0
               srcOffI	= phase.toInt
               fltOff	= q * fltIncr
               fltOffI	= fltOff.toInt
               while( (fltOffI < fltLen) && (srcOffI >= 0) ) {
                  val r    = fltOff % 1.0;		// 0...1 for interpol.
                  value   += src( srcOffI ) * (flt( fltOffI ) + fltD( fltOffI ) * r)
                  srcOffI -= 1
                  fltOff  += fltIncr
                  fltOffI  = fltOff.toInt
               }

               srcOffI	= phase.toInt + 1
               fltOff	= (1.0 - q) * fltIncr
               fltOffI	= fltOff.toInt
               while( (fltOffI < fltLen) && (srcOffI < srcLen) ) {
                  val r     = fltOff % 1.0;		// 0...1 for interpol.
                  value    += src( srcOffI ) * (flt( fltOffI ) + fltD( fltOffI ) * r)
                  srcOffI  += 1
                  fltOff   += fltIncr
                  fltOffI  = fltOff.toInt
               }

               dest( destOff ) = (value * rsmpGain).toFloat
               destOff += 1
               i += 1
               phase = srcOff + i * smpIncr
            }
         }
      }
   }
}
trait Resample {
   def quality: Resample.Quality

   /**
  	 *	Daten resamplen; "bandlimited interpolation"
  	 *
  	 *	@param	srcOff		erlaubt verschiebung unterhalb sample-laenge!
  	 *	@param	length		bzgl. dest!! src muss laenge 'length/factor' aufgerundet haben!!
  	 *	@param	factor		dest-smpRate/src-smpRate
  	 */
   def process( src: Array[ Float ], srcOff: Double, dest: Array[ Float ], destOff: Int, length: Int, factor: Double ) : Unit
}