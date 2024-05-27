package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.CategoryAdapter;
import com.szte.tudastenger.databinding.ActivityMainBinding;
import com.szte.tudastenger.databinding.ActivitySavedQuestionGameBinding;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.HashMap;

public class SavedQuestionGameActivity extends DrawerBaseActivity {
    private ActivitySavedQuestionGameBinding activitySavedQuestionGameBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private User currentUser;
    private StorageReference storageReference;
    private CollectionReference mUsers;
    private CollectionReference mQuestions;
    private String savedQuestionId; //mentett kérdés ID-je
    private String savedDocumentId; //mentés dokumentum ID-ja
    private boolean isSelectedAnswer; // választott-e már választ a felhasználó az adott kérdésnél
    private Button saveQuestionButton;
    private ImageView questionImageView;
    private TextView questionTextView;
    private boolean isQuestionSavedNow = false; // korábban már mentett kérdés volt: ez változhat most
    private String userAnswer;
    private String correctAnswer;
    private boolean isCorrect = false; // helyes választ adott-e a felhasználó




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySavedQuestionGameBinding = ActivitySavedQuestionGameBinding.inflate(getLayoutInflater());
        setContentView(activitySavedQuestionGameBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
        }

        Intent intent = getIntent();

        // Mentett kérdések activity-ről érkezik a felhasználó
        if (intent != null && intent.hasExtra("questionId")) {
            savedQuestionId = intent.getStringExtra("questionId");
        }

        mFirestore = FirebaseFirestore.getInstance();

        mUsers = mFirestore.collection("Users");
        mQuestions = mFirestore.collection("Questions");

        isSelectedAnswer = false;

        questionTextView = findViewById(R.id.questionTextView);
        saveQuestionButton = findViewById(R.id.saveQuestionButton);
        questionImageView = findViewById(R.id.questionImage);

        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                userRef = mUsers.document(currentUser.getId());
                startGame();
            }
        });

    }

    private void startGame() {
        isQuestionSavedNow = true;

        setTitle("Mentett kérdés");

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
                            displayQuestion(question);
                        }
                    }
                });
    }

    private void displayQuestion(Question question) {
        LinearLayout answersLayout = findViewById(R.id.answersLayout);
        answersLayout.removeAllViews();

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

            final int correctAnswerIndex = question.getCorrectAnswerIndex();
            final int clickedIndex = i;

            answerCardView.setOnClickListener(v -> {
                if(!isSelectedAnswer) {
                    isSelectedAnswer = true;
                    userAnswer = question.getAnswers().get(clickedIndex);
                    correctAnswer = question.getAnswers().get(correctAnswerIndex);
                    isCorrect = false;

                    if (clickedIndex == correctAnswerIndex) {
                        isCorrect = true;
                        popUpResult();
                    } else {
                        popUpResult();
                    }
                } else {
                    //lekezelni, hogyha nem jött fel az új ablak és már nem kattinthat újat
                }
            });
            answersLayout.addView(answerCardView);
        }
    }
    private void saveQuestion() {
        Log.d("Mentés kezdődne", "Mentés kezdődne...");
        HashMap<String, String> questionToSave = new HashMap<String, String>();
        questionToSave.put("userId", currentUser.getId());
        questionToSave.put("questionId", savedQuestionId);
        questionToSave.put("date", String.valueOf(Timestamp.now()));

        if (!isQuestionSavedNow) { // ha nincs mentve
            mFirestore.collection("SavedQuestions").add(questionToSave)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Toast.makeText(getApplicationContext(), "Sikeresen elmentve", Toast.LENGTH_SHORT).show();
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
                    .whereEqualTo("questionId", savedQuestionId)
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
        Button savedQuestionsButton = popupView.findViewById(R.id.nextQuestionButton);

        savedQuestionsButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmarks,0,0,0);
        savedQuestionsButton.setText("Mentett kérdések");

        if(isQuestionSavedNow){
            bookmarkButton.setImageResource(R.drawable.ic_remove_bookmark);
            bookmarkButton.setTag("bookmarked");
        } else{
            bookmarkButton.setTag("not_bookmarked");
        }

        savedQuestionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SavedQuestionGameActivity.this, SavedQuestionsActivity.class));
            }
        });

        homepageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SavedQuestionGameActivity.this, MainActivity.class));
            }
        });

        bookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveQuestion();
                if(bookmarkButton.getTag().equals("bookmarked")) {
                    bookmarkButton.setImageResource(R.drawable.ic_add_bookmark);
                    bookmarkButton.setTag("not_bookmarked");
                } else {
                    bookmarkButton.setImageResource(R.drawable.ic_remove_bookmark);
                    bookmarkButton.setTag("bookmarked");
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

}