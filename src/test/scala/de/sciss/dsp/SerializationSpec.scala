package de.sciss.dsp

import de.sciss.serial.{DataInput, DataOutput}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SerializationSpec extends AnyFlatSpec with Matchers {
  "ConstQ.Config" should "serialize forth and back" in {
    val cfg     = ConstQ.Config()
    cfg.sampleRate  = 96000.0
    cfg.minFreq     = 99.9f
    cfg.maxFreq     = 6666.6f
    cfg.maxTimeRes  = 8.8f
    cfg.bandsPerOct = 123
    cfg.maxFFTSize  = 8192
    cfg.threading   = Threading.Custom(3)

    val out         = DataOutput()
    ConstQ.Config.format.write(cfg, out)
    val in          = DataInput(out.toByteArray)
    val houdini     = ConstQ.Config.format.read(in)

    assert(cfg.build === houdini)
  }

  "MFCC.Config" should "serialize forth and back" in {
    val cfg     = MFCC.Config()
    cfg.sampleRate  = 96000.0
    cfg.minFreq     = 99.9f
    cfg.maxFreq     = 6666.6f
    cfg.fftSize     = 8192
    cfg.numCoeff    = 22
    cfg.preEmphasis = true
    cfg.numFilters  = 33
    cfg.threading   = Threading.Custom(3)

    val out         = DataOutput()
    MFCC.Config.format.write(cfg, out)
    val in          = DataInput(out.toByteArray)
    val houdini     = MFCC.Config.format.read(in)

    assert(cfg.build === houdini)
  }
}