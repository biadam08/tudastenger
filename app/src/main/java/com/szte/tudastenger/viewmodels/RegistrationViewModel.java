package com.szte.tudastenger.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.szte.tudastenger.models.User;

public class RegistrationViewModel extends ViewModel {
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mUsers;

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registrationSuccess = new MutableLiveData<>();

    public RegistrationViewModel() {
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
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


        mUsers.whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            errorMessage.setValue("A megadott felhasználónévvel már létezik felhasználó");
                        } else {
                            createUserWithEmailAndPassword(username, email, password);
                        }
                    } else {
                        errorMessage.setValue("Hiba történt a regisztráció során");
                    }
                });
    }

    private void createUserWithEmailAndPassword(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(tokenTask -> {
                                    if (!tokenTask.isSuccessful()) {
                                        errorMessage.setValue("FCM token lekérése sikertelen!");
                                        return;
                                    }

                                    String fcmToken = tokenTask.getResult();
                                    User user = new User(uid, username, email, fcmToken);

                                    mUsers.document(uid).set(user)
                                            .addOnSuccessListener(aVoid -> {
                                                registrationSuccess.setValue(true);
                                            })
                                            .addOnFailureListener(e -> {
                                                errorMessage.setValue("Sikertelen regisztráció!");
                                            });
                                });
                    } else {
                        errorMessage.setValue("Regisztráció sikertelen!");
                    }
                });
    }
}