package com.szte.tudastenger.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SearchView;
import android.widget.TextView;

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
import com.szte.tudastenger.interfaces.OnFriendRemoved;
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

public class FriendsActivity extends DrawerBaseActivity implements OnFriendRequestAdded, OnFriendAdded, OnFriendRequestRemoved, OnFriendRemoved {
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
    private Button popUpShowUsersButton;
    private TextView mNoFriendRequestsTextView;
    private TextView mNoFriendsYetTextView;
    private boolean userHasFriend;
    private boolean userHasFriendRequest;

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

        mNoFriendsYetTextView = findViewById(R.id.noFriendsYetTextView);
        mNoFriendRequestsTextView = findViewById(R.id.noFriendRequestsTextView);

        mFriendListRecyclerView = findViewById(R.id.friendListRecyclerView);
        mFriendListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFriendsData = new ArrayList<>();

        mFriendRequestListRecyclerView = findViewById(R.id.friendRequestListRecyclerView);
        mFriendRequestListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFriendRequestsData = new ArrayList<>();

        popUpShowUsersButton = findViewById(R.id.popUpShowUsersButton);

        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);

                mFriendRequestAdapter = new FriendRequestAdapter(this, mFriendRequestsData, currentUser.getId(), this::onFriendAdded, this::onFriendRequestRemoved);
                mFriendRequestListRecyclerView.setAdapter(mFriendRequestAdapter);

                mFriendAdapter = new FriendAdapter(this, mFriendsData, currentUser.getId(), this::onFriendRemoved);
                mFriendListRecyclerView.setAdapter(mFriendAdapter);

                queryData();
            }
        });



    }

    private void queryData() {
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

                                                            // Frissítés után az üzenet elrejtése
                                                            mNoFriendsYetTextView.setVisibility(View.GONE);
                                                        }
                                                    }
                                                });
                                    }
                                }
                            }

                            if (mFriendsData.isEmpty()) {
                                mNoFriendsYetTextView.setVisibility(View.VISIBLE);
                                mNoFriendsYetTextView.setText("Jelenleg nincs barátod");
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

                                                    // Frissítés után az üzenet elrejtése
                                                    mNoFriendRequestsTextView.setVisibility(View.GONE);
                                                }
                                            }
                                        });
                            }

                            if (mFriendRequestsData.isEmpty()) {
                                mNoFriendRequestsTextView.setVisibility(View.VISIBLE);
                                mNoFriendRequestsTextView.setText("Jelenleg nincs függő barátkérelem");
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

                                                    // Frissítés után az üzenet elrejtése
                                                    mNoFriendRequestsTextView.setVisibility(View.GONE);
                                                }
                                            }
                                        });
                            }

                            if (mFriendRequestsData.isEmpty()) {
                                mNoFriendRequestsTextView.setVisibility(View.VISIBLE);
                                mNoFriendRequestsTextView.setText("Jelenleg nincs függő barátkérelem");
                            }
                        }
                    }
                });

        popUpShowUsersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUsers();
            }
        });
    }


    public void showUsers() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_show_users, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);


        mUserListRecyclerView = popupView.findViewById(R.id.userListRecyclerView);
        Button mPopUpCloseButton = popupView.findViewById(R.id.popUpCloseButton);

        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mUsersData = new ArrayList<>();
        mUserAdapter = new UserAdapter(this, mUsersData, currentUser.getId(), this);
        mUserListRecyclerView.setAdapter(mUserAdapter);

        mPopUpCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        mFirestore.collection("Users").orderBy("username").limit(10).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                User user = document.toObject(User.class);
                if(!Objects.equals(user.getId(), currentUser.getId())) {
                    mUsersData.add(user);
                    mUserAdapter.notifyDataSetChanged();
                }
            }
        });

        SearchView searchView = popupView.findViewById(R.id.searchByName);
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

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    public static void dimBehind(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(container, p);
    }

    @Override
    public void onFriendRequestAdded(User user) {
        mFriendRequestsData.add(user);
        mFriendRequestAdapter.notifyDataSetChanged();

        if (!mFriendRequestsData.isEmpty()) {
            mNoFriendRequestsTextView.setVisibility(View.GONE);
        }
    }
    @Override
    public void onFriendAdded(User user) {
        mFriendsData.add(user);
        mFriendAdapter.notifyDataSetChanged();

        if (!mFriendsData.isEmpty()) {
            mNoFriendsYetTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onFriendRequestRemoved(User user) {
        mFriendRequestsData.remove(user);
        mFriendRequestAdapter.notifyDataSetChanged();

        if (mFriendRequestsData.isEmpty()) {
            mNoFriendRequestsTextView.setVisibility(View.VISIBLE);
            mNoFriendRequestsTextView.setText("Jelenleg nincs függő barátkérelem");
        }
    }

    @Override
    public void onFriendRemoved(User user) {
        mFriendsData.remove(user);
        mFriendAdapter.notifyDataSetChanged();

        if (mFriendsData.isEmpty()) {
            mNoFriendsYetTextView.setVisibility(View.VISIBLE);
            mNoFriendsYetTextView.setText("Jelenleg nincs barátod");
        }
    }
}