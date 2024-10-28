package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;
import java.util.List;

public class QuestionListViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final FirebaseAuth mAuth;
    private MutableLiveData<List<Question>> questions = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public QuestionListViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public LiveData<List<Question>> getQuestions() { return questions; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void checkAdminAndLoadData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            isAdmin.setValue(false);
            return;
        }

        loadQuestions();
        loadCategories();
    }

    private void loadQuestions() {
        mFirestore.collection("Questions")
                .orderBy("questionText")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Question> questionList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Question question = document.toObject(Question.class);
                        questionList.add(question);
                    }
                    questions.setValue(questionList);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                });
    }

    private void loadCategories() {
        mFirestore.collection("Categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> categoryList = new ArrayList<>();
                    categoryList.add(new Category("0", "Összes kategória", null));

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        category.setId(document.getId());
                        categoryList.add(category);
                    }
                    categories.setValue(categoryList);
                })
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    public String getCategoryIdByName(String categoryName) {
        if ("Összes kategória".equals(categoryName)) {
            return "0";
        }

        List<Category> categoryList = categories.getValue();
        if (categoryList != null) {
            for (Category category : categoryList) {
                if (category.getName().equals(categoryName)) {
                    return category.getId();
                }
            }
        }
        return null;
    }
}