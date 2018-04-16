package michaelreichle.tenfingertyping.DeviceListView;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import michaelreichle.tenfingertyping.R;

/**
 * By michaelreichle on 13.04.2018 at 17:55.
 */

public class DeviceAdapter extends BaseAdapter {

    public static long DEFAULT_DEVICE_ID;
    public static int NONE = -1;

    private List<DeviceHolder> devices;
    private Context context;
    private int selectedPosition = NONE;

    public DeviceAdapter(Context context, List<DeviceHolder> devices) {
        this.context = context;
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int i) {
        return devices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return DEFAULT_DEVICE_ID;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        // inflate the layout for each list row
        if (view == null) {
            view = LayoutInflater.from(context).
                    inflate(R.layout.device_list_view_item, parent, false);
        }

        // get current item to be displayed
        DeviceHolder currentItem = (DeviceHolder) getItem(i);

        int color = (i == selectedPosition) ? R.color.backgroundItemSelected : R.color.backgroundItemNotSelected;

        view.findViewById(R.id.background).setBackgroundColor(ContextCompat.getColor(context, color));

        // get the TextView for item name and item description
        TextView name = (TextView)
                view.findViewById(R.id.device_name);
        TextView address = (TextView)
                view.findViewById(R.id.device_adress);

        //sets the text for item name and item description from the current item object
        name.setText(currentItem.getName());
        address.setText(currentItem.getMac());

        // returns the view for the current row
        return view;
    }

    public void add(DeviceHolder device) {
        devices.add(device);
        notifyDataSetChanged();
    }

    public boolean contains(DeviceHolder device) {
        return devices.contains(device);
    }

    public void setSelectedPosition(int position) {
        if (position >= 0 && position < getCount()) {
            this.selectedPosition = position;
            notifyDataSetChanged();
        }
    }

    public int getSelectedPosition() {
        return this.selectedPosition;
    }

    public void clear() {
        devices.clear();
        this.notifyDataSetChanged();
    }
}
