package com.example.messangerapp.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.messangerapp.MainActivity;
import com.example.messangerapp.PermissionsActivity;
import com.example.messangerapp.R;
import com.example.messangerapp.password.ForgotPasswordActivity;
import com.example.messangerapp.profile.ProfileActivity;
import com.example.messangerapp.signup.SignUpActivity;
import com.example.messangerapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {

    /* existing user can reenter email and password to start with app */

    @BindView(R.id.appLogoImage)
    ImageView appLogoImage;
    @BindView(R.id.loginCardView)
    CardView loginCardView;
    @BindView(R.id.loginCardViewLinearLayout)
    LinearLayout loginCardViewLinearLayout;
    @BindView(R.id.emailTextInputLayout)
    TextInputLayout emailTextInputLayout;
    @BindView(R.id.emailTextInputEditText)
    TextInputEditText emailTextInputEditText;
    @BindView(R.id.passwordTextInputLayout)
    TextInputLayout passwordTextInputLayout;
    @BindView(R.id.passwordTextInputEditText)
    TextInputEditText passwordTextInputEditText;
    @BindView(R.id.loginBtn)
    Button loginBtn;
    @BindView(R.id.forgotPasswordTextView)
    TextView forgotPasswordTextView;
    @BindView(R.id.createNewAccountTextView)
    TextView createNewAccountTextView;
    @BindView(R.id.signUpTextView)
    TextView signUpTextView;
    @BindView(R.id.progressBar)
    View progressBar;

    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(); // to create object of Firebase Auth class to fetch currently loged in user
    FirebaseUser firebaseUser; // to create object of Firebase User class to get current user to store currently loged in user

    String email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkLogInCredentials();
            }
        });
        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });
    }

    // validating required fields

    private boolean validateEmail() {
        email = emailTextInputEditText.getText().toString().trim();
        if (email.isEmpty()) {
            emailTextInputLayout.setError("Enter Email Address");
            return false;
        } else {
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailTextInputLayout.setError(null);
                emailTextInputLayout.setErrorEnabled(false);
                return true;
            } else {
                emailTextInputLayout.setError("Invalid Email Format");
                return false;
            }
        }
    }

    private boolean validatePassword() {
        password = passwordTextInputEditText.getText().toString().trim();
        if (password.isEmpty()) {
            passwordTextInputLayout.setError("Enter Password");
            return false;
        } else if (password.length() < 6) {
            passwordTextInputLayout.setError("Weak Password");
            return false;
        } else {
            passwordTextInputLayout.setError(null);
            passwordTextInputLayout.setErrorEnabled(false);
            return true;
        }
    }

    public void checkLogInCredentials() {
        if (!validateEmail() | !validatePassword()) {
            validateEmail();
            validatePassword();
        } else {
            if (Util.checkInternetConnection(LoginActivity.this)) {
                progressBar.setVisibility(View.VISIBLE);

                // logging in user using Email and Password

                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) // user is authenticated
                        { startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                          finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                startActivity(new Intent(LoginActivity.this, PermissionsActivity.class));
            }
        }
    }

    @Override
    protected void onStart()  // automatic login functionality
    {
        super.onStart();

        firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser!=null){
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
}