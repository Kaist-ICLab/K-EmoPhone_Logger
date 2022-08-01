package kaist.iclab.abc.common.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import kaist.iclab.abc.background.collector.*
import kaist.iclab.abc.common.RuntimeAppUsageDeniedException
import kaist.iclab.abc.common.RuntimeGoogleFitnessDeniedException
import kaist.iclab.abc.common.RuntimeNotificationAccessDeniedException
import kaist.iclab.abc.common.RuntimePermissionDeniedException
import kaist.iclab.abc.data.entities.ParticipationEntity

object PermissionUtils {
    fun throwExceptionForCurrentPermission(context: Context) {
        val entity = try { ParticipationEntity.getParticipatedExperimentFromLocal() } catch (e: Exception) { null }
        if(entity != null) {
            val permission = listOfNotNull(
                if (entity.requiresLocationAndActivity) LocationAndActivityCollector.REQUIRED_PERMISSIONS else null,
                if (entity.requiresAmbientSound) AmbientSoundCollector.REQUIRED_PERMISSIONS else null,
                if (entity.requiresContentProviders) ContentProviderCollector.REQUIRED_PERMISSIONS else null,
                if (entity.requiresGoogleFitness) GoogleFitnessCollector.REQUIRED_PERMISSIONS else null,
                listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ).flatten().toSet()

            if(!PermissionUtils.checkPermissionAtRuntime(context, permission)) throw RuntimePermissionDeniedException(permission.toTypedArray())
            if(entity.requiresAppUsage && !AppUsageCollector.checkEnableToCollect(context)) throw RuntimeAppUsageDeniedException()
            if(entity.requiresNotification && !NotificationCollector.checkEnableToCollect(context)) throw RuntimeNotificationAccessDeniedException()
            if(entity.requiresGoogleFitness && !GoogleFitnessCollector.checkEnableToCollect(context)) throw RuntimeGoogleFitnessDeniedException()
        }
    }

    fun checkPermissionAtRuntime(context: Context, permissions: Collection<String>) : Boolean {
        return if(permissions.isEmpty()) {
            true
        } else {permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun getPermissionGroupLabels(context: Context) : List<String>? {
        val pm = context.packageManager

        val groups = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)?.requestedPermissions?.mapNotNull {permission ->
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return@mapNotNull null
            }

            try {
                val permissionInfo = pm.getPermissionInfo(permission, 0)
                if(permissionInfo?.group != null) {
                    return@mapNotNull pm.getPermissionGroupInfo(permissionInfo.group, 0).loadLabel(pm).toString()
                }
            } catch (e: Exception) { }
            return@mapNotNull null
        }?.toSet()?.toList()

        if(groups?.isEmpty() == true)
            return null

        return groups
    }
}