package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EditProfileViewModel extends AndroidViewModel {
    private final UserRepository userRepository;

    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> profilePictureUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>();
    private final MutableLiveData<Boolean> accountDeleted = new MutableLiveData<>();

    @Inject
    public EditProfileViewModel(Application application, UserRepository userRepository) {
        super(application);
        this.userRepository = userRepository;
    }

    public LiveData<String> getUsername() { return username; }
    public LiveData<String> getProfilePictureUrl() { return profilePictureUrl; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Integer> getUploadProgress() { return uploadProgress; }
    public LiveData<Boolean> getAccountDeleted() { return accountDeleted; }

    public void loadUserProfile() {
        isLoading.setValue(true);
        userRepository.loadUserProfile(
                (userName, profilePicturePath) -> {
                    username.setValue(userName);
                    loadProfilePicture(profilePicturePath);
                    isLoading.setValue(false);
                },
                error -> {
                    errorMessage.setValue(error);
                    isLoading.setValue(false);
                }
        );
    }

    private void loadProfilePicture(String profilePicturePath) {
        userRepository.loadProfilePicture(
                profilePicturePath,
                url -> profilePictureUrl.setValue(url),
                error -> errorMessage.setValue(error)
        );
    }

    public void uploadProfilePicture(Uri imageUri) {
        if (imageUri != null) {
            isLoading.setValue(true);
            userRepository.uploadProfilePicture(
                    imageUri,
                    fileName -> {
                        loadProfilePicture(fileName);
                        isLoading.setValue(false);
                        successMessage.setValue("Sikeres profilkép feltöltés");
                    },
                    progress -> uploadProgress.setValue(progress),
                    error -> {
                        errorMessage.setValue(error);
                        isLoading.setValue(false);
                    }
            );
        }
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.trim().isEmpty()) {
            errorMessage.setValue("A jelszómódosításhoz add meg a jelenlegi jelszavadat!");
            return;
        }

        if (newPassword.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            errorMessage.setValue("A jelszómódosításhoz add meg az új jelszót!");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            errorMessage.setValue("Az új jelszó és annak megerősítése nem egyezik!");
            return;
        }

        isLoading.setValue(true);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            userRepository.changePassword(
                    user.getEmail(),
                    currentPassword,
                    newPassword,
                    success -> {
                        isLoading.setValue(false);
                        successMessage.setValue(success);
                    },
                    error -> {
                        errorMessage.setValue(error);
                        isLoading.setValue(false);
                    }
            );
        } else {
            errorMessage.setValue("Nincs bejelentkezett felhasználó");
            isLoading.setValue(false);
        }
    }

    public void deleteAccount() {
        isLoading.setValue(true);
        userRepository.deleteAccount(
                success -> {
                    isLoading.setValue(false);
                    successMessage.setValue(success);
                    accountDeleted.setValue(true);
                },
                error -> {
                    errorMessage.setValue(error);
                    isLoading.setValue(false);
                }
        );
    }
}
