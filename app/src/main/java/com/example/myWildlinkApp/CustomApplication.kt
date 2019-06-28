package com.example.myWildlinkApp

import android.app.Application
import me.wildfire.apiwrapper.ApiWrapper

class CustomApplication : Application(){
    override fun onCreate() {
        super.onCreate()

        ApiWrapper.setAppId("YOUR APP ID GOES HERE")
            .setConnectTimeout(15000)
            .setReadTimeout(15000)
            .setLogLevel(1)
            .setSecret("YOUR APP SECRET GOES HERE")
    }
}