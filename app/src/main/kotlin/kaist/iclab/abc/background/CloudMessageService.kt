package kaist.iclab.abc.background

import android.text.TextUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kaist.iclab.abc.common.util.NotificationUtils

class CloudMessageService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage?.notification?.title
        val text = remoteMessage?.notification?.body

        if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(text)) {
            NotificationUtils.notifyUpdateAvailable(this, title!!, text!!)
        }

    }
}