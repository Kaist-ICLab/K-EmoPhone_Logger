package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Entity

/**
 * It represents a data usage.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis when measurement ends.
 * @property utcOffset Offset time
 * @property duration duration
 * @property rxKiloBytes the data received in kilo bytes
 * @property txKiloBytes the data transmitted in kilo bytes
 */
@Entity
data class DataTrafficEntity (
    val duration: Long = Long.MIN_VALUE,
    val rxKiloBytes: Long = Long.MIN_VALUE,
    val txKiloBytes: Long = Long.MIN_VALUE
) : Base()
