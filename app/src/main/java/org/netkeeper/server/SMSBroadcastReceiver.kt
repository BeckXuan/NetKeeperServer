package org.netkeeper.server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION
import android.telephony.SmsMessage

interface OnReceivedMessageListener {
    fun onReceived(message: String)
}

class SMSBroadcastReceiver : BroadcastReceiver() {

    private var onReceivedMessageListener: OnReceivedMessageListener? = null
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == SMS_RECEIVED_ACTION) {
            val pdus = intent.extras!!["pdus"] as Array<*>?
            val format = intent.getStringExtra("format")
            for (pdu in pdus!!) {
                val smsMessage: SmsMessage = SmsMessage.createFromPdu(pdu as ByteArray, format)
                val sender: String = smsMessage.displayOriginatingAddress
                val content: String = smsMessage.displayMessageBody

                //println("SMS $sender $content")
                if (sender.endsWith("106593005")) {
                    this.onReceivedMessageListener?.onReceived(content)
                    // abortBroadcast()
                }
            }
        }
    }

    fun setOnReceivedMessageListener(onReceivedMessageListener: OnReceivedMessageListener) {
        this.onReceivedMessageListener = onReceivedMessageListener
    }
}