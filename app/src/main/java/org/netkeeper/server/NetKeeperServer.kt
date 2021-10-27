package org.netkeeper.server

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

class ServerSocketService : Service() {

    // 填写对应的电脑IP，用于鉴权
    // 若无需限制对应IP，则留空即可
    private val targetIP = "192.168.0.6"

    // 开放的端口号，对应NetKeeperHelper的phone_port参数
    private val localPort = 60666

    // 选择发送短信的手机卡
    private val simSlotIndex = 0 // 0为卡一，1为卡二，单卡默认0

    // 以下无需修改
    private var isRun = true
    private var isOpen = false
    private var curSocket: Socket? = null
    private var mSMSBroadcastReceiver: SMSBroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notification())
        registerSMSBroadcast()
        startServerSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        if (mSMSBroadcastReceiver != null) {
            unregisterReceiver(mSMSBroadcastReceiver)
        }
    }

    private fun notification(): Notification {
        val channelId = "ServerSocketService"
        val channelName = "default"
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notificationChannel = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_LOW
        )
        notificationChannel.enableLights(true)
        notificationChannel.setShowBadge(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(notificationChannel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NetKeeperServer")
            .setContentText("Service running...")
            .setSmallIcon(android.R.mipmap.sym_def_app_icon)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun registerSMSBroadcast() {
        mSMSBroadcastReceiver = SMSBroadcastReceiver()
        val intentFilter = IntentFilter(android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        intentFilter.priority = 1000
        registerReceiver(mSMSBroadcastReceiver, intentFilter)

        mSMSBroadcastReceiver!!.setOnReceivedMessageListener(object : OnReceivedMessageListener {
            override fun onReceived(message: String) {
                println("Received: $message")
                respondAndClose(curSocket, message)
            }
        })
    }

    private fun startServerSocket() {
        if (isOpen) return
        Thread {
            var serverSocket: ServerSocket? = null
            try {
                //serverSocket = ServerSocket(localPort, 5, InetAddress.getByName("192.168.51.81"))
                serverSocket = ServerSocket(localPort, 3)
                isOpen = true
                while (isRun) {
                    val socket = serverSocket.accept()
                    handleSocket(socket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    serverSocket?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isOpen = false
                }
            }
        }.start()
    }

    private fun handleSocket(socket: Socket) {
        Thread {
            try {
                socket.keepAlive = true
                socket.soTimeout = 10000 // input stream read timeout 10s
                val address = socket.inetAddress.hostAddress
                println("connection established from $address")
                if (address == targetIP) {
                    val bufferIn = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val revMsg = bufferIn.readLine()
                    println("valid msg: $revMsg")
                    handleCommand(socket, revMsg)
                } else {
                    respondAndClose(socket, "invalid IP")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleCommand(socket: Socket?, command: String) {
        when (command) {
            "password" -> passwordCommand(socket)
            "success" -> respondAndClose(socket, "success")
            "fail" -> respondAndClose(socket, "fail")
            else -> respondAndClose(socket, "invalid command")
        }
    }

    private fun passwordCommand(socket: Socket?) {
        try {
            curSocket!!.sendUrgentData(0xFF)
            // 能发送且没报错，说明上一个连接未断开
            respondAndClose(socket, "conflict")
        } catch (e: Exception) {
            // 报错，说明上一个连接断开
            try {
                curSocket?.close()
            } catch (e: Exception) {
                //e.printStackTrace()
            }
            curSocket = socket
            if (!sendSMS()) {
                respondAndClose(socket, "fail")
            }
        }
    }

    private fun sendSMS(): Boolean {
        // check permission
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_PHONE_STATE
            ) == android.content.pm.PackageManager.PERMISSION_DENIED
        ) {
            println("Permission denied")
            return false
        }

        return try {
            val subscriptionManager =
                getSystemService(AppCompatActivity.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val cardList = subscriptionManager.activeSubscriptionInfoList
            val subId = cardList[simSlotIndex].subscriptionId
            //println("cardList $cardList")
            //println("subId $subId")
            SmsManager.getSmsManagerForSubscriptionId(subId)
                .sendTextMessage("+86106593005", null, "mm", null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun respondAndClose(socket: Socket?, message: String) {
        Thread {
            try {
                if (socket?.isClosed == false) {
                    val bufferOut =
                        BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    bufferOut.write(message)
                    bufferOut.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    socket?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }
}