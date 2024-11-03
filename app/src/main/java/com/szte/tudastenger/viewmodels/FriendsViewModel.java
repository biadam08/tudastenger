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
import com.szte.tudastenger.repositories.FriendRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FriendsViewModel extends AndroidViewModel {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<User>> mFriendsData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<User>> mFriendRequestsData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<User>> mUsersData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> noFriendsVisibility = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> noRequestsVisibility = new MutableLiveData<>(false);
    private final MutableLiveData<Map<String, Boolean>> friendButtonStates = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<User> friendRequestSent = new MutableLiveData<>();


    public FriendsViewModel(Application application) {
        super(application);
        userRepository = new UserRepository();
        friendRepository = new FriendRepository();

        initCurrentUser();
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<ArrayList<User>> getFriendsData() {
        return mFriendsData;
    }

    public LiveData<ArrayList<User>> getFriendRequestsData() {
        return mFriendRequestsData;
    }

    public LiveData<ArrayList<User>> getUsersData() {
        return mUsersData;
    }

    public LiveData<Boolean> getNoFriendsVisibility() {
        return noFriendsVisibility;
    }

    public LiveData<Boolean> getNoRequestsVisibility() {
        return noRequestsVisibility;
    }

    public LiveData<Map<String, Boolean>> getFriendButtonStates() {
        return friendButtonStates;
    }

    public LiveData<User> getFriendRequestSent() {
        return friendRequestSent;
    }

    private void initCurrentUser() {
        userRepository.loadCurrentUser(
                user -> {
                    currentUser.setValue(user);
                    queryData();
                }
        );
    }

    public void queryData() {
        if (currentUser.getValue() == null){
            return;
        }

        friendRepository.queryFriends(
                currentUser.getValue().getId(),
                friend -> {
                    ArrayList<User> currentList = mFriendsData.getValue();
                    currentList.add(friend);
                    mFriendsData.setValue(currentList);
                    noFriendsVisibility.setValue(false);
                    Log.d("FriendsVWM noFriendsVisibility", noFriendsVisibility.getValue().toString());
                },
                () -> {
                    noFriendsVisibility.setValue(true);
                    Log.d("FriendsVWM noFriendsVisibility", noFriendsVisibility.getValue().toString());
                }
        );

        friendRepository.queryFriendRequests(
                currentUser.getValue().getId(),
                request -> {
                    ArrayList<User> currentList = mFriendRequestsData.getValue();
                    currentList.add(request);
                    mFriendRequestsData.setValue(currentList);
                    noRequestsVisibility.setValue(false);
                },
                () -> noRequestsVisibility.setValue(true)
        );
    }

    public void loadUsers() {
        userRepository.loadUsers(users -> {
            ArrayList<User> filteredUsers = new ArrayList<>();
            for (User user : users) {
                if (!Objects.equals(user.getId(), currentUser.getValue().getId())) {
                    filteredUsers.add(user);
                }
            }
            mUsersData.setValue(filteredUsers);
        });
    }

    public void deleteFriend(User friendToDelete) {
        friendRepository.deleteFriend(
                currentUser.getValue().getId(),
                friendToDelete.getId(),
                () -> {
                    ArrayList<User> currentFriends = mFriendsData.getValue();
                    currentFriends.remove(friendToDelete);
                    mFriendsData.setValue(currentFriends);
                    noFriendsVisibility.setValue(currentFriends.isEmpty());
                }
        );
    }

    public void checkFriendship(String currentUserId, String userId) {
        friendRepository.checkFriendship(
                currentUserId,
                userId,
                (checkedUserId, canSendRequest) -> {
                    Map<String, Boolean> currentStates = friendButtonStates.getValue();
                    currentStates.put(checkedUserId, canSendRequest);
                    friendButtonStates.setValue(currentStates);
                }
        );
    }

    public void sendFriendRequest(String currentUserId, User targetUser) {
        friendRepository.sendFriendRequest(
                currentUserId,
                targetUser,
                sentToUser -> {
                    Map<String, Boolean> currentStates = friendButtonStates.getValue();
                    currentStates.put(targetUser.getId(), false);
                    friendButtonStates.setValue(currentStates);
                    friendRequestSent.setValue(targetUser);
                }
        );
    }

    public LiveData<Boolean> checkCanApproveRequest(String userId, String currentUserId) {
        return friendRepository.canApprove(userId, currentUserId);
    }

    public void approveFriendRequest(User requestUser) {
        friendRepository.approveFriendRequest(
                currentUser.getValue().getId(),
                requestUser.getId(),
                () -> {
                    onFriendAdded(requestUser);
                    onFriendRequestRemoved(requestUser);
                }
        );
    }

    public void declineFriendRequest(User requestUser) {
        friendRepository.declineFriendRequest(
                currentUser.getValue().getId(),
                requestUser.getId(),
                () -> onFriendRequestRemoved(requestUser)
        );
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