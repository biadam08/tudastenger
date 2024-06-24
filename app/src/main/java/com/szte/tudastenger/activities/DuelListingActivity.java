package com.szte.tudastenger.activities;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.DuelAdapter;
import com.szte.tudastenger.databinding.ActivityDuelListingBinding;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private ArrayList<Duel> mPendingDuelsData;

    private RecyclerView mPendingDuelsRecyclerView;

    private DuelAdapter mPendingDuelsAdapter;

    private ArrayList<Duel> mFinishedDuelsData;

    private RecyclerView mFinishedDuelsRecyclerView;

    private DuelAdapter mFinishedDuelsAdapter;

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

        mPendingDuelsRecyclerView = findViewById(R.id.pendingDuelListRecyclerView);
        mPendingDuelsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mPendingDuelsData = new ArrayList<>();

        mFinishedDuelsRecyclerView = findViewById(R.id.finishedDuelListRecyclerView);
        mFinishedDuelsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mFinishedDuelsData = new ArrayList<>();

        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                mPendingDuelsAdapter = new DuelAdapter(this, mPendingDuelsData, currentUser.getId());
                mPendingDuelsRecyclerView.setAdapter(mPendingDuelsAdapter);

                mFinishedDuelsAdapter = new DuelAdapter(this, mFinishedDuelsData, currentUser.getId());
                mFinishedDuelsRecyclerView.setAdapter(mFinishedDuelsAdapter);

                queryData();
            }
        });
    }

    private void queryData() {
        mPendingDuelsData.clear();
        mFinishedDuelsData.clear();

        mFirestore.collection("Duels")
                .whereEqualTo("challengedUid", currentUser.getId())
                .whereEqualTo("challengedUserResults", null) //függőben lévő
                .orderBy("date", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
            for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                Duel duel = document.toObject(Duel.class);
                mPendingDuelsData.add(duel);
            }

            mPendingDuelsAdapter.notifyDataSetChanged();
        });

        //azokat a párbajokat kérjük le, amelyek véget értek és a jelenlegi felhasználó volt a kihívott
        Task<QuerySnapshot> queryA = mFirestore.collection("Duels")
                .whereEqualTo("challengedUid", currentUser.getId())
                .whereEqualTo("finished", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .get();

        //azokat a párbajokat kérjük le, amelyek véget értek és a jelenlegi felhasználó volt a kihívó
        Task<QuerySnapshot> queryB = mFirestore.collection("Duels")
                .whereEqualTo("challengerUid", currentUser.getId())
                .whereEqualTo("finished", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .get();

        //egyesítjük őket
        Tasks.whenAllSuccess(queryA, queryB)
                .addOnSuccessListener(results -> {
                    Set<String> docIds = new HashSet<>();
                    List<Duel> mergedResults = new ArrayList<>();

                    for (Object result : results) {
                        QuerySnapshot querySnapshot = (QuerySnapshot) result;
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            if (!docIds.contains(document.getId())) {
                                docIds.add(document.getId());
                                Duel duel = document.toObject(Duel.class);
                                mergedResults.add(duel);
                                Log.d("duel456", duel.getId());
                                mFinishedDuelsData.add(duel);
                            }
                        }
                    }

                    mFinishedDuelsAdapter.notifyDataSetChanged();
                });
    }
}