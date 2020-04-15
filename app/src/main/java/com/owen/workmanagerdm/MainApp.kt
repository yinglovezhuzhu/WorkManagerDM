package com.owen.workmanagerdm

import android.app.Application
import androidx.work.Configuration

/**
 *
 * <br/>Author：yunying.zhang
 * <br/>Email: yunyingzhang@rastar.com
 * <br/>Date: 2020/4/15
 */
class MainApp : Application(), Configuration.Provider {


    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setMaxSchedulerLimit(Configuration.MIN_SCHEDULER_LIMIT + 100)
            .build()
    }

}