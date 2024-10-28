package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.models.Category;

import java.util.UUID;

public class CategoryEditUploadViewModel extends AndroidViewModel {
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mCategories;
    private final StorageReference mStorage;
    private FirebaseAuth mAuth;

    private final MutableLiveData<String> categoryId = new MutableLiveData<>();
    private final MutableLiveData<String> categoryName = new MutableLiveData<>();
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<String> imageUrl = new MutableLiveData<>();
    private final MutableLiveData<String> existingImageName = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isImageUploading = new MutableLiveData<>();
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>();
    private final MutableLiveData<Category> categoryData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();

    public CategoryEditUploadViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mCategories = mFirestore.collection("Categories");
        mStorage = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    public LiveData<String> getCategoryId() { return categoryId; }
    public LiveData<String> getCategoryName() { return categoryName; }
    public LiveData<Uri> getImageUri() { return imageUri; }
    public LiveData<String> getImageUrl() { return imageUrl; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Category> getCategoryData() { return categoryData; }
    public LiveData<Boolean> getIsImageUploading() { return isImageUploading; }
    public MutableLiveData<Integer> getUploadProgress() { return uploadProgress; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }


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
        mCategories.document(categoryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Category category = documentSnapshot.toObject(Category.class);
                        categoryData.setValue(category);
                        categoryName.setValue(category.getName());
                        existingImageName.setValue(category.getImage());

                        if(category.getImage() != null) {
                            mStorage.child("images/" + category.getImage())
                                    .getDownloadUrl()
                                    .addOnSuccessListener(uri -> imageUrl.postValue(uri.toString()));
                        }
                    }
                });
    }

    public void uploadCategory(String name) {
        if (name.isEmpty()) {
            errorMessage.setValue("A kategória neve nem lehet üres");
            return;
        }
        categoryName.setValue(name);

        if (imageUri.getValue() != null) {
            uploadImage(imageUri.getValue());
        } else {
            if (categoryId.getValue() != null) {
                updateCategory(null);
            } else {
                addNewCategory(null);
            }
        }
    }

    private void uploadImage(Uri imageUri) {
        if (imageUri != null) {
            isImageUploading.setValue(true);

            String fileName = UUID.randomUUID().toString();
            StorageReference ref = mStorage.child("images/" + fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        String filename = taskSnapshot.getMetadata().getReference().getName();
                        if (categoryId.getValue() != null) {
                            updateCategory(filename);
                        } else {
                            addNewCategory(filename);
                        }
                    })
                    .addOnFailureListener(e -> {
                        errorMessage.setValue("Failed: " + e.getMessage());
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        uploadProgress.setValue((int) progress);
                    });
        }
    }

    private void updateCategory(String filename) {
        String imageToSave = (filename != null) ? filename : existingImageName.getValue();
        Category category = new Category(categoryId.getValue(), categoryName.getValue(), imageToSave);

        mFirestore.collection("Categories").document(categoryId.getValue())
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    clearData();
                    successMessage.setValue("A kategória sikeresen módosítva lett!");
                })
                .addOnFailureListener(e -> errorMessage.setValue("Hiba történt a frissítés során."));
    }

    private void addNewCategory(String filename) {
        Category category = new Category(null, categoryName.getValue(), filename);

        mCategories.add(category)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    category.setId(docId);
                    mFirestore.collection("Categories").document(docId)
                            .update("id", docId);
                    clearData();
                    successMessage.setValue("A kategória sikeresen létrehozva!");
                })
                .addOnFailureListener(e -> errorMessage.setValue("Sikertelen kategória hozzáadás!"));
    }

    public void deleteCategory() {
        if (categoryId.getValue() == null) {
            errorMessage.setValue("Nincs törlendő kategória");
            return;
        }

        mFirestore.collection("Categories")
                .whereEqualTo("name", "Besorolatlan")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String uncategorizedId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        updateQuestionsAfterCategoryDeletion(categoryId.getValue(), uncategorizedId);
                    } else {
                        createUncategorizedCategoryAndDelete(categoryId.getValue());
                    }
                })
                .addOnFailureListener(e -> errorMessage.setValue("Hiba történt a Besorolatlan kategória lekérdezésekor"));
    }

    private void createUncategorizedCategoryAndDelete(String currentCategoryId) {
        Category uncategorizedCategory = new Category(null, "Besorolatlan", null);

        mCategories.add(uncategorizedCategory)
                .addOnSuccessListener(documentReference -> {
                    String uncategorizedId = documentReference.getId();
                    uncategorizedCategory.setId(uncategorizedId);
                    mFirestore.collection("Categories").document(uncategorizedId)
                            .update("id", uncategorizedId);
                    updateQuestionsAfterCategoryDeletion(currentCategoryId, uncategorizedId);
                })
                .addOnFailureListener(e -> errorMessage.setValue("Hiba történt a Besorolatlan kategória létrehozása közben"));
    }

    private void updateQuestionsAfterCategoryDeletion(String currentCategoryId, String newCategoryId) {
        mFirestore.collection("Questions")
                .whereEqualTo("category", currentCategoryId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = mFirestore.batch();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        DocumentReference questionRef = document.getReference();
                        batch.update(questionRef, "category", newCategoryId);
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                mFirestore.collection("Categories").document(currentCategoryId)
                                        .delete()
                                        .addOnSuccessListener(aVoid2 -> successMessage.setValue("A kategória sikeresen törölve lett!"))
                                        .addOnFailureListener(e -> errorMessage.setValue("Hiba történt a törlés közben"));
                            })
                            .addOnFailureListener(e -> errorMessage.setValue("Hiba történt a kérdések frissítése közben"));
                })
                .addOnFailureListener(e -> errorMessage.setValue("Hiba történt a kérdések lekérdezése közben"));
    }

    private void clearData() {
        categoryName.setValue("");
        imageUri.setValue(null);
        existingImageName.setValue(null);
    }
}