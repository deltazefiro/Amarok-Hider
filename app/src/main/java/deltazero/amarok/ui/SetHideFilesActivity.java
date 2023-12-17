package deltazero.amarok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.nio.file.Paths;
import java.util.Set;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.SDCardUtil;

public class SetHideFilesActivity extends AmarokActivity {

    private ActivityResultLauncher<Uri> mDirRequest;
    private static final String TAG = "SetHideFiles";
    private RecyclerView rvFileList;
    private FileListAdapter adapter;
    private MaterialToolbar tbToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidefiles);

        rvFileList = findViewById(R.id.hidefiles_rv_filelist);
        tbToolBar = findViewById(R.id.hidefiles_tb_toolbar);

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
                        String newPath;
                        newPath = getPathFromUri(uri);
                        if (newPath == null) {
                            Log.w(TAG, "Not supported Directory: " + uri);
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.not_local_storage)
                                    .setMessage(R.string.not_local_storage_description)
                                    .setPositiveButton(R.string.ok, null)
                                    .setNeutralButton(R.string.help, null)
                                    .show();
                            return;
                        }

                        Set<String> hideFilePath = PrefMgr.getHideFilePath();

                        // Check if the path is duplicated
                        var p2 = Paths.get(newPath).toAbsolutePath();
                        for (String p : hideFilePath) {
                            var p1 = Paths.get(p).toAbsolutePath();
                            String msg = null;

                            if (p1.startsWith(p2)) {
                                msg = getString(R.string.path_duplicated_description, newPath, p);
                            } else if (p2.startsWith(p1)) {
                                msg = getString(R.string.path_duplicated_description, p, newPath);
                            }

                            if (msg != null) {
                                new MaterialAlertDialogBuilder(this)
                                        .setTitle(R.string.path_duplicated)
                                        .setMessage(msg)
                                        .setPositiveButton(R.string.ok, null)
                                        .show();
                                return;
                            }
                        }

                        hideFilePath.add(newPath);
                        PrefMgr.setHideFilePath(hideFilePath);

                        adapter.lsPath.add(newPath);
                        adapter.notifyItemInserted(adapter.lsPath.size() - 1);

                        Log.i(TAG, "Added file hide path: " + newPath);

                    } else {
                        // request denied by user
                        Log.i(TAG, "Add file hide path cancelled");
                    }
                }
        );

        // Enable back button
        tbToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Nullable
    private String getPathFromUri(Uri uri) {
        String[] splitUri = uri.getPath().split(":");
        if (splitUri.length != 2) {
            return null;
        }

        // Check if removable storage (i.e. SD card)
        String path = SDCardUtil.getSdCardPathFromUri(this, splitUri);
        if (path != null) {
            return path;
        }

        // Check if external storage
        // FIXME: `com.android.externalstorage.documents` can not exactly filter out
        //  whether the path is in external storage or removable SdCard.
        if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
            return Environment.getExternalStorageDirectory() + File.separator + splitUri[1];
        }

        // Other virtual path
        return null;
    }

    public void addHideFolder(View view) {
        try {
            mDirRequest.launch(null);
        } catch (Exception e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.failed_to_open_doc_tree)
                    .setMessage(R.string.failed_to_open_doc_tree_description)
                    .show();
        }
    }
}