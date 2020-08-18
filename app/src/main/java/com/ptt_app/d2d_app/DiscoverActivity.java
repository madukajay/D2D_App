package com.ptt_app.d2d_app;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class DiscoverActivity extends AppCompatActivity {

    private static final String TAG = "DiscoverActivity";
    private static final int SEPRATION_DIST_THRESHOLD = 50;
    ArrayList<Point> device_points = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        ImageView imageView=(ImageView)findViewById(R.id.centerImage);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.startRippleAnimation();
            }
        });

        Button backBtn = (Button) findViewById(R.id.backDis);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rippleBackground.stopRippleAnimation();
            }
        });
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


    /*public View createNewDevice(String device_name){
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
    }*/

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
}
