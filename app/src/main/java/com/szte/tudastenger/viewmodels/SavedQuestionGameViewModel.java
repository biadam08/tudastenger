package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.cardview.widget.CardView;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.R;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.QuestionRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;

public class SavedQuestionGameViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    private final MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAnswerSelected = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> clickedIndex = new MutableLiveData<>();
    private final MutableLiveData<Integer> correctIndex = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isQuestionSaved = new MutableLiveData<>(true);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private String savedQuestionId;
    private String savedDocumentId;

    public SavedQuestionGameViewModel(Application application) {
        super(application);
        userRepository = new UserRepository();
        questionRepository = new QuestionRepository();
    }

    public LiveData<Question> getCurrentQuestion() { return currentQuestion; }
    public LiveData<Boolean> getIsAnswerSelected() { return isAnswerSelected; }
    public LiveData<Integer> getClickedIndex() { return clickedIndex; }
    public LiveData<Integer> getCorrectIndex() { return correctIndex; }
    public LiveData<Boolean> getIsQuestionSaved() { return isQuestionSaved; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<User> getCurrentUser() { return currentUser; }

    public void init(String questionId) {
        this.savedQuestionId = questionId;
        loadUser();
    }

    private void loadUser() {
        userRepository.loadCurrentUser(
                user -> {
                    currentUser.setValue(user);
                    loadQuestion();
                }
        );
    }

    private void loadQuestion() {
        questionRepository.loadSavedQuestion( savedQuestionId,
                question -> currentQuestion.setValue(question),
                error -> toastMessage.setValue(error)
        );
    }


    public void handleAnswerClick(int clicked, int correct) {
        if (!Boolean.TRUE.equals(isAnswerSelected.getValue())) {
            isAnswerSelected.postValue(true);
            clickedIndex.setValue(clicked);
            correctIndex.setValue(correct);
        }
    }

    public void saveQuestion() {
        User user = currentUser.getValue();
        if (user == null) return;

        if (!Boolean.TRUE.equals(isQuestionSaved.getValue())) {
            questionRepository.saveQuestion(
                    user.getId(),
                    savedQuestionId,
                    (isSaved, documentId) -> {
                        savedDocumentId = documentId;
                        isQuestionSaved.setValue(true);
                        toastMessage.setValue("Sikeresen elmentve");
                    },
                    error -> toastMessage.setValue(error)
            );
        } else {
            questionRepository.removeQuestionFromSaved(
                    user.getId(),
                    savedQuestionId,
                    (isSaved, documentId) -> {
                        isQuestionSaved.setValue(false);
                        toastMessage.setValue("Sikeresen eltávolítva");
                    }
            );
        }
    }
}