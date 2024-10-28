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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class QuizGameViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final FirebaseAuth mAuth;
    private final FirebaseUser mUser;
    private final FirebaseStorage mStorage;
    private final MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isQuestionSaved = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> savedQuestionId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isHelpUsed = new MutableLiveData<>(false);
    private final MutableLiveData<String> explanationText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAnswerSelected = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();

    public QuizGameViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mStorage = FirebaseStorage.getInstance();
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

    public void loadCurrentUser() {
        mFirestore.collection("Users")
                .whereEqualTo("email", mUser.getEmail())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        currentUser.setValue(user);
                        return;
                    }
                    errorMessage.setValue("User not found");
                })
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    public void loadQuestionImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageUri.setValue(null);
            return;
        }

        mStorage.getReference()
                .child("images/" + imagePath)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    imageUri.setValue(uri);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Hiba a kép betöltésekor");
                    imageUri.setValue(null);
                });
    }
    public void queryRandomQuestion(String categoryId, boolean isMixed) {
        mFirestore.collection("AnsweredQuestions")
                .whereEqualTo("userId", currentUser.getValue().getId())
                .get()
                .addOnSuccessListener(answeredQuestions -> {
                    List<String> answeredQuestionIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : answeredQuestions) {
                        answeredQuestionIds.add(doc.getString("questionId"));
                    }

                    Query query = mFirestore.collection("Questions");
                    if (!isMixed && categoryId != null) {
                        query = query.whereEqualTo("category", categoryId);
                    }

                    query.get().addOnSuccessListener(questions -> {
                                List<Question> availableQuestions = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : questions) {
                                    if (!answeredQuestionIds.contains(doc.getId())) {
                                        Question question = doc.toObject(Question.class);
                                        question.setId(doc.getId());
                                        availableQuestions.add(question);
                                    }
                                }

                                if (availableQuestions.isEmpty()) {
                                    errorMessage.setValue("Nincs több kérdés");
                                    return;
                                }

                                Question randomQuestion = availableQuestions.get(new Random().nextInt(availableQuestions.size()));
                                currentQuestion.setValue(randomQuestion);
                                isAnswerSelected.setValue(false);
                                isHelpUsed.setValue(false);
                                isQuestionSaved.setValue(false);
                                savedQuestionId.setValue(null);

                                if (randomQuestion.getImage() != null && !randomQuestion.getImage().isEmpty()) {
                                    loadQuestionImage(randomQuestion.getImage());
                                }

                                if(randomQuestion.getExplanationText() != null) {
                                    explanationText.setValue(randomQuestion.getExplanationText());
                                } else {
                                    explanationText.setValue("Sajnos nincs megjelenítendő magyarázat ehhez a kérdéshez");
                                }
                            })
                            .addOnFailureListener(e -> {
                                errorMessage.setValue(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                });
    }

    public void submitAnswer(int selectedAnswerIndex) {
        if (isAnswerSelected.getValue()){
            return;
        }

        Question question = currentQuestion.getValue();
        User user = currentUser.getValue();
        boolean isCorrect = selectedAnswerIndex == question.getCorrectAnswerIndex();
        int goldChange = isCorrect ? 25 : -25;

        AnsweredQuestion answeredQuestion = new AnsweredQuestion(
                question.getId(),
                question.getCategory(),
                user.getId(),
                question.getAnswers().get(selectedAnswerIndex),
                isCorrect
        );

        mFirestore.collection("AnsweredQuestions")
                .add(answeredQuestion)
                .addOnSuccessListener(documentReference -> {
                    mFirestore.collection("Users")
                            .document(user.getId())
                            .update("gold", goldChange)
                            .addOnSuccessListener(aVoid -> {
                                user.setGold(user.getGold() + goldChange);
                                currentUser.setValue(user);
                                isAnswerSelected.setValue(true);
                            });

                    DocumentReference questionRef = mFirestore.collection("Questions").document(question.getId());
                    if (isCorrect) {
                        questionRef.update("numCorrectAnswers", FieldValue.increment(1));
                    } else {
                        questionRef.update("numWrongAnswers", FieldValue.increment(1));
                    }
                })
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    public void saveQuestion() {
        Question question = currentQuestion.getValue();
        User user = currentUser.getValue();

        if (isQuestionSaved.getValue()) {
            mFirestore.collection("SavedQuestions")
                    .document(savedQuestionId.getValue())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        isQuestionSaved.setValue(false);
                        savedQuestionId.setValue(null);
                    })
                    .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
        } else {
            HashMap<String, String> questionToSave = new HashMap<>();
            questionToSave.put("userId", user.getId());
            questionToSave.put("questionId", question.getId());
            questionToSave.put("date", String.valueOf(Timestamp.now()));

            mFirestore.collection("SavedQuestions")
                    .add(questionToSave)
                    .addOnSuccessListener(documentReference -> {
                        isQuestionSaved.setValue(true);
                        savedQuestionId.setValue(documentReference.getId());
                    })
                    .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
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

        mFirestore.collection("Users")
                .document(user.getId())
                .update("gold", -10)
                .addOnSuccessListener(aVoid -> {
                    user.setGold(user.getGold() - 10);
                    isHelpUsed.setValue(true);
                    currentUser.setValue(user);
                });
        return true;
    }
}