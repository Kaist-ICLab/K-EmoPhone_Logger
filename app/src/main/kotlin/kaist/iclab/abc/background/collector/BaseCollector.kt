package kaist.iclab.abc.background.collector

interface BaseCollector {
    fun startCollection(uuid: String, group: String, email: String)
    fun stopCollection()
}