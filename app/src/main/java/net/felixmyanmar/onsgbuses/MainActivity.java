package net.felixmyanmar.onsgbuses;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity {

    private MyCoolDatabase myCoolDatabase;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private ArrayList<String> myDataset;

    @InjectView(R.id.cool_recycler_view)
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myCoolDatabase = new MyCoolDatabase(this);
        myDataset = myCoolDatabase.getAllBusServices();

        ButterKnife.inject(this);

        recyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new CoolRecycleViewAdapter(myDataset);
        recyclerView.setAdapter(mAdapter);
    }
}
