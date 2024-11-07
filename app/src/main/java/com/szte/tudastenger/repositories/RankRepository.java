package com.szte.tudastenger.repositories;


import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.models.Rank;


import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class RankRepository {
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mRanks;

    @Inject
    public RankRepository(FirebaseFirestore firestore) {
        this.mFirestore = firestore;
        this.mRanks = mFirestore.collection("Ranks");
    }

    public void updateRank(Rank rank, SuccessCallback successCallback, ErrorCallback errorCallback) {
        // rang ellenőrzése: van-e már ilyen névvel vagy határral rang feltöltve
        mRanks.whereEqualTo("rankName", rank.getRankName())
                .get()
                .addOnSuccessListener(nameQuerySnapshot -> {
                    boolean hasNameDuplicate = false;
                    for (DocumentSnapshot doc : nameQuerySnapshot) {
                        if (!doc.getId().equals(rank.getId())) {
                            hasNameDuplicate = true;
                            break;
                        }
                    }

                    if (hasNameDuplicate) {
                        errorCallback.onError("Már létezik rang ezzel a névvel!");
                        return;
                    }

                    mRanks.whereEqualTo("threshold", rank.getThreshold())
                            .get()
                            .addOnSuccessListener(thresholdQuerySnapshot -> {
                                boolean hasThresholdDuplicate = false;
                                for (DocumentSnapshot doc : thresholdQuerySnapshot) {
                                    if (!doc.getId().equals(rank.getId())) {
                                        hasThresholdDuplicate = true;
                                        break;
                                    }
                                }

                                if (hasThresholdDuplicate) {
                                    errorCallback.onError("Már létezik rang ezzel a ponthatárral!");
                                    return;
                                }

                                // ha nincs, végrehajtjuk a módosítást
                                mRanks.document(rank.getId())
                                        .set(rank)
                                        .addOnSuccessListener(aVoid -> successCallback.onSuccess("Rang sikeresen módosítva lett!"))
                                        .addOnFailureListener(e -> errorCallback.onError("Hiba történt a módosítás során."));
                            })
                            .addOnFailureListener(e -> errorCallback.onError("Hiba történt az ellenőrzés során."));
                })
                .addOnFailureListener(e -> errorCallback.onError("Hiba történt az ellenőrzés során."));
    }

    public void addNewRank(Rank rank, SuccessCallback successCallback, ErrorCallback errorCallback) {
        // rang ellenőrzése: van-e már ilyen névvel vagy határral rang feltöltve
        mRanks.whereEqualTo("rankName", rank.getRankName())
                .get()
                .addOnSuccessListener(nameQuerySnapshot -> {
                    if (!nameQuerySnapshot.isEmpty()) {
                        errorCallback.onError("Már létezik rang ezzel a névvel!");
                        return;
                    }

                    mRanks.whereEqualTo("threshold", rank.getThreshold())
                            .get()
                            .addOnSuccessListener(thresholdQuerySnapshot -> {
                                if (!thresholdQuerySnapshot.isEmpty()) {
                                    errorCallback.onError("Már létezik rang ezzel a ponthatárral!");
                                    return;
                                }

                                // ha nincs, végrehajtjuk a módosítást
                                mRanks.add(rank)
                                        .addOnSuccessListener(documentReference -> {
                                            String docId = documentReference.getId();
                                            rank.setId(docId);
                                            mRanks.document(docId)
                                                    .update("id", docId);
                                            successCallback.onSuccess("Rang sikeresen létrehozva!");
                                        })
                                        .addOnFailureListener(e -> errorCallback.onError("Sikertelen rang hozzáadás!"));
                            })
                            .addOnFailureListener(e -> errorCallback.onError("Hiba történt az ellenőrzés során."));
                })
                .addOnFailureListener(e -> errorCallback.onError("Hiba történt az ellenőrzés során."));
    }

    public void loadRankData(String rankId, RankReceivedCallback rankReceivedCallback) {
        mRanks.document(rankId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Rank rank = documentSnapshot.toObject(Rank.class);
                        rankReceivedCallback.onRankReceived(rank);
                    }
                });
    }

    public void deleteRank(String rankId, SuccessCallback successCallback, ErrorCallback errorCallback) {
        mRanks.document(rankId)
                .delete()
                .addOnSuccessListener(aVoid2 -> successCallback.onSuccess("A rang sikeresen törölve lett!"))
                .addOnFailureListener(e -> errorCallback.onError("Hiba történt a törlés közben"));
    }

    public void loadRanks(RankLoadedCallback rankLoadedCallback, ErrorCallback errorCallback) {
        mRanks.orderBy("threshold")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Rank> rankList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Rank rank = doc.toObject(Rank.class);
                        rank.setId(doc.getId());
                        rankList.add(rank);
                    }
                    rankLoadedCallback.onRanksLoaded(rankList);
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public interface SuccessCallback {
        void onSuccess(String message);
    }
    public interface ErrorCallback {
        void onError(String error);
    }
    public interface RankReceivedCallback {
        void onRankReceived(Rank rank);
    }

    public interface RankLoadedCallback {
        void onRanksLoaded(List<Rank> ranks);
    }
}
