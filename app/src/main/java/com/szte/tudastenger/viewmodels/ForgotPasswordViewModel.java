package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordViewModel extends AndroidViewModel {
    private final FirebaseAuth mAuth;

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSuccess= new MutableLiveData<>();

    public ForgotPasswordViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
    }

    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getResetSuccess() { return isSuccess; }

    public void resetPassword(String email) {
        if (email.trim().isEmpty()) {
            errorMessage.setValue("Add meg az e-mail címedet!");
            return;
        }

        mAuth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> {
                    isSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Hiba: " + e.getMessage());
                });
    }
}
