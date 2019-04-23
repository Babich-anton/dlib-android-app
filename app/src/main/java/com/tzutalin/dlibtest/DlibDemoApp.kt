package com.tzutalin.dlibtest

import android.app.Application
import android.util.Log

import timber.log.Timber


class DlibDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    /**
     * A tree which logs important information
     */
    private class ReleaseTree : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }
            super.log(priority, tag, message, t)
        }
    }
}