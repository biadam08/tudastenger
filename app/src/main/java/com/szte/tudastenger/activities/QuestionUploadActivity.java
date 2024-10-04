package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;

import android.Manifest;
import android.app.Activity;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.szte.tudastenger.databinding.ActivityQuestionUploadBinding;
import com.szte.tudastenger.models.Question;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class QuestionUploadActivity extends DrawerBaseActivity {
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

    private static final int PICK_IMAGE_REQUEST = 1;

    private StorageReference storageReference;
    private EditText explanationEditText;
    private TextView addQuestionTextView;
    private Button addQuestionButton;
    private String explanationText;
    private String questionText;
    private String category;
    private ArrayList<String> answers;
    private int correctAnswerIndex = -1;
    private String questionId; // Kérdés ID szerkesztés esetén

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityQuestionUploadBinding = ActivityQuestionUploadBinding.inflate(getLayoutInflater());
        setContentView(activityQuestionUploadBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        storageReference = mStorage.getReference();
        spinner = findViewById(R.id.questionCategory);
        container = findViewById(R.id.container);
        answerContainer = findViewById(R.id.answerContainer);
        radioGroup = findViewById(R.id.radioGroup);
        addQuestionTextView = findViewById(R.id.addQuestionTextView);
        addQuestionButton = findViewById(R.id.addQuestionButton);

        questionImagePreview = findViewById(R.id.questionImagePreview);
        Button uploadImageButton = findViewById(R.id.uploadImageButton);

        questionId = getIntent().getStringExtra("questionId");

        mFirestore.collection("Categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> categoryList = new ArrayList<>();
            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                categoryList.add(documentSnapshot.getString("name"));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(QuestionUploadActivity.this, android.R.layout.simple_spinner_item, categoryList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            if (questionId != null) {
                loadQuestionData(questionId);
                addQuestionTextView.setText("Kérdés szerkesztése");
                addQuestionButton.setText("Kérdés módosítása");
            }
        });

        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
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

    private void loadQuestionData(String questionId) {
        mFirestore.collection("Questions").document(questionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Question question = documentSnapshot.toObject(Question.class);

                        // kérdés adatainak betöltése
                        questionText = question.getQuestionText();
                        answers = question.getAnswers();
                        correctAnswerIndex = question.getCorrectAnswerIndex();
                        category = question.getCategory();
                        explanationText = question.getExplanationText();

                        // UI elemek frissítése
                        ((EditText) findViewById(R.id.questionName)).setText(questionText);
                        spinner.setSelection(getSpinnerIndex(spinner, category));

                        for (int i = 0; i < answers.size(); i++) {
                            EditText answerEditText = (EditText) answerContainer.getChildAt(i);
                            RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
                            answerEditText.setText(answers.get(i));

                            if (i == correctAnswerIndex) {
                                radioButton.setChecked(true);
                            }
                        }

                        String imageName = question.getImage();
                        if (imageName != null && !imageName.isEmpty()) {
                            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + imageName);
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                Glide.with(this)
                                        .load(imageUrl)
                                        .into(questionImagePreview);
                                questionImagePreview.setVisibility(View.VISIBLE);
                            }).addOnFailureListener(e -> {
                                questionImagePreview.setVisibility(View.GONE);
                                Toast.makeText(QuestionUploadActivity.this, "Hiba a kép betöltésekor", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            questionImagePreview.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private int getSpinnerIndex(Spinner spinner, String category) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(category)) {
                return i;
            }
        }
        return 0;
    }

    public void uploadQuestion(View view) {
        if (validateQuestionInput()) {
            if (imageUri != null) {
                uploadImage(imageUri);  // Kép feltöltése és kérdés mentése/frissítése
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
        Question question = new Question(questionId, questionText, category, answers, correctAnswerIndex, fileName, explanationText);

        mFirestore.collection("Questions").document(questionId)
                .set(question)
                .addOnSuccessListener(aVoid -> {
                    clearInputFields();
                    showSuccessDialog("A kérdés sikeresen módosítva lett!");
                })
                .addOnFailureListener(e -> Toast.makeText(QuestionUploadActivity.this, "Hiba történt a frissítés során.", Toast.LENGTH_SHORT).show());
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
                            Toast.makeText(QuestionUploadActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        Question question = new Question(null, questionText, category, answers, correctAnswerIndex, fileName, explanationText);

        mFirestore.collection("Questions").add(question)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String documentId = documentReference.getId();
                        question.setId(documentId);

                        mFirestore.collection("Questions").document(documentId)
                                .update("id", documentId);

                        clearInputFields();
                        showSuccessDialog("A kérdés sikeresen feltöltve!");
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

    private void showSuccessDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sikeres feltöltés") /// ezt át kell írni
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
                    generateExplanationWithOkHttp();
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
                Toast.makeText(QuestionUploadActivity.this, "Sikeresen elmentve!", Toast.LENGTH_SHORT).show();
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

    private void generateExplanationWithOkHttp() {
        String apiKey = "Bearer ";

        String prompt = "Adj egy rövid 3-5 mondatos ismeretterjesztő választ az alábbi kérdésre és a hozzátartozó helyes válaszra. \nKérdés: " + questionText + "\nHelyes válasz: " + answers.get(correctAnswerIndex);

        OkHttpClient client = new OkHttpClient();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-4o-mini");

            JSONArray messages = new JSONArray();

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            messages.put(userMessage);

            jsonObject.put("messages", messages);
            jsonObject.put("max_tokens", 350);
            jsonObject.put("temperature", 0.7);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                jsonObject.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(QuestionUploadActivity.this, "Failed to generate explanation",  Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        String responseBody = response.body().string();
                        Log.d("API_RESPONSE", responseBody); // Itt írasd ki a teljes választ a Logcat-be
                        String explanation = extractExplanationFromResponse(responseBody);

                        runOnUiThread(() -> {
                            explanationEditText.setText(explanation);
                            explanationText = explanation;
                        });
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    runOnUiThread(() -> {
                        Log.d("ERRORAPI", errorBody);
                        Toast.makeText(QuestionUploadActivity.this, "Error generating explanation: " + errorBody, Toast.LENGTH_LONG).show();
                    });
                }
            }

        });
    }

    private String extractExplanationFromResponse(String responseBody) {
        try {
            JSONObject jsonObject = new JSONObject(responseBody);
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                return message.getString("content").trim();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "Nincs elérhető magyarázat.";
    }


}