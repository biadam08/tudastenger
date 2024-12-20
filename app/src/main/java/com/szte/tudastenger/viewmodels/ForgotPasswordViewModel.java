package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.szte.tudastenger.repositories.UserRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ForgotPasswordViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSuccess= new MutableLiveData<>();

    @Inject
    public ForgotPasswordViewModel(Application application, UserRepository userRepository) {
        super(application);
        this.userRepository = userRepository;
    }

    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getResetSuccess() { return isSuccess; }

    public void resetPassword(String email) {
        if (email.trim().isEmpty()) {
            errorMessage.setValue("Add meg az e-mail címedet!");
            return;
        }

        userRepository.resetPassword(
                email.trim(),
                success -> isSuccess.setValue(true),
                error -> errorMessage.setValue(error)
        );
    }

}
