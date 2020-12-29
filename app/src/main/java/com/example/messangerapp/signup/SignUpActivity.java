package com.example.messangerapp.signup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.messangerapp.PermissionsActivity;
import com.example.messangerapp.R;
import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.login.LoginActivity;
import com.example.messangerapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class SignUpActivity extends AppCompatActivity {

    @BindView(R.id.userProfileImage)
    CircleImageView userProfileImage;
    @BindView(R.id.signUpCardView)
    CardView signUpCardView;
    @BindView(R.id.signUpCardViewLinearLayout)
    LinearLayout signUpCardViewLinearLayout;
    @BindView(R.id.nameTextInputLayout)
    TextInputLayout nameTextInputLayout;
    @BindView(R.id.nameTextInputEditText)
    TextInputEditText nameTextInputEditText;
    @BindView(R.id.emailTextInputLayout)
    TextInputLayout emailTextInputLayout;
    @BindView(R.id.emailTextInputEditText)
    TextInputEditText emailTextInputEditText;
    @BindView(R.id.passwordTextInputLayout)
    TextInputLayout passwordTextInputLayout;
    @BindView(R.id.passwordTextInputEditText)
    TextInputEditText passwordTextInputEditText;
    @BindView(R.id.confirmPasswordTextInputLayout)
    TextInputLayout confirmPasswordTextInputLayout;
    @BindView(R.id.confirmPasswordTextInputEditText)
    TextInputEditText confirmPasswordTextInputEditText;
    @BindView(R.id.signUpBtn)
    Button signUpBtn;
    @BindView(R.id.progressBar)
    View progressBar;

    FirebaseAuth firebaseAuth; // to create object of Firebase Auth class to fetch currently loged in user
    FirebaseUser firebaseUser; // to create object of Firebase User class to get current user to store currently loged in user
    DatabaseReference userDatabaseReference;
    HashMap<String,String> usersNodeHashMap;
    StorageReference storageReference, profileImageStorageReference; // to upload profile image to firebase
    Uri serverImageUri, selectedImageUri;

    UserProfileChangeRequest userProfileChangeRequest;

    String name, email, password, confirmPassword, userFirebaseId, profileImageName;
    int profileImageRequestCode=101 , readExternalStorageRequestCode=102;

    Intent profileImageIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.bind(this);

        storageReference = FirebaseStorage.getInstance().getReference(); // give reference to root folder of file storage
        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // checking permissions to access phone storage
                if(ContextCompat.checkSelfPermission(SignUpActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                    profileImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(profileImageIntent,profileImageRequestCode);
                }else {
                    ActivityCompat.requestPermissions(SignUpActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},readExternalStorageRequestCode);
                }
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkSignUpCredentials();
            }
        });
    }

    // validating required fields

    private boolean validateName(){
        name = nameTextInputEditText.getText().toString().trim();
        if(name.isEmpty()){
            nameTextInputLayout.setError("Enter User Name");
            return false;
        }else {
            nameTextInputLayout.setError(null);
            nameTextInputLayout.setErrorEnabled(false);
            return true;
        }
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
                return true;
            }else {
                emailTextInputLayout.setError("Invalid Email Format");
                return false;
            }
        }
    }

    private boolean validatePassword(){
        password = passwordTextInputEditText.getText().toString().trim();
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
        confirmPassword = confirmPasswordTextInputEditText.getText().toString().trim();
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

    private boolean validateProfileImage(){
        if(selectedImageUri==null){
            Toast.makeText(SignUpActivity.this,"Add Profile Image",Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (resultCode == RESULT_OK) {
                    if (requestCode == profileImageRequestCode) {
                        // Get the url from data
                        selectedImageUri = data.getData();

                        if (null != selectedImageUri) {
                            // Get the path from the Uri
                            String path = getPathFromURI(selectedImageUri);
                            // Set the image in ImageView
                            userProfileImage.post(new Runnable() {
                                @Override
                                public void run() {
                                    userProfileImage.setImageURI(selectedImageUri);
                                }
                            });

                        }else {
                            validateProfileImage();
                        }
                    }
                }
            }
        }).start();
    }

    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==readExternalStorageRequestCode){
            if(ContextCompat.checkSelfPermission(SignUpActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Intent profileImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(profileImageIntent,profileImageRequestCode);
                }else {
                    Toast.makeText(SignUpActivity.this,"Access Gallery Permission Required",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void checkSignUpCredentials(){
        if(!validateName() | !validateEmail() | !validatePassword() | !validateConfirmPassword() | !validateProfileImage()){
            validateName();
            validateEmail();
            validatePassword();
            validateConfirmPassword();
            validateProfileImage();
        }else {
            if(Util.checkInternetConnection(SignUpActivity.this)) {
                progressBar.setVisibility(View.VISIBLE);
                firebaseAuth = FirebaseAuth.getInstance();

                // signing up user using Email and Password
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            firebaseUser = firebaseAuth.getCurrentUser();
                            updateUserProfile();
                        } else {
                            Toast.makeText(SignUpActivity.this, "SignUp Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }else {
                startActivity(new Intent(SignUpActivity.this, PermissionsActivity.class));
            }
        }
    }

    public void updateUserProfile(){
        progressBar.setVisibility(View.VISIBLE);
        userFirebaseId = firebaseUser.getUid();

        profileImageName = userFirebaseId + ".jpg";
        profileImageStorageReference = storageReference.child("profileImages/" + profileImageName);
        profileImageStorageReference.putFile(selectedImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() { // start uploading image on server
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    profileImageStorageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri serverUri) {
                            serverImageUri = serverUri;

                            userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(name).setPhotoUri(serverImageUri).build();
                            firebaseUser.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if(task.isSuccessful()){
                                        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS); // getReference() gives access to root
                                        usersNodeHashMap = new HashMap<>();
                                        usersNodeHashMap.put(NodeNames.NAME,name);
                                        usersNodeHashMap.put(NodeNames.EMAIL,email);
                                        usersNodeHashMap.put(NodeNames.PASSWORD,password);
                                        usersNodeHashMap.put(NodeNames.ACTIVESTATUS,"Online");
                                        usersNodeHashMap.put(NodeNames.PHOTOURL,serverImageUri.getPath());
                                        usersNodeHashMap.put(NodeNames.USERID,userFirebaseId);

                                        progressBar.setVisibility(View.VISIBLE);
                                        userDatabaseReference.child(userFirebaseId).setValue(usersNodeHashMap).addOnCompleteListener(new OnCompleteListener<Void>() { // adding userId to USERS node and corresponding data
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                progressBar.setVisibility(View.GONE);
                                                if(task.isSuccessful()){
                                                    Toast.makeText(SignUpActivity.this,"User Registered Successfully",Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                                }else {
                                                    Toast.makeText(SignUpActivity.this,"Failed to add details: " + task.getException(),Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }
}