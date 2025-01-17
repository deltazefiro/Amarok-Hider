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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class AppInfoUtil {
    private final PackageManager pkgMgr;
    private final List<AppInfo> appInfoList = new ArrayList<>();
    private final Set<String> predefinedRootApps;

    public AppInfoUtil(Context context) {
        pkgMgr = context.getPackageManager();
        predefinedRootApps = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.root_app_packages)));
    }

    private static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null)
            return false;
        if (searchStr.isEmpty())
            return true;
        return str.toLowerCase().contains(searchStr.toLowerCase());
    }

    private boolean isRootApp(ApplicationInfo appInfo) {
        boolean isXposedModule = appInfo.metaData != null && appInfo.metaData.containsKey("xposedmodule");
        return isXposedModule || predefinedRootApps.contains(appInfo.packageName);
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM;
    }

    public void refresh() {
        appInfoList.clear();

        Set<String> hiddenApps = PrefMgr.getHideApps();

        // Get applications info
        List<ApplicationInfo> installedApplications = pkgMgr.getInstalledApplications(GET_META_DATA | MATCH_DISABLED_COMPONENTS | MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo applicationInfo : installedApplications) {

            // Filter out Amarok itself
            if (applicationInfo.packageName.contains("deltazero.amarok"))
                continue;

            var appInfo = new AppInfo(
                    applicationInfo.packageName,
                    pkgMgr.getApplicationLabel(applicationInfo).toString(),
                    isSystemApp(applicationInfo),
                    isRootApp(applicationInfo),
                    pkgMgr.getApplicationIcon(applicationInfo)
            );

            appInfoList.add(appInfo);
        }

        // Sort with app name, with the hidden apps always on the top
        appInfoList.sort((o1, o2) -> {
            if (hiddenApps.contains(o1.packageName) && !hiddenApps.contains(o2.packageName))
                return -1;
            if (hiddenApps.contains(o2.packageName) && !hiddenApps.contains(o1.packageName))
                return 1;
            return (o1.label.compareTo(o2.label));
        });
    }

    public List<AppInfo> getFilteredApps(String query, boolean includeSystemApps, boolean includeRootApps) {
        List<AppInfo> filtered = new ArrayList<>();
        Set<String> hiddenApps = PrefMgr.getHideApps();

        for (AppInfo appInfo : appInfoList) {
            boolean query_filter_result = query == null || containsIgnoreCase(appInfo.label, query) || containsIgnoreCase(appInfo.packageName, query);
            boolean system_filter_result = includeSystemApps || !appInfo.isSystemApp || hiddenApps.contains(appInfo.packageName);
            boolean root_filter_result = includeRootApps || !appInfo.isRootApp || hiddenApps.contains(appInfo.packageName);
            if (query_filter_result && system_filter_result && root_filter_result)
                filtered.add(appInfo);
        }
        return filtered;
    }

    public record AppInfo(
            @NonNull String packageName,
            @NonNull String label,
            boolean isSystemApp,
            boolean isRootApp,
            Drawable icon
    ) {
    }
}
