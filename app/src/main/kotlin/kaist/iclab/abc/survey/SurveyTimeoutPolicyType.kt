package kaist.iclab.abc.survey

import kaist.iclab.abc.common.type.EnumMap
import kaist.iclab.abc.common.type.HasId
import kaist.iclab.abc.common.type.buildValueMap

enum class SurveyTimeoutPolicyType (override val id: Int): HasId {
    NONE(0),
    DISABLED(1),
    ALTERNATIVE_TEXT(2);

    companion object: EnumMap<SurveyTimeoutPolicyType>(buildValueMap())
}