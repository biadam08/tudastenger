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
import com.szte.tudastenger.repositories.AnsweredQuestionsRepository;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AnsweredQuestionsRepository answeredQuestionsRepository;

    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<Map<String, String>> categoryScores = new MutableLiveData<>();
    private MutableLiveData<String> profilePictureUrl = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileViewModel(Application application) {
        super(application);
        userRepository = new UserRepository();
        categoryRepository = new CategoryRepository();
        answeredQuestionsRepository = new AnsweredQuestionsRepository();
        categoryScores.setValue(new HashMap<>());
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Map<String, String>> getCategoryScores() { return categoryScores; }
    public LiveData<String> getProfilePictureUrl() { return profilePictureUrl; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadUserData() {
        userRepository.loadCurrentUser(
                user -> {
                    currentUser.setValue(user);
                    loadProfilePicture(user.getProfilePicture());
                    loadCategories();
                }
        );

    }

    private void loadProfilePicture(String profilePicturePath) {
        userRepository.loadProfilePicture( profilePicturePath, url -> profilePictureUrl.setValue(url), error -> errorMessage.setValue(error));
    }

    private void loadCategories() {
        categoryRepository.loadCategories(
                categoryList -> {
                    categories.setValue((ArrayList<Category>) categoryList);
                    for (Category category : categoryList) {
                        loadCategoryScore(category);
                    }
                },
                error -> errorMessage.setValue(error)
        );
    }

    private void loadCategoryScore(Category category) {
        User user = currentUser.getValue();
        if (user == null) return;

        answeredQuestionsRepository.loadCategoryScore(
                user.getId(),
                category.getId(),
                (categoryId, correct, total) -> {
                    Map<String, String> scores = categoryScores.getValue();
                    if (scores == null) scores = new HashMap<>();
                    scores.put(categoryId, correct + "/" + total);
                    categoryScores.setValue(scores);
                },
                error -> errorMessage.setValue(error)
        );
    }
}