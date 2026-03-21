package deltazero.amarok.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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
 *   "shortcutApps": ["com.a.b"]
 * }
 * </pre>
 */
public class ConfigPorter {

    private static final String TAG = "ConfigPorter";
    private static final int CONFIG_VERSION = 1;

    // -------------------------------------------------------------------------
    // 导出
    // -------------------------------------------------------------------------

    /**
     * 将当前配置写入指定 Uri（由 SAF 文件选择器提供）。
     *
     * @return 是否成功
     */
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
        public final List<String> hideImported = new ArrayList<>();   // 成功导入（并集后新增）
        public final List<String> hideSkipped  = new ArrayList<>();   // 未安装，跳过
        public final List<String> scImported   = new ArrayList<>();   // 快捷方式已标记
        public final List<String> scSkipped    = new ArrayList<>();   // 快捷方式跳过（未安装）
    }

    /**
     * 从指定 Uri 读取配置并导入。
     *
     * <ul>
     *   <li>隐藏应用：过滤未安装，与本地列表求并集</li>
     *   <li>快捷方式：过滤未安装，与本地 shortcutApps 求并集（仅标记，不自动 pin）</li>
     * </ul>
     *
     * @return 导入结果，失败时返回 null
     */
    public static ImportResult importFrom(Context context, Uri srcUri) {
        try {
            // 读取文件
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
                    if (!isInstalled(pm, pkg)) {
                        result.hideSkipped.add(pkg);
                    } else if (!localHideApps.contains(pkg)) {
                        // 新增，不重复添加
                        result.hideImported.add(pkg);
                    }
                    // 已在列表中的静默跳过
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
                    if (!isInstalled(pm, pkg)) {
                        result.scSkipped.add(pkg);
                    } else if (!localShortcuts.contains(pkg)) {
                        result.scImported.add(pkg);
                    }
                }
            }
            Set<String> newShortcuts = new HashSet<>(localShortcuts);
            newShortcuts.addAll(result.scImported);
            PrefMgr.setShortcutApps(newShortcuts);

            Log.i(TAG, "Config imported: hideImported=" + result.hideImported.size()
                    + " hideSkipped=" + result.hideSkipped.size()
                    + " scImported=" + result.scImported.size()
                    + " scSkipped=" + result.scSkipped.size());
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            return null;
        }
    }

    private static boolean isInstalled(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
