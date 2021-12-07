import Dependencies._
import BuildHelper._


inThisBuild(List(
  version          := "0.1.0-SNAPSHOT",
  organization     := "st.alzo",
  organizationName := "alzo",
  crossScalaVersions := Seq("2.13.7", "3.1.1-RC1"),
  scalaVersion := crossScalaVersions.value.head,
  licenses := Seq(("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))),
  homepage := Some(url("https://github.com/narma/zio-docker")),
  developers := List(
    Developer(id = "narma", name = "Sergey Rublev", email = "alzo@alzo.space", url = url("https://narma.github.io"))
  )
))

lazy val root = (project in file("."))
  .settings(
    name := "zio-docker",
    libraryDependencies ++= allDepsCross(scalaVersion.value),
    scalacOptions := projectScalaOptions(scalaVersion.value),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    Compile / console / scalacOptions  ~= (_.filterNot(
      filteredConsoleOptions.contains
    )),
    Test / console / scalacOptions  ~= (_.filterNot(
      filteredConsoleOptions.contains
    )),
    autoAPIMappings := true
  )

