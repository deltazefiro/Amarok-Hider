package deltazero.amarok.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SelectHideAppActivity extends AppCompatActivity {

    private final LinkedList<String> appNames = new LinkedList<String>();
    private final LinkedList<String> appPkgNames = new LinkedList<String>();
    private final LinkedList<Boolean> isHiddenList = new LinkedList<Boolean>();
    private RecyclerView rvAppList;
    private AppListAdapter adapter;
    private Set<String> hiddenAppPkgNames;
    private PackageManager pkgMgr;

    private PrefMgr prefMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hideapp);

        pkgMgr = getPackageManager();
        rvAppList = findViewById(R.id.hideapp_rv_applist);

        prefMgr = new PrefMgr(this);
        hiddenAppPkgNames = prefMgr.getHideApps();

        // Get app info
        List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo a: apps) {
            appPkgNames.add(a.packageName);
            appNames.add((String) pkgMgr.getApplicationLabel(a));
            isHiddenList.add(hiddenAppPkgNames.contains(a.packageName));
        }

        Log.d("TEST", appNames.toString());

        // Inflate App list
        adapter = new AppListAdapter(this, appNames, isHiddenList);
        rvAppList.setAdapter(adapter);
        rvAppList.setLayoutManager(new LinearLayoutManager(this));

    }

}