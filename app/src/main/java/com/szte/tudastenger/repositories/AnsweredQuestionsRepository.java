package com.szte.tudastenger.repositories;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AnsweredQuestionsRepository {
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mAnsweredQuestions;

    public AnsweredQuestionsRepository() {
        this.mFirestore = FirebaseFirestore.getInstance();
        this.mAnsweredQuestions = mFirestore.collection("AnsweredQuestions");
    }

    public void loadCategoryScore(String userId, String categoryId, CategoryScoreCallback scoreCallback, ErrorCallback errorCallback) {
        mAnsweredQuestions
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", categoryId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int correctAnswers = 0;
                    int totalAnswers = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        totalAnswers++;
                        if (doc.getBoolean("correct")) {
                            correctAnswers++;
                        }
                    }
                    scoreCallback.onCategoryScoreLoaded(categoryId, correctAnswers, totalAnswers);
                }).addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void loadAnsweredQuestions(String userId, String selectedCategory, AnsweredQuestionsCallback answeredQuestionsCallback, NoQuestionsCallback noQuestionsCallback, ErrorCallback errorCallback) {
        mFirestore.collection("AnsweredQuestions")
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            noQuestionsCallback.onNoQuestions();
                        } else {
                            ArrayList<AnsweredQuestion> answeredQuestionsList = new ArrayList<>();

                            int totalQuestions = task.getResult().size();
                            AtomicInteger processedQuestions = new AtomicInteger();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String questionId = document.getString("questionId");
                                String answer = document.getString("answer");
                                boolean correct = document.getBoolean("correct");
                                Timestamp timestamp = document.getTimestamp("date");

                                mFirestore.collection("Questions").document(questionId)
                                        .get()
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                DocumentSnapshot questionDoc = task2.getResult();
                                                if (questionDoc.exists()) {
                                                    String categoryId = questionDoc.getString("category");

                                                    mFirestore.collection("Categories").document(categoryId)
                                                            .get()
                                                            .addOnSuccessListener(categoryDoc -> {
                                                                processedQuestions.getAndIncrement();

                                                                if (categoryDoc.exists()) {
                                                                    String categoryName = categoryDoc.getString("name");
                                                                    if (selectedCategory.equals("Összes kategória") || selectedCategory.equals(categoryName)) {
                                                                        AnsweredQuestion answeredQuestion = new AnsweredQuestion(
                                                                                questionId, categoryName,
                                                                                userId, timestamp, answer, correct
                                                                        );
                                                                        answeredQuestionsList.add(answeredQuestion);
                                                                    }

                                                                    //ha fel lett már dolgozva az összes megválaszolt kérdés
                                                                    if(processedQuestions.get() == totalQuestions) {
                                                                        // a legújabb kitöltés legyen legfelül
                                                                        answeredQuestionsList.sort((q1, q2) ->
                                                                                q2.getDate().compareTo(q1.getDate()));

                                                                        if (answeredQuestionsList.isEmpty()) {
                                                                            noQuestionsCallback.onNoQuestions();
                                                                        } else {
                                                                            answeredQuestionsCallback.onAnsweredQuestionsLoaded(answeredQuestionsList);
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        });
                            }
                        }
                    } else {
                        errorCallback.onError(task.getException().getMessage());
                    }
                });
    }

    public void getQuestionDetails(String questionId, QuestionDetailsCallback questionDetailsCallback) {
        mFirestore.collection("Questions").document(questionId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot questionDoc = task.getResult();
                        if (questionDoc.exists()) {
                            Question question = new Question();
                            question.setQuestionText(questionDoc.getString("questionText"));
                            question.setAnswers((ArrayList<String>) questionDoc.get("answers"));
                            question.setCorrectAnswerIndex(questionDoc.getLong("correctAnswerIndex").intValue());
                            questionDetailsCallback.onQuestionDetailsLoaded(question);
                        }
                    }
                });
    }

    public void loadLeaderboardData(Date startDate, Date endDate, String categoryId, LeaderboardLoadedCallback leaderboardCallback, NoDataCallback noDataCallback, ErrorCallback errorCallback) {
        Query query = mFirestore.collection("AnsweredQuestions");

        if (startDate != null && endDate != null) {
            /*
                Azért szükséges hozzáadni egy napot, mert csak az adott nap 00:00:00-ig nézné a kvízeket,
                nem pedig az adott napi 23:59:59-ig
             */
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.add(Calendar.DATE, 1);
            Date endDatePlusOne = cal.getTime();

            query = query.whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDatePlusOne);
        }

        if (categoryId != null && !categoryId.equals("0")) {
            query = query.whereEqualTo("category", categoryId);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                processLeaderboardData(task.getResult(), leaderboardCallback, noDataCallback);
            } else {
                errorCallback.onError(task.getException().getMessage());
            }
        });
    }

    private void processLeaderboardData(QuerySnapshot querySnapshot, LeaderboardLoadedCallback leaderboardCallback, NoDataCallback noDataCallback) {
        HashMap<String, Integer> userPoints = new HashMap<>();

        for (QueryDocumentSnapshot document : querySnapshot) {
            String userId = document.getString("userId");
            boolean correct = document.getBoolean("correct");
            userPoints.put(userId, userPoints.getOrDefault(userId, 0) + (correct ? 25 : -25));
        }

        List<Map.Entry<String, Integer>> userList = new ArrayList<>(userPoints.entrySet());
        Collections.sort(userList, (entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        final CountDownLatch latch = new CountDownLatch(userList.size());
        HashMap<String, Integer> sortedMapWithUsernames = new HashMap<>();

        for (Map.Entry<String, Integer> entry : userList) {
            mFirestore.collection("Users").document(entry.getKey()).get()
                    .addOnCompleteListener(userTask -> {
                        if (userTask.isSuccessful() && userTask.getResult().exists()) {
                            String username = userTask.getResult().getString("username");
                            sortedMapWithUsernames.put(username, sortedMapWithUsernames.getOrDefault(username, 0) + entry.getValue());
                        }
                        latch.countDown();
                    });
        }

        new Thread(() -> {
            try {
                latch.await();
                List<Map.Entry<String, Integer>> finalList = new ArrayList<>(sortedMapWithUsernames.entrySet());
                Collections.sort(finalList, (entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                if (finalList.isEmpty()) {
                    noDataCallback.onNoData();
                } else {
                    leaderboardCallback.onLeaderboardLoaded(finalList);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    public interface CategoryScoreCallback {
        void onCategoryScoreLoaded(String categoryId, int correctAnswers, int totalAnswers);
    }

    public interface ErrorCallback {
        void onError(String error);
    }

    public interface AnsweredQuestionsCallback {
        void onAnsweredQuestionsLoaded(List<AnsweredQuestion> answeredQuestions);
    }

    public interface NoQuestionsCallback {
        void onNoQuestions();
    }

    public interface QuestionDetailsCallback {
        void onQuestionDetailsLoaded(Question question);
    }
    public interface LeaderboardLoadedCallback {
        void onLeaderboardLoaded(List<Map.Entry<String, Integer>> leaderboardData);
    }

    public interface NoDataCallback {
        void onNoData();
    }


}
