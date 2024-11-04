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
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.DuelRepository;
import com.szte.tudastenger.repositories.QuestionRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DuelViewModel extends AndroidViewModel {
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DuelRepository duelRepository;
    private String categoryId;
    private String category;
    private String duelId;
    private String challengerUserId;
    private String challengedUserId;
    private Duel actualDuel;
    private Integer questionNumber;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<Category>> mCategoriesData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>();
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
        questionRepository = new QuestionRepository();
        duelRepository = new DuelRepository();
        categoryRepository = new CategoryRepository();
        userRepository = new UserRepository();
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<ArrayList<Category>> getCategoriesData() { return mCategoriesData; }
    public LiveData<String> getSelectedCategory() { return selectedCategory; }
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

    public String getCategory() {
        return category;
    }

    public void init(String challengerId, String challengedId, String duelId) {
        this.challengerUserId = challengerId;
        this.challengedUserId = challengedId;
        this.duelId = duelId;

        loadCurrentUser();
    }

    private void loadCurrentUser(){
        userRepository.loadCurrentUser(user -> {
            currentUser.setValue(user);

            if(user.getId().equals(challengerUserId)) {
                showButtonsLayout.setValue(true);
                loadCategories();
            } else {
                showButtonsLayout.setValue(false);
                loadDuelData();
            }
        });
    }

    public void loadDuelData() {
        duelRepository.loadDuelWithQuestions(
                duelId,
                (duel, questionIds) -> {
                    actualDuel = duel;
                    questionIdsList.setValue(questionIds);
                },
                questions -> {
                    questionsList.setValue(questions);
                    startDuel();
                }
        );
    }


    public void selectCategory(String categoryName) {
        if(!categoryName.equals("Vegyes kategória")) {
            categoryRepository.selectCategory(categoryName, selectedCategoryId  -> categoryId = selectedCategoryId);
            category = categoryName;
            selectedCategory.setValue(categoryName);
        } else {
            categoryId = null;
            category = "mixed";
            selectedCategory.setValue("mixed");
        }
    }

    public void loadCategories() {
        ArrayList<Category> categories = new ArrayList<>();
        categories.add(new Category(null, "Vegyes kategória", null));

        categoryRepository.loadCategories(
                categoryList -> {
                    categories.addAll(categoryList);
                    mCategoriesData.setValue(categories);
                },
                error -> {}
        );
    }

    public void initializeQuestions(int questionCount) {
        if(categoryId == null) {
            categoryId = "mixed";
        }

        this.questionNumber = questionCount;
        showButtonsLayout.setValue(false);

        questionRepository.initializeDuelQuestions(
                categoryId,
                category,
                questionNumber,
                (questions, questionIds) -> {
                    questionsList.setValue(questions);
                    questionIdsList.setValue(questionIds);
                    startDuel();
                },
                error -> {
                    toastMessage.setValue(error);
                    if (error.equals("Nincs elég kérdés ebben a kategóriában!")) {
                        showButtonsLayout.setValue(true);
                    }
                }
        );
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

    public void loadQuestionImage(){
        Question question = currentQuestion.getValue();
        questionRepository.loadQuestionImage(question.getImage(), uri -> imageUri.setValue(Uri.parse(uri)), error -> imageUri.setValue(null));
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
        if (currentUser.getValue().getId().equals(challengerUserId)) {
            Duel duel = new Duel(null, challengerUserId, challengedUserId, categoryId, questionIdsList.getValue(), challengerUserResults.getValue(), null);

            // kihívás után mentés
            duelRepository.createNewDuel(
                    duel,
                    this::getDuelResult
            );

        } else {
            // kihívott játéka után frissítés
            duelRepository.updateDuelWithChallengedResults(
                    duelId,
                    challengedUserResults.getValue(),
                    this::getDuelResult
            );
        }
    }


    private void getDuelResult() {
        if(currentUser.getValue().getId().equals(challengerUserId)) {
            int correctAnswers = 0;
            for (Boolean res : challengerUserResults.getValue()) {
                if (res) correctAnswers++;
            }
            result.setValue("Eredményed: " + correctAnswers + "/" + questionIdsList.getValue().size());
        } else {
            duelRepository.getDuelById(
                    duelId,
                    challengerResults -> {
                        int challengerCorrect = 0;
                        int challengedCorrect = 0;

                        for (int i = 0; i < questionIdsList.getValue().size(); i++) {
                            if (challengerResults.get(i)) challengerCorrect++;
                            if (challengedUserResults.getValue().get(i)) challengedCorrect++;
                        }

                        result.setValue("Eredményed: " + challengedCorrect + "/" + questionIdsList.getValue().size() + "\n" + "A kihívó eredménye: " + challengerCorrect + "/" + questionIdsList.getValue().size());
                    });
        }
    }

    public void resetForNextQuestion() {
        isSelectedAnswer.setValue(false);
        showNavigationButtons.setValue(false);
    }
}