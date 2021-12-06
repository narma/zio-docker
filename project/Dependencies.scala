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
    "com.github.docker-java" % "docker-java" % dockerJava
  )

  lazy val testDeps = Seq(
    "com.github.docker-java" % "docker-java-transport-httpclient5" % dockerJava,
    "dev.zio" %% "zio-test" % zio,
    "dev.zio" %% "zio-test-sbt" % zio
  ).map(_ % Test)

  lazy val allDeps = zDeps ++ javaDockerDeps ++ testDeps
}
