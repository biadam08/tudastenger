package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.QuestionRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class QuestionEditUploadViewModel extends AndroidViewModel {
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> successMessage = new MutableLiveData<>();
    private MutableLiveData<String> explanationText = new MutableLiveData<>();
    private MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private MutableLiveData<String> imageUrl = new MutableLiveData<>();
    private MutableLiveData<String> existingImageName = new MutableLiveData<>();
    private MutableLiveData<Boolean> isImageUploading = new MutableLiveData<>();
    private MutableLiveData<Integer> uploadProgress = new MutableLiveData<>();
    private MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();

    @Inject
    public QuestionEditUploadViewModel(Application application, QuestionRepository questionRepository, UserRepository userRepository, CategoryRepository categoryRepository) {
        super(application);
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Question> getCurrentQuestion() { return currentQuestion; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<String> getExplanationText() { return explanationText; }
    public LiveData<Uri> getImageUri() { return imageUri; }
    public LiveData<String> getImageUrl() { return imageUrl; }
    public LiveData<String> getExistingImageName() { return existingImageName; }
    public LiveData<Boolean> getIsImageUploading() { return isImageUploading; }
    public LiveData<Integer> getUploadProgress() { return uploadProgress; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }

    public void checkAdmin() {
        userRepository.checkAdmin(isAdminStatus -> isAdmin.setValue(isAdminStatus), error -> errorMessage.setValue(error));
    }

    public void loadCategories() {
        categoryRepository.loadCategories(categoryList -> categories.setValue(categoryList), error -> errorMessage.setValue(error));
    }

    public void loadQuestionData(String questionId) {
        questionRepository.loadQuestionData(questionId, question -> {
                    currentQuestion.setValue(question);
                    explanationText.setValue(question.getExplanationText());
                    existingImageName.setValue(question.getImage());
                },
                url -> imageUrl.setValue(url), error -> errorMessage.setValue(error)
        );
    }

    public void uploadQuestion(Question question, Uri imageUri) {
        isImageUploading.setValue(true);
        questionRepository.uploadQuestionWithImage(question, imageUri,
                message -> {
                    successMessage.setValue(message);
                    isImageUploading.setValue(false);
                    updateSuccess.setValue(true);
                },
                error -> {
                    errorMessage.setValue(error);
                    isImageUploading.setValue(false);
                },
                progress -> uploadProgress.setValue(progress), updatedQuestion -> currentQuestion.setValue(updatedQuestion)
        );
    }
    public void deleteQuestion(String questionId) {
        questionRepository.deleteQuestion(questionId,
                message -> {
                    successMessage.setValue(message);
                    deleteSuccess.setValue(true);
                },
                error -> errorMessage.setValue(error)
        );
    }

    public void generateExplanation(String questionText, String correctAnswer) {
        questionRepository.generateExplanation(questionText, correctAnswer, explanation -> explanationText.setValue(explanation), error -> errorMessage.setValue(error)
        );
    }

    public void setImageUri(Uri uri) {
        imageUri.setValue(uri);
    }

    public void clearImageUri() {
        imageUri.setValue(null);
        imageUrl.setValue(null);
    }

    public void setExplanationText(String text) {
        explanationText.setValue(text);
    }
}