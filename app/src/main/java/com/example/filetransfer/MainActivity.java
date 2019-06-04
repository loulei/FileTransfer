package com.example.filetransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PeerListListener, ConnectionInfoListener{

    private WifiP2pManager manager;
    private Channel channel;

    private IntentFilter intentFilter = new IntentFilter();

    private BroadcastReceiver receiver;
    private boolean isWifiP2pEnabled = false;

    private ListView lv_devices;
    private Button btn_pickfile;

    private List<WifiP2pDevice> devices;
    private WifiPeerListAdapter adapter;

    private WifiP2pInfo info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        devices = new ArrayList<>();
        lv_devices = (ListView) findViewById(R.id.lv_devices);
        btn_pickfile = findViewById(R.id.btn_pickfile);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        adapter = new WifiPeerListAdapter(devices);
        lv_devices.setAdapter(adapter);
        lv_devices.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                WifiP2pDevice device = devices.get(position);
                System.out.println("selected device:"+device.toString());
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                manager.connect(channel, config, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void onFailure(int reason) {
                        // TODO Auto-generated method stub
                        Toast.makeText(getApplicationContext(), "connect fail", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        lv_devices.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                if(info.groupFormed && info.isGroupOwner){
                    System.out.println("isGroupOwner");
                    Intent intent = new Intent(MainActivity.this, FileTransferService.class);
                    intent.setAction(FileTransferService.ACTION_RECV_FILE);
                    intent.putExtra(FileTransferService.EXTRA_GROUP_OWNER_PORT, 9900);
                    startService(intent);
                }else{
                    System.out.println("not GroupOwner");
                    Intent intent = new Intent(MainActivity.this, FileTransferService.class);
                    intent.setAction(FileTransferService.ACTION_SEND_FILE);
                    intent.putExtra(FileTransferService.EXTRA_FILE_PATH, btn_pickfile.getText().toString());
                    intent.putExtra(FileTransferService.EXTRA_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
                    intent.putExtra(FileTransferService.EXTRA_GROUP_OWNER_PORT, 9900);
                    startService(intent);
                }
                return true;
            }
        });

        btn_pickfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FilePickerActivity.class);
                startActivityForResult(intent, FilePickerActivity.REQUEST_CODE_FILE_PICK);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == FilePickerActivity.REQUEST_CODE_FILE_PICK && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.KEY_FILE_PATH);
            System.out.println(filePath);

            btn_pickfile.setText(filePath);


        }
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        receiver = new WifiDirectReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
            case R.id.action_enable:
                if (manager != null && channel != null) {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
                return true;
            case R.id.action_discover:
                if(!isWifiP2pEnabled){
                    Toast.makeText(getApplicationContext(), "wifi direct off", Toast.LENGTH_SHORT).show();
                    return true;
                }
                manager.discoverPeers(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        // TODO Auto-generated method stub
                        Toast.makeText(getApplicationContext(), "discovery initiated", Toast.LENGTH_SHORT).show();
                        reset();
                    }

                    @Override
                    public void onFailure(int reason) {
                        // TODO Auto-generated method stub
                        Toast.makeText(getApplicationContext(), "discovery fail", Toast.LENGTH_SHORT).show();
                    }
                });
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void reset(){
        devices.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        // TODO Auto-generated method stub

        for(WifiP2pDevice device : peers.getDeviceList()){
            devices.add(device);
        }
        adapter.notifyDataSetChanged();

    }

    private class WifiPeerListAdapter extends BaseAdapter{

        private List<WifiP2pDevice> devices;

        public WifiPeerListAdapter(List<WifiP2pDevice> devices) {
            super();
            this.devices = devices;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return devices == null ? 0 : devices.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            View view = convertView;
            if(view == null){
                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_device, null);
            }
            WifiP2pDevice device = devices.get(position);
            if(device != null){
                TextView tv_name = (TextView) view.findViewById(R.id.tv_name);
                TextView tv_detail = (TextView) view.findViewById(R.id.tv_detail);
                tv_name.setText(device.deviceName);
                tv_detail.setText(getStatus(device.status));
            }
            return view;
        }


        /* public static final int CONNECTED   = 0;
            public static final int INVITED     = 1;
            public static final int FAILED      = 2;
            public static final int AVAILABLE   = 3;
            public static final int UNAVAILABLE = 4;*/
        private String getStatus(int status){
            switch (status) {
                case WifiP2pDevice.CONNECTED:
                    return "CONNECTED";
                case WifiP2pDevice.INVITED:
                    return "INVITED";
                case WifiP2pDevice.FAILED:
                    return "FAILED";
                case WifiP2pDevice.AVAILABLE:
                    return "AVAILABLE";
                case WifiP2pDevice.UNAVAILABLE:
                    return "UNAVAILABLE";
                default:
                    return "UNKNOWN";
            }
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        // TODO Auto-generated method stub
        this.info = info;

    }


}
