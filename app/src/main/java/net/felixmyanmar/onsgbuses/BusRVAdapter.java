package net.felixmyanmar.onsgbuses;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class BusRVAdapter extends RecyclerView.Adapter<BusRVAdapter.ViewHolder> implements Filterable {

    ArrayList<BusStops> mDataset;
    List<BusStops> orig;
    Context mContext;


    private static void setIntegerArrayPref(Context context, String key, ArrayList<Integer> values) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray a = new JSONArray();
        for (int i = 0; i < values.size(); i++) {
            a.put(values.get(i));
        }
        if (!values.isEmpty()) {
            editor.putString(key, a.toString());
        } else {
            editor.putString(key, null);
        }
        editor.apply();
    }

    public static ArrayList<Integer> getIntegerArrayPref(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String json = prefs.getString(key, null);
        ArrayList<Integer> urls = new ArrayList<>();
        if (json != null) {
            try {
                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++) {
                    Integer url = a.getInt(i);
                    urls.add(url);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return urls;
    }


    private void clearPref(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
    }


    public BusRVAdapter(Context context, ArrayList<BusStops> myDataset) {
        this.mContext = context;
        this.mDataset = myDataset;

        clearPref(context, "selectedIds");
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     * <p/>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     * <p/>
     * The new ViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(ViewHolder, int)}. Since it will be re-used to display different
     * items in the data set, it is a good idea to cache references to sub views of the View to
     * avoid unnecessary {@link View#findViewById(int)} calls.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(ViewHolder, int)
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_busstops, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * should update the contents of the {@link ViewHolder#itemView} to reflect the item at
     * the given position.
     * <p/>
     * Note that unlike {@link ListView}, RecyclerView will not call this
     * method again if the position of the item changes in the data set unless the item itself
     * is invalidated or the new position cannot be determined. For this reason, you should only
     * use the <code>position</code> parameter while acquiring the related data item inside this
     * method and should not keep a copy of it. If you need the position of an item later on
     * (e.g. in a click listener), use {@link ViewHolder#getAdapterPosition()} which will have
     * the updated adapter position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    ArrayList<Integer> selectedIds;
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        String BusName = mDataset.get(position).getBusStopName();
        final int BusStop = mDataset.get(position).getBusStopNo();

        holder.txtViewName.setText(BusName);
        holder.txtViewStop.setText(BusStop + "");

        // Sett all alarms to false first.. then get those in selectIds from SharePreference,
        // and enable it.
        selectedIds = new ArrayList<>();
        selectedIds = getIntegerArrayPref(mContext,"selectedIds");
        if (selectedIds != null) {
            for (int i=0; i<selectedIds.size(); i++) {
                holder.toggleButton.setChecked(false);
            }

            for (int i=0; i<selectedIds.size(); i++) {
                if (selectedIds.get(i) == BusStop) holder.toggleButton.setChecked(true);
            }
        }

//        holder.toggleButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (holder.toggleButton.isChecked()) selectedIds.add(BusStop);
//                else selectedIds.remove(Integer.valueOf(BusStop));
//                setIntegerArrayPref(mContext, "selectedIds", selectedIds);
//            }
//        });
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.toggleButton.toggle();
                if (holder.toggleButton.isChecked()) selectedIds.add(BusStop);
                else selectedIds.remove(Integer.valueOf(BusStop));
                setIntegerArrayPref(mContext, "selectedIds", selectedIds);
            }
        });
    }

    /**
     * Returns the total number of items in the data set hold by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public Filter getFilter() {
        return new busNoFilter();
    }

    class busNoFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults onReturn = new FilterResults();
            List<BusStops> results = new ArrayList<>();
            if (orig == null) orig = mDataset;
            if (charSequence != null) {
                if (orig != null && orig.size() > 0) {
                    for (BusStops g : orig) {
                        String data = g.getBusStopNo() + " " + g.getBusStopName().toLowerCase();
                        if (data.contains(charSequence.toString().toLowerCase()))
                            results.add(g);
                    }
                }
                onReturn.values = results;
            }
            return onReturn;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mDataset = (ArrayList<BusStops>) filterResults.values;
            notifyDataSetChanged();
        }
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        @InjectView(R.id.imageView) ImageView imageView;
        @InjectView(R.id.textViewName) TextView txtViewName;
        @InjectView(R.id.textViewStop) TextView txtViewStop;
        @InjectView(R.id.toggleButton) ToggleButton toggleButton;
        @InjectView(R.id.cv) CardView cardView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }
    }


}
