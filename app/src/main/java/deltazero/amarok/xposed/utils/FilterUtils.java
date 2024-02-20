package deltazero.amarok.xposed.utils;

import static deltazero.amarok.xposed.utils.XPref.isXHideActive;
import static deltazero.amarok.xposed.utils.XPref.shouldHide;

import android.annotation.SuppressLint;

import com.github.kyuubiran.ezxhelper.Log;
import com.github.kyuubiran.ezxhelper.ObjectUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FilterUtils {

    @SuppressLint("DefaultLocale")
    public static List<Object> filterAppsOrPkgs(List<Object> appsOrPkgs, Method m) {
        if (!isXHideActive()) return appsOrPkgs;

        AtomicInteger filteredCount = new AtomicInteger();
        var filteredPackages = appsOrPkgs.stream()
                .filter(appOrPkg -> {
                    boolean shouldHide;
                    try {
                        shouldHide = shouldHide(ObjectUtils.getObjectOrNullAs(appOrPkg, "packageName"));
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                    if (shouldHide) filteredCount.getAndIncrement();
                    return !shouldHide;
                })
                .collect(java.util.stream.Collectors.toList());

        Log.i(String.format("Filtered %d packages: %s", filteredCount.get(), m.getName()), null);
        return filteredPackages;
    }

    public static Object filterAppsOrPkgsInSlices(Object /* ParceledListSlice<*> */ appsOrPkgs, Method m) {
        List<Object> slice = ParceledListSliceUtil.sliceToList(appsOrPkgs);
        return ParceledListSliceUtil.listToSlice(filterAppsOrPkgs(slice, m));
    }
}