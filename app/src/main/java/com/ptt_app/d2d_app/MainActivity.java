package com.ptt_app.d2d_app;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "MainActivity";
    private static final int SEPRATION_DIST_THRESHOLD = 50;
    Button btnOnOff;
    Button btnDiscover;
    Button btnChat;
    Button btnCall;
    Button ripple;
    ListView listvw;
    TextView readmsg, status;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    private static int device_count = 0;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ArrayList<Point> device_points = new ArrayList<>();
    ArrayList<CustomDevice> custom_peers = new ArrayList<>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ = 1;
    ServerClass serverClass;
    ClientClass clientClass;

    RippleBackground rippleBackground;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here

        }
        initialWork();
        wifi_listener();
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.startRippleAnimation();
            }
        });

        ripple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent rippleIntent = new Intent(getApplicationContext(), DiscoverActivity.class);
                //startActivity(rippleIntent);
                rippleBackground.stopRippleAnimation();
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    readmsg.setText(tempMsg);
                    break;
            }
            return true;
        }
    });


    @SuppressLint("ClickableViewAccessibility")
    private void wifi_listener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("ON");
                }
                else{
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverPeersTillSuccess();
            }
        });

        listvw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                final WifiP2pDevice device =  deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Connected to "+device.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(), "Not Connected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatintent = new Intent(getApplicationContext(), chatActivity.class);
                startActivity(chatintent);

            }
        });

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(getApplicationContext(), callActivity.class);
                startActivity(callIntent);
            }
        });


    }


    private void discoverPeersTillSuccess() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                    status.setText("Discovery Started");

            }

            @Override
            public void onFailure(int reasonCode) {
                discoverPeersTillSuccess();
            }
        });
    };


    private void initialWork(){
        btnOnOff = (Button) findViewById(R.id.onOff);
        btnDiscover = (Button) findViewById(R.id.discover);
        listvw = (ListView) findViewById(R.id.peerListView);
        status = (TextView) findViewById(R.id.connectionStatus);
        btnChat = (Button) findViewById(R.id.chat);
        btnCall = (Button) findViewById(R.id.call);
        ripple = (Button) findViewById(R.id.ripple);
        rippleBackground=(RippleBackground)findViewById(R.id.content);
        imageView=(ImageView)findViewById(R.id.centerImage);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiP2PBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers)){

                ArrayList<CustomDevice> device_already_present = new ArrayList<>();
                for(WifiP2pDevice device : peerList.getDeviceList()){
                    int idx = checkPeersListByName(device.deviceName);
                    if(idx != -1){
                        // device already in list
                        device_already_present.add(custom_peers.get(idx));
                    }
                }

                if(!custom_peers.isEmpty()){
                    for(CustomDevice d : custom_peers){
                        rippleBackground.removeView(findViewById(d.id));
                    }
                }
                custom_peers.clear();
                custom_peers.addAll(device_already_present);

                for(CustomDevice d : device_already_present){
                    rippleBackground.addView(d.icon_view);
                }

                for(WifiP2pDevice device : peerList.getDeviceList()) {
                    if (checkPeersListByName(device.deviceName) == -1) {
                        // device not already present
                        View tmp_device = createNewDevice(device.deviceName);
                        rippleBackground.addView(tmp_device);
                        foundDevice(tmp_device);

                        CustomDevice tmp_device_obj = new CustomDevice();
                        tmp_device_obj.deviceName = device.deviceName;
                        tmp_device_obj.id = tmp_device.getId();
                        tmp_device_obj.device = device;
                        tmp_device_obj.icon_view = tmp_device;

                        custom_peers.add(tmp_device_obj);
                    }
                }

                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                for (WifiP2pDevice device: peerList.getDeviceList()){
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listvw.setAdapter(adapter);
            }

            if(peers.size()==0){
                Toast.makeText(getApplicationContext(), "No Devices Found", Toast.LENGTH_SHORT);
            }

        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwneraddress = info.groupOwnerAddress;
            if(info.groupFormed && info.isGroupOwner){
                status.setText("HOST");
                serverClass = new ServerClass();
                serverClass.start();
            }else if(info.groupFormed){
                status.setText("CLIENT");
                clientClass = new ClientClass(groupOwneraddress);
                clientClass.start();
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onClick(View v) {
        int view_id = v.getId();
        if(getIndexFromIdPeerList(view_id) != -1){
            int idx = getIndexFromIdPeerList(view_id);
            final WifiP2pDevice device = custom_peers.get(idx).device;
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Connected to "+device.deviceName, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(getApplicationContext(), "Error in connecting to "+device.deviceName, Toast.LENGTH_SHORT).show();
                }
            });
        }/*else{
            switch (v.getId()){
                case R.id.centerImage:
                    rippleBackground.startRippleAnimation();
                    break;
                default:
                    break;
            }
        }*/
    }

    private int getIndexFromIdPeerList(int id){
        for(CustomDevice d : custom_peers){
            if(d.id == id){
                return custom_peers.indexOf(d);
            }
        }
        return -1;
    }


    public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                SocketHandler.setSocket(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostAddress;
        public ClientClass(InetAddress address){
            this.hostAddress = address.getHostAddress();
            this.socket = new Socket();
        }
        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAddress, 8888), 500);
                SocketHandler.setSocket(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public View createNewDevice(String device_name){
        View device1 = LayoutInflater.from(this).inflate(R.layout.device_icon, null);
        Point new_point = generateRandomPosition();
        RippleBackground.LayoutParams params = new RippleBackground.LayoutParams(350,350);
        params.setMargins(new_point.x, new_point.y, 0, 0);
        device1.setLayoutParams(params);

        TextView txt_device1 = device1.findViewById(R.id.myImageViewText);
        int device_id = (int)System.currentTimeMillis() + device_count++;
        txt_device1.setText(device_name);
        device1.setId(device_id);
        device1.setOnClickListener(this);
        device1.setVisibility(View.INVISIBLE);
        return device1;
    }

    Point generateRandomPosition(){
        Display display = getWindowManager(). getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int SCREEN_WIDTH = size.x;
        int SCREEN_HEIGHT = size.y;

        int height_start = SCREEN_HEIGHT / 2 - 300;
        int x = 0;
        int y = 0;

        do{
            x = (int)(Math.random() * SCREEN_WIDTH);
            y = (int)(Math.random() * height_start);
        }while(checkPositionOverlap(new Point(x, y)));

        Point new_point = new Point(x, y);
        device_points.add(new_point);

        return new_point;

    }

    boolean checkPositionOverlap(Point new_p){
        //  if overlap, then return true, else return false
        if(!device_points.isEmpty()){
            for(Point p:device_points){
                int distance = (int)Math.sqrt(Math.pow(new_p.x - p.x, 2) + Math.pow(new_p.y - p.y, 2));
                Log.d(TAG, distance+"");
                if(distance < SEPRATION_DIST_THRESHOLD){
                    return true;
                }
            }
        }
        return false;
    }

    private void foundDevice(View foundDevice){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(400);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        ArrayList<Animator> animatorList=new ArrayList<Animator>();
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleX", 0f, 1.2f, 1f);
        animatorList.add(scaleXAnimator);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleY", 0f, 1.2f, 1f);
        animatorList.add(scaleYAnimator);
        animatorSet.playTogether(animatorList);
        foundDevice.setVisibility(View.VISIBLE);
        animatorSet.start();
    }

    private int checkPeersListByName(String deviceName){
        for(CustomDevice d :custom_peers) {
            if (d.deviceName.equals(deviceName)) {
                return custom_peers.indexOf(d);
            }
        }
        return -1;
    }

}

class CustomDevice{
    int id;
    String deviceName;
    WifiP2pDevice device;
    View icon_view;
    CustomDevice(){
    }
}