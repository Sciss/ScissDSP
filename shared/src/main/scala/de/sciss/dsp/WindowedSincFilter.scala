/*
 * WindowedSincFilter.scala
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

object WindowedSincFilter {
  import math.Pi
  import Window.calcBesselZero

  val DefaultSamplesPerCrossing	= 256

// -------- public Methoden --------

  /** @param	impResp				      target array of size 'halfWinSize' for impulse response
    * @param	freq				        cut-off frequency
    * @param	halfWinSize			    size of Kaiser window divided by two
    * @param	kaiserBeta			    Kaiser window parameter
    * @param	samplesPerCrossing	number of coefficients per period
    */
  def createLPF(impResp: Array[Double], freq: Double, halfWinSize: Int, kaiserBeta: Double,
                samplesPerCrossing: Int = DefaultSamplesPerCrossing): Unit = {
    val dNum		   = samplesPerCrossing.toDouble
    val smpRate		= freq * 2.0
    val normFactor	= 1.0 / (halfWinSize - 1)

    // ideal lpf = infinite sinc-function; create truncated version
    impResp(0) = smpRate.toDouble
    var i = 1
    while (i < halfWinSize) {
      val d = Pi * i / dNum
      impResp(i) = math.sin( smpRate * d ) / d
      i += 1
    }

    // apply Kaiser window
    val iBeta = 1.0 / calcBesselZero( kaiserBeta )
    i = 1
    while	(i < halfWinSize) {
      val d = i * normFactor
      impResp(i) *= calcBesselZero(kaiserBeta * math.sqrt(1.0 - d * d)) * iBeta
      i += 1
    }
  }

  /** @param	impResp			wird mit Impulsantworten gefuellt
    * @param	impRespD		Differenzen : null erlaubt, dann keine Interpolation
    *                     von Resample etc. moeglich
    * @param	halfWinSize			Zahl d. Koeffizienten; => smpPerCrossing * ZahlDerNulldurchlaeufe
    * @param	samplesPerCrossing	bezogen auf den sinc
    * @param	rollOff			0...1 CutOff
    * @param	kaiserBeta		Parameter fuer Kaiser-Fenster
    *
    * @return	Gain-Wert (abs amp), der den LPF bedingten Lautstaerkeverlust ausgleichen wuerde
    */
  def createAntiAliasFilter(impResp: Array[Double], impRespD: Array[Double], halfWinSize: Int, rollOff: Double,
                            kaiserBeta: Double, samplesPerCrossing: Int = DefaultSamplesPerCrossing): Double = {

    createLPF(impResp, 0.5 * rollOff, halfWinSize, kaiserBeta, samplesPerCrossing)

    if (impRespD != null) {
      var i = 0
      while (i < halfWinSize - 1) {
        impRespD(i) = impResp(i + 1) - impResp(i)
        i += 1
      }
      impRespD(i) = -impResp(i)
    }
    var dcGain = 0.0
    var j = samplesPerCrossing
    while (j < halfWinSize) {
      dcGain += impResp(j)
      j += samplesPerCrossing
    }
    dcGain = 2 * dcGain + impResp(0)

    1.0 / math.abs(dcGain)
  }
}