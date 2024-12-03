package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.szte.tudastenger.models.Challenge;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.ChallengeRepository;
import com.szte.tudastenger.repositories.QuestionRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ChallengeViewModel extends AndroidViewModel {
    private final QuestionRepository questionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private Challenge currentChallenge;
    private final ArrayList<Boolean> userResults = new ArrayList<>();

    private final MutableLiveData<ArrayList<Question>> questionsList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> actualQuestionNumber = new MutableLiveData<>(0);
    private final MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<String> explanationText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLastQuestion = new MutableLiveData<>(false);
    private final MutableLiveData<String> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNavigationButtons = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSelectedAnswer = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Map<CalendarDay, String>> challengeResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> userHasCompleted = new MutableLiveData<>();

    @Inject
    public ChallengeViewModel(Application application, QuestionRepository questionRepository, ChallengeRepository challengeRepository, UserRepository userRepository) {
        super(application);
        this.questionRepository = questionRepository;
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
    }


    public LiveData<ArrayList<Question>> getQuestionsList() { return questionsList; }
    public LiveData<Question> getCurrentQuestion() { return currentQuestion; }
    public LiveData<String> getExplanationText() { return explanationText; }
    public LiveData<Boolean> getIsLastQuestion() { return isLastQuestion; }
    public LiveData<String> getResult() { return result; }
    public LiveData<Boolean> getShowNavigationButtons() { return showNavigationButtons; }
    public LiveData<Boolean> getIsSelectedAnswer() { return isSelectedAnswer; }
    public LiveData<Uri> getImageUri() { return imageUri; }
    public LiveData<Integer> getActualQuestionNumber() { return actualQuestionNumber; }
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Map<CalendarDay, String>> getChallengeResults() { return challengeResults; }
    public LiveData<Boolean> getUserHasCompleted() { return userHasCompleted; }


    public void init() {
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        userRepository.loadCurrentUser(
                user -> {
                    currentUser.setValue(user);
                    loadResults();
                    getTodaysChallenge();
                }
        );
    }

    private void loadResults() {
        challengeRepository.getChallengeResultsByUser(
                currentUser.getValue().getId(),
                results -> challengeResults.setValue(results)
        );
    }

    private void getTodaysChallenge() {
        challengeRepository.getTodaysChallenge(
                challenge -> {
                    currentChallenge = challenge;
                    challengeRepository.checkIfUserCompleted(
                            currentUser.getValue().getId(),
                            challenge.getId(),
                            hasCompleted -> {
                                if(!hasCompleted){
                                    userHasCompleted.setValue(false);
                                } else{
                                    userHasCompleted.setValue(true);
                                    errorMessage.setValue("A mai kihívást már kitöltötted!");
                                }
                            });
                },
                error -> { errorMessage.setValue(error); }
        );
    }

    public void loadTodaysChallenge() {
        challengeRepository.getTodaysChallenge(
            challenge -> {
                currentChallenge = challenge;
                challengeRepository.checkIfUserCompleted(
                        currentUser.getValue().getId(),
                        challenge.getId(),
                        hasCompleted -> {
                            if(!hasCompleted){
                                userHasCompleted.setValue(false);
                                loadQuestions(challenge.getQuestionIds());
                            } else{
                                userHasCompleted.setValue(true);
                                errorMessage.setValue("A mai kihívást már kitöltötted!");
                            }
                        });
            },
            error -> { errorMessage.setValue(error); }
        );
    }

    private void loadQuestions(ArrayList<String> questionIds) {
        challengeRepository.loadChallengeQuestions(
            questionIds,
            questions -> {
                questionsList.setValue(questions);
                startChallenge();
            }
        );
    }

    public void startChallenge() {
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
        questionRepository.loadQuestionImage(question.getImage(), uri -> imageUri.setValue(Uri.parse(uri)), error -> imageUri.setValue(null));
    }

    public void handleAnswerClick(int clickedIndex, int correctAnswerIndex) {
        if (!isSelectedAnswer.getValue()) {
            isSelectedAnswer.setValue(true);
            boolean isCorrect = clickedIndex == correctAnswerIndex;
            userResults.add(isCorrect);
            showNavigationButtons.setValue(true);
        }
    }

    public void finishChallenge() {
        int correctAnswers = 0;
        for (Boolean result : userResults) {
            if (result) {
                correctAnswers++;
            }
        }
        result.setValue("Eredményed: " + correctAnswers + "/" + questionsList.getValue().size());
        
        challengeRepository.saveChallengeResult(
            currentChallenge.getId(),
            correctAnswers,
            userResults
        );
    }

    public void resetForNextQuestion() {
        isSelectedAnswer.setValue(false);
        showNavigationButtons.setValue(false);
    }

}
