package kaist.iclab.abc.data.entities

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import kaist.iclab.abc.App
import kaist.iclab.abc.common.NoNetworkAvailableException
import kaist.iclab.abc.common.NoParticipatedExperimentException
import kaist.iclab.abc.common.NoSignedAccountException
import kaist.iclab.abc.common.type.DayOfWeek
import kaist.iclab.abc.common.type.HourMin
import kaist.iclab.abc.common.type.HourMinRange
import kaist.iclab.abc.common.type.YearMonthDay
import kaist.iclab.abc.common.util.NetworkUtils
import kaist.iclab.abc.communication.GrpcApi
import kaist.iclab.abc.data.PreferenceAccessor
import kaist.iclab.abc.data.types.HourMinConverter
import kaist.iclab.abc.data.types.YearMonthDayConverter
import kaist.iclab.abc.protos.EnumProtos

@Entity
data class ParticipationEntity(
    @Id
    var id: Long = 0,

    var experimentUuid: String = "",

    var containsWeekend: Boolean = false,
    var durationInHour : Long = Long.MIN_VALUE,
    @Convert(converter = HourMinConverter::class, dbType = String::class)
    var dailyStartTime : HourMin = HourMin(0, 0),
    @Convert(converter = HourMinConverter::class, dbType = String::class)
    var dailyEndTime : HourMin = HourMin(24, 0),
    var requiresEventAndTraffic: Boolean = false,
    var requiresLocationAndActivity: Boolean = false,
    var requiresAmbientSound: Boolean = false,
    var requiresContentProviders: Boolean = false,
    var requiresAppUsage: Boolean = false,
    var requiresNotification: Boolean = false,
    var requiresGoogleFitness: Boolean = false,

    var participateTime: Long = Long.MIN_VALUE,
    var subjectEmail: String = "",
    var subjectPhoneNumber: String = "",
    var subjectName: String = "",
    var subjectAffiliation: String = "",
    @Convert(converter = YearMonthDayConverter::class, dbType = String::class)
    var subjectBirthDate: YearMonthDay = YearMonthDay.empty(),
    var subjectIsMale: Boolean = true,
    var survey : String = "",
    var experimentGroup: String = ""
) {
    fun checkValidTimeRange(now: Long) : Boolean {
        val timeRange = HourMinRange(dailyStartTime, dailyEndTime)
        if(!timeRange.isInRange(HourMin.fromMillis(now))) return false

        val dayOfWeek = DayOfWeek.fromMillis(now)
        if(!containsWeekend && dayOfWeek.isWeekend()) return false

        return true
    }

    companion object {
        fun getParticipatedExperimentFromLocal() : ParticipationEntity {
            val email = FirebaseAuth.getInstance().currentUser?.email ?: throw NoSignedAccountException()

            return App.boxFor<ParticipationEntity>().query()
                .equal(ParticipationEntity_.subjectEmail, email)
                .orderDesc(ParticipationEntity_.participateTime)
                .build().findFirst() ?: throw NoParticipatedExperimentException()
        }

        fun getParticipatedExperimentFromServer(context: Context) : ParticipationEntity {
            if(!NetworkUtils.isNetworkAvailable(context)) throw NoNetworkAvailableException()

            val email = FirebaseAuth.getInstance().currentUser?.email ?: throw NoSignedAccountException()
            val box = App.boxFor<ParticipationEntity>()
            var entity = try { ParticipationEntity.getParticipatedExperimentFromLocal() } catch (e: Exception) { ParticipationEntity() }
            val result = GrpcApi.getParticipatedExperiment(email)

            PreferenceAccessor.getInstance(context).isParticipated = result != null

            if(result == null) {
                if(entity.id > 0) box.remove(entity)
                throw NoParticipatedExperimentException()
            } else {
                entity = entity.copy(
                    id = entity.id,
                    experimentUuid = result.subject.experimentUuid,
                    containsWeekend = result.constraint.containsWeekend,
                    durationInHour = result.constraint.durationInHour,
                    dailyStartTime = HourMin(result.constraint.dailyStartTime.hour, result.constraint.dailyEndTime.min),
                    dailyEndTime = HourMin(result.constraint.dailyEndTime.hour, result.constraint.dailyEndTime.min),
                    requiresAppUsage = result.constraint.requiresAppUsage,
                    requiresNotification = result.constraint.requiresNotificationReceived,
                    requiresGoogleFitness = result.constraint.requiresGoogleFitness,
                    requiresAmbientSound = result.constraint.requiresAmbientSound,
                    requiresContentProviders = result.constraint.requiresContentProviders,
                    requiresLocationAndActivity = result.constraint.requiresLocationAndActivity,
                    requiresEventAndTraffic = result.constraint.requiresDeviceEventAndTraffic,
                    participateTime = result.subject.participatedTimestamp,
                    experimentGroup = result.subject.experimentGroup,
                    subjectEmail = result.subject.email,
                    survey = result.subject.survey,
                    subjectPhoneNumber = result.subject.phoneNumber,
                    subjectName = result.subject.name,
                    subjectAffiliation = result.subject.affiliation,
                    subjectBirthDate = YearMonthDay(result.subject.birthDate.year, result.subject.birthDate.month, result.subject.birthDate.day),
                    subjectIsMale = result.subject.gender == EnumProtos.GenderType.GENDER_MALE
                )
                box.put(entity)
                return entity
            }
        }
    }
}