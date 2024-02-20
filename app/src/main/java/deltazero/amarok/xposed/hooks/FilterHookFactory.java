package deltazero.amarok.xposed.hooks;

import com.github.kyuubiran.ezxhelper.ClassUtils;
import com.github.kyuubiran.ezxhelper.HookFactory;
import com.github.kyuubiran.ezxhelper.Log;
import com.github.kyuubiran.ezxhelper.finders.MethodFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import deltazero.amarok.xposed.utils.FilterUtils;

public class FilterHookFactory {
    public static List<IHook> build(String className, String methodName, boolean isSlice) {
        Class<?> c;
        try {
            c = ClassUtils.loadClass(className, null);
        } catch (ClassNotFoundException e) {
            Log.e(String.format("Error when initializing: %s$%s", className, methodName), e);
            return Collections.emptyList();
        }

        var methods = MethodFinder.fromClass(c).filterByName(methodName).toSet();
        if (methods.isEmpty()) {
            Log.e(String.format("Method not found: %s$%s", methodName, c), null);
            return Collections.emptyList();
        }

        List<IHook> hooks = new ArrayList<>();
        for (var m : methods) {
            hooks.add(new IHook() {

                private XC_MethodHook.Unhook unhook;

                @Override
                public String getName() {
                    return m.getName();
                }

                /** @noinspection unchecked*/
                @Override
                public void load() {
                    Log.d(String.format("Loading hook: %s", m), null);
                    try {
                        unhook = HookFactory.createMethodHook(m, hookFactory -> hookFactory.after(param -> {
                            try {
                                param.setResult(isSlice
                                        ? FilterUtils.filterAppsOrPkgsInSlices(param.getResult(), m)
                                        : FilterUtils.filterAppsOrPkgs((List<Object>) param.getResult(), m));
                            } catch (Exception e) {
                                Log.e(String.format("Error while hooking %s", m), e);
                                unhook.unhook();
                            }
                        }));
                    } catch (Exception e) {
                        Log.e(String.format("Hooked failed: %s", m), e);
                    } finally {
                        Log.i(String.format("Method hooked: %s", m), null);
                    }
                }

            });
        }
        return hooks;
    }
}
