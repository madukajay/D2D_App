package com.ptt_app.d2d_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class chatActivity extends AppCompatActivity {

    static final int MESSAGE_READ = 1;
    static TextView readmsg;
    ImageView btnBack, btnSend;
    EditText wtMsg;
    //SendReceiveMessage sendReceiveMessage;
    ArrayList <SendReceiveMessage> sendReceiveMessage;
    TextView chatcallertxt;
    String temp;
    ArrayList<Socket> sockets;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                temp= null;
            } else {
                temp= extras.getString("CHAT_NAME");
            }
        } else {
            temp= (String) savedInstanceState.getSerializable("CHAT_NAME");
        }

        readmsg = (TextView) findViewById(R.id.readchatMsg);
        btnSend = (ImageView) findViewById(R.id.sendchatButton);
        btnBack = (ImageView) findViewById(R.id.chatbackBtn);
        wtMsg = (EditText) findViewById(R.id.writechatMsg);
        chatcallertxt = (TextView) findViewById(R.id.chatcaller);
        chatcallertxt.setText(temp);

        //Socket socket = SocketHandler.getSocket();
        sockets = SocketHandler.getSockets();
        final OutputStream[] outputStream = new OutputStream[5];
        sendReceiveMessage = new ArrayList<SendReceiveMessage>();

        for (int i = 0; i< sockets.size();i++){
            Log.d("CONNECTWIFI", "Socket " + i + sockets.get(i).getInetAddress().toString() + ".");
        }

        for (int i = 0; i< sockets.size();i++){
                sendReceiveMessage.add(new chatActivity.SendReceiveMessage(sockets.get(i)));
                sendReceiveMessage.get(i).start();
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = wtMsg.getText().toString();
                //sendReceiveMessage.write(msg.getBytes());
                for (int i = 0; i< sockets.size();i++){
                    try {
                        outputStream[i] = sockets.get(i).getOutputStream();
                        outputStream[i].write(msg.getBytes());
                        Log.d("CONNECTWIFI", "Sent msg");
                        outputStream[i].flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("CONNECTWIFI", "in sendData() input and output stream creation failed:" + e.getMessage() + ".");
                    }
                }

            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendReceiveMessage.interrupt();
                Intent mIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mIntent);
            }
        });

    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            final OutputStream[] outputStream = new OutputStream[5];
            switch (msg.what){
                case MESSAGE_READ:
                    //Log.d("CONNECTWIFI", "Handler Triggers ");
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    Log.d("CONNECTWIFI", tempMsg);
                    readmsg.setText(tempMsg);
                    if (SocketHandler.getclientSize() > 1){
                        for (int i = 0; i< sockets.size();i++){
                            try {
                                outputStream[i] = sockets.get(i).getOutputStream();
                                outputStream[i].write(tempMsg.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    });

    private class SendReceiveMessage extends Thread{
        private volatile boolean flag = true;
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceiveMessage(Socket sckt){
            socket = sckt;
            try {
                inputStream = socket.getInputStream();
                //outputStream = socket.getOutputStream();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (flag){
                while (socket != null) {
                    try {
                        bytes = socket.getInputStream().read(buffer);
                        if (bytes > 0) {
                            handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                            Log.d("CONNECTWIFI", "run: receivers count "+sendReceiveMessage.size());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        public void stopRunning()
        {
            flag = false;
        }

        public void startRunning()
        {
            flag = true;
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("CONNECTWIFI", "in sendData() input and output stream creation failed:" + e.getMessage() + ".");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (int i = 0; i< sockets.size();i++){
            sendReceiveMessage.get(i).stopRunning();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (int i = 0; i< sockets.size();i++){
            sendReceiveMessage.get(i).startRunning();
        }
    }
}
