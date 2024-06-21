package com.szte.tudastenger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.CategoryAdapter;
import com.szte.tudastenger.adapters.DuelAdapter;
import com.szte.tudastenger.adapters.FriendAdapter;
import com.szte.tudastenger.adapters.FriendRequestAdapter;
import com.szte.tudastenger.databinding.ActivityDuelBinding;
import com.szte.tudastenger.databinding.ActivityDuelListingBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;

public class DuelListingActivity extends DrawerBaseActivity{
    private ActivityDuelListingBinding activityDuelListingBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private User currentUser;
    private DocumentReference userRef;
    private StorageReference storageReference;
    private CollectionReference mUsers;

    private CollectionReference mDuels;

    private ArrayList<Duel> mDuelsData;

    private RecyclerView mRecyclerView;

    private DuelAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duel_listing);
        activityDuelListingBinding = ActivityDuelListingBinding.inflate(getLayoutInflater());
        setContentView(activityDuelListingBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
        }

        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");

        mRecyclerView = findViewById(R.id.pendingDuelListRecyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        mDuelsData = new ArrayList<>();
        mAdapter = new DuelAdapter(this, mDuelsData);
        mRecyclerView.setAdapter(mAdapter);

        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                queryData();
            }
        });
    }

    private void queryData() {
        mDuelsData.clear();

        mFirestore.collection("Duels")
                .whereEqualTo("challengedUid", currentUser.getId())
                .whereEqualTo("challengedUserResults", null) //függőben lévő
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
            for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                Duel duel = document.toObject(Duel.class);
                mDuelsData.add(duel);
            }

            mAdapter.notifyDataSetChanged();
        });

    }
}