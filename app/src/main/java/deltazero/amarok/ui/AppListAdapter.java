package deltazero.amarok.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppListHolder> {

    private final List<ApplicationInfo> lsAppInfo;
    private final PackageManager pkgMgr;
    private final PrefMgr prefMgr;

    private final LayoutInflater inflater;

    public AppListAdapter(Context context) {
        inflater = LayoutInflater.from(context);

        pkgMgr = context.getPackageManager();
        prefMgr = new PrefMgr(context);
        lsAppInfo = pkgMgr.getInstalledApplications(PackageManager.GET_META_DATA);

        // Remove system apps
        lsAppInfo.removeIf(a -> (a.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM);
        // Remove Amarok
        lsAppInfo.removeIf(a -> (Objects.equals(a.packageName, "deltazero.amarok")));
        // Sort with name
        lsAppInfo.sort(new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo o1, ApplicationInfo o2) {
                return pkgMgr.getApplicationLabel(o1).toString().compareTo(pkgMgr.getApplicationLabel(o2).toString());
            }
        });
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
        ApplicationInfo currAppInfo = lsAppInfo.get(position);
        holder.tvAppName.setText(pkgMgr.getApplicationLabel(currAppInfo));
        holder.cbIsHidden.setChecked(prefMgr.getHideApps().contains(currAppInfo.packageName));
        holder.tvPkgName.setText(currAppInfo.packageName);
        holder.ivAppIcon.setImageDrawable(pkgMgr.getApplicationIcon(currAppInfo)); // FIXME: Generate app icon realtime is slow, pre-generate it.
    }

    @Override
    public int getItemCount() {
        return lsAppInfo.size();
    }

    public class AppListHolder extends RecyclerView.ViewHolder implements MaterialCheckBox.OnCheckedChangeListener {

        // For every single item in the list

        public TextView tvAppName;
        public TextView tvPkgName;
        public MaterialCheckBox cbIsHidden;
        public ImageView ivAppIcon;
        AppListAdapter adapter;

        public AppListHolder(View view, AppListAdapter adapter) {
            super(view);
            this.adapter = adapter;

            // Init for adapter.onBindViewHolder
            tvAppName = view.findViewById(R.id.hideapp_tv_appname);
            tvPkgName = view.findViewById(R.id.hideapp_tv_pkgname);
            cbIsHidden = view.findViewById(R.id.hideapp_cb_ishidden);
            ivAppIcon = view.findViewById(R.id.hideapp_iv_appicon);

            cbIsHidden.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            String appPkgName = lsAppInfo.get(getLayoutPosition()).packageName;
            Set<String> hideApps = prefMgr.getHideApps();

            if (buttonView.isChecked()) {
                hideApps.add(appPkgName);
            } else {
                hideApps.remove(appPkgName);
            }
            prefMgr.setHideApps(hideApps);
        }
    }
}
