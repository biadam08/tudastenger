package com.szte.tudastenger.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityEditQuestionBinding;
import com.szte.tudastenger.databinding.ActivityMainBinding;
import com.szte.tudastenger.models.Question;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.ArrayList;
import java.util.UUID;

public class EditQuestionActivity extends DrawerBaseActivity {

    private ActivityEditQuestionBinding activityEditQuestionBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseStorage mStorage;
    private StorageReference storageReference;

    private EditText questionTextEdit;
    private Spinner categorySpinner;
    private RadioGroup answerRadioGroup;
    private EditText[] answerEditTexts = new EditText[4];
    private ImageView questionImagePreview;
    private Button updateQuestionButton;

    private String questionId;
    private Uri imageUri;

    private int correctAnswerIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityEditQuestionBinding = ActivityEditQuestionBinding.inflate(getLayoutInflater());
        setContentView(activityEditQuestionBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        storageReference = mStorage.getReference();

        questionTextEdit = findViewById(R.id.editQuestionText);
        categorySpinner = findViewById(R.id.editCategorySpinner);
        answerRadioGroup = findViewById(R.id.editRadioGroup);
        questionImagePreview = findViewById(R.id.editQuestionImagePreview);
        updateQuestionButton = findViewById(R.id.updateQuestionButton);

        // questionId megszerzése az intentből
        questionId = getIntent().getStringExtra("questionId");

        loadQuestionData(questionId);

        updateQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateQuestion();
            }
        });
    }

    private void loadQuestionData(String questionId) {
        mFirestore.collection("Questions").document(questionId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Question question = documentSnapshot.toObject(Question.class);

                            questionTextEdit.setText(question.getQuestionText());

                            correctAnswerIndex = question.getCorrectAnswerIndex();

                        } else {
                            Toast.makeText(EditQuestionActivity.this, "Question not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditQuestionActivity.this, "Error loading question", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateQuestion() {
        String updatedQuestionText = questionTextEdit.getText().toString();

        if (imageUri != null) {
            uploadImageAndUpdateQuestion(imageUri);
        } else {
            updateQuestionInFirestore(null);
        }
    }

    private void uploadImageAndUpdateQuestion(Uri imageUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading image...");
        progressDialog.show();

        StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
        ref.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        String uploadedImageUrl = taskSnapshot.getMetadata().getReference().getPath();
                        updateQuestionInFirestore(uploadedImageUrl);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(EditQuestionActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateQuestionInFirestore(String imageUrl) {
        Question updatedQuestion = new Question(
                questionId,
                questionTextEdit.getText().toString(),
                categorySpinner.getSelectedItem().toString(),
                getUpdatedAnswers(),
                correctAnswerIndex,
                imageUrl,
                "" // magyarázó szöveg
        );

        mFirestore.collection("Questions").document(questionId)
                .set(updatedQuestion)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(EditQuestionActivity.this, "Question updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditQuestionActivity.this, "Error updating question", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private ArrayList<String> getUpdatedAnswers() {
        ArrayList<String> answers = new ArrayList<>();
        for (int i = 0; i < answerEditTexts.length; i++) {
            answers.add(answerEditTexts[i].getText().toString());
        }
        return answers;
    }

}
