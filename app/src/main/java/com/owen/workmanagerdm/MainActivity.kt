package com.owen.workmanagerdm

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
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
            Log.e("TEST", "Start Checking system.")
            val checkSystem = OneTimeWorkRequestBuilder<CheckSystemWorker>()
                .build()
            WorkManager.getInstance(applicationContext).enqueue(checkSystem)
        }

        btnCheckSystemNetwork.setOnClickListener {
            Log.e("TEST", "Start Checking system.")
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED) // 需要联网
                .build()

            val checkSystem = OneTimeWorkRequestBuilder<CheckSystemWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(checkSystem)
        }

        btnCheckSystemDelay.setOnClickListener {
            Log.e("TEST", "Start Checking system.")
            val checkSystem = OneTimeWorkRequestBuilder<CheckSystemWorker>()
                .setInitialDelay(3000, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(checkSystem)
        }

        btnCheckDiskBackoff.setOnClickListener {
            Log.e("TEST", "Start Checking Disk.")
            val checkDisk = OneTimeWorkRequestBuilder<CheckDiskWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(checkDisk)
        }

        btnCheckNetworkWithInputOutput.setOnClickListener {
            Log.e("TEST", "Start Checking Network.")
            val checkNetwork = OneTimeWorkRequestBuilder<CheckNetworkWorker>()
                .setInputData(Data.Builder().let {
                    // Data以Builder的方式进行构建，传入的是键值对
                    it.putString("operator", "Owen")
                    it.putString("description", "Check network state")
                    it.build()
                })
                .addTag("networkWork")
                .build()

//            WorkManager.getInstance(applicationContext).getWorkInfoByIdLiveData(checkNetwork.id).observeForever {
//                for((key, value) in it.outputData.keyValueMap) {
//                    Log.d("TEST", "Out Data $key ---- $value")
//                }
//            }

            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(checkNetwork.id)
                .observe(this, object : Observer<WorkInfo> {
                override fun onChanged(t: WorkInfo?) {
                    // 任务执行完毕之后，会在这里获取到返回的结果
                    Log.e("TEST", "WorkRequest state: ${t?.state}")
                    if(t?.state == WorkInfo.State.SUCCEEDED) {
                        // 显示任务完成通知提示
                        Toast.makeText(this@MainActivity, "Check network success", Toast.LENGTH_LONG).show()
                        for((key, value) in t.outputData!!.keyValueMap) {
                            Log.d("TEST", "Out Data $key ---- $value")
                        }
                    }
                }
            })
            WorkManager.getInstance(applicationContext).enqueue(checkNetwork)
        }
        btnCheckDiskWithProgress.setOnClickListener {
            Log.e("TEST", "Start Checking Disk.")
            val checkDisk = OneTimeWorkRequestBuilder<CheckDiskProgressWorker>()
                .build()

            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(checkDisk.id)
                .observe(this, object : Observer<WorkInfo> {
                override fun onChanged(t: WorkInfo?) {
                    // 任务执行完毕之后，会在这里获取到返回的结果
                    Log.e("TEST", "WorkRequest state: ${t?.state}")
                    if(t?.state == WorkInfo.State.RUNNING) {
                        Log.d("TEST", "Work progress --- ${t.progress.getInt("progress", 0)}")
                        Toast.makeText(this@MainActivity, "Check disk... ${t.progress.getInt("progress", 0)}%", Toast.LENGTH_LONG).show()
                    } else if(t?.state == WorkInfo.State.SUCCEEDED){
                        Toast.makeText(this@MainActivity, "Check disk success", Toast.LENGTH_LONG).show()
                    } else if(t?.state == WorkInfo.State.FAILED){
                        Toast.makeText(this@MainActivity, "Check disk failed", Toast.LENGTH_LONG).show()
                    }
                }
            })
            WorkManager.getInstance(applicationContext).enqueue(checkDisk)
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

var times: Int = 0
var lastTime = 0L

class CheckDiskWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {
    override fun doWork(): Result {
        Log.e("TEST", "Checking disk。。。。。。。。")
        if(times > 0) {
            Log.w("TEST", "Retry ${times}, Delay is ${(System.currentTimeMillis() - lastTime) / 1000} s")
        }
        Thread.sleep(3000)
        lastTime = System.currentTimeMillis()
        if(times < 5) {
            Log.e("TEST", "Checking disk failure.")
            times++
            // 需要重试，比如操作失败了，返回Result.retry()
            return Result.retry()
        }
        Log.e("TEST", "Checking disk done.")
        times = 0
        // 返回成功时，将不会再重试
        return Result.success()
    }
}


class CheckNetworkWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {
    override fun doWork(): Result {
        Log.e("TEST", "Checking network。。。。。。。。")
        Log.d("TEST", "Checking network, params......")
        for((key, value) in inputData.keyValueMap) {
            Log.d("TEST", "$key ---- $value")
        }
        Thread.sleep(3000)
        Log.e("TEST", "Checking network done.")
        return Result.success(Data.Builder().let {
            it.putInt("code", 200)
            it.putString("msg", "Network is fine")
            it.build()
        })
    }
}
class CheckDiskProgressWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {
    override fun doWork(): Result {
        Log.e("TEST", "Checking disk。。。。。。。。")
        Thread.sleep(1000)
        // 更新进度
        setProgressAsync(Data.Builder().let {
            it.putInt("progress", 20)
            it.build()
        })
        Thread.sleep(1000)
        // 更新进度
        setProgressAsync(Data.Builder().let {
            it.putInt("progress", 40)
            it.build()
        })
        Thread.sleep(1000)
        // 更新进度
        setProgressAsync(Data.Builder().let {
            it.putInt("progress", 60)
            it.build()
        })
        Thread.sleep(1000)
        // 更新进度
        setProgressAsync(Data.Builder().let {
            it.putInt("progress", 80)
            it.build()
        })
        Thread.sleep(1000)
        // 更新进度
        setProgressAsync(Data.Builder().let {
            it.putInt("progress", 100)
            it.build()
        })
        Log.e("TEST", "Checking network done.")
        return Result.success(Data.Builder().let {
            it.putInt("code", 200)
            it.putString("msg", "Network is fine")
            it.build()
        })
    }
}
