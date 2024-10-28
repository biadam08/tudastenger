package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileViewModel extends AndroidViewModel {
    private FirebaseFirestore mFirestore;
    private FirebaseStorage mStorage;
    private FirebaseAuth mAuth;

    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<Map<String, String>> categoryScores = new MutableLiveData<>();
    private MutableLiveData<String> profilePictureUrl = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
        categoryScores.setValue(new HashMap<>());
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Map<String, String>> getCategoryScores() { return categoryScores; }
    public LiveData<String> getProfilePictureUrl() { return profilePictureUrl; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        mFirestore.collection("Users")
                .whereEqualTo("email", mAuth.getCurrentUser().getEmail())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        currentUser.setValue(user);
                        loadProfilePicture(user.getProfilePicture());
                    }
                    loadCategories();
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                });
    }

    private void loadProfilePicture(String profilePicturePath) {
        String imagePath = !profilePicturePath.isEmpty() ?
                "profile-pictures/" + profilePicturePath :
                "profile-pictures/default.jpg";

        mStorage.getReference()
                .child(imagePath)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> profilePictureUrl.setValue(uri.toString()))
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    private void loadCategories() {
        mFirestore.collection("Categories")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> categoryList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Category category = doc.toObject(Category.class);
                        categoryList.add(category);
                        loadCategoryScore(category);
                    }
                    categories.setValue(categoryList);
                })
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    private void loadCategoryScore(Category category) {
        User user = currentUser.getValue();
        if (user == null) return;

        mFirestore.collection("AnsweredQuestions")
                .whereEqualTo("userId", user.getId())
                .whereEqualTo("category", category.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int correctAnswers = 0;
                    int totalAnswers = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        totalAnswers++;
                        if (doc.getBoolean("correct")) {
                            correctAnswers++;
                        }
                    }
                    Map<String, String> scores = categoryScores.getValue();
                    if (scores == null) scores = new HashMap<>();
                    scores.put(category.getId(), correctAnswers + "/" + totalAnswers);
                    categoryScores.setValue(scores);
                })
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }
}