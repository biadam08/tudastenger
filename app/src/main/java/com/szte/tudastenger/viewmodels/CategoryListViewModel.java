package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.models.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryListViewModel extends AndroidViewModel {
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;

    private MutableLiveData<List<Category>> categoriesData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public CategoryListViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public LiveData<List<Category>> getCategoriesData() { return categoriesData; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void checkAdmin() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            isAdmin.setValue(false);
            return;
        }

        mFirestore.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean role = documentSnapshot.exists() &&
                            "admin".equals(documentSnapshot.getString("role"));
                    isAdmin.setValue(role);
                })
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    public void loadCategories() {
        mFirestore.collection("Categories")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> categoryList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        categoryList.add(category);
                    }
                    categoriesData.setValue(categoryList);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                });
    }
}

