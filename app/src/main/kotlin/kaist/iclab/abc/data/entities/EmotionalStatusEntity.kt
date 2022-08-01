package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Entity


@Entity
data class EmotionalStatusEntity(
    val anger: Float = Float.MIN_VALUE,
    val contempt: Float = Float.MIN_VALUE,
    val disgust: Float = Float.MIN_VALUE,
    val fear: Float = Float.MIN_VALUE,
    val happiness: Float = Float.MIN_VALUE,
    val neutral: Float = Float.MIN_VALUE,
    val sadness: Float = Float.MIN_VALUE,
    val surprise: Float = Float.MIN_VALUE
) : Base()