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

import java.util.UUID;

public class EditProfileViewModel extends AndroidViewModel {
    private final FirebaseAuth mAuth;
    private final FirebaseUser mUser;
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mUsers;
    private final FirebaseStorage mStorage;
    private final StorageReference mStorageReference;

    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> profilePictureUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>();
    private final MutableLiveData<Boolean> accountDeleted = new MutableLiveData<>();

    public EditProfileViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
        mStorage = FirebaseStorage.getInstance();
        mStorageReference = mStorage.getReference();
    }

    public LiveData<String> getUsername() { return username; }
    public LiveData<String> getProfilePictureUrl() { return profilePictureUrl; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Integer> getUploadProgress() { return uploadProgress; }
    public LiveData<Boolean> getAccountDeleted() { return accountDeleted; }

    public void loadUserProfile() {
        if (mUser != null && mUser.getEmail() != null) {
            isLoading.setValue(true);
            DocumentReference userDocRef = mUsers.document(mUser.getUid());

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    username.setValue(documentSnapshot.getString("username"));
                    loadProfilePicture(documentSnapshot.getString("profilePicture"));
                }
                isLoading.setValue(false);
            }).addOnFailureListener(e -> {
                errorMessage.setValue("Nem sikerült betölteni a profilt");
                isLoading.setValue(false);
            });
        }
    }

    private void loadProfilePicture(String profilePicturePath) {
        String imagePath = !profilePicturePath.equals("") ?
                "profile-pictures/" + profilePicturePath :
                "profile-pictures/default.jpg";

        mStorageReference.child(imagePath).getDownloadUrl()
                .addOnSuccessListener(uri -> profilePictureUrl.setValue(uri.toString()))
                .addOnFailureListener(e -> errorMessage.setValue("Nem sikerült betölteni a képet"));
    }

    public void uploadProfilePicture(Uri imageUri) {
        if (imageUri != null) {
            isLoading.setValue(true);
            String fileName = UUID.randomUUID().toString();
            StorageReference ref = mStorageReference.child("profile-pictures/" + fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        DocumentReference userDocRef = mUsers.document(mUser.getUid());
                        userDocRef.update("profilePicture", fileName)
                                .addOnSuccessListener(aVoid -> {
                                    loadProfilePicture(fileName);
                                    isLoading.setValue(false);
                                    successMessage.setValue("Sikeres profilkép feltöltés");
                                });
                    })
                    .addOnFailureListener(e -> {
                        isLoading.setValue(false);
                        errorMessage.setValue("Hiba történt a feltöltés során");
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        uploadProgress.setValue((int) progress);
                    });
        }
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        if (mUser == null || mUser.getEmail() == null) {
            errorMessage.setValue("Nincs bejelentkezett felhasználó");
            return;
        }

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
        AuthCredential credential = EmailAuthProvider.getCredential(mUser.getEmail(), currentPassword);

        mUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid ->
                        mUser.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid2 -> {
                                    isLoading.setValue(false);
                                    successMessage.setValue("A jelszavadat sikeresen megváltoztattad!");
                                })
                                .addOnFailureListener(e -> {
                                    isLoading.setValue(false);
                                    errorMessage.setValue("A jelszavad megváltoztatása közben hiba lépett fel!");
                                })
                )
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Az általad beírt jelenlegi jelszó nem egyezik meg a jelszavaddal!");
                });
    }

    public void deleteAccount() {
        isLoading.setValue(true);
        DocumentReference userDocRef = mUsers.document(mUser.getUid());

        userDocRef.delete()
                .addOnSuccessListener(aVoid ->
                        mUser.delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    isLoading.setValue(false);
                                    accountDeleted.setValue(true);
                                })
                                .addOnFailureListener(e -> {
                                    isLoading.setValue(false);
                                    errorMessage.setValue("A fiók törlése nem sikerült!");
                                })
                )
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("A fiók törlése nem sikerült!");
                });
    }
}
