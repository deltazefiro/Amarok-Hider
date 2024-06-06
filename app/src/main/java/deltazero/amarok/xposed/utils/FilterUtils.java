package deltazero.amarok.xposed.utils;

import android.annotation.SuppressLint;

import com.github.kyuubiran.ezxhelper.Log;
import com.github.kyuubiran.ezxhelper.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FilterUtils {

    // NOTE: This method will not refresh XPref cache. Call refreshCache() before calling this method
    @SuppressLint("DefaultLocale")
    public static List<Object> filterAppsOrPkgs(List<Object> appsOrPkgs, Method m) {
        if (!XPref.isXHideActive()) return appsOrPkgs;

        AtomicInteger filteredCount = new AtomicInteger();
        var filteredPackages = appsOrPkgs.stream()
                .filter(appOrPkg -> {
                    boolean shouldHide;
                    try {
                        shouldHide = XPref.shouldHide(ObjectUtils.getObjectOrNullUntilSuperclassAs(
                                appOrPkg, "packageName", null));
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                    if (shouldHide) filteredCount.getAndIncrement();
                    return !shouldHide;
                })
                .collect(java.util.stream.Collectors.toList());

        Log.d(String.format("%s$%s(%s): Filtered %d packages",
                m.getDeclaringClass().getSimpleName(), m.getName(), Arrays.toString(m.getParameterTypes()),
                filteredCount.get()), null);
        return filteredPackages;
    }

    // NOTE: This method will not refresh XPref cache. Call refreshCache() before calling this method
    public static Object filterAppsOrPkgsInSlices(Object /* ParceledListSlice<*> */ appsOrPkgs, Method m) {
        List<Object> slice = ParceledListSliceUtil.sliceToList(appsOrPkgs);
        return ParceledListSliceUtil.listToSlice(filterAppsOrPkgs(slice, m));
    }
}