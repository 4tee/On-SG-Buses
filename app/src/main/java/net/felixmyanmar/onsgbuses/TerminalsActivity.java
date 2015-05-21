package net.felixmyanmar.onsgbuses;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminals);
        ButterKnife.inject(this);



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

        if (service_no != null) {
            query = service_no;
        }

        if (query!=null) {

            setTitle(query);

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
