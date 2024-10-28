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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final FirebaseUser mUser;
    private final CollectionReference mUsers;
    private final CollectionReference mQuestions;
    private final CollectionReference mAnsweredQuestions;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<AnsweredQuestion>> answeredQuestions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNoQuestions = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private ArrayList<AnsweredQuestion> mAnsweredQuestionsData = new ArrayList<>();

    public HistoryViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUsers = mFirestore.collection("Users");
        mQuestions = mFirestore.collection("Questions");
        mAnsweredQuestions = mFirestore.collection("AnsweredQuestions");

        initializeUser();
    }

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<AnsweredQuestion>> getAnsweredQuestions() { return answeredQuestions; }
    public LiveData<Boolean> getShowNoQuestions() { return showNoQuestions; }
    public LiveData<Question> getQuestionDetails(String questionId) {
        MutableLiveData<Question> questionData = new MutableLiveData<>();

        mQuestions.document(questionId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot questionDoc = task.getResult();
                        if (questionDoc.exists()) {
                            Question question = new Question();
                            question.setQuestionText(questionDoc.getString("questionText"));
                            question.setAnswers((ArrayList<String>) questionDoc.get("answers"));
                            question.setCorrectAnswerIndex(questionDoc.getLong("correctAnswerIndex").intValue());
                            questionData.setValue(question);
                        }
                    }
                });

        return questionData;
    }


    private void initializeUser() {
        if (mUser != null) {
            mUsers.whereEqualTo("email", mUser.getEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            User currentUser = doc.toObject(User.class);
                            currentUserId.setValue(currentUser.getId());
                        }
                        loadCategories();
                    });
        }
    }

    private void loadCategories() {
        List<Category> categoryList = new ArrayList<>();
        categoryList.add(new Category("0", "Összes kategória", null));

        mFirestore.collection("Categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        category.setId(document.getId());
                        categoryList.add(category);
                    }
                    categories.setValue(categoryList);
                });
    }

    public void onCategorySelected(String category) {
        queryAnsweredQuestions(category);
    }

    private void queryAnsweredQuestions(String selectedCategory) {
        mAnsweredQuestionsData.clear();
        answeredQuestions.setValue(new ArrayList<>());

        mAnsweredQuestions
                .whereEqualTo("userId", currentUserId.getValue())
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            showNoQuestions.setValue(true);
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String questionId = document.getString("questionId");
                                String answer = document.getString("answer");
                                boolean correct = document.getBoolean("correct");
                                Timestamp timestamp = document.getTimestamp("date");

                                mQuestions.document(questionId)
                                        .get()
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                DocumentSnapshot questionDoc = task2.getResult();
                                                if (questionDoc.exists()) {
                                                    String categoryId = questionDoc.getString("category");

                                                    mFirestore.collection("Categories")
                                                            .document(categoryId)
                                                            .get()
                                                            .addOnSuccessListener(categoryDoc -> {
                                                                if (categoryDoc.exists()) {
                                                                    String categoryName = categoryDoc.getString("name");
                                                                    if (selectedCategory.equals("Összes kategória") ||
                                                                            selectedCategory.equals(categoryName)) {
                                                                        AnsweredQuestion answeredQuestion = new AnsweredQuestion(
                                                                                questionId, categoryName,
                                                                                currentUserId.getValue(),
                                                                                timestamp, answer, correct
                                                                        );
                                                                        mAnsweredQuestionsData.add(answeredQuestion);
                                                                    }

                                                                    // a legújabb kitöltés legyen legfelül
                                                                    mAnsweredQuestionsData.sort((q1, q2) ->
                                                                            q2.getDate().compareTo(q1.getDate()));

                                                                    // lista frissítése, nincs kérdés kezelése
                                                                    answeredQuestions.setValue(mAnsweredQuestionsData);
                                                                    showNoQuestions.setValue(mAnsweredQuestionsData.isEmpty());
                                                                }
                                                            });
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });
    }
}
