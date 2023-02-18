package deltazero.amarok.utils;

import static android.content.pm.PackageManager.GET_META_DATA;
import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class AppInfoUtil {
    private final PackageManager pkgMgr;
    private List<AppInfo> appInfoList = new ArrayList<>();

    public AppInfoUtil(Context context) {
        pkgMgr = context.getPackageManager();
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

    public void update() {
        appInfoList.clear();

        // Get applications info
        List<ApplicationInfo> installedApplications = pkgMgr.getInstalledApplications(GET_META_DATA | MATCH_DISABLED_COMPONENTS | MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo applicationInfo : installedApplications) {

            // Ignore system application
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)
                continue;

            // Ignore Amarok itself
            if (applicationInfo.packageName.contains("deltazero.amarok"))
                continue;

            var appInfo = new AppInfo(
                    applicationInfo.packageName,
                    pkgMgr.getApplicationLabel(applicationInfo).toString(),
                    pkgMgr.getApplicationIcon(applicationInfo)
            );

            appInfoList.add(appInfo);
        }
    }

    public List<AppInfo> getInstalledApps(String query) {
        List<AppInfo> queryAppInfoList = new ArrayList<>();
        for (AppInfo appInfo: appInfoList) {
            // Apply query filter
            if (query == null || query.isEmpty() ||
                    containsIgnoreCase(appInfo.packageName, query) || containsIgnoreCase(appInfo.label, query))
                queryAppInfoList.add(appInfo);
        }
        return queryAppInfoList;
    }

    public static class AppInfo {
        @NonNull
        public String packageName;
        @NonNull
        public String label;
        public Drawable icon;

        public AppInfo(@NonNull String packageName, @NonNull String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }
}
