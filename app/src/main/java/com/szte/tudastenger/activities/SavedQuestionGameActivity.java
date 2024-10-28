package com.szte.tudastenger.activities;

import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivitySavedQuestionGameBinding;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.viewmodels.SavedQuestionGameViewModel;

import java.util.ArrayList;

public class SavedQuestionGameActivity extends DrawerBaseActivity {
    private ActivitySavedQuestionGameBinding binding;
    private SavedQuestionGameViewModel viewModel;
    private String explanationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedQuestionGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SavedQuestionGameViewModel.class);

        String questionId = getIntent().getStringExtra("questionId");
        if (questionId == null) {
            finish();
            return;
        }

        setupButtons();
        observeViewModel();
        viewModel.init(questionId);
    }

    private void setupButtons() {
        binding.backButton.setOnClickListener(v ->
                startActivity(new Intent(this, SavedQuestionsActivity.class)));

        binding.showExplanationButton.setOnClickListener(v -> popUpExplanation());
        binding.saveQuestionButton.setOnClickListener(v -> viewModel.saveQuestion());
    }

    private void observeViewModel() {
        viewModel.getCurrentQuestion().observe(this, this::displayQuestion);

        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsQuestionSaved().observe(this, isSaved -> {
            String text;
            if(isSaved){
                text = "Eltávolítás";
            } else{
                text = "Mentés";
            }

            binding.saveQuestionButton.setText(text);
        });

        viewModel.getIsAnswerSelected().observe(this, isSelected -> {
            if (isSelected) {
                Integer clicked = viewModel.getClickedIndex().getValue();
                Integer correct = viewModel.getCorrectIndex().getValue();

                if (clicked != null && correct != null) {
                    View clickedView = binding.answersLayout.getChildAt(clicked);
                    CardView clickedCard = clickedView.findViewById(R.id.cardView);

                    if (clicked.equals(correct)) {
                        clickedCard.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
                    } else {
                        clickedCard.setCardBackgroundColor(getResources().getColor(R.color.incorrect_red));

                        View correctView = binding.answersLayout.getChildAt(correct);
                        CardView correctCard = correctView.findViewById(R.id.cardView);
                        correctCard.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
                    }

                    binding.navigationButtonsLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void displayQuestion(Question question) {
        binding.answersLayout.removeAllViews();

        if (question.getImage() == null) {
            binding.questionImage.setVisibility(View.GONE);
        } else {
            binding.questionImage.setVisibility(View.VISIBLE);
        }

        binding.questionTextView.setText(question.getQuestionText());

        if(question.getExplanationText() != null){
            explanationText = question.getExplanationText();
        } else{
            explanationText = "Sajnos nincs megjelenítendő magyarázat ehhez a kérdéshez";
        }

        if (question.getImage() != null && !question.getImage().isEmpty()) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Kép betöltése...");
            progressDialog.show();

            String imagePath = "images/" + question.getImage();
            FirebaseStorage.getInstance().getReference().child(imagePath)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Glide.with(this).load(uri).into(binding.questionImage);
                        progressDialog.dismiss();
                    })
                    .addOnFailureListener(e -> progressDialog.dismiss());
        }

        for (int i = 0; i < question.getAnswers().size(); i++) {
            String answer = question.getAnswers().get(i);
            View answerCardView = getLayoutInflater().inflate(R.layout.answer_card, null);
            TextView answerTextView = answerCardView.findViewById(R.id.answerTextView);
            CardView cardView = answerCardView.findViewById(R.id.cardView);

            answerTextView.setText(answer);
            cardView.setCardBackgroundColor(getResources().getColor(R.color.lightbrowne));

            final int correctAnswerIndex = question.getCorrectAnswerIndex();
            final int clickedIndex = i;

            answerCardView.setOnClickListener(v -> {
                viewModel.handleAnswerClick(clickedIndex, correctAnswerIndex);
            });

            binding.answersLayout.addView(answerCardView);
        }
    }

    private void popUpExplanation() {
        View popupView = getLayoutInflater().inflate(R.layout.popup_correct_answer_explanation, null);
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                false
        );

        TextView explanationTextView = popupView.findViewById(R.id.explanationTextView);
        Button closeButton = popupView.findViewById(R.id.closeButton);

        explanationTextView.setText(explanationText);
        closeButton.setOnClickListener(v -> popupWindow.dismiss());

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(false);

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    private void dimBehind(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) container.getLayoutParams();
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        params.dimAmount = 0.5f;
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .updateViewLayout(container, params);
    }
}

