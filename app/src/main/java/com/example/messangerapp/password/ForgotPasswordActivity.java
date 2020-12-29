package com.example.messangerapp.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.messangerapp.R;
import com.example.messangerapp.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ForgotPasswordActivity extends AppCompatActivity {

    @BindView(R.id.emailTextInputLayout)
    TextInputLayout emailTextInputLayout;
    @BindView(R.id.emailTextInputEditText)
    TextInputEditText emailTextInputEditText;
    @BindView(R.id.resetPasswordBtn)
    Button resetPasswordBtn;
    @BindView(R.id.resetPasswordCardViewLinearLayout)
    LinearLayout resetPasswordCardViewLinearLayout;
    @BindView(R.id.resetPasswordMessageLinearLayout)
    LinearLayout resetPasswordMessageLinearLayout;
    @BindView(R.id.resetPasswordMessage)
    TextView resetPasswordMessage;
    @BindView(R.id.retryResetPasswordBtn)
    Button retryResetPasswordBtn;
    @BindView(R.id.closeResetPasswordBtn)
    Button closeResetPasswordBtn;
    @BindView(R.id.progressBar)
    View progressBar;

    FirebaseAuth firebaseAuth; // to create object of Firebase Auth class to fetch currently loged in user

    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        ButterKnife.bind(this);

        closeResetPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            }
        });
        resetPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateEmail();
            }
        });
    }

    private boolean validateEmail(){
        email = emailTextInputEditText.getText().toString().trim();
        if(email.isEmpty()){
            emailTextInputLayout.setError("Enter Email Address");
            return false;
        }else {
            if(Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                emailTextInputLayout.setError(null);
                emailTextInputLayout.setErrorEnabled(false);
                firebaseAuth = FirebaseAuth.getInstance();
                progressBar.setVisibility(View.VISIBLE);

                // sending password verification to email provided

                firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        resetPasswordCardViewLinearLayout.setVisibility(View.GONE);
                        resetPasswordMessageLinearLayout.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        if(task.isSuccessful()){
                            resetPasswordMessage.setText("Reset Password Instructions Have been sent on" + email);
                            new CountDownTimer(60000, 1000) {
                                @Override
                                public void onTick(long timeLeft) {
                                    retryResetPasswordBtn.setText("Retry: " + timeLeft/1000);
                                    retryResetPasswordBtn.setEnabled(false);
                                }
                                @Override
                                public void onFinish() {
                                    retryResetPasswordBtn.setText("Retry");
                                    retryResetPasswordBtn.setEnabled(true);
                                    retryResetPasswordBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            resetPasswordCardViewLinearLayout.setVisibility(View.VISIBLE);
                                            resetPasswordMessageLinearLayout.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                }
                            }.start();
                        }else {
                            resetPasswordMessage.setText("Failed to send Email: " + task.getException());
                            retryResetPasswordBtn.setText("Retry");
                            retryResetPasswordBtn.setEnabled(true);
                            retryResetPasswordBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    resetPasswordCardViewLinearLayout.setVisibility(View.VISIBLE);
                                    resetPasswordMessageLinearLayout.setVisibility(View.INVISIBLE);
                                }
                            });
                        }
                    }
                });
                return true;
            }else {
                emailTextInputLayout.setError("Invalid Email Format");
                return false;
            }
        }
    }
}