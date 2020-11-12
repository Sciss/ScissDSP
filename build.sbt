lazy val baseName  = "ScissDSP"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "2.1.0-SNAPSHOT"
lazy val mimaVersion    = "2.1.0"

lazy val deps = new {
  val main = new {
    // val jtransforms = "2.4.0"
    val transform4s = "0.1.0-SNAPSHOT"
    val serial      = "2.0.0"
  }
  val test = new {
    val scalaTest   = "3.2.3"
    val audioFile   = "2.2.0"
  }
}

lazy val commonJvmSettings = Seq(
  crossScalaVersions := Seq("3.0.0-M1", "2.13.3", "2.12.12"),
)

lazy val root = crossProject(JSPlatform, JVMPlatform).in(file("."))
  .jvmSettings(commonJvmSettings)
  .settings(
    name               := baseName,
    version            := projectVersion,
    organization       := "de.sciss",
    description        := "Collection of DSP algorithms and components for Scala",
    homepage           := Some(url(s"https://git.iem.at/sciss/${name.value}")),
    licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
    scalaVersion       := "2.13.3",
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
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := { val n = name.value
<scm>
  <url>git@git.iem.at:sciss/{n}.git</url>
  <connection>scm:git:git@git.iem.at:sciss/{n}.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
  }
)

