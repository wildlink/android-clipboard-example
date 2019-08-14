package com.example.myWildlinkApp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import me.wildfire.apiwrapper.ApiWrapper
import me.wildfire.apiwrapper.ApiWrapperException
import me.wildfire.apiwrapper.public_models.Device
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        doAsync {
            // you only have to do this one time in one activity
            val preferences = getSharedPreferences("myprefs", Context.MODE_PRIVATE)
            // find if you have previously stored the device
            var deviceJson = preferences.getString("device", null)

            if (deviceJson == null) {
                // does not exist save it and set it in the apiwrapper
                deviceJson = try {
                    ApiWrapper.createDevice()
                } catch (e: ApiWrapperException) {
                    uiThread {
                        alert("Error creating device ${e.statusCode} ${e.message}") {
                        }.show()
                    }
                    null
                }

                deviceJson?.let {
                    val editor = preferences.edit()
                    editor.putString("device", deviceJson)
                    editor.commit()
                    Log.d("exampleapp", "deviceJson : $deviceJson")

                    // we'll get back to this service in a moment
                    ServiceInstaller.installServices(this@MainActivity)
                }
            } else {
                ApiWrapper.setDevice(Device(deviceJson))
            }
        }
    }
}
