package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.QuestionRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SavedQuestionListViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Question>> questions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNoQuestions = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();

    @Inject
    public SavedQuestionListViewModel(Application application, UserRepository userRepository, QuestionRepository questionRepository, CategoryRepository categoryRepository) {
        super(application);
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
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
