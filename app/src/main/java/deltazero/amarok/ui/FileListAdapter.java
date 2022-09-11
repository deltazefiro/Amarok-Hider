package deltazero.amarok.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.LinkedList;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final LayoutInflater inflater;
    private final LinkedList<String> lsPath;
    private final PrefMgr prefMgr;
    private final Context context;

    private final int TYPE_FILE_ITEM = 0;
    private final int TYPE_FOOTAGE = 1;

    public FileListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        prefMgr = new PrefMgr(context);
        lsPath = new LinkedList<>(prefMgr.getHideFilePath());
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTAGE)
            return new FootageHolder(inflater.inflate(R.layout.item_hidefiles_footage, parent, false));
        return new FileListHolder(inflater.inflate(R.layout.item_hidefiles, parent, false));
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

    public class FileListHolder extends RecyclerView.ViewHolder implements DialogInterface.OnClickListener {

        public MaterialTextView tvFolderName, tvPath;

        public FileListHolder(@NonNull View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.hidefile_tv_foldername);
            tvPath = itemView.findViewById(R.id.hidefile_tv_path);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.delete_hide_path)
                    .setMessage(context.getString(R.string.delete_hide_path_description, tvPath.getText()))
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Set<String> hideFilePath = prefMgr.getHideFilePath();
                            hideFilePath.remove(lsPath.get(which));
                            prefMgr.setHideFilePath(hideFilePath);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);
        }
    }

    public static class FootageHolder extends RecyclerView.ViewHolder {

        public FootageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
