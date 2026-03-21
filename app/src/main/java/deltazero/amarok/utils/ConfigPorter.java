package deltazero.amarok.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import deltazero.amarok.PrefMgr;

/**
 * 配置文件导出 / 导入工具。
 *
 * <p>格式（JSON）：
 * <pre>
 * {
 *   "version": 1,
 *   "hideApps": ["com.a.b", "com.c.d"],
 *   "shortcutApps": ["com.a.b"],
 *   "hideFiles": ["/sdcard/foo", "/sdcard/bar"]
 * }
 * </pre>
 */
public class ConfigPorter {

    private static final String TAG = "ConfigPorter";
    private static final int CONFIG_VERSION = 1;

    // -------------------------------------------------------------------------
    // 导出
    // -------------------------------------------------------------------------

    public static boolean export(Context context, Uri destUri) {
        try {
            JSONObject json = new JSONObject();
            json.put("version", CONFIG_VERSION);

            JSONArray hideApps = new JSONArray();
            for (String pkg : PrefMgr.getHideApps()) hideApps.put(pkg);
            json.put("hideApps", hideApps);

            JSONArray shortcutApps = new JSONArray();
            for (String pkg : PrefMgr.getShortcutApps()) shortcutApps.put(pkg);
            json.put("shortcutApps", shortcutApps);

            JSONArray hideFiles = new JSONArray();
            for (String path : PrefMgr.getHideFilePath()) hideFiles.put(path);
            json.put("hideFiles", hideFiles);

            try (OutputStream os = context.getContentResolver().openOutputStream(destUri)) {
                if (os == null) return false;
                os.write(json.toString(2).getBytes("UTF-8"));
            }
            Log.i(TAG, "Config exported to: " + destUri);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // 导入
    // -------------------------------------------------------------------------

    public static class ImportResult {
        // 隐藏应用
        public final List<String> hideImported = new ArrayList<>();
        public final List<String> hideSkipped  = new ArrayList<>();
        // 快捷方式
        public final List<String> scImported   = new ArrayList<>();
        public final List<String> scSkipped    = new ArrayList<>();
        // 隐藏文件
        public final List<String> fileImported = new ArrayList<>();
        public final List<String> fileSkipped  = new ArrayList<>();
    }

    public static ImportResult importFrom(Context context, Uri srcUri) {
        try {
            StringBuilder sb = new StringBuilder();
            try (InputStream is = context.getContentResolver().openInputStream(srcUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            }

            JSONObject json = new JSONObject(sb.toString());
            PackageManager pm = context.getPackageManager();
            ImportResult result = new ImportResult();

            // --- 隐藏应用 ---
            Set<String> localHideApps = PrefMgr.getHideApps();
            JSONArray hideArr = json.optJSONArray("hideApps");
            if (hideArr != null) {
                for (int i = 0; i < hideArr.length(); i++) {
                    String pkg = hideArr.getString(i);
                    if (!isAppInstalled(pm, pkg)) {
                        result.hideSkipped.add(pkg);
                    } else if (!localHideApps.contains(pkg)) {
                        result.hideImported.add(pkg);
                    }
                }
            }
            Set<String> newHideApps = new HashSet<>(localHideApps);
            newHideApps.addAll(result.hideImported);
            PrefMgr.setHideApps(newHideApps);

            // --- 快捷方式 ---
            Set<String> localShortcuts = PrefMgr.getShortcutApps();
            JSONArray scArr = json.optJSONArray("shortcutApps");
            if (scArr != null) {
                for (int i = 0; i < scArr.length(); i++) {
                    String pkg = scArr.getString(i);
                    if (!isAppInstalled(pm, pkg)) {
                        result.scSkipped.add(pkg);
                    } else if (!localShortcuts.contains(pkg)) {
                        result.scImported.add(pkg);
                    }
                }
            }
            Set<String> newShortcuts = new HashSet<>(localShortcuts);
            newShortcuts.addAll(result.scImported);
            PrefMgr.setShortcutApps(newShortcuts);

            // --- 隐藏文件 ---
            Set<String> localFiles = PrefMgr.getHideFilePath();
            JSONArray fileArr = json.optJSONArray("hideFiles");
            if (fileArr != null) {
                for (int i = 0; i < fileArr.length(); i++) {
                    String path = fileArr.getString(i);
                    if (!new File(path).exists()) {
                        result.fileSkipped.add(path);
                    } else if (!localFiles.contains(path)) {
                        result.fileImported.add(path);
                    }
                }
            }
            Set<String> newFiles = new HashSet<>(localFiles);
            newFiles.addAll(result.fileImported);
            PrefMgr.setHideFilePath(newFiles);

            Log.i(TAG, "Imported: hideApps=" + result.hideImported.size()
                    + " sc=" + result.scImported.size()
                    + " files=" + result.fileImported.size());
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            return null;
        }
    }

    private static boolean isAppInstalled(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
