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
    TextView readmsg;
    ImageView btnBack, btnSend;
    EditText wtMsg;
    SendReceiveMessage sendReceiveMessage;
    TextView chatcallertxt;
    String temp;


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

        Socket socket = SocketHandler.getSocket();
        final ArrayList<Socket> sockets = SocketHandler.getSockets();
        final OutputStream[] outputStream = new OutputStream[5];
        sendReceiveMessage = new chatActivity.SendReceiveMessage(socket);
        sendReceiveMessage.start();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = wtMsg.getText().toString();
                //sendReceiveMessage.write(msg.getBytes());
                for (int i = 0; i< sockets.size();i++){
                    try {
                        outputStream[i] = sockets.get(i).getOutputStream();
                        outputStream[i].write(msg.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReceiveMessage.interrupt();
                Intent mIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mIntent);
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

    private class SendReceiveMessage extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceiveMessage(Socket sckt){
            socket = sckt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket!=null){
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes>0){
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("Fatal Error", "in sendData() input and output stream creation failed:" + e.getMessage() + ".");
            }
        }
    }
}
