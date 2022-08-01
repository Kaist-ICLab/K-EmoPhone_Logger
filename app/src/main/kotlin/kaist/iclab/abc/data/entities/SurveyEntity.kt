package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abc.App
import kaist.iclab.abc.data.types.SurveyTimeConverter
import kaist.iclab.abc.data.types.SurveyTimeoutPolicyTypeConverter
import kaist.iclab.abc.survey.SurveyTime
import kaist.iclab.abc.survey.SurveyTimeoutPolicyType
import java.util.concurrent.TimeUnit

@Entity
data class SurveyEntity(
    val title: String = "",
    val message: String = "",
    val deliveredTime: Long = Long.MIN_VALUE,
    val reactionTime: Long = Long.MIN_VALUE,
    val firstQuestionTime: Long = Long.MIN_VALUE,
    val responses: String = "",
    @Convert(converter = SurveyTimeoutPolicyTypeConverter::class, dbType = String::class)
    val timeoutPolicy: SurveyTimeoutPolicyType = SurveyTimeoutPolicyType.NONE,
    @Convert(converter = SurveyTimeConverter::class, dbType = String::class)
    val timeout: SurveyTime = SurveyTime(Long.MIN_VALUE, TimeUnit.MILLISECONDS)
) : Base() {
    fun isEnableToResponed(now: Long) : Boolean {
        val isAfterTimeout = now - deliveredTime >= timeout.toMillis()
        return timestamp <= 0 && (!isAfterTimeout || timeoutPolicy != SurveyTimeoutPolicyType.DISABLED)
    }

    companion object {
        fun numberNotRepliedEntities(entity: ParticipationEntity, now: Long): Int {
            return App.boxFor<SurveyEntity>().query().filter {
                it.experimentUuid == entity.experimentUuid &&
                    it.subjectEmail == entity.subjectEmail &&
                    it.timestamp <= 0 &&
                    it.deliveredTime >= entity.participateTime &&
                    (now - it.deliveredTime < it.timeout.toMillis() || it.timeoutPolicy != SurveyTimeoutPolicyType.DISABLED)
            }.build().find().count()
        }
    }
}
