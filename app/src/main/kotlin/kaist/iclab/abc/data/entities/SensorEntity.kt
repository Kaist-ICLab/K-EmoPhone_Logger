package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Entity

@Entity
data class SensorEntity(
    val type: String = "",
    val firstValue: Float = Float.MIN_VALUE,
    val secondValue: Float = Float.MIN_VALUE,
    val thirdValue: Float = Float.MIN_VALUE,
    val fourthValue: Float = Float.MIN_VALUE
) : Base()