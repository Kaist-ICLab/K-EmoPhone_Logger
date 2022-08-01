package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Entity

@Entity
data class PhysicalStatusEntity (
    val activity: String = "",
    val type: String = "",
    val startTime: Long = Long.MIN_VALUE,
    val endTime: Long = Long.MIN_VALUE,
    val value: Float = Float.MIN_VALUE
) : Base()