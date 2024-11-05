package com.szte.tudastenger.repositories;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.szte.tudastenger.models.User;

import javax.inject.Inject;

public class AuthRepository {
    private final FirebaseAuth mAuth;
    private final CollectionReference mUsers;
    private final FirebaseMessaging mFirebaseMessaging;

    @Inject
    public AuthRepository(FirebaseAuth auth, FirebaseFirestore firestore, FirebaseMessaging firebaseMessaging) {
        this.mAuth = auth;
        this.mUsers = firestore.collection("Users");
        this.mFirebaseMessaging = firebaseMessaging;
    }

    public void checkCurrentUser(AuthStateCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        callback.onAuthStateChecked(currentUser != null);
    }

    public void login(String email, String password, AuthCallback authCallback, ErrorCallback errorCallback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        authCallback.onAuthSuccess();
                    } else {
                        errorCallback.onError("Sikertelen belépés!");
                    }
                });
    }

    public void signInWithGoogle(AuthCredential credential, AuthCallback authCallback, ErrorCallback errorCallback) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            handleGoogleUser(firebaseUser, authCallback, errorCallback);
                        }
                    } else {
                        errorCallback.onError("Hiba a bejelentkezés során: " + task.getException().getMessage());
                    }
                });
    }

    private void handleGoogleUser(FirebaseUser firebaseUser, AuthCallback authCallback, ErrorCallback errorCallback) {
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String displayName = firebaseUser.getDisplayName();

        mUsers.document(uid).get()
                .addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        if (!userTask.getResult().exists()) {
                            createNewGoogleUser(uid, displayName, email, authCallback, errorCallback);
                        } else {
                            authCallback.onAuthSuccess();
                        }
                    } else {
                        errorCallback.onError("Hiba a felhasználó ellenőrzése során");
                    }
                });
    }

    private void createNewGoogleUser(String uid, String displayName, String email, AuthCallback authCallback,  ErrorCallback errorCallback) {
        mFirebaseMessaging.getToken()
                .addOnCompleteListener(fcmTask -> {
                    if (fcmTask.isSuccessful()) {
                        String fcmToken = fcmTask.getResult();
                        User user = new User(uid, displayName, email, fcmToken);

                        mUsers.document(uid).set(user)
                                .addOnSuccessListener(aVoid -> authCallback.onAuthSuccess())
                                .addOnFailureListener(e ->
                                        errorCallback.onError("Hiba a felhasználó létrehozása során"));
                    } else {
                        errorCallback.onError("Hiba az FCM token lekérése során");
                    }
                });
    }


    public void checkUsernameAvailability(String username, UsernameCheckCallback usernameCallback, ErrorCallback errorCallback) {
        mUsers.whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isAvailable = task.getResult().isEmpty();
                        usernameCallback.onUsernameChecked(isAvailable);
                    } else {
                        errorCallback.onError("Hiba történt a felhasználónév ellenőrzése során");
                    }
                });
    }

    public void registerUser(String username, String email, String password, AuthCallback authCallback, ErrorCallback errorCallback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();
                        mFirebaseMessaging.getToken()
                                .addOnCompleteListener(tokenTask -> {
                                    if (tokenTask.isSuccessful()) {
                                        String fcmToken = tokenTask.getResult();
                                        User user = new User(uid, username, email, fcmToken);

                                        mUsers.document(uid).set(user)
                                                .addOnSuccessListener(aVoid -> authCallback.onAuthSuccess())
                                                .addOnFailureListener(e ->
                                                        errorCallback.onError("Sikertelen regisztráció!"));
                                    } else {
                                        errorCallback.onError("FCM token lekérése sikertelen!");
                                    }
                                });
                    } else {
                        errorCallback.onError("Regisztráció sikertelen!");
                    }
                });
    }

    public interface AuthStateCallback {
        void onAuthStateChecked(boolean isLoggedIn);
    }

    public interface AuthCallback {
        void onAuthSuccess();
    }

    public interface ErrorCallback {
        void onError(String message);
    }

    public interface UsernameCheckCallback {
        void onUsernameChecked(boolean isAvailable);
    }
}