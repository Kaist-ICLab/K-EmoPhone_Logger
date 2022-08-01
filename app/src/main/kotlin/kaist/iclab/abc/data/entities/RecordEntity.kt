package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Entity

@Entity
data class RecordEntity(
    val sampleRate: Int = Int.MIN_VALUE,
    val channelMask: String = "",
    val encoding: String = "",
    val path: String = "",
    val duration: Long = Long.MIN_VALUE
) : Base()