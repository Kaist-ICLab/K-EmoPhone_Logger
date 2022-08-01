package kaist.iclab.abc.data.entities


import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abc.data.types.*

/**
 * It represents a single item of SMS/MMS logs
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property number a phone number
 * @property messageClass MMS or SMS
 * @property messageBox message box type (e.g., inbox, outbox, draft, etc.)
 * @property contact a contact type
 * @property timesContacted a number of times that a user has contacted to this number
 * @property isStarred indicates this phone number is favorite or not
 * @property isPinned indicates this phone number is pinned
 */

@Entity
data class MessageEntity (
    val number: String = "",
    @Convert(converter = MessageClassTypeConverter::class, dbType = String::class)
    val messageClass: MessageClassType = MessageClassType.UNDEFINED,
    @Convert(converter = MessageBoxTypeConverter::class, dbType = String::class)
    val messageBox: MessageBoxType = MessageBoxType.UNDEFINED,
    @Convert(converter = ContactTypeConverter::class, dbType = String::class)
    val contact: ContactType = ContactType.UNDEFINED,
    val timesContacted: Int = Int.MIN_VALUE,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false
) : Base()