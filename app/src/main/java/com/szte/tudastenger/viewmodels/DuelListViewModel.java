package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuelListViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final FirebaseUser mUser;
    private final CollectionReference mUsers;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<List<Duel>> pendingDuels = new MutableLiveData<>();
    private final MutableLiveData<List<Duel>> finishedDuels = new MutableLiveData<>();
    private final MutableLiveData<String> categoryAndQuestionNumber = new MutableLiveData<>();


    public DuelListViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUsers = mFirestore.collection("Users");
        loadUser();
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<List<Duel>> getPendingDuels() { return pendingDuels; }
    public LiveData<List<Duel>> getFinishedDuels() { return finishedDuels; }
    public LiveData<String> getCategoryAndQuestionNumber() { return categoryAndQuestionNumber; }

    public LiveData<String> getUsernames(Duel currentDuel) {
        MutableLiveData<String> usernames = new MutableLiveData<>();

        mFirestore.collection("Users")
                .document(currentDuel.getChallengerUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mFirestore.collection("Users")
                                .document(currentDuel.getChallengedUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot2 -> {
                                    if (documentSnapshot2.exists()) {
                                        String challengedUsername = documentSnapshot2.getString("username");
                                        String challengerUsername = documentSnapshot.getString("username");
                                        String players = challengerUsername + " - " + challengedUsername;
                                        usernames.setValue(players);
                                        Log.d("ViewModel", players);
                                    }
                                });
                    }
                });
        return usernames;
    }

    private void loadUser() {
        if (mUser != null) {
            mUsers.whereEqualTo("email", mUser.getEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            currentUser.setValue(doc.toObject(User.class));
                            loadDuels();
                        }
                    });
        }
    }

    private void loadDuels() {
        loadPendingDuels();
        loadFinishedDuels();
    }

    private void loadPendingDuels() {
        mFirestore.collection("Duels")
                .whereEqualTo("challengedUid", currentUser.getValue().getId())
                .whereEqualTo("challengedUserResults", null) //függőben lévő
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Duel> pendingDuelsList = new ArrayList<>();
                    for(QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Duel duel = document.toObject(Duel.class);
                        pendingDuelsList.add(duel);
                    }
                    pendingDuels.setValue(pendingDuelsList);
                });
    }

    private void loadFinishedDuels() {
        //azokat a párbajokat kérjük le, amelyek véget értek és a jelenlegi felhasználó volt a kihívott
        Task<QuerySnapshot> queryA = mFirestore.collection("Duels")
                .whereEqualTo("challengedUid", currentUser.getValue().getId())
                .whereEqualTo("finished", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .get();

        //azokat a párbajokat kérjük le, amelyek véget értek és a jelenlegi felhasználó volt a kihívó
        Task<QuerySnapshot> queryB = mFirestore.collection("Duels")
                .whereEqualTo("challengerUid", currentUser.getValue().getId())
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
                            }
                        }
                    }
                    finishedDuels.setValue(mergedResults);
                });
    }

    public void loadCategoryAndQuestionNumber(String categoryId, int questionCount) {
        mFirestore.collection("Categories")
                .document(categoryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String categoryName = documentSnapshot.getString("name");
                        String categoryAndQuestionNumberText = categoryName + " / " + questionCount + " db";
                        categoryAndQuestionNumber.setValue(categoryAndQuestionNumberText);
                    }
                });
    }

}
