package kaist.iclab.abc

import kaist.iclab.abc.common.type.DayOfWeek
import kaist.iclab.abc.common.type.HourMin
import kaist.iclab.abc.common.type.HourMinRange
import kaist.iclab.abc.survey.SurveyPolicy
import org.junit.Test
import java.util.*

class SurveyTriggerTest {
    private fun buildDefaultSurveyPolicy() = SurveyPolicy(
        dailySurveyTime = HourMinRange(HourMin(9, 0), HourMin(12, 0)),
        daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    )

    @Test
    fun testRecentTimeTrigger() {
        val policy = buildDefaultSurveyPolicy()
        val now = System.currentTimeMillis()

        val triggerTime = GregorianCalendar.getInstance().apply {
            timeInMillis = now
            add(GregorianCalendar.DAY_OF_MONTH, 0)
            set(GregorianCalendar.HOUR_OF_DAY, 2)
            set(GregorianCalendar.MINUTE, 0)
        }.timeInMillis

        val actualTrigger = policy.getMostRecentTriggerTime(triggerTime)

        println(
            GregorianCalendar.getInstance().apply { timeInMillis = triggerTime }.time
        )

        println(
            GregorianCalendar.getInstance().apply { timeInMillis = actualTrigger }.time
        )
    }
}