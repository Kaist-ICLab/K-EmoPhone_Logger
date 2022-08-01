package kaist.iclab.abc.background

import kaist.iclab.abc.R
import kaist.iclab.abc.common.type.EnumMap
import kaist.iclab.abc.common.type.HasId
import kaist.iclab.abc.common.type.buildValueMap

data class Status(val state: State, val error: Throwable? = null) {
    enum class State(override val id: Int): HasId {
        STARTED(R.string.status_started),
        RUNNING(R.string.status_running),
        CANCELLED(R.string.status_canceled),
        ABORTED(R.string.status_aborted);

        companion object: EnumMap<State>(buildValueMap())
    }

    companion object {
        val STARTED = Status(State.STARTED)
        val RUNNING = Status(State.RUNNING)
        val CANCELED = Status(State.CANCELLED)
        fun ABORTED(throwable: Throwable?) = Status(State.ABORTED, throwable)
    }
}