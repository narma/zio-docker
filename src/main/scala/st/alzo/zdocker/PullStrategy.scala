package st.alzo.zdocker

case class PullStrategy(always: Boolean = false, ifNotExists: Boolean = true, ifLatest: Boolean = false)
