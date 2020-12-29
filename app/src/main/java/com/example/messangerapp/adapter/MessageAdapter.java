package com.example.messangerapp.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messangerapp.ChatActivity;
import com.example.messangerapp.R;
import com.example.messangerapp.SelectFriendActivity;
import com.example.messangerapp.firebasetree.Constants;
import com.example.messangerapp.model.MessageModelClass;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<MessageModelClass> messageModelClassList;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private ActionMode actionMode; // activate action on a message (delete, share)
    private ConstraintLayout selectedConstraintLayout;

    public MessageAdapter(Context context, List<MessageModelClass> messageModelClassList) {
        this.context = context;
        this.messageModelClassList = messageModelClassList;
    }

    @NonNull
    @Override
    public MessageAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_layout,parent,false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.MessageViewHolder holder, int position) {

        MessageModelClass messageModelClass = messageModelClassList.get(position);

        // getting current user

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        String currentUserId = Objects.requireNonNull(firebaseUser).getUid();

        String senderUserId = messageModelClass.getMessageSenderId(); // message sender id

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

        String dateTime = simpleDateFormat.format(new Date(messageModelClass.getMessageTime())); // retrieve date from TimeStamp
        String[] splitString = dateTime.split(" "); // since date n time are separated by " "

        String messageDate = splitString[0];
        String messageTime = splitString[1];

        if(currentUserId.equals(senderUserId)){
            // message is sent by user

            holder.receivedMessagesLinearLayout.setVisibility(View.GONE);
            holder.receivedImageLinearLayout.setVisibility(View.GONE);

            if(messageModelClass.getMessageType().equals(Constants.TEXTMESSAGE)){
                holder.sentMessagesLinearLayout.setVisibility(View.VISIBLE);
                holder.sentImageLinearLayout.setVisibility(View.GONE);
                holder.sentMessageTextView.setText(messageModelClass.getMessageValue());
                holder.sentMessageTimeTextView.setText(messageTime);
            }else {
                holder.sentMessagesLinearLayout.setVisibility(View.GONE);
                holder.sentImageLinearLayout.setVisibility(View.VISIBLE);
                holder.sentImageTimeTextView.setText(messageTime);
                Glide.with(context).load(messageModelClass.getMessageValue()).placeholder(R.drawable.chat_app_icon).into(holder.sentImageView);
            }

        }else {
            // message is received
            holder.sentMessagesLinearLayout.setVisibility(View.GONE);
            holder.sentImageLinearLayout.setVisibility(View.GONE);

            if(messageModelClass.getMessageType().equals(Constants.TEXTMESSAGE)){
                holder.receivedMessagesLinearLayout.setVisibility(View.VISIBLE);
                holder.receivedImageLinearLayout.setVisibility(View.GONE);
                holder.receivedMessageTextView.setText(messageModelClass.getMessageValue());
                holder.receivedMessageTimeTextView.setText(messageTime);
            }else {
                holder.receivedMessagesLinearLayout.setVisibility(View.GONE);
                holder.receivedImageLinearLayout.setVisibility(View.VISIBLE);
                holder.receivedImageTimeTextView.setText(messageTime);

                if(messageModelClass.getMessageType().equals(Constants.IMAGEMESSAGE)){
                    StorageReference image = FirebaseStorage.getInstance().getReference().child(Constants.IMAGEMESSAGEFOLDER + "/" + messageModelClass.getMessageId() + ".jpg");
                    image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Glide.with(context).load(uri).placeholder(R.drawable.chat_app_icon).into(holder.receivedImageView);
                            holder.receivedImageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                                    intent.setDataAndType(uri,"image/jpg");
                                    context.startActivity(intent);
                                }
                            });
                        }
                    });
                }else {
                    StorageReference video = FirebaseStorage.getInstance().getReference().child(Constants.VIDEOMESSAGEFOLDER + "/" + messageModelClass.getMessageId() + ".mp4");
                    video.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Glide.with(context).load(uri).placeholder(R.drawable.chat_app_icon).into(holder.receivedImageView);
                            holder.receivedImageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                                    intent.setDataAndType(uri,"video/mp4");
                                    context.startActivity(intent);
                                }
                            });
                        }
                    });
                }
            }
        }

        holder.messageConstraintLayout.setTag(R.id.Tag_Message_Value,messageModelClass.getMessageValue());
        holder.messageConstraintLayout.setTag(R.id.Tag_Message_Id,messageModelClass.getMessageId());
        holder.messageConstraintLayout.setTag(R.id.Tag_Message_Type,messageModelClass.getMessageType());

        holder.messageConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageType = view.getTag(R.id.Tag_Message_Type).toString();
                Uri uri = Uri.parse(view.getTag(R.id.Tag_Message_Value).toString());

                if(messageType.equals(Constants.VIDEOMESSAGE)){
                    Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                    intent.setDataAndType(uri,"video/mp4");
                    context.startActivity(intent);
                }else if(messageType.equals(Constants.IMAGEMESSAGE)){
                    Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                    intent.setDataAndType(uri,"image/jpg");
                    context.startActivity(intent);
                }
            }
        });

        holder.messageConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(actionMode!=null){
                    return false;
                }

                selectedConstraintLayout = holder.messageConstraintLayout;

                actionMode = ((AppCompatActivity)context).startSupportActionMode(actionModeCallBack);
                holder.messageConstraintLayout.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));

                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageModelClassList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder{

        private ConstraintLayout messageConstraintLayout;
        private LinearLayout sentMessagesLinearLayout, receivedMessagesLinearLayout, sentImageLinearLayout, receivedImageLinearLayout;
        private TextView sentMessageTextView, sentMessageTimeTextView, receivedMessageTextView, receivedMessageTimeTextView, sentImageTimeTextView, receivedImageTimeTextView;
        private ImageView sentImageView, receivedImageView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            messageConstraintLayout = itemView.findViewById(R.id.messageConstraintLayout);
            sentMessagesLinearLayout = itemView.findViewById(R.id.sentMessagesLinearLayout);
            receivedMessagesLinearLayout = itemView.findViewById(R.id.receivedMessagesLinearLayout);
            sentImageLinearLayout = itemView.findViewById(R.id.sentImageLinearLayout);
            receivedImageLinearLayout = itemView.findViewById(R.id.receivedImageLinearLayout);
            sentMessageTextView = itemView.findViewById(R.id.sentMessageTextView);
            sentMessageTimeTextView = itemView.findViewById(R.id.sentMessageTimeTextView);
            receivedMessageTextView = itemView.findViewById(R.id.receivedMessageTextView);
            receivedMessageTimeTextView = itemView.findViewById(R.id.receivedMessageTimeTextView);
            sentImageTimeTextView = itemView.findViewById(R.id.sentImageTimeTextView);
            receivedImageTimeTextView = itemView.findViewById(R.id.receivedImageTimeTextView);
            sentImageView = itemView.findViewById(R.id.sentImageView);
            receivedImageView = itemView.findViewById(R.id.receivedImageView);
        }
    }

    // contextual action bar

    public ActionMode.Callback actionModeCallBack = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater menuInflater = actionMode.getMenuInflater();
            menuInflater.inflate(R.menu.chat_menu,menu);

            String selectedMessageType = String.valueOf(selectedConstraintLayout.getTag(R.id.Tag_Message_Type));

            if(selectedMessageType.equals(Constants.TEXTMESSAGE)){
                MenuItem downLoadItem = menu.findItem(R.id.downloadChatMenu);
                downLoadItem.setVisible(false);
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

            String selectedMessageId = String.valueOf(selectedConstraintLayout.getTag(R.id.Tag_Message_Id));
            String selectedMessageType = String.valueOf(selectedConstraintLayout.getTag(R.id.Tag_Message_Type));
            String selectedMessageValue = String.valueOf(selectedConstraintLayout.getTag(R.id.Tag_Message_Value));

            int itemId = menuItem.getItemId();

            switch (itemId){

            /*    case R.id.forwardChatMenu:
                    if(context instanceof ChatActivity){
                        ((ChatActivity)context).forwardMessage(selectedMessageId,selectedMessageValue,selectedMessageType);
                    }
                    Toast.makeText(context,"Forward Message",Toast.LENGTH_SHORT).show();
                    actionMode.finish();
                    break; */

                case R.id.deleteChatMenu:
                    if(context instanceof ChatActivity){
                        ((ChatActivity)context).deleteMessage(selectedMessageId,selectedMessageType);
                    }
                    Toast.makeText(context,"Delete Message",Toast.LENGTH_SHORT).show();
                    actionMode.finish();
                    break;

                case R.id.shareChatMenu:
                    if(selectedMessageType.equals(Constants.TEXTMESSAGE)){
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_TEXT,selectedMessageValue);
                        intent.setType("text/plain");
                        context.startActivity(intent);
                    }else {
                        if(context instanceof ChatActivity){
                            ((ChatActivity)context).downloadMessageFile(selectedMessageId,selectedMessageType,true);
                        }
                    }
                    Toast.makeText(context,"Share Message",Toast.LENGTH_SHORT).show();
                    actionMode.finish();
                    break;

                case R.id.downloadChatMenu:
                    if(context instanceof ChatActivity){
                        ((ChatActivity)context).downloadMessageFile(selectedMessageId,selectedMessageType,false);
                    }
                    Toast.makeText(context,"Download",Toast.LENGTH_SHORT).show();
                    actionMode.finish();
                    break;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionMode = null;
            selectedConstraintLayout.setBackgroundColor(context.getResources().getColor(R.color.chatBackground)); // default color of layout after deleting a file
        }
    };

}
