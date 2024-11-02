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
import com.szte.tudastenger.repositories.AuthRepository;

public class LoginViewModel extends AndroidViewModel {
    private final AuthRepository authRepository;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();

    public LoginViewModel(Application application) {
        super(application);
        authRepository = new AuthRepository();
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public void checkCurrentUser() {
        authRepository.checkCurrentUser(isLoggedIn -> loginSuccess.setValue(isLoggedIn));
    }

    public void login(String email, String password) {
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

        authRepository.login(
                email,
                password,
                () -> loginSuccess.setValue(true),
                error -> errorMessage.setValue(error)
        );
    }

    public void handleGoogleSignIn(AuthCredential credential) {
        authRepository.signInWithGoogle(
                credential,
                () -> loginSuccess.setValue(true),
                error -> errorMessage.setValue(error)
        );
    }
}