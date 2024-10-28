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

import java.util.ArrayList;
import java.util.HashMap;

public class SavedQuestionGameViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final FirebaseUser mUser;
    private final CollectionReference mUsers;
    private final CollectionReference mQuestions;
    private final StorageReference mStorage;

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
        mFirestore = FirebaseFirestore.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUsers = mFirestore.collection("Users");
        mQuestions = mFirestore.collection("Questions");
        mStorage = FirebaseStorage.getInstance().getReference();
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
        if (mUser != null) {
            mUsers.whereEqualTo("email", mUser.getEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            User user = doc.toObject(User.class);
                            currentUser.setValue(user);
                            loadQuestion();
                        }
                    });
        }
    }

    private void loadQuestion() {
        mQuestions.document(savedQuestionId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        Question question = new Question(
                                savedQuestionId,
                                doc.getString("questionText"),
                                doc.getString("category"),
                                (ArrayList<String>) doc.get("answers"),
                                doc.getLong("correctAnswerIndex").intValue(),
                                doc.getString("image"),
                                doc.getString("explanationText")
                        );
                        currentQuestion.setValue(question);
                    }
                });
    }

    public void handleAnswerClick(int clicked, int correct) {
        if (!Boolean.TRUE.equals(isAnswerSelected.getValue())) {
            isAnswerSelected.postValue(true);
            clickedIndex.setValue(clicked);
            correctIndex.setValue(correct);
        }
    }

    public void saveQuestion() {
        if (!Boolean.TRUE.equals(isQuestionSaved.getValue())) {
            // Mentés
            HashMap<String, Object> questionToSave = new HashMap<>();
            questionToSave.put("userId", currentUser.getValue().getId());
            questionToSave.put("questionId", savedQuestionId);
            questionToSave.put("date", Timestamp.now());

            mFirestore.collection("SavedQuestions")
                    .add(questionToSave)
                    .addOnSuccessListener(documentReference -> {
                        savedDocumentId = documentReference.getId();
                        isQuestionSaved.setValue(true);
                        toastMessage.setValue("Sikeresen elmentve");
                    })
                    .addOnFailureListener(e ->
                            toastMessage.setValue("Nem sikerült elmenteni"));
        } else {
            // Törlés
            mFirestore.collection("SavedQuestions")
                    .whereEqualTo("userId", currentUser.getValue().getId())
                    .whereEqualTo("questionId", savedQuestionId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                doc.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            isQuestionSaved.setValue(false);
                                            toastMessage.setValue("Sikeresen eltávolítva");
                                        });
                            }
                        }
                    });
        }
    }
}