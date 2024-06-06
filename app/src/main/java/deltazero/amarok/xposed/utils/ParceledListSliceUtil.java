package deltazero.amarok.xposed.utils;

import static com.github.kyuubiran.ezxhelper.ClassUtils.loadClass;
import static com.github.kyuubiran.ezxhelper.ClassUtils.newInstanceBestMatch;
import static com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch;

import com.github.kyuubiran.ezxhelper.Log;

import java.util.List;

public class ParceledListSliceUtil {
    private static Class<?> parceledListSliceClass;

    public static void init() {
        Log.d("Initializing ParceledListSliceUtil...", null);
        try {
            parceledListSliceClass = loadClass("android.content.pm.ParceledListSlice", null);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Log.d("ParceledListSliceUtil initialized.", null);
    }

    /**
     * @noinspection unchecked
     */
    public static <T> List<T> sliceToList(Object slice) {
        try {
            return (List<T>) invokeMethodBestMatch(slice, "getList", null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    public static Object listToSlice(List<?> list) {
        try {
            return newInstanceBestMatch(parceledListSliceClass, list);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
