package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.security.crypto.MasterKeys;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.AuthRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends AndroidViewModel {
    private final AuthRepository authRepository;
    private SharedPreferences sharedPreferences;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canUseBiometric = new MutableLiveData<>();
    private final MutableLiveData<Pair<String, String>> savedCredentials = new MutableLiveData<>();

    @Inject
    public LoginViewModel(Application application, AuthRepository authRepository) {
        super(application);
        this.authRepository = authRepository;

        try {
            MasterKey masterKey = new MasterKey.Builder(application)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    application,
                    "secret_shared_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize EncryptedSharedPreferences", e);
        }
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }
    public LiveData<Boolean> getCanUseBiometric() { return canUseBiometric; }
    public LiveData<Pair<String, String>> getSavedCredentials() {  return savedCredentials; }

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
                () -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("email", email);
                    editor.putString("password", password);
                    editor.apply();

                    loginSuccess.setValue(true);
                },
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

    public void checkBiometricAvailability(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        boolean canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;

        String savedEmail = sharedPreferences.getString("email", "");
        String savedPassword = sharedPreferences.getString("password", "");

        canUseBiometric.setValue(canAuthenticate && !savedEmail.isEmpty() && !savedPassword.isEmpty());
    }

    public void loadSavedCredentials() {
        String savedEmail = sharedPreferences.getString("email", "");
        String savedPassword = sharedPreferences.getString("password", "");
        savedCredentials.setValue(new Pair<>(savedEmail, savedPassword));
    }
}