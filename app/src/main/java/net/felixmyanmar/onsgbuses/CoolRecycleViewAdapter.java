package net.felixmyanmar.onsgbuses;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class CoolRecycleViewAdapter extends RecyclerView.Adapter<CoolRecycleViewAdapter.ViewHolder> {

    ArrayList<BusTerminal> mDataset;

    // Provide a suitable constructor (depends on the kind of dataset)
    public CoolRecycleViewAdapter(ArrayList<BusTerminal> myDataset) {
        this.mDataset = myDataset;
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        String startBusName = mDataset.get(position).getStartTerminal();
        String stopBusName = mDataset.get(position).getEndTerminal();

        holder.txtBusBeginName.setText(startBusName);
        holder.txtBusEndName.setText(stopBusName);
//        holder.txtHeader.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                remove(name);
//            }
//        });

    }


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        @InjectView(R.id.topLine) TextView txtBusBeginName;
        @InjectView(R.id.bottomLine) TextView txtBusEndName;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }
    }



    // Create new views (invoked by the layout manager)
    @Override
    public CoolRecycleViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }




}
