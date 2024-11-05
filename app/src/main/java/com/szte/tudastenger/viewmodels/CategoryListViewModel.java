package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CategoryListViewModel extends AndroidViewModel {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private MutableLiveData<List<Category>> categoriesData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @Inject
    public CategoryListViewModel(Application application, CategoryRepository categoryRepository, UserRepository userRepository) {
        super(application);
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public LiveData<List<Category>> getCategoriesData() { return categoriesData; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void checkAdmin() {
        userRepository.checkAdmin(isAdminStatus -> isAdmin.setValue(isAdminStatus), error -> errorMessage.setValue(error));
    }

    public void loadCategories() {
        categoryRepository.loadCategories(categories -> categoriesData.setValue(categories), error -> errorMessage.setValue(error));
    }
}
