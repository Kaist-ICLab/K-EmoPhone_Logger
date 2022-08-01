package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Entity

@Entity
data class InstalledAppEntity (
    val name: String = "",
    val packageName: String = "",
    val isSystemApp: Boolean = false,
    val isUpdatedSystemApp: Boolean = false,
    val firstInstallTime : Long = Long.MIN_VALUE,
    val lastUpdateTime: Long = Long.MIN_VALUE
) : Base()