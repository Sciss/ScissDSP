# ScissDSP

[![Flattr this](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=sciss&url=https%3A%2F%2Fgithub.com%2FSciss%2FScissDSP&title=ScissDSP%20Library&language=Scala&tags=github&category=software)
[![Build Status](https://travis-ci.org/Sciss/ScissDSP.svg?branch=master)](https://travis-ci.org/Sciss/ScissDSP)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/scissdsp_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/scissdsp_2.11)

## statement

ScissDSP is a collection of Digital Signal Processing (DSP) components for the Scala programming language. It is (C)opyright 2001&ndash;2017 by Hanns Holger Rutz. All rights reserved.

ScissDSP is released under the [GNU Lesser General Public License](http://github.com/Sciss/ScissDSP/blob/master/licenses/ScissDSP-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

For project status, API and current version, visit [github.com/Sciss/ScissDSP](http://github.com/Sciss/ScissDSP).

## building

This project compiles with sbt 0.13 against Scala 2.12, 2.11, 2.10. It depends on [JTransforms](https://sites.google.com/site/piotrwendykier/software/jtransforms) for the FFT. JTransforms provides three licenses (MPL/LGPL/GPL) and is incorporated here based on the GNU LGPL.

## linking

The following artifact is necessary as dependency:

    libraryDependencies += "de.sciss" %% "scissdsp" % v

The current version `v` is `"1.2.3"`

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## notes

- As of v1.0.0, the FFT algorithm has changed. It seems that in the previous version the phases of the real transform were inverted. The new algorithm seems consistent with other FFT algorithms tested. The floating point rounding noise of the new algorithm has not changed (in fact is a tiny bit smaller).
- For an example of the Constant-Q transform, see the [SonogramOverview project](http://github.com/Sciss/SonogramOverview).
- the MFCC implementation is based on code by [Ganesh Tiwari](https://code.google.com/p/speech-recognition-java-hidden-markov-model-vq-mfcc/), released 2012 under the Apache License 2.0. 
