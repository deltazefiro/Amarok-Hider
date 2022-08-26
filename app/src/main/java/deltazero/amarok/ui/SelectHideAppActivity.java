package deltazero.amarok.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import deltazero.amarok.R;

public class SelectHideAppActivity extends AppCompatActivity {

    private RecyclerView rvAppList;
    private AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hideapp);

        rvAppList = findViewById(R.id.hideapp_rv_applist);

        adapter = new AppListAdapter(this, null, null);
        rvAppList.setAdapter(adapter);
        rvAppList.setLayoutManager(new LinearLayoutManager(this));

    }
}