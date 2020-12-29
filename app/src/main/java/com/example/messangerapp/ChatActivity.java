package com.example.messangerapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.messangerapp.adapter.MessageAdapter;
import com.example.messangerapp.firebasetree.Constants;
import com.example.messangerapp.firebasetree.Extras;
import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.model.MessageModelClass;
import com.example.messangerapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Node;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatActivity extends AppCompatActivity {

    /* displaying all the chat messages with time between two friends with multiple actions can be performed on chat messages (on long click) :
       --> for text messages: Delete message, share message
       --> for audio/video messages: Download file, Delete message, share message
     */

    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.chatsRecyclerView)
    RecyclerView chatsRecyclerView;
    @BindView(R.id.uploadFileLinearLayout)
    LinearLayout uploadFileLinearLayout;
    @BindView(R.id.messageEditText)
    EditText messageEditText;
    @BindView(R.id.attachmentImageView)
    ImageView attachmentImageView;
    @BindView(R.id.sendMessageImageView)
    ImageView sendMessageImageView;

    private MessageAdapter messageAdapter;
    private List<MessageModelClass> messageModelClassList;

    private int currentPage = 1;
    private static final int messagePerPage = 30, readExternalStorageCode = 1001, cameraClickRequestCode = 1002, photoPickRequestCode = 1003, videoPickRequestCode = 1004, writeExternalStorageCode = 1005, forwardMessageCode = 1006;
    private ChildEventListener childEventListener;

    private ImageView userProfileImageView;
    private TextView userNameTextView, onlineStatusTextView;
    private String userProfileName, userPhotoName;

    private BottomSheetDialog bottomSheetDialog; // comes from bottom of activity

    FirebaseAuth firebaseAuth; // to create object of Firebase Auth class to fetch currently loged in user
    FirebaseUser firebaseUser; // to create object of Firebase User class to get current user to store currently loged in user
    DatabaseReference userDatabaseReference, contactsDatabaseReference, rootDatabaseReference, messageDataBaseReference, chatsDatabaseReference;

    String currentUserId, chatUserId; // chatUserId will be retrieved from ChatListAdapter through intent

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        // getting current user

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        currentUserId = Objects.requireNonNull(firebaseUser).getUid();

        // referring to database nodes

        rootDatabaseReference = FirebaseDatabase.getInstance().getReference();
        userDatabaseReference = rootDatabaseReference.child(NodeNames.USERS);
        contactsDatabaseReference = rootDatabaseReference.child(NodeNames.CONTACTS);
        chatsDatabaseReference = rootDatabaseReference.child(NodeNames.CHATS);

        // attaching custom action bar layout

        ActionBar actionBar = getSupportActionBar(); // gives object of action bar
        if(actionBar!=null){
            actionBar.setTitle("");
            ViewGroup viewGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.chat_action_bar,null);

            actionBar.setDisplayHomeAsUpEnabled(true); // adding arrow on action bar to move to previous activity
            actionBar.setHomeButtonEnabled(true);
            actionBar.setElevation(0);
            actionBar.setCustomView(viewGroup);
            actionBar.setDisplayOptions(actionBar.getDisplayOptions()|ActionBar.DISPLAY_SHOW_CUSTOM);

            userProfileImageView = viewGroup.findViewById(R.id.userProfileImageView);
            userNameTextView = viewGroup.findViewById(R.id.userNameTextView);
            onlineStatusTextView = viewGroup.findViewById(R.id.onlineStatusTextView);
        }

        // receiving intent from ChatListAdapter

        if(getIntent().hasExtra(Extras.CHATUSERID)){
            chatUserId = getIntent().getStringExtra(Extras.CHATUSERID);

            userDatabaseReference.child(chatUserId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String status = "";

                    if(snapshot.child(NodeNames.ACTIVESTATUS).getValue()!=null){
                        status = Objects.requireNonNull(snapshot.child(NodeNames.ACTIVESTATUS).getValue()).toString();
                    }
                    if(status.equals("Online")){
                        onlineStatusTextView.setText("Online");
                    }else {
                        onlineStatusTextView.setText("Offline");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        if(getIntent().hasExtra(Extras.CHATUSERNAME)){
            userProfileName = getIntent().getStringExtra(Extras.CHATUSERNAME);
            userNameTextView.setText(userProfileName);
        }
        if(getIntent().hasExtra(Extras.CHATUSERPHOTO)){
            userPhotoName = getIntent().getStringExtra(Extras.CHATUSERPHOTO);

            if(!TextUtils.isEmpty(userPhotoName)){
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(Constants.IMAGESFOLDER + "/" + chatUserId + ".jpg");
                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(ChatActivity.this).load(uri).placeholder(R.drawable.profile).into(userProfileImageView);
                    }
                });
            }
        }

    /*    if(getIntent().hasExtra(Extras.MESSAGEVALUE) && getIntent().hasExtra(Extras.MESSAGEID) && getIntent().hasExtra(Extras.MESSAGETYPE)){
            String message = getIntent().getStringExtra(Extras.MESSAGEVALUE);
            String messageId = getIntent().getStringExtra(Extras.MESSAGEID);
            String messageType = getIntent().getStringExtra(Extras.MESSAGETYPE);

            DatabaseReference forwardMessageReference = rootDatabaseReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();

            String forwardMessageId  = forwardMessageReference.getKey();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if(Objects.equals(messageType, Constants.TEXTMESSAGE)){
                    sendMessage(message,messageType,forwardMessageId);
                }else {
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                    String folderName = messageType.equals(Constants.VIDEOMESSAGE)?(Constants.VIDEOMESSAGEFOLDER):(Constants.IMAGEMESSAGEFOLDER);
                    String oldFileName = messageType.equals(Constants.VIDEOMESSAGE)?(messageId + ".mp4"):(messageId + ".jpg");
                    String newFileName = messageType.equals(Constants.VIDEOMESSAGE)?(forwardMessageId + ".mp4"):(messageId + ".jpg");

                    String localFilePath = Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath() + "/" + oldFileName;
                    File localFile = new File(localFilePath);

                    StorageReference newFileReference = storageReference.child(folderName).child(newFileName);
                    storageReference.child(folderName).child(oldFileName).getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                            sendMessage(String.valueOf(Uri.fromFile(localFile)),messageType,messageId);
                        }
                    });
                }
            }
        } */

        messageModelClassList = new ArrayList<>();
        messageAdapter = new MessageAdapter(ChatActivity.this,messageModelClassList);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatsRecyclerView.setAdapter(messageAdapter);

        sendMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Util.checkInternetConnection(ChatActivity.this)){
                    DatabaseReference messageReference = rootDatabaseReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
                    String messagePushId = messageReference.getKey();
                    String message = messageEditText.getText().toString().trim();
                    sendMessage(message, Constants.TEXTMESSAGE,messagePushId);
                }else {
                    Toast.makeText(ChatActivity.this,"No internet connection",Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadMessages();
        chatsRecyclerView.scrollToPosition(messageModelClassList.size()-1);

        rootDatabaseReference.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREADCOUNT).setValue(0); // setting unread count to zero after reading messages

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currentPage++;
                loadMessages();
            }
        });

        // bottom sheet dialog appears after clicking on attachment image

        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.upload_file_layout,null);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setCancelable(false);

        attachmentImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // checking permission to access device gallery
                if(ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    if(bottomSheetDialog != null){
                        bottomSheetDialog.show();
                    }
                }else {
                    ActivityCompat.requestPermissions(ChatActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},readExternalStorageCode);
                }

                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(),0);
            }
        });

        view.findViewById(R.id.cameraImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,cameraClickRequestCode);
            }
        });

        view.findViewById(R.id.photoImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,photoPickRequestCode);
            }
        });

        view.findViewById(R.id.videoImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,videoPickRequestCode);
            }
        });

        view.findViewById(R.id.closeImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
            }
        });

        // updating "typing status" to ChatDatabaseReference

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if(editable.toString().matches("")){
                    chatsDatabaseReference.child(currentUserId).child(chatUserId).child(NodeNames.TYPINGSTATUS).setValue("0");
                }else {
                    chatsDatabaseReference.child(currentUserId).child(chatUserId).child(NodeNames.TYPINGSTATUS).setValue("1");
                }
            }
        });

        chatsDatabaseReference.child(chatUserId).child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(NodeNames.TYPINGSTATUS).getValue()!=null){
                    String typingStatus = Objects.requireNonNull(snapshot.child(NodeNames.TYPINGSTATUS).getValue()).toString();

                    if(typingStatus.equals("1")){
                        onlineStatusTextView.setText("typing...");
                    }else {
                        onlineStatusTextView.setText("Online");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String message, String messageType, String messageId){
        if(!message.equals("")){
            HashMap<String,Object> messageHashMap = new HashMap<>();
            messageHashMap.put(NodeNames.MESSAGEID,messageId);
            messageHashMap.put(NodeNames.MESSAGEVALUE,message);
            messageHashMap.put(NodeNames.MESSAGEFROM,currentUserId);
            messageHashMap.put(NodeNames.MESSAGETIME,ServerValue.TIMESTAMP);
            messageHashMap.put(NodeNames.MESSAGETYPE,messageType);


            String currentUserReference = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
            String chatUserReference = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;

            HashMap<String,Object> messageUserMap = new HashMap<>();
            messageUserMap.put(currentUserReference + "/" + messageId, messageHashMap);
            messageUserMap.put(chatUserReference + "/" + messageId, messageHashMap);

            messageEditText.setText(null);

            rootDatabaseReference.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if(error!=null){
                        Toast.makeText(ChatActivity.this,"Failed to send message: " + error.getMessage(),Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(ChatActivity.this,"Message Sent",Toast.LENGTH_SHORT).show();

                    /*    String notificationTitle = "";

                        if(messageType.equals(Constants.TEXTMESSAGE)){
                            notificationTitle = "New Message";
                        }else if(messageType.equals(Constants.IMAGEMESSAGE)){
                            notificationTitle = "New Image";
                        }else {
                            notificationTitle = "New video";
                        }
                        String notificationMessage = "Friend Request Accepted by: " + firebaseUser.getDisplayName();
                        Util.sendNotification(ChatActivity.this,notificationTitle,message,chatUserId); */

                        String title = "";
                        if(messageType.equals(Constants.TEXTMESSAGE)){
                            title = "New Message";
                        }
                        if(messageType.equals(Constants.IMAGEMESSAGE)){
                            title = "New Image";
                        }
                        if(messageType.equals(Constants.VIDEOMESSAGE)){
                            title = "New Video";
                        }
                        String lastMessage = !title.equals("New Message")?title:message;
                        Util.updateChatDetails(ChatActivity.this,currentUserId,chatUserId,lastMessage); // updating ChatDatabaseReference
                    }
                }
            });
        }
    }

    private void loadMessages(){
        messageModelClassList.clear();
        messageDataBaseReference = rootDatabaseReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);

        Query messageQuery = messageDataBaseReference.limitToLast(currentPage*messagePerPage);

        if(childEventListener != null){
            messageQuery.removeEventListener(childEventListener);
        }

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                MessageModelClass messageModelClass = snapshot.getValue(MessageModelClass.class);

                messageModelClassList.add(messageModelClass);
                messageAdapter.notifyDataSetChanged();

                chatsRecyclerView.scrollToPosition(messageModelClassList.size()-1);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                loadMessages(); // refresh list on delete of messages
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
            }
        };

        messageQuery.addChildEventListener(childEventListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            // if anything is selected
            if(requestCode == cameraClickRequestCode){
                Bitmap bitmap = (Bitmap) Objects.requireNonNull(Objects.requireNonNull(data).getExtras()).get("data");

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Objects.requireNonNull(bitmap).compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                uploadBytes(byteArrayOutputStream, Constants.IMAGEMESSAGE);

            }else if(requestCode == photoPickRequestCode){
                Uri uri = Objects.requireNonNull(data).getData();
                uploadFile(uri,Constants.IMAGEMESSAGE);

            }else if(requestCode == videoPickRequestCode){
                Uri uri = Objects.requireNonNull(data).getData();
                uploadFile(uri,Constants.VIDEOMESSAGE);
            }else if(requestCode == forwardMessageCode){
                Intent intent = new Intent(this,ChatActivity.class);

                intent.putExtra(Extras.CHATUSERNAME, Objects.requireNonNull(data).getStringExtra(Extras.CHATUSERNAME));
                intent.putExtra(Extras.CHATUSERID, Objects.requireNonNull(data).getStringExtra(Extras.CHATUSERID));
                intent.putExtra(Extras.CHATUSERPHOTO, Objects.requireNonNull(data).getStringExtra(Extras.CHATUSERPHOTO));

                intent.putExtra(Extras.MESSAGEVALUE, Objects.requireNonNull(data).getStringExtra(Extras.MESSAGEVALUE));
                intent.putExtra(Extras.MESSAGETYPE,data.getStringExtra(Extras.MESSAGETYPE));
                intent.putExtra(Extras.MESSAGEID,data.getStringExtra(Extras.MESSAGEID));

                startActivity(intent);
                finish();
            }
        }
    }

    private void uploadFile(Uri uri, String messageType){
        DatabaseReference messageIdDatabaseReference = rootDatabaseReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();

        String messageId = messageIdDatabaseReference.getKey();

        String folderName = messageType.equals(Constants.VIDEOMESSAGE)?(Constants.VIDEOMESSAGEFOLDER):(Constants.IMAGEMESSAGEFOLDER);
        String fileName = messageType.equals(Constants.VIDEOMESSAGE)?(messageId + ".mp4"):(messageId + ".jpg");

        StorageReference fileReference = storageReference.child(folderName).child(fileName);
        fileReference.putFile(uri);
        sendMessage(uri.toString(),messageType,messageId);
    //    UploadTask uploadTask = fileReference.putFile(uri);

    //    uploadFileProgress(uploadTask,fileReference,messageId,messageType);
    }

    private void uploadBytes(ByteArrayOutputStream byteArrayOutputStream, String messageType){
        DatabaseReference messageIdDatabaseReference = rootDatabaseReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();

        String messageId = messageIdDatabaseReference.getKey();

        String folderName = messageType.equals(Constants.VIDEOMESSAGE)?(Constants.VIDEOMESSAGEFOLDER):(Constants.IMAGEMESSAGEFOLDER);
        String fileName = messageType.equals(Constants.VIDEOMESSAGE)?(messageId + ".mp4"):(messageId + ".jpg");

        StorageReference fileReference = storageReference.child(folderName).child(fileName);
        fileReference.putBytes(byteArrayOutputStream.toByteArray());
        sendMessage(Arrays.toString(byteArrayOutputStream.toByteArray()),messageType,messageId);
     //   UploadTask uploadTask = fileReference.putBytes(byteArrayOutputStream.toByteArray());

    //    uploadFileProgress(uploadTask,fileReference,messageId,messageType);
    }

    @SuppressLint("SetTextI18n")
    private void uploadFileProgress(UploadTask uploadTask, StorageReference storageReference, String messageId, String messageType){

        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.upload_file_layout,null);

        TextView uploadingFileTextView = view.findViewById(R.id.uploadingFileTextView);
        ProgressBar progressBar = view.findViewById(R.id.uploadFileProgressBar);
        ImageView pauseUploadImageView = view.findViewById(R.id.pauseUploadImageView);
        ImageView playUploadImageView = view.findViewById(R.id.playUploadImageView);
        ImageView cancelImageView = view.findViewById(R.id.cancelImageView);

        pauseUploadImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadTask.pause();
                pauseUploadImageView.setVisibility(View.GONE);
                playUploadImageView.setVisibility(View.VISIBLE);
            }
        });

        playUploadImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadTask.resume();
                pauseUploadImageView.setVisibility(View.VISIBLE);
                playUploadImageView.setVisibility(View.GONE);
            }
        });

        cancelImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadTask.cancel();
            }
        });

        uploadFileLinearLayout.addView(view);
        uploadingFileTextView.setText("Uploading: " + messageType + " 0%");

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                progressBar.setProgress((int) progress);
                uploadingFileTextView.setText("Uploading: " + messageType + " " + progressBar.getProgress() + "%");
            }
        });

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                uploadFileLinearLayout.removeView(view);
                if(task.isSuccessful()){
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();

                            sendMessage(downloadUrl,messageType,messageId);
                        }
                    });
                }
            }
        });

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                uploadFileLinearLayout.removeView(view);
                Toast.makeText(ChatActivity.this,"Failed to upload file: " + e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==readExternalStorageCode){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(ChatActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                    if(bottomSheetDialog != null){
                        bottomSheetDialog.show();
                    }
                }else {
                    Toast.makeText(ChatActivity.this,"Permissions required to access files",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();

        switch (itemId){
            case android.R.id.home:
                 finish(); // handling back arrow click on action bar
                 break;

            default:
                 break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void deleteMessage(String messageId, String messageType){
        DatabaseReference currentDatabaseReference = rootDatabaseReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).child(messageId);

        currentDatabaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    DatabaseReference chatDatabaseReference = rootDatabaseReference.child(NodeNames.MESSAGES).child(chatUserId).child(currentUserId).child(messageId);
                    chatDatabaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(ChatActivity.this,"Message Deleted",Toast.LENGTH_SHORT).show();
                                if(!messageType.equals(Constants.TEXTMESSAGE)){
                                    StorageReference storageReference = FirebaseStorage.getInstance().getReference();

                                    String folderName = messageType.equals(Constants.VIDEOMESSAGE)?(Constants.VIDEOMESSAGEFOLDER):(Constants.IMAGEMESSAGEFOLDER);
                                    String fileName = messageType.equals(Constants.VIDEOMESSAGE)?(messageId + ".mp4"):(messageId + ".jpg");

                                    StorageReference fileReference = storageReference.child(folderName).child(fileName);

                                    fileReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(!task.isSuccessful()){
                                                Toast.makeText(ChatActivity.this,"Failed to delete file",Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            }else {
                                Toast.makeText(ChatActivity.this,"Failed to delete message: " + task.getException(),Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else {
                    Toast.makeText(ChatActivity.this,"Failed to delete message: " + task.getException(),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void downloadMessageFile(String messageId, String messageType, boolean isShare){
        if(ActivityCompat.checkSelfPermission(ChatActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){

            StorageReference storageReference = FirebaseStorage.getInstance().getReference();

            String folderName = messageType.equals(Constants.VIDEOMESSAGE)?(Constants.VIDEOMESSAGEFOLDER):(Constants.IMAGEMESSAGEFOLDER);
            String fileName = messageType.equals(Constants.VIDEOMESSAGE)?(messageId + ".mp4"):(messageId + ".jpg");

            StorageReference fileReference = storageReference.child(folderName).child(fileName);
            String localFilePath = Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath() + "/" + fileName;

            File localFile = new File(localFilePath);

            try {
                if(localFile.exists() || localFile.createNewFile()){

                    FileDownloadTask fileDownloadTask = fileReference.getFile(localFile);

                    fileDownloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                            if(task.isSuccessful()){
                                if(isShare){
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.putExtra(Intent.EXTRA_STREAM,Uri.parse(localFilePath));

                                    if(messageType.equals(Constants.IMAGEMESSAGE)){
                                        intent.setType("image/jpg");
                                    }
                                    if(messageType.equals(Constants.VIDEOMESSAGE)){
                                        intent.setType("video/mp4");
                                    }

                                    startActivity(Intent.createChooser(intent,"Share with..."));
                                }else {
                                    Snackbar snackbar = Snackbar.make(uploadFileLinearLayout,"File downloaded successfully",Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                    snackbar.setAction("View File", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Uri uri = Uri.parse(localFilePath);
                                            Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                                            if(messageType.equals(Constants.VIDEOMESSAGE)){
                                                intent.setDataAndType(uri,"video/mp4");
                                            }else if(messageType.equals(Constants.IMAGEMESSAGE)){
                                                intent.setDataAndType(uri,"image/jpg");
                                            }

                                            startActivity(intent);
                                        }
                                    });
                                }
                            }else {
                                Toast.makeText(ChatActivity.this,"Failed to download file",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }else {
                    Toast.makeText(ChatActivity.this,"Failed to store file",Toast.LENGTH_SHORT).show();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }else {
            ActivityCompat.requestPermissions(ChatActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},writeExternalStorageCode);
        }
    }

    public void forwardMessage(String messageId, String messageValue, String messageType){
        Intent intent = new Intent(ChatActivity.this,SelectFriendActivity.class);
        intent.putExtra(Extras.MESSAGEID,messageId);
        intent.putExtra(Extras.MESSAGEVALUE,messageValue);
        intent.putExtra(Extras.MESSAGETYPE,messageType);
        startActivityForResult(intent,forwardMessageCode);
    }

    @Override
    public void onBackPressed() {

        rootDatabaseReference.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREADCOUNT).setValue(0);

        super.onBackPressed();
    }
}