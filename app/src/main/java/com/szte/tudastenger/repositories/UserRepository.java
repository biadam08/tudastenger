package com.szte.tudastenger.repositories;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class UserRepository {
    private final FirebaseFirestore mFirestore;
    private final FirebaseAuth mAuth;
    private final StorageReference mStorage;
    private final FirebaseUser mCurrentUser;

    @Inject
    public UserRepository(FirebaseFirestore firestore, FirebaseAuth auth, StorageReference storage) {
        this.mFirestore = firestore;
        this.mAuth = auth;
        this.mStorage = storage;
        this.mCurrentUser = mAuth.getCurrentUser();
    }
    public void checkAdmin(AdminStatusCallback adminStatusCallback, ErrorCallback errorCallback) {
        if (mCurrentUser == null || mCurrentUser.getEmail() == null) {
            adminStatusCallback.onCheckAdmin(false);
            return;
        }

        mFirestore.collection("Users")
                .document(mCurrentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean role = documentSnapshot.exists() && "admin".equals(documentSnapshot.getString("role"));
                    adminStatusCallback.onCheckAdmin(role);
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void getUsernamesForDuel(String challengerUid, String challengedUid, UsernamesLoadedCallback usernamesLoadedCallback){
        mFirestore.collection("Users")
                .document(challengerUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mFirestore.collection("Users")
                                .document(challengedUid)
                                .get()
                                .addOnSuccessListener(documentSnapshot2 -> {
                                    if (documentSnapshot2.exists()) {
                                        String challengedUsername = documentSnapshot2.getString("username");
                                        String challengerUsername = documentSnapshot.getString("username");
                                        String players = challengerUsername + " - " + challengedUsername;
                                        usernamesLoadedCallback.onUsernamesLoaded(players);
                                    }
                                });
                    }
                });
    }

    public void loadCurrentUser(UserLoadedCallback userCallback) {
        if (mCurrentUser != null) {
            mFirestore.collection("Users").whereEqualTo("email", mCurrentUser.getEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            userCallback.onUserLoaded(doc.toObject(User.class));
                        }
                    });
        }
    }

    public void loadProfilePicture(String profilePicturePath, ProfilePictureLoadedCallback pictureCallback, ErrorCallback errorCallback) {
        String imagePath = !profilePicturePath.isEmpty() ? "profile-pictures/" + profilePicturePath : "profile-pictures/default.jpg";

        mStorage.child(imagePath)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> pictureCallback.onProfilePictureLoaded(uri.toString()))
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void uploadProfilePicture(Uri imageUri, UploadCallback uploadCallback, ProgressCallback progressCallback, ErrorCallback errorCallback) {
        if (imageUri != null && mCurrentUser != null) {
            String fileName = UUID.randomUUID().toString();
            StorageReference ref = mStorage.child("profile-pictures/" + fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        mFirestore.collection("Users").document(mCurrentUser.getUid())
                                .update("profilePicture", fileName)
                                .addOnSuccessListener(aVoid -> uploadCallback.onUploadSuccess(fileName))
                                .addOnFailureListener(e -> errorCallback.onError("Hiba történt a feltöltés során"));
                    })
                    .addOnFailureListener(e -> errorCallback.onError("Hiba történt a feltöltés során"))
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        progressCallback.onProgress((int) progress);
                    });
        }
    }

    public void loadUserProfile(UserProfileCallback profileCallback, ErrorCallback errorCallback) {
        if (mCurrentUser != null && mCurrentUser.getEmail() != null) {
            mFirestore.collection("Users").document(mCurrentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String profilePicture = documentSnapshot.getString("profilePicture");
                            profileCallback.onProfileLoaded(username, profilePicture);
                        }
                    })
                    .addOnFailureListener(e -> errorCallback.onError("Nem sikerült betölteni a profilt"));
        }
    }

    public void changePassword(String email, String currentPassword, String newPassword, SuccessCallback successCallback, ErrorCallback errorCallback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(aVoid ->
                            currentUser.updatePassword(newPassword)
                                    .addOnSuccessListener(aVoid2 -> successCallback.onSuccess("A jelszavadat sikeresen megváltoztattad!"))
                                    .addOnFailureListener(e -> errorCallback.onError("A jelszavad megváltoztatása közben hiba lépett fel!"))
                    )
                    .addOnFailureListener(e -> errorCallback.onError("Az általad beírt jelenlegi jelszó nem egyezik meg a jelszavaddal!"));
        }
    }

    public void deleteAccount(SuccessCallback successCallback, ErrorCallback errorCallback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mFirestore.collection("Users").document(currentUser.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid ->
                            currentUser.delete()
                                    .addOnSuccessListener(aVoid2 -> successCallback.onSuccess("A fiók sikeresen törölve!"))
                                    .addOnFailureListener(e -> errorCallback.onError("A fiók törlése nem sikerült!"))
                    )
                    .addOnFailureListener(e -> errorCallback.onError("A fiók törlése nem sikerült!"));
        }
    }

    public void resetPassword(String email, SuccessCallback successCallback, ErrorCallback errorCallback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> successCallback.onSuccess(""))
                .addOnFailureListener(e -> errorCallback.onError("Hiba: " + e.getMessage()));
    }

    public void updateGold(String userId, int amount, SuccessCallback callback, ErrorCallback errorCallback) {
        mFirestore.collection("Users")
                .document(userId)
                .update("gold", FieldValue.increment(amount))
                .addOnSuccessListener(aVoid -> callback.onSuccess("Sikeres aranyfrissítés"))
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void loadUsers(UsersLoadedCallback callback) {
        mFirestore.collection("Users").orderBy("username")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> usersList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        usersList.add(user);
                    }
                    callback.onUsersLoaded(usersList);
                });
    }


    public interface AdminStatusCallback {
        void onCheckAdmin(boolean isAdmin);
    }
    public interface SuccessCallback {
        void onSuccess(String message);
    }
    public interface ErrorCallback {
        void onError(String error);
    }
    public interface UsernamesLoadedCallback {
        void onUsernamesLoaded(String usernames);
    }

    public interface UserLoadedCallback {
        void onUserLoaded(User user);
    }
    public interface ProfilePictureLoadedCallback {
        void onProfilePictureLoaded(String url);
    }
    public interface UserProfileCallback {
        void onProfileLoaded(String username, String profilePicturePath);
    }

    public interface UploadCallback {
        void onUploadSuccess(String fileName);
    }

    public interface ProgressCallback {
        void onProgress(int progress);
    }
    public interface UsersLoadedCallback {
        void onUsersLoaded(List<User> users);
    }
}
