package deltazero.amarok.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import deltazero.amarok.BuildConfig;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateUtil {
    private static final String TAG = "UpdateUtil";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build();

    public enum UpdateChannel {
        RELEASE, BETA;

        public static UpdateChannel fromString(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (Exception e) {
                return RELEASE;
            }
        }
    }

    public record Release(String version, String url) {
    }

    /**
     * Check for updates and show dialog if available.
     *
     * @param context Context
     * @param silent  If true, only show dialog when update is available
     */
    public static void checkAndNotify(@NonNull Context context, boolean silent) {
        if (!silent) {
            mainHandler.post(() -> Toast.makeText(context, R.string.checking_update, Toast.LENGTH_SHORT).show());
        }

        executor.execute(() -> {
            try {
                String currentVersion = getCurrentVersion(context);
                Release latestRelease = fetchLatestRelease(PrefMgr.getUpdateChannel());

                Log.d(TAG, "Latest release version: " + latestRelease.version + " Current version: v" + currentVersion);

                if (isNewerVersion(currentVersion, latestRelease.version)) {
                    mainHandler.post(() -> showUpdateDialog(context, latestRelease));
                } else if (!silent) {
                    mainHandler.post(() -> Toast.makeText(context, R.string.no_update_ava, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to check for updates", e);
                if (!silent) {
                    mainHandler.post(() -> Toast.makeText(context, R.string.update_check_failed, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private static void showUpdateDialog(@NonNull Context context, @NonNull Release release) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.update_available_title)
                .setMessage(context.getString(R.string.update_available_message, release.version))
                .setPositiveButton(R.string.view_on_github, (dialog, which)
                        -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(release.url))))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private static Release fetchLatestRelease(UpdateChannel channel) throws Exception {
        Request request = new Request.Builder().url(BuildConfig.GITHUB_CHECK_UPDATE_URL).header("Accept", "application/vnd.github.v3+json").get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Network error: " + response.code());
            }

            JSONArray releases = new JSONArray(response.body().string());

            for (int i = 0; i < releases.length(); i++) {
                JSONObject release = releases.getJSONObject(i);
                if (release.getBoolean("draft"))
                    continue;
                if (channel == UpdateChannel.RELEASE && release.getBoolean("prerelease"))
                    continue;
                return new Release(release.getString("tag_name"), release.getString("html_url"));
            }

            throw new Exception("No release found");
        }
    }

    @NonNull
    private static String getCurrentVersion(@NonNull Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0.0.0";
        }
    }

    private static boolean isNewerVersion(@NonNull String current, @NonNull String newVersion) {
        current = current.replaceFirst("^v", "");
        newVersion = newVersion.replaceFirst("^v", "");
        return new ComparableVersion(newVersion).compareTo(new ComparableVersion(current)) > 0;
    }
}

