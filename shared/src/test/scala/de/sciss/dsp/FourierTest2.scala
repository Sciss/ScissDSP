package de.sciss.dsp

object FourierTest2 extends App with Runnable {
  run()

  def run(): Unit = {
    val rnd = new util.Random(0L)
    val in = Array.fill(64)(rnd.nextDouble() * 2 - 1)
    val fft = Fourier(32)
    val out = in.clone()
    fft.complexForward(out)
    println(out.mkString("[ ", ", ", " ]"))
  }
}
