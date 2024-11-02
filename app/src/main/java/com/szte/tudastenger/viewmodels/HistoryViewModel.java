package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.AnsweredQuestionsRepository;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.QuestionRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final AnsweredQuestionsRepository answeredQuestionsRepository;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<AnsweredQuestion>> answeredQuestions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNoQuestions = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private ArrayList<AnsweredQuestion> mAnsweredQuestionsData = new ArrayList<>();

    public HistoryViewModel(Application application) {
        super(application);
        userRepository = new UserRepository();
        categoryRepository = new CategoryRepository();
        questionRepository = new QuestionRepository();
        answeredQuestionsRepository = new AnsweredQuestionsRepository();

        initializeUser();
    }

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<AnsweredQuestion>> getAnsweredQuestions() { return answeredQuestions; }
    public LiveData<Boolean> getShowNoQuestions() { return showNoQuestions; }
    public LiveData<Question> getQuestionDetails(String questionId) {
        MutableLiveData<Question> questionData = new MutableLiveData<>();

        answeredQuestionsRepository.getQuestionDetails(
                questionId,
                data -> questionData.setValue(data)
        );

        return questionData;
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
                categoryList -> categories.setValue(categoryList),
                error -> {}
        );
    }

    public void queryAnsweredQuestions(String category) {
        mAnsweredQuestionsData.clear();
        answeredQuestions.setValue(new ArrayList<>());

        String userId = currentUserId.getValue();
        if (userId != null) {
            answeredQuestionsRepository.loadAnsweredQuestions(
                    userId,
                    category,
                    answeredQuestionsList -> {
                        answeredQuestions.setValue(answeredQuestionsList);
                        showNoQuestions.setValue(false);
                    },
                    () -> showNoQuestions.setValue(true),
                    error -> {}
            );
        }
    }
}
