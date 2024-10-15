package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityQuizGameBinding;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class QuizGameActivity extends DrawerBaseActivity {

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ActivityQuizGameBinding activityQuizGameBinding;
    private CollectionReference mUsers;
    private CollectionReference mQuestions;
    private StorageReference storageReference;
    private User currentUser;
    private TextView userGold;
    private String intentCategoryId; // adott kategóriában való játszásnál a kategória neve
    private boolean isSelectedAnswer; // választott-e már választ a felhasználó az adott kérdésnél
    private String questionDocId; // kérdés dokumentum ID-ja, ami megegyezik a kérdés ID-jával
    private DocumentReference userRef;
    private String savedDocumentId; //mentés dokumentum ID-ja
    private boolean savedNow = false; // játék közben mentette el
    private boolean isMixed; // vegyes kvízjáték indult-e
    private boolean isCorrect = false; // helyes választ adott-e a felhasználó
    private Button helpButton;
    private Button saveQuestionButton;
    private ImageView questionImageView;
    private TextView questionTextView;
    private String userAnswer;
    private String correctAnswer;
    private String explanationText;
    private Button nextQuestionButton;
    private Button showExplanationButton;
    private Button goToHomeButton;
    private LinearLayout buttonsLayout;

    private TextView numCorrectAnswers;

    private Question actualQuestion;
    List<View> answerViews = new ArrayList<>();
    private boolean isHelpUsedYet; // aktuális kérdésnél volt-e már segítség használva
    private final Integer helpCost = 10; // a segítség ára

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityQuizGameBinding = ActivityQuizGameBinding.inflate(getLayoutInflater());
        setContentView(activityQuizGameBinding.getRoot());

        Intent intent = getIntent();

        // Adott kategóriájú kérdést akar
        if (intent != null && intent.hasExtra("intentCategoryId")) {
            intentCategoryId = intent.getStringExtra("intentCategoryId");
        }

        // Vegyes játékot indított
        if (intent != null && intent.hasExtra("mixed")) {
            isMixed = Boolean.parseBoolean(intent.getStringExtra("mixed"));
        }

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
        }

        mFirestore = FirebaseFirestore.getInstance();

        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
        mQuestions = mFirestore.collection("Questions");

        isSelectedAnswer = false;

        userGold = findViewById(R.id.userGold);
        helpButton = findViewById(R.id.helpButton);
        questionTextView = findViewById(R.id.questionTextView);
        saveQuestionButton = findViewById(R.id.saveQuestionButton);
        questionImageView = findViewById(R.id.questionImage);

        nextQuestionButton = findViewById(R.id.nextQuestionButton);
        showExplanationButton = findViewById(R.id.showExplanationButton);
        goToHomeButton = findViewById(R.id.goToHomeButton);
        buttonsLayout = findViewById(R.id.buttonsLinearLayout);

        numCorrectAnswers = findViewById(R.id.numCorrectAnswersTextView);

        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                userRef = mUsers.document(currentUser.getId());
                userGold.setText(currentUser.getGold().toString());
                queryRandomQuestion();
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isSelectedAnswer){
                    showOkAlertDialog("Már válaszoltál", "Ezt a kérdést már megválaszoltad, így nem vehetsz igénybe segítséget!");
                    return;
                }
                popUpHelp();
            }
        });
    }

    private void queryRandomQuestion() {
        if (savedNow) {
            saveQuestionButton.setText("Eltávolítás");
        } else {
            saveQuestionButton.setText("Mentés");
        }

        String userId = currentUser.getId();

        mFirestore.collection("AnsweredQuestions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(answeredQuestions -> {

                    List<String> answeredQuestionIds = new ArrayList<>();

                    for (DocumentSnapshot answeredQuestion : answeredQuestions.getDocuments()) {
                        answeredQuestionIds.add(answeredQuestion.getString("questionId"));
                    }

                    // Összes kérdés lekérdezése
                    Query query = mQuestions;

                    // Ha van kategória kiválasztva, akkor csak azon belül szűrünk
                    if (!isMixed) {
                        query = query.whereEqualTo("category", intentCategoryId);
                    }

                    query.get().addOnSuccessListener(questions -> {
                        List<DocumentSnapshot> documents = questions.getDocuments();
                        List<DocumentSnapshot> filteredDocuments = new ArrayList<>();

                        // Szűrés a megválaszolt kérdések alapján
                        for (DocumentSnapshot document : documents) {
                            if (!answeredQuestionIds.contains(document.getId())) {
                                filteredDocuments.add(document);
                            }
                        }

                        // Ha van még megválaszolatlan kérdés
                        if (!filteredDocuments.isEmpty()) {
                            DocumentSnapshot randomDoc = filteredDocuments.get(new Random().nextInt(filteredDocuments.size()));
                            Question question = randomDoc.toObject(Question.class);
                            if(question.getExplanationText() != null) {
                                explanationText = question.getExplanationText();
                            } else{
                                explanationText = "Sajnos nincs megjelenítendő magyarázat ehhez a kérdéshez";
                            }
                            questionDocId = randomDoc.getId();
                            displayQuestion(question);
                        } else {
                            handleNoMoreQuestions();
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getApplicationContext(), "Kérdés lekérése sikertelen!", Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Megválaszolt kérdések lekérése sikertelen!", Toast.LENGTH_LONG).show();
                });
    }

    private void handleNoMoreQuestions() {
        questionImageView = findViewById(R.id.questionImage);
        LinearLayout answersLayout = findViewById(R.id.answersLayout);
        questionImageView.setVisibility(View.GONE);
        answersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.GONE);

        if (intentCategoryId != null) {
            questionTextView.setText("Sajnos nincs több kérdés ebben a kategóriában!");
        } else {
            questionTextView.setText("Sajnos nincs több megválaszolatlan kérdés!");
        }
        saveQuestionButton.setClickable(false);
        helpButton.setClickable(false);

        Toast.makeText(getApplicationContext(), "Nincs több kérdés ebben a kategóriában!", Toast.LENGTH_LONG).show();
    }

    private void displayQuestion(Question question) {
        isHelpUsedYet = false;
        buttonsLayout.setVisibility(View.GONE);
        numCorrectAnswers.setText("");
        numCorrectAnswers.setVisibility(View.GONE);

        LinearLayout answersLayout = findViewById(R.id.answersLayout);
        answersLayout.removeAllViews();
        answerViews.clear();
        actualQuestion = question;

        if (question.getImage() == null) {
            questionImageView.setVisibility(View.GONE);
        } else{
            questionImageView.setVisibility(View.VISIBLE);
        }

        questionTextView.setText(question.getQuestionText());

        //Kérdés elmentése a Mentés gombbal
        saveQuestionButton.setOnClickListener(v -> {
            saveQuestion();
        });


        //Ha van kép, megjelenítjük
        if (question.getImage() != null && !question.getImage().isEmpty()) {
            ProgressDialog progressDialog  = new ProgressDialog(this);
            progressDialog.setTitle("Kép betöltése...");
            progressDialog.show();

            ImageView questionImageView = findViewById(R.id.questionImage);

            String imagePath = "images/" + question.getImage();
            storageReference = FirebaseStorage.getInstance().getReference().child(imagePath);

            storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(this)
                        .load(uri.toString())
                        .into(questionImageView);
            }).addOnFailureListener(exception -> {

            });

            progressDialog.dismiss();

        }

        for (int i = 0; i < question.getAnswers().size(); i++) {
            String answer = question.getAnswers().get(i);
            View answerCardView = getLayoutInflater().inflate(R.layout.answer_card, null);
            TextView answerTextView = answerCardView.findViewById(R.id.answerTextView);
            answerTextView.setText(answer);

            CardView cardView = answerCardView.findViewById(R.id.cardView);
            cardView.setCardBackgroundColor(getResources().getColor(R.color.lightbrowne));

            final int correctAnswerIndex = question.getCorrectAnswerIndex();
            final int clickedIndex = i;

            answerCardView.setOnClickListener(v -> {
                if(!isSelectedAnswer) {
                    isSelectedAnswer = true;
                    userAnswer = question.getAnswers().get(clickedIndex);
                    correctAnswer = question.getAnswers().get(correctAnswerIndex);
                    isCorrect = clickedIndex == correctAnswerIndex;

                    if (isCorrect) {
                        cardView.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
                    } else {
                        cardView.setCardBackgroundColor(getResources().getColor(R.color.incorrect_red));

                        View correctAnswerView = answersLayout.getChildAt(correctAnswerIndex);
                        CardView correctCardView = correctAnswerView.findViewById(R.id.cardView);
                        correctCardView.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
                    }

                    buttonsLayout.setVisibility(View.VISIBLE);

                    int goldChange = isCorrect ? 25 : -25;

                    AnsweredQuestion answeredQuestion = new AnsweredQuestion(questionDocId, question.getCategory(), currentUser.getId(), answer, isCorrect);
                    mFirestore.collection("AnsweredQuestions").add(answeredQuestion)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    userRef.update("gold", FieldValue.increment(goldChange));
                                    currentUser.setGold(currentUser.getGold() + goldChange);
                                    userGold.setText(currentUser.getGold().toString());

                                    DocumentReference questionRef = mFirestore.collection("Questions").document(questionDocId);
                                    if (isCorrect) {
                                        questionRef.update("numCorrectAnswers", FieldValue.increment(1));
                                    } else {
                                        questionRef.update("numWrongAnswers", FieldValue.increment(1));
                                    }

                                    String numCorrectAnswerText = "";
                                    int totalAnswers = question.getNumCorrectAnswers() + question.getNumWrongAnswers();
                                    if (totalAnswers > 0) {
                                        int correctPercentage = Math.round(((float) question.getNumCorrectAnswers() / totalAnswers) * 100);
                                        numCorrectAnswerText = "A játékosok " + correctPercentage + "%-a válaszolt helyesen erre a kérdésre";
                                        numCorrectAnswers.setText(numCorrectAnswerText);
                                        numCorrectAnswers.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                }
            });


            nextQuestionButton.setOnClickListener(v -> {
                isSelectedAnswer = false; // új kérdésnél még nem válaszolt
                savedNow = false;
                queryRandomQuestion();
            });

            showExplanationButton.setOnClickListener(v -> {
                popUpExplanation();
            });

            goToHomeButton.setOnClickListener(v -> {
                Intent intent = new Intent(QuizGameActivity.this, MainActivity.class);
                startActivity(intent);
            });

            answersLayout.addView(answerCardView);
            answerViews.add(answerCardView);

        }
    }

    private void saveQuestion() {
        HashMap<String, String> questionToSave = new HashMap<String, String>();
        questionToSave.put("userId", currentUser.getId());
        questionToSave.put("questionId", questionDocId);
        questionToSave.put("date", String.valueOf(Timestamp.now()));

        if (!savedNow) { // ha nincs mentve
            mFirestore.collection("SavedQuestions").add(questionToSave)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Toast.makeText(getApplicationContext(), "Sikeresen elmentve", Toast.LENGTH_SHORT).show();
                            savedNow = true;
                            saveQuestionButton.setText("Eltávolítás");
                            savedDocumentId = documentReference.getId();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Nem sikerült elmenteni", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            mFirestore.collection("SavedQuestions")
                    .whereEqualTo("userId", currentUser.getId())
                    .whereEqualTo("questionId", questionDocId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                    savedDocumentId = documentSnapshot.getId();

                                    mFirestore.collection("SavedQuestions").document(savedDocumentId)
                                            .delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(getApplicationContext(), "Sikeresen eltávolítva", Toast.LENGTH_SHORT).show();
                                                    savedNow = false;
                                                    saveQuestionButton.setText("Mentés");
                                                }
                                            });
                                }
                            }
                        }
                    });
        }
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

        explanationTextView.setText(explanationText);

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
    }

    public void popUpHelp() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
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

        removeWrongAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(QuizGameActivity.this);
                builder.setTitle("Segítség vásárlása");
                builder.setMessage("Biztosan megvásárolod a segítséget " + helpCost.toString() + " aranyért?");

                builder.setPositiveButton("Igen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(currentUser.getGold() >= helpCost) {
                            if(isHelpUsedYet){
                                popupWindow.dismiss();
                                showOkAlertDialog("Már használtál segítséget","Ennél a kérdésnél már használtál segítséget!");
                            } else {
                                userRef.update("gold", FieldValue.increment(-helpCost))
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                currentUser.setGold(currentUser.getGold() - helpCost);
                                                userGold.setText(currentUser.getGold().toString());
                                                popupWindow.dismiss();
                                                removeWrongAnswer();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(QuizGameActivity.this, "A segítséget nem sikerült igénybevenni!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            popupWindow.dismiss();
                            showOkAlertDialog("Nincs elég aranyad","A segítség megvásárlásához szükséges arannyal sajnos nem rendelkezel!");
                        }
                    }
                });

                builder.setNegativeButton("Nem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        closeHelpPopupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
    }

    private void removeWrongAnswer() {
        isHelpUsedYet = true;
        List<Integer> incorrectAnswers = new ArrayList<>();
        for (int i = 0; i < actualQuestion.getAnswers().size(); i++) {
            if (i != actualQuestion.getCorrectAnswerIndex()) {
                incorrectAnswers.add(i);
            }
        }

        if (!incorrectAnswers.isEmpty()) {
            Random random = new Random();
            int randomIncorrectIndex = incorrectAnswers.get(random.nextInt(incorrectAnswers.size()));

            View incorrectAnswerView = answerViews.get(randomIncorrectIndex);
            incorrectAnswerView.setVisibility(View.INVISIBLE);
            incorrectAnswerView.setClickable(false);
        }
    }

    private void showOkAlertDialog(String title, String message){
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