package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.LeaderboardEntry;
import com.szte.tudastenger.repositories.AnsweredQuestionsRepository;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LeaderboardViewModel extends AndroidViewModel {
    private final CategoryRepository categoryRepository;
    private final AnsweredQuestionsRepository answeredQuestionsRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Map.Entry<String, Integer>>> leaderboardData = new MutableLiveData<>();
    private final MutableLiveData<List<LeaderboardEntry>> processedLeaderboardData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNoData = new MutableLiveData<>();
    private final MutableLiveData<String> dateRangeText = new MutableLiveData<>();

    private Date startDate;
    private Date endDate;
    private String selectedCategoryId = "0";

    @Inject
    public LeaderboardViewModel(Application application, CategoryRepository categoryRepository, AnsweredQuestionsRepository answeredQuestionsRepository, UserRepository userRepository) {
        super(application);
        this.categoryRepository = categoryRepository;
        this.answeredQuestionsRepository = answeredQuestionsRepository;
        this.userRepository = userRepository;
        loadCategories();
    }

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Map.Entry<String, Integer>>> getLeaderboardData() { return leaderboardData; }
    public LiveData<Boolean> getShowNoData() { return showNoData; }
    public LiveData<String> getDateRangeText() { return dateRangeText; }

    public MutableLiveData<List<LeaderboardEntry>> getProcessedLeaderboardData() {
        return processedLeaderboardData;
    }

    private void loadCategories() {
        categoryRepository.loadCategoriesWithAll(
                categoryList -> categories.setValue(categoryList),
                error -> {}
        );
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
        answeredQuestionsRepository.loadLeaderboardData(
                startDate,
                endDate,
                selectedCategoryId,
                leaderboardEntries -> {
                    processLeaderBoardEntries(leaderboardEntries);
                    showNoData.postValue(false);
                },
                () -> {
                    leaderboardData.postValue(null);
                    processedLeaderboardData.postValue(null);
                    showNoData.postValue(true);
                },
                error -> {
                    leaderboardData.postValue(null);
                    processedLeaderboardData.postValue(null);
                    showNoData.postValue(true);
                }
        );
    }

    private void processLeaderBoardEntries(List<Map.Entry<String, Integer>> leaderboardEntries) {
        List<LeaderboardEntry> entryList = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(leaderboardEntries.size());

        for (Map.Entry<String, Integer> entry : leaderboardEntries) {
            String username = entry.getKey();
            int points = entry.getValue();

            userRepository.getUserRankByName(username, new UserRepository.SuccessCallback() {
                @Override
                public void onSuccess(String rank) {
                    entryList.add(new LeaderboardEntry(username, points, rank));

                    // ellenőrizzük, hogy az összes lekérdezés befejeződött
                    if (counter.decrementAndGet() == 0) {
                        entryList.sort((e1, e2) -> Integer.compare(e2.getPoints(), e1.getPoints()));
                        processedLeaderboardData.postValue(entryList);                    }
                }
            }, new UserRepository.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    entryList.add(new LeaderboardEntry(username, points, ""));
                    
                    // ellenőrizzük, hogy az összes lekérdezés befejeződött
                    if (counter.decrementAndGet() == 0) {
                        entryList.sort((e1, e2) -> Integer.compare(e2.getPoints(), e1.getPoints()));
                        processedLeaderboardData.postValue(entryList);
                    }
                }
            });
        }
    }

}

