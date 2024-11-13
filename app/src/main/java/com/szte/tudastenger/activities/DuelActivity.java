package com.szte.tudastenger.activities;

import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.szte.tudastenger.InputFilterMinMax;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.DuelCategoryAdapter;
import com.szte.tudastenger.databinding.ActivityDuelBinding;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.viewmodels.DuelViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DuelActivity extends DrawerBaseActivity {
    private ActivityDuelBinding binding;
    private DuelViewModel viewModel;
    private DuelCategoryAdapter mAdapter;
    private String challengerUserId;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDuelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        viewModel = new ViewModelProvider(this).get(DuelViewModel.class);

        challengerUserId = getIntent().getStringExtra("challengerUserId");
        String challengedUserId = getIntent().getStringExtra("challengedUserId");
        String duelId = getIntent().getStringExtra("duelId");

        viewModel.init(challengerUserId, challengedUserId, duelId);

        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        binding.configureDuelButton.setOnClickListener(v -> showDuelConfigDialog());

        binding.backToFriendsActivityButton.setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));

        binding.nextQuestionButton.setOnClickListener(v -> {
            if(viewModel.getIsLastQuestion().getValue()) {
                viewModel.finishDuel();
                showResultDialog();
            } else {
                viewModel.resetForNextQuestion();
                viewModel.startDuel();
            }
        });
        binding.showExplanationButton.setOnClickListener(v -> popUpExplanation());
    }

    private void observeViewModel() {

        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null && user.getId().equals(challengerUserId)) {
                showDuelConfigDialog();
            }
        });

        viewModel.getCurrentQuestion().observe(this, this::displayQuestion);

        viewModel.getActualQuestionNumber().observe(this, number -> {
            if (number != null && viewModel.getQuestionsList().getValue() != null) {
                binding.questionNumberTextView.setText(number + "/" +
                        viewModel.getQuestionsList().getValue().size());
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

        viewModel.getShowButtonsLayout().observe(this, show ->
                binding.buttonsLinearLayout.setVisibility(show ? View.VISIBLE : View.GONE));

        viewModel.getIsLastQuestion().observe(this, isLast -> {
            if (isLast) {
                binding.nextQuestionButton.setText("Befejezés");
            } else {
                binding.nextQuestionButton.setText("Következő");
            }
        });

        viewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                showDuelConfigDialog();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                showErrorDialog("Hiba", error);
            }
        });
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

    private void showDuelConfigDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.popup_start_a_duel, null);
        PopupWindow popupWindow = new PopupWindow(dialogView,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);

        RecyclerView recyclerView = dialogView.findViewById(R.id.catRecyclerView);
        EditText questionNumberInput = dialogView.findViewById(R.id.questionNumberEditText);
        Button startDuelButton = dialogView.findViewById(R.id.startDuel);

        setupQuestionNumberInput(questionNumberInput);
        setupCategoryRecyclerView(recyclerView);

        startDuelButton.setOnClickListener(v -> {
            String input = questionNumberInput.getText().toString();
            if (!input.isEmpty() && viewModel.getSelectedCategory().getValue() != null) {
                viewModel.initializeQuestions(Integer.parseInt(input));
                popupWindow.dismiss();

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.METHOD, "start_duel_button");
                bundle.putString("category_name", viewModel.getCategory());
                bundle.putInt("number_of_questions", Integer.parseInt(input));
                mFirebaseAnalytics.logEvent("start_duel", bundle);
            }
        });

        popupWindow.showAtLocation(binding.getRoot(), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    private void setupQuestionNumberInput(EditText input) {
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{
                new InputFilterMinMax(1, 10),
                new InputFilter.LengthFilter(2)
        });
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
    }

    private void setupCategoryRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DuelCategoryAdapter(this,
                new ArrayList<>(),
                categoryName -> viewModel.selectCategory(categoryName));
        recyclerView.setAdapter(mAdapter);

        viewModel.getCategoriesData().observe(this, categories -> {
            if (categories != null) {
                mAdapter.updateCategories(categories);
            }
        });
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
            startActivity(new Intent(this, DuelListingActivity.class));
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

    private void dimBehind(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(container, p);
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(DuelActivity.this, DuelListingActivity.class));
                    }
                })
                .show();
    }

}
