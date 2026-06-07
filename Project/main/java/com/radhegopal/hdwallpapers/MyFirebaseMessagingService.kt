package com.radhegopal.hdwallpapers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Jab bhi Firebase se naya message aayega, ye function apne aap chalega
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Agar message ke andar title aur body hai, toh notification dikhao
        if (remoteMessage.notification != null) {
            showNotification(
                remoteMessage.notification?.title.toString(),
                remoteMessage.notification?.body.toString()
            )
        }
    }

    private fun showNotification(title: String, message: String) {
        // Notification par click karne se MainActivity khulegi
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val channelId = "RadheGopal_Channel"

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // App ka default icon dikhega
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true) // Click karne par notification hatt jayega
            .setContentIntent(pendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 (Oreo) aur usse upar ke liye Channel banana zaroori hota hai
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Radhe Gopal Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Notification phone screen par bhejo
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}