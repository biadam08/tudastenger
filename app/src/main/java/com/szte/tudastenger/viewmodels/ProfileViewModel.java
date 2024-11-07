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

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ProfileViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AnsweredQuestionsRepository answeredQuestionsRepository;

    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<Map<String, String>> categoryScores = new MutableLiveData<>();
    private MutableLiveData<String> profilePictureUrl = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> userRank = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(Application application, UserRepository userRepository, CategoryRepository categoryRepository, AnsweredQuestionsRepository answeredQuestionsRepository) {
        super(application);
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.answeredQuestionsRepository = answeredQuestionsRepository;
        categoryScores.setValue(new HashMap<>());
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Map<String, String>> getCategoryScores() { return categoryScores; }
    public LiveData<String> getProfilePictureUrl() { return profilePictureUrl; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getUserRank() { return userRank; }

    public void loadUserData() {
        userRepository.loadCurrentUser(
                user -> {
                    currentUser.setValue(user);
                    loadProfilePicture(user.getProfilePicture());
                    loadUserRank();
                    loadCategories();
                }
        );
    }

    private void loadUserRank() {
        userRepository.getUserRank(
                currentUser.getValue().getGold(),
                rank -> userRank.setValue(rank),
                error -> userRank.setValue(error)
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