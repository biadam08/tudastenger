package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DuelViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final FirebaseAuth mAuth;
    private final FirebaseUser mUser;
    private final CollectionReference mUsers;
    private final CollectionReference mQuestions;
    private final CollectionReference mCategories;
    private final StorageReference storageReference;
    private String categoryId;
    private String category;
    private String duelId;
    private String challengerUserId;
    private String challengedUserId;
    private Duel actualDuel;
    private Integer questionNumber;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<Category>> mCategoriesData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<Question>> questionsList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<String>> questionIdsList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<Boolean>> challengerUserResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<Boolean>> challengedUserResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> actualQuestionNumber = new MutableLiveData<>(0);
    private final MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<String> explanationText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLastQuestion = new MutableLiveData<>(false);
    private final MutableLiveData<String> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNavigationButtons = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSelectedAnswer = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showButtonsLayout = new MutableLiveData<>(true);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();


    public DuelViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mUsers = mFirestore.collection("Users");
        mQuestions = mFirestore.collection("Questions");
        mCategories = mFirestore.collection("Categories");
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<ArrayList<Category>> getCategoriesData() { return mCategoriesData; }
    public LiveData<ArrayList<Question>> getQuestionsList() { return questionsList; }
    public LiveData<Question> getCurrentQuestion() { return currentQuestion; }
    public LiveData<String> getExplanationText() { return explanationText; }
    public LiveData<Boolean> getIsLastQuestion() { return isLastQuestion; }
    public LiveData<String> getResult() { return result; }
    public LiveData<Boolean> getShowNavigationButtons() { return showNavigationButtons; }
    public LiveData<Boolean> getIsSelectedAnswer() { return isSelectedAnswer; }
    public LiveData<Uri> getImageUri() { return imageUri; }
    public LiveData<Integer> getActualQuestionNumber() { return actualQuestionNumber; }
    public LiveData<Boolean> getShowButtonsLayout() { return showButtonsLayout; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    public void init(String challengerId, String challengedId, String duelId) {
        this.challengerUserId = challengerId;
        this.challengedUserId = challengedId;
        this.duelId = duelId;

        loadCurrentUser();
    }

    private void loadCurrentUser() {
        if (mUser != null) {
            mUsers.whereEqualTo("email", mUser.getEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            currentUser.setValue(doc.toObject(User.class));
                            if(currentUser.getValue().getId().equals(challengerUserId)) {
                                showButtonsLayout.setValue(true);
                                loadCategories();
                            } else {
                                showButtonsLayout.setValue(false);
                                loadDuelData();
                            }
                        }
                    });
        }
    }

    public void loadDuelData() {
        mFirestore.collection("Duels").document(duelId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        actualDuel = documentSnapshot.toObject(Duel.class);
                        ArrayList<String> qIds = actualDuel.getQuestionIds();
                        questionIdsList.setValue(qIds);

                        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                        for (String questionId : qIds) {
                            tasks.add(mQuestions.document(questionId).get());
                        }

                        Tasks.whenAllSuccess(tasks).addOnSuccessListener(list -> {
                            ArrayList<Question> questions = new ArrayList<>();
                            for (Object object : list) {
                                DocumentSnapshot document = (DocumentSnapshot) object;
                                Question question = document.toObject(Question.class);
                                questions.add(question);
                            }
                            questionsList.setValue(questions);
                            startDuel();
                        });
                    }
                });
    }

    public void selectCategory(String categoryName) {
        if(!categoryName.equals("Vegyes kategória")) {
            mFirestore.collection("Categories")
                    .whereEqualTo("name", categoryName)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            categoryId = documentSnapshot.getId();
                        }
                    });
            category = categoryName;
        } else {
            categoryId = null;
            category = "mixed";
        }
    }

    public void loadCategories() {
        ArrayList<Category> categories = new ArrayList<>();
        categories.add(new Category(null, "Vegyes kategória", null));
        mCategoriesData.setValue(categories);

        mCategories.orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                        Category category = document.toObject(Category.class);
                        categories.add(category);
                    }
                    mCategoriesData.setValue(categories);
                });
    }

    public void initializeQuestions(int questionCount) {
        this.questionNumber = questionCount;

        if(categoryId == null) {
            categoryId = "mixed";
        }

        showButtonsLayout.setValue(false);

        Query query = mQuestions;
        if (!category.equals("mixed")) {
            query = query.whereEqualTo("category", categoryId);
        }

        query.get()
                .addOnSuccessListener(questions -> {
                    List<DocumentSnapshot> documents = questions.getDocuments();
                    if (documents.size() >= questionNumber) {
                        Random random = new Random();
                        ArrayList<Question> newQuestionsList = new ArrayList<>();
                        ArrayList<String> newQuestionIdsList = new ArrayList<>();

                        while (newQuestionsList.size() < questionNumber) {
                            DocumentSnapshot randomDoc = documents.get(random.nextInt(documents.size()));
                            Question question = randomDoc.toObject(Question.class);
                            if (question != null && !newQuestionIdsList.contains(question.getId())) {
                                newQuestionsList.add(question);
                                newQuestionIdsList.add(question.getId());
                            }
                        }

                        questionsList.setValue(newQuestionsList);
                        questionIdsList.setValue(newQuestionIdsList);
                        startDuel();
                    } else {
                        toastMessage.setValue("Nincs elég kérdés ebben a kategóriában!");
                        showButtonsLayout.setValue(true);
                    }
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("Hiba történt a kérdések betöltésekor!");
                });
    }

    public void startDuel() {
        showButtonsLayout.setValue(false);
        actualQuestionNumber.setValue(actualQuestionNumber.getValue() + 1);
        isLastQuestion.setValue(actualQuestionNumber.getValue() == questionsList.getValue().size());
        currentQuestion.setValue(questionsList.getValue().get(actualQuestionNumber.getValue() - 1));
        showNavigationButtons.setValue(false);
        isSelectedAnswer.setValue(false);

        if(currentQuestion.getValue().getExplanationText() != null) {
            explanationText.setValue(currentQuestion.getValue().getExplanationText());
        } else {
            explanationText.setValue("Sajnos nincs megjelenítendő magyarázat ehhez a kérdéshez");
        }

        loadQuestionImage();
    }

    private void loadQuestionImage() {
        Question question = currentQuestion.getValue();
        if (question != null && question.getImage() != null && !question.getImage().isEmpty()) {
            String imagePath = "images/" + question.getImage();
            storageReference.child(imagePath).getDownloadUrl()
                    .addOnSuccessListener(uri -> imageUri.setValue(uri))
                    .addOnFailureListener(e -> imageUri.setValue(null));
        } else {
            imageUri.setValue(null);
        }
    }

    public void handleAnswerClick(int clickedIndex, int correctAnswerIndex) {
        if (!isSelectedAnswer.getValue()) {
            isSelectedAnswer.setValue(true);
            Question question = currentQuestion.getValue();
            boolean isCorrect = clickedIndex == correctAnswerIndex;

            ArrayList<Boolean> results = currentUser.getValue().getId().equals(challengerUserId) ?
                    challengerUserResults.getValue() : challengedUserResults.getValue();
            results.add(isCorrect);

            if (currentUser.getValue().getId().equals(challengerUserId)) {
                challengerUserResults.setValue(results);
            } else {
                challengedUserResults.setValue(results);
            }

            showNavigationButtons.setValue(true);
        }
    }

    public void finishDuel() {
        if(currentUser.getValue().getId().equals(challengerUserId)) {
            Duel duel = new Duel(null, challengerUserId, challengedUserId, categoryId,
                    questionIdsList.getValue(), challengerUserResults.getValue(), null);

            mFirestore.collection("Duels")
                    .add(duel)
                    .addOnSuccessListener(documentReference -> {
                        String documentId = documentReference.getId();
                        duel.setId(documentId);
                        mFirestore.collection("Duels")
                                .document(documentId)
                                .update("id", documentId);
                    });
        } else {
            mFirestore.collection("Duels")
                    .document(duelId)
                    .update(
                            "challengedUserResults", challengedUserResults.getValue(),
                            "finished", true
                    );
        }

        getDuelResult();
    }

    private void getDuelResult() {
        if(currentUser.getValue().getId().equals(challengerUserId)) {
            int correctAnswers = 0;
            for (Boolean res : challengerUserResults.getValue()) {
                if (res) correctAnswers++;
            }
            result.setValue("Eredményed: " + correctAnswers + "/" + questionIdsList.getValue().size());
        } else {
            mFirestore.collection("Duels")
                    .document(duelId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            ArrayList<Boolean> challengerResults = (ArrayList<Boolean>) documentSnapshot.get("challengerUserResults");

                            int challengerCorrect = 0;
                            int challengedCorrect = 0;

                            for (int i = 0; i < questionIdsList.getValue().size(); i++) {
                                if (challengerResults.get(i)) challengerCorrect++;
                                if (challengedUserResults.getValue().get(i)) challengedCorrect++;
                            }

                            result.setValue("Eredményed: " + challengedCorrect + "/" +
                                    questionIdsList.getValue().size() + "\n" +
                                    "A kihívó eredménye: " + challengerCorrect + "/" +
                                    questionIdsList.getValue().size());
                        }
                    });
        }
    }

    public void resetForNextQuestion() {
        isSelectedAnswer.setValue(false);
        showNavigationButtons.setValue(false);
    }
}