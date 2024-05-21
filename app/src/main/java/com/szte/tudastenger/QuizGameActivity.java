package com.szte.tudastenger;

import androidx.annotation.NonNull;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.szte.tudastenger.databinding.ActivityQuizGameBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class QuizGameActivity extends DrawerBaseActivity {

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private String categoryName;
    private ActivityQuizGameBinding activityQuizGameBinding;
    private CollectionReference mUsers;
    private CollectionReference mQuestions;
    private StorageReference storageReference;
    private User currentUser;
    private TextView userGold;
    private boolean isSelectedAnswer;
    private String questionDocId;
    private DocumentReference userRef;
    private boolean savedQuestion = false; // korábban már alapból mentett kérdés volt
    private boolean isQuestionSavedNow = false; // korábban már mentett kérdés volt: ez változhat most
    private boolean savedNow = false; // játék közben mentette el
    private String savedQuestionId;
    private Button saveQuestionButton;
    private String savedDocumentId;
    private boolean isMixed;
    private boolean isCorrect = false;
    private Button helpButton;
    private TextView questionTextView;
    private String userAnswer;
    private String correctAnswer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityQuizGameBinding = ActivityQuizGameBinding.inflate(getLayoutInflater());
        setContentView(activityQuizGameBinding.getRoot());

        Intent intent = getIntent();

        // Adott kategóriájú kérdést akar
        if (intent != null && intent.hasExtra("CategoryName")) {
            categoryName = intent.getStringExtra("CategoryName");
        }

        // Mentett kérdések activity-ről érkezik a felhasználó
        if (intent != null && intent.hasExtra("questionId")) {
            savedQuestionId = intent.getStringExtra("questionId");
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


        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                userRef = mUsers.document(currentUser.getId());
                userGold.setText(currentUser.getGold().toString());
                startGame();
            }
        });
    }

    private void startGame() {
        if(savedQuestionId == null) {
            queryRandomQuestion();
        } else{
            savedQuestion = true;
            isQuestionSavedNow = true;

            setTitle("Mentett kérdés");
            ImageView coinImageView = findViewById(R.id.coinImageView);
            TextView userGold = findViewById(R.id.userGold);
            helpButton.setVisibility(View.GONE);
            coinImageView.setVisibility(View.GONE);
            userGold.setVisibility(View.GONE);

            saveQuestionButton.setText("Eltávolítás");

            Toast.makeText(getApplicationContext(), "Mentett kérdés!", Toast.LENGTH_SHORT).show();

            mQuestions
                    .document(savedQuestionId)
                    .get()
                    .addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            DocumentSnapshot questionDocument = task2.getResult();
                            if (questionDocument.exists()) {
                                String questionText = questionDocument.getString("questionText");
                                ArrayList<String> answers = (ArrayList<String>) questionDocument.get("answers");
                                int correctAnswerIndex = questionDocument.getLong("correctAnswerIndex").intValue();
                                String category = questionDocument.getString("category");
                                String image = questionDocument.getString("image");
                                Question question = new Question(savedQuestionId, questionText, category, answers, correctAnswerIndex, image);
                                Log.d("EXTRA", question.getId() + ", " + question.getQuestionText() + ": " + question.getCategory() + " - " + question.getAnswers().get(0));
                                questionDocId = savedQuestionId; //törléshez szükséges
                                displayQuestion(question);
                            }
                        }
                    });
        }
    }

    private void queryRandomQuestion() {
        String userId = currentUser.getId();

        mFirestore.collection("AnsweredQuestions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(answeredQuestions -> {
                    List<String> answeredQuestionIds = new ArrayList<>();
                    for (DocumentSnapshot answeredQuestion : answeredQuestions.getDocuments()) {
                        answeredQuestionIds.add(answeredQuestion.getString("questionId"));
                    }

                    Query query = mQuestions;
                    if (!answeredQuestionIds.isEmpty()) {
                        query = query.whereNotIn("id", answeredQuestionIds);
                    }
                    if (!isMixed) {
                        query = query.whereEqualTo("category", categoryName);  // Firestore index létrehozás volt szükséges hozzá
                    }

                    query
                            .get()
                            .addOnSuccessListener(questions -> {
                                List<DocumentSnapshot> documents = questions.getDocuments();
                                if (documents.size() > 0) {
                                    // Véletlenszerű dokumentum kiválasztása
                                    DocumentSnapshot randomDoc = documents.get(new Random().nextInt(documents.size()));
                                    Question question = randomDoc.toObject(Question.class);
                                    questionDocId = randomDoc.getId();
                                    displayQuestion(question);
                                } else{
                                    ImageView questionImage = findViewById(R.id.questionImage);
                                    LinearLayout answersLayout = findViewById(R.id.answersLayout);
                                    questionImage.setVisibility(View.GONE);
                                    answersLayout.setVisibility(View.GONE);

                                    if(!categoryName.isEmpty()) {
                                        questionTextView.setText("Sajnos nincs több kérdés ebben a kategóriában!");
                                    } else{
                                        questionTextView.setText("Sajnos nincs több megválaszolatlan kérdés!");
                                    }
                                    saveQuestionButton.setClickable(false);
                                    helpButton.setClickable(false);

                                    Toast.makeText(getApplicationContext(), "Nincs több kérdés ebben a kategóriában!", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getApplicationContext(), "Nincs több kérdés ebben a kategóriában!", Toast.LENGTH_LONG).show();
                            });
                });
    }



    private void displayQuestion(Question question) {
        LinearLayout answersLayout = findViewById(R.id.answersLayout);
        answersLayout.removeAllViews();

        if (question.getImage() == null) {
            ImageView questionImageView = findViewById(R.id.questionImage);
            questionImageView.setVisibility(View.GONE);
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

            final int correctAnswerIndex = question.getCorrectAnswerIndex();
            final int clickedIndex = i;

            answerCardView.setOnClickListener(v -> {
                if(!isSelectedAnswer) {
                    isSelectedAnswer = true;
                    userAnswer = question.getAnswers().get(clickedIndex);
                    correctAnswer = question.getAnswers().get(correctAnswerIndex);

                    if (clickedIndex == correctAnswerIndex) {
                        isCorrect = true;
                        popUpResult();
                    } else {
                        popUpResult();
                    }

                    int goldChange = isCorrect ? 25 : -25;

                    if(!savedQuestion) {
                        AnsweredQuestion answeredQuestion = new AnsweredQuestion(questionDocId, question.getCategory(), currentUser.getId(), answer, isCorrect);
                        mFirestore.collection("AnsweredQuestions").add(answeredQuestion)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        userRef.update("gold", FieldValue.increment(goldChange))
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("Update", "Sikeres arany módosítás");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.w("Update", "Arany módosítása sikertelen", e);
                                                });
                                        currentUser.setGold(currentUser.getGold() + goldChange);
                                        userGold.setText(currentUser.getGold().toString());
                                        Toast.makeText(QuizGameActivity.this, "Sikeresen elmentve a jó válasz!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(QuizGameActivity.this, "Hiba történt a mentés során.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Már válaszoltál...", Toast.LENGTH_SHORT).show();
                    //lekezelni, hogyha nem jött fel az új ablak és már nem kattinthat újat
                }
            });
            answersLayout.addView(answerCardView);
        }
    }

    private void saveQuestion() {
        HashMap<String, String> questionToSave = new HashMap<String, String>();
        questionToSave.put("userId", currentUser.getId());
        questionToSave.put("questionId", questionDocId);
        questionToSave.put("date", String.valueOf(Timestamp.now()));

        if (!savedNow && !isQuestionSavedNow) { // ha nincs mentve
            mFirestore.collection("SavedQuestions").add(questionToSave)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Toast.makeText(getApplicationContext(), "Sikeresen elmentve", Toast.LENGTH_SHORT).show();
                            savedNow = true;
                            isQuestionSavedNow = true;
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
        } else { // ha korábban lett mentve a kérdés, nem most
            //a SavedQuestions dokumentum ID-jának lekérdezése
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
                                    Log.d("Talalat", savedDocumentId);

                                    mFirestore.collection("SavedQuestions").document(savedDocumentId)
                                            .delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(getApplicationContext(), "Sikeresen eltávolítva", Toast.LENGTH_SHORT).show();
                                                    savedNow = false;
                                                    isQuestionSavedNow = false;
                                                    saveQuestionButton.setText("Mentés");
                                                }
                                            });
                                }
                            } else {
                                Log.d("Talalat", "Nincs ilyen dokumentum");
                            }
                        }
                    });
        }
    }

    public void popUpResult() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_quiz_result, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(false);

        TextView resultTextView = popupView.findViewById(R.id.resultTextView);
        TextView userAnswerTextView = popupView.findViewById(R.id.userAnswerTextView);
        TextView correctAnswerTextView = popupView.findViewById(R.id.correctAnswerTextView);

        if(isCorrect){
            resultTextView.setText("Helyes választ adtál!");
            userAnswerTextView.setVisibility(View.GONE);
            correctAnswerTextView.setVisibility(View.GONE);
        } else{
            resultTextView.setText("Hibás választ adtál");
            userAnswerTextView.setVisibility(View.VISIBLE);
            correctAnswerTextView.setVisibility(View.VISIBLE);
            userAnswerTextView.setText("Válaszod:\n" + userAnswer);
            correctAnswerTextView.setText("Helyes válasz:\n" + correctAnswer);
        }
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);

        ImageButton homepageButton = popupView.findViewById(R.id.homepageButton);
        ImageButton bookmarkButton = popupView.findViewById(R.id.bookmarkButton);
        Button nextQuestionButton = popupView.findViewById(R.id.nextQuestionButton);

        if(savedQuestion){
            nextQuestionButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmarks,0,0,0);
            nextQuestionButton.setText("Mentett kérdések");
        }

        if(isQuestionSavedNow){
            bookmarkButton.setImageResource(R.drawable.ic_remove_bookmark);
            bookmarkButton.setTag("not_bookmarked");
        }

        homepageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(QuizGameActivity.this, MainActivity.class));
            }
        });

        bookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveQuestion();
                if(bookmarkButton.getTag() != null && bookmarkButton.getTag().equals("bookmarked")) {
                    bookmarkButton.setImageResource(R.drawable.ic_remove_bookmark);
                    bookmarkButton.setTag("not_bookmarked");
                } else {
                    bookmarkButton.setImageResource(R.drawable.ic_add_bookmark);
                    bookmarkButton.setTag("bookmarked");
                }
            }
        });

        nextQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!savedQuestion){
                    popupWindow.dismiss();
                    isSelectedAnswer = false; // új kérdésnél még nem válaszolt
                    queryRandomQuestion();
                }
            }
        });


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

    public static void disableDim(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.0f;
        wm.updateViewLayout(container, p);
    }
}