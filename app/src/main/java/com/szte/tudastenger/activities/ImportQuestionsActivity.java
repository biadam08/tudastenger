package com.szte.tudastenger.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.opencsv.CSVReader;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityImportQuestionsBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ImportQuestionsActivity extends DrawerBaseActivity {
    ActivityImportQuestionsBinding activityImportQuestionsBinding;
    private Button importButton;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityImportQuestionsBinding = ActivityImportQuestionsBinding.inflate(getLayoutInflater());
        setContentView(activityImportQuestionsBinding.getRoot());

        importButton = findViewById(R.id.importButton);
        mFirestore = FirebaseFirestore.getInstance();


        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFilePicker();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri uri = data.getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                            CSVReader csvReader = new CSVReader(reader);
                            String[] nextLine;

                            while ((nextLine = csvReader.readNext()) != null) {
                                String questionText = nextLine[0].trim();
                                String category = nextLine[1];
                                ArrayList<String> answers = new ArrayList<>();
                                answers.add(nextLine[2]);
                                answers.add(nextLine[3]);
                                answers.add(nextLine[4]);
                                answers.add(nextLine[5]);
                                int correctAnswerIndex = Integer.parseInt(nextLine[6]) - 1; //a csv fájlban 1-től indexeljük a válaszokat

                                Question question = new Question(null, questionText, category, answers, correctAnswerIndex, null, null);
                                if(validateQuestion(question)){
                                    setCategoryId(question);
                                }
                            }
                            csvReader.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void setCategoryId(Question question) {
        mFirestore.collection("Categories")
                .whereEqualTo("name", question.getCategory())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            question.setCategory(documentSnapshot.getId());
                        }
                        saveQuestion(question);
                    } else {
                        mFirestore.collection("Categories")
                                .whereEqualTo("name", "Besorolatlan")
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                    if (!queryDocumentSnapshots2.isEmpty()) {
                                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots2) {
                                            question.setCategory(documentSnapshot.getId());
                                        }
                                        saveQuestion(question);
                                    }
                                });
                    }
                });
    }

    private void saveQuestion(Question question) {
        mFirestore.collection("Questions").add(question)
                .addOnSuccessListener(documentReference -> {
                    String documentId = documentReference.getId();
                    question.setId(documentId);

                    mFirestore.collection("Questions").document(documentId)
                            .update("id", documentId);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ImportQuestionsActivity.this, "Hiba történt a mentés során.", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateQuestion(Question question) {
        if(question.getQuestionText().isEmpty()){
            return false;
        }
        if(question.getCategory().isEmpty()){
            return false;
        }
        if(question.getAnswers().size() < question.getCorrectAnswerIndex()){
            return false;
        }
        return true;
    }
}