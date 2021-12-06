import sbt.CrossVersion

object BuildHelper {

  def projectScalaOptions(scalaVersion: String) = {
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, v)) if v > 12 => projectScalac213Options()
      case Some((3, _)) => projectScala3Options()
    }
  }

  def projectScala3Options(): Seq[String] = Seq(
    "-encoding",
    "utf-8",
    "-feature",
    "-explain",
    // "-indent",
//    "-new-syntax",
    "-source", "3.0-migration",
//     "-rewrite",
    "-explain-types",
    "-language:existentials",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Xfatal-warnings",
    "-unchecked",
    "-Ykind-projector"
  )

  def projectScalac213Options(): Seq[String] = Seq(
    "-encoding",
    "utf-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xsource:3.0",
    "-Xlint:_,-missing-interpolator,-byname-implicit",
    "-Ywarn-unused",
    "-Ymacro-annotations",
    "-Yrangepos",
    "-Werror",
    "-explaintypes",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Xfatal-warnings",
    "-Wconf:any:error",
  )
}
