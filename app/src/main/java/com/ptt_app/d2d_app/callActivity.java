package com.ptt_app.d2d_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.net.Socket;

public class callActivity extends AppCompatActivity {

    ImageView backBtn;
    ToggleButton talkBtn;
    TextView callertxt;
    Socket sckt;
    private MainConversation audioClient;
    String caller;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                caller= null;
            } else {
                caller= extras.getString("CALLER_NAME");
            }
        } else {
            caller= (String) savedInstanceState.getSerializable("CALLER_NAME");
        }

        backBtn = (ImageView) findViewById(R.id.backBtn);
        talkBtn = (ToggleButton) findViewById(R.id.talkBtn);
        callertxt = (TextView)findViewById(R.id.caller);
        callertxt.setText(caller);

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

