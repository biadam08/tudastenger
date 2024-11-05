package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.repositories.CategoryRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends AndroidViewModel {
    private static final String CHANNEL_ID = "FRIEND_REQUESTS_NOTIFICATION";
    private CategoryRepository categoryRepository;

    private MutableLiveData<List<Category>> categoriesData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> areNotificationsEnabled = new MutableLiveData<>();

    @Inject
    public MainViewModel(Application application, CategoryRepository categoryRepository) {
        super(application);
        this.categoryRepository = categoryRepository;
        categoriesData.setValue(new ArrayList<>());
    }

    public LiveData<List<Category>> getCategoriesData() { return categoriesData; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadCategories() {
        categoryRepository.loadCategories(categories -> categoriesData.setValue(categories), error -> errorMessage.setValue(error));
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Értesítések";
            String description = "Értesítések a barátkérésekről és a párbajokról";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getApplication().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public boolean checkNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
            boolean enabled = manager != null && manager.areNotificationsEnabled();
            areNotificationsEnabled.setValue(enabled);
            return enabled;
        }
        areNotificationsEnabled.setValue(true);
        return true;
    }
}