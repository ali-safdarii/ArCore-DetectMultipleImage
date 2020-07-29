package com.shliama.augmentedvideotutorial.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.shliama.augmentedvideotutorial.R;

public class TestActivity extends AppCompatActivity implements OnSwipeTouchListener.CallBack {

    private long CLICK_DURATION = 2000;

    boolean isLeft=false;
    boolean isRight=false;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Switch sw = findViewById(R.id.switch1);

        sw.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(TestActivity.this, "HOld", Toast.LENGTH_SHORT).show();
                if (isLeft){
                    Toast.makeText(TestActivity.this, "isLeft", Toast.LENGTH_SHORT).show();
                }else if (isRight)
                    Toast.makeText(TestActivity.this, "isRight", Toast.LENGTH_SHORT).show();
                return false;



            }
        });

    /*    sw.setOnTouchListener(new View.OnTouchListener() {
            private float y1;
            private float x1;
            private long t1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        y1 = event.getY();
                        t1 = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float x2 = event.getX();
                        float y2 = event.getY();
                        long t2 = System.currentTimeMillis();

                        if (x1 == x2 && y1 == y2 && (t2 - t1) < CLICK_DURATION) {
                            Toast.makeText(TestActivity.this, "Click", Toast.LENGTH_SHORT).show();
                        } else if ((t2 - t1) >= CLICK_DURATION) {
                            Toast.makeText(TestActivity.this, "Long click", Toast.LENGTH_SHORT).show();
                        } else if (x1 > x2) {
                            Toast.makeText(TestActivity.this, "Left swipe", Toast.LENGTH_SHORT).show();
                        } else if (x2 > x1) {
                            Toast.makeText(TestActivity.this, "Right swipe", Toast.LENGTH_SHORT).show();
                        }


                        return true;
                }

                return false;
            }

        });

        */
    }

    @Override
    public void isLeft(boolean flag) {
        isLeft=false;
    }

    @Override
    public void isRight(boolean flag) {

        isRight=flag;
    }
}