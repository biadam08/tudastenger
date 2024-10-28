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

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private static final String CHANNEL_ID = "FRIEND_REQUESTS_NOTIFICATION";
    private FirebaseFirestore mFirestore;
    private CollectionReference mCategories;

    private MutableLiveData<List<Category>> categoriesData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> areNotificationsEnabled = new MutableLiveData<>();


    public MainViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mCategories = mFirestore.collection("Categories");

        categoriesData.setValue(new ArrayList<>());
    }

    public LiveData<List<Category>> getCategoriesData() { return categoriesData; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadCategories() {
        mCategories.orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> categoryList = new ArrayList<>();
                    for(QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        categoryList.add(category);
                    }
                    categoriesData.setValue(categoryList);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                });
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