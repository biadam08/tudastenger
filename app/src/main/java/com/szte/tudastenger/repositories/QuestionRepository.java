package com.szte.tudastenger.repositories;

import android.net.Uri;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;

public class QuestionRepository {
    private final FirebaseFirestore mFirestore;
    private final StorageReference mStorage;
    private final FirebaseFunctions mFunctions;
    private final CollectionReference mQuestions;

    @Inject
    public QuestionRepository(FirebaseFirestore firestore, StorageReference storage, FirebaseFunctions functions) {
        this.mFirestore = firestore;
        this.mStorage = storage;
        this.mFunctions = functions;
        this.mQuestions = mFirestore.collection("Questions");
    }


    public void loadQuestionData(String questionId, QuestionReceivedCallback questionReceivedCallback, ImageUrlCallback imageUrlCallback, ErrorCallback errorCallback) {
        if (questionId == null) return;

        mQuestions.document(questionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Question question = documentSnapshot.toObject(Question.class);
                        questionReceivedCallback.onQuestionReceived(question);

                        if (question.getImage() != null && !question.getImage().isEmpty()) {
                            loadQuestionImage(question.getImage(), imageUrlCallback, errorCallback);
                        }
                    } else {
                        errorCallback.onError("Kérdés nem található");
                    }
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void loadQuestionImage(String imageName, ImageUrlCallback imageUrlCallback, ErrorCallback errorCallback) {
        mStorage.child("images/" + imageName)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> imageUrlCallback.onImageUrlReceived(uri.toString()))
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void uploadQuestionWithImage(Question question, Uri imageUri, SuccessCallback successCallback, ErrorCallback errorCallback, ProgressCallback progressCallback, QuestionReceivedCallback questionReceivedCallback) {
        if (imageUri != null) {
            String fileName = UUID.randomUUID().toString();
            StorageReference ref = mStorage.child("images/" + fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        question.setImage(fileName);
                        saveOrUpdateQuestion(question, successCallback, errorCallback, questionReceivedCallback);
                    })
                    .addOnFailureListener(e -> errorCallback.onError(e.getMessage()))
                    .addOnProgressListener(taskSnapshot -> {
                        int progress = (int) ((100.0 * taskSnapshot.getBytesTransferred()) /
                                taskSnapshot.getTotalByteCount());
                        progressCallback.onProgress(progress);
                    });
        } else {
            saveOrUpdateQuestion(question, successCallback, errorCallback, questionReceivedCallback);
        }
    }

    private void saveOrUpdateQuestion(Question question, SuccessCallback successCallback, ErrorCallback errorCallback, QuestionReceivedCallback questionReceivedCallback) {
        if (question.getId() != null) {
            mQuestions.document(question.getId())
                    .set(question)
                    .addOnSuccessListener(aVoid -> {
                        successCallback.onSuccess("A kérdés sikeresen módosítva lett!");
                        questionReceivedCallback.onQuestionReceived(question);
                    })
                    .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
        } else {
            mQuestions.add(question)
                    .addOnSuccessListener(documentReference -> {
                        String documentId = documentReference.getId();
                        question.setId(documentId);
                        mQuestions.document(documentId)
                                .update("id", documentId)
                                .addOnSuccessListener(aVoid -> {
                                    successCallback.onSuccess("A kérdés sikeresen feltöltve!");
                                    questionReceivedCallback.onQuestionReceived(question);
                                })
                                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
                    })
                    .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
        }
    }

    public void deleteQuestion(String questionId, SuccessCallback successCallback, ErrorCallback errorCallback) {
        mQuestions.document(questionId)
                .delete()
                .addOnSuccessListener(aVoid -> successCallback.onSuccess("Kérdés sikeresen törölve"))
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void generateExplanation(String questionText, String correctAnswer,
                                    ExplanationCallback explanationCallback, ErrorCallback errorCallback) {
        String prompt = "Kérlek, készíts egy pontos, 3-6 mondatos választ az alábbi kérdésre, figyelembe véve, " +
                "hogy a helyes válasz kizárólag a megadott helyes válasz lehet: \nKérdés: " +
                questionText + "\nHelyes válasz: " + correctAnswer + ".";

        mFunctions.getHttpsCallable("getChatGPTResponse")
                .call(Collections.singletonMap("prompt", prompt))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> resultMap = (Map<String, Object>) task.getResult().getData();
                        if (resultMap.containsKey("response")) {
                            explanationCallback.onExplanationGenerated((String) resultMap.get("response"));
                        } else {
                            errorCallback.onError("Nem sikerült magyarázatot generálni");
                        }
                    } else {
                        errorCallback.onError(task.getException().getMessage());
                    }
                });
    }

    public void loadAllQuestions(QuestionsLoadedCallback questionsLoadedCallback, ErrorCallback errorCallback) {
        mQuestions.orderBy("questionText")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Question> questionList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Question question = document.toObject(Question.class);
                        questionList.add(question);
                    }
                    questionsLoadedCallback.onQuestionsLoaded(questionList);
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void initializeDuelQuestions(String categoryId, String category, int questionCount, QuestionsInitializedCallback questionsInitializedCallback, ErrorCallback errorCallback) {
        if(categoryId == null) {
            categoryId = "mixed";
        }

        Query query = mQuestions;
        if (!category.equals("mixed")) {
            query = query.whereEqualTo("category", categoryId);
        }

        query.get()
                .addOnSuccessListener(questions -> {
                    List<DocumentSnapshot> documents = questions.getDocuments();
                    if (documents.size() >= questionCount) {
                        Random random = new Random();
                        ArrayList<Question> newQuestionsList = new ArrayList<>();
                        ArrayList<String> newQuestionIdsList = new ArrayList<>();

                        while (newQuestionsList.size() < questionCount) {
                            DocumentSnapshot randomDoc = documents.get(random.nextInt(documents.size()));
                            Question question = randomDoc.toObject(Question.class);
                            if (question != null && !newQuestionIdsList.contains(question.getId())) {
                                newQuestionsList.add(question);
                                newQuestionIdsList.add(question.getId());
                            }
                        }

                        questionsInitializedCallback.onQuestionsInitialized(newQuestionsList, newQuestionIdsList);
                    } else {
                        errorCallback.onError("Nincs elég kérdés ebben a kategóriában!");
                    }
                })
                .addOnFailureListener(e -> errorCallback.onError("Hiba történt a kérdések betöltésekor!"));
    }
    public void loadSavedQuestions(String userId, String selectedCategory, SavedQuestionsLoadedCallback callback, NoQuestionsCallback noDataCallback, ErrorCallback errorCallback) {
        ArrayList<Question> questionsList = new ArrayList<>();

        mFirestore.collection("SavedQuestions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            noDataCallback.onNoQuestions();
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String questionId = document.getString("questionId");
                                loadSavedQuestionDetails(questionId, selectedCategory, questionsList, callback);
                            }
                        }
                    } else {
                        errorCallback.onError(task.getException().getMessage());
                    }
                });
    }

    private void loadSavedQuestionDetails(String questionId, String selectedCategory, List<Question> questionsList, SavedQuestionsLoadedCallback callback) {
        mQuestions.document(questionId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot questionDoc = task.getResult();
                        String categoryId = questionDoc.getString("category");

                        mFirestore.collection("Categories").document(categoryId)
                                .get()
                                .addOnSuccessListener(categoryDoc -> {
                                    if (categoryDoc.exists()) {
                                        String categoryName = categoryDoc.getString("name");
                                        if (selectedCategory.equals("Összes kategória") || selectedCategory.equals(categoryName)) {
                                            Question question = new Question(
                                                    questionId,
                                                    questionDoc.getString("questionText"),
                                                    categoryName,
                                                    (ArrayList<String>) questionDoc.get("answers"),
                                                    questionDoc.getLong("correctAnswerIndex").intValue(),
                                                    questionDoc.getString("image"),
                                                    questionDoc.getString("explanationText")
                                            );
                                            questionsList.add(question);
                                        }
                                        callback.onQuestionsLoaded(questionsList);
                                    }
                                });
                    }
                });
    }

    public void loadSavedQuestion(String questionId, QuestionLoadedCallback callback, ErrorCallback errorCallback) {
        mQuestions.document(questionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Question question = new Question(
                                questionId,
                                doc.getString("questionText"),
                                doc.getString("category"),
                                (ArrayList<String>) doc.get("answers"),
                                doc.getLong("correctAnswerIndex").intValue(),
                                doc.getString("image"),
                                doc.getString("explanationText")
                        );
                        callback.onQuestionLoaded(question);
                    } else {
                        errorCallback.onError("Kérdés nem található");
                    }
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void saveQuestion(String userId, String questionId, SaveOperationCallback callback, ErrorCallback errorCallback) {
        HashMap<String, Object> questionToSave = new HashMap<>();
        questionToSave.put("userId", userId);
        questionToSave.put("questionId", questionId);
        questionToSave.put("date", Timestamp.now());

        mFirestore.collection("SavedQuestions")
                .add(questionToSave)
                .addOnSuccessListener(documentReference ->
                        callback.onSaveOperationComplete(true, documentReference.getId()))
                .addOnFailureListener(e -> errorCallback.onError("Nem sikerült elmenteni"));
    }

    public void removeQuestionFromSaved(String userId, String questionId, SaveOperationCallback callback) {
        mFirestore.collection("SavedQuestions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("questionId", questionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            doc.getReference().delete()
                                    .addOnSuccessListener(aVoid ->
                                            callback.onSaveOperationComplete(false, null));
                        }
                    }
                });
    }

    public void deleteSavedQuestion(String userId, String questionId, DeleteCallback deleteCallback) {
        mFirestore.collection("SavedQuestions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("questionId", questionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            doc.getReference().delete()
                                    .addOnSuccessListener(aVoid -> deleteCallback.onQuestionDeleted());
                        }
                    }
                });
    }

    public void loadRandomQuestion(String userId, String categoryId, boolean isMixed, QuestionLoadedCallback questionCallback, NoQuestionsCallback noQuestionsCallback, ErrorCallback errorCallback) {
        mFirestore.collection("AnsweredQuestions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(answeredQuestions -> {
                    List<String> answeredQuestionIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : answeredQuestions) {
                        answeredQuestionIds.add(doc.getString("questionId"));
                    }

                    Query query = mQuestions;
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
                                    noQuestionsCallback.onNoQuestions();
                                    return;
                                }

                                Question randomQuestion = availableQuestions.get(new Random().nextInt(availableQuestions.size()));
                                questionCallback.onQuestionLoaded(randomQuestion);
                            })
                            .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void submitAnswer(AnsweredQuestion answeredQuestion, String userId, boolean isCorrect, SubmitAnswerCallback callback, ErrorCallback errorCallback) {
        int goldChange = isCorrect ? 25 : -25;

        mFirestore.collection("AnsweredQuestions")
                .add(answeredQuestion)
                .addOnSuccessListener(docRef -> {

                    mFirestore.collection("Users")
                            .document(userId)
                            .update("gold", FieldValue.increment(goldChange))
                            .addOnSuccessListener(aVoid -> callback.onAnswerSubmitted(goldChange))
                            .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));

                    DocumentReference questionRef = mFirestore.collection("Questions").document(answeredQuestion.getQuestionId());
                    if (isCorrect) {
                        questionRef.update("numCorrectAnswers", FieldValue.increment(1));
                    } else {
                        questionRef.update("numWrongAnswers", FieldValue.increment(1));
                    }
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void saveQuestion(String userId, String questionId, SaveQuestionCallback callback, ErrorCallback errorCallback) {
        HashMap<String, Object> questionToSave = new HashMap<>();
        questionToSave.put("userId", userId);
        questionToSave.put("questionId", questionId);
        questionToSave.put("date", Timestamp.now());

        mFirestore.collection("SavedQuestions")
                .add(questionToSave)
                .addOnSuccessListener(docRef -> callback.onQuestionSaved(docRef.getId()))
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void removeSavedQuestion(String savedQuestionId, RemoveQuestionCallback callback, ErrorCallback errorCallback) {
        mFirestore.collection("SavedQuestions")
                .document(savedQuestionId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onQuestionRemoved())
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }



    public interface SuccessCallback {
        void onSuccess(String message);
    }

    public interface ErrorCallback {
        void onError(String error);
    }

    public interface ProgressCallback {
        void onProgress(int progress);
    }

    public interface ImageUrlCallback {
        void onImageUrlReceived(String url);
    }

    public interface QuestionReceivedCallback {
        void onQuestionReceived(Question question);
    }

    public interface QuestionsLoadedCallback {
        void onQuestionsLoaded(List<Question> questions);
    }

    public interface ExplanationCallback {
        void onExplanationGenerated(String explanation);
    }

    public interface QuestionsInitializedCallback {
        void onQuestionsInitialized(ArrayList<Question> questions, ArrayList<String> questionIds);
    }
    public interface QuestionLoadedCallback {
        void onQuestionLoaded(Question questions);
    }
    public interface SaveOperationCallback {
        void onSaveOperationComplete(boolean isSaved, String documentId);
    }
    public interface SavedQuestionsLoadedCallback {
        void onQuestionsLoaded(List<Question> questions);
    }
    public interface DeleteCallback {
        void onQuestionDeleted();
    }
    public interface NoQuestionsCallback {
        void onNoQuestions();
    }
    public interface SubmitAnswerCallback {
        void onAnswerSubmitted(int goldChange);
    }

    public interface SaveQuestionCallback {
        void onQuestionSaved(String documentId);
    }
    public interface RemoveQuestionCallback {
        void onQuestionRemoved();
    }
}