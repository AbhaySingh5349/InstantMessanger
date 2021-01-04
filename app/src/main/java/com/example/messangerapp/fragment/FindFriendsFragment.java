package com.example.messangerapp.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messangerapp.R;
import com.example.messangerapp.firebasetree.Constants;
import com.example.messangerapp.profile.ProfileActivity;
import com.example.messangerapp.util.Util;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.model.FindFriendsModelClass;
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

import butterknife.internal.ListenerClass;
import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsFragment extends Fragment {

    /* searching users on this app and allowing user to send , accept, decline or cancel requests */

    private EditText searchFriendsEditText;
    private RecyclerView findFriendsFragmentRecyclerView;
    private View findFriendsFragmentProgressBar;

    private DatabaseReference userDatabaseReference, friendRequestDatabaseReference, contactsDatabaseReference, chatDatabaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    String search, currentUserId;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FindFriendsFragment() {
        // Required empty public constructor
    }

    public static FindFriendsFragment newInstance(String param1, String param2) {
        FindFriendsFragment fragment = new FindFriendsFragment();
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
        View view =  inflater.inflate(R.layout.fragment_find_friends, container, false);

        searchFriendsEditText = view.findViewById(R.id.searchFriendsEditText);
        findFriendsFragmentRecyclerView = view.findViewById(R.id.findFriendsFragmentRecyclerView);
        findFriendsFragmentProgressBar = view.findViewById(R.id.findFriendsFragmentProgressBar);

        findFriendsFragmentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        findFriendsFragmentRecyclerView.setHasFixedSize(true);

        // getting current user

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        currentUserId = Objects.requireNonNull(firebaseUser).getUid();

        // getting reference to database nodes

        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        friendRequestDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIENDREQUESTS);
        contactsDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.CONTACTS);
        chatDatabaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);

        // search users on basis of user name

        searchFriendsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // listener could be added to the EditText to execute an action whenever the text is changed in the EditText View
                search = charSequence.toString();
                onStart();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // FirebaseRecyclerOptions provided by the FirebaseUI to make a query in the database to fetch appropriate data

        FirebaseRecyclerOptions<FindFriendsModelClass> findFriendsFirebaseRecyclerOptions = null;

        if(search == null){
            findFriendsFirebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<FindFriendsModelClass>().setQuery(userDatabaseReference.orderByChild(NodeNames.NAME),FindFriendsModelClass.class).build();
        }else {
            findFriendsFirebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<FindFriendsModelClass>().setQuery(userDatabaseReference.orderByChild(NodeNames.NAME).startAt(search).endAt(search + "\uf8ff"),FindFriendsModelClass.class).build();
        }

        // FirebaseRecyclerAdapter binds a Query to a RecyclerView and responds to all real-time events included items being added, removed, moved, or changed

        FirebaseRecyclerAdapter<FindFriendsModelClass,FindFriendsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<FindFriendsModelClass, FindFriendsViewHolder>(findFriendsFirebaseRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder, int position, @NonNull FindFriendsModelClass model) {
                holder.userNameTextView.setText(model.getName());

                String requestReceiverUserId = getRef(position).getKey(); // get database reference key of Recycler View item

                StorageReference profileImage = FirebaseStorage.getInstance().getReference().child(Constants.IMAGESFOLDER + "/" + requestReceiverUserId + ".jpg");
                profileImage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(Objects.requireNonNull(getContext())).load(uri).placeholder(R.drawable.profile).into(holder.userProfileImageView); // loading profile image
                    }
                });

                if(Objects.requireNonNull(requestReceiverUserId).equals(currentUserId)){
                    holder.addFriendTextView.setVisibility(View.VISIBLE);
                    holder.cancelRequestTextView.setVisibility(View.GONE);
                    holder.addFriendTextView.setText("Your Profile");

                    holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(getContext(), ProfileActivity.class));
                        }
                    });
                }else {
                    friendRequestDatabaseReference.child(currentUserId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                if(snapshot.hasChild(requestReceiverUserId)){
                                    // request is sent or received by current user
                                    String requestType = Objects.requireNonNull(snapshot.child(requestReceiverUserId).child(NodeNames.REQUESTTYPE).getValue()).toString();

                                    if(requestType.equals(Constants.FRIENDREQUESTSENT)){
                                        holder.addFriendTextView.setVisibility(View.VISIBLE);
                                        holder.cancelRequestTextView.setVisibility(View.GONE);
                                        holder.addFriendTextView.setText("Cancel Request");

                                        // removing node from friendRequestDatabaseReference

                                        holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                friendRequestDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()){
                                                            friendRequestDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        Toast.makeText(getContext(),"Request Cancelled Successfully",Toast.LENGTH_SHORT).show();
                                                                        holder.addFriendTextView.setText("+Friend");
                                                                    }else {
                                                                        Toast.makeText(getContext(),"Failed to Cancel Request",Toast.LENGTH_SHORT).show();
                                                                        holder.addFriendTextView.setText("Cancel Request");
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    }else {

                                        // removing node from friendRequestDatabaseReference and adding node to contactsDatabaseReference

                                        holder.addFriendTextView.setVisibility(View.VISIBLE);
                                        holder.addFriendTextView.setText("Accept");
                                        holder.cancelRequestTextView.setVisibility(View.VISIBLE);
                                        holder.cancelRequestTextView.setText("Decline");

                                        holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                contactsDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.CONTACTSTATUS).setValue(Constants.CONTACTSAVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()){
                                                            contactsDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.CONTACTSTATUS).setValue(Constants.CONTACTSAVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        friendRequestDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if(task.isSuccessful()){
                                                                                    friendRequestDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                            if(task.isSuccessful()){
                                                                                                Toast.makeText(getContext(),"Request Accepted",Toast.LENGTH_SHORT).show();
                                                                                                holder.addFriendTextView.setText("Unfriend");
                                                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                                                chatDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.TIMESTAMP).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                        if(task.isSuccessful()){
                                                                                                            chatDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.TIMESTAMP).setValue(ServerValue.TIMESTAMP);
                                                                                                        }
                                                                                                    }
                                                                                                });
                                                                                            }else {
                                                                                                Toast.makeText(getContext(),"Failed to Accept Request",Toast.LENGTH_SHORT).show();
                                                                                                holder.addFriendTextView.setVisibility(View.VISIBLE);
                                                                                                holder.addFriendTextView.setText("Accept");
                                                                                                holder.cancelRequestTextView.setVisibility(View.VISIBLE);
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

                                        // removing node from friendRequestDatabaseReference

                                        holder.cancelRequestTextView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                friendRequestDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()){
                                                            friendRequestDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        Toast.makeText(getContext(),"Request Declined Successfully",Toast.LENGTH_SHORT).show();
                                                                        holder.addFriendTextView.setText("+Friend");
                                                                        holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                    }else {
                                                                        Toast.makeText(getContext(),"Failed to Decline Request",Toast.LENGTH_SHORT).show();
                                                                        holder.addFriendTextView.setVisibility(View.VISIBLE);
                                                                        holder.addFriendTextView.setText("Accept");
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
                                    }
                                }else {
                                    contactsDatabaseReference.child(currentUserId).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if(snapshot.exists()){
                                                if(snapshot.hasChild(requestReceiverUserId)){
                                                    holder.addFriendTextView.setText("Unfriend");
                                                    holder.cancelRequestTextView.setVisibility(View.GONE);

                                                    // removing friend from contactsDatabaseReference

                                                    holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            contactsDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.CONTACTSTATUS).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        contactsDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.CONTACTSTATUS).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if(task.isSuccessful()){
                                                                                    Toast.makeText(getContext(),"Unfriend Successfully",Toast.LENGTH_SHORT).show();
                                                                                    holder.addFriendTextView.setText("+Friend");
                                                                                    holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                                }else {
                                                                                    Toast.makeText(getContext(),"Failed to remove friend",Toast.LENGTH_SHORT).show();
                                                                                    holder.addFriendTextView.setText("Unfriend");
                                                                                    holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                }else {
                                                    holder.addFriendTextView.setText("+Friend");
                                                    holder.cancelRequestTextView.setVisibility(View.GONE);

                                                    // sending friend request and adding node to friendRequestDatabaseReference

                                                    holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            friendRequestDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.REQUESTTYPE).setValue(Constants.FRIENDREQUESTSENT).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        friendRequestDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(Constants.FRIENDREQUESTRECEIVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if(task.isSuccessful()){
                                                                                    Toast.makeText(getContext(),"Request Sent Successfully",Toast.LENGTH_SHORT).show();
                                                                                    holder.addFriendTextView.setVisibility(View.VISIBLE);
                                                                                    holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                                    holder.addFriendTextView.setText("Cancel Request");
                                                                                }else {
                                                                                    Toast.makeText(getContext(),"Failed to send request",Toast.LENGTH_SHORT).show();
                                                                                    holder.addFriendTextView.setText("+Friend");
                                                                                    holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            }else {
                                                holder.addFriendTextView.setText("+Friend");
                                                holder.cancelRequestTextView.setVisibility(View.GONE);

                                                holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        friendRequestDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.REQUESTTYPE).setValue(Constants.FRIENDREQUESTSENT).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    friendRequestDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(Constants.FRIENDREQUESTRECEIVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()){
                                                                                Toast.makeText(getContext(),"Request Sent Successfully",Toast.LENGTH_SHORT).show();
                                                                                holder.addFriendTextView.setVisibility(View.VISIBLE);
                                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                                holder.addFriendTextView.setText("Cancel Request");
                                                                            }else {
                                                                                Toast.makeText(getContext(),"Failed to send request",Toast.LENGTH_SHORT).show();
                                                                                holder.addFriendTextView.setText("+Friend");
                                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
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

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }else {
                                contactsDatabaseReference.child(currentUserId).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if(snapshot.exists()){
                                            if(snapshot.hasChild(requestReceiverUserId)){
                                                holder.addFriendTextView.setText("Unfriend");
                                                holder.cancelRequestTextView.setVisibility(View.GONE);

                                                // removing contact from contactsDatabaseReference

                                                holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        contactsDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.CONTACTSTATUS).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    contactsDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.CONTACTSTATUS).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()){
                                                                                Toast.makeText(getContext(),"Unfriend Successfully",Toast.LENGTH_SHORT).show();
                                                                                holder.addFriendTextView.setText("+Friend");
                                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                            }else {
                                                                                Toast.makeText(getContext(),"Failed to remove friend",Toast.LENGTH_SHORT).show();
                                                                                holder.addFriendTextView.setText("Unfriend");
                                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        });
                                                    }
                                                });
                                            }else {
                                                holder.addFriendTextView.setText("+Friend");
                                                holder.cancelRequestTextView.setVisibility(View.GONE);

                                                // sending friend request and adding node to friendRequestDatabaseReference

                                                holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        friendRequestDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.REQUESTTYPE).setValue(Constants.FRIENDREQUESTSENT).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    friendRequestDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(Constants.FRIENDREQUESTRECEIVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()){
                                                                                Toast.makeText(getContext(),"Request Sent Successfully",Toast.LENGTH_SHORT).show();
                                                                                holder.addFriendTextView.setVisibility(View.VISIBLE);
                                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                                holder.addFriendTextView.setText("Cancel Request");
                                                                            }else {
                                                                                Toast.makeText(getContext(),"Failed to send request",Toast.LENGTH_SHORT).show();
                                                                                holder.addFriendTextView.setText("+Friend");
                                                                                holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        }else {
                                            holder.addFriendTextView.setText("+Friend");
                                            holder.cancelRequestTextView.setVisibility(View.GONE);

                                            // sending friend request and adding node to friendRequestDatabaseReference

                                            holder.addFriendTextView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    friendRequestDatabaseReference.child(currentUserId).child(requestReceiverUserId).child(NodeNames.REQUESTTYPE).setValue(Constants.FRIENDREQUESTSENT).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(task.isSuccessful()){
                                                                friendRequestDatabaseReference.child(requestReceiverUserId).child(currentUserId).child(NodeNames.REQUESTTYPE).setValue(Constants.FRIENDREQUESTRECEIVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if(task.isSuccessful()){
                                                                            Toast.makeText(getContext(),"Request Sent Successfully",Toast.LENGTH_SHORT).show();
                                                                            holder.addFriendTextView.setVisibility(View.VISIBLE);
                                                                            holder.cancelRequestTextView.setVisibility(View.GONE);
                                                                            holder.addFriendTextView.setText("Cancel Request");
                                                                        }else {
                                                                            Toast.makeText(getContext(),"Failed to send request",Toast.LENGTH_SHORT).show();
                                                                            holder.addFriendTextView.setText("+Friend");
                                                                            holder.cancelRequestTextView.setVisibility(View.GONE);
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
            }

            @NonNull
            @Override
            public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.find_friends_layout,parent,false); // attaching user display layout to Recycler View
                return new FindFriendsViewHolder(view);
            }
        };
        findFriendsFragmentRecyclerView.setAdapter(firebaseRecyclerAdapter); // attaching adapter to Recycler View
        firebaseRecyclerAdapter.startListening(); // an event listener to monitor changes to the Firebase query
    }

    //  ViewHolder describes an item view and metadata about its place within the RecyclerView

    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView userProfileImageView;
        private TextView userNameTextView, addFriendTextView, cancelRequestTextView;

        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            addFriendTextView = itemView.findViewById(R.id.addFriendTextView);
            cancelRequestTextView = itemView.findViewById(R.id.cancelRequestTextView);
        }
    }
}