package deltazero.amarok.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

import deltazero.amarok.filehider.ChmodFileHider;
import deltazero.amarok.filehider.NoMediaFileHider;
import deltazero.amarok.filehider.ObfuscateFileHider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public final LinkedList<String> lsPath;
    private final LayoutInflater inflater;
    private final Context context;

    private final int TYPE_FILE_ITEM = 0;
    private final int TYPE_FOOTAGE = 1;

    public FileListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        lsPath = new LinkedList<>(PrefMgr.getHideFilePath());
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTAGE)
            return new FootageHolder(inflater.inflate(R.layout.item_hidefiles_footage, parent, false), this);
        return new FileListHolder(inflater.inflate(R.layout.item_hidefiles, parent, false), this);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < lsPath.size()) {
            String currPath = lsPath.get(position);
            ((FileListHolder) holder).tvFolderName.setText(
                    currPath.substring(currPath.lastIndexOf(File.separator) + 1)
            );
            ((FileListHolder) holder).tvPath.setText(currPath);
        }
    }

    @Override
    public int getItemCount() {
        return lsPath.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == lsPath.size())
            return TYPE_FOOTAGE;
        return TYPE_FILE_ITEM;
    }

    public class FileListHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public MaterialTextView tvFolderName, tvPath;
        public LinearLayout llPathItem;
        private final FileListAdapter adapter;

        public FileListHolder(@NonNull View itemView, FileListAdapter adapter) {
            super(itemView);
            this.adapter = adapter;

            tvFolderName = itemView.findViewById(R.id.hidefile_tv_foldername);
            tvPath = itemView.findViewById(R.id.hidefile_tv_path);
            llPathItem = itemView.findViewById(R.id.hideapp_ll_pathitem);

            llPathItem.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.remove_hide_path)
                    .setMessage(context.getString(R.string.remove_hide_path_description, tvPath.getText()))
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Set<String> hideFilePath = PrefMgr.getHideFilePath();
                            hideFilePath.remove(lsPath.get(getLayoutPosition()));
                            PrefMgr.setHideFilePath(hideFilePath);

                            lsPath.remove(getLayoutPosition());
                            adapter.notifyItemRemoved(getAdapterPosition());
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    public class FootageHolder extends RecyclerView.ViewHolder {

        public FootageHolder(@NonNull View itemView, FileListAdapter adapter) {
            super(itemView);

            MaterialButton btAddFolder = itemView.findViewById(R.id.hidefiles_bt_add_folder);
            btAddFolder.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    MaterialAlertDialogBuilder alertBuilder = new MaterialAlertDialogBuilder(itemView.getContext())
                            .setTitle(R.string.manually_set_path)
                            .setMessage(R.string.manually_set_path_description)
                            .setNeutralButton(R.string.cancel, null);

                    View dlPathInput = LayoutInflater.from(alertBuilder.getContext()).inflate(R.layout.dialog_path_input, null);
                    EditText etPathInput = dlPathInput.findViewById(R.id.dialog_path_input_et_input);
                    etPathInput.setHint(Environment.getExternalStorageDirectory().getPath() + "/...");

                    alertBuilder.setView(dlPathInput)
                            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                String input = Objects.requireNonNull(etPathInput.getText()).toString();

                                /* Check for experiment flags */
                                switch (input) {
                                    case "#nomedia-filehider" -> {
                                        PrefMgr.setFileHiderMode(NoMediaFileHider.class);
                                        Toast.makeText(context, "NoMedia filehider activated.", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    case "#obfuscate-filehider" -> {
                                        PrefMgr.setFileHiderMode(ObfuscateFileHider.class);
                                        Toast.makeText(context, "Obfuscate filehider activated.", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    case "#chmod-filehider" -> {
                                        PrefMgr.setFileHiderMode(ChmodFileHider.class);
                                        Toast.makeText(context, "Chmod filehider activated.", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                }

                                /* Check path availability */
                                try {
                                    Paths.get(input);
                                } catch (Exception e) {
                                    Log.w("FilePicker", String.format("Invalid path %s: ", input), e);
                                    Toast.makeText(context, R.string.invalid_path, Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Set<String> hideFilePaths = PrefMgr.getHideFilePath();
                                hideFilePaths.add(input);
                                PrefMgr.setHideFilePath(hideFilePaths);

                                lsPath.add(input);
                                adapter.notifyItemInserted(getAdapterPosition());
                            })
                            .show();
                    return true;
                }
            });

        }
    }
}
