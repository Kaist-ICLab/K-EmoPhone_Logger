package kaist.iclab.abc.survey

import kaist.iclab.abc.common.type.EnumMap
import kaist.iclab.abc.common.type.HasId
import kaist.iclab.abc.common.type.buildValueMap

enum class SurveyQuestionType (override val id: Int) : HasId{
    NONE(0),
    SINGLE_TEXT(1),
    SINGLE_CHOICE(2),
    MULTIPLE_TEXTS(3),
    MULTIPLE_CHOICES(4),
    LIKERT(5);

    companion object: EnumMap<SurveyQuestionType>(buildValueMap())
}