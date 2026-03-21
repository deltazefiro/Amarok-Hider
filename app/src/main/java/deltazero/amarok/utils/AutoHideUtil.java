package deltazero.amarok.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;

public class AutoHideUtil {

    private static final String AUTO_HIDE_WORK_NAME = "deltazero.amarok.AUTO_HIDE_WORK";
    private static final String TAG = "AutoHideUtil";

    public static class AutoHideWorker extends Worker {
        public AutoHideWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            Context context = getApplicationContext();
            Set<String> tempUnhidden = PrefMgr.getTempUnhiddenApps();

            if (!tempUnhidden.isEmpty()) {
                // 有通过快捷方式/更新临时取消隐藏的应用，只重新隐藏这部分
                Log.i(TAG, "Auto hide triggered (single apps). Hiding: " + tempUnhidden);
                PrefMgr.getAppHider(context).tryToActivate((cls, succeed, msg) -> {
                    if (succeed) {
                        try {
                            PrefMgr.getAppHider(context).hide(tempUnhidden, false);
                            PrefMgr.clearTempUnhiddenApps();
                        } catch (Exception e) {
                            Log.w(TAG, "Auto hide single apps failed", e);
                        }
                    }
                });
            } else {
                // 正常全量隐藏
                Log.i(TAG, "Auto hide triggered (full). Start hiding.");
                Hider.hide(context);
            }
            return Result.success();
        }
    }

    public static void setAutoHide(Context context) {
        if (!PrefMgr.getEnableAutoHide()) return;
        // 全量隐藏中且没有单应用临时取消隐藏的情况 → 不需要设定
        if (Hider.getState() == Hider.State.HIDDEN && PrefMgr.getTempUnhiddenApps().isEmpty()) return;
        scheduleAutoHide(context);
    }

    private static void scheduleAutoHide(Context context) {
        Log.i(TAG, "Auto hide set. Delay: " + PrefMgr.getAutoHideDelay() + " minutes.");
        WorkManager.getInstance(context).enqueueUniqueWork(
                AUTO_HIDE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(AutoHideWorker.class)
                        .setInitialDelay(PrefMgr.getAutoHideDelay(), TimeUnit.MINUTES)
                        .build()
        );
    }

    public static void cancelAutoHide(Context context) {
        var workManager = WorkManager.getInstance(context);

        var future = workManager.getWorkInfosForUniqueWork(AUTO_HIDE_WORK_NAME);
        List<WorkInfo> workInfos;
        try {
            workInfos = future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Should not happen.", e);
        }

        if (workInfos.size() == 0) return;
        if (workInfos.get(0).getState() != WorkInfo.State.RUNNING) {
            workManager.cancelUniqueWork(AUTO_HIDE_WORK_NAME);
            Log.i(TAG, "Auto hide cancelled.");
        }
    }
}
