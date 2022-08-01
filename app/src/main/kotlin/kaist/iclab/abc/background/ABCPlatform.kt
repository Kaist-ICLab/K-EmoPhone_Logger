package kaist.iclab.abc.background

import android.content.Context
import kaist.iclab.abc.data.PreferenceAccessor

object ABCPlatform {
    fun start(context: Context) {
        CollectorWorker.start(true)
        SurveyManager.schedule(context)
        SyncManager.sync(true)
    }

    fun maintain(context: Context) {
        CollectorWorker.start(false)
        SurveyManager.schedule(context)
        SyncManager.sync(false)
    }

    fun stop(context: Context) {
        CollectorWorker.stop(context)
        SurveyManager.cancel(context)
        PreferenceAccessor.getInstance(context).clear()
    }
}