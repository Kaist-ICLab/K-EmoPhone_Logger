package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abc.data.types.PhysicalActivityType
import kaist.iclab.abc.data.types.PhysicalActivityTypeConverter

/**
 * It represents a user'transitionScanClient current activity (see https://developers.google.com/android/reference/com/google/android/gms/location/DetectedActivity)
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property type Physical activity questionType
 * @property confidence a confidence (or probability)
 */

@Entity
data class PhysicalActivityEventEntity (
    @Convert(converter = PhysicalActivityTypeConverter::class, dbType = String::class)
    val type: PhysicalActivityType = PhysicalActivityType.UNKNOWN,
    val confidence: Float = 0F
) : Base()