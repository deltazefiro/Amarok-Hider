package deltazero.amarok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SetHideFilesActivity extends AppCompatActivity {

    private ActivityResultLauncher<Uri> mDirRequest;
    private PrefMgr prefMgr;
    private static final String TAG = "SetHideFiles";
    private RecyclerView rvFileList;
    private FileListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidefiles);

        prefMgr = new PrefMgr(this);

        rvFileList = findViewById(R.id.hidefiles_rv_filelist);

        // Inflate File list
        adapter = new FileListAdapter(this);
        rvFileList.setAdapter(adapter);
        rvFileList.setLayoutManager(new LinearLayoutManager(this));

        // Register file-picker result handler
        mDirRequest = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        // call this to persist permission across device reboots
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        // set hide path
                        // FIXME: Assign a not local file may cause an error.
                        String path;
                        try {
                            path = Environment.getExternalStorageDirectory() + "/" + uri.getPath().split(":")[1];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Log.w(TAG, "Not supported Directory: " + uri);

                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.not_local_storage)
                                    .setMessage(R.string.not_local_storage_description)
                                    .setPositiveButton(R.string.ok, null)
                                    .setNeutralButton(R.string.help, null)
                                    .show();
                            return;
                        }

                        Set<String> hideFilePath = prefMgr.getHideFilePath();
                        hideFilePath.add(path);
                        prefMgr.setHideFilePath(hideFilePath);

                        adapter.lsPath.add(path);
                        adapter.notifyItemInserted(adapter.lsPath.size() - 1);

                        Log.i(TAG, "Added file hide path: " + path);

                    } else {
                        // request denied by user
                        Log.i(TAG, "Add file hide path cancelled");
                    }
                }
        );

    }

    public void addHideFolder(View view) {
        mDirRequest.launch(null);
    }
}