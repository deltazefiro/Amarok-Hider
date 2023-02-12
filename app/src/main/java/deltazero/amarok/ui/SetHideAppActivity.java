package deltazero.amarok.ui;

import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import deltazero.amarok.R;

public class SetHideAppActivity extends AppCompatActivity {

    private RecyclerView rvAppList;
    private AppListAdapter adapter;
    private MaterialToolbar tbToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hideapp);

        rvAppList = findViewById(R.id.hideapp_rv_applist);
        tbToolBar = findViewById(R.id.hideapp_tb_toolbar);

        // Inflate App list
        adapter = new AppListAdapter(this);
        rvAppList.setAdapter(adapter);
        rvAppList.setLayoutManager(new LinearLayoutManager(this));

        // Enable back button
        setSupportActionBar(tbToolBar);
        tbToolBar.setNavigationOnClickListener(v -> finish());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_hideapp, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search_app).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            private boolean once = false;

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (once) adapter.update(newText);
                else once = true;
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            adapter.update(null);
            return true;
        });

        return super.onCreateOptionsMenu(menu);
    }
}