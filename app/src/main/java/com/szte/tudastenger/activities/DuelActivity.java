package com.szte.tudastenger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.InputFilterMinMax;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.DuelCategoryAdapter;
import com.szte.tudastenger.adapters.FriendAdapter;
import com.szte.tudastenger.databinding.ActivityDuelBinding;
import com.szte.tudastenger.interfaces.OnCategoryClickListener;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class DuelActivity extends DrawerBaseActivity{
    private ActivityDuelBinding activityDuelBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private User currentUser;
    private DocumentReference userRef;
    private StorageReference storageReference;
    private CollectionReference mUsers;
    private String challengerUserId;
    private String challengedUserId;
    private String duelId; // ha a kihívott játszik
    private CollectionReference mQuestions;
    private Button saveQuestionButton;
    private ImageView questionImageView;
    private TextView questionTextView;

    private CollectionReference mCategories;

    private ArrayList<Category> mCategoriesData;

    private RecyclerView mRecyclerView;

    private DuelCategoryAdapter mAdapter;
    private Button startMixedGameButton;
    private Button startDuelButton;
    private String category;
    private Integer questionNumber;
    private Button configureDuelButton;
    private Button backToFriendsButton;
    private LinearLayout buttonsLinearLayout;
    private ArrayList<Question> questionsList;
    private ArrayList<String> questionIdsList;
    private ArrayList<Boolean> challengerUserResults;
    private ArrayList<Boolean> challengedUserResults;
    private Integer actualQuestionNumber = 0;
    private boolean isCorrect = false; // helyes választ adott-e a felhasználó
    private String userAnswer;
    private String correctAnswer;
    private boolean isSelectedAnswer;
    private TextView questionNumberTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityDuelBinding = ActivityDuelBinding.inflate(getLayoutInflater());
        setContentView(activityDuelBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
        }

        Intent intent = getIntent();

        if (intent != null && intent.hasExtra("challengerUserId")) {
            challengerUserId = intent.getStringExtra("challengerUserId");
        }

        if (intent != null && intent.hasExtra("challengedUserId")) {
            challengedUserId = intent.getStringExtra("challengedUserId");
        }

        if (intent != null && intent.hasExtra("duelId")) {
            duelId = intent.getStringExtra("duelId");
        }

        mFirestore = FirebaseFirestore.getInstance();

        mUsers = mFirestore.collection("Users");
        mQuestions = mFirestore.collection("Questions");

        questionTextView = findViewById(R.id.questionTextView);
        questionNumberTextView = findViewById(R.id.questionNumberTextView);
        saveQuestionButton = findViewById(R.id.saveQuestionButton);
        questionImageView = findViewById(R.id.questionImage);

        configureDuelButton = findViewById(R.id.configureDuelButton);
        backToFriendsButton = findViewById(R.id.backToFriendsActivityButton);
        buttonsLinearLayout = findViewById(R.id.buttonsLinearLayout);

        challengerUserResults = new ArrayList<>();
        challengedUserResults = new ArrayList<>();


        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                userRef = mUsers.document(currentUser.getId());
            }
            if(currentUser.getId().equals(challengerUserId)){
                makeDuel();
            } else{
                startDuel();
            }
        });

        configureDuelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                category = null;
                questionNumber = null;
                makeDuel();
            }
        });

        backToFriendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DuelActivity.this, FriendsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void makeDuel() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_start_a_duel, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);


        mCategories = mFirestore.collection("Categories");
        startMixedGameButton = popupView.findViewById(R.id.startMixedGameButton);
        startMixedGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                category = "mixed";
            }
        });

        int minValue = 1;
        int maxValue = 10;

        EditText input = (EditText) popupView.findViewById(R.id.questionNumberEditText);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]
                {
                        new InputFilterMinMax(minValue, maxValue),
                        new InputFilter.LengthFilter(String.valueOf(maxValue).length())
                });
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        mRecyclerView = popupView.findViewById(R.id.catRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mCategoriesData = new ArrayList<>();
        mAdapter = new DuelCategoryAdapter(this, mCategoriesData, new OnCategoryClickListener() {
            @Override
            public void onCategoryClicked(String categoryName) {
                category = categoryName;
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        mCategories.orderBy("name").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                Category category = document.toObject(Category.class);
                mCategoriesData.add(category);
            }
            mAdapter.notifyDataSetChanged();
        });


        startDuelButton = popupView.findViewById(R.id.startDuel);
        startDuelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                questionNumber = Integer.parseInt(input.getText().toString());

                if(questionNumber != null && questionNumber <= 10 && questionNumber >= 1 && category != null){
                    popupWindow.dismiss();
                    initializeQuestions();
                }
            }
        });

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    private void initializeQuestions() {
        questionsList = new ArrayList<>();
        questionIdsList = new ArrayList<>();
        buttonsLinearLayout.setVisibility(View.GONE);

        Query query = mQuestions;

        if (!category.equals("mixed")) {
            query = query.whereEqualTo("category", category);
        }

        query
                .get()
                .addOnSuccessListener(questions -> {
                    List<DocumentSnapshot> documents = questions.getDocuments();
                    if (documents.size() >= questionNumber) {
                        Random random = new Random();

                        while (questionsList.size() < questionNumber) {
                            DocumentSnapshot randomDoc = documents.get(random.nextInt(documents.size()));
                            Question question = randomDoc.toObject(Question.class);
                            if (question != null && !questionIdsList.contains(question.getId())) {
                                questionsList.add(question);
                                questionIdsList.add(question.getId());
                            }
                        }

                        startDuel();

                    } else {
                        Toast.makeText(getApplicationContext(), "Nincs elég kérdés ebben a kategóriában!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Nincs több kérdés ebben a kategóriában!", Toast.LENGTH_LONG).show();
                });

    }

    private void startDuel() {
        questionNumberTextView.setText((actualQuestionNumber + 1) + "/" + questionsList.size());
        displayQuestion(questionsList.get(actualQuestionNumber));
        actualQuestionNumber++;
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
                        if(currentUser.getId().equals(challengerUserId)){
                            challengerUserResults.add(true);
                        } else{
                            challengedUserResults.add(true);
                        }
                    } else {
                        popUpResult();
                        if(currentUser.getId().equals(challengerUserId)){
                            challengerUserResults.add(false);
                        } else{
                            challengedUserResults.add(false);
                        }
                    }
                } else {
                    //lekezelni, hogyha nem jött fel az új ablak és már nem kattinthat újat
                }
            });
            answersLayout.addView(answerCardView);
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

        Button nextQuestionButton = popupView.findViewById(R.id.nextQuestionButton);
        LinearLayout buttonsLinearLayout = popupView.findViewById(R.id.buttonsLinearLayout);
        buttonsLinearLayout.setVisibility(View.GONE);

        if(actualQuestionNumber == questionsList.size()){
            nextQuestionButton.setText("Befejezés");
        }

        nextQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                isSelectedAnswer = false;
                if(actualQuestionNumber == questionsList.size()){
                    finishDuel();
                } else {
                    startDuel();
                }
            }
        });
    }

    private void finishDuel() {
        if(currentUser.getId().equals(challengerUserId)){
            Duel duel = new Duel(null, challengerUserId, challengedUserId, questionIdsList, challengerUserResults, null);
            Log.d("duel", duel.getChallengerUid());
            Log.d("duel", duel.getChallengedUid());
            Log.d("duel", duel.getQuestionIds().toString());
            Log.d("duel", duel.getChallengerUserResults().toString());
            mFirestore.collection("Duels").add(duel)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            String documentId = documentReference.getId();
                            duel.setId(documentId);

                            mFirestore.collection("Duels").document(documentId)
                                    .update("id", documentId);
                        }
                    });
        } else {
            mFirestore.collection("Duels").document(duelId)
                    .update("challengedUserResults", challengedUserResults);
        }
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