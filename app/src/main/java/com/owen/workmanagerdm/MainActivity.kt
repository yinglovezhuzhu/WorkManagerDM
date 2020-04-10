package com.owen.workmanagerdm

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val periodicWorkRequest = PeriodicWorkRequestBuilder<CheckSystemWorker>(5L, TimeUnit.SECONDS)
            .addTag("Check01")
            .build()

        btnCheckSystem.setOnClickListener() {
            view ->
            val checkSystem = OneTimeWorkRequestBuilder<CheckSystemWorker>()
                .build()
            WorkManager.getInstance(applicationContext).enqueue(checkSystem)
        }

        btnCheckSystemNetwork.setOnClickListener {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED) // 需要联网
                .build()

            val checkSystem = OneTimeWorkRequestBuilder<CheckSystemWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(checkSystem)
        }
    }
}


class CheckSystemWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

    override fun doWork(): Result {

        Log.e("TEST", "Checking system。。。。。。。。")
        Thread.sleep(3000)
        Log.e("TEST", "Checking system done.")
        return Result.success()
    }

}
