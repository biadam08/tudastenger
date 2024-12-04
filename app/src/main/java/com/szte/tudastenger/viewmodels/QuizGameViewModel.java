package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.QuestionRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class QuizGameViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isQuestionSaved = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> savedQuestionId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isHelpUsed = new MutableLiveData<>(false);
    private final MutableLiveData<String> explanationText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAnswerSelected = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<Integer> hiddenAnswerIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<Integer> correctAnswerIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<Integer> selectedAnswerIndex = new MutableLiveData<>(-1);


    @Inject
    public QuizGameViewModel(Application application, UserRepository userRepository, QuestionRepository questionRepository, CategoryRepository categoryRepository) {
        super(application);
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        isQuestionSaved.setValue(false);
    }

    public LiveData<Question> getCurrentQuestion() { return currentQuestion; }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<Boolean> getIsQuestionSaved() { return isQuestionSaved; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsHelpUsed() { return isHelpUsed; }
    public LiveData<String> getExplanationText() { return explanationText; }
    public LiveData<Boolean> getIsAnswerSelected() { return isAnswerSelected; }
    public LiveData<Uri> getImageUri() { return imageUri; }

    public LiveData<Integer> getHiddenAnswerIndex() { return hiddenAnswerIndex; }

    public void setHiddenAnswerIndex(int index) { hiddenAnswerIndex.setValue(index); }
    public LiveData<Integer> getCorrectAnswerIndex() { return correctAnswerIndex; }

    public LiveData<Integer> getSelectedAnswerIndex() { return selectedAnswerIndex; }

    public void setCorrectAnswerIndex(int index) { correctAnswerIndex.setValue(index); }

    public void setSelectedAnswerIndex(int index) { selectedAnswerIndex.setValue(index); }

    public void loadCurrentUser() {
        userRepository.loadCurrentUser(user -> currentUser.setValue(user));
    }

    public void loadQuestionImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageUri.setValue(null);
            return;
        }

        questionRepository.loadQuestionImage(
                imagePath,
                uri -> imageUri.setValue(Uri.parse(uri)),
                error -> {
                    errorMessage.setValue(error);
                    imageUri.setValue(null);
                }
        );
    }

    public void queryRandomQuestion(String categoryId, boolean isMixed) {
        hiddenAnswerIndex.setValue(-1);
        correctAnswerIndex.setValue(-1);
        selectedAnswerIndex.setValue(-1);
        imageUri.setValue(null);

        User user = currentUser.getValue();

        questionRepository.loadRandomQuestion(
                user.getId(),
                categoryId,
                isMixed,
                question -> {
                    currentQuestion.setValue(question);
                    isAnswerSelected.setValue(false);
                    isHelpUsed.setValue(false);
                    isQuestionSaved.setValue(false);
                    savedQuestionId.setValue(null);

                    if (question.getImage() != null && !question.getImage().isEmpty()) {
                        loadQuestionImage(question.getImage());
                    }

                    if(question.getExplanationText() != null) {
                        explanationText.setValue(question.getExplanationText());
                    } else {
                        explanationText.setValue("Sajnos nincs megjelenítendő magyarázat ehhez a kérdéshez");
                    }

                },
                () -> errorMessage.setValue("Nincs több kérdés"),
                error -> errorMessage.setValue(error)
        );
    }

    public void submitAnswer(int selectedAnswerIndex) {
        if (isAnswerSelected.getValue()){
            return;
        }

        Question question = currentQuestion.getValue();
        User user = currentUser.getValue();

        boolean isCorrect = selectedAnswerIndex == question.getCorrectAnswerIndex();
        AnsweredQuestion answeredQuestion = new AnsweredQuestion(
                question.getId(),
                question.getCategory(),
                user.getId(),
                question.getAnswers().get(selectedAnswerIndex),
                isCorrect
        );

        questionRepository.submitAnswer(
                answeredQuestion,
                user.getId(),
                isCorrect,
                goldChange -> {
                    user.setGold(user.getGold() + goldChange);
                    currentUser.setValue(user);
                    isAnswerSelected.setValue(true);
                },
                error -> errorMessage.setValue(error)
        );
    }


    public void saveQuestion() {
        Question question = currentQuestion.getValue();
        User user = currentUser.getValue();
        if (question == null || user == null) return;

        if (Boolean.TRUE.equals(isQuestionSaved.getValue())) {
            questionRepository.removeSavedQuestion(
                    savedQuestionId.getValue(),
                    () -> {
                        isQuestionSaved.setValue(false);
                        savedQuestionId.setValue(null);
                    },
                    error -> errorMessage.setValue(error)
            );
        } else {
            questionRepository.saveQuestion(
                    user.getId(),
                    question.getId(),
                    documentId -> {
                        isQuestionSaved.setValue(true);
                        savedQuestionId.setValue(documentId);
                    },
                    error -> errorMessage.setValue(error)
            );
        }
    }

    public boolean useHelp() {
        if (Boolean.TRUE.equals(isHelpUsed.getValue())){
            errorMessage.setValue("Már használtál segítséget ehhez a kérdéshez!");
            return false;
        }
        if(Boolean.TRUE.equals(isAnswerSelected.getValue())){
            errorMessage.setValue("Már válaszoltál erre a kérdésre!");
            return false;
        }

        User user = currentUser.getValue();
        if (user.getGold() < 10) {
            errorMessage.setValue("Nincs elég aranyad a segítség vásárlásához!");
            return false;
        }

        userRepository.updateGold(
                user.getId(),
                -10,
                success -> {
                    user.setGold(user.getGold() - 10);
                    isHelpUsed.setValue(true);
                    currentUser.setValue(user); //pont frissítése
                },
                error -> errorMessage.setValue(error)
        );
        return true;
    }
}