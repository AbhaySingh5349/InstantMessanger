package com.example.messangerapp.fragment;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.messangerapp.R;
import com.example.messangerapp.adapter.ChatListAdapter;
import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.model.ChatModelClass;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatFragment extends Fragment {

    /* displaying list of all the friends and tabs of chats with each friend, having last message shared with time, unread count of messages */

    private RecyclerView chatFragmentRecyclerView;
    private ChatListAdapter chatListAdapter;
    private List<ChatModelClass> chatModelClassList;
    private ChildEventListener childEventListener; // since list will be updated every time message is sent that is why cannot use Value Event Listener

    private Query query;
    private List<String> userIds;

    FirebaseAuth firebaseAuth; // to create object of Firebase Auth class to fetch currently loged in user
    FirebaseUser firebaseUser; // to create object of Firebase User class to get current user to store currently loged in user
    DatabaseReference userDatabaseReference, chatsDatabaseReference;

    String currentUserId;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatFragmentRecyclerView = view.findViewById(R.id.chatFragmentRecyclerView);

        chatModelClassList = new ArrayList<>();
        userIds = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);    // so the last received message is shown at top
        linearLayoutManager.setStackFromEnd(true);
        chatFragmentRecyclerView.setLayoutManager(linearLayoutManager);

        chatListAdapter = new ChatListAdapter(getContext(),chatModelClassList);
        chatFragmentRecyclerView.setAdapter(chatListAdapter);

        // getting current user

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        currentUserId = Objects.requireNonNull(firebaseUser).getUid();

        // getting reference to database nodes

        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        chatsDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);

        query = chatsDatabaseReference.child(currentUserId).orderByChild(NodeNames.TIMESTAMP);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // verifying current user should not be added to chat list, as not allowed to chat with itself
                    if(!Objects.equals(snapshot.getKey(), currentUserId)){
                        updateChatList(snapshot,true,snapshot.getKey()); // updated on addition of new friend
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // verifying current user should not be added to chat list, as not allowed to chat with itself
                    if(!Objects.equals(snapshot.getKey(), currentUserId)){
                        updateChatList(snapshot,false,snapshot.getKey()); // updates when new message is shared
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        query.addChildEventListener(childEventListener);

        return view;
    }

    private void updateChatList(DataSnapshot dataSnapshot,boolean isNewRecord, String userId){
        String lastMessage, lastMessageTime;

        // getting unread message count
        String unreadCount = dataSnapshot.child(NodeNames.UNREADCOUNT).getValue()==null?"0": Objects.requireNonNull(dataSnapshot.child(NodeNames.UNREADCOUNT).getValue()).toString();

        if(dataSnapshot.child(NodeNames.LASTMESSAGE).getValue()!=null){
            lastMessage = Objects.requireNonNull(dataSnapshot.child(NodeNames.LASTMESSAGE).getValue()).toString();
        }else {
            lastMessage = "";
        }

        if(dataSnapshot.child(NodeNames.LASTMESSAGETIME).getValue()!=null){
            lastMessageTime = Objects.requireNonNull(dataSnapshot.child(NodeNames.LASTMESSAGETIME).getValue()).toString();
        }else {
            lastMessageTime = "";
        }

        userDatabaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){

                    chatModelClassList.clear();

                    String userName = Objects.requireNonNull(snapshot.child(NodeNames.NAME).getValue()).toString();
                    String profileImage = Objects.requireNonNull(snapshot.child(NodeNames.PHOTOURL).getValue()).toString();

                    ChatModelClass chatModelClass = new ChatModelClass(userId, userName, profileImage, unreadCount, lastMessage, lastMessageTime);

                    if(isNewRecord){
                        chatModelClassList.add(chatModelClass);
                        userIds.add(userId);
                    }else {
                        int indexOfUserClicked = userIds.indexOf(userId);
                        chatModelClassList.add(indexOfUserClicked,chatModelClass);
                    }
                    chatListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),"Failed to fetch Chat List: " + error.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        query.removeEventListener(childEventListener);
    }
}