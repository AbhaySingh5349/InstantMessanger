package com.example.messangerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.messangerapp.login.LoginActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SplashScreenActivity extends AppCompatActivity {

    @BindView(R.id.appLogoImage)
    ImageView appLogoImage;
    @BindView(R.id.appNameTextView)
    TextView appNameTextView;

    private Animation splashScreenAnimations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        ButterKnife.bind(this);

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }

        splashScreenAnimations = AnimationUtils.loadAnimation(this,R.anim.splash_screen_animation);
        splashScreenAnimations.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        appLogoImage.startAnimation(splashScreenAnimations);
        appNameTextView.setAnimation(splashScreenAnimations);
    }
}