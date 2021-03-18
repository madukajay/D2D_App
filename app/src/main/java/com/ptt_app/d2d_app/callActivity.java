package com.ptt_app.d2d_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class callActivity extends AppCompatActivity {

    ImageView backBtn;
    ToggleButton talkBtn;
    TextView callertxt;
    Socket sckt;
    private MainConversation audioClient;
    //private ArrayList<MainConversation> audioClients;
    private ArrayList<InputStream> inStreams;
    private ArrayList<OutputStream> outStreams;
    private ArrayList<receiveCall> incomingCalls;
    private Thread recordingThread = null;
    private Thread playThread = null;
    private AudioRecord recorder = null;
    private volatile AudioTrack track = null;
    private AudioManager am = null;
    private byte buffer[] = null;
    private byte playBuffer[] = null;
    int minSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private int bufferSize = minSize;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    String caller;
    ArrayList<Socket> sockets;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                caller = null;
            } else {
                caller = extras.getString("CALLER_NAME");
            }
        } else {
            caller = (String) savedInstanceState.getSerializable("CALLER_NAME");
        }

        backBtn = (ImageView) findViewById(R.id.backBtn);
        talkBtn = (ToggleButton) findViewById(R.id.talkBtn);
        callertxt = (TextView)findViewById(R.id.caller);
        callertxt.setText(caller);

        //audioClient = new MainConversation();
        //audioClients = new ArrayList<MainConversation>();
        inStreams = new ArrayList<>();
        outStreams = new ArrayList<>();
        incomingCalls = new ArrayList<>();
        sockets = SocketHandler.getSockets();
        setupStreams();

        track = new AudioTrack(AudioManager.STREAM_MUSIC,
                16000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);

        for (int i = 0; i< sockets.size();i++){
            incomingCalls.add(new callActivity.receiveCall(inStreams.get(i),i));
            incomingCalls.get(i).start();
        }

        /*
        sckt = SocketHandler.getSocket();
        audioClient.audioCreate();
        audioClient.setSocket(sckt);
        audioClient.setupStreams();
        audioClient.startPlaying();
        */
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        talkBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN ) {
                    //for (int i = 0; i< sockets.size();i++){
                    //    incomingCalls.get(i).stopRunning();
                    //}
                    isRecording = true;
                    if (track != null) {
                        track.stop();
                    }
                    startRecording();

                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ) {
                    stopRecording();
                    //for (int i = 0; i< sockets.size();i++){
                    //    incomingCalls.get(i).startRunning();
                    //}
                    track.play();
                }
                return false;
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //audioClient.destroyProcesses();
                for (int i = 0; i< sockets.size();i++){
                    incomingCalls.get(i).stopRunning();
                }
                Intent mIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mIntent);
            }
        });


    }

    public void startRecording() {
        Log.d("AUDIO", "Assigning recorder");
        buffer = new byte[bufferSize];

        // Start Recording
        recorder.startRecording();
        isRecording = true;
        // Start a thread
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendRecording();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }
    // Method for sending Audio
    public void sendRecording() {
        // Infinite loop until microphone button is released
        while (isRecording) {
            try {
                recorder.read(buffer, 0, bufferSize);
                for (int i = 0; i< outStreams.size();i++){
                    outStreams.get(i).write(buffer);
                }
            } catch (IOException e) {
                Log.d("AUDIO", "Error when sending recording");
            }
        }
    }

    public void setupStreams() {
        try {
            for (int i = 0; i< sockets.size();i++){
                inStreams.add(sockets.get(i).getInputStream());
            }
        } catch (IOException e) {
            Log.e("SOCKET", "Error when creating input stream", e);
        }
        try {
            for (int i = 0; i< sockets.size();i++){
                outStreams.add(sockets.get(i).getOutputStream());
            }
        } catch (IOException e) {
            Log.e("SOCKET", "Error when creating output stream", e);
        }
    }

    public void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            Log.e("AUDIO", "Recorder is still recording");
            recorder.stop();
        }
    }
    /*
    // Playback received audio
    public void startPlaying(final int x) {
        Log.d("AUDIO", "Assigning player");
        // Receive Buffer
        playBuffer = new byte[minSize];

        track.play();
        // Receive and play audio
        playThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveRecording(x);
            }
        }, "AudioTrack Thread");
        playThread.start();
    }

    // Receive audio and write into audio track object for playback
    public void receiveRecording(int x) {
        while (!isRecording) {
            try {
                if (inStreams.get(x).available() == 0) {
                    //Do nothing
                } else {
                    inStreams.get(x).read(playBuffer);
                    track.write(playBuffer, 0, playBuffer.length);
                }
            } catch (IOException e) {
                Log.d("AUDIO", "Error when receiving recording");
            }
        }
    }
    */

    private class receiveCall extends Thread {
        private volatile boolean flag = true;
        private InputStream inputStream;
        private int socketnum;

        public receiveCall(InputStream inputStream, int socketnum) {
            this.inputStream = inputStream;
            this.socketnum = socketnum;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[minSize];

            track.play();
            while (flag) {
                while (!isRecording) {
                    try {
                        if (inputStream.available() == 0) {
                            //Do nothing
                        } else {
                            inputStream.read(buffer);
                            track.write(buffer, 0, buffer.length);
                            if(sockets.size()>1){
                                for (int i = 0; i< outStreams.size();i++){
                                    if(i==socketnum){
                                        continue;
                                    }
                                    outStreams.get(i).write(buffer);
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.d("AUDIO", "Error when receiving recording");
                    }
                }
            }
        }
        public void stopRunning()
        {
            flag = false;
            //playtrack.stop();
        }

        public void startRunning()
        {
            flag = true;
            //playtrack.play();
        }
    }

    public void destroyProcesses() {
        //Release resources for audio objects
        //track.release();
        recorder.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //audioClient.destroyProcesses();
        recorder.release();
        for (int i = 0; i< incomingCalls.size();i++){
            incomingCalls.get(i).stopRunning();
        }
    }
}

