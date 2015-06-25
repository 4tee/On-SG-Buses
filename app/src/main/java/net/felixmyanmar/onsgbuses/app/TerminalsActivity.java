package net.felixmyanmar.onsgbuses.app;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import net.felixmyanmar.onsgbuses.R;
import net.felixmyanmar.onsgbuses.container.BusRoute;
import net.felixmyanmar.onsgbuses.container.BusStops;
import net.felixmyanmar.onsgbuses.container.BusTerminal;
import net.felixmyanmar.onsgbuses.database.CoolDatabase;
import net.felixmyanmar.onsgbuses.helper.MyRecentSuggestionProvider;
import net.felixmyanmar.onsgbuses.helper.SharedPreferenceHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class TerminalsActivity extends AppCompatActivity {

    ArrayList<BusRoute> busRoutes;

    @InjectView(R.id.cool_recycler_view)
    RecyclerView recyclerView;

    @InjectView(R.id.cool_textView)
    TextView textView;

    @InjectView(R.id.toolbar)
    Toolbar toolBar;

    @InjectView(R.id.lbl_title)
    TextView titleLabel;

    @Override
    protected void onResume() {
        super.onResume();

        // Clear all alarm stops
        SharedPreferenceHelper.clearPref(this, "selectedIds");
        SharedPreferenceHelper.clearPref(this, "last_found");
        SharedPreferenceHelper.clearPref(this, "isLockedDir");
        SharedPreferenceHelper.clearPref(this, "busStop");
        SharedPreferenceHelper.clearPref(this, "busStops");
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminals);

        ButterKnife.inject(this);
        setTitle("");
        setSupportActionBar(toolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        toolBar.setNavigationIcon(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_action_add_circle,getTheme()));
//        toolBar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onBackPressed();
//            }
//        });


        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        Intent touchIntent = getIntent();
        String service_no = touchIntent.getStringExtra("service_id");
        String query = null;

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        }

        if (service_no == null) {
            service_no = SharedPreferenceHelper.getSharedStringPref(this,"service_id",null);
        }


        if (service_no != null) {
            query = service_no;
        }

        if (query!=null) {

            // Save it into the recent search
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MyRecentSuggestionProvider.AUTHORITY, MyRecentSuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);

            titleLabel.setText(query);

            CoolDatabase db = new CoolDatabase(this);
            busRoutes = db.getBusRoute(query);

            List<BusTerminal> terminals = new ArrayList<>();

            ArrayList<BusStops> route1StartStops = db.getBusStops(query, 1, false);
            ArrayList<BusStops> route1EndStops = db.getBusStops(query, 1, true);

            Set<BusStops> container = new LinkedHashSet<>(route1StartStops);
            container.addAll(route1EndStops);
            List<BusStops> route1Stops = new ArrayList<>(container);
            if (route1Stops.size()>0) {
                route1Stops.get(route1Stops.size() - 1).setSequence(route1Stops.get(route1Stops.size() - 1).getSequence() + 1);
                BusTerminal busTerminal = new BusTerminal();
                busTerminal.setServiceNo(query);
                busTerminal.setStartTerminal(route1Stops.get(0).getBusStopName());
                busTerminal.setEndTerminal(route1Stops.get(route1Stops.size() - 1).getBusStopName());
                terminals.add(busTerminal);
            }


            ArrayList<BusStops> route2StartStops = db.getBusStops(query, 2, false);
            ArrayList<BusStops> route2EndStops = db.getBusStops(query, 2, true);

            container = new LinkedHashSet<>(route2StartStops);
            container.addAll(route2EndStops);
            List<BusStops> route2Stops = new ArrayList<>(container);
            if (route2Stops.size()>0) {
                route2Stops.get(route2Stops.size() - 1).setSequence(route2Stops.get(route2Stops.size() - 1).getSequence() + 1);
                BusTerminal busTerminal = new BusTerminal();
                busTerminal.setServiceNo(query);
                busTerminal.setStartTerminal(route2Stops.get(0).getBusStopName());
                busTerminal.setEndTerminal(route2Stops.get(route2Stops.size() - 1).getBusStopName());
                terminals.add(busTerminal);
            }

            int visiblity = (busRoutes.size() > 0) ? View.GONE : View.VISIBLE;
            textView.setVisibility(visiblity);

            RecyclerView.Adapter mAdapter = new TerminalsRVAdapter((ArrayList) terminals);
            recyclerView.setAdapter(mAdapter);
        }
    }
}
