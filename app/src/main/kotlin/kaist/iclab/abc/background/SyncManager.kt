package kaist.iclab.abc.background

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import androidx.work.Worker
import io.objectbox.Box
import io.objectbox.EntityInfo
import kaist.iclab.abc.App
import kaist.iclab.abc.R
import kaist.iclab.abc.common.NoParticipatedExperimentException
import kaist.iclab.abc.common.NoWifiNetworkAvailableException
import kaist.iclab.abc.common.util.*
import kaist.iclab.abc.communication.GrpcApi
import kaist.iclab.abc.data.PreferenceAccessor
import kaist.iclab.abc.data.entities.*
import java.io.File

object SyncManager {
    val TAG: String = SyncManager::class.java.simpleName

    private const val LIMIT: Long = 60
    private const val THREE_DAYS_IN_MS: Long = 1000 * 60 * 60 * 24 * 3

    fun sync(isForced: Boolean = false) {
        WorkerUtils.startPeriodicWorkerAsync<SyncWorker>(1000 * 60 * 15, isForced)
    }

    fun syncWithProgressShown(context: Context) {
        context.startService(Intent(context, SyncService::class.java))
    }


    class SyncWorker: Worker() {
        override fun doWork(): Result {
            sync(applicationContext, false)
            return Result.SUCCESS
        }
    }

    class SyncService: IntentService(TAG) {
        override fun onHandleIntent(intent: Intent?) {
            sync(this, true)
        }
    }

    private fun sync(context: Context, showProgress: Boolean) {
        val pref = PreferenceAccessor.getInstance(context)
        pref.isSyncInProgress = true

        try {
            ParticipationEntity.getParticipatedExperimentFromServer(context)
        } catch (e: NoParticipatedExperimentException) {
            pref.clear()
        } catch (e: Exception) { }

        try {
            val unitProgress = 100 / 21
            var progress = 0
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * progress, 100)

            uploadLogs()
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<AppUsageEventEntity>(), AppUsageEventEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<AppUsageStatEntity>(), AppUsageStatEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<BatteryEntity>(), BatteryEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<CallLogEntity>(), CallLogEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<ConnectivityEntity>(), ConnectivityEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<DataTrafficEntity>(), DataTrafficEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<DeviceEventEntity>(), DeviceEventEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<EmotionalStatusEntity>(), EmotionalStatusEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<InstalledAppEntity>(), InstalledAppEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<LocationEntity>(), LocationEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<MediaEntity>(), MediaEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<MessageEntity>(), MessageEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<NotificationEntity>(), NotificationEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<PhysicalActivityEventEntity>(), PhysicalActivityEventEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<PhysicalStatusEntity>(), PhysicalStatusEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<PhysicalActivityTransitionEntity>(), PhysicalActivityTransitionEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<RecordEntity>(), RecordEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<SensorEntity>(), SensorEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<WeatherEntity>(), WeatherEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<WifiEntity>(), WifiEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context,unitProgress * (++progress), 100)

            uploadEntity(context, App.boxFor<SurveyEntity>(), SurveyEntity_.__INSTANCE)
            if(showProgress) NotificationUtils.notifyUploadProgress(context, 0, 0)

            pref.lastTimeSynced = System.currentTimeMillis()
        } catch (e: Exception) {
            if(showProgress) NotificationUtils.notifyUploadProgress(context, 0, 0, e)
        } finally {
            pref.isSyncInProgress = false
        }
    }

    private fun uploadLogs() {
        val box = App.boxFor<LogEntity>()
        val uploadQuery = box.query()
            .equal(LogEntity_.isUploaded, false)
            .build()

        while(uploadQuery.count() > 0) {
            uploadQuery.find(0, LIMIT).let { entities ->
                GrpcApi.uploadLog(entities)
                entities.forEach { it.isUploaded = true }
                box.put(entities)
            }
        }

        val removeQuery = box.query()
            .equal(LogEntity_.isUploaded, true)
            .build()
        removeQuery.remove()
    }

    private inline fun <reified T: Base> uploadEntity(context: Context, box: Box<T>, info: EntityInfo<T>) {
        if(!NetworkUtils.isWifiAvailable(context)) {
            throw NoWifiNetworkAvailableException()
        }
        Log.d(TAG, "uploadEntity: ${T::class.java.simpleName}")

        FunctionUtils.runIfAllNotNull(
            info.allProperties.find { it.name == "timestamp" },
            info.allProperties.find { it.name == "isUploaded" }
        ) { timestampProperty, isUploadedProperty ->
            val uploadQuery = box.query()
                .greater(timestampProperty, 0)
                .and()
                .equal(isUploadedProperty, false)
                .build()

            while (uploadQuery.count() > 0) {
                uploadQuery.find(0, LIMIT).let { entities ->
                    GrpcApi.uploadEntities(entities)
                    entities.forEach { it.isUploaded = true }
                    box.put(entities)
                }
            }

            if(T::class.java == SurveyEntity::class.java) return@runIfAllNotNull

            val removeQuery = box.query()
                .less(timestampProperty, System.currentTimeMillis() - THREE_DAYS_IN_MS)
                .and()
                .equal(isUploadedProperty, true)
                .build()

            removeQuery.forEach {
                if(it is RecordEntity) {
                    val file = File(it.path)
                    if(file.exists()) file.delete()
                }
            }
            removeQuery.remove()
        }
    }
}
