package com.szte.tudastenger.repositories;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class DuelRepository {
    private final FirebaseFirestore mFirestore;

    @Inject
    public DuelRepository(FirebaseFirestore firestore) {
        this.mFirestore = firestore;
    }


    public void loadPendingDuels(String userId, DuelsLoadedCallback duelsCallback) {
        mFirestore.collection("Duels")
                .whereEqualTo("challengedUid", userId)
                .whereEqualTo("challengedUserResults", null) //függőben lévő
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Duel> pendingDuelsList = new ArrayList<>();
                    for(QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Duel duel = document.toObject(Duel.class);
                        pendingDuelsList.add(duel);
                    }
                    duelsCallback.onDuelsLoaded(pendingDuelsList);
                });
    }

    public void loadFinishedDuels(String userId, DuelsLoadedCallback duelsCallback) {
        //azokat a párbajokat kérjük le, amelyek véget értek és a jelenlegi felhasználó volt a kihívott
        Task<QuerySnapshot> queryA = mFirestore.collection("Duels")
                .whereEqualTo("challengedUid", userId)
                .whereEqualTo("finished", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .get();

        //azokat a párbajokat kérjük le, amelyek véget értek és a jelenlegi felhasználó volt a kihívó
        Task<QuerySnapshot> queryB = mFirestore.collection("Duels")
                .whereEqualTo("challengerUid", userId)
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
                    duelsCallback.onDuelsLoaded(mergedResults);
                });
    }

    public void loadDuelWithQuestions(String duelId, DuelLoadedCallback duelCallback, QuestionsLoadedCallback questionsCallback) {
        mFirestore.collection("Duels").document(duelId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Duel duel = documentSnapshot.toObject(Duel.class);
                        ArrayList<String> questionIds = duel.getQuestionIds();
                        duelCallback.onDuelLoaded(duel, questionIds);

                        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                        for (String questionId : questionIds) {
                            tasks.add(mFirestore.collection("Questions").document(questionId).get());
                        }

                        Tasks.whenAllSuccess(tasks).addOnSuccessListener(list -> {
                            ArrayList<Question> questionList = new ArrayList<>();
                            for (Object object : list) {
                                DocumentSnapshot document = (DocumentSnapshot) object;
                                Question question = document.toObject(Question.class);
                                questionList.add(question);
                            }
                            questionsCallback.onQuestionsLoaded(questionList);
                        });
                    }
                });
    }

    public void createNewDuel(Duel duel, DuelCreatedCallback duelCreatedCallback) {
        mFirestore.collection("Duels")
                .add(duel)
                .addOnSuccessListener(documentReference -> {
                    String documentId = documentReference.getId();
                    duel.setId(documentId);
                    mFirestore.collection("Duels")
                            .document(documentId)
                            .update("id", documentId)
                            .addOnSuccessListener(aVoid -> duelCreatedCallback.onDuelCreated());
                });
    }

    public void updateDuelWithChallengedResults(String duelId, ArrayList<Boolean> challengedResults, DuelUpdatedCallback duelUpdatedCallback) {
        mFirestore.collection("Duels")
                .document(duelId)
                .update(
                        "challengedUserResults", challengedResults,
                        "finished", true
                )
                .addOnSuccessListener(aVoid -> duelUpdatedCallback.onDuelUpdated());
    }

    public void getDuelById(String duelId, DuelByIdLoadedCallback duelByIdLoadedCallback) {
        mFirestore.collection("Duels").document(duelId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ArrayList<Boolean> challengerResults = (ArrayList<Boolean>) documentSnapshot.get("challengerUserResults");
                        duelByIdLoadedCallback.onDuelLoaded(challengerResults);
                    }
                });
    }


    public interface DuelsLoadedCallback {
        void onDuelsLoaded(List<Duel> duels);
    }
    public interface DuelLoadedCallback {
        void onDuelLoaded(Duel duel, ArrayList<String> questionIds);
    }
    public interface QuestionsLoadedCallback {
        void onQuestionsLoaded(ArrayList<Question> questions);
    }
    public interface DuelCreatedCallback {
        void onDuelCreated();
    }
    public interface DuelUpdatedCallback {
        void onDuelUpdated();
    }
    public interface DuelByIdLoadedCallback {
        void onDuelLoaded(ArrayList<Boolean> challengerResults);
    }

}
