package com.szte.tudastenger.repositories;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.models.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class CategoryRepository {
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mCategories;
    private final StorageReference mStorage;

    @Inject
    public CategoryRepository(FirebaseFirestore firestore, StorageReference storage) {
        this.mFirestore = firestore;
        this.mCategories = mFirestore.collection("Categories");
        this.mStorage = storage;
    }

    public void loadCategoryData(String categoryId, CategoryReceivedCallback categoryReceivedCallback, ImageUrlCallback imageUrlCallback) {
        mCategories.document(categoryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Category category = documentSnapshot.toObject(Category.class);
                        categoryReceivedCallback.onCategoryReceived(category);

                        if(category.getImage() != null) {
                            mStorage.child("images/" + category.getImage())
                                    .getDownloadUrl()
                                    .addOnSuccessListener(uri -> imageUrlCallback.onImageUrlReceived(uri.toString()));
                        }
                    }
                });
    }

    public void uploadImage(Uri imageUri, SuccessCallback successCallback, ErrorCallback errorCallback, ProgressCallback progressCallback) {
        if (imageUri != null) {
            String fileName = UUID.randomUUID().toString();
            StorageReference ref = mStorage.child("images/" + fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        String filename = taskSnapshot.getMetadata().getReference().getName();
                        successCallback.onSuccess(filename);
                    })
                    .addOnFailureListener(e -> {
                        errorCallback.onError("Failed: " + e.getMessage());
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        progressCallback.onProgress((int) progress);
                    });
        }
    }

    public void updateCategory(Category category, SuccessCallback successCallback, ErrorCallback errorCallback) {
        mCategories.document(category.getId())
                .set(category)
                .addOnSuccessListener(aVoid -> successCallback.onSuccess("A kategória sikeresen módosítva lett!"))
                .addOnFailureListener(e -> errorCallback.onError("Hiba történt a frissítés során."));
    }

    public void addNewCategory(Category category, SuccessCallback successCallback, ErrorCallback errorCallback) {
        mCategories.add(category)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    category.setId(docId);
                    mFirestore.collection("Categories").document(docId)
                            .update("id", docId);
                    successCallback.onSuccess("A kategória sikeresen létrehozva!");
                })
                .addOnFailureListener(e -> errorCallback.onError("Sikertelen kategória hozzáadás!"));
    }

    public void deleteCategory(String categoryId, SuccessCallback successCallback, ErrorCallback errorCallback) {
        mFirestore.collection("Categories")
                .whereEqualTo("name", "Besorolatlan")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String uncategorizedId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        updateQuestionsAfterCategoryDeletion(categoryId, uncategorizedId, successCallback, errorCallback);
                    } else {
                        createUncategorizedCategoryAndDelete(categoryId, successCallback, errorCallback);
                    }
                })
                .addOnFailureListener(e -> errorCallback.onError("Hiba történt a Besorolatlan kategória lekérdezésekor"));
    }

    private void createUncategorizedCategoryAndDelete(String currentCategoryId, SuccessCallback successCallback, ErrorCallback errorCallback) {
        Category uncategorizedCategory = new Category(null, "Besorolatlan", null);

        mCategories.add(uncategorizedCategory)
                .addOnSuccessListener(documentReference -> {
                    String uncategorizedId = documentReference.getId();
                    uncategorizedCategory.setId(uncategorizedId);
                    mFirestore.collection("Categories").document(uncategorizedId)
                            .update("id", uncategorizedId);
                    updateQuestionsAfterCategoryDeletion(currentCategoryId, uncategorizedId, successCallback, errorCallback);
                })
                .addOnFailureListener(e -> errorCallback.onError("Hiba történt a Besorolatlan kategória létrehozása közben"));
    }

    private void updateQuestionsAfterCategoryDeletion(String currentCategoryId, String newCategoryId, SuccessCallback successCallback, ErrorCallback errorCallback) {
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
                                        .addOnSuccessListener(aVoid2 -> successCallback.onSuccess("A kategória sikeresen törölve lett!"))
                                        .addOnFailureListener(e -> errorCallback.onError("Hiba történt a törlés közben"));
                            })
                            .addOnFailureListener(e -> errorCallback.onError("Hiba történt a kérdések frissítése közben"));
                })
                .addOnFailureListener(e -> errorCallback.onError("Hiba történt a kérdések lekérdezése közben"));
    }

    public void loadCategoriesWithAll(CategoryLoadedCallback categoryLoadedCallback, ErrorCallback errorCallback) {
        mFirestore.collection("Categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> categoryList = new ArrayList<>();
                    categoryList.add(new Category("0", "Összes kategória", null));

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        category.setId(document.getId());
                        categoryList.add(category);
                    }
                    categoryLoadedCallback.onCategoriesLoaded(categoryList);
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void loadCategories(CategoryLoadedCallback categoryLoadedCallback, QuestionRepository.ErrorCallback errorCallback) {
        mFirestore.collection("Categories")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> categoryList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Category category = doc.toObject(Category.class);
                        category.setId(doc.getId());
                        categoryList.add(category);
                    }
                    categoryLoadedCallback.onCategoriesLoaded(categoryList);
                })
                .addOnFailureListener(e -> errorCallback.onError(e.getMessage()));
    }

    public void selectCategory(String categoryName, CategorySelectedCallback categorySelectedCallback){
        mCategories.whereEqualTo("name", categoryName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String categoryId = documentSnapshot.getId();
                        categorySelectedCallback.onCategorySelected(categoryId);
                    }
                });
    }


    public interface SuccessCallback {
        void onSuccess(String message);
    }
    public interface ErrorCallback {
        void onError(String error);
    }

    public interface ProgressCallback {
        void onProgress(int progress);
    }

    public interface ImageUrlCallback {
        void onImageUrlReceived(String url);
    }

    public interface CategoryReceivedCallback {
        void onCategoryReceived(Category category);
    }

    public interface CategoryLoadedCallback {
        void onCategoriesLoaded(List<Category> categories);
    }

    public interface CategorySelectedCallback {
        void onCategorySelected(String categoryId);
    }
}
