package kaist.iclab.abc.foreground.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.view.MenuItem
import kaist.iclab.abc.R
import kaist.iclab.abc.background.collector.LocationAndActivityCollector
import kaist.iclab.abc.common.base.BaseAppCompatActivity
import kaist.iclab.abc.data.types.PhysicalActivityTransitionType
import kotlinx.android.synthetic.main.activity_container_with_toolbar.*
import kotlinx.android.synthetic.main.activity_debug.*

class DebugActivity: BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container_with_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.activity_title_debug)
            setDisplayHomeAsUpEnabled(true)
        }
        container.addView(layoutInflater.inflate(R.layout.activity_debug, container, false))

        btnEventEnterStill.setOnClickListener {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                Intent(LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE)
                    .putExtra(LocationAndActivityCollector.EXTRA_ACTIVITY_TRANSITIONS, arrayOf(PhysicalActivityTransitionType.ENTER_STILL.name))
            )
        }
        btnEventExitStill.setOnClickListener {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                Intent(LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE)
                    .putExtra(LocationAndActivityCollector.EXTRA_ACTIVITY_TRANSITIONS, arrayOf(PhysicalActivityTransitionType.EXIT_STILL.name))
            )
        }
        btnEventEnterVehicle.setOnClickListener {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                Intent(LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE)
                    .putExtra(LocationAndActivityCollector.EXTRA_ACTIVITY_TRANSITIONS, arrayOf(PhysicalActivityTransitionType.ENTER_IN_VEHICLE.name))
            )
        }
        btnEventExitVehicle.setOnClickListener {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                Intent(LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE)
                    .putExtra(LocationAndActivityCollector.EXTRA_ACTIVITY_TRANSITIONS, arrayOf(PhysicalActivityTransitionType.EXIT_IN_VEHICLE.name))
            )
        }
        btnEventCombination1.setOnClickListener {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                Intent(LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE)
                    .putExtra(LocationAndActivityCollector.EXTRA_ACTIVITY_TRANSITIONS, arrayOf(PhysicalActivityTransitionType.EXIT_WALKING.name, PhysicalActivityTransitionType.ENTER_STILL.name))
            )
        }
        btnEventCombination2.setOnClickListener {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                Intent(LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE)
                    .putExtra(LocationAndActivityCollector.EXTRA_ACTIVITY_TRANSITIONS, arrayOf(PhysicalActivityTransitionType.ENTER_WALKING.name, PhysicalActivityTransitionType.EXIT_STILL.name))
            )
        }

     }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    companion object {
        fun newIntent(context: Context) : Intent = Intent(context, DebugActivity::class.java)
    }
}