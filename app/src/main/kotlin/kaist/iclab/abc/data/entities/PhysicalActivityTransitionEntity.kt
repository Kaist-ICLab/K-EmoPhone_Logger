package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abc.data.types.PhysicalActivityTransitionType
import kaist.iclab.abc.data.types.PhysicalActivityTransitionTypeConverter

@Entity
data class PhysicalActivityTransitionEntity(
    @Convert(converter = PhysicalActivityTransitionTypeConverter::class, dbType = String::class)
    val transitionType: PhysicalActivityTransitionType = PhysicalActivityTransitionType.NONE
) : Base()