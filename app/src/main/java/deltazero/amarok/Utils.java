package deltazero.amarok;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//import com.catchingnow.icebox.sdk_client.IceBox;

public class Utils {
//
//    private static int PERMISSION_REQUEST_CODE = 100;
//
//    private boolean checkStoragePermission(Context context) {
//        if (SDK_INT >= Build.VERSION_CODES.R) {
//            // For android 11+
//            return Environment.isExternalStorageManager();
//        } else {
//            int read = ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE);
//            int write = ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE);
//            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
//        }
//    }
//
//    private void requestStoragePermission(Context context) {
//        if (SDK_INT >= Build.VERSION_CODES.R) {
//            try {
//                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//                intent.addCategory("android.intent.category.DEFAULT");
//                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
//                startActivityForResult(intent, 2296);
//            } catch (Exception e) {
//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivityForResult(intent, 2296);
//            }
//        } else {
//            //below android 11
//            ActivityCompat.requestPermissions(context, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
//        }
//    }

//    static public IceBox.SilentInstallSupport getIceboxAvailability(Context context) {
//        try {
//            context.getPackageManager().getPackageInfo("com.catchingnow.icebox", 0);
//            return IceBox.querySupportSilentInstall(context);
//        } catch (PackageManager.NameNotFoundException e) {
//            return IceBox.SilentInstallSupport.NOT_INSTALLED;
//        }
//    }
}
