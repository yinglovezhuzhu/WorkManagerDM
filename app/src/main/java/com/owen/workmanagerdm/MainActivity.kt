package com.owen.workmanagerdm

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        btnCheckAllContinuation.setOnClickListener {
            Log.e("TEST", "Start Checking All.")

            // 创建工作链并提交到系统排队
            WorkManager.getInstance(applicationContext)
                .beginWith(OneTimeWorkRequestBuilder<ContinuationCheck1>().also {
                    it.setInputData(workDataOf("name" to "Owen", "position" to "Manager"))
                }.build())
                .then(OneTimeWorkRequestBuilder<ContinuationCheck2>().build())
                .then(listOf(OneTimeWorkRequestBuilder<ContinuationCheck3>().build(), OneTimeWorkRequestBuilder<ContinuationCheck4>().build()))
                .then(OneTimeWorkRequestBuilder<ContinuationCheck5>().also {
                    // 设置输入数据合并模式
                    it.setInputMerger(ArrayCreatingInputMerger::class)
                }.build())
                .enqueue()


        }

        var checkDisk:PeriodicWorkRequest? = null
        btnStartPeriodicWork.setOnClickListener {
            Log.e("TEST", "Start Checking Disk periodic work")

//            checkDisk = PeriodicWorkRequest.Builder(CheckDiskProgressWorker::class.java,
//                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS).build()

            checkDisk = PeriodicWorkRequestBuilder<CheckDiskProgressWorker>(
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS).build()

            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(checkDisk!!.id)
                .observe(this@MainActivity, object : Observer<WorkInfo> {
                    override fun onChanged(t: WorkInfo?) {
                        Log.e("TEST", "WorkRequest state: ${t?.state}")
                        if(t?.state == WorkInfo.State.RUNNING) {
                            Log.d("TEST", "Work progress --- ${t.progress.getInt("progress", 0)}")
                            Toast.makeText(this@MainActivity, "Check disk... ${t.progress.getInt("progress", 0)}%", Toast.LENGTH_LONG).show()
                        } else if(t?.state == WorkInfo.State.SUCCEEDED){
                            Toast.makeText(this@MainActivity, "Check disk success", Toast.LENGTH_LONG).show()
                        } else if(t?.state == WorkInfo.State.FAILED){
                            Toast.makeText(this@MainActivity, "Check disk failed", Toast.LENGTH_LONG).show()
                        } else if(t?.state == WorkInfo.State.CANCELLED){
                            Toast.makeText(this@MainActivity, "Check disk canceled", Toast.LENGTH_LONG).show()
                        }
                    }
                })
            WorkManager.getInstance(applicationContext).enqueue(checkDisk!!)
        }

        btnStopPeriodicWork.setOnClickListener {
            Log.e("TEST", "Stop Checking Disk periodic work")
            if(null != checkDisk) {
                WorkManager.getInstance(applicationContext).cancelWorkById(checkDisk!!.id)
            }
        }

        btnExistingWorkPolicyReplace.setOnClickListener {

            WorkManager.getInstance(applicationContext).enqueueUniqueWork("checkDisk",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DoWorker>()
                    .setInputData(workDataOf("op" to "Check"))
                    .build().also {
                        Log.e("TEST", "${it.id} Check")
                    })
        }

        btnExistingWorkPolicyKeep.setOnClickListener {
            WorkManager.getInstance(applicationContext).enqueueUniqueWork("checkDisk",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DoWorker>()
                    .setInputData(workDataOf("op" to "Check"))
                    .build().also {
                        Log.e("TEST", "${it.id} Check")
                    })
        }

        btnExistingWorkPolicyAppend.setOnClickListener {
            WorkManager.getInstance(applicationContext).enqueueUniqueWork("checkDisk",
                ExistingWorkPolicy.APPEND,
                OneTimeWorkRequestBuilder<DoWorker>()
                    .setInputData(workDataOf("op" to "Check"))
                    .build().also {
                        Log.e("TEST", "${it.id} Check")
                    })
        }

        btnUniqueWorkContinuation.setOnClickListener {

//            WorkManager.getInstance(applicationContext).beginUniqueWork("check",
//                ExistingWorkPolicy.KEEP,
//                OneTimeWorkRequestBuilder<DoWorker>()
//                    .setInputData(workDataOf("op" to "Check Disk"))
//                    .build())
//                .then(OneTimeWorkRequestBuilder<DoWorker>()
//                    .setInputData(workDataOf("op" to "Check Network"))
//                    .build())
//                .then(OneTimeWorkRequestBuilder<DoWorker>()
//                    .setInputData(workDataOf("op" to "Check System"))
//                    .build())
//                .enqueue()
            WorkManager.getInstance(applicationContext).beginUniqueWork("check",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<CWorker>()
                    .setInputData(workDataOf("op" to "Check Disk"))
                    .build())
                .then(OneTimeWorkRequestBuilder<CWorker>()
                    .setInputData(workDataOf("op" to "Check Network"))
                    .build())
                .then(OneTimeWorkRequestBuilder<CWorker>()
                    .setInputData(workDataOf("op" to "Check System"))
                    .build())
                .enqueue()
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
        Log.e("TEST", "Checking disk done.")
        return Result.success(Data.Builder().let {
            it.putInt("code", 200)
            it.putString("msg", "Disk is fine")
            it.build()
        })
    }


}

class ContinuationCheck1(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

    override fun doWork(): Result {
        Log.e("TEST", "ContinuationCheck1 .......")

        // 打印输入数据
        val strB = StringBuilder("ContinuationCheck1 输入数据：\n")
        for ((key, value) in inputData.keyValueMap) {
            strB.append(key)
                .append(" : ")
                .append(value)
                .append("\n")
        }
        Log.e("TEST", strB.toString())

        Thread.sleep(3000)

        Log.e("TEST", "ContinuationCheck1 done .......")

        return Result.success(workDataOf("whereFrom" to "This is from ContinuationCheck1"))
        // 如果失败了，所有从属的Worker都不会执行
//        return Result.failure()
    }
}

class ContinuationCheck2(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

    override fun doWork(): Result {
        Log.e("TEST", "ContinuationCheck2 .......")

        // 打印输入数据
        val strB = StringBuilder("ContinuationCheck2 输入数据：\n")
        for ((key, value) in inputData.keyValueMap) {
            strB.append(key)
                .append(" : ")
                .append(value)
                .append("\n")
        }
        Log.e("TEST", strB.toString())

        Thread.sleep(3000)

        Log.e("TEST", "ContinuationCheck2 done .......")

        return Result.success(workDataOf("whereFrom" to "This is from ContinuationCheck2"))
    }
}

class ContinuationCheck3(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

    override fun doWork(): Result {
        Log.e("TEST", "ContinuationCheck3 .......")

        // 打印输入数据
        val strB = StringBuilder("ContinuationCheck3 输入数据：\n")
        for ((key, value) in inputData.keyValueMap) {
            strB.append(key)
                .append(" : ")
                .append(value)
                .append("\n")
        }
        Log.e("TEST", strB.toString())

        Thread.sleep(3000)

        Log.e("TEST", "ContinuationCheck3 done .......")

        return Result.success(workDataOf("whereFrom" to "This is from ContinuationCheck3"))
    }
}

class ContinuationCheck4(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

    override fun doWork(): Result {
        Log.e("TEST", "ContinuationCheck4 .......")

        // 打印输入数据
        val strB = StringBuilder("ContinuationCheck4 输入数据：\n")
        for ((key, value) in inputData.keyValueMap) {
            strB.append(key)
                .append(" : ")
                .append(value)
                .append("\n")
        }
        Log.e("TEST", strB.toString())

        Thread.sleep(3000)

        Log.e("TEST", "ContinuationCheck4 done .......")

        return Result.success(workDataOf("whereFrom" to "This is from ContinuationCheck4"))
    }
}

class ContinuationCheck5(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

    override fun doWork(): Result {
        Log.e("TEST", "ContinuationCheck5 .......")

        // 打印输入数据
        val strB = StringBuilder("ContinuationCheck5 输入数据：\n")
        for ((key, value) in inputData.keyValueMap) {

            strB.append(key)
                .append(" : ")
            // 使用了ArrayCreatingInputMerger，判断值是否为数组
            if (value is Array<*>) {
                strB.append("[")
                (value as Array<*>).forEach {
                    strB.append(it)
                        .append(",")
                }
                strB.also {
                    it.delete(it.lastIndexOf(",") - 1, it.length)
                }.append("]")
            } else {
                strB.append(value)
            }

            strB.append("\n")
        }
        Log.e("TEST", strB.toString())

        Thread.sleep(3000)

        Log.e("TEST", "ContinuationCheck5 done .......")

        return Result.success(workDataOf("whereFrom" to "This is from ContinuationCheck5"))
    }


}

class DoWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

    override fun doWork(): Result {

        val op = inputData.getString("op");

        Log.e("TEST", "$id $op .......")

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            onStopped()
        }
        if(isStopped) {
            Log.e("TEST", "$id $op stopped!")
            return Result.failure()
        }
        Log.e("TEST", "$id $op 50%.......")
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            onStopped()
        }
        if(isStopped) {
            Log.e("TEST", "$id $op stopped!")
            return Result.failure()
        }
        Log.e("TEST", "$id $op done .......")

        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
    }
}

class CWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val op = inputData.getString("op");

        Log.e("TEST", "$id $op .......")

        delay(3000)

        if(isStopped) {
            Log.e("TEST", "$id $op stopped!")
            return Result.failure()
        }
        Log.e("TEST", "$id $op 50%.......")

        delay(3000)

        if(isStopped) {
            Log.e("TEST", "$id $op stopped!")
            return Result.failure()
        }
        Log.e("TEST", "$id $op done .......")
        return Result.success()
    }
}
