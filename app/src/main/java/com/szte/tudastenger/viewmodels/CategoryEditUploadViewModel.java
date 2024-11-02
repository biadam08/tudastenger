package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.*;

public class CategoryEditUploadViewModel extends AndroidViewModel {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<String> categoryId = new MutableLiveData<>();
    private final MutableLiveData<String> categoryName = new MutableLiveData<>();
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<String> imageUrl = new MutableLiveData<>();
    private final MutableLiveData<String> existingImageName = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isImageUploading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>();
    private final MutableLiveData<Category> categoryData = new MutableLiveData<>();

    public CategoryEditUploadViewModel(Application application) {
        super(application);
        categoryRepository = new CategoryRepository();
        userRepository = new UserRepository();
    }

    public LiveData<String> getCategoryId() { return categoryId; }
    public LiveData<String> getCategoryName() { return categoryName; }
    public LiveData<Uri> getImageUri() { return imageUri; }
    public LiveData<String> getImageUrl() { return imageUrl; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Category> getCategoryData() { return categoryData; }
    public LiveData<Boolean> getIsImageUploading() { return isImageUploading; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public MutableLiveData<Integer> getUploadProgress() { return uploadProgress; }

    public void checkAdmin() {
        userRepository.checkAdmin(isAdminStatus -> isAdmin.setValue(isAdminStatus), error -> errorMessage.setValue(error));
    }

    public void init(String categoryId) {
        this.categoryId.setValue(categoryId);
        if (categoryId != null) {
            loadCategoryData(categoryId);
        }
    }

    public void setImageUri(Uri uri) {
        imageUri.setValue(uri);
    }

    public void clearImage() {
        imageUri.setValue(null);
        existingImageName.setValue(null);
    }

    private void loadCategoryData(String categoryId) {
        categoryRepository.loadCategoryData(categoryId, category -> {
            categoryData.setValue(category);
            categoryName.setValue(category.getName());
            existingImageName.setValue(category.getImage());
        }, uri -> imageUrl.postValue(uri.toString()));
    }

    public void uploadCategory(String name) {
        if (name.isEmpty()) {
            errorMessage.setValue("A kategória neve nem lehet üres");
            return;
        }
        categoryName.setValue(name);

        if (imageUri.getValue() != null) {
            isImageUploading.setValue(true);
            categoryRepository.uploadImage(imageUri.getValue(), filename -> {
                if (categoryId.getValue() != null) {
                    updateCategory(filename);
                } else {
                    addNewCategory(filename);
                }
            }, error -> errorMessage.setValue(error), progress -> uploadProgress.setValue(progress));
        } else {
            if (categoryId.getValue() != null) {
                updateCategory(null);
            } else {
                addNewCategory(null);
            }
        }
    }

    private void updateCategory(String filename) {
        String imageToSave = (filename != null) ? filename : existingImageName.getValue();
        Category category = new Category(categoryId.getValue(), categoryName.getValue(), imageToSave);
        categoryRepository.updateCategory(category, message -> {
            clearData();
            successMessage.setValue(message);
        }, error -> errorMessage.setValue(error));
    }

    private void addNewCategory(String filename) {
        Category category = new Category(null, categoryName.getValue(), filename);
        categoryRepository.addNewCategory(category, message -> {
            clearData();
            successMessage.setValue(message);
        }, error -> errorMessage.setValue(error));
    }

    public void deleteCategory() {
        if (categoryId.getValue() == null) {
            errorMessage.setValue("Nincs törlendő kategória");
            return;
        }
        categoryRepository.deleteCategory(categoryId.getValue(), message -> successMessage.setValue(message),
                error -> errorMessage.setValue(error));
    }

    private void clearData() {
        categoryName.setValue("");
        imageUri.setValue(null);
    }
}
