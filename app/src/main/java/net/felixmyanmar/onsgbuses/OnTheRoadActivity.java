package net.felixmyanmar.onsgbuses;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class OnTheRoadActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    ArrayList<BusStops> busStops;
    BusRVAdapter mAdapter;

    @InjectView(R.id.cool_recycler_view)
    RecyclerView recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ontheroad);
        ButterKnife.inject(this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Intent touchIntent = getIntent();
        String service_no = touchIntent.getStringExtra("service_id");
        int direction = touchIntent.getIntExtra("position", 1);

        CoolDatabase db = new CoolDatabase(this);
        ArrayList<BusStops> routeStartStops = db.getBusStops(service_no, direction, false);
        ArrayList<BusStops> routeEndStops = db.getBusStops(service_no, direction, true);

        Set<BusStops> container = new LinkedHashSet<>(routeStartStops);
        container.addAll(routeEndStops);
        busStops = new ArrayList<>(container);

        mAdapter = new BusRVAdapter(this, busStops);
        recyclerView.setAdapter(mAdapter);
    }

    /**
     * Called when the user submits the query. This could be due to a key press on the
     * keyboard or due to pressing a submit button.
     * The listener can override the standard behavior by returning true
     * to indicate that it has handled the submit request. Otherwise return false to
     * let the SearchView handle the submission by launching any associated intent.
     *
     * @param query the query text that is to be submitted
     * @return true if the query has been handled by the listener, false to let the
     * SearchView perform the default action.
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    /**
     * Called when the query text is changed by the user.
     *
     * @param newText the new content of the query text field.
     * @return false if the SearchView should perform the default action of showing any
     * suggestions if available, true if the action was handled by the listener.
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            mAdapter.getFilter().filter("");
        } else {
            mAdapter.getFilter().filter(newText);
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setQueryHint("Search Bus Stop No");
            searchView.setOnQueryTextListener(this);

        }
        return true;

    }
}
