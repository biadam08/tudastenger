package com.szte.tudastenger.activities;

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
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityQuizGameBinding;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.viewmodels.QuizGameViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuizGameActivity extends DrawerBaseActivity {
    private ActivityQuizGameBinding binding;
    private QuizGameViewModel viewModel;
    private List<View> answerViews = new ArrayList<>();
    private String intentCategoryId;
    private boolean isMixed;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        viewModel = new ViewModelProvider(this).get(QuizGameViewModel.class);

        setupIntentData();
        setupObservers();
        setupClickListeners();

        viewModel.loadCurrentUser();

    }

    private void setupIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            intentCategoryId = intent.getStringExtra("intentCategoryId");
            isMixed = Boolean.parseBoolean(intent.getStringExtra("mixed"));
        }
    }

    private void setupObservers() {
        viewModel.getCurrentUser().observe(this, user -> {
            binding.userGold.setText(String.valueOf(user.getGold()));

            if(viewModel.getCurrentQuestion().getValue() == null) {
                viewModel.queryRandomQuestion(intentCategoryId, isMixed);
            }
        });

        viewModel.getCurrentQuestion().observe(this, this::displayQuestion);

        viewModel.getImageUri().observe(this, uri -> {
            if (uri != null) {
                Glide.with(this)
                        .load(uri)
                        .into(binding.questionImage);
                binding.questionImage.setVisibility(View.VISIBLE);
            } else {
                binding.questionImage.setVisibility(View.GONE);
            }
        });

        viewModel.getIsQuestionSaved().observe(this, isSaved ->
                binding.saveQuestionButton.setText(isSaved ? "Eltávolítás" : "Mentés"));

        viewModel.getErrorMessage().observe(this, error ->
                showOkAlertDialog("Hiba", error));

        viewModel.getIsAnswerSelected().observe(this, isSelected -> {
            if (isSelected) {
                binding.buttonsLinearLayout.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsHelpUsed().observe(this, isHelpUsed -> {
            if (isHelpUsed) {
                removeWrongAnswer();
            }
        });
    }
    private void setupClickListeners() {
        binding.helpButton.setOnClickListener(v -> {
            if (viewModel.getIsAnswerSelected().getValue()) {
                showOkAlertDialog("Már válaszoltál",
                        "Ezt a kérdést már megválaszoltad, így nem vehetsz igénybe segítséget!");
                return;
            }
            popUpHelp();
        });

        binding.saveQuestionButton.setOnClickListener(v -> viewModel.saveQuestion());

        binding.nextQuestionButton.setOnClickListener(v ->
                viewModel.queryRandomQuestion(intentCategoryId, isMixed));

        binding.showExplanationButton.setOnClickListener(v -> popUpExplanation());

        binding.goToHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(QuizGameActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void displayQuestion(Question question) {
        // előző kérdés adatainak eltűntetése
        binding.answersLayout.removeAllViews();
        answerViews.clear();
        binding.buttonsLinearLayout.setVisibility(View.GONE);
        binding.numCorrectAnswersTextView.setText("");
        binding.numCorrectAnswersTextView.setVisibility(View.GONE);

        // adatok beállítása
        binding.questionTextView.setText(question.getQuestionText());
        if (question.getImage() != null && !question.getImage().isEmpty()) {
            binding.questionImage.setVisibility(View.VISIBLE);
        } else {
            binding.questionImage.setVisibility(View.GONE);
        }

        for (int i = 0; i < question.getAnswers().size(); i++) {
            String answer = question.getAnswers().get(i);
            View answerCardView = getLayoutInflater().inflate(R.layout.answer_card, null);
            TextView answerTextView = answerCardView.findViewById(R.id.answerTextView);
            answerTextView.setText(answer);

            CardView cardView = answerCardView.findViewById(R.id.cardView);
            cardView.setCardBackgroundColor(getResources().getColor(R.color.lightbrowne));

            final int answerIndex = i;
            answerCardView.setOnClickListener(v -> {
                if (!viewModel.getIsAnswerSelected().getValue()) {
                    boolean userAnsweredCorrectly = handleAnswerSelection(answerIndex, question);
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, question.getId());
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, question.getQuestionText());
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "quiz_question");
                    bundle.putString("answer", question.getAnswers().get(answerIndex));
                    bundle.putBoolean("result", userAnsweredCorrectly);
                    mFirebaseAnalytics.logEvent("quiz_answer_selected", bundle);
                }
            });

            binding.answersLayout.addView(answerCardView);
            answerViews.add(answerCardView);
        }
    }

    private boolean handleAnswerSelection(int selectedIndex, Question question) {
        viewModel.submitAnswer(selectedIndex);
        int correctAnswerIndex = question.getCorrectAnswerIndex();
        boolean userAnsweredCorrectly = selectedIndex == correctAnswerIndex;

        // válasz cardview-ok háttérszíneinek módosítása a válasz alapján
        for (int i = 0; i < answerViews.size(); i++) {
            View answerView = answerViews.get(i);
            CardView cardView = answerView.findViewById(R.id.cardView);
            boolean isCorrect = i == correctAnswerIndex;

            if (isCorrect) {
                cardView.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
            } else if (!isCorrect && selectedIndex == i){
                cardView.setCardBackgroundColor(getResources().getColor(R.color.incorrect_red));
            }
        }

        // a kérdéshez tartozó statisztika megjelenítése
        int totalAnswers = question.getNumCorrectAnswers() + question.getNumWrongAnswers();
        if (totalAnswers > 0) {
            int correctPercentage = Math.round(((float) question.getNumCorrectAnswers() / totalAnswers) * 100);
            String statsText = "A játékosok " + correctPercentage + "%-a válaszolt helyesen erre a kérdésre";
            binding.numCorrectAnswersTextView.setText(statsText);
            binding.numCorrectAnswersTextView.setVisibility(View.VISIBLE);
        }
        return userAnsweredCorrectly;
    }

    public void popUpExplanation() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_correct_answer_explanation, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(false);

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

    public void popUpHelp() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_buy_help, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(false);

        Button removeWrongAnswerButton = popupView.findViewById(R.id.removeWrongAnswerButton);
        Button closeHelpPopupButton = popupView.findViewById(R.id.closeHelpPopupButton);

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);

        removeWrongAnswerButton.setOnClickListener(v -> {
            showHelpConfirmationDialog(popupWindow);
        });

        closeHelpPopupButton.setOnClickListener(v -> popupWindow.dismiss());
    }

    private void showHelpConfirmationDialog(PopupWindow popupWindow) {
        new AlertDialog.Builder(this)
                .setTitle("Segítség vásárlása")
                .setMessage("Biztosan megvásárolod a segítséget 10 aranyért?")
                .setPositiveButton("Igen", (dialog, which) -> {
                    boolean canUseHelp = viewModel.useHelp();
                    if (canUseHelp) {
                        removeWrongAnswer();
                    }
                    popupWindow.dismiss();
                })
                .setNegativeButton("Nem", (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }

    private void removeWrongAnswer() {
        Question question = viewModel.getCurrentQuestion().getValue();
        if (question == null){
            return;
        }

        List<Integer> incorrectAnswers = new ArrayList<>();
        for (int i = 0; i < question.getAnswers().size(); i++) {
            if (i != question.getCorrectAnswerIndex()) {
                incorrectAnswers.add(i);
            }
        }

        if (!incorrectAnswers.isEmpty()) {
            int randomIncorrectIndex = incorrectAnswers.get(new Random().nextInt(incorrectAnswers.size()));
            View incorrectAnswerView = answerViews.get(randomIncorrectIndex);
            incorrectAnswerView.setVisibility(View.INVISIBLE);
            incorrectAnswerView.setClickable(false);
        }
    }

    private void showOkAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(QuizGameActivity.this);
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

    private void dimBehind(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(container, p);
    }
}