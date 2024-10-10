package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityQuestionEditUploadBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class QuestionEditUploadActivity extends DrawerBaseActivity {
    private static final String LOG_TAG = QuestionEditUploadActivity.class.getName();

    private ActivityQuestionEditUploadBinding activityQuestionEditUploadBinding;
    private FirebaseFirestore mFirestore;

    private LinearLayout container;
    private LinearLayout editBarLinearLayout;
    private LinearLayout answerContainer;
    private LinearLayout manageImageLinearLayout;
    private Spinner spinner;

    private RadioGroup radioGroup;
    private int answerCounter = 2;

    private ImageView questionImagePreview;
    private Uri imageUri;

    private FirebaseStorage mStorage;

    private static final int PICK_IMAGE_REQUEST = 1;

    private StorageReference storageReference;
    private EditText explanationEditText;
    private TextView addQuestionTextView;
    private Button addQuestionButton;
    private Button backButton;
    private Button deleteButton;
    private Button addExplanationButton;
    private Button uploadImageButton;
    private Button deleteImageButton;
    private Button modifyImageButton;
    private String explanationText;
    private String questionText;
    private String category;
    private ArrayList<String> answers;
    private int correctAnswerIndex = -1;
    private String questionId; // Kérdés ID szerkesztés esetén
    private String existingImageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityQuestionEditUploadBinding = ActivityQuestionEditUploadBinding.inflate(getLayoutInflater());
        setContentView(activityQuestionEditUploadBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        storageReference = mStorage.getReference();
        spinner = findViewById(R.id.questionCategory);
        container = findViewById(R.id.container);
        answerContainer = findViewById(R.id.answerContainer);
        editBarLinearLayout = findViewById(R.id.editBarLinearLayout);
        manageImageLinearLayout = findViewById(R.id.manageImageLinearLayout);
        radioGroup = findViewById(R.id.radioGroup);
        addQuestionTextView = findViewById(R.id.addQuestionTextView);
        addQuestionButton = findViewById(R.id.addQuestionButton);
        backButton = findViewById(R.id.backButton);
        deleteButton = findViewById(R.id.deleteButton);
        addExplanationButton = findViewById(R.id.addExplanationButton);

        questionImagePreview = findViewById(R.id.questionImagePreview);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        modifyImageButton = findViewById(R.id.modifyImageButton);
        deleteImageButton = findViewById(R.id.deleteImageButton);

        questionId = getIntent().getStringExtra("questionId");

        mFirestore.collection("Categories").orderBy("name").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Category> categoryList = new ArrayList<>();
            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                Category category = documentSnapshot.toObject(Category.class);
                category.setId(documentSnapshot.getId());
                categoryList.add(category);
            }

            ArrayAdapter<Category> adapter = new ArrayAdapter<>(QuestionEditUploadActivity.this, android.R.layout.simple_spinner_item, categoryList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        });

        if (questionId != null) {
            editBarLinearLayout.setVisibility(View.VISIBLE);
            loadQuestionData(questionId);
            addQuestionTextView.setText("Kérdés szerkesztése");
            addQuestionButton.setText("Kérdés módosítása");
        }

        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
            }
        });

        addExplanationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddExplanationPopup();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(QuestionEditUploadActivity.this, QuestionListActivity.class);
                startActivity(intent);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(questionId != null) {
                    showDeleteConfirmationDialog(questionId);
                } else{
                    Toast.makeText(QuestionEditUploadActivity.this, "Nincs törlendő kérdés", Toast.LENGTH_SHORT).show();
                }
            }
        });

        deleteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageUri = null;
                existingImageName = null;

                questionImagePreview.setVisibility(View.GONE);
                uploadImageButton.setVisibility(View.VISIBLE);
                manageImageLinearLayout.setVisibility(View.GONE);
            }
        });

        modifyImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
            }
        });
    }

    private void loadQuestionData(String questionId) {
        mFirestore.collection("Questions").document(questionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Question question = documentSnapshot.toObject(Question.class);

                        questionText = question.getQuestionText();
                        answers = question.getAnswers();
                        correctAnswerIndex = question.getCorrectAnswerIndex();
                        category = question.getCategory(); // This is the category ID
                        explanationText = question.getExplanationText();
                        existingImageName = question.getImage();

                        mFirestore.collection("Categories")
                                .whereEqualTo("id", category)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        DocumentSnapshot categoryDoc = queryDocumentSnapshots.getDocuments().get(0);
                                        category = categoryDoc.getString("name");
                                        Log.d("kategórialoadban", category);
                                    }
                                    updateUI();
                                })
                                .addOnFailureListener(e -> {
                                    category = "Besorolatlan";
                                    updateUI();
                                });
                    }
                });
    }

    private void updateUI() {
        ((EditText) findViewById(R.id.questionName)).setText(questionText);
        Log.d("kategóriaupdateui", category);
        spinner.setSelection(getSpinnerIndex(spinner, category));

        for (int i = 0; i < answers.size(); i++) {
            EditText answerEditText = (EditText) answerContainer.getChildAt(i);
            RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
            answerEditText.setText(answers.get(i));

            if (i == correctAnswerIndex) {
                radioButton.setChecked(true);
            }
        }

        if (existingImageName != null && !existingImageName.isEmpty()) {
            uploadImageButton.setVisibility(View.GONE);
            manageImageLinearLayout.setVisibility(View.VISIBLE);
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + existingImageName);
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                Glide.with(this)
                        .load(imageUrl)
                        .into(questionImagePreview);
                questionImagePreview.setVisibility(View.VISIBLE);
            }).addOnFailureListener(e -> {
                questionImagePreview.setVisibility(View.GONE);
                Toast.makeText(QuestionEditUploadActivity.this, "Hiba a kép betöltés során", Toast.LENGTH_SHORT).show();
            });
        } else {
            questionImagePreview.setVisibility(View.GONE);
        }
    }

    private int getSpinnerIndex(Spinner spinner, String categoryName) {
        for (int i = 0; i < spinner.getCount(); i++) {
            Category category = (Category) spinner.getItemAtPosition(i);
            if (category.getName().equals(categoryName)) {
                return i;
            }
        }
        return 0;
    }

    public void uploadQuestion(View view) {
        if (validateQuestionInput()) {
            if (imageUri != null) {
                uploadImage(imageUri);  // Kép feltöltés, majd ezután kérdés mentése/frissítése
            } else {
                if (questionId != null) {
                    updateQuestion(null);  // Kép nélkül kérdés frissítése
                } else {
                    saveQuestion(null);  // Kép nélkül új kérdés mentése
                }
            }
        }
    }


    private boolean validateQuestionInput() {
        questionText = ((EditText) findViewById(R.id.questionName)).getText().toString();
        category = ((Spinner) findViewById(R.id.questionCategory)).getSelectedItem().toString();
        ArrayList<Boolean> answerInputHasValue = new ArrayList<>();
        answers = new ArrayList<>();

        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            EditText answer = (EditText) answerContainer.getChildAt(i);
            RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);

            if (answer.getText().toString().isEmpty() && radioButton.isChecked()) {
                answer.setError("Nem lehet üres válasz a helyes");
                return false;
            }

            if (radioButton.isChecked()) {
                correctAnswerIndex = i;
            }

            if (!answer.getText().toString().isEmpty()) {
                answerInputHasValue.add(true);
            } else {
                answerInputHasValue.add(false);
            }

            if (!answer.getText().toString().isEmpty()) {
                answers.add(answer.getText().toString());
            }
        }

        if (correctAnswerIndex == -1) {
            Toast.makeText(this, "Kérjük, jelöljön meg egy helyes választ!", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (int i = 0; i < answerInputHasValue.size() - 1; i++) {
            if (!answerInputHasValue.get(i) && answerInputHasValue.get(i + 1)) {
                showErrorDialog("Az egyik válaszlehetőség üres, miközben utána az lévő mezőben meg van adva egy válaszlehetőség!");
                return false;
            }
        }

        return true;
    }

    private void updateQuestion(String fileName) {
        String imageToSave = (fileName != null) ? fileName : existingImageName;
        Category selectedCategory = (Category) spinner.getSelectedItem();
        String selectedCategoryId = selectedCategory.getId();

        Question question = new Question(questionId, questionText, selectedCategoryId, answers, correctAnswerIndex, imageToSave, explanationText);

        mFirestore.collection("Questions").document(questionId)
                .set(question)
                .addOnSuccessListener(aVoid -> {
                    clearInputFields();
                    showSuccessDialog("Sikeres módosítás!", "A kérdés sikeresen módosítva lett!");
                    uploadImageButton.setVisibility(View.VISIBLE);
                    manageImageLinearLayout.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> Toast.makeText(QuestionEditUploadActivity.this, "Hiba történt a frissítés során.", Toast.LENGTH_SHORT).show());
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            if(CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
            } else {
                startCrop(imageUri);
            }
        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK) {
                imageUri = result.getUri();
                questionImagePreview.setImageURI(imageUri);
                questionImagePreview.setVisibility(View.VISIBLE);
                uploadImageButton.setVisibility(View.GONE);
                manageImageLinearLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startCrop(Uri imageUri) {
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMultiTouchEnabled(true)
                .start(this);
    }
    private void uploadImage(Uri imageUri) {
        if(imageUri != null) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Feltöltés...");
            progressDialog.show();

            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());

            ref.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            String fileName = taskSnapshot.getMetadata().getReference().getName();
                            if (questionId != null) {
                                updateQuestion(fileName);
                            } else {
                                saveQuestion(fileName);  // Új kérdés létrehozása
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(QuestionEditUploadActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Feltöltve: " + (int) progress + "%");
                        }
                    });
        }
    }

    private void saveQuestion(String fileName) {
        Category selectedCategory = (Category) spinner.getSelectedItem();
        String selectedCategoryId = selectedCategory.getId();

        Question question = new Question(null, questionText, selectedCategoryId, answers, correctAnswerIndex, fileName, explanationText);

        mFirestore.collection("Questions").add(question)
                .addOnSuccessListener(documentReference -> {
                    String documentId = documentReference.getId();
                    question.setId(documentId);

                    mFirestore.collection("Questions").document(documentId)
                            .update("id", documentId);

                    clearInputFields();
                    showSuccessDialog("Sikeres feltöltés", "A kérdés sikeresen feltöltve!");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(QuestionEditUploadActivity.this, "Hiba történt a mentés során.", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearInputFields() {
        ((EditText) findViewById(R.id.questionName)).setText("");
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            ((EditText) answerContainer.getChildAt(i)).setText("");
            RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
            radioButton.setChecked(false);
        }

        spinner.setSelection(0);

        imageUri = null;
        questionImagePreview.setVisibility(View.GONE);
    }

    private void showSuccessDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hiba")
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }

    public void showAddExplanationPopup() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_add_explanation, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);

        explanationEditText = popupView.findViewById(R.id.editExplanationTextMultiLine);

        if (explanationText != null && !explanationText.isEmpty()) {
            explanationEditText.setText(explanationText);
        }

        Button mPopUpCloseButton = popupView.findViewById(R.id.popUpCloseButton);
        Button mGenerateTextButton = popupView.findViewById(R.id.generateTextButton);
        Button mSaveExplanationButton = popupView.findViewById(R.id.saveExplanationText);

        mGenerateTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateQuestionInput()) {
                    generateExplanationWithFirebase();
                }
            }
        });


        mPopUpCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        mSaveExplanationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                explanationText = explanationEditText.getText().toString();
                Toast.makeText(QuestionEditUploadActivity.this, "Sikeresen elmentve!", Toast.LENGTH_SHORT).show();
            }
        });


        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    public static void dimBehind(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(container, p);
    }

    private void generateExplanationWithFirebase() {
        String prompt = "Kérlek, készíts egy pontos, 3-6 mondatos választ az alábbi kérdésre, figyelembe véve, hogy a helyes válasz kizárólag a megadott helyes válasz lehet: \nKérdés: " + questionText + "\nHelyes válasz: " + answers.get(correctAnswerIndex) + ".";

        FirebaseFunctions functions = FirebaseFunctions.getInstance();

        functions
                .getHttpsCallable("getChatGPTResponse")
                .call(Collections.singletonMap("prompt", prompt))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> resultMap = (Map<String, Object>) task.getResult().getData();

                        if (resultMap.containsKey("response")) {
                            String explanation = (String) resultMap.get("response");

                            runOnUiThread(() -> {
                                explanationEditText.setText(explanation);
                                explanationText = explanation;
                            });
                        } else {
                            Toast.makeText(this, "Hiba történt a generálás során", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Hiba történt a generálás során", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDeleteConfirmationDialog(String questionId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kérdés törlése");
        builder.setMessage("Biztosan törölni szeretnéd ezt a kérdést?");

        builder.setPositiveButton("Igen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteQuestion(questionId);
            }
        });

        builder.setNegativeButton("Nem", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteQuestion(String questionId) {
        mFirestore.collection("Questions").document(questionId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Kérdés sikeresen törölve", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Hiba történt a törlés közben", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}