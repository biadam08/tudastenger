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

import java.util.ArrayList;
import java.util.List;

public class SavedQuestionsViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final FirebaseUser mUser;
    private final CollectionReference mUsers;
    private final CollectionReference mQuestions;
    private final CollectionReference mSavedQuestions;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Question>> questions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNoQuestions = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();

    public SavedQuestionsViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUsers = mFirestore.collection("Users");
        mQuestions = mFirestore.collection("Questions");
        mSavedQuestions = mFirestore.collection("SavedQuestions");

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
        if (mUser != null) {
            mUsers.whereEqualTo("email", mUser.getEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            User currentUser = doc.toObject(User.class);
                            currentUserId.setValue(currentUser.getId());
                            loadCategories();
                        }
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
                    queryQuestions("Összes kategória");
                });
    }

    public void onCategorySelected(String category) {
        queryQuestions(category);
    }

    private void queryQuestions(String selectedCategory) {
        questions.setValue(new ArrayList<>());
        ArrayList<Question> questionsList = new ArrayList<>();

        mSavedQuestions.whereEqualTo("userId", currentUserId.getValue())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            showNoQuestions.setValue(true);
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String questionId = document.getString("questionId");
                                loadQuestionDetails(questionId, selectedCategory, questionsList);
                            }
                        }
                    }
                });
    }

    private void loadQuestionDetails(String questionId, String selectedCategory, List<Question> questionsList) {
        mQuestions.document(questionId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot questionDoc = task.getResult();
                        String categoryId = questionDoc.getString("category");

                        mFirestore.collection("Categories")
                                .document(categoryId)
                                .get()
                                .addOnSuccessListener(categoryDoc -> {
                                    if (categoryDoc.exists()) {
                                        String categoryName = categoryDoc.getString("name");
                                        if (selectedCategory.equals("Összes kategória") || selectedCategory.equals(categoryName)) {
                                            Question question = new Question(questionId,
                                                    questionDoc.getString("questionText"),
                                                    categoryName,
                                                    (ArrayList<String>) questionDoc.get("answers"),
                                                    questionDoc.getLong("correctAnswerIndex").intValue(),
                                                    questionDoc.getString("image"),
                                                    questionDoc.getString("explanationText")
                                            );
                                            questionsList.add(question);
                                            updateQuestionsList(questionsList, selectedCategory);
                                        } else {
                                            updateQuestionsList(questionsList, selectedCategory);
                                        }
                                    }
                                });
                    }
                });
    }

    private void updateQuestionsList(List<Question> questionsList, String selectedCategory) {
        questions.setValue(questionsList);
        if (!selectedCategory.equals("Összes kategória") && questionsList.isEmpty()) {
            showNoQuestions.setValue(true);
        } else {
            showNoQuestions.setValue(questionsList.isEmpty());
        }
    }

    public void deleteQuestion(Question question) {
        mSavedQuestions.whereEqualTo("userId", currentUserId.getValue())
                .whereEqualTo("questionId", question.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            mSavedQuestions.document(documentSnapshot.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        List<Question> currentQuestions = questions.getValue();
                                        if (currentQuestions != null) {
                                            currentQuestions.remove(question);
                                            questions.setValue(currentQuestions);
                                            showNoQuestions.setValue(currentQuestions.isEmpty());
                                        }
                                    });
                        }
                    }
                });
    }
}
