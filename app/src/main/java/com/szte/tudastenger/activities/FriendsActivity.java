package com.szte.tudastenger.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.SearchView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.UserAdapter;
import com.szte.tudastenger.databinding.ActivityFriendsBinding;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;

public class FriendsActivity extends DrawerBaseActivity {
    private ActivityFriendsBinding activityFriendsBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseUser user;
    private FirebaseAuth mAuth;

    private RecyclerView mUserListRecyclerView;
    private ArrayList<User> mUsersData;
    private UserAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityFriendsBinding = ActivityFriendsBinding.inflate(getLayoutInflater());
        setContentView(activityFriendsBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if(user == null){
            finish();
        }

        mFirestore = FirebaseFirestore.getInstance();

        mUserListRecyclerView = findViewById(R.id.userListRecyclerView);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mUsersData = new ArrayList<>();
        mAdapter = new UserAdapter(this, mUsersData);
        mUserListRecyclerView.setAdapter(mAdapter);

        queryData();

        SearchView searchView = findViewById(R.id.searchByName);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mAdapter.getFilter().filter(s);
                return false;
            }
        });
    }

    private void queryData() {
        mFirestore.collection("Users").orderBy("username").limit(10).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                User user = document.toObject(User.class);
                mUsersData.add(user);
                mAdapter.notifyDataSetChanged();

            }
        });
    }
}