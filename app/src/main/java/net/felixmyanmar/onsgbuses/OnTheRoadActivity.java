package net.felixmyanmar.onsgbuses;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class OnTheRoadActivity extends AppCompatActivity {

    ArrayList<BusStops> busStops;

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

        RecyclerView.Adapter mAdapter = new BusRVAdapter(busStops);
        recyclerView.setAdapter(mAdapter);
    }
}
