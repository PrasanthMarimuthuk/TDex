package com.example.tdexv01

import android.app.Application
import com.cloudinary.android.MediaManager


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Replace with your Cloudinary credentials
        MediaManager.init(this, object : HashMap<String?, String?>() {
            init {
                put("cloud_name", "dbxgxgcff")
                put("api_key", "111493647131452")
                put("api_secret", "iRRMnTWRjcSQpLCe0lTrboBJooM")
            }
        })
    }
}