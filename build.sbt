Global / excludeLintKeys += organization

val scalafixDeps = Seq(
  "com.github.liancheng" %% "organize-imports" % "0.6.0"
)

val devs = List(
  Developer("pdalpra", "Pierre Dal-Pra", "p.dalpra@stuart.com", url("https://stuart.com")),
  Developer("aartigao", "Alan Artigao Carreño", "a.artigao@stuart.com", url("https://stuart.com"))
)

inThisBuild(
  Seq(
    scalaVersion           := "2.13.8",
    crossScalaVersions     := List(scalaVersion.value, "3.1.3"),
    versionScheme          := Some("semver-spec"),
    organization           := "com.stuart",
    homepage               := Some(url("https://github.com/StuartApp/zcaffeine")),
    licenses               := List(License.Apache2),
    developers             := devs,
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
    scalafixDependencies ++= scalafixDeps
  )
)

val zioVersion = "2.0.1"

val zcaffeine = (project in file("."))
  .settings(fmtAllAlias)
  .settings(organization := "com.stuart")
  .settings(
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    autoAPIMappings   := true,
    Compile / doc / scalacOptions ++= scaladocOptions(scalaVersion.value),
    libraryDependencies ++= Seq(
      "com.github.ben-manes.caffeine" % "caffeine"           % "3.1.1",
      "dev.zio"                      %% "zio"                % zioVersion,
      "dev.zio"                      %% "zio-prelude"        % "1.0.0-RC15",
      "org.scala-lang.modules"       %% "scala-java8-compat" % "1.0.2",
      // Testing
      "dev.zio" %% "zio-test"     % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val fmtAllAlias = addCommandAlias(
  "fmt",
  List(
    "scalafixAll",
    "scalafmtAll",
    "scalafmtSbt"
  ).mkString(";", ";", "")
)

def scaladocOptions(scalaVersion: String): Seq[String] =
  if (scalaVersion.startsWith("2")) List("-skip-packages", "zio") else Nil
