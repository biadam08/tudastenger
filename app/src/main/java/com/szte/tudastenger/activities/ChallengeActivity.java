package com.szte.tudastenger.activities;

import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityChallengeBinding;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.viewmodels.ChallengeViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChallengeActivity extends DrawerBaseActivity {
    private ActivityChallengeBinding binding;
    private ChallengeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChallengeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ChallengeViewModel.class);
        viewModel.init();

        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        viewModel.loadTodaysChallenge();

        binding.nextQuestionButton.setOnClickListener(v -> {
            if(viewModel.getIsLastQuestion().getValue()) {
                viewModel.finishChallenge();
                showResultDialog();
            } else {
                viewModel.resetForNextQuestion();
                viewModel.startChallenge();
            }
        });
        
        binding.showExplanationButton.setOnClickListener(v -> popUpExplanation());
    }

    private void observeViewModel() {
        viewModel.getCurrentQuestion().observe(this, this::displayQuestion);

        viewModel.getActualQuestionNumber().observe(this, number -> {
            if (number != null && viewModel.getQuestionsList().getValue() != null) {
                binding.questionNumberTextView.setText(number + "/" + viewModel.getQuestionsList().getValue().size());
            }
        });

        viewModel.getImageUri().observe(this, uri -> {
            if (uri != null) {
                binding.questionImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).into(binding.questionImage);
            } else {
                binding.questionImage.setVisibility(View.GONE);
            }
        });

        viewModel.getShowNavigationButtons().observe(this, show ->
                binding.navigationButtonsLayout.setVisibility(show ? View.VISIBLE : View.GONE));

        viewModel.getIsLastQuestion().observe(this, isLast -> {
            if (isLast) {
                binding.nextQuestionButton.setText("Befejezés");
            } else {
                binding.nextQuestionButton.setText("Következő");
            }
        });

        viewModel.getErrorMessage().observe(this, error -> showOkAlertDialog("Hiba", error));
    }

    private void displayQuestion(Question question) {
        if (question == null){
            return;
        }

        binding.questionTextView.setText(question.getQuestionText());
        binding.answersLayout.removeAllViews();

        for (int i = 0; i < question.getAnswers().size(); i++) {
            View answerCardView = getLayoutInflater().inflate(R.layout.answer_card, null);
            TextView answerTextView = answerCardView.findViewById(R.id.answerTextView);
            CardView cardView = answerCardView.findViewById(R.id.cardView);

            answerTextView.setText(question.getAnswers().get(i));
            cardView.setCardBackgroundColor(getResources().getColor(R.color.lightbrowne));

            final int clickedIndex = i;
            answerCardView.setOnClickListener(v -> {
                if (!viewModel.getIsSelectedAnswer().getValue()) {
                    viewModel.handleAnswerClick(clickedIndex, question.getCorrectAnswerIndex());
                    updateAnswerCardColors(clickedIndex, question.getCorrectAnswerIndex());
                }
            });

            binding.answersLayout.addView(answerCardView);
        }
    }

    private void updateAnswerCardColors(int clickedIndex, int correctIndex) {
        CardView clickedCard = binding.answersLayout.getChildAt(clickedIndex).findViewById(R.id.cardView);
        CardView correctCard = binding.answersLayout.getChildAt(correctIndex).findViewById(R.id.cardView);

        if (clickedIndex == correctIndex) {
            clickedCard.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
        } else {
            clickedCard.setCardBackgroundColor(getResources().getColor(R.color.incorrect_red));
            correctCard.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
        }
    }

    private void showResultDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.popup_quiz_result, null);
        PopupWindow popupWindow = new PopupWindow(dialogView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                false);

        TextView resultTextView = dialogView.findViewById(R.id.resultTextView);
        Button finishButton = dialogView.findViewById(R.id.finishDuelButton);

        viewModel.getResult().observe(this, result -> {
            if (result != null) {
                resultTextView.setText(result);
            }
        });

        finishButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ChallengeResultListing.class));
            finish();
        });

        popupWindow.showAtLocation(binding.getRoot(), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    private void popUpExplanation() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_correct_answer_explanation, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);

        TextView explanationTextView = popupView.findViewById(R.id.explanationTextView);
        Button closeButton = popupView.findViewById(R.id.closeButton);

        viewModel.getExplanationText().observe(this, explanation -> {
            if (explanation != null) {
                explanationTextView.setText(explanation);
            }
        });

        closeButton.setOnClickListener(v -> popupWindow.dismiss());

        popupWindow.showAtLocation(binding.getRoot(), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    private void dimBehind(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(container, p);
    }

    private void showOkAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChallengeActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}

