package st.alzo.zio.docker

sealed trait LogEntry extends Product with Serializable {
  def value: String
}

object LogEntry {
  final case class Err(value: String) extends LogEntry
  final case class Out(value: String) extends LogEntry
}

