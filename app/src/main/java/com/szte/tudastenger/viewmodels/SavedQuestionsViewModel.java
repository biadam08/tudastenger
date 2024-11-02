package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.QuestionRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class SavedQuestionsViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Question>> questions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNoQuestions = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();

    public SavedQuestionsViewModel(Application application) {
        super(application);
        userRepository = new UserRepository();
        questionRepository = new QuestionRepository();
        categoryRepository = new CategoryRepository();
        initializeUser();
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<Question>> getQuestions() {
        return questions;
    }

    public LiveData<Boolean> getShowNoQuestions() {
        return showNoQuestions;
    }

    public LiveData<String> getCurrentUserId() {
        return currentUserId;
    }

    private void initializeUser() {
        userRepository.loadCurrentUser(
                user -> {
                    currentUserId.setValue(user.getId());
                    loadCategories();
                }
        );
    }


    private void loadCategories() {
        categoryRepository.loadCategoriesWithAll(
                categoryList -> {
                    categories.setValue(categoryList);
                    queryQuestions("Összes kategória");
                },
                error -> {}
        );
    }

    public void onCategorySelected(String category) {
        queryQuestions(category);
    }

    private void queryQuestions(String selectedCategory) {
        questions.setValue(new ArrayList<>());
        String userId = currentUserId.getValue();

        questionRepository.loadSavedQuestions(
                userId,
                selectedCategory,
                questionsList -> {
                    questions.setValue(questionsList);
                    showNoQuestions.setValue(questionsList.isEmpty());
                },
                () -> showNoQuestions.setValue(true),
                error -> {}
        );
    }


    public void deleteQuestion(Question question) {
        String userId = currentUserId.getValue();

        questionRepository.deleteSavedQuestion(userId, question.getId(),
                () -> {
                    List<Question> currentQuestions = questions.getValue();
                    if (currentQuestions != null) {
                        currentQuestions.remove(question);
                        questions.setValue(currentQuestions);
                        showNoQuestions.setValue(currentQuestions.isEmpty());
                    }
                }
        );
    }


}
