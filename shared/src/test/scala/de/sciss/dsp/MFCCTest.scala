package de.sciss.dsp

import java.net.URI

import de.sciss.audiofile.AudioFile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object MFCCTest extends App {
  val c = MFCC.Config()
//  val uri = new URI("file:/home/hhrutz/Documents/devel/MutagenTx/audio_work/mfcc_input.aif")
  val uri = new URI("file:/data/projects/AchromaticSimultan/HRIR_L2702.aif")
  val fut = for {
    af <- AudioFile.openReadAsync(uri)
    buf = af.buffer(1024)
    _ <- {
      c.sampleRate  = af.sampleRate
      c.fftSize     = 1024
      af.read(buf)
    }
    _ <- af.close()
  } yield {
    val t     = MFCC(c)
    val res   = t.process(buf(0).map(_.toDouble), 0, 1024)
    res
  }

  fut.onComplete {
    case Success(res) =>
      println("Result:")
      println(res.mkString("[", ", ", "]"))
    case Failure(ex) =>
      ex.printStackTrace()
  }

  Await.ready(fut, Duration.Inf)
}
