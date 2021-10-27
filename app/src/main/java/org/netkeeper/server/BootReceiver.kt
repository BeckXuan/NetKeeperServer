package org.netkeeper.server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val actionBoot : String = "android.intent.action.BOOT_COMPLETED"

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == actionBoot) {
            Log.d("BootReceiver", "system boot completed")
            val service = Intent(context, ServerSocketService::class.java)
            context?.startForegroundService(service)
        }
    }
}