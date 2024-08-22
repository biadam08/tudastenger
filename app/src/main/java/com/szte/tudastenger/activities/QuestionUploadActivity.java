package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.SearchView;
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
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.UserAdapter;
import com.szte.tudastenger.databinding.ActivityQuestionUploadBinding;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class QuestionUploadActivity extends DrawerBaseActivity{
    private static final String LOG_TAG = QuestionUploadActivity.class.getName();

    private ActivityQuestionUploadBinding activityQuestionUploadBinding;
    private FirebaseFirestore mFirestore;

    private LinearLayout container;
    private LinearLayout answerContainer;
    private Spinner spinner;

    private RadioGroup radioGroup;
    private int answerCounter = 2;

    private ImageView questionImagePreview;
    private Uri imageUri;

    private FirebaseStorage mStorage;

    private int correctAnswerIndex = -1;
    private static final int PICK_IMAGE_REQUEST = 1;

    private StorageReference storageReference;
    private EditText editExplanationTextMultiLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityQuestionUploadBinding = ActivityQuestionUploadBinding.inflate(getLayoutInflater());
        setContentView(activityQuestionUploadBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        storageReference = mStorage.getReference();
        spinner = findViewById(R.id.questionCategory);

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

        EditText editExplanationTextMultiLine = findViewById(R.id.editExplanationTextMultiLine);

        questionImagePreview = findViewById(R.id.question_image_preview);
        Button uploadImageButton = findViewById(R.id.upload_image_button);

        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        Button addExplanationButton = findViewById(R.id.addExplanationButton);
        addExplanationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddExplanationPopup();
            }
        });

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

    public void uploadImage(View view)
    {
        String questionText = ((EditText) findViewById(R.id.questionName)).getText().toString();
        String category = ((Spinner) findViewById(R.id.questionCategory)).getSelectedItem().toString();
        ArrayList<Boolean> answerInputHasValue = new ArrayList<>();
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

            //ennek segítségével fogjuk ellenőrizni, hogy van-e olyan mező, ami üres, de a következő mezőbe már van válaszlehetőség írva
            if(!answer.getText().toString().isEmpty()) {
                answerInputHasValue.add(true);
            } else {
                answerInputHasValue.add(false);
            }

            if(!answer.getText().toString().isEmpty()) {
                answers.add(answer.getText().toString());
            }
        }

        if (correctAnswerIndex == -1) {
            Toast.makeText(this, "Kérjük, jelöljön meg egy helyes választ!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isValid = true;
        for(int i = 0; i < answerInputHasValue.size() - 1; i++){
            if(!answerInputHasValue.get(i) && answerInputHasValue.get(i+1)){
                isValid = false;
            }
        }

        if (imageUri != null) {

            ProgressDialog progressDialog  = new ProgressDialog(this);
            progressDialog.setTitle("Feltöltés...");
            progressDialog.show();

            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());

            boolean finalIsValid = isValid;
            ref.putFile(imageUri)
                    .addOnSuccessListener(
                            new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {

                                    progressDialog.dismiss();
                                    String fileName = taskSnapshot.getMetadata().getReference().getName();
                                    if(finalIsValid) {
                                        saveQuestion(questionText, category, answers, correctAnswerIndex, fileName);
                                    } else {
                                        showErrorDialog();
                                    }
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
            if(isValid) {
                saveQuestion(questionText, category, answers, correctAnswerIndex, null);
            } else {
                showErrorDialog();
            }
        }
    }

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

                        clearInputFields();
                        showSuccessDialog();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(QuestionUploadActivity.this, "Hiba történt a mentés során.", Toast.LENGTH_SHORT).show();
                    }
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

    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sikeres feltöltés")
                .setMessage("A kérdés sikeresen feltöltve!")
                .setPositiveButton("Rendben", null)
                .show();
    }

    private void showErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sikertelen feltöltés!")
                .setMessage("A kérdést nem sikerült feltölteni, mert az egyik válaszlehetőség üres, miközben utána lévő mezőben van válaszlehetőség!")
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

        Button mPopUpCloseButton = popupView.findViewById(R.id.popUpCloseButton);
        Button mGenerateTextButton = popupView.findViewById(R.id.generateTextButton);
        Button mSaveExplanationButton = popupView.findViewById(R.id.saveExplanationText);

        mPopUpCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
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

}