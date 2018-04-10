lazy val baseName  = "ScissDSP"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "1.3.0-SNAPSHOT"
lazy val mimaVersion    = "1.3.0"

name               := baseName
version            := projectVersion
organization       := "de.sciss"
description        := "Collection of DSP algorithms and components for Scala"
homepage           := Some(url(s"https://github.com/Sciss/${name.value}"))
licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))
scalaVersion       := "2.12.5"
crossScalaVersions := Seq("2.12.5", "2.11.12")

mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion)

libraryDependencies ++= Seq(
  "net.sourceforge.jtransforms" %  "jtransforms"    % "2.4.0",
  "de.sciss"                    %% "serial"         % "1.1.0-SNAPSHOT",
  "org.scalatest"               %% "scalatest"      % "3.0.5" % "test",
  "de.sciss"                    %% "scalaaudiofile" % "1.5.0-SNAPSHOT" % "test"
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint")

initialCommands in console := """
  |import de.sciss.dsp._
  |def randomSignal( size: Int = 128 ) = Array.fill( size )( util.Random.nextFloat() * 2 - 1 )""".stripMargin

// ---- build info ----

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
  BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
  BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
)

buildInfoPackage := "de.sciss.dsp"

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := { val n = name.value
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
}

// ---- ls.implicit.ly ----
// 
// seq(lsSettings :_*)
// 
// (LsKeys.tags   in LsKeys.lsync) := Seq("audio", "spectrum", "dsp", "signal")
// 
// (LsKeys.ghUser in LsKeys.lsync) := Some("Sciss")
// 
// (LsKeys.ghRepo in LsKeys.lsync) := Some(name.value)
