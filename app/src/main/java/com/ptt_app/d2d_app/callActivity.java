package com.ptt_app.d2d_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.net.Socket;

public class callActivity extends AppCompatActivity {

    Button backBtn, talkBtn;
    Socket sckt;
    private MainConversation audioClient;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        backBtn = (Button) findViewById(R.id.backBtn);
        talkBtn = (Button) findViewById(R.id.talkBtn);
        audioClient = new MainConversation();

        sckt = SocketHandler.getSocket();
        audioClient.audioCreate();
        audioClient.setSocket(sckt);
        audioClient.setupStreams();
        audioClient.startPlaying();

        talkBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN ) {
                    audioClient.stopPlaying();
                    audioClient.startRecording();

                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ) {
                    audioClient.stopRecording();
                    audioClient.startPlaying();
                }
                return false;
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioClient.destroyProcesses();
                Intent mIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mIntent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioClient.destroyProcesses();
    }
}

