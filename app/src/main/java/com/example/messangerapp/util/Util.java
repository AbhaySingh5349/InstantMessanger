package com.example.messangerapp.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.messangerapp.firebasetree.Constants;
import com.example.messangerapp.firebasetree.NodeNames;
import com.example.messangerapp.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Util {

    public static boolean checkInternetConnection(Context context){

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager!=null && connectivityManager.getActiveNetworkInfo()!=null){
            return connectivityManager.getActiveNetworkInfo().isAvailable();
        }else {
            return false;
        }
    }


   /* public static void updateDeviceToken(Context context,String token){

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        String currentUserId = Objects.requireNonNull(firebaseUser).getUid();

        DatabaseReference rootDatabaseReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference tokenDatabaseReference = rootDatabaseReference.child(NodeNames.TOKENS).child(currentUserId);

        HashMap<String,String > hashMap = new HashMap<>();
        hashMap.put(NodeNames.DEVICETOKEN,token);

        tokenDatabaseReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(context,"Failed to save device token",Toast.LENGTH_SHORT).show();
                }
            }
        });
    } */

    public static void sendNotification(Context context, String title, String message, String userId){
        DatabaseReference rootDatabaseReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference tokenDatabaseReference = rootDatabaseReference.child(NodeNames.TOKENS).child(userId);

        tokenDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String deviceToken = null;
                if(snapshot.child(NodeNames.DEVICETOKEN).getValue()!=null){
                    deviceToken = Objects.requireNonNull(snapshot.child(NodeNames.DEVICETOKEN).getValue()).toString();
                }
                JSONObject notificationJsonObject = new JSONObject();
                JSONObject notificationDataJsonObject = new JSONObject();

                try {
                    notificationDataJsonObject.put(Constants.NOTIFICATIONTITLE,title);
                    notificationDataJsonObject.put(Constants.NOTIFICATIONMESAGE,message);

                    notificationJsonObject.put(Constants.NOTIFICATIONTO,deviceToken);
                    notificationJsonObject.put(Constants.NOTIFICATIONDATA,notificationDataJsonObject);

                    String fcmApiUrl = "https://fcm.googleapis.com/fcm/send";
                    String contentType = "application/json";

                    Response.Listener successListener = new Response.Listener() {
                        @Override
                        public void onResponse(Object response) {
                            Toast.makeText(context,"Notification Sent",Toast.LENGTH_SHORT).show();
                        }
                    };

                    Response.ErrorListener failureListener = new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(context,"Failed to send Notification",Toast.LENGTH_SHORT).show();
                        }
                    };

                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(fcmApiUrl,notificationJsonObject,successListener,failureListener){
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {

                            HashMap<String,String> hashMap = new HashMap<>();
                            hashMap.put("Authorization","key=" + Constants.FIREBASECLOUDKEY);
                            hashMap.put("Sender","id=" + Constants.FIREBASESENDERID);
                            hashMap.put("Content-Type",contentType);

                            return super.getHeaders();
                        }
                    };

                    RequestQueue requestQueue = Volley.newRequestQueue(context);
                    requestQueue.add(jsonObjectRequest);

                }
                catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(context,"Failed to send Notification",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context,"Failed to send Notification",Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void updateChatDetails(Context context, String currentUserId, String chatUserId, String lastMessage){

        DatabaseReference rootDatabaseReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chatDatabaseReference = rootDatabaseReference.child(NodeNames.CHATS).child(chatUserId).child(currentUserId); // to update unread counts of chat user

        chatDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String currentUnreadCount = "0";

                if(dataSnapshot.child(NodeNames.UNREADCOUNT).getValue()!=null){
                    currentUnreadCount = Objects.requireNonNull(dataSnapshot.child(NodeNames.UNREADCOUNT).getValue()).toString();
                }

                HashMap chatHashMap = new HashMap();
                chatHashMap.put(NodeNames.TIMESTAMP, ServerValue.TIMESTAMP);
                chatHashMap.put(NodeNames.UNREADCOUNT,Integer.parseInt(currentUnreadCount) + 1);
                chatHashMap.put(NodeNames.LASTMESSAGE,lastMessage);
                chatHashMap.put(NodeNames.LASTMESSAGETIME,ServerValue.TIMESTAMP);

                HashMap chatUserMap = new HashMap();
                chatUserMap.put(NodeNames.CHATS + "/" + chatUserId + "/" + currentUserId, chatHashMap);

                rootDatabaseReference.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if(databaseError!=null){
                            Toast.makeText(context,"Something went wrong: " + databaseError.getMessage(),Toast.LENGTH_SHORT);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context,"Something went wrong: " + databaseError.getMessage(),Toast.LENGTH_SHORT);
            }
        });
    }
}
