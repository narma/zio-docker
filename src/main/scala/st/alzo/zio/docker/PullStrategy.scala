package st.alzo.zio.docker

case class PullStrategy(always: Boolean = false, ifNotExists: Boolean = true, ifLatest: Boolean = false)
