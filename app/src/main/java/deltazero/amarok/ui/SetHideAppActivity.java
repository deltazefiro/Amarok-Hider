package deltazero.amarok.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;

import deltazero.amarok.R;

public class SetHideAppActivity extends AppCompatActivity {

    private RecyclerView rvAppList;
    private AppListAdapter adapter;
    private MaterialToolbar tbToolBar;
    private SwipeRefreshLayout srLayout;

    private boolean showSystemApps = false;

    private String query = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hideapp);

        rvAppList = findViewById(R.id.hideapp_rv_applist);
        tbToolBar = findViewById(R.id.hideapp_tb_toolbar);
        srLayout = findViewById(R.id.hideapp_sr_layout);

        // Inflate App list
        adapter = new AppListAdapter(this, srLayout);
        rvAppList.setAdapter(adapter);
        rvAppList.setLayoutManager(new LinearLayoutManager(this));

        // Enable back button
        setSupportActionBar(tbToolBar);
        tbToolBar.setNavigationOnClickListener(v -> finish());

        // Setup onRefresh listener
        srLayout.setOnRefreshListener(() -> adapter.update(query, true, showSystemApps));

        // Setup menu
        addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_hideapp, menu);

                menu.findItem(R.id.display_system_apps).setChecked(showSystemApps);

                SearchView searchView = (SearchView) menu.findItem(R.id.action_search_app).getActionView();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    private boolean once = false;

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        query = newText.isEmpty() ? null : newText;
                        if (once) adapter.update(newText, false, showSystemApps);
                        else once = true;
                        return true;
                    }
                });
                searchView.setOnCloseListener(() -> {
                    query = null;
                    adapter.update(null, false, showSystemApps);
                    return true;
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.display_system_apps) {
                    showSystemApps = !showSystemApps;
                    menuItem.setChecked(showSystemApps);
                    adapter.update(query, false, showSystemApps);
                    return true;
                }
                return false;
            }
        });
    }

}