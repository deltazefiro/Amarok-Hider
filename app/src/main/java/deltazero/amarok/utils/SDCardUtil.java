package deltazero.amarok.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SDCardUtil {

    /**
     * Get path from uri which points to a path in SdCard.
     *
     * @param context  Context.
     * @param splitUri Uri split by `:`
     * @return If the uri is in the sdcard, return the path. Otherwise return null.
     */
    @Nullable
    public static String getSdCardPathFromUri(Context context, String[] splitUri) {
        assert splitUri.length == 2;

        List<String> sdCardPaths = new ArrayList<>();
        try {
            sdCardPaths = SDCardUtil.getSdCardPaths(context, false);
        } catch (Exception e) {
            Log.e("SDCardUtil", "Get sdcard path failed", e);
        }

        if (sdCardPaths == null || sdCardPaths.isEmpty()) {
            return null;
        }

        for (String sdCardPath : sdCardPaths) {
            if (splitUri[0].contains(Paths.get(sdCardPath).getFileName().toString())) {
                return sdCardPath + File.separator + splitUri[1];
            }
        }

        return null;
    }

    /**
     * Get absolute SdCard Path.
     * FIXME: 2022/12/31 THIS FUNCTION HAS NOT BEEN TESTED. IT MAY WORK DIFFERENTLY AND UNEXPECTEDLY ON DIFFERENT ANDROID VERSION!
     *
     * @param includePrimaryExternalStorage set to true if you wish to also include the path of the primary external storage
     * @return a list of all available sd cards paths, or null if not found.
     * Modified form @android developer's answer on stackoverflow.
     * @see <a href="https://stackoverflow.com/questions/11281010/how-can-i-get-the-external-sd-card-path-for-android-4-0/27197248#27197248">...</a>
     */
    public static List<String> getSdCardPaths(Context context, boolean includePrimaryExternalStorage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StorageManager storageManager = (StorageManager) context
                    .getSystemService(Context.STORAGE_SERVICE);
            List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
            if (!storageVolumes.isEmpty()) {
                StorageVolume primaryVolume = storageManager.getPrimaryStorageVolume();
                List<String> result = new ArrayList<>(storageVolumes.size());
                for (StorageVolume storageVolume : storageVolumes) {
                    String volumePath = getVolumePath(storageVolume);
                    if (volumePath == null) {
                        continue;
                    }
                    if (storageVolume.getMediaStoreVolumeName().equals(primaryVolume.getMediaStoreVolumeName())
                            || storageVolume.isPrimary()) {
                        if (includePrimaryExternalStorage)
                            result.add(volumePath);
                        continue;
                    }
                    result.add(volumePath);
                }
                return result.isEmpty() ? null : result;
            }
        }

        File[] externalCacheDirs = ContextCompat.getExternalCacheDirs(context);
        if (externalCacheDirs.length == 0)
            return null;
        if (externalCacheDirs.length == 1) {
            if (externalCacheDirs[0] == null)
                return null;
            String storageState = EnvironmentCompat.getStorageState(externalCacheDirs[0]);
            if (!Environment.MEDIA_MOUNTED.equals(storageState))
                return null;
            if (!includePrimaryExternalStorage && Environment.isExternalStorageEmulated())
                return null;
        }
        List<String> result = new ArrayList<>();
        if (externalCacheDirs[0] != null && (includePrimaryExternalStorage || externalCacheDirs.length == 1))
            result.add(getRootOfInnerSdCardFolder(context, externalCacheDirs[0]));
        for (int i = 1; i < externalCacheDirs.length; ++i) {
            File file = externalCacheDirs[i];
            if (file == null)
                continue;
            String storageState = EnvironmentCompat.getStorageState(file);
            if (Environment.MEDIA_MOUNTED.equals(storageState))
                result.add(getRootOfInnerSdCardFolder(context, externalCacheDirs[i]));
        }
        return result.isEmpty() ? null : result;
    }

    private static String getRootOfInnerSdCardFolder(Context context, File inputFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StorageManager storageManager = (StorageManager) context
                    .getSystemService(Context.STORAGE_SERVICE);
            StorageVolume storageVolume = storageManager.getStorageVolume(inputFile);
            if (storageVolume != null) {
                String result = getVolumePath(storageVolume);
                if (result != null)
                    return result;
            }
        }

        File file = inputFile;
        long totalSpace = file.getTotalSpace();
        while (true) {
            File parentFile = file.getParentFile();
            if (parentFile == null || parentFile.getTotalSpace() != totalSpace || !parentFile.canRead())
                return file.getAbsolutePath();
            file = parentFile;
        }
    }

    private static String getVolumePath(StorageVolume storageVolume) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return storageVolume.getDirectory().getAbsolutePath();
        }
        try {
            Method getPath = StorageVolume.class.getMethod("getPath");
            return (String) getPath.invoke(storageVolume);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}


