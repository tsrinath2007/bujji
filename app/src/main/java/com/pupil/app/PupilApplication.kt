package com.pupil.app

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PupilApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(applicationContext)
            Log.d("PupilApplication", "PDFBoxResourceLoader initialized.")
        } catch (e: Exception) {
            Log.e("PupilApplication", "Failed to init PDFBoxResourceLoader", e)
        }
    }
}
