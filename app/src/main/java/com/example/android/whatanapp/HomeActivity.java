package com.example.android.whatanapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.android.whatanapp.ChatFiles.Chat;
import com.example.android.whatanapp.ChatFiles.ChatListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private Button logOutbtn, findUsersbtn;

    private RecyclerView chatList;
    private RecyclerView.Adapter chatListAdapter;
    private RecyclerView.LayoutManager chatListLayoutManager;

    ArrayList<Chat> chatArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        chatArrayList = new ArrayList<>();

        logOutbtn = findViewById(R.id.log_out);
        findUsersbtn = findViewById(R.id.find_users);

        logOutbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        findUsersbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), FindUsersActivity.class));
            }
        });

        getPermissions();
        initRecyclerView();
        getUserChatList();
    }

    private void getUserChatList(){
        DatabaseReference userChatDb = FirebaseDatabase.getInstance().getReference().child("user").child(FirebaseAuth.getInstance().getUid()).child("chat");

        userChatDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()){
                        Chat chat = new Chat(childSnapshot.getKey());
                        boolean exists = false;
                        for(Chat chatIter : chatArrayList){
                            if(chatIter.getChatId().equals(chat.getChatId()))
                                exists = true;
                        }
                        if(exists)
                            continue;

                        chatArrayList.add(chat);
                        chatListAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void initRecyclerView() {
        chatList = findViewById(R.id.chat_list);
        chatList.setNestedScrollingEnabled(false);
        chatList.setHasFixedSize(false);

        chatListLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        chatList.setLayoutManager(chatListLayoutManager);

        chatListAdapter = new ChatListAdapter(chatArrayList);
        chatList.setAdapter(chatListAdapter);
    }


    private void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS}, 1);
        }
    }
}
