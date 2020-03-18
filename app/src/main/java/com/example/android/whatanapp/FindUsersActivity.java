package com.example.android.whatanapp;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.whatanapp.UserFiles.User;
import com.example.android.whatanapp.UserFiles.UserListAdapter;
import com.example.android.whatanapp.Utils.CountryToPhonePrefix;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FindUsersActivity extends AppCompatActivity {

    private RecyclerView userList;
    private RecyclerView.Adapter userListAdapter;
    private RecyclerView.LayoutManager userListLayoutManager;

    ArrayList<User> userArrayList, contactsArrayList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_findusers);

        userArrayList = new ArrayList<>();
        contactsArrayList = new ArrayList<>();


        initRecyclerView();
        getContactList();
    }

    private void getContactList(){
        String isoPrefix = getCountryISO();
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones.moveToNext()){
                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                phone = phone.replace(" ", "");
                phone = phone.replace("-", "");
                phone = phone.replace("()", "");
                phone = phone.replace(")", "");

                if(!String.valueOf(phone.charAt(0)).equals("+")){
                    phone = isoPrefix + phone;
                }

                User contact = new User(name, phone, "");
                contactsArrayList.add(contact);
                getUserDetails(contact);
        }
    }

    private void getUserDetails(User contact) {
        DatabaseReference userDb = FirebaseDatabase.getInstance().getReference().child("user");
        Query query = userDb.orderByChild("phone").equalTo(contact.getPhone());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String phone = "", name = "";
                    for(DataSnapshot childSnapshots : dataSnapshot.getChildren()){
                        if(childSnapshots.child("phone").getValue() != null){
                            phone = childSnapshots.child("phone").getValue().toString();
                        }
                        if(childSnapshots.child("name").getValue() != null){
                            name = childSnapshots.child("name").getValue().toString();
                        }

                        User user = new User(name, phone, childSnapshots.getKey());

                        if(name.equals(phone)){
                            for(User contactIter : contactsArrayList){

                                Log.v("checkingPhone", contactIter.getPhone() + "-*-*-*-*-" + user.getPhone());
                                if(contactIter.getPhone().equals(user.getPhone())){
                                    Log.v("settingName", contactIter.getName() + "-*-*-*-*-");
                                    user.setName(contactIter.getName());
                                }
                            }
                        }

                        userArrayList.add(user);
                        userListAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getCountryISO(){
        String iso = null;

        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
        if(telephonyManager.getNetworkCountryIso() != null){
            if(!telephonyManager.getNetworkCountryIso().toString().equals("")){
                iso = telephonyManager.getNetworkCountryIso().toString();
            }
        }
        return CountryToPhonePrefix.getPhone(iso);
    }

    private void initRecyclerView() {
        userList = findViewById(R.id.user_list);
        userList.setNestedScrollingEnabled(false);
        userList.setHasFixedSize(false);

        userListLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        userList.setLayoutManager(userListLayoutManager);

        userListAdapter = new UserListAdapter(userArrayList);
        userList.setAdapter(userListAdapter);
    }
}
