package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity

import kaist.iclab.abc.data.types.AppUsageEventType
import kaist.iclab.abc.data.types.AppUsageEventTypeConverter

@Entity
data class AppUsageStatEntity (
    val name: String = "",
    val packageName: String = "",
    val isSystemApp: Boolean = false,
    val isUpdatedSystemApp: Boolean = false,
    val startTime: Long = Long.MIN_VALUE,
    val endTime: Long = Long.MIN_VALUE,
    val lastTimeUsed: Long = Long.MIN_VALUE,
    val totalTimeForeground: Long = Long.MIN_VALUE
) : Base()