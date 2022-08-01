package kaist.iclab.abc.background

import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import kaist.iclab.abc.R
import kaist.iclab.abc.background.collector.*
import kaist.iclab.abc.common.base.BaseService
import kaist.iclab.abc.common.util.NotificationUtils
import kaist.iclab.abc.data.entities.LogEntity
import kaist.iclab.abc.data.entities.LogEntityCursor
import kaist.iclab.abc.data.entities.ParticipationEntity

class CollectorService: BaseService() {
    companion object {
        val EXTRA_STOP_SERVICE = "${CollectorService::class.java.canonicalName}.EXTRA_STOP_SERVICE"
    }

    private lateinit var collectors: MutableMap<String, BaseCollector>

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        collectors = mutableMapOf()

        LocalBroadcastManager.getInstance(this).registerReceiver(SurveyManager.EventReceiver,
            IntentFilter(LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(
            NotificationUtils.NOTIFICATION_ID_EXPERIMENT_IN_PROGRESS,
            NotificationUtils.buildNotificationForExperimentInProgress(this)
        )

        if(intent?.getBooleanExtra(EXTRA_STOP_SERVICE, false) == true) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        try {
            val entity = ParticipationEntity.getParticipatedExperimentFromLocal()

            if(entity.requiresAmbientSound && !exists<AmbientSoundCollector>()) put(AmbientSoundCollector( this))
            if(entity.requiresLocationAndActivity && !exists<LocationAndActivityCollector>()) put(LocationAndActivityCollector( this))
            if(entity.requiresGoogleFitness && !exists<GoogleFitnessCollector>()) put(GoogleFitnessCollector( this))
            if(entity.requiresEventAndTraffic && !exists<DeviceEventAndTrafficCollector>()) put(DeviceEventAndTrafficCollector( this))
            if(entity.requiresContentProviders && !exists<ContentProviderCollector>()) put(ContentProviderCollector( this))
            if(entity.requiresAppUsage && !exists<AppUsageCollector>()) put(AppUsageCollector( this))
            if(!exists<WeatherCollector>()) put(WeatherCollector( this))

            start(entity)
            LogEntity.log(TAG, "onStartCommand()")
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        stop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(SurveyManager.EventReceiver)
        LogEntity.log(TAG, "onDestroy()")
        super.onDestroy()
    }

    private inline fun <reified T : BaseCollector> exists() = collectors[T::class.java.name] != null

    private inline fun <reified T : BaseCollector> put(collector : T) {
        collectors[T::class.java.name] = collector
    }

    private fun start(entity: ParticipationEntity) {
        collectors.values.forEach { it.startCollection(uuid = entity.experimentUuid, group = entity.experimentGroup, email = entity.subjectEmail) }
    }

    private fun stop() {
        collectors.values.forEach { it.stopCollection() }
        collectors.clear()
    }
}



