import sbt._

object Dependencies {
  object V {
    val zio = "2.0.0-M6-2"
    val dockerJava = "3.2.12"
  }
  import V._

  lazy val zDeps = Seq(
    "dev.zio" %% "zio",
    "dev.zio" %% "zio-streams"
  ).map(_ % zio)

  lazy val javaDockerDeps = Seq(
    "com.github.docker-java" % "docker-java-api",
    "com.github.docker-java" % "docker-java-core",
    "com.github.docker-java" % "docker-java-transport"
  ).map(_ % dockerJava)

  lazy val testDeps = Seq(
    "com.github.docker-java" % "docker-java-transport-httpclient5" % dockerJava,
    "dev.zio" %% "zio-test" % zio,
    "dev.zio" %% "zio-test-sbt" % zio
  ).map(_ % Test)

  lazy val allDeps = zDeps ++ javaDockerDeps ++ testDeps

  def allDepsCross(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
    case Some((3, _)) => allDeps ++ Seq(
      "org.immutables" % "value" % "2.8.2" // workaround for https://github.com/lampepfl/dotty/issues/13523
    )
    case _ => allDeps
  }


}
