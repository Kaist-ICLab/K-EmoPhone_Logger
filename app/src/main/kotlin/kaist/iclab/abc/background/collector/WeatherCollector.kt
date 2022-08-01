package kaist.iclab.abc.background.collector

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import io.objectbox.Box
import kaist.iclab.abc.App
import kaist.iclab.abc.background.Status
import kaist.iclab.abc.common.util.NetworkUtils
import kaist.iclab.abc.common.util.Utils
import kaist.iclab.abc.communication.GrpcApi
import kaist.iclab.abc.data.entities.LocationEntity
import kaist.iclab.abc.data.entities.LocationEntity_
import kaist.iclab.abc.data.entities.WeatherEntity
import kaist.iclab.abc.data.entities.WeatherEntity_
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class WeatherCollector(val context: Context) : BaseCollector {
    private var scheduledFuture: ScheduledFuture<*>? = null

    override fun startCollection(uuid: String, group: String, email: String) {
        if(scheduledFuture?.isDone == false) return

        status.postValue(Status.STARTED)
        scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            try {
                if(NetworkUtils.isNetworkAvailable(context)) {
                    collect(uuid, group, email)
                }
            } catch (e: Exception) { }
        }, 0, 15, TimeUnit.MINUTES)
    }

    override fun stopCollection() {
        scheduledFuture?.cancel(true)
        status.postValue(Status.CANCELED)
    }

    private fun collect(uuid: String, group: String, email: String) {
        status.postValue(Status.RUNNING)

        val locationBox: Box<LocationEntity> = App.boxFor()
        val weatherBox: Box<WeatherEntity> = App.boxFor()

        val now = System.currentTimeMillis()
        val currentHour = GregorianCalendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = now }.get(GregorianCalendar.HOUR_OF_DAY)

        for(i in MIN_HOUR_OF_DAY..Math.min(MAX_HOUR_OF_DAY, currentHour)) {
            val from = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = now
                set(GregorianCalendar.HOUR_OF_DAY, i)
                set(GregorianCalendar.MINUTE, 0)
                set(GregorianCalendar.SECOND, 0)
                set(GregorianCalendar.MILLISECOND, 0)
            }.timeInMillis

            val to = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = now
                set(GregorianCalendar.HOUR_OF_DAY, i)
                set(GregorianCalendar.MINUTE, 59)
                set(GregorianCalendar.SECOND, 0)
                set(GregorianCalendar.MILLISECOND, 0)
            }.timeInMillis

            val hasWeather = weatherBox.query().filter {
                it.timestamp in from..to
            }.build().find().count() > 0

            if(!hasWeather) {
                val lastLocation = locationBox.query()
                    .between(LocationEntity_.timestamp, from, to)
                    .orderDesc(LocationEntity_.timestamp)
                    .build().findFirst()

                lastLocation?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val locationCal = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                        timeInMillis = location.timestamp
                    }
                    GrpcApi.retrieveWeather(
                        latitude = latitude,
                        longitude = longitude,
                        year = locationCal.get(Calendar.YEAR),
                        month = locationCal.get(Calendar.MONTH) + 1,
                        day = locationCal.get(Calendar.DAY_OF_MONTH),
                        hour = locationCal.get(Calendar.HOUR_OF_DAY)
                    ).let {
                        val entity = WeatherEntity(
                            latitude = latitude,
                            longitude = latitude,
                            temperature = it.weather.temperature,
                            rainfall = it.weather.rainfall,
                            sky = it.weather.sky,
                            windEw = it.weather.windEw,
                            windNs = it.weather.windNs,
                            humidity = it.weather.humidity,
                            rainType = it.weather.rainType,
                            lightning = it.weather.lightning,
                            windSpeed = it.weather.windSpeed,
                            windDirection = it.weather.windDirection,
                            so2Value = it.weather.so2Value,
                            so2Grade = it.weather.so2Grade,
                            coValue = it.weather.coValue,
                            coGrade = it.weather.coGrade,
                            no2Value = it.weather.no2Value,
                            no2Grade = it.weather.no2Grade,
                            o3Value = it.weather.o3Value,
                            o3Grade = it.weather.o3Grade,
                            pm10Value = it.weather.pm10Value,
                            pm10Grade = it.weather.pm10Grade,
                            pm25Value = it.weather.pm25Value,
                            pm25Grade = it.weather.pm25Grade,
                            airValue = it.weather.airValue,
                            airGrade = it.weather.airGrade
                        ).apply {
                            timestamp = it.time.timestamp
                            utcOffset = Utils.utcOffsetInHour()
                            subjectEmail = email
                            experimentUuid = uuid
                            experimentGroup = group
                            isUploaded = false
                        }
                        weatherBox.put(entity)
                        Log.d(TAG, "Box.put(" +
                            "timestamp = ${entity.timestamp}, subjectEmail = ${entity.subjectEmail}, experimentUuid = ${entity.experimentUuid}, " +
                            "experimentGroup = ${entity.experimentGroup}, entity = $entity)")
                    }
                }
            }
        }
    }

    companion object {
        private val TAG : String = WeatherCollector::class.java.simpleName
        
        private const val MIN_HOUR_OF_DAY = 6
        private const val MAX_HOUR_OF_DAY = 21

        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }
    }
}