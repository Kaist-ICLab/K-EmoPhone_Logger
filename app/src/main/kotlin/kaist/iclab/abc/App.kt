package kaist.iclab.abc

import android.app.Application
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import kaist.iclab.abc.data.entities.MyObjectBox
import com.crashlytics.android.Crashlytics
import com.google.firebase.messaging.FirebaseMessaging
import io.fabric.sdk.android.Fabric




class App : Application(){
    companion object {
        private val TAG = App::class.java.canonicalName
        lateinit var boxStore: BoxStore

        inline fun <reified T> boxFor() = boxStore.boxFor<T>()
    }

    class AppLifecycleOwner private constructor() : LifecycleOwner {
        companion object {
            private var instance = AppLifecycleOwner()

            fun getInstance() : AppLifecycleOwner {
                return instance
            }
        }

        private val registry = LifecycleRegistry(this)

        override fun getLifecycle(): Lifecycle {
            return registry
        }

        fun onCreate () {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onDestroy () {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        WorkManager.initialize(this, Configuration.Builder().build())
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()

        Fabric.with(Fabric.Builder(this)
            .kits(Crashlytics())
            .debuggable(true)
            .build())
        AppLifecycleOwner.getInstance().onCreate()

        boxStore = MyObjectBox.builder()
            .androidContext(applicationContext)
            .name(getString(R.string.db_name))
            .build()
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "onTerminate()")

        AppLifecycleOwner.getInstance().onDestroy()
    }
}
