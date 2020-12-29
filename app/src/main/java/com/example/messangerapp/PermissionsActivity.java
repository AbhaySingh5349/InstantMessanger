package com.example.messangerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.messangerapp.util.Util;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PermissionsActivity extends AppCompatActivity {

    @BindView(R.id.checkPermissionProgressBar)
    ProgressBar checkPermissionProgressBar;
    @BindView(R.id.internetPermissionTextView)
    TextView internetPermissionTextView;
    @BindView(R.id.checkRetryBtn)
    Button checkRetryBtn;
    @BindView(R.id.checkCloseBtn)
    Button checkCloseBtn;

    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        ButterKnife.bind(this);

        if(Build.VERSION.SDK_INT >20){
            networkCallback = new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    finish();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    internetPermissionTextView.setText("No Internet Available");
                }
            };
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
            connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),networkCallback);
        }

        checkRetryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermissionProgressBar.setVisibility(View.VISIBLE);
                if(Util.checkInternetConnection(PermissionsActivity.this)){
                    finish();
                }else {
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkPermissionProgressBar.setVisibility(View.GONE);
                        }
                    },1000);
                }
            }
        });

        checkCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();
            }
        });
    }
}