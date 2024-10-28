package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.szte.tudastenger.models.User;

public class LoginViewModel extends AndroidViewModel {
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mUsers;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();

    public LoginViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loginSuccess.setValue(true);
        }
    }

    public void login(String email, String password) {
        // Input validation
        if (email.trim().isEmpty()) {
            errorMessage.setValue("Az email mező nem lehet üres");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.setValue("Hibás email formátum");
            return;
        }

        if (password.trim().isEmpty()) {
            errorMessage.setValue("A jelszó mező nem lehet üres");
            return;
        }


        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        loginSuccess.setValue(true);
                    } else {
                        errorMessage.setValue("Sikertelen belépés!");
                    }
                });
    }

    public void handleGoogleSignIn(AuthCredential credential) {

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            handleGoogleUser(firebaseUser);
                        }
                    } else {
                        errorMessage.setValue("Hiba a bejelentkezés során: " + task.getException().getMessage());
                    }
                });
    }

    private void handleGoogleUser(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String displayName = firebaseUser.getDisplayName();

        mUsers.document(uid).get()
                .addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        if (!userTask.getResult().exists()) {
                            createNewGoogleUser(uid, displayName, email);
                        } else {
                            loginSuccess.setValue(true);
                        }
                    } else {
                        errorMessage.setValue("Hiba a felhasználó ellenőrzése során");
                    }
                });
    }

    private void createNewGoogleUser(String uid, String displayName, String email) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(fcmTask -> {
                    if (fcmTask.isSuccessful()) {
                        String fcmToken = fcmTask.getResult();
                        User user = new User(uid, displayName, email, fcmToken);

                        mUsers.document(uid).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    loginSuccess.setValue(true);
                                })
                                .addOnFailureListener(e -> {
                                    errorMessage.setValue("Hiba a felhasználó létrehozása során");
                                });
                    } else {
                        errorMessage.setValue("Hiba az FCM token lekérése során");
                    }
                });
    }
}
