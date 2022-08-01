package kaist.iclab.abc.foreground.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abc.R
import kaist.iclab.abc.background.ABCPlatform
import kaist.iclab.abc.background.SyncManager
import kaist.iclab.abc.background.collector.*
import kaist.iclab.abc.common.*
import kaist.iclab.abc.data.PreferenceAccessor
import kaist.iclab.abc.common.base.BaseAppCompatActivity
import kaist.iclab.abc.common.util.*
import kaist.iclab.abc.foreground.fragment.*
import kotlinx.android.synthetic.main.activity_container_with_toolbar_drawer.*
import kotlinx.android.synthetic.main.view_nav_header.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class MainActivity : BaseAppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    ParticipatedExperimentFragment.OnLoadParticipatedExperimentListener {

    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private var isBackPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupContentView()
        setupActionBar()
        setupDrawer()

        if(intent.getBooleanExtra(EXTRA_IS_FROM_SIGN_IN, false)) {
            ABCPlatform.start(this)
        } else {
            ABCPlatform.maintain(this)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        actionBarDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        actionBarDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            Handler().postDelayed({
                drawer.closeDrawer(GravityCompat.START, true)
            }, 200)
        } else {
            if(isBackPressedOnce) {
                super.onBackPressed()
            } else {
                isBackPressedOnce = true
                ViewUtils.showToast(this, R.string.msg_general_exit_app)
                Handler().postDelayed({ isBackPressedOnce = false }, 2000)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Handler().postDelayed( {
            drawer.closeDrawer(GravityCompat.START, true)
        }, 200)

        val (title: String?, fragment: Fragment?) = when(item.itemId) {
            R.id.menuSurveysAll -> Pair(item.title.toString(), SurveyListFragment.newInstance(false))
            R.id.menuSurvyesNotRespond -> Pair(item.title.toString(), SurveyListFragment.newInstance(true))
            R.id.menuExperiments -> Pair(item.title.toString(), ExperimentListFragment())
            R.id.menuExperimentParticipated -> Pair(item.title.toString(), ParticipatedExperimentFragment())
            R.id.menuSync -> {
                if(!NetworkUtils.isWifiAvailable(this)) {
                    ViewUtils.showToast(this, R.string.error_no_wifi_network_available)
                } else {
                    SyncManager.syncWithProgressShown(this)
                    ViewUtils.showToast(this, R.string.msg_general_sync)
                }
                Pair(null, null)
            }
            R.id.menuLogOut -> {
                ABCPlatform.stop(this)
                val lastEmail = FirebaseAuth.getInstance().currentUser?.email
                FirebaseAuth.getInstance().signOut()
                startActivity(SignInActivity.newIntent(this, lastEmail).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
                Pair(null, null)
            }
            R.id.menuDebug -> {
                startActivity(Intent(this, DebugActivity::class.java))
                Pair(null, null)
            }
            else -> Pair(null, null)
        }

        if(title != null) {
            supportActionBar?.title = title
        }
        if(fragment != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
        }

        return true
    }

    override fun onLoadParticipatedExperiment(isParticipating: Boolean) {
        if(isParticipating) {
            ABCPlatform.maintain(this@MainActivity)
        } else {
            ABCPlatform.stop(this@MainActivity)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            REQUEST_CODE_FOR_GOOGLE_FITNESS -> {
                if(resultCode == Activity.RESULT_OK) {
                    ViewUtils.showPermissionDialog(this, GoogleFitnessCollector.REQUIRED_PERMISSIONS.toTypedArray(),
                        {
                            GoogleFitnessCollector.subscribeFitnessData(this@MainActivity)
                                .addOnSuccessListener { _ -> ABCPlatform.start(this@MainActivity) }
                                .addOnFailureListener { ViewUtils.showToast(this@MainActivity, FitnessDataSubscriptionFailedException().getErrorStringRes()) }
                        },
                        { ViewUtils.showToast(this@MainActivity, PermissionDeniedException().getErrorStringRes()) }
                    )
                }
            }
            REQUEST_CODE_FOR_APP_USAGE, REQUEST_CODE_FOR_NOTIFICATION -> ABCPlatform.start(this)
        }
    }

    private fun checkPermission () {
        val executor = Executors.newSingleThreadExecutor()

        Tasks.call(executor, Callable {
            PermissionUtils.throwExceptionForCurrentPermission(this)
        }).addOnFailureListener {
            when(it) {
                is RuntimePermissionDeniedException -> {
                    ViewUtils.showSnackBar(rootContainer, it.getErrorStringRes(), true, R.string.btn_set_permission) {
                        ViewUtils.showPermissionDialog(this, it.requiredPermissions,
                            { ABCPlatform.start(this@MainActivity) },
                            { ViewUtils.showToast(this@MainActivity, PermissionDeniedException().getErrorStringRes())})
                    }
                }
                is RuntimeAppUsageDeniedException -> {
                    ViewUtils.showSnackBar(rootContainer, it.getErrorStringRes(), true, R.string.btn_set_permission) {
                        startActivityForResult(AppUsageCollector.newIntentForSetup(), REQUEST_CODE_FOR_APP_USAGE)
                        ViewUtils.showToast(this, R.string.msg_set_app_usage)
                    }
                }
                is RuntimeNotificationAccessDeniedException -> {
                    ViewUtils.showSnackBar(rootContainer, it.getErrorStringRes(), true, R.string.btn_set_permission) {
                        startActivityForResult(NotificationCollector.newIntentForSetup(), REQUEST_CODE_FOR_NOTIFICATION)
                        ViewUtils.showToast(this, R.string.msg_set_notification)
                    }
                }
                is RuntimeGoogleFitnessDeniedException -> {
                    ViewUtils.showSnackBar(rootContainer, it.getErrorStringRes(), true, R.string.btn_set_permission) {
                        GoogleFitnessCollector.newIntentForSetup(this)
                            .addOnSuccessListener { intent ->
                                startActivityForResult(intent, REQUEST_CODE_FOR_GOOGLE_FITNESS)
                                ViewUtils.showToast(this@MainActivity, R.string.msg_set_google_fitness)
                            }
                            .addOnFailureListener { _ -> ViewUtils.showToast(this@MainActivity, R.string.error_google_sign_out_error) }
                    }
                }
            }
        }
    }

    private fun setupContentView() {
        setContentView(R.layout.activity_container_with_toolbar_drawer)
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.activity_title_main)
    }

    private fun setupDrawer() {
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.general_close, R.string.general_open)

        drawer.addDrawerListener(actionBarDrawerToggle)
        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(p0: Int) { }

            override fun onDrawerSlide(p0: View, p1: Float) { }

            override fun onDrawerClosed(p0: View) { }

            override fun onDrawerOpened(p0: View) {
                val pref = PreferenceAccessor.getInstance(this@MainActivity)

                txtUserEmail.text = (FirebaseAuth.getInstance().currentUser?.email ?: "").let {
                    if(TextUtils.isEmpty(it)) getString(R.string.general_empty_email) else it
                }

                txtLastSyncedTime.text = when {
                    pref.isSyncInProgress -> getString(R.string.general_sync_in_progress)
                    pref.lastTimeSynced < 0 -> String.format("%s: %s", getString(R.string.label_last_sync_time), getString(R.string.general_empty_sync_time))
                    else -> String.format( "%s: %s", getString(R.string.label_last_sync_time), FormatUtils.formatTimeBeforeExact(pref.lastTimeSynced, Math.max(pref.lastTimeSynced, System.currentTimeMillis())))
                }
                txtParticipationState.text = if(pref.isParticipated) {
                    getString(R.string.general_participating_in_experiment)
                } else {
                    getString(R.string.general_no_participated_experiment)
                }
            }
        })

        navView.setNavigationItemSelectedListener(this)
        navView.setCheckedItem(R.id.menuSurveysAll)
        navView.menu.findItem(R.id.menuDebug).isVisible = resources.getBoolean(R.bool.is_debug_mode)

        navView.menu.performIdentifierAction(R.id.menuSurveysAll, 0)
    }

    companion object {
        private val EXTRA_IS_FROM_SIGN_IN = "${MainActivity::class.java.canonicalName}.EXTRA_IS_FROM_SIGN_IN"

        fun newIntent(context: Context, isFromSignIn: Boolean = false) : Intent = Intent(context, MainActivity::class.java).putExtra(EXTRA_IS_FROM_SIGN_IN, isFromSignIn)

        const val REQUEST_CODE_FOR_GOOGLE_FITNESS = 0x00000001
        const val REQUEST_CODE_FOR_APP_USAGE = 0x00000002
        const val REQUEST_CODE_FOR_NOTIFICATION = 0x00000003
    }
}