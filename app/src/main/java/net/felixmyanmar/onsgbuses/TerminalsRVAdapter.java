package net.felixmyanmar.onsgbuses;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TerminalsRVAdapter extends RecyclerView.Adapter<TerminalsRVAdapter.ViewHolder> {

    ArrayList<BusTerminal> mDataset;

    // Provide a suitable constructor (depends on the kind of dataset)
    public TerminalsRVAdapter(ArrayList<BusTerminal> myDataset) {
        this.mDataset = myDataset;
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        String startBusName = mDataset.get(position).getStartTerminal();
        String stopBusName = mDataset.get(position).getEndTerminal();
        final String busService = mDataset.get(position).getServiceNo();

        holder.txtBusBeginName.setText(startBusName);
        holder.txtBusEndName.setText(stopBusName);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Context context = view.getContext();
                Intent intent = new Intent(context, OnTheRoadActivity.class);
                intent.putExtra("service_id", busService);
                intent.putExtra("direction",position+1);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((Activity)context).getWindow().setExitTransition(new Fade());
                    context.startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation((Activity)context).toBundle());
                } else {
                    context.startActivity(intent);
                }
            }
        });

    }


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        @InjectView(R.id.topLine) TextView txtBusBeginName;
        @InjectView(R.id.bottomLine) TextView txtBusEndName;
        @InjectView(R.id.cv) CardView cardView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }
    }



    // Create new views (invoked by the layout manager)
    @Override
    public TerminalsRVAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_terminals, parent, false);
        return new ViewHolder(v);
    }




}
