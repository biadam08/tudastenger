package com.szte.tudastenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.szte.tudastenger.databinding.ActivityQuestionUploadBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class QuestionUploadActivity extends DrawerBaseActivity{
    private static final String LOG_TAG = QuestionUploadActivity.class.getName();

    private ActivityQuestionUploadBinding activityQuestionUploadBinding;
    private FirebaseFirestore mFirestore;

    private LinearLayout container;
    private LinearLayout answerContainer;
    private Button addAnswerButton;

    private Button removeLastAnswerButton;

    private RadioGroup radioGroup;
    private int answerCounter = 2;

    private ImageView questionImagePreview;
    private Uri imageUri;

    private FirebaseStorage mStorage;

    private int correctAnswerIndex = -1;
    private static final int PICK_IMAGE_REQUEST = 1;

    private StorageReference storageReference;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityQuestionUploadBinding = ActivityQuestionUploadBinding.inflate(getLayoutInflater());
        setContentView(activityQuestionUploadBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        storageReference = mStorage.getReference();
        Spinner spinner = findViewById(R.id.questionCategory);

        mFirestore.collection("Categories").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<String> questionList = new ArrayList<>();
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    questionList.add(documentSnapshot.getString("name"));
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(QuestionUploadActivity.this, android.R.layout.simple_spinner_item, questionList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }
        });

        container = findViewById(R.id.container);
        answerContainer = findViewById(R.id.answerContainer);
        radioGroup = findViewById(R.id.radioGroup);
       // addAnswerButton = findViewById(R.id.add_answer_button);
      //  removeLastAnswerButton = findViewById(R.id.remove_last_answer_button);

        /*addAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewAnswerEditText();
            }
        });*/

       /* removeLastAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeLastAnswerEditText();
            }
        });*/

        questionImagePreview = findViewById(R.id.question_image_preview);
        Button uploadImageButton = findViewById(R.id.upload_image_button);

        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

    }

    private void removeLastAnswerEditText() {
        if (answerCounter > 2) {
            container.removeViewAt(container.getChildCount() - 3);
            answerCounter--;
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Válassz képet"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            questionImagePreview.setImageURI(imageUri);
            questionImagePreview.setVisibility(View.VISIBLE);
        }
    }

    // UploadImage method
    public void uploadImage(View view)
    {
        String questionText = ((EditText) findViewById(R.id.questionName)).getText().toString();
        String category = ((Spinner) findViewById(R.id.questionCategory)).getSelectedItem().toString();
        ArrayList<String> answers = new ArrayList<>();

        for (int i = 0; i < radioGroup.getChildCount(); i++){
            EditText answer = (EditText) answerContainer.getChildAt(i);
            RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);

            if(answer.getText().toString().isEmpty() && radioButton.isChecked()){
                answer.setError("Nem lehet üres válasz a helyes");
                return;
            }

            if(radioButton.isChecked()){
                correctAnswerIndex = i;
            }

            if(!answer.getText().toString().isEmpty()) {
                answers.add(answer.getText().toString());
            }
        }

        if (correctAnswerIndex == -1) {
            Toast.makeText(this, "Kérjük, jelöljön meg egy helyes választ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {

            ProgressDialog progressDialog  = new ProgressDialog(this);
            progressDialog.setTitle("Feltöltés...");
            progressDialog.show();

            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());

            ref.putFile(imageUri)
                    .addOnSuccessListener(
                            new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {

                                    progressDialog.dismiss();
                                    Toast
                                            .makeText(QuestionUploadActivity.this,
                                                    "Sikeresen feltöltve!",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                    String fileName = taskSnapshot.getMetadata().getReference().getName();
                                    saveQuestion(questionText, category, answers, correctAnswerIndex, fileName);
                                }
                            })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            // Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast
                                    .makeText(QuestionUploadActivity.this,
                                            "Failed " + e.getMessage(),
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {

                                // Progress Listener for loading
                                // percentage on the dialog box
                                @Override
                                public void onProgress(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    double progress
                                            = (100.0
                                            * taskSnapshot.getBytesTransferred()
                                            / taskSnapshot.getTotalByteCount());
                                    progressDialog.setMessage(
                                            "Feltöltve: "
                                                    + (int)progress + "%");

                                }
                            });
        } else{
            saveQuestion(questionText, category, answers, correctAnswerIndex, null);
        }
}

    /*private void addNewAnswerEditText() {
        LinearLayout answerLayout = new LinearLayout(this);
        answerLayout.setOrientation(LinearLayout.HORIZONTAL);
        answerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText newAnswerEditText = new EditText(this);
        newAnswerEditText.setId(View.generateViewId());
        LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.8f
        );

        newAnswerEditText.setLayoutParams(editTextParams);
        answerLayout.setPadding(12, 12, 12, 12);
        newAnswerEditText.setBackgroundResource(R.drawable.custom_shape_edittext);
        newAnswerEditText.setHint((answerCounter + 1) + ". válaszlehetőség");
        newAnswerEditText.setInputType(InputType.TYPE_CLASS_TEXT);

        RadioButton newAnswerRadioButton = new RadioButton(this);
        newAnswerRadioButton.setId(View.generateViewId());
        LinearLayout.LayoutParams radioButtonParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.2f
        );

        newAnswerRadioButton.setLayoutParams(radioButtonParams);

        answerLayout.addView(newAnswerEditText);
        answerLayout.addView(newAnswerRadioButton);

        // Hozzáadás a RadioGroup-hoz
        radioGroup.addView(answerLayout);

        answerCounter++;
    }*/

    public void saveQuestion(String questionText, String category, ArrayList<String> answers, int correctAnswerIndex, String fileName) {
        Question question = new Question(null, questionText, category, answers, correctAnswerIndex, fileName);

        mFirestore.collection("Questions").add(question)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String documentId = documentReference.getId();
                        question.setId(documentId);

                        mFirestore.collection("Questions").document(documentId)
                                .update("id", documentId);

                        Toast.makeText(QuestionUploadActivity.this, "Kérdés sikeresen mentve!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(QuestionUploadActivity.this, "Hiba történt a mentés során.", Toast.LENGTH_SHORT).show();
                    }
                });

    }


}