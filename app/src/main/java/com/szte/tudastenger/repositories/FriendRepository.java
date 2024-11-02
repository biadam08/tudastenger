package com.szte.tudastenger.repositories;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.models.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FriendRepository {
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mUsers;
    private final CollectionReference mFriends;
    private final CollectionReference mFriendRequests;

    public FriendRepository() {
        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
        mFriends = mFirestore.collection("Friends");
        mFriendRequests = mFirestore.collection("FriendRequests");
    }

    public void queryFriends(String userId, FriendLoadedCallback friendCallback, NoFriendsCallback noFriendsCallback) {
        mFriends.document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> friends = document.getData();
                            if (friends != null) {
                                for (Map.Entry<String, Object> entry : friends.entrySet()) {
                                    if ((boolean) entry.getValue()) {
                                        String friendId = entry.getKey();
                                        mUsers.document(friendId).get()
                                                .addOnSuccessListener(documentSnapshot -> {
                                                    if (documentSnapshot.exists()) {
                                                        User friend = documentSnapshot.toObject(User.class);
                                                        friendCallback.onFriendLoaded(friend);
                                                    }
                                                });
                                    }
                                }
                            }
                        }
                    } else {
                        noFriendsCallback.onNoFriends();
                    }
                });
    }

    public void queryFriendRequests(String userId, RequestLoadedCallback callback, NoRequestsCallback noRequestsCallback) {
        // Bejövő barátkérelmek
        mFriendRequests.whereEqualTo("user_uid1", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String user_uid2 = document.getString("user_uid2");
                            mUsers.document(user_uid2).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            User friend = documentSnapshot.toObject(User.class);
                                            callback.onRequestLoaded(friend);
                                            noRequestsCallback.onRequestExists();
                                        }
                                    });
                        }
                        if (task.getResult().isEmpty()) {
                            noRequestsCallback.onNoRequests();
                        }
                    }
                });

        // Kimenő barátkérelmek
        mFriendRequests.whereEqualTo("user_uid2", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String user_uid1 = document.getString("user_uid1");
                            mUsers.document(user_uid1).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            User friend = documentSnapshot.toObject(User.class);
                                            callback.onRequestLoaded(friend);
                                            noRequestsCallback.onRequestExists();
                                        }
                                    });
                        }
                        if (task.getResult().isEmpty()) {
                            noRequestsCallback.onNoRequests();
                        }
                    }
                });
    }

    public void deleteFriend(String currentUserId, String friendId, SuccessCallback successCallback) {
        mFriends.document(currentUserId)
                .update(Collections.singletonMap(friendId, FieldValue.delete()));

        mFriends.document(friendId)
                .update(Collections.singletonMap(currentUserId, FieldValue.delete()))
                .addOnSuccessListener(aVoid -> successCallback.onSuccess());
    }

    public void checkFriendship(String currentUserId, String userId, FriendshipStatusCallback callback) {
        mFriends.document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists() && document.get(userId) != null) {
                            callback.onStatusChecked(userId, false);
                        } else {
                            checkOriginalFriendRequest(currentUserId, userId, callback);
                        }
                    }
                });
    }

    private void checkOriginalFriendRequest(String currentUserId, String userId, FriendshipStatusCallback callback) {
        mFriendRequests.whereEqualTo("user_uid1", currentUserId)
                .whereEqualTo("user_uid2", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            checkReverseFriendRequest(currentUserId, userId, callback);
                        } else {
                            callback.onStatusChecked(userId, false);
                        }
                    }
                });
    }

    private void checkReverseFriendRequest(String currentUserId, String userId, FriendshipStatusCallback callback) {
        mFriendRequests.whereEqualTo("user_uid1", userId)
                .whereEqualTo("user_uid2", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onStatusChecked(userId, task.getResult().isEmpty());
                    }
                });
    }

    public void sendFriendRequest(String currentUserId, User targetUser, RequestSentCallback callback) {
        Map<String, String> data = new HashMap<>();
        data.put("user_uid1", currentUserId);
        data.put("user_uid2", targetUser.getId());

        mFriendRequests.add(data)
                .addOnSuccessListener(documentReference -> callback.onRequestSent(targetUser));
    }

    public void approveFriendRequest(String currentUserId, String requesterId, SuccessCallback successCallback) {
        // Töröljük a kérelmet
        deleteFriendRequests(currentUserId, requesterId);

        // Hozzáadjuk a Friends kollekcióhoz
        addToFriends(currentUserId, requesterId);
        addToFriends(requesterId, currentUserId);

        successCallback.onSuccess();
    }

    private void addToFriends(String userId1, String userId2) {
        Map<String, Object> friend = new HashMap<>();
        friend.put(userId2, true);

        mFriends.document(userId1)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mFriends.document(userId1)
                                .update(friend);
                    } else {
                        mFriends.document(userId1)
                                .set(friend);
                    }
                });
    }

    public void declineFriendRequest(String currentUserId, String requesterId, SuccessCallback successCallback) {
        deleteFriendRequests(currentUserId, requesterId);
        successCallback.onSuccess();
    }

    private void deleteFriendRequests(String userId1, String userId2) {
        // Törlés az első irányból
        mFriendRequests.whereEqualTo("user_uid1", userId1)
                .whereEqualTo("user_uid2", userId2)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                        }
                    }
                });

        // Törlés a másik irányból
        mFriendRequests.whereEqualTo("user_uid1", userId2)
                .whereEqualTo("user_uid2", userId1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                        }
                    }
                });
    }

    public MutableLiveData<Boolean> canApprove(String userId1, String userId2) {
        MutableLiveData<Boolean> canApprove = new MutableLiveData<>(false);
        mFriendRequests
                .whereEqualTo("user_uid1", userId1)
                .whereEqualTo("user_uid2", userId2)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        canApprove.setValue(!task.getResult().isEmpty());
                    }
                });

        return canApprove;
    }

    public interface FriendLoadedCallback {
        void onFriendLoaded(User friend);
    }

    public interface NoFriendsCallback {
        void onNoFriends();
    }

    public interface RequestLoadedCallback {
        void onRequestLoaded(User user);
    }

    public interface NoRequestsCallback {
        void onNoRequests();

        void onRequestExists();
    }

    public interface FriendshipStatusCallback {
        void onStatusChecked(String userId, boolean canSendRequest);
    }

    public interface RequestSentCallback {
        void onRequestSent(User targetUser);
    }

    public interface SuccessCallback {
        void onSuccess();
    }

    public interface ErrorCallback {
        void onError(String error);
    }
}