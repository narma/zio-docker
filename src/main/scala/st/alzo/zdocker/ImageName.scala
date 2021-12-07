package st.alzo.zdocker

import zio.{Task, ZIO}

case class ImageName(image: String, tag: String, repository: Option[String] = None) {
  def name: String = repository match {
    case None      => image
    case Some(rep) => s"$rep/$image"
  }

  def fullName: String = s"$name:$tag"
}

object ImageName {
  case class ImageNameInvalid(msg: String) extends RuntimeException(msg)

  def apply(s: String): Task[ImageName] =
    ZIO.getOrFailWith(ImageNameInvalid(s"can't parse Docker Image name: $s"))(fromString(s))

  def fromString(s: String): Option[ImageName] = {
    val parts = s.split('/').toList
    val imgFull = parts.last
    val rep = parts.dropRight(1).mkString("/")

    val parsed = imgFull.split(':').toList match {
      case List(img, tag) => Some((img, tag))
      case List(img)      => Some((img, "latest"))
      case _              => None
    }

    parsed.map { case (img, tag) =>
      ImageName(img, tag, Option.when(rep.nonEmpty)(rep))
    }
  }

}
