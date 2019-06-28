package com.example.myWildlinkApp

import android.content.Context
import android.content.Intent

import android.util.Log

object ServiceInstaller {
    fun installServices(context: Context) {
        // start the clipboard watching service
        Log.d("exampleapp", "attempting to install clipboard monitor service ... ")

        val clipboardIntent = Intent(context, ClipboardMonitorService::class.java)
        context.startService(clipboardIntent)
    }
}