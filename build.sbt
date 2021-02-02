lazy val baseName  = "ScissDSP"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "2.2.2"
lazy val mimaVersion    = "2.2.0"

lazy val deps = new {
  val main = new {
    // val jtransforms = "2.4.0"
    val transform4s = "0.1.1"
    val serial      = "2.0.1"
  }
  val test = new {
    val scalaTest   = "3.2.3"
    val audioFile   = "2.3.3"
  }
}

lazy val commonJvmSettings = Seq(
  crossScalaVersions := Seq("3.0.0-M3", "2.13.4", "2.12.13"),
)

// sonatype plugin requires that these are in global
ThisBuild / version      := projectVersion
ThisBuild / organization := "de.sciss"

lazy val root = crossProject(JSPlatform, JVMPlatform).in(file("."))
  .jvmSettings(commonJvmSettings)
  .settings(
    name               := baseName,
//    version            := projectVersion,
//    organization       := "de.sciss",
    description        := "Collection of DSP algorithms and components for Scala",
    homepage           := Some(url(s"https://git.iem.at/sciss/${name.value}")),
    licenses           := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
    scalaVersion       := "2.13.4",
    mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion),
    libraryDependencies ++= Seq(
      // "net.sourceforge.jtransforms" %  "jtransforms"    % deps.main.jtransforms,
      "de.sciss"       %%% "serial"         % deps.main.serial,
      "de.sciss"       %%% "transform4s"    % deps.main.transform4s,
      "de.sciss"       %%% "audiofile"      % deps.test.audioFile % Test,
      "org.scalatest"  %%% "scalatest"      % deps.test.scalaTest % Test,
    ),
    scalacOptions in (Compile, compile) ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint", "-Xsource:2.13"),
    initialCommands in console := 
      """import de.sciss.dsp._
        |def randomSignal(size: Int = 128) = Array.fill(size)(util.Random.nextFloat() * 2 - 1)""".stripMargin,
  )
  .settings(publishSettings)

// ---- publishing ----
lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  developers := List(
    Developer(
      id    = "sciss",
      name  = "Hanns Holger Rutz",
      email = "contact@sciss.de",
      url   = url("https://www.sciss.de")
    )
  ),
  scmInfo := {
    val h = "git.iem.at"
    val a = s"sciss/${name.value}"
    Some(ScmInfo(url(s"https://$h/$a"), s"scm:git@$h:$a.git"))
  },
)

