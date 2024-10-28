package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.models.Category;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class LeaderboardViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mUsers;
    private final CollectionReference mAnsweredQuestions;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Map.Entry<String, Integer>>> leaderboardData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNoData = new MutableLiveData<>();
    private final MutableLiveData<String> dateRangeText = new MutableLiveData<>();

    private Date startDate;
    private Date endDate;
    private String selectedCategoryId = "0";

    public LeaderboardViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
        mAnsweredQuestions = mFirestore.collection("AnsweredQuestions");
        loadCategories();
    }

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Map.Entry<String, Integer>>> getLeaderboardData() { return leaderboardData; }
    public LiveData<Boolean> getShowNoData() { return showNoData; }
    public LiveData<String> getDateRangeText() { return dateRangeText; }

    private void loadCategories() {
        List<Category> categoryList = new ArrayList<>();
        categoryList.add(new Category("0", "Összes kategória", null));

        mFirestore.collection("Categories").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        category.setId(document.getId());
                        categoryList.add(category);
                    }
                    categories.setValue(categoryList);
                });
    }

    public void setDateRange(Date start, Date end, String startString, String endString) {
        this.startDate = start;
        this.endDate = end;
        if (start != null && end != null) {
            dateRangeText.setValue(startString + " - " + endString);
        } else {
            dateRangeText.setValue(null);
        }
        loadLeaderboardData();
    }

    public void clearDateRange() {
        startDate = null;
        endDate = null;
        dateRangeText.setValue(null);
        loadLeaderboardData();
    }

    public void setSelectedCategory(String categoryId) {
        this.selectedCategoryId = categoryId;
        loadLeaderboardData();
    }

    private void loadLeaderboardData() {
        Query query = mAnsweredQuestions;

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

        if (selectedCategoryId != null && !selectedCategoryId.equals("0")) {
            query = query.whereEqualTo("category", selectedCategoryId);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                HashMap<String, Integer> userPoints = new HashMap<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String userId = document.getString("userId");
                    boolean correct = document.getBoolean("correct");
                    userPoints.put(userId, userPoints.getOrDefault(userId, 0) + (correct ? 25 : -25));
                }

                List<Map.Entry<String, Integer>> userList = new ArrayList<>(userPoints.entrySet());
                Collections.sort(userList, (entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                final CountDownLatch latch = new CountDownLatch(userList.size());
                HashMap<String, Integer> sortedMapWithUsernames = new HashMap<>();

                for (Map.Entry<String, Integer> entry : userList) {
                    mUsers.document(entry.getKey()).get()
                            .addOnCompleteListener(userTask -> {
                                if (userTask.isSuccessful() && userTask.getResult().exists()) {
                                    String username = userTask.getResult().getString("username");
                                    sortedMapWithUsernames.put(username,
                                            sortedMapWithUsernames.getOrDefault(username, 0) + entry.getValue());
                                }
                                latch.countDown();
                            });
                }

                new Thread(() -> {
                    try {
                        latch.await();
                        List<Map.Entry<String, Integer>> finalList = new ArrayList<>(sortedMapWithUsernames.entrySet());
                        Collections.sort(finalList, (entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                        showNoData.postValue(finalList.isEmpty());
                        leaderboardData.postValue(finalList);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showNoData.setValue(true);
            }
        });
    }
}
