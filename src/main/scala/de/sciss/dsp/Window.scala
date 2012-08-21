package de.sciss.dsp

import collection.immutable.{IndexedSeq => IIdxSeq}
import annotation.switch

object Window {
   import math.Pi
   import Util.Pi2

   def apply( id: Int, param: Double = 0.0 ) : Window = (id: @switch) match {
      case 0 => Hamming
      case 1 => Blackman
      case 2 => Kaiser( param )
      case 3 => Rectangle
      case 4 => Hanning
      case 5 => Triangle
      case _ => throw new IllegalArgumentException( id.toString )
   }

   def all : IIdxSeq[ Window ] = IIdxSeq(
      Hamming, Blackman, Kaiser4, Kaiser5, Kaiser6, Kaiser8, Rectangle, Hanning, Triangle
   )

   sealed trait Parameterless extends Window {
      final def param: Double = 0.0
   }

   case object Hamming extends Parameterless {
      val id = 0

      def fill( a: Array[ Float ], off: Int, len: Int ) : Array[ Float ] = {
         val win        = if( a == null ) new Array[ Float ]( len ) else a
       	val normFactor = Pi2 / len
         var i = 0; var j = off; while( i < len ) {
            val d = i * normFactor + Pi
            win( j ) = (0.54 + 0.46 * math.cos( d )).toFloat
         i += 1; j += 1 }
         win
      }
   }

	case object Blackman extends Parameterless {
      val id = 1

      def fill( a: Array[ Float ], off: Int, len: Int ) : Array[ Float ] = {
         val win        = if( a == null ) new Array[ Float ]( len ) else a
         val normFactor = Pi2 / len
         var i = 0; var j = off; while( i < len ) {
            val d = i * normFactor + Pi
            win( j ) = (0.42 + 0.5 * math.cos( d ) + 0.08 * math.cos( 2 * d )).toFloat
         i += 1; j += 1 }
         win
      }
   }

//   object Kaiser {
//      def apply( beta: Double ) : Kaiser = new Impl( beta )
//      def unapply( fun: Function ) : Option[ Double ] = {
//         if( fun.isInstanceOf[ Kaiser ]) Some( fun.asInstanceOf[ Kaiser ].beta ) else None
//      }
//
//      private final class Impl( val beta: Double ) extends Kaiser {
//         override def equals( that: Any ) = that != null && that.isInstanceOf[ Kaiser ] && {
//            val k = that.asInstanceOf[ Kaiser ]; k.beta == beta
//         }
//      }
//   }
   final case class Kaiser( beta: Double ) extends Window {
      def param: Double = beta
      def id = 2
      override def toString = "Kaiser \03B2=" + beta

      def fill( a: Array[ Float ], off: Int, len: Int ) : Array[ Float ] = {
         val win        = if( a == null ) new Array[ Float ]( len ) else a
         val normFactor = 2.0 / len
         val iBeta      = 1.0 / calcBesselZero( beta )
         var i = 0; var j = off; while( i < len ) {
            val d = i * normFactor - 1
            win( j ) = (calcBesselZero( beta * math.sqrt( 1.0 - d*d )) * iBeta).toFloat
         i += 1; j += 1 }

         win
      }
   }

   val Kaiser4 = Kaiser( 4.0 )
   val Kaiser5 = Kaiser( 5.0 )
   val Kaiser6 = Kaiser( 6.0 )
   val Kaiser8 = Kaiser( 8.0 )

   case object Rectangle extends Parameterless {
      val id = 3

      def fill( a: Array[ Float ], off: Int, len: Int ) : Array[ Float ] = {
         val win = if( a == null ) new Array[ Float ]( len ) else a
         val stop = off + len; var j = off; while( j < stop ) {
            win( j ) = 1f
         j += 1 }
         win
      }
   }

   case object Hanning extends Parameterless {
      val id = 4

      def fill( a: Array[ Float ], off: Int, len: Int ) : Array[ Float ] = {
         val win        = if( a == null ) new Array[ Float ]( len ) else a
         val normFactor = Pi2 / len
         var i = 0; var j = off; while( i < len ) {
            val d    = i * normFactor + Pi
            win( j ) = (0.5 + 0.5 * math.cos( d )).toFloat
         i += 1; j += 1 }
         win
      }
   }

   case object Triangle extends Parameterless {
      val id = 5

      def fill( a: Array[ Float ], off: Int, len: Int ) : Array[ Float ] = {
         val win        = if( a == null ) new Array[ Float ]( len ) else a
         val normFactor = 2.0 / len
         var i = 0; var j = off; while( j < len ) {
            val d    = i * normFactor - 1
            win( j ) = (1.0 - math.abs( d )).toFloat
         i += 1; j += 1 }
         win
      }
   }

   private[dsp] def calcBesselZero( x: Double ) : Double = {
  		var d2	= 1.0
  		var sum  = 1.0
  		var n	   = 1
  		val xh   = x * 0.5

  		do {
  			val d1 = xh / n
  			n     += 1
  			d2	   *= d1 * d1
  			sum   += d2
  		} while( d2 >= sum * 1e-21 )  // auf 20 Nachkommastellen genau

  		sum
  	}
}
sealed trait Window {
   def id: Int
   def param: Double

   final def create( len: Int ) : Array[ Float ] = fill( null, 0, len )
   def fill( a: Array[ Float ], off: Int, len: Int ) : Array[ Float ]
}