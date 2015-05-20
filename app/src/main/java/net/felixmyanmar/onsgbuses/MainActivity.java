package net.felixmyanmar.onsgbuses;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;



public class MainActivity extends AppCompatActivity {

    //    private RecyclerView.LayoutManager mLayoutManager;
//    private RecyclerView.Adapter mAdapter;
//    private List<List<String>> myDataset;
    ArrayList<String> allBusNos;


    @InjectView(R.id.cool_listView)
    ListView listView;

    @OnItemClick(R.id.cool_listView)
    void onItemClick(int position) {
        Toast.makeText(this, "Clicked position " + allBusNos.get(position) + "!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // dependency injection
        ButterKnife.inject(this);

        MyCoolDatabase myCoolDatabase = new MyCoolDatabase(this);
        allBusNos = myCoolDatabase.getAllBusServices();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.busservice_layout, allBusNos);
        listView.setAdapter(adapter);

//        recyclerView.setHasFixedSize(true);
//
//        mLayoutManager = new LinearLayoutManager(this);
//        recyclerView.setLayoutManager(mLayoutManager);
//
//        mAdapter = new CoolRecycleViewAdapter(myDataset);
//        recyclerView.setAdapter(mAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            SearchManager searchManager =
                    (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();

            ComponentName cn = new ComponentName(this, SearchResultsActivity.class);
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
}
