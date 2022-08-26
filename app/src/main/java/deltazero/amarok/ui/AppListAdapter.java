package deltazero.amarok.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import deltazero.amarok.R;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppListHolder> {

    private final String[] appNames;
    private final boolean[] isHidden;

    private final LayoutInflater inflater;

    public AppListAdapter(Context context, String[] appNames, boolean[] isHidden) {
        inflater = LayoutInflater.from(context);
        this.appNames = appNames;
        this.isHidden = isHidden;
    }


    @NonNull
    @Override
    public AppListAdapter.AppListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = inflater.inflate(R.layout.item_hideapps, parent, false);
        return new AppListHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull AppListAdapter.AppListHolder holder, int position) {
        // Get current item
        holder.tvAppName.setText(appNames[position]);
        holder.cbIsHidden.setEnabled(isHidden[position]);
    }

    @Override
    public int getItemCount() {
        return appNames.length;
    }

    public class AppListHolder extends RecyclerView.ViewHolder {

        // For every single item in the list

        public TextView tvAppName;
        public MaterialCheckBox cbIsHidden;
        AppListAdapter adapter;

        public AppListHolder(View view, AppListAdapter adapter) {
            super(view);
            this.adapter = adapter;

            // Init for adapter.onBindViewHolder
            tvAppName = view.findViewById(R.id.hideapp_tv_appname);
            cbIsHidden = view.findViewById(R.id.hideapp_cb_ishidden);
        }
    }
}
