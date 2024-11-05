package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RegistrationViewModel extends AndroidViewModel {
    private final AuthRepository authRepository;

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registrationSuccess = new MutableLiveData<>();

    @Inject
    public RegistrationViewModel(Application application, AuthRepository authRepository) {
        super(application);
        this.authRepository = authRepository;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getRegistrationSuccess() {
        return registrationSuccess;
    }

    public void register(String username, String email, String password, String passwordAgain) {
        // Ellenőrzés, hogy a felhasználónév mező nem üres-e
        if (username.trim().isEmpty()) {
            errorMessage.setValue("A felhasználónév mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy az email mező nem üres-e
        if (email.trim().isEmpty()) {
            errorMessage.setValue("Az email mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy az email formátum helyes-e
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.setValue("Hibás email formátum");
            return;
        }

        // Ellenőrzés, hogy a jelszó mező nem üres-e
        if (password.trim().isEmpty()) {
            errorMessage.setValue("A jelszó mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy a megerősítő jelszó mező nem üres-e
        if (passwordAgain.trim().isEmpty()) {
            errorMessage.setValue("A megerősítő jelszó mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy a két jelszó megegyezik-e
        if (!password.equals(passwordAgain)) {
            errorMessage.setValue("A két jelszó nem egyezik");
            return;
        }


        authRepository.checkUsernameAvailability(
                username,
                isAvailable -> {
                    if (isAvailable) {
                        authRepository.registerUser( username, email, password,
                                () -> registrationSuccess.setValue(true),
                                error -> errorMessage.setValue(error)
                        );
                    } else {
                        errorMessage.setValue("A megadott felhasználónévvel már létezik felhasználó");
                    }
                },
                error -> errorMessage.setValue(error)
        );

    }
}