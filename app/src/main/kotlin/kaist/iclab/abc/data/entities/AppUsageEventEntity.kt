package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity

import kaist.iclab.abc.data.types.AppUsageEventType
import kaist.iclab.abc.data.types.AppUsageEventTypeConverter


@Entity
data class AppUsageEventEntity (
    val name: String = "",
    val packageName: String = "",
    @Convert(converter = AppUsageEventTypeConverter::class, dbType = String::class)
    val type: AppUsageEventType = AppUsageEventType.NONE,
    val isSystemApp: Boolean = false,
    val isUpdatedSystemApp: Boolean = false
) : Base()