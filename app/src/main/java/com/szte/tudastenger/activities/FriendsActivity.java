package com.szte.tudastenger.activities;

import android.os.Bundle;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.interfaces.OnFriendAdded;
import com.szte.tudastenger.interfaces.OnFriendRequestRemoved;
import com.szte.tudastenger.interfaces.OnFriendRequestAdded;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.FriendAdapter;
import com.szte.tudastenger.adapters.FriendRequestAdapter;
import com.szte.tudastenger.adapters.UserAdapter;
import com.szte.tudastenger.databinding.ActivityFriendsBinding;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class FriendsActivity extends DrawerBaseActivity implements OnFriendRequestAdded, OnFriendAdded, OnFriendRequestRemoved {
    private ActivityFriendsBinding activityFriendsBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseUser user;
    private User currentUser;
    private CollectionReference mUsers;
    private FirebaseAuth mAuth;

    private RecyclerView mUserListRecyclerView;
    private RecyclerView mFriendListRecyclerView;
    private RecyclerView mFriendRequestListRecyclerView;
    private ArrayList<User> mUsersData;
    private ArrayList<User> mFriendsData;
    private ArrayList<User> mFriendRequestsData;
    private UserAdapter mUserAdapter;
    private FriendAdapter mFriendAdapter;
    private FriendRequestAdapter mFriendRequestAdapter;

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
        mUsers = mFirestore.collection("Users");

        mUserListRecyclerView = findViewById(R.id.userListRecyclerView);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mUsersData = new ArrayList<>();

        mFriendListRecyclerView = findViewById(R.id.friendListRecyclerView);
        mFriendListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFriendsData = new ArrayList<>();
        mFriendAdapter = new FriendAdapter(this, mFriendsData);
        mFriendListRecyclerView.setAdapter(mFriendAdapter);

        mFriendRequestListRecyclerView = findViewById(R.id.friendRequestListRecyclerView);
        mFriendRequestListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFriendRequestsData = new ArrayList<>();


        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);

                mUserAdapter = new UserAdapter(this, mUsersData, currentUser.getId(), this);
                mUserListRecyclerView.setAdapter(mUserAdapter);

                mFriendRequestAdapter = new FriendRequestAdapter(this, mFriendRequestsData, currentUser.getId(), this::onFriendAdded, this::onFriendRequestRemoved);
                mFriendRequestListRecyclerView.setAdapter(mFriendRequestAdapter);

                queryData();
            }
        });


        SearchView searchView = findViewById(R.id.searchByName);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mUserAdapter.getFilter().filter(s);
                return false;
            }
        });
    }

    private void queryData() {
        mFirestore.collection("Users").orderBy("username").limit(10).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                User user = document.toObject(User.class);
                if(!Objects.equals(user.getId(), currentUser.getId())) {
                    mUsersData.add(user);
                    mUserAdapter.notifyDataSetChanged();
                }
            }
        });

        mFirestore.collection("Friends").document(currentUser.getId()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Map<String, Object> friends = document.getData();
                                for (Map.Entry<String, Object> entry : friends.entrySet()) {
                                    if ((boolean) entry.getValue()) { // Ha a barát státusza true
                                        String friendId = entry.getKey();

                                        mFirestore.collection("Users").document(friendId).get()
                                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                        if (documentSnapshot.exists()) {
                                                            User friend = documentSnapshot.toObject(User.class);
                                                            mFriendsData.add(friend);
                                                            mFriendAdapter.notifyDataSetChanged();
                                                        }
                                                    }
                                                });
                                    }
                                }
                            }
                        }
                    }
                });

        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", currentUser.getId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String user_uid2 = document.getString("user_uid2");

                                mFirestore.collection("Users").document(user_uid2).get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                if (documentSnapshot.exists()) {
                                                    User friend = documentSnapshot.toObject(User.class);
                                                    mFriendRequestsData.add(friend);
                                                    mFriendRequestAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });

        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid2", currentUser.getId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String user_uid1 = document.getString("user_uid1");

                                mFirestore.collection("Users").document(user_uid1).get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                if (documentSnapshot.exists()) {
                                                    User friend = documentSnapshot.toObject(User.class);
                                                    mFriendRequestsData.add(friend);
                                                    mFriendRequestAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });

    }

    @Override
    public void onFriendRequestAdded(User user) {
        mFriendRequestsData.add(user);
        mFriendRequestAdapter.notifyItemInserted(mFriendRequestsData.size() - 1);
    }

    @Override
    public void onFriendAdded(User user) {
        mFriendsData.add(user);
        mFriendAdapter.notifyItemInserted(mFriendsData.size() - 1);
    }

    @Override
    public void onFriendRequestRemoved(User user) {
        mFriendRequestsData.remove(user);
        mFriendRequestAdapter.notifyItemRemoved(mFriendRequestsData.size() - 1);
    }
}