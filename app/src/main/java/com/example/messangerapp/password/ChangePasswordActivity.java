package com.example.messangerapp.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.messangerapp.R;
import com.example.messangerapp.profile.ProfileActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChangePasswordActivity extends AppCompatActivity {

    @BindView(R.id.appLogoImage)
    ImageView appLogoImage;
    @BindView(R.id.loginCardView)
    CardView loginCardView;
    @BindView(R.id.loginCardViewLinearLayout)
    LinearLayout loginCardViewLinearLayout;
    @BindView(R.id.passwordTextInputLayout)
    TextInputLayout passwordTextInputLayout;
    @BindView(R.id.passwordTextInputEditText)
    TextInputEditText passwordTextInputEditText;
    @BindView(R.id.confirmPasswordTextInputLayout)
    TextInputLayout confirmPasswordTextInputLayout;
    @BindView(R.id.confirmPasswordTextInputEditText)
    TextInputEditText confirmPasswordTextInputEditText;
    @BindView(R.id.changePasswordBtn)
    Button changePasswordBtn;
    @BindView(R.id.progressBar)
    View progressBar;

    FirebaseAuth firebaseAuth; // to create object of Firebase Auth class
    FirebaseUser firebaseUser; // to create object of Firebase User class to get current user

    String password, confirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        ButterKnife.bind(this);

        changePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPasswordCredentials();
            }
        });
    }

    // validating required fields

    private boolean validatePassword(){
        password = Objects.requireNonNull(passwordTextInputEditText.getText()).toString().trim();
        if(password.isEmpty()){
            passwordTextInputLayout.setError("Enter Password");
            return false;
        }else if(password.length()<6){
            passwordTextInputLayout.setError("Weak Password");
            return false;
        }else {
            passwordTextInputLayout.setError(null);
            passwordTextInputLayout.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateConfirmPassword(){
        confirmPassword = Objects.requireNonNull(confirmPasswordTextInputEditText.getText()).toString().trim();
        if(confirmPassword.isEmpty()){
            confirmPasswordTextInputLayout.setError("Re-Enter Password");
            return false;
        }else {
            if(confirmPassword.equals(password)){
                confirmPasswordTextInputLayout.setError(null);
                confirmPasswordTextInputLayout.setErrorEnabled(false);
                return true;
            }else {
                confirmPasswordTextInputLayout.setError("Password Mismatch");
                return false;
            }

        }
    }

    public void checkPasswordCredentials(){
        if(!validatePassword() | !validateConfirmPassword()){
            validatePassword();
            validateConfirmPassword();
        }else {
            progressBar.setVisibility(View.VISIBLE);
            firebaseAuth = FirebaseAuth.getInstance();
            firebaseUser = firebaseAuth.getCurrentUser();
            if(firebaseUser!=null){

                // updating password

                firebaseUser.updatePassword(password).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        if(task.isSuccessful()){
                            Toast.makeText(ChangePasswordActivity.this,"Password Changed Successfully",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(ChangePasswordActivity.this, ProfileActivity.class));
                            finish();
                        }else {
                            Toast.makeText(ChangePasswordActivity.this,"Password change Failed: " + task.getException(),Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }
}