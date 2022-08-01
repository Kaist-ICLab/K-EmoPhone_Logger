package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Entity

@Entity
data class WeatherEntity (
    val latitude: Double = Double.MIN_VALUE,
    val longitude: Double = Double.MIN_VALUE,
    val temperature: Float = Float.MIN_VALUE,
    val rainfall: Float = Float.MIN_VALUE,
    val sky: String = "",
    val windEw: Float = Float.MIN_VALUE,
    val windNs: Float = Float.MIN_VALUE,
    val humidity: Float = Float.MIN_VALUE,
    val rainType: String = "",
    val lightning: String = "",
    val windSpeed: Float = Float.MIN_VALUE,
    val windDirection: Float = Float.MIN_VALUE,
    val so2Value: Float = Float.MIN_VALUE,
    val so2Grade: Float = Float.MIN_VALUE,
    val coValue: Float = Float.MIN_VALUE,
    val coGrade: Float = Float.MIN_VALUE,
    val no2Value: Float = Float.MIN_VALUE,
    val no2Grade: Float = Float.MIN_VALUE,
    val o3Value: Float = Float.MIN_VALUE,
    val o3Grade: Float = Float.MIN_VALUE,
    val pm10Value: Float = Float.MIN_VALUE,
    val pm10Grade: Float = Float.MIN_VALUE,
    val pm25Value: Float = Float.MIN_VALUE,
    val pm25Grade: Float = Float.MIN_VALUE,
    val airValue: Float = Float.MIN_VALUE,
    val airGrade: Float = Float.MIN_VALUE
) : Base()