package com.example.android.whatanapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.android.whatanapp.ChatFiles.Chat;
import com.example.android.whatanapp.ChatFiles.MediaAdapter;
import com.example.android.whatanapp.ChatFiles.MessageAdapter;
import com.example.android.whatanapp.ChatFiles.MessageObject;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chat, media;
    private RecyclerView.Adapter chatAdapter, mediaAdapter;
    private RecyclerView.LayoutManager chatLayoutManager, mediaLayoutManager;

    ArrayList<MessageObject> messageList;

    String chatId;
    DatabaseReference chatDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Button sendBtn = findViewById(R.id.send_btn);
        Button mediaBtn = findViewById(R.id.media_btn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        mediaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        chatId = getIntent().getExtras().getString("ChatID");
        chatDb = FirebaseDatabase.getInstance().getReference().child("chat").child(chatId);

        initMessage();
        initMedia();
        getChatMessaages();
    }

    private void getChatMessaages() {
        chatDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(dataSnapshot.exists()){
                    String text = "", creatorId = "";
                    if(dataSnapshot.child("text").getValue() != null){
                        text = dataSnapshot.child("text").getValue().toString();
                    }
                    if(dataSnapshot.child("creator").getValue() != null){
                        creatorId = dataSnapshot.child("creator").getValue().toString();
                    }

                    MessageObject message = new MessageObject(dataSnapshot.getKey(), text, creatorId);
                    messageList.add(message);
                    chatLayoutManager.scrollToPosition(messageList.size() - 1);
                    chatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    int mediaUploadeditr = 0;
    ArrayList<String> mediaIdList = new ArrayList<>();
    EditText messageet;

    private void sendMessage(){
        messageet = findViewById(R.id.message_et);

        String messageId = chatDb.push().getKey();
        final DatabaseReference newMessageDb = chatDb.child(messageId);
        final Map newMessageMap = new HashMap<>();


        newMessageMap.put("creator", FirebaseAuth.getInstance().getUid());

        if(!messageet.getText().toString().isEmpty()) {
            newMessageMap.put("text", messageet.getText().toString());
        }

        if(!mediaUriList.isEmpty()){
            for(String mediaUri : mediaUriList){
                String mediaId = newMessageDb.child("media").push().getKey();
                mediaIdList.add(mediaId);
                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("chat").child(messageId).child(mediaId);

                UploadTask uploadTask = filePath.putFile(Uri.parse(mediaUri));
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                newMessageMap.put("/media/" + mediaIdList.get(mediaUploadeditr) + "/", uri.toString());

                                mediaUploadeditr++;
                                if(mediaUploadeditr == mediaUriList.size()){
                                    updateDatabaseWithNewMessage(newMessageDb, newMessageMap);
                                }
                            }
                        });
                    }
                });
            }
        } else{

            if(!messageet.getText().toString().isEmpty()) {
                updateDatabaseWithNewMessage(newMessageDb, newMessageMap);
            }
        }

    }

    private void updateDatabaseWithNewMessage(DatabaseReference newMessageDb, Map newMessageMap){
        newMessageDb.updateChildren(newMessageMap);
        messageet.setText(null);
        mediaUriList.clear();
        mediaIdList.clear();
        mediaAdapter.notifyDataSetChanged();
    }

    private void initMessage() {
        messageList = new ArrayList<>();
        chat = findViewById(R.id.message_rv);
        chat.setNestedScrollingEnabled(false);
        chat.setHasFixedSize(false);

        chatLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        chat.setLayoutManager(chatLayoutManager);

        chatAdapter = new MessageAdapter(messageList);
        chat.setAdapter(chatAdapter);
    }

    int PICKED_IMAGE_INTENT = 1;
    ArrayList<String> mediaUriList = new ArrayList<>();

    private void initMedia() {
        media = findViewById(R.id.media_rv);
        media.setNestedScrollingEnabled(false);
        media.setHasFixedSize(false);

        mediaLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        media.setLayoutManager(mediaLayoutManager);

        mediaAdapter = new MediaAdapter(getApplicationContext(), mediaUriList);
        media.setAdapter(mediaAdapter);
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture(s)"), PICKED_IMAGE_INTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == PICKED_IMAGE_INTENT){
                if(data.getClipData() == null)
                    mediaUriList.add(data.getData().toString());
                else{
                    for(int i = 0; i < data.getClipData().getItemCount(); i++){
                        mediaUriList.add(data.getClipData().getItemAt(i).getUri().toString());
                    }
                }
                mediaAdapter.notifyDataSetChanged();
            }
        }
    }
}
