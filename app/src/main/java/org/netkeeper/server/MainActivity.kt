package org.netkeeper.server

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        grantPermissions()

        val service = Intent(this, ServerSocketService::class.java)
        startForegroundService(service)
    }

    private fun grantPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
        )

        for (permission in permissions) {
            val isDenied =
                android.content.pm.PackageManager.PERMISSION_DENIED == this.checkSelfPermission(
                    permission
                )
            if (isDenied) {
                this.requestPermissions(permissions, 0)
                break
            }
        }
    }
}