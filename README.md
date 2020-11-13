# ScissDSP

[![Build Status](https://travis-ci.org/Sciss/ScissDSP.svg?branch=main)](https://travis-ci.org/Sciss/ScissDSP)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/scissdsp_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/scissdsp_2.13)

## statement

ScissDSP is a collection of Digital Signal Processing (DSP) components for the Scala programming language. It
is (C)opyright 2001&ndash;2020 by Hanns Holger Rutz. All rights reserved.

ScissDSP is released under the [GNU Affero General Public License](https://git.iem.at/sciss/ScissDSP/raw/main/LICENSE) v3+
and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

For project status, API and current version, visit [git.iem.at/sciss/ScissDSP](https://git.iem.at/sciss/ScissDSP).

## building

This project builds with sbt against Scala 2.13, 2.12, Dotty (JVM) and 2.13 (Scala.js).
The last version to support Scala 2.11 was v1.3.2.

The project depends
on [Transforms4s](https://github.com/Sciss/Transform4s) for the FFT.

## linking

The following artifact is necessary as dependency:

    libraryDependencies += "de.sciss" %% "scissdsp" % v

The current version `v` is `"2.1.0"`

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## notes

- As of v1.0.0, the FFT algorithm has changed. It seems that in the previous version the phases of the real transform
  were inverted. The new algorithm seems consistent with other FFT algorithms tested. The floating point rounding 
  noise of the new algorithm has not changed (in fact is a tiny bit smaller).
- For an example of the Constant-Q transform, see the 
  [SonogramOverview project](https://git.iem.at/sciss/SonogramOverview).
- the MFCC implementation is based on code by 
  [Ganesh Tiwari](https://code.google.com/p/speech-recognition-java-hidden-markov-model-vq-mfcc/), released 2012 
  under the Apache License 2.0. 
