package deltazero.amarok.ui;

import static android.content.pm.PackageManager.GET_META_DATA;
import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
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

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class AppListAdapter extends ListAdapter<ApplicationInfo, AppListAdapter.AppListHolder> {

    private final PackageManager pkgMgr;
    private final PrefMgr prefMgr;

    private final LayoutInflater inflater;
    private Set<String> hiddenApps;

    private static final HandlerThread backgroundThread = new HandlerThread("APP_ADAPTER_THREAD");
    private final Handler backgroundHandler;
    private boolean isRefreshing = false;

    private final Activity activity;

    public AppListAdapter(Activity activity) {

        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull ApplicationInfo oldItem, @NonNull ApplicationInfo newItem) {
                return Objects.equals(oldItem.packageName, newItem.packageName);
            }

            @Override
            public boolean areContentsTheSame(@NonNull ApplicationInfo oldItem, @NonNull ApplicationInfo newItem) {
                return areItemsTheSame(oldItem, newItem);
            }
        });

        inflater = LayoutInflater.from(activity);

        if (backgroundThread.getState() == Thread.State.NEW)
            backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        pkgMgr = activity.getPackageManager();
        prefMgr = new PrefMgr(activity);

        this.activity = activity;

        update(null);
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
        ApplicationInfo currAppInfo = getCurrentList().get(position);
        holder.tvAppName.setText(pkgMgr.getApplicationLabel(currAppInfo));
        holder.cbIsHidden.setChecked(prefMgr.getHideApps().contains(currAppInfo.packageName));
        holder.tvPkgName.setText(currAppInfo.packageName);
        holder.ivAppIcon.setImageDrawable(pkgMgr.getApplicationIcon(currAppInfo)); // FIXME: Generate app icon realtime is slow, pre-generate it.
    }

    private static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }

    public void update(String query) {

        if (isRefreshing) return;
        else isRefreshing = true;

        backgroundHandler.post(() -> {
            // Get app info and labels
            List<ApplicationInfo> lsAppInfo = pkgMgr.getInstalledApplications(GET_META_DATA | MATCH_DISABLED_COMPONENTS | MATCH_UNINSTALLED_PACKAGES);
            HashMap<String, String> pkgNameToLabel = new HashMap<>();
            for (var appInfo : lsAppInfo) {
                pkgNameToLabel.put(appInfo.packageName, pkgMgr.getApplicationLabel(appInfo).toString());
            }

            // Remove system apps
            lsAppInfo.removeIf(a -> (a.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM);
            // Remove Amarok
            lsAppInfo.removeIf(a -> (a.packageName.contains("deltazero.amarok")));
            // Apply search filter
            if (query != null && !query.isEmpty()) {
                lsAppInfo.removeIf(a -> (
                        !containsIgnoreCase(a.packageName, query)
                                && !containsIgnoreCase(pkgNameToLabel.get(a.packageName), query)));
            }

            // Sort with app name
            hiddenApps = prefMgr.getHideApps();
            lsAppInfo.sort((o1, o2) -> {
                if (hiddenApps.contains(o1.packageName) && !hiddenApps.contains(o2.packageName))
                    return -1;
                if (hiddenApps.contains(o2.packageName) && !hiddenApps.contains(o1.packageName))
                    return 1;
                return (Objects.requireNonNull(pkgNameToLabel.get(o1.packageName))
                        .compareTo(Objects.requireNonNull(pkgNameToLabel.get(o2.packageName))));
            });

            // Notify update
            activity.runOnUiThread(() -> {
                submitList(lsAppInfo);
            });
            isRefreshing = false;
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
