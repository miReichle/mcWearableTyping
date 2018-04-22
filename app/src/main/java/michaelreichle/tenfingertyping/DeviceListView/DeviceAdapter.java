package michaelreichle.tenfingertyping.DeviceListView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import michaelreichle.tenfingertyping.R;

/**
 * By michaelreichle on 13.04.2018 at 17:55.
 */

public class DeviceAdapter extends BaseAdapter {

    public static long DEFAULT_DEVICE_ID;

    private List<DeviceHolder> devices;
    private Context context;
    private OnDeviceConnectClicked listener;

    public DeviceAdapter(Context context, List<DeviceHolder> devices, OnDeviceConnectClicked listener) {
        this.context = context;
        this.devices = devices;
        this.listener = listener;
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
        if (view == null) {
            view = LayoutInflater.from(context).
                    inflate(R.layout.device_list_view_item, parent, false);
        }
        DeviceHolder currentItem = (DeviceHolder) getItem(i);

        TextView nameView = (TextView) view.findViewById(R.id.device_name);
        TextView addressView = (TextView) view.findViewById(R.id.device_address);
        ImageButton connectButton = (ImageButton) view.findViewById(R.id.connectButton);

        String name = currentItem.getName();
        if (name.length() >= 20) {
            name = name.substring(0, 20) + "…";
        }
        nameView.setText(name);
        addressView.setText(currentItem.getMac());

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.connect(currentItem);
            }
        });
        boolean enable = currentItem.isSupported();
        connectButton.setEnabled(enable);
        connectButton.setClickable(enable);
        int backgroundId = (enable) ? R.color.enabled_background : R.color.disabled_background;
        connectButton.setBackgroundTintList(context.getResources().getColorStateList(backgroundId));

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

    public void clear() {
        devices.clear();
        this.notifyDataSetChanged();
    }
}
