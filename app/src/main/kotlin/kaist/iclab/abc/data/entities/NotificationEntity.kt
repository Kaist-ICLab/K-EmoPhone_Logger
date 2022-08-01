package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abc.data.types.NotificationVisibilityType
import kaist.iclab.abc.data.types.NotificationVisibilityTypeConverter

/**
 * It represents a notification info. that a user receives.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property packageName package name identifier that a user currently interacts with
 * @property isSystemApp indicates that the app is installed by a system (i.e., default app)
 * @property isUpdatedSystemApp indicates that the app is installed by a system and has been updated
 * @property title a title of the notification
 * @property visibility a visibility in the lock screen
 * @property category a category information (see https://developer.android.com/reference/android/app/Notification#category)
 * @property hasVibration indicates that a phone is vibrated or not when the notification is received
 * @property lightColor LED light colours
 * @property hasSound indicates that a phone plays some sound when the notification is received
 */

@Entity
data class NotificationEntity (
    val name: String = "",
    val packageName: String = "",
    val isSystemApp: Boolean = false,
    val isUpdatedSystemApp: Boolean = false,
    val title: String = "",
    @Convert(converter = NotificationVisibilityTypeConverter::class, dbType = String::class)
    val visibility: NotificationVisibilityType = NotificationVisibilityType.UNDEFINED,
    val category: String = "",
    val hasVibration: Boolean = false,
    val hasSound: Boolean = false,
    val lightColor: String = "",
    val key: String = "",
    val isPosted: Boolean = false
) : Base()