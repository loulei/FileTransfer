package com.example.filetransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

public class WifiDirectReceiver extends BroadcastReceiver {
	
	private WifiP2pManager manager;
	private Channel channel;
	private MainActivity activity;
	
	

	public WifiDirectReceiver(WifiP2pManager manager, Channel channel, MainActivity activity) {
		super();
		this.manager = manager;
		this.channel = channel;
		this.activity = activity;
	}



	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// TODO Auto-generated method stub
		String action = arg1.getAction();
		if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
			int state = arg1.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
				activity.setIsWifiP2pEnabled(true);
			}else{
				activity.setIsWifiP2pEnabled(false);
			}
		} else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
			if(manager != null){
				activity.reset();
				manager.requestPeers(channel, activity);
			}
		} else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
			if(manager == null) 
				return;
			NetworkInfo networkInfo = arg1.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
			if(networkInfo.isConnected()){
				manager.requestConnectionInfo(channel, activity);
			}else{
				activity.reset();
			}
		} else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
			WifiP2pDevice wifiP2pDevice = arg1.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
		}
	}

}
