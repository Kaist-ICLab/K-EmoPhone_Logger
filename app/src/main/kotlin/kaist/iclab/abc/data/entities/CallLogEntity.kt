package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abc.data.types.*

/**
 * It represents a single item in call logs.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property duration a duration of call time
 * @property number a phone number
 * @property type a call type (e.g., MISSED, OUTGOING, INCOMING)
 * @property presentation a presentation type that is shown in a screen when a call incomes.
 * @property dataUsage an amount of data usage when any data-related call (e.g., video call)
 * @property contact a contact type
 * @property timesContacted a number of times that a user has contacted to this number
 * @property isStarred indicates this phone number is favorite or not
 * @property isPinned indicates this phone number is pinned
 */

@Entity
data class CallLogEntity (
    val duration: Long = Long.MIN_VALUE,
    val number: String = "",
    @Convert(converter = CallTypeConverter::class, dbType = String::class)
    val type: CallType = CallType.UNDEFINED,
    @Convert(converter = CallPresentationTypeConverter::class, dbType = String::class)
    val presentation: CallPresentationType = CallPresentationType.UNDEFINED,
    val dataUsage: Long = Long.MIN_VALUE,

    @Convert(converter = ContactTypeConverter::class, dbType = String::class)
    val contact: ContactType = ContactType.UNDEFINED,
    val timesContacted: Int = Int.MIN_VALUE,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false
): Base()
