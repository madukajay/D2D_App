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
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class chatActivity extends AppCompatActivity {

    static final int MESSAGE_READ = 1;
    TextView readmsg;
    Button btnBack, btnSend;
    EditText wtMsg;
    SendReceiveMessage sendReceiveMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        readmsg = (TextView) findViewById(R.id.readchatMsg);
        btnSend = (Button) findViewById(R.id.sendchatButton);
        btnBack = (Button) findViewById(R.id.back);
        wtMsg = (EditText) findViewById(R.id.writechatMsg);

        Socket socket = SocketHandler.getSocket();
        sendReceiveMessage = new chatActivity.SendReceiveMessage(socket);
        sendReceiveMessage.start();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = wtMsg.getText().toString();
                sendReceiveMessage.write(msg.getBytes());
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
