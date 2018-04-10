lazy val baseName  = "ScissDSP"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "1.3.0"
lazy val mimaVersion    = "1.3.0"

lazy val deps = new {
  val main = new {
    val jtransforms = "2.4.0"
    val serial      = "1.1.0"
  }
  val test = new {
    val scalaTest   = "3.0.5"
    val audioFile   = "1.5.0"
  }
}

lazy val root = project.withId(baseNameL).in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name               := baseName,
    version            := projectVersion,
    organization       := "de.sciss",
    description        := "Collection of DSP algorithms and components for Scala",
    homepage           := Some(url(s"https://github.com/Sciss/${name.value}")),
    licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
    scalaVersion       := "2.12.5",
    crossScalaVersions := Seq("2.12.5", "2.11.12"),
    mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion),
    libraryDependencies ++= Seq(
      "net.sourceforge.jtransforms" %  "jtransforms"    % deps.main.jtransforms,
      "de.sciss"                    %% "serial"         % deps.main.serial,
      "org.scalatest"               %% "scalatest"      % deps.test.scalaTest % Test,
      "de.sciss"                    %% "audiofile"      % deps.test.audioFile % Test
    ),
    scalacOptions in (Compile, compile) ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint"),
    initialCommands in console := 
      """import de.sciss.dsp._
        |def randomSignal(size: Int = 128) = Array.fill(size)(util.Random.nextFloat() * 2 - 1)""".stripMargin,
    // ---- build info ----
    buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
      BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
      BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
    ),
    buildInfoPackage := "de.sciss.dsp"
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
)

