package net.felixmyanmar.onsgbuses.app;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Fade;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.felixmyanmar.onsgbuses.R;
import net.felixmyanmar.onsgbuses.database.CoolDatabase;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;


public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    ArrayList<String> allBusNos;
    ArrayAdapter<String> adapter;

    @InjectView(R.id.cool_listView) ListView listView;

    @OnItemClick(R.id.cool_listView) void onItemClick(int position) {

        Intent intent = new Intent(this, TerminalsActivity.class);
        intent.putExtra("service_id", adapter.getItem(position));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setExitTransition(new Fade());
            startActivity(intent,
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        } else {
            startActivity(intent);
        }
    }

    @InjectView(R.id.toolbar) Toolbar toolBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // dependency injection
        ButterKnife.inject(this);
        setTitle("");
        setSupportActionBar(toolBar);

        CoolDatabase coolDatabase = new CoolDatabase(this);
        allBusNos = coolDatabase.getAllBusServices();

        adapter = new ArrayAdapter<>(this,
                R.layout.listviewitem_service, allBusNos);
        listView.setAdapter(adapter);
        listView.setTextFilterEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            SearchManager searchManager =
                    (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setQueryHint("Search Bus");
            searchView.setOnQueryTextListener(this);

            ComponentName cn = new ComponentName(this, TerminalsActivity.class);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(cn));
            searchView.setIconifiedByDefault(false);
        }
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                onSearchRequested();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText))
            listView.clearTextFilter();
        else
            listView.setFilterText(newText);
        return true;
    }
}
