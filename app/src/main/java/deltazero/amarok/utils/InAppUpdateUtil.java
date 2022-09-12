package deltazero.amarok.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.DistributeListener;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.UpdateAction;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.ui.SettingsActivity;

public class InAppUpdateUtil {

    public static class AmarokDistributeListener implements DistributeListener {

        @Override
        public boolean onReleaseAvailable(Activity activity, ReleaseDetails releaseDetails) {

            String versionName = releaseDetails.getShortVersion();
            int versionCode = releaseDetails.getVersion();
            String releaseNotes = releaseDetails.getReleaseNotes();
            Uri releaseNotesUrl = releaseDetails.getReleaseNotesUrl();

            Log.i("CheckUpdate", "Found new update: Amarok " + versionName);

            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.update_ava)
                    .setMessage(activity.getString(R.string.update_description, versionName, versionCode))
                    .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Distribute.notifyUpdateAction(UpdateAction.UPDATE);
                        }
                    })
                    .setNegativeButton(R.string.never, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new PrefMgr(activity).setEnableAutoUpdate(false);
                        }
                    })
                    .setNeutralButton(R.string.cancel, null)
                    .show();

            return true;
        }

        @Override
        public void onNoReleaseAvailable(Activity activity) {
            if (activity instanceof SettingsActivity)
                Toast.makeText(activity, activity.getString(R.string.no_update_ava), Toast.LENGTH_SHORT).show();
            Log.i("CheckUpdate", "No available update yet.");
        }
    }

}