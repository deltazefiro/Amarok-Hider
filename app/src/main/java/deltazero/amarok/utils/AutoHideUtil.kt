package deltazero.amarok.utils

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import deltazero.amarok.AmarokApplication
import deltazero.amarok.core.Hider
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class AutoHideUtil {

  class AutoHideWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
      Log.i(TAG, "Auto hide triggered. Start hiding.")
      Hider.processAll(applicationContext, Hider.Action.HIDE)
      return Result.success()
    }
  }

  companion object {
    private const val AUTO_HIDE_WORK_NAME = "deltazero.amarok.AUTO_HIDE_WORK"
    private const val TAG = "AutoHideUtil"

    @JvmStatic
    fun setAutoHide(context: Context) {
      val settings = (context.applicationContext as AmarokApplication).settingsRepo.settings.value
      if (!settings.autoHide || Hider.getState() == Hider.State.HIDDEN) return
      Log.i(TAG, "Auto hide set. Delay: " + settings.autoHideDelay + " minutes.")
      WorkManager.getInstance(context)
        .enqueueUniqueWork(
          AUTO_HIDE_WORK_NAME,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequest.Builder(AutoHideWorker::class.java)
            .setInitialDelay(settings.autoHideDelay.toLong(), TimeUnit.MINUTES)
            .build(),
        )
    }

    @JvmStatic
    fun cancelAutoHide(context: Context) {
      val workManager = WorkManager.getInstance(context)

      val future = workManager.getWorkInfosForUniqueWork(AUTO_HIDE_WORK_NAME)
      val workInfos: List<WorkInfo>
      try {
        workInfos = future.get()
      } catch (e: ExecutionException) {
        throw RuntimeException("Should not happen.", e)
      } catch (e: InterruptedException) {
        throw RuntimeException("Should not happen.", e)
      }

      if (workInfos.isEmpty()) return
      if (workInfos[0].state != WorkInfo.State.RUNNING) {
        workManager.cancelUniqueWork(AUTO_HIDE_WORK_NAME)
        Log.i(TAG, "Auto hide cancelled.")
      }
    }
  }
}
