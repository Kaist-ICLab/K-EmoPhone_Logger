package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Entity

/**
 * It represents a user'transitionScanClient current location
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property latitude latitude in GPS
 * @property longitude longitude in GPS
 * @property altitude altitude in GPS
 * @property accuracy accuracy in GPS
 * @property speed speed
 */

@Entity
data class LocationEntity (
    val latitude: Double = Double.MIN_VALUE,
    val longitude: Double = Double.MIN_VALUE,
    val altitude: Double = Double.MIN_VALUE,
    val accuracy: Float = Float.MIN_VALUE,
    val speed: Float = Float.MIN_VALUE
) : Base()