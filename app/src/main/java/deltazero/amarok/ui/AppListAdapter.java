package deltazero.amarok.ui;

import static deltazero.amarok.utils.AppInfoUtil.AppInfo;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppInfoUtil;

public class AppListAdapter extends ListAdapter<AppInfo, AppListAdapter.AppListHolder> {

    private final PackageManager pkgMgr;
    private final PrefMgr prefMgr;
    private final AppInfoUtil appInfoUtil;

    private final LayoutInflater inflater;
    private Set<String> hiddenApps;

    private static final HandlerThread backgroundThread = new HandlerThread("APP_ADAPTER_THREAD");
    private final Handler backgroundHandler;
    private boolean isRefreshing = false;

    private final SetHideAppActivity activity;
    private final SwipeRefreshLayout srLayout;

    public AppListAdapter(SetHideAppActivity activity, SwipeRefreshLayout srLayout) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
                return Objects.equals(oldItem.packageName, newItem.packageName);
            }

            @Override
            public boolean areContentsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
                return areItemsTheSame(oldItem, newItem);
            }
        });

        inflater = LayoutInflater.from(activity);

        if (backgroundThread.getState() == Thread.State.NEW)
            backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        pkgMgr = activity.getPackageManager();
        prefMgr = new PrefMgr(activity);
        appInfoUtil = new AppInfoUtil(activity);

        this.activity = activity;
        this.srLayout = srLayout;

        update(null, true);
    }

    @NonNull
    @Override
    public AppListAdapter.AppListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the viewHolder
        View mItemView = inflater.inflate(R.layout.item_hideapps, parent, false);
        return new AppListHolder(mItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AppListAdapter.AppListHolder holder, int position) {
        // Run for each item
        AppInfo currAppInfo = getCurrentList().get(position);
        holder.tvAppName.setText(currAppInfo.label);
        holder.cbIsHidden.setChecked(prefMgr.getHideApps().contains(currAppInfo.packageName));
        holder.tvPkgName.setText(currAppInfo.packageName);
        holder.ivAppIcon.setImageDrawable(currAppInfo.icon);
    }


    public void update(String query, boolean fullUpdate) {

        // Refreshing thread lock
        if (isRefreshing) return;
        else isRefreshing = true;

        if (fullUpdate) srLayout.setRefreshing(true);

        backgroundHandler.post(() -> {
            // Refresh installed apps
            if (fullUpdate) appInfoUtil.update();
            // Get app info
            List<AppInfo> lsAppInfo = appInfoUtil.getInstalledApps(query);
            // Sort with app name
            hiddenApps = prefMgr.getHideApps();
            lsAppInfo.sort((o1, o2) -> {
                if (hiddenApps.contains(o1.packageName) && !hiddenApps.contains(o2.packageName))
                    return -1;
                if (hiddenApps.contains(o2.packageName) && !hiddenApps.contains(o1.packageName))
                    return 1;
                return (o1.label.compareTo(o2.label));
            });
            // Notify update
            activity.runOnUiThread(() -> {
                submitList(lsAppInfo);
                srLayout.setRefreshing(false);
                isRefreshing = false;
            });
        });
    }

    public class AppListHolder extends RecyclerView.ViewHolder implements MaterialCheckBox.OnCheckedChangeListener {

        // For every single item in the list

        public TextView tvAppName, tvPkgName;
        public MaterialCheckBox cbIsHidden;
        public ImageView ivAppIcon;

        public AppListHolder(View view) {
            super(view);

            // Init for adapter.onBindViewHolder
            tvAppName = view.findViewById(R.id.hideapp_tv_appname);
            tvPkgName = view.findViewById(R.id.hideapp_tv_pkgname);
            cbIsHidden = view.findViewById(R.id.hideapp_cb_ishidden);
            ivAppIcon = view.findViewById(R.id.hideapp_iv_appicon);

            cbIsHidden.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            String appPkgName = getCurrentList().get(getLayoutPosition()).packageName;
            hiddenApps = prefMgr.getHideApps();

            if (buttonView.isChecked()) {
                hiddenApps.add(appPkgName);
            } else {
                hiddenApps.remove(appPkgName);
            }
            prefMgr.setHideApps(hiddenApps);
        }
    }
}
