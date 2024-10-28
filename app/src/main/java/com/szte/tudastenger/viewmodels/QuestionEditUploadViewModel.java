package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestionEditUploadViewModel extends AndroidViewModel {
    private FirebaseFirestore mFirestore;
    private StorageReference mStorage;
    private FirebaseFunctions mFunctions;
    private FirebaseAuth mAuth;

    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> successMessage = new MutableLiveData<>();
    private MutableLiveData<String> explanationText = new MutableLiveData<>();
    private MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private MutableLiveData<String> imageUrl = new MutableLiveData<>();
    private MutableLiveData<String> existingImageName = new MutableLiveData<>();
    private MutableLiveData<Boolean> isImageUploading = new MutableLiveData<>();
    private MutableLiveData<Integer> uploadProgress = new MutableLiveData<>();
    private MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();

    public QuestionEditUploadViewModel(Application application) {
        super(application);
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance().getReference();
        mFunctions = FirebaseFunctions.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Question> getCurrentQuestion() { return currentQuestion; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<String> getExplanationText() { return explanationText; }
    public LiveData<Uri> getImageUri() { return imageUri; }
    public LiveData<String> getImageUrl() { return imageUrl; }
    public LiveData<String> getExistingImageName() { return existingImageName; }
    public LiveData<Boolean> getIsImageUploading() { return isImageUploading; }
    public LiveData<Integer> getUploadProgress() { return uploadProgress; }
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

    public void loadCategories() {
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
                    categories.postValue(categoryList);
                })
                .addOnFailureListener(e -> errorMessage.postValue(e.getMessage()));
    }

    public void loadQuestionData(String questionId) {
        if (questionId == null) return;

        mFirestore.collection("Questions")
                .document(questionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Question question = documentSnapshot.toObject(Question.class);
                        currentQuestion.postValue(question);
                        explanationText.postValue(question.getExplanationText());
                        existingImageName.setValue(question.getImage());

                        if (question.getImage() != null && !question.getImage().isEmpty()) {
                            loadQuestionImage(question.getImage());
                        }
                    } else {
                        errorMessage.postValue("Kérdés nem található");
                    }
                })
                .addOnFailureListener(e -> errorMessage.postValue(e.getMessage()));
    }

    public void loadQuestionImage(String imageName) {
        mStorage.child("images/" + imageName)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> imageUrl.postValue(uri.toString()))
                .addOnFailureListener(e -> errorMessage.postValue(e.getMessage()));
    }

    public void setImageUri(Uri uri) {
        imageUri.setValue(uri);
    }

    public void clearImageUri() {
        imageUri.setValue(null);
        imageUrl.setValue(null);
    }

    public void uploadQuestion(Question question, Uri imageUri) {
        if (imageUri != null) {
            isImageUploading.setValue(true);
            String fileName = UUID.randomUUID().toString();
            StorageReference ref = mStorage.child("images/" + fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        question.setImage(fileName);
                        saveOrUpdateQuestion(question);
                        isImageUploading.setValue(false);
                    })
                    .addOnFailureListener(e -> {
                        errorMessage.postValue(e.getMessage());
                        isImageUploading.setValue(false);
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        int progress = (int) ((100.0 * taskSnapshot.getBytesTransferred()) /
                                taskSnapshot.getTotalByteCount());
                        uploadProgress.postValue(progress);
                    });
        } else {
            saveOrUpdateQuestion(question);
        }
    }

    private void saveOrUpdateQuestion(Question question) {
        if (question.getId() != null) {
            mFirestore.collection("Questions")
                    .document(question.getId())
                    .set(question)
                    .addOnSuccessListener(aVoid -> {
                        updateSuccess.postValue(true);
                        successMessage.postValue("A kérdés sikeresen módosítva lett!");
                        currentQuestion.postValue(question);
                    })
                    .addOnFailureListener(e -> errorMessage.postValue(e.getMessage()));
        } else {
            mFirestore.collection("Questions")
                    .add(question)
                    .addOnSuccessListener(documentReference -> {
                        String documentId = documentReference.getId();
                        question.setId(documentId);
                        mFirestore.collection("Questions")
                                .document(documentId)
                                .update("id", documentId)
                                .addOnSuccessListener(aVoid -> {
                                    successMessage.postValue("A kérdés sikeresen feltöltve!");
                                    currentQuestion.postValue(question);
                                })
                                .addOnFailureListener(e -> errorMessage.postValue(e.getMessage()));
                    })
                    .addOnFailureListener(e -> errorMessage.postValue(e.getMessage()));
        }
    }

    public void deleteQuestion(String questionId) {
        mFirestore.collection("Questions")
                .document(questionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    deleteSuccess.postValue(true);
                    successMessage.postValue("Kérdés sikeresen törölve");
                })
                .addOnFailureListener(e -> errorMessage.postValue(e.getMessage()));
    }

    public void generateExplanation(String questionText, String correctAnswer) {
        String prompt = "Kérlek, készíts egy pontos, 3-6 mondatos választ az alábbi kérdésre, figyelembe véve, hogy a helyes válasz kizárólag a megadott helyes válasz lehet: \nKérdés: " + questionText + "\nHelyes válasz: " + correctAnswer + ".";

        mFunctions.getHttpsCallable("getChatGPTResponse")
                .call(Collections.singletonMap("prompt", prompt))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> resultMap = (Map<String, Object>) task.getResult().getData();
                        if (resultMap.containsKey("response")) {
                            explanationText.postValue((String) resultMap.get("response"));
                        } else {
                            errorMessage.postValue("Nem sikerült magyarázatot generálni");
                        }
                    } else {
                        errorMessage.postValue(task.getException().getMessage());
                    }
                });
    }

    public void setExplanationText(String text) {
        explanationText.setValue(text);
    }
}