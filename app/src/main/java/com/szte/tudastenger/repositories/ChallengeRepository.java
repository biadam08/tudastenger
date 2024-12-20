package com.szte.tudastenger.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.szte.tudastenger.models.Challenge;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.ChallengeResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;

public class ChallengeRepository {
    private final FirebaseFirestore mFirestore;

    @Inject
    public ChallengeRepository(FirebaseFirestore firestore) {
        this.mFirestore = firestore;
    }

    public void getTodaysChallenge(ChallengeLoadedCallback callback, ErrorCallback errorCallback) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Budapest"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endOfDay = calendar.getTime();

        mFirestore.collection("Challenges")
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Challenge challenge = querySnapshot.getDocuments().get(0).toObject(Challenge.class);
                        if(challenge.getIsActive()) {
                            callback.onChallengeLoaded(challenge);
                        } else{
                            errorCallback.onError("A napi kihívás már nem aktív!");
                        }
                    } else {
                        errorCallback.onError("Nincs mai kihívás!");
                    }
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void loadChallengeQuestions(ArrayList<String> questionIds, QuestionsLoadedCallback callback) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String questionId : questionIds) {
            tasks.add(mFirestore.collection("Questions").document(questionId).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(list -> {
            ArrayList<Question> questionList = new ArrayList<>();
            for (Object object : list) {
                DocumentSnapshot document = (DocumentSnapshot) object;
                Question question = document.toObject(Question.class);
                questionList.add(question);
            }
            callback.onQuestionsLoaded(questionList);
        });
    }

    public void saveChallengeResult(String challengeId, int correctAnswers, ArrayList<Boolean> results) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        ChallengeResult challengeResult = new ChallengeResult(
            userId,
            challengeId,
            correctAnswers,
            results,
            new Date()
        );

        mFirestore.collection("ChallengeResults")
            .add(challengeResult)
            .addOnSuccessListener(documentReference -> {
                documentReference.update("id", documentReference.getId());
            });
    }

    public void checkIfUserCompleted(String userId, String challengeId, CompletionCheckCallback callback) {
        mFirestore.collection("ChallengeResults")
                .whereEqualTo("userId", userId)
                .whereEqualTo("challengeId", challengeId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasCompleted = false;
                    if (!querySnapshot.isEmpty()) {
                        hasCompleted = true;
                    }
                    callback.onCompletionChecked(hasCompleted);
                })
                .addOnFailureListener(e -> {
                    callback.onCompletionChecked(false);
                });
    }

    public void getChallengeResultsByUser(String userId, ResultsLoadedCallback resultsLoadedCallback) {
        Map<CalendarDay, String> results = new HashMap<>();

        mFirestore.collection("ChallengeResults")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Date completedAtDate = document.getDate("completedAt");

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(completedAtDate);
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH) + 1;
                        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                        CalendarDay calendarDay = CalendarDay.from(year, month, dayOfMonth);

                        int correctAnswers = document.getLong("correctAnswers").intValue();
                        List<Boolean> resultList = (List<Boolean>) document.get("results");
                        int totalQuestions = resultList != null ? resultList.size() : 0;

                        double successRate = (double) correctAnswers / totalQuestions;

                        String result = "unsuccessful";
                        if(successRate >= 0.5){
                            result = "successful";
                        }

                        results.put(calendarDay, result);
                    }
                    resultsLoadedCallback.onResultsLoaded(results);
                });
    }
    public interface ChallengeLoadedCallback {
        void onChallengeLoaded(Challenge challenge);
    }

    public interface QuestionsLoadedCallback {
        void onQuestionsLoaded(ArrayList<Question> questions);
    }

    public interface ErrorCallback {
        void onError(String error);
    }

    public interface CompletionCheckCallback {
        void onCompletionChecked(boolean hasCompleted);
    }
    public interface ResultsLoadedCallback {
        void onResultsLoaded(Map<CalendarDay, String> results);
    }
}