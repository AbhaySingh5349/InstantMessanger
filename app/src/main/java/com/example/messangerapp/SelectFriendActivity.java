package com.example.messangerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.messangerapp.firebasetree.Constants;
import com.example.messangerapp.firebasetree.Extras;
import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.model.FindFriendsModelClass;
import com.example.messangerapp.model.SelectFriendModelClass;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class SelectFriendActivity extends AppCompatActivity {

    @BindView(R.id.selectFriendRecyclerView)
    RecyclerView selectFriendRecyclerView;

    private View progressBar;

    FirebaseAuth firebaseAuth; // to create object of Firebase Auth class to fetch currently loged in user
    FirebaseUser firebaseUser; // to create object of Firebase User class to get current user to store currently loged in user
    DatabaseReference userDatabaseReference, contactsDatabaseReference;

    String currentUserId, forwardMessageId, forwardMessageValue, forwardMessageType;

    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);
        ButterKnife.bind(this);

        if(getIntent().hasExtra(Extras.MESSAGEVALUE)){
            forwardMessageValue = getIntent().getStringExtra(Extras.MESSAGEVALUE);
            forwardMessageId = getIntent().getStringExtra(Extras.MESSAGEID);
            forwardMessageType = getIntent().getStringExtra(Extras.MESSAGETYPE);
        }

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        selectFriendRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        currentUserId = Objects.requireNonNull(firebaseUser).getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        contactsDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.CONTACTS);

        onStart();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<SelectFriendModelClass> firebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<SelectFriendModelClass>().setQuery(contactsDatabaseReference.child(currentUserId),SelectFriendModelClass.class).build();

        FirebaseRecyclerAdapter<SelectFriendModelClass, SelectFriendViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<SelectFriendModelClass, SelectFriendViewHolder>(firebaseRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull SelectFriendViewHolder holder, int position, @NonNull SelectFriendModelClass model) {

                progressBar.setVisibility(View.GONE);

                String friendId = getRef(position).getKey();
                final String[] photoName = new String[1];
                final String[] name = new String[1];

                        userDatabaseReference.child(Objects.requireNonNull(friendId)).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists()){
                                    name[0] = Objects.requireNonNull(snapshot.child(NodeNames.NAME).getValue()).toString();
                                    holder.userNameTextView.setText(name[0]);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        StorageReference profileImage = FirebaseStorage.getInstance().getReference().child(Constants.IMAGESFOLDER + "/" + friendId + ".jpg");
                        profileImage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                photoName[0] = String.valueOf(uri);
                                Glide.with(SelectFriendActivity.this).load(uri).placeholder(R.drawable.profile).into(holder.userProfileImageView);
                            }
                        });

                        holder.selectFriendLinearLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                returnSelectedFriend(friendId,name[0],photoName[0]);
                            }
                        });
            }

            @NonNull
            @Override
            public SelectFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_friend_layout,parent,false);
                return new SelectFriendViewHolder(view);
            }
        };
        selectFriendRecyclerView.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public static class SelectFriendViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout selectFriendLinearLayout;
        private CircleImageView userProfileImageView;
        private TextView userNameTextView;

        public SelectFriendViewHolder(@NonNull View itemView) {
            super(itemView);

            selectFriendLinearLayout = itemView.findViewById(R.id.selectFriendLinearLayout);
            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
        }
    }

    public void returnSelectedFriend(String userId, String userName, String photoName){
        Intent intent = new Intent();

        intent.putExtra(Extras.CHATUSERID,userId);
        intent.putExtra(Extras.CHATUSERNAME,userName);
        intent.putExtra(Extras.CHATUSERPHOTO,photoName);

        intent.putExtra(Extras.MESSAGEVALUE,forwardMessageValue);
        intent.putExtra(Extras.MESSAGETYPE,forwardMessageType);
        intent.putExtra(Extras.MESSAGEVALUE,forwardMessageValue);

        setResult(Activity.RESULT_OK,intent);
        finish();
    }
}