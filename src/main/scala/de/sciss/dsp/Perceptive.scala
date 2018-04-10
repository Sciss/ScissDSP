/*
 * Perceptive.scala
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

object Perceptive {
  private val dBAweight = Array[Float](
    //		1 Hz		1.25		1.6			2			2.5			3.15
       0.0f,		1.000e-7f,	2.512e-7f,	6.310e-7f,	1.567e-6f,	3.890e-6f,
    //		4			5			6.3			8			10			12
       9.661e-6f,	2.371e-5f,	5.370e-5f,	1.365e-4f,	3.162e-4f,	7.161e-4f,
    //		16			20			25			31.5		40			50
       1.531e-3f,	3.126e-3f,	6.026e-3f,	1.096e-2f,	1.905e-2f,	3.090e-2f,
    //		63			80			100			125			160			200
       4.955e-2f,	7.586e-2f,	1.109e-1f,	1.585e-1f,	2.163e-1f,	2.884e-1f,
    //		250			315			400			500			630			800
       3.715e-1f,	4.677e-1f,	5.754e-1f,	6.918e-1f,	8.035e-1f,	9.120e-1f,
    //		1k			1.25k		1.6k		2k			2.5k		3.15k
       1.0f,		1.072f,		1.122f,		1.148f,		1.161f,		1.148f,
    //		4k			5k		6.3k		8k			10k			12.5k
       1.122f,		1.059f,		9.886e-1f,	8.810e-1f,	7.499e-1f,	6.095e-1f,
    //		16k			20k			25k			31.5k
       4.677e-1f,	3.428e-1f,	2.399e-1f,	0.0f
    )

  /** Fills Array with dB(A)-weighting factors
    *
    * @param	weights				Array der Groesse 'num', wird beschrieben
    * @param	freq				Center-Frequenzen in Hertz; Objekt darf mit 'weights'
    *                     identisch sein, d.h. die Frequenzen werden durch
    *                     die zugehoerigen Gewichtungen ueberschrieben!
    * @param	num					Zahl der Gewichte resp. Frequenzen
    */
  def calcDBAweights(weights: Array[Float], freq: Array[Float], num: Int): Unit = {
    var i = 0; while (i < num) {
      val f = freq(i)
      weights(i) = if (f < 1.0f) {
        dBAweight(0)
      } else if (f > 31622.7f) {
        dBAweight(dBAweight.length - 1)
      } else {
        val f2 = (10 * math.log10(f)).toFloat
        val j  = f2.toInt
        val f3 = f2 % 1.0f
        dBAweight(j) * (1.0f - f3) + dBAweight(j + 1) * f3 // ? XXX
      }
    i += 1 }
  }
}
