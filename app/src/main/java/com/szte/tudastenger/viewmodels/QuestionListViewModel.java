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

import java.util.List;

public class QuestionListViewModel extends AndroidViewModel {
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private MutableLiveData<List<Question>> questions = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public QuestionListViewModel(Application application) {
        super(application);
        questionRepository = new QuestionRepository();
        userRepository = new UserRepository();
        categoryRepository = new CategoryRepository();
    }

    public LiveData<List<Question>> getQuestions() { return questions; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void checkAdminAndLoadData() {
        userRepository.checkAdmin(isAdminStatus -> {
            isAdmin.setValue(isAdminStatus);
            if(isAdminStatus){
                loadQuestions();
                loadCategories();
            }
        }, error -> errorMessage.setValue(error));
    }

    private void loadQuestions() {
        questionRepository.loadAllQuestions(questionList -> questions.setValue(questionList), error -> errorMessage.setValue(error));
    }

    private void loadCategories() {
        categoryRepository.loadCategoriesWithAll(categoryList -> categories.setValue(categoryList), error -> errorMessage.setValue(error));
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
