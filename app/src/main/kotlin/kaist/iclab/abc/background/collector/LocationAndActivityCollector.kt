package kaist.iclab.abc.background.collector

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.arch.lifecycle.MutableLiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.location.*
import kaist.iclab.abc.App
import kaist.iclab.abc.background.Status
import kaist.iclab.abc.common.util.PermissionUtils
import kaist.iclab.abc.common.util.Utils
import kaist.iclab.abc.data.entities.LocationEntity
import kaist.iclab.abc.data.entities.PhysicalActivityEventEntity
import kaist.iclab.abc.data.entities.PhysicalActivityTransitionEntity
import kaist.iclab.abc.data.entities.WifiEntity
import kaist.iclab.abc.data.types.PhysicalActivityType
import kaist.iclab.abc.data.types.PhysicalActivityTransitionType
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class LocationAndActivityCollector(val context: Context) : BaseCollector {
    private var scheduledFuture: ScheduledFuture<*>? = null
    private var isCollecting = false

    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val activityClient: ActivityRecognitionClient = ActivityRecognition.getClient(context)
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val locationUpdateIntent: PendingIntent = PendingIntent.getBroadcast(context,
        REQUEST_CODE_LOCATION_UPDATE, Intent(ACTION_LOCATION_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT)

    private var activityUpdateIntent: PendingIntent = PendingIntent.getBroadcast(context,
        REQUEST_CODE_ACTIVITY_UPDATE, Intent(ACTION_ACTIVITY_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT)

    private var activityTransitionUpdateIntent: PendingIntent = PendingIntent.getBroadcast(context,
        REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE, Intent(ACTION_ACTIVITY_TRANSITION_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT)


    private val intentFilter = IntentFilter().apply {
        addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        addAction(ACTION_ACTIVITY_TRANSITION_UPDATE)
        addAction(ACTION_ACTIVITY_UPDATE)
        addAction(ACTION_LOCATION_UPDATE)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return

            extractLocationEntity(intent)?.let {
                App.boxFor<LocationEntity>().put(it)
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, email = ${it.subjectEmail}, uuid = ${it.experimentUuid}, " +
                    "group = ${it.experimentGroup}, entity = $it)")
            }

            extractPhysicalActivityEntity(intent)?.let {entities ->
                App.boxFor<PhysicalActivityEventEntity>().put(entities)
                entities.forEach {
                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${it.timestamp}, email = ${it.subjectEmail}, uuid = ${it.experimentUuid}, " +
                        "group = ${it.experimentGroup}, entity = $it)")
                }
            }

            extractWifiEntity(intent)?.let {entities ->
                App.boxFor<WifiEntity>().put(entities)

                entities.forEach {
                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${it.timestamp}, email = ${it.subjectEmail}, uuid = ${it.experimentUuid}, " +
                        "group = ${it.experimentGroup}, entity = $it)")
                }
            }

            extractPhysicalActivityTransition(intent)?.let { entities ->
                App.boxFor<PhysicalActivityTransitionEntity>().put(entities)

                entities.forEach { entity ->
                    Log.d(TAG, "Box.put(" +
                        "timestamp = ${entity.timestamp}, email = ${entity.subjectEmail}, uuid = ${entity.experimentUuid}, " +
                        "group = ${entity.experimentGroup}, entity = $entity)")

                    when(entity.transitionType) {
                        PhysicalActivityTransitionType.ENTER_STILL -> {
                            try {
                                collectLocation(MAX_PERIOD_LOCATION_IN_MS, MAX_PERIOD_WIFI_IN_MS)
                            } catch (e: SecurityException) {
                                stopCollection()
                                status.postValue(Status.ABORTED(e))
                            } catch (e: Exception) { }
                        }
                        PhysicalActivityTransitionType.EXIT_STILL -> {
                            try {
                                collectLocation(MIN_PERIOD_LOCATION_IN_MS, MIN_PERIOD_WIFI_IN_MS)
                            } catch (e: SecurityException) {
                                stopCollection()
                                status.postValue(Status.ABORTED(e))
                            } catch (e: Exception) { }
                        }
                        else -> { }
                    }
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                    Intent(ACTION_ACTIVITY_TRANSITION_AVAILABLE)
                        .putExtra(EXTRA_ACTIVITY_TRANSITIONS, entities.map { it.transitionType.name }.toTypedArray())
                )
            }
        }
    }

    private lateinit var uuid: String
    private lateinit var group: String
    private lateinit var email: String

    override fun startCollection(uuid: String, group: String, email: String) {
        if(isCollecting) return
        isCollecting = true

        status.postValue(Status.STARTED)

        try {
            this.uuid = uuid
            this.group = group
            this.email = email
            context.registerReceiver(receiver, intentFilter)

            collectActivity()
            collectLocation(MAX_PERIOD_LOCATION_IN_MS, MAX_PERIOD_WIFI_IN_MS)

            status.postValue(Status.RUNNING)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is SecurityException) {
                stopCollection()
                status.postValue(Status.ABORTED(e))
            }
        }
    }

    override fun stopCollection() {
        if(!isCollecting) return

        isCollecting = false
        context.unregisterReceiver(receiver)

        activityClient.removeActivityUpdates(activityUpdateIntent)
        activityClient.removeActivityTransitionUpdates(activityTransitionUpdateIntent)
        locationClient.removeLocationUpdates(locationUpdateIntent)
        scheduledFuture?.cancel(true)

        status.postValue(Status.CANCELED)
    }

    private fun collectActivity() {
        if(!checkEnableToCollect(context)) throw SecurityException("Activity is not granted to be collected.")

        activityClient.removeActivityUpdates(activityUpdateIntent)
            .onSuccessTask { activityClient.removeActivityTransitionUpdates(activityTransitionUpdateIntent) }
            .onSuccessTask { activityClient.requestActivityUpdates(PERIOD_ACTIVITY_IN_MS, activityUpdateIntent) }
            .onSuccessTask {  activityClient.requestActivityTransitionUpdates(
                ActivityTransitionRequest(
                    PhysicalActivityTransitionType.values().mapNotNull { type ->
                        return@mapNotNull if(type != PhysicalActivityTransitionType.NONE) {
                            ActivityTransition.Builder()
                                .setActivityTransition(type.row)
                                .setActivityType(type.col)
                                .build()
                        } else {
                            null
                        }
                    }
                ), activityTransitionUpdateIntent)
            }
            .addOnSuccessListener { status.postValue(Status.RUNNING) }
            .addOnFailureListener {
                stopCollection()
                it.printStackTrace()
                status.postValue(Status.ABORTED(it))
            }
    }

    @SuppressLint("MissingPermission")
    private fun collectLocation(locationPeriod: Long, wifiPeriod: Long) {
        if(!checkEnableToCollect(context)) throw SecurityException("Location is not granted to be collected.")

        locationClient.removeLocationUpdates(locationUpdateIntent)
            .onSuccessTask { locationClient.requestLocationUpdates(LocationRequest.create().setInterval(locationPeriod).setSmallestDisplacement(5.0F), locationUpdateIntent) }
            .addOnSuccessListener { status.postValue(Status.RUNNING) }
            .addOnFailureListener {
                stopCollection()
                it.printStackTrace()
                status.postValue(Status.ABORTED(it))
            }

        scheduledFuture?.cancel(true)
        scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            try {
                if(!checkEnableToCollect(context)) throw SecurityException("WiFi is not granted to be collected.")
                if(wifiManager.isWifiEnabled) wifiManager.startScan()
                status.postValue(Status.RUNNING)
            } catch (e: Exception) {
                e.printStackTrace()
                if(e is SecurityException) {
                    stopCollection()
                    status.postValue(Status.ABORTED(e))
                }
            }
        }, 5000, wifiPeriod, TimeUnit.MILLISECONDS)
    }

    private fun extractLocationEntity(intent: Intent): LocationEntity? {
        if (intent.action != ACTION_LOCATION_UPDATE && !LocationResult.hasResult(intent)) {
            return null
        }

        return LocationResult.extractResult(intent)?.lastLocation?.let {
            LocationEntity(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                accuracy = it.accuracy,
                speed = it.speed
            ).apply {
                timestamp = it.time
                utcOffset = Utils.utcOffsetInHour()
                experimentUuid = uuid
                experimentGroup = group
                subjectEmail = email
                isUploaded = false
            }
        }
    }

    private fun extractPhysicalActivityEntity(intent: Intent): List<PhysicalActivityEventEntity>? {
        if (intent.action != ACTION_ACTIVITY_UPDATE || !ActivityRecognitionResult.hasResult(intent)) {
            return null
        }

        return ActivityRecognitionResult.extractResult(intent)?.let { result ->
            return@let result.probableActivities.asSequence().map {
                PhysicalActivityEventEntity(
                    type = PhysicalActivityType.fromValue(it.type, PhysicalActivityType.UNKNOWN),
                    confidence = it.confidence.toFloat() / 100
                ).apply {
                    timestamp = result.time
                    utcOffset = Utils.utcOffsetInHour()
                    experimentUuid = uuid
                    experimentGroup = group
                    subjectEmail = email
                    isUploaded = false
                }
            }.toList()
        }
    }

    private fun extractPhysicalActivityTransition(intent: Intent): List<PhysicalActivityTransitionEntity>? {
        if (intent.action != ACTION_ACTIVITY_TRANSITION_UPDATE || !ActivityTransitionResult.hasResult(intent)) {
            return null
        }

        val now = System.currentTimeMillis()

        return ActivityTransitionResult.extractResult(intent)?.transitionEvents?.asSequence()?.sortedByDescending { it.elapsedRealTimeNanos }?.map {
            PhysicalActivityTransitionEntity(
                transitionType = PhysicalActivityTransitionType.fromValue(it.transitionType, it.activityType, PhysicalActivityTransitionType.NONE)
            ).apply {
                timestamp = now
                utcOffset = Utils.utcOffsetInHour()
                experimentUuid = uuid
                experimentGroup = group
                subjectEmail = email
                isUploaded = false
            }
        }?.toList()
    }

    private fun extractWifiEntity(intent: Intent) : List<WifiEntity>? {
        if (intent.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return null

        val time = System.currentTimeMillis()

        return wifiManager.scanResults?.asSequence()?.map {
            WifiEntity(
                frequency = it.frequency,
                rssi = it.level,
                bssid = it.BSSID,
                ssid = it.BSSID
            ).apply {
                timestamp = time
                utcOffset = Utils.utcOffsetInHour()
                experimentUuid = uuid
                experimentGroup = group
                subjectEmail = email
                isUploaded = false
            }
        }?.toList()
    }

    companion object {
        private val TAG : String = LocationAndActivityCollector::class.java.simpleName
        
        val ACTION_ACTIVITY_TRANSITION_AVAILABLE = "${LocationAndActivityCollector::class.java.canonicalName}.ACTION_ACTIVITY_TRANSITION_AVAILABLE"
        val EXTRA_ACTIVITY_TRANSITIONS = "${LocationAndActivityCollector::class.java.canonicalName}.EXTRA_ACTIVITY_TRANSITIONS"

        private const val ACTION_LOCATION_UPDATE = "ACTION_LOCATION_UPDATE"
        private const val ACTION_ACTIVITY_UPDATE = "ACTION_ACTIVITY_UPDATE"
        private const val ACTION_ACTIVITY_TRANSITION_UPDATE = "ACTION_ACTIVITY_TRANSITION_UPDATE"
        private const val REQUEST_CODE_LOCATION_UPDATE = 0xff
        private const val REQUEST_CODE_ACTIVITY_UPDATE = 0xfe
        private const val REQUEST_CODE_ACTIVITY_TRANSITION_UPDATE = 0xfd

        private const val MIN_PERIOD_LOCATION_IN_MS = 1000 * 5L
        private const val MAX_PERIOD_LOCATION_IN_MS = 1000 * 60 * 5L
        private const val MIN_PERIOD_WIFI_IN_MS = 1000 * 15L
        private const val MAX_PERIOD_WIFI_IN_MS = 1000 * 60 * 5L
        private const val PERIOD_ACTIVITY_IN_MS = 1000 * 15L

        fun checkEnableToCollect(context: Context) = PermissionUtils.checkPermissionAtRuntime(context, REQUIRED_PERMISSIONS)

        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }
    }
}