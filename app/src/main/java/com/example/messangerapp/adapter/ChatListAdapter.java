package com.example.messangerapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messangerapp.ChatActivity;
import com.example.messangerapp.R;
import com.example.messangerapp.firebasetree.Constants;
import com.example.messangerapp.firebasetree.Extras;
import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.model.ChatModelClass;
import com.example.messangerapp.util.Util;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>{

    private Context context;
    private List<ChatModelClass> chatModelClassList;

    long currentUserTimeStamp = 0, chatUserTimeStamp = 0;
    DatabaseReference chatReference;

    public ChatListAdapter(Context context, List<ChatModelClass> chatModelClassList) {
        this.context = context;
        this.chatModelClassList = chatModelClassList;
    }

    @NonNull
    @Override
    public ChatListAdapter.ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_layout,parent,false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListAdapter.ChatListViewHolder holder, int position) {
        ChatModelClass chatModelClass = chatModelClassList.get(position);

        String chatUserId = chatModelClass.getUserId(); // getting chat user id

        chatReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // retrieving last message shared between the two with time and whether it was "sent" or "received" by particular user

        chatReference.child(chatUserId).child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.hasChild(NodeNames.LASTMESSAGETIME)){
                    currentUserTimeStamp = Long.parseLong(snapshot.child(NodeNames.LASTMESSAGETIME).getValue().toString());
                }

                chatReference.child(currentUserId).child(chatUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists() && snapshot.hasChild(NodeNames.LASTMESSAGETIME)){
                            chatUserTimeStamp = Long.parseLong(snapshot.child(NodeNames.LASTMESSAGETIME).getValue().toString());
                        }
                        if(chatUserTimeStamp < currentUserTimeStamp){
                            chatReference.child(chatUserId).child(currentUserId).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.exists() && snapshot.hasChild(NodeNames.LASTMESSAGE)){
                                        String lastMessage = snapshot.child(NodeNames.LASTMESSAGE).getValue().toString();
                                        lastMessage = lastMessage.length()>30?lastMessage.substring(0,30):lastMessage; // showing 1st 30 characters of last received message
                                        holder.lastMessageTextView.setText("(sent): " + lastMessage);

                                        chatUserTimeStamp = Long.parseLong(snapshot.child(NodeNames.LASTMESSAGETIME).getValue().toString());
                                        holder.lastMessageTimeTextView.setText(timeAgo(chatUserTimeStamp)); // function to get message time ago
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }else {
                            chatReference.child(currentUserId).child(chatUserId).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.exists() && snapshot.hasChild(NodeNames.LASTMESSAGE)){
                                        String lastMessage = snapshot.child(NodeNames.LASTMESSAGE).getValue().toString();
                                        lastMessage = lastMessage.length()>30?lastMessage.substring(0,30):lastMessage; // showing 1st 30 characters of last received message
                                        holder.lastMessageTextView.setText("(received): " + lastMessage);

                                        currentUserTimeStamp = Long.parseLong(snapshot.child(NodeNames.LASTMESSAGETIME).getValue().toString());
                                        holder.lastMessageTimeTextView.setText(timeAgo(currentUserTimeStamp)); // function to get message time ago
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.userNameTextView.setText(chatModelClass.getName());

        StorageReference profileImage = FirebaseStorage.getInstance().getReference().child(Constants.IMAGESFOLDER + "/" + chatUserId + ".jpg");
        profileImage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context).load(uri).placeholder(R.drawable.profile).into(holder.userProfileImageView); // loading chat user profile image
            }
        });

        if(!chatModelClass.getUnreadCount().equals("0")){
            holder.unreadCountTextView.setVisibility(View.VISIBLE);
            holder.unreadCountTextView.setText(chatModelClass.getUnreadCount()); // setting unread count value
        }else {
            holder.unreadCountTextView.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(Extras.CHATUSERID,chatModelClass.getUserId()); // id of user clicked
                intent.putExtra(Extras.CHATUSERNAME,chatModelClass.getName());
                intent.putExtra(Extras.CHATUSERPHOTO,chatModelClass.getPhotoURL());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatModelClassList.size();
    }

    public static class ChatListViewHolder extends RecyclerView.ViewHolder{

        private CircleImageView userProfileImageView;
        private TextView userNameTextView,lastMessageTextView,unreadCountTextView,lastMessageTimeTextView;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);

            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            unreadCountTextView = itemView.findViewById(R.id.unreadCountTextView);
            lastMessageTimeTextView = itemView.findViewById(R.id.lastMessageTimeTextView);
        }
    }

    public static String timeAgo(long time){
        final long SECOND_MILLIS = 1000;
        final long MINUTE_MILLIS = 60*SECOND_MILLIS;
        final long HOUR_MILLIS = 60*MINUTE_MILLIS;
        final long DAY_MILLIS = 24*HOUR_MILLIS;

        long now = System.currentTimeMillis();

        long difference = now-time;

        if(difference<MINUTE_MILLIS){
            return "just now";
        }else if(difference<2*MINUTE_MILLIS){
            return "a minute ago";
        }else if(difference<59*MINUTE_MILLIS){
            return difference/MINUTE_MILLIS + " minutes ago";
        }else if(difference<90*MINUTE_MILLIS){
            return "an hour ago";
        }else if(difference<24*HOUR_MILLIS){
            return difference/HOUR_MILLIS + " hours ago";
        }else if(difference<48*HOUR_MILLIS){
            return "yesterday";
        }else {
            return difference/DAY_MILLIS + " days ago";
        }
    }
}


