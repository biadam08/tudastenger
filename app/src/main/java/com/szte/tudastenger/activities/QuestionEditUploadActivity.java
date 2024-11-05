package com.szte.tudastenger.activities;

import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityQuestionEditUploadBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.viewmodels.QuestionEditUploadViewModel;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuestionEditUploadActivity extends DrawerBaseActivity {
    private ActivityQuestionEditUploadBinding binding;
    private QuestionEditUploadViewModel viewModel;
    private String questionId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionEditUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(QuestionEditUploadViewModel.class);

        questionId = getIntent().getStringExtra("questionId");
        setupViews();
        setupObservers();
        setupClickListeners();

        viewModel.loadCategories();
        if (questionId != null) {
            binding.editBarLinearLayout.setVisibility(View.VISIBLE);
            binding.addQuestionTextView.setText("Kérdés szerkesztése");
            binding.addQuestionButton.setText("Kérdés módosítása");
            viewModel.checkAdmin();
            viewModel.loadQuestionData(questionId);
        }
    }

    private void setupViews() {
        binding.editBarLinearLayout.setVisibility(View.GONE);
        binding.manageImageLinearLayout.setVisibility(View.GONE);
        binding.questionImagePreview.setVisibility(View.GONE);
    }

    private void setupObservers() {
        viewModel.getIsAdmin().observe(this, isAdmin -> {
            if (!isAdmin) {
                finish();
            }
        });

        viewModel.getCategories().observe(this, categories -> {
            ArrayAdapter<Category> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.questionCategory.setAdapter(adapter);
        });

        viewModel.getCurrentQuestion().observe(this, question -> {
            updateUI(question);
        });

        viewModel.getErrorMessage().observe(this, message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show());

        viewModel.getSuccessMessage().observe(this, message ->
                showSuccessDialog("Sikeres művelet", message));

        viewModel.getImageUrl().observe(this, url -> {
            if (url != null) {
                binding.uploadImageButton.setVisibility(View.GONE);
                binding.manageImageLinearLayout.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(url)
                        .into(binding.questionImagePreview);
                binding.questionImagePreview.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsImageUploading().observe(this, isUploading -> {
            if (isUploading) {
                showProgressDialog("Feltöltés...");
            } else {
                hideProgressDialog();
            }
        });

        viewModel.getUploadProgress().observe(this, progress ->
                updateProgressDialog("Feltöltve: " + progress + "%"));
    }

    private void setupClickListeners() {
        binding.uploadImageButton.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, 1001);
        });

        binding.addExplanationButton.setOnClickListener(v -> showAddExplanationPopup());
        binding.backButton.setOnClickListener(v -> navigateToQuestionList());
        binding.deleteButton.setOnClickListener(v -> {
            if (questionId != null) {
                showDeleteConfirmationDialog();
            } else {
                Toast.makeText(this, "Nincs törlendő kérdés", Toast.LENGTH_SHORT).show();
            }
        });

        binding.deleteImageButton.setOnClickListener(v -> {
            viewModel.clearImageUri();
            binding.questionImagePreview.setVisibility(View.GONE);
            binding.uploadImageButton.setVisibility(View.VISIBLE);
            binding.manageImageLinearLayout.setVisibility(View.GONE);
        });

        binding.modifyImageButton.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, 1001);
        });
    }

    private void updateUI(Question question) {
        if (question == null){
            return;
        }

        binding.questionName.setText(question.getQuestionText());

        if (question.getCategory() != null) {
            for (int i = 0; i < binding.questionCategory.getCount(); i++) {
                Category category = (Category) binding.questionCategory.getItemAtPosition(i);
                if (category.getId().equals(question.getCategory())) {
                    binding.questionCategory.setSelection(i);
                    break;
                }
            }
        }

        ArrayList<String> answers = question.getAnswers();
        for (int i = 0; i < answers.size(); i++) {
            EditText answerEditText = (EditText) binding.answerContainer.getChildAt(i);
            RadioButton radioButton = (RadioButton) binding.radioGroup.getChildAt(i);

            answerEditText.setText(answers.get(i));
            if (i == question.getCorrectAnswerIndex()) {
                radioButton.setChecked(true);
            }
        }
    }

    public void uploadQuestion(View view) {
        if (validateQuestionInput()) {
            Question question = createQuestion();
            viewModel.uploadQuestion(question, viewModel.getImageUri().getValue());
        }
    }

    private Question createQuestion() {
        String questionText = binding.questionName.getText().toString();
        Category selectedCategory = (Category) binding.questionCategory.getSelectedItem();
        String categoryId = selectedCategory.getId();

        ArrayList<String> answers = new ArrayList<>();
        int correctAnswerIndex = -1;

        for (int i = 0; i < binding.radioGroup.getChildCount(); i++) {
            EditText answer = (EditText) binding.answerContainer.getChildAt(i);
            RadioButton radioButton = (RadioButton) binding.radioGroup.getChildAt(i);

            if (radioButton.isChecked()) {
                correctAnswerIndex = i;
            }

            if (!answer.getText().toString().isEmpty()) {
                answers.add(answer.getText().toString());
            }
        }

        return new Question(
                questionId,
                questionText,
                categoryId,
                answers,
                correctAnswerIndex,
                viewModel.getExistingImageName().getValue(),
                viewModel.getExplanationText().getValue()
        );
    }

    private boolean validateQuestionInput() {
        String questionText = binding.questionName.getText().toString();
        ArrayList<Boolean> answerInputHasValue = new ArrayList<>();
        int correctAnswerIndex = -1;

        for (int i = 0; i < binding.radioGroup.getChildCount(); i++) {
            EditText answer = (EditText) binding.answerContainer.getChildAt(i);
            RadioButton radioButton = (RadioButton) binding.radioGroup.getChildAt(i);

            if (answer.getText().toString().isEmpty() && radioButton.isChecked()) {
                answer.setError("Nem lehet üres válasz a helyes");
                return false;
            }

            if (radioButton.isChecked()) {
                correctAnswerIndex = i;
            }

            answerInputHasValue.add(!answer.getText().toString().isEmpty());
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                startCrop(selectedImageUri);
            }
        } else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            viewModel.setImageUri(resultUri);
            binding.questionImagePreview.setImageURI(resultUri);
            binding.questionImagePreview.setVisibility(View.VISIBLE);
            binding.uploadImageButton.setVisibility(View.GONE);
            binding.manageImageLinearLayout.setVisibility(View.VISIBLE);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Hiba a kép vágásakor: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startCrop(Uri imageUri) {
        String destinationFileName = UUID.randomUUID().toString() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));

        UCrop.of(imageUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800)
                .start(this);
    }

    private void showAddExplanationPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_add_explanation, null);

        PopupWindow popupWindow = createPopupWindow(popupView);
        EditText explanationEditText = popupView.findViewById(R.id.editExplanationTextMultiLine);

        String currentExplanation = viewModel.getExplanationText().getValue();
        if (currentExplanation != null && !currentExplanation.isEmpty()) {
            explanationEditText.setText(currentExplanation);
        }

        Button closeButton = popupView.findViewById(R.id.popUpCloseButton);
        Button generateButton = popupView.findViewById(R.id.generateTextButton);
        Button saveButton = popupView.findViewById(R.id.saveExplanationText);

        viewModel.getExplanationText().observe(this, explanationText -> {
            if (explanationText != null && !explanationText.isEmpty()) {
                explanationEditText.setText(explanationText);
            }
        });

        generateButton.setOnClickListener(v -> {
            if (validateQuestionInput()) {
                String questionText = binding.questionName.getText().toString();
                String correctAnswer = "";
                for (int i = 0; i < binding.radioGroup.getChildCount(); i++) {
                    RadioButton radioButton = (RadioButton) binding.radioGroup.getChildAt(i);
                    if (radioButton.isChecked()) {
                        EditText answer = (EditText) binding.answerContainer.getChildAt(i);
                        correctAnswer = answer.getText().toString();
                        break;
                    }
                }
                viewModel.generateExplanation(questionText, correctAnswer);
            }
        });

        closeButton.setOnClickListener(v -> popupWindow.dismiss());

        saveButton.setOnClickListener(v -> {
            viewModel.setExplanationText(explanationEditText.getText().toString());
            Toast.makeText(this, "Sikeresen elmentve!", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    private PopupWindow createPopupWindow(View popupView) {
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);
        return popupWindow;
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kérdés törlése");
        builder.setMessage("Biztosan törölni szeretnéd ezt a kérdést?");

        builder.setPositiveButton("Igen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                viewModel.deleteQuestion(questionId);
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

    private void showSuccessDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", (dialog, which) -> {
                    if(questionId != null) {
                        navigateToQuestionList();
                    } else{
                        Intent intent = new Intent(QuestionEditUploadActivity.this, QuestionEditUploadActivity.class);
                        startActivity(intent);
                        finish();
                    }
                 })
                .show();
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hiba")
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }

    private void navigateToQuestionList() {
        Intent intent = new Intent(this, QuestionListActivity.class);
        startActivity(intent);
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

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setTitle(message);
        progressDialog.show();
    }

    private void updateProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
