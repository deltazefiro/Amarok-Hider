package deltazero.amarok.utils;

import static android.content.pm.PackageManager.GET_META_DATA;
import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import deltazero.amarok.PrefMgr;

public class AppInfoUtil {
    private final PackageManager pkgMgr;
    private final PrefMgr prefMgr;
    private final List<AppInfo> appInfoList = new ArrayList<>();

    public AppInfoUtil(Context context) {
        pkgMgr = context.getPackageManager();
        prefMgr = new PrefMgr(context);
    }

    private static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }

    public void refresh() {
        appInfoList.clear();

        Set<String> hiddenApps = prefMgr.getHideApps();

        // Get applications info
        List<ApplicationInfo> installedApplications = pkgMgr.getInstalledApplications(GET_META_DATA | MATCH_DISABLED_COMPONENTS | MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo applicationInfo : installedApplications) {

            // Filter out Amarok itself
            if (applicationInfo.packageName.contains("deltazero.amarok"))
                continue;

            var appInfo = new AppInfo(
                    applicationInfo.packageName,
                    pkgMgr.getApplicationLabel(applicationInfo).toString(),
                    (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                    pkgMgr.getApplicationIcon(applicationInfo)
            );

            appInfoList.add(appInfo);
        }

        // Sort with app name, and stick the hidden apps to the top
        appInfoList.sort((o1, o2) -> {
            if (hiddenApps.contains(o1.packageName) && !hiddenApps.contains(o2.packageName))
                return -1;
            if (hiddenApps.contains(o2.packageName) && !hiddenApps.contains(o1.packageName))
                return 1;
            return (o1.label.compareTo(o2.label));
        });
    }

    public List<AppInfo> getFilteredApps(String query, boolean includeSystemApps) {
        Set<String> hiddenApps = prefMgr.getHideApps();
        Log.d("AppInfoUtil", "Hidden apps: " + hiddenApps.toString());
        List<AppInfo> queryAppInfoList = new ArrayList<>();
        for (AppInfo appInfo : appInfoList) {

            boolean query_filter_result = (query == null || query.isEmpty())
                    || containsIgnoreCase(appInfo.packageName, query) || containsIgnoreCase(appInfo.label, query);

            boolean system_filter_result = hiddenApps.contains(appInfo.packageName) /* If the app is hidden, show it regardless of whether it is a system app or not. */
                    || includeSystemApps /* Skip this filter if user enable `Display system apps` */
                    || !appInfo.isSystemApp;

            if (query_filter_result && system_filter_result)
                queryAppInfoList.add(appInfo);
        }
        return queryAppInfoList;
    }

    public static class AppInfo {
        @NonNull
        public String packageName;
        @NonNull
        public String label;
        public boolean isSystemApp;
        public Drawable icon;

        public AppInfo(@NonNull String packageName, @NonNull String label, boolean isSystemApp, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.isSystemApp = isSystemApp;
            this.icon = icon;
        }
    }
}
