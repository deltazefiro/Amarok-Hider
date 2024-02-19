package deltazero.amarok.xposed.hooks;

import static deltazero.amarok.xposed.utils.XPref.isXHideActive;
import static deltazero.amarok.xposed.utils.XPref.shouldHide;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.github.kyuubiran.ezxhelper.ClassUtils;
import com.github.kyuubiran.ezxhelper.HookFactory;
import com.github.kyuubiran.ezxhelper.Log;
import com.github.kyuubiran.ezxhelper.finders.MethodFinder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;
import deltazero.amarok.xposed.utils.ParceledListSliceUtil;

public class HookTarget33 extends BaseHook {

    private XC_MethodHook.Unhook unhook1, unhook2;

    @Override
    public String getName() {
        return "HookTarget33";
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public void init() {
        Log.d("Loading HookTarget33", null);

        try {
            // parceledListSliceClass = ClassUtils.loadClass("android.content.pm.ParceledListSlice", null);
            var packageManagerBaseClass = ClassUtils.loadClass("com.android.server.pm.IPackageManagerBase", null);

            /*
            public final ParceledListSlice<PackageInfo> getInstalledPackages(
                    @PackageManager.PackageInfoFlagsBits long flags, int userId)
             */
            var m1 = MethodFinder.fromClass(packageManagerBaseClass)
                    .filterByName("getInstalledPackages")
                    .first();

            unhook1 = HookFactory.createMethodHook(m1, hookFactory -> hookFactory.after(param -> {
                try {
                    if (!isXHideActive()) return;

                    List<PackageInfo> packages = ParceledListSliceUtil.sliceToList(param.getResult());
                    AtomicInteger filteredCount = new AtomicInteger();

                    var filteredPackages = packages.stream()
                            .filter(pkg -> {
                                boolean shouldHide = shouldHide(pkg.packageName);
                                if (shouldHide) filteredCount.getAndIncrement();
                                return !shouldHide;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    Log.i("Filtered getInstalledPackages. Filtered " + filteredCount.get() + " packages.", null);
                    param.setResult(ParceledListSliceUtil.listToSlice(filteredPackages));
                } catch (Exception e) {
                    Log.e("Error in getInstalledPackages hook", e);
                    unhook1.unhook();
                }
            }));

            /*
            public final ParceledListSlice<ApplicationInfo> getInstalledApplications(
                    @PackageManager.ApplicationInfoFlagsBits long flags, int userId)
             */
            var m2 = MethodFinder.fromClass(packageManagerBaseClass)
                    .filterByName("getInstalledApplications")
                    .first();

            unhook2 = HookFactory.createMethodHook(m2, hookFactory -> hookFactory.after(param -> {
                try {
                    if (!isXHideActive()) return;

                    List<ApplicationInfo> apps = ParceledListSliceUtil.sliceToList(param.getResult());
                    AtomicInteger filteredCount = new AtomicInteger();

                    var filteredApps = apps.stream()
                            .filter(app -> {
                                boolean shouldHide = shouldHide(app.packageName);
                                if (shouldHide) filteredCount.getAndIncrement();
                                return !shouldHide;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    Log.i("Filtered getInstalledApplications. Filtered " + filteredCount.get() + " apps.", null);
                    param.setResult(ParceledListSliceUtil.listToSlice(filteredApps));
                } catch (Exception e) {
                    Log.e("Error in getInstalledApplications hook", e);
                    unhook2.unhook();
                }
            }));

        } catch (Exception e) {
            Log.e("Error loading HookTarget33", e);
        } finally {
            Log.i("HookTarget33 loaded.", null);
        }
    }
}
