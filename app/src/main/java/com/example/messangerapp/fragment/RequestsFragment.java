package com.example.messangerapp.fragment;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.messangerapp.R;
import com.example.messangerapp.firebasetree.Constants;
import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.model.FindFriendsModelClass;
import com.example.messangerapp.util.Util;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestsFragment extends Fragment {

    /* displaying friend requests received by user (allowing accept or reject request) and friend requests sent by user (allowing to cancel request) */

    private RecyclerView requestsFragmentRecyclerView;
    private View requestFragmentProgressBar;
    private TextView requestsListTextView;

    FirebaseAuth firebaseAuth; // to create object of Firebase Auth class to fetch currently logged in user
    FirebaseUser firebaseUser; // to create object of Firebase User class to get current user to store currently logged in user
    DatabaseReference usersDatabaseReference, friendRequestDatabaseReference, contactsDatabaseReference, requestTypeDatabaseReference, chatDatabaseReference;

    private String currentUserId;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RequestsFragment() {
        // Required empty public constructor
    }

    public static RequestsFragment newInstance(String param1, String param2) {
        RequestsFragment fragment = new RequestsFragment();
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
        View view =  inflater.inflate(R.layout.fragment_requests, container, false);

        requestsFragmentRecyclerView = view.findViewById(R.id.requestsFragmentRecyclerView);
        requestFragmentProgressBar = view.findViewById(R.id.requestFragmentProgressBar);
        requestsListTextView = view.findViewById(R.id.requestsListTextView);

        // getting current user

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        currentUserId = Objects.requireNonNull(firebaseUser).getUid();

        // referring to database nodes

        usersDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        friendRequestDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIENDREQUESTS);
        contactsDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.CONTACTS);
        chatDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);

        requestsFragmentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // FirebaseRecyclerOptions provided by the FirebaseUI to make a query in the database to fetch appropriate data

        FirebaseRecyclerOptions<FindFriendsModelClass> friendRequestsRecyclerOptions = new FirebaseRecyclerOptions.Builder<FindFriendsModelClass>().setQuery(friendRequestDatabaseReference.child(currentUserId),FindFriendsModelClass.class).build();

        // FirebaseRecyclerAdapter binds a Query to a RecyclerView and responds to all real-time events included items being added, removed, moved, or changed

        FirebaseRecyclerAdapter<FindFriendsModelClass,RequestsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<FindFriendsModelClass, RequestsViewHolder>(friendRequestsRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull RequestsViewHolder holder, int position, @NonNull FindFriendsModelClass model) {

                String requestListId = Objects.requireNonNull(getRef(position).getKey()); // get database reference key of Recycler View item

                usersDatabaseReference.child(requestListId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String name = Objects.requireNonNull(snapshot.child(NodeNames.NAME).getValue()).toString();
                        holder.userNameTextView.setText(name);

                        StorageReference profileImage = FirebaseStorage.getInstance().getReference().child(Constants.IMAGESFOLDER + "/" + requestListId + ".jpg");
                        profileImage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                if(getContext()==null){
                                    return;
                                }
                                Glide.with(getContext()).load(uri).placeholder(R.drawable.profile).into(holder.userProfileImageView); // loading profile image
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                requestTypeDatabaseReference = getRef(position).child(NodeNames.REQUESTTYPE).getRef(); // getting request type information
                requestTypeDatabaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            String requestType = Objects.requireNonNull(snapshot.getValue()).toString();

                            if(requestType.equals(Constants.FRIENDREQUESTRECEIVED)){
                                holder.acceptRequestTextView.setVisibility(View.VISIBLE);
                                holder.acceptRequestTextView.setText("Accept");
                                holder.cancelRequestTextView.setVisibility(View.VISIBLE);
                                holder.cancelRequestTextView.setText("Decline");

                                // accepting friend request

                                holder.acceptRequestTextView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        contactsDatabaseReference.child(currentUserId).child(requestListId).child(NodeNames.CONTACTSTATUS).setValue(Constants.CONTACTSAVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    contactsDatabaseReference.child(requestListId).child(currentUserId).child(NodeNames.CONTACTSTATUS).setValue(Constants.CONTACTSAVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(task.isSuccessful()){
                                                                friendRequestDatabaseReference.child(currentUserId).child(requestListId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if(task.isSuccessful()){
                                                                            friendRequestDatabaseReference.child(requestListId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if(task.isSuccessful()){
                                                                                        Toast.makeText(getContext(),"Request Accepted",Toast.LENGTH_SHORT).show();
                                                                                        holder.acceptRequestTextView.setText("Friends");
                                                                                        holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                                        chatDatabaseReference.child(currentUserId).child(requestListId).child(NodeNames.TIMESTAMP).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if(task.isSuccessful()){
                                                                                                    chatDatabaseReference.child(requestListId).child(currentUserId).child(NodeNames.TIMESTAMP).setValue(ServerValue.TIMESTAMP);
                                                                                                }
                                                                                            }
                                                                                        });
                                                                                    }else {
                                                                                        Toast.makeText(getContext(),"Failed to Accept Request",Toast.LENGTH_SHORT).show();
                                                                                        holder.acceptRequestTextView.setVisibility(View.VISIBLE);
                                                                                        holder.acceptRequestTextView.setText("Accept");
                                                                                        holder.cancelRequestTextView.setText(View.VISIBLE);
                                                                                        holder.cancelRequestTextView.setText("Decline");
                                                                                    }
                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                });

                                // cancelling friend request

                                holder.cancelRequestTextView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        friendRequestDatabaseReference.child(currentUserId).child(requestListId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    friendRequestDatabaseReference.child(requestListId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(task.isSuccessful()){
                                                                Toast.makeText(getContext(),"Request Declined Successfully",Toast.LENGTH_SHORT).show();
                                                                holder.acceptRequestTextView.setText("Cancelled");
                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
                                                            }else {
                                                                Toast.makeText(getContext(),"Failed to Decline Request",Toast.LENGTH_SHORT).show();
                                                                holder.acceptRequestTextView.setVisibility(View.VISIBLE);
                                                                holder.acceptRequestTextView.setText("Accept");
                                                                holder.cancelRequestTextView.setVisibility(View.VISIBLE);
                                                                holder.cancelRequestTextView.setText("Decline");
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                });
                            }else {
                                holder.acceptRequestTextView.setVisibility(View.GONE);
                                holder.cancelRequestTextView.setVisibility(View.VISIBLE);
                                holder.cancelRequestTextView.setText("Cancel Sent Request");
                                holder.cancelRequestTextView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        friendRequestDatabaseReference.child(currentUserId).child(requestListId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    friendRequestDatabaseReference.child(requestListId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(task.isSuccessful()){
                                                                Toast.makeText(getContext(),"Request Cancelled Successfully",Toast.LENGTH_SHORT).show();
                                                                holder.cancelRequestTextView.setText("Cancelled");
                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
                                                            }else {
                                                                Toast.makeText(getContext(),"Failed to Decline Request",Toast.LENGTH_SHORT).show();
                                                                holder.acceptRequestTextView.setVisibility(View.GONE);
                                                                holder.cancelRequestTextView.setVisibility(View.VISIBLE);
                                                                holder.cancelRequestTextView.setText("Cancel Request");
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
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }

            @NonNull
            @Override
            public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.requests_layout,parent,false); // attaching user display layout to Recycler View
                return new RequestsViewHolder(view);
            }
        };
        requestsFragmentRecyclerView.setAdapter(firebaseRecyclerAdapter); // attaching adapter to Recycler View
        firebaseRecyclerAdapter.startListening(); // an event listener to monitor changes to the Firebase query
    }

    //  ViewHolder describes an item view and metadata about its place within the RecyclerView

    public static class RequestsViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView userProfileImageView;
        private TextView userNameTextView,acceptRequestTextView,cancelRequestTextView;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);

            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            acceptRequestTextView = itemView.findViewById(R.id.acceptRequestTextView);
            cancelRequestTextView = itemView.findViewById(R.id.cancelRequestTextView);
        }
    }
}