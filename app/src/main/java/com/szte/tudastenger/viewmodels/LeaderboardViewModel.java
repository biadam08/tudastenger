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
import com.szte.tudastenger.repositories.AnsweredQuestionsRepository;
import com.szte.tudastenger.repositories.CategoryRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class LeaderboardViewModel extends AndroidViewModel {
    private final CategoryRepository categoryRepository;
    private final AnsweredQuestionsRepository answeredQuestionsRepository;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Map.Entry<String, Integer>>> leaderboardData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showNoData = new MutableLiveData<>();
    private final MutableLiveData<String> dateRangeText = new MutableLiveData<>();

    private Date startDate;
    private Date endDate;
    private String selectedCategoryId = "0";

    public LeaderboardViewModel(Application application) {
        super(application);
        categoryRepository = new CategoryRepository();
        answeredQuestionsRepository = new AnsweredQuestionsRepository();
        loadCategories();
    }

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Map.Entry<String, Integer>>> getLeaderboardData() { return leaderboardData; }
    public LiveData<Boolean> getShowNoData() { return showNoData; }
    public LiveData<String> getDateRangeText() { return dateRangeText; }

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
                    leaderboardData.postValue(leaderboardEntries);
                    showNoData.postValue(false);
                },
                () -> {
                    leaderboardData.postValue(null);
                    showNoData.postValue(true);
                },
                error -> {
                    leaderboardData.postValue(null);
                    showNoData.postValue(true);
                }
        );
    }
}
