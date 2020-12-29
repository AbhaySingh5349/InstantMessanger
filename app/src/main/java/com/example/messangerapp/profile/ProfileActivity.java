package com.example.messangerapp.profile;

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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.messangerapp.MainActivity;
import com.example.messangerapp.R;
import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.login.LoginActivity;
import com.example.messangerapp.password.ChangePasswordActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    /* to add profile picture, profile name, change password or logout from app */

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
    @BindView(R.id.changePasswordTextView)
    TextView changePasswordTextView;
    @BindView(R.id.confirmBtn)
    Button confirmBtn;
    @BindView(R.id.logoutBtn)
    Button logoutBtn;
    @BindView(R.id.progressBar)
    View progressBar;

    FirebaseAuth firebaseAuth; // to create object of Firebase Auth class
    FirebaseUser firebaseUser; // to create object of Firebase User class to get current user
    DatabaseReference userDatabaseReference;
    HashMap<String,String> usersNodeHashMap;
    StorageReference storageReference, profileImageStorageReference; // to upload profile image to firebase
    Uri serverImageUri, selectedImageUri, photoUri;

    UserProfileChangeRequest userProfileChangeRequest;

    String name, email, password, userFirebaseId, profileImageName;
    int profileImageRequestCode=101 , readExternalStorageRequestCode=102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);

        storageReference = FirebaseStorage.getInstance().getReference(); // give reference to root folder of file storage

        // getting current user
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if(firebaseUser!=null){
            nameTextInputEditText.setText(firebaseUser.getDisplayName());
            emailTextInputEditText.setText(firebaseUser.getEmail());
            emailTextInputEditText.setEnabled(false);
            serverImageUri = firebaseUser.getPhotoUrl();

            if(serverImageUri!=null){
                Glide.with(ProfileActivity.this).load(serverImageUri).placeholder(R.drawable.profile).error(R.drawable.profile).into(userProfileImage); // loading profile image
            }
            userProfileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // checking permissions to access phone storage
                    if(ContextCompat.checkSelfPermission(ProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                        Intent profileImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(profileImageIntent,profileImageRequestCode);

                    }else {
                        ActivityCompat.requestPermissions(ProfileActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},readExternalStorageRequestCode);
                    }
                }
            });
            changePasswordTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
                }
            });
            confirmBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkProfileCredentials();
                }
            });
            logoutBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(ProfileActivity.this,LoginActivity.class));
                    finish();
                }
            });
        }
    }

    // validating required fields

    private boolean validateName(){
        name = Objects.requireNonNull(nameTextInputEditText.getText()).toString().trim();
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
        email = Objects.requireNonNull(emailTextInputEditText.getText()).toString().trim();
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
            if(ContextCompat.checkSelfPermission(ProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Intent profileImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(profileImageIntent,profileImageRequestCode);
                }else {
                    Toast.makeText(ProfileActivity.this,"Access Gallery Permission Required",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void checkProfileCredentials(){
        if(!validateName() | !validateEmail()){
            validateName();
            validateEmail();
        }else {
            updateUserProfile();
        }
    }

    public void updateUserProfile(){
        if(validateName() && validateEmail()){
            userFirebaseId = firebaseUser.getUid();

            profileImageName = userFirebaseId + ".jpg";
            profileImageStorageReference = storageReference.child("profileImages/" + profileImageName);

            if(selectedImageUri==null){
                photoUri = serverImageUri;
            }else {
                photoUri = selectedImageUri;
            }

            profileImageStorageReference.putFile(photoUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()){
                        profileImageStorageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                photoUri = uri;
                            }
                        });
                    }
                }
            });
            userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(name).setPhotoUri(photoUri).build();
            firebaseUser.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    progressBar.setVisibility(View.GONE);
                    if(task.isSuccessful()){
                        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS); // getReference() gives access to root
                        usersNodeHashMap = new HashMap<>();
                        usersNodeHashMap.put(NodeNames.NAME,name);
                        usersNodeHashMap.put(NodeNames.EMAIL,email);
                        usersNodeHashMap.put(NodeNames.ACTIVESTATUS,"Online");
                        usersNodeHashMap.put(NodeNames.PHOTOURL,photoUri.getPath());
                        usersNodeHashMap.put(NodeNames.USERID,userFirebaseId);

                        userDatabaseReference.child(userFirebaseId).setValue(usersNodeHashMap).addOnCompleteListener(new OnCompleteListener<Void>() { // adding userId to USERS node and corresponding data
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(ProfileActivity.this,"Details Saved Successfully",Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(ProfileActivity.this,MainActivity.class));
                                    finish();
                                }else {
                                    Toast.makeText(ProfileActivity.this,"Failed to add details: " + task.getException(),Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            });
        }
    }
}