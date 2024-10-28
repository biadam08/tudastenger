package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FriendsViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final FirebaseAuth mAuth;
    private final FirebaseUser mUser;
    private final CollectionReference mUsers;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<User>> mFriendsData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<User>> mFriendRequestsData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<User>> mUsersData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> noFriendsVisibility = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noRequestsVisibility = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Boolean>> friendButtonStates = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<User> friendRequestSent = new MutableLiveData<>();


    public FriendsViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mUsers = mFirestore.collection("Users");

        if(mUser != null) {
            initCurrentUser();
        }
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<ArrayList<User>> getFriendsData() { return mFriendsData; }
    public LiveData<ArrayList<User>> getFriendRequestsData() { return mFriendRequestsData; }
    public LiveData<ArrayList<User>> getUsersData() { return mUsersData; }
    public LiveData<Boolean> getNoFriendsVisibility() { return noFriendsVisibility; }
    public LiveData<Boolean> getNoRequestsVisibility() { return noRequestsVisibility; }

    public LiveData<Map<String, Boolean>> getFriendButtonStates() {
        return friendButtonStates;
    }
    public LiveData<User> getFriendRequestSent() {
        return friendRequestSent;
    }

    private void initCurrentUser() {
        mUsers.whereEqualTo("email", mUser.getEmail())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        currentUser.setValue(doc.toObject(User.class));
                        queryData();
                    }
                });
    }

    public void queryData() {
        // Barátok lekérdezése
        mFirestore.collection("Friends").document(currentUser.getValue().getId()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> friends = document.getData();
                            for (Map.Entry<String, Object> entry : friends.entrySet()) {
                                if ((boolean) entry.getValue()) { // Ha a barát státusza true
                                    String friendId = entry.getKey();

                                    mFirestore.collection("Users").document(friendId).get()
                                            .addOnSuccessListener(documentSnapshot -> {
                                                if (documentSnapshot.exists()) {
                                                    User friend = documentSnapshot.toObject(User.class);
                                                    ArrayList<User> currentList = mFriendsData.getValue();
                                                    currentList.add(friend);
                                                    mFriendsData.setValue(currentList);
                                                    noFriendsVisibility.setValue(false);
                                                }
                                            });
                                }
                            }
                        }

                        if (mFriendsData.getValue().isEmpty()) {
                            noFriendsVisibility.setValue(true);
                        }
                    }
                });

        // Bejövő barátkérelmek lekérdezése
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", currentUser.getValue().getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String user_uid2 = document.getString("user_uid2");

                            mFirestore.collection("Users").document(user_uid2).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            User friend = documentSnapshot.toObject(User.class);
                                            ArrayList<User> currentList = mFriendRequestsData.getValue();
                                            currentList.add(friend);
                                            mFriendRequestsData.setValue(currentList);
                                            noRequestsVisibility.setValue(false);
                                        }
                                    });
                        }

                        if (mFriendRequestsData.getValue().isEmpty()) {
                            noRequestsVisibility.setValue(true);
                        }
                    }
                });

        // Kimenő barátkérelmek lekérdezése
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid2", currentUser.getValue().getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String user_uid1 = document.getString("user_uid1");

                            mFirestore.collection("Users").document(user_uid1).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            User friend = documentSnapshot.toObject(User.class);
                                            ArrayList<User> currentList = mFriendRequestsData.getValue();
                                            currentList.add(friend);
                                            mFriendRequestsData.setValue(currentList);
                                            noRequestsVisibility.setValue(false);
                                        }
                                    });
                        }

                        if (mFriendRequestsData.getValue().isEmpty()) {
                            noRequestsVisibility.setValue(true);
                        }
                    }
                });
    }

    public void loadUsers() {
        mFirestore.collection("Users")
                .orderBy("username")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if(!Objects.equals(user.getId(), currentUser.getValue().getId())) {
                            users.add(user);
                        }
                    }
                    mUsersData.setValue(users);
                });
    }

    public void deleteFriend(User friendToDelete) {
        mFirestore.collection("Friends")
                .document(currentUser.getValue().getId())
                .update(Collections.singletonMap(friendToDelete.getId(), FieldValue.delete()));

        mFirestore.collection("Friends")
                .document(friendToDelete.getId())
                .update(Collections.singletonMap(currentUser.getValue().getId(), FieldValue.delete()));

        ArrayList<User> currentFriends = mFriendsData.getValue();
        if (currentFriends != null) {
            currentFriends.remove(friendToDelete);
            mFriendsData.setValue(currentFriends);
            noFriendsVisibility.setValue(currentFriends.isEmpty());
        }
    }

    public LiveData<Boolean> checkCanApproveRequest(String userId, String currentUserId) {
        MutableLiveData<Boolean> canApprove = new MutableLiveData<>(false);

        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", userId)
                .whereEqualTo("user_uid2", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        canApprove.setValue(!task.getResult().isEmpty());
                    }
                });

        return canApprove;
    }

    public void approveFriendRequest(User requestUser) {
        declineFriendRequest(requestUser);

        // Adjuk hozzá a Friends kollekcióba
        addToFriends(currentUser.getValue().getId(), requestUser.getId());
        addToFriends(requestUser.getId(), currentUser.getValue().getId());

        // UI frissítése
        onFriendAdded(requestUser);
        onFriendRequestRemoved(requestUser);
    }

    private void addToFriends(String userId1, String userId2) {
        Map<String, Object> friend = new HashMap<>();
        friend.put(userId2, true);

        mFirestore.collection("Friends")
                .document(userId1)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mFirestore.collection("Friends")
                                .document(userId1)
                                .update(friend);
                    } else {
                        mFirestore.collection("Friends")
                                .document(userId1)
                                .set(friend);
                    }
                });
    }

    public void declineFriendRequest(User requestUser) {
        // Törlés az első irányból
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", currentUser.getValue().getId())
                .whereEqualTo("user_uid2", requestUser.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                        }
                    }
                });

        // Törlés a másik irányból
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", requestUser.getId())
                .whereEqualTo("user_uid2", currentUser.getValue().getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                        }
                    }
                });

        onFriendRequestRemoved(requestUser);
    }

    public void checkFriendship(String currentUserId, String userId) {
        mFirestore.collection("Friends").document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists() && document.get(userId) != null) {
                            updateButtonState(userId, false);
                        } else {
                            checkOriginalFriendRequest(currentUserId, userId);
                        }
                    }
                });
    }

    private void checkOriginalFriendRequest(String currentUserId, String userId) {
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", currentUserId)
                .whereEqualTo("user_uid2", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            checkReverseFriendRequest(currentUserId, userId);
                        } else {
                            updateButtonState(userId, false);
                        }
                    }
                });
    }

    private void checkReverseFriendRequest(String currentUserId, String userId) {
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", userId)
                .whereEqualTo("user_uid2", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateButtonState(userId, task.getResult().isEmpty());
                    }
                });
    }

    private void updateButtonState(String userId, boolean isVisible) {
        Map<String, Boolean> currentStates = friendButtonStates.getValue();
        currentStates.put(userId, isVisible);
        friendButtonStates.setValue(currentStates);
    }

    public void sendFriendRequest(String currentUserId, User targetUser) {
        final Map<String, Object> data = new HashMap<>();
        data.put("user_uid1", currentUserId);
        data.put("user_uid2", targetUser.getId());

        mFirestore.collection("Friends").document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists() && document.getBoolean(targetUser.getId()) != null) {
                            Log.d("barathozzaad", "Már barát");
                        } else {
                            checkAndSendRequest(currentUserId, targetUser, data);
                        }
                    }
                });
    }

    private void checkAndSendRequest(String currentUserId, User targetUser, Map<String, Object> data) {
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", currentUserId)
                .whereEqualTo("user_uid2", targetUser.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            checkReverseAndSend(currentUserId, targetUser, data);
                        } else {
                            Log.d("barathozzaad", "A barátmeghívási kérelem már létezik");
                        }
                    }
                });
    }

    private void checkReverseAndSend(String currentUserId, User targetUser, Map<String, Object> data) {
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", targetUser.getId())
                .whereEqualTo("user_uid2", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            mFirestore.collection("FriendRequests").add(data)
                                    .addOnSuccessListener(documentReference -> {
                                        updateButtonState(targetUser.getId(), false);
                                        friendRequestSent.setValue(targetUser);  // Értesítjük a UI-t
                                    })
                                    .addOnFailureListener(e ->
                                            updateButtonState(targetUser.getId(), true));
                        } else {
                            Log.d("barathozzaad", "A barátmeghívási kérelem már létezik");
                        }
                    }
                });
    }


    public void onFriendRequestAdded(User user) {
        ArrayList<User> currentRequests = mFriendRequestsData.getValue();
        currentRequests.add(user);
        mFriendRequestsData.setValue(currentRequests);
        noRequestsVisibility.setValue(false);
    }

    public void onFriendAdded(User user) {
        ArrayList<User> currentFriends = mFriendsData.getValue();
        currentFriends.add(user);
        mFriendsData.setValue(currentFriends);
        noFriendsVisibility.setValue(false);
    }

    public void onFriendRequestRemoved(User user) {
        ArrayList<User> currentRequests = mFriendRequestsData.getValue();
        currentRequests.remove(user);
        mFriendRequestsData.setValue(currentRequests);
        noRequestsVisibility.setValue(currentRequests.isEmpty());
    }

    public void onFriendRemoved(User user) {
        ArrayList<User> currentFriends = mFriendsData.getValue();
        currentFriends.remove(user);
        mFriendsData.setValue(currentFriends);
        noFriendsVisibility.setValue(currentFriends.isEmpty());
    }
}