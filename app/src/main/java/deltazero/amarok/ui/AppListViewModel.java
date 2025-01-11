package deltazero.amarok.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.utils.AppInfoUtil;
import deltazero.amarok.utils.AppInfoUtil.AppInfo;

public class AppListViewModel extends AndroidViewModel {
    private final AppInfoUtil appInfoUtil;
    private final Executor executor;

    private final MutableLiveData<List<AppInfo>> appList = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> showSystemApps = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showRootApps = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AppListViewModel(@NonNull Application application) {
        super(application);
        appInfoUtil = new AppInfoUtil(application);
        executor = Executors.newSingleThreadExecutor();
        refreshApps();
    }

    public LiveData<List<AppInfo>> getAppList() {
        return appList;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getShowSystemApps() {
        return showSystemApps;
    }

    public LiveData<Boolean> getShowRootApps() {
        return showRootApps;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
        updateAppList();
    }

    public void toggleSystemApps() {
        Boolean current = showSystemApps.getValue();
        showSystemApps.setValue(current == null || !current);
        updateAppList();
    }

    public void toggleRootApps() {
        Boolean current = showRootApps.getValue();
        showRootApps.setValue(current == null || !current);
        updateAppList();
    }

    public void refreshApps() {
        isLoading.setValue(true);
        executor.execute(() -> {
            appInfoUtil.refresh();
            updateAppList();
            isLoading.postValue(false);
        });
    }

    public void toggleAppHidden(AppInfo app) {
        Set<String> hiddenApps = PrefMgr.getHideApps();
        if (hiddenApps.contains(app.packageName())) {
            hiddenApps.remove(app.packageName());
        } else {
            hiddenApps.add(app.packageName());
        }
        PrefMgr.setHideApps(hiddenApps);
    }

    private void updateAppList() {
        executor.execute(() -> {
            String query = searchQuery.getValue();
            Boolean showSystem = showSystemApps.getValue();
            Boolean showRoot = showRootApps.getValue();
            List<AppInfo> filtered = appInfoUtil.getFilteredApps(
                    query == null || query.isEmpty() ? null : query,
                    showSystem != null && showSystem,
                    showRoot != null && showRoot
            );
            appList.postValue(filtered);
        });
    }
} 