package kaist.iclab.abc.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import io.objectbox.kotlin.boxFor
import kaist.iclab.abc.App
import kaist.iclab.abc.common.util.Utils
import kaist.iclab.abc.data.entities.DeviceEventEntity
import kaist.iclab.abc.data.entities.ParticipationEntity
import kaist.iclab.abc.data.types.DeviceEventType
import kaist.iclab.abc.foreground.activity.AvoidSmartManagerActivity
import java.util.*


class BootReceiver: BroadcastReceiver() {
    companion object {
        const val PACKAGE_NAME_SMART_MANAGER = "com.samsung.android.sm"
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ) {
            val experiment = try { ParticipationEntity.getParticipatedExperimentFromLocal() } catch (e: Exception) { null }
            if(experiment != null) {
                App.boxFor<DeviceEventEntity>().put(
                    DeviceEventEntity(DeviceEventType.TURN_ON_DEVICE).apply {
                        timestamp = System.currentTimeMillis()
                        utcOffset = Utils.utcOffsetInHour()
                        subjectEmail = experiment.subjectEmail
                        experimentUuid = experiment.experimentUuid
                        experimentGroup = experiment.experimentGroup
                    }
                )
            }

            SyncManager.sync(true)

            try {
                context.packageManager.getPackageInfo(PACKAGE_NAME_SMART_MANAGER, PackageManager.GET_META_DATA)

                Handler().postDelayed( {
                    context.startActivity(Intent(context, AvoidSmartManagerActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })

                }, Random(System.currentTimeMillis()).nextInt(3000).toLong())
            } catch (e: PackageManager.NameNotFoundException) { }
        }
    }
}
