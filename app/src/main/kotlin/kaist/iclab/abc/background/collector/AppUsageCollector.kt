package kaist.iclab.abc.background.collector

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.support.v4.app.Fragment
import android.util.Log
import io.objectbox.kotlin.boxFor
import kaist.iclab.abc.App
import kaist.iclab.abc.background.Status
import kaist.iclab.abc.common.util.Utils
import kaist.iclab.abc.data.PreferenceAccessor
import kaist.iclab.abc.data.entities.AppUsageEventEntity
import kaist.iclab.abc.data.entities.AppUsageStatEntity
import kaist.iclab.abc.data.types.AppUsageEventType
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class AppUsageCollector(val context: Context): BaseCollector {
    private var scheduledFuture: ScheduledFuture<*>? = null

    override fun startCollection(uuid: String, group: String, email: String) {
        if(scheduledFuture?.isDone == false) return
        status.postValue(Status.STARTED)

        scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            try {
                val pref = PreferenceAccessor.getInstance(context)
                val fiveMin = TimeUnit.MINUTES.toMillis(5)
                val to = System.currentTimeMillis()
                val from = if(pref.lastTimeAppUsageAccessed <= 0) to - TimeUnit.HOURS.toMillis(1) else pref.lastTimeAppUsageAccessed

                for(i in from until to step fiveMin) {
                    if(i + fiveMin < to) {
                        collect(uuid, group, email, i, i + fiveMin)
                        pref.lastTimeAppUsageAccessed = i + fiveMin
                    }
                }
            } catch (e: Exception) {
                if (e is SecurityException) {
                    stopCollection()
                    status.postValue(Status.ABORTED(e))
                }
            }

        }, 0, 15, TimeUnit.MINUTES)
    }

    override fun stopCollection() {
        scheduledFuture?.cancel(true)
        status.postValue(Status.CANCELED)
    }

    private fun collect(uuid: String, group: String, email: String, from: Long, to: Long) {
        status.postValue(Status.RUNNING)

        if(!checkEnableToCollect(context)) throw SecurityException("Access to app usage stats is not granted.")
        val manager = getUsageStatManager(context)

        val appStatEntities = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, from, to).asSequence().mapNotNull {
            return@mapNotNull if(it.totalTimeInForeground <= 0) {
                null
            } else {
                AppUsageStatEntity(
                    name = Utils.getApplicationName(context, it.packageName) ?: "",
                    packageName = it.packageName,
                    isSystemApp = Utils.isSystemApp(context, it.packageName),
                    isUpdatedSystemApp = Utils.isUpdatedSystemApp(context, it.packageName),
                    startTime = it.firstTimeStamp,
                    endTime = it.lastTimeStamp,
                    lastTimeUsed = it.lastTimeUsed,
                    totalTimeForeground = it.totalTimeInForeground
                ).apply {
                    timestamp = to
                    utcOffset = Utils.utcOffsetInHour()
                    subjectEmail = email
                    experimentUuid = uuid
                    experimentGroup = group
                    isUploaded = false
                }
            }
        }.toList()

        val events = manager.queryEvents(from, to)
        val event = UsageEvents.Event()
        val appEventEntities: MutableList<AppUsageEventEntity> = mutableListOf()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            appEventEntities.add(AppUsageEventEntity(
                name = Utils.getApplicationName(context, event.packageName) ?: "",
                packageName = event.packageName,
                type = AppUsageEventType.fromValue(event.eventType, AppUsageEventType.NONE),
                isSystemApp = Utils.isSystemApp(context, event.packageName),
                isUpdatedSystemApp = Utils.isUpdatedSystemApp(context, event.packageName)
            ).apply {
                timestamp = event.timeStamp
                utcOffset = Utils.utcOffsetInHour()
                subjectEmail = email
                experimentUuid = uuid
                experimentGroup = group
                isUploaded = false
            })
        }
        App.boxFor<AppUsageStatEntity>().put(appStatEntities)
        App.boxFor<AppUsageEventEntity>().put(appEventEntities)

        appStatEntities.forEach {
            Log.d(TAG, "Box.put(" +
                "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                "experimentGroup = ${it.experimentGroup}, entity = $it)")
        }
        appEventEntities.forEach {
            Log.d(TAG, "Box.put(" +
                "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                "experimentGroup = ${it.experimentGroup}, entity = $it)")
        }
    }

    @SuppressLint("WrongConstant")
    private fun getUsageStatManager(context: Context) : UsageStatsManager {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        } else {
            context.getSystemService("usagestats") as UsageStatsManager
        }
    }

    companion object {
        fun checkEnableToCollect(context: Context) : Boolean {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }

        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }

        fun newIntentForSetup() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

        private val TAG : String = AppUsageCollector::class.java.simpleName
    }
}
