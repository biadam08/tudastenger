package com.szte.tudastenger.activities;

import androidx.cardview.widget.CardView;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.InputFilterMinMax;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.DuelCategoryAdapter;
import com.szte.tudastenger.databinding.ActivityDuelBinding;
import com.szte.tudastenger.interfaces.OnCategoryClickListener;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Button startDuelButton;
    private String category;
    private String categoryId;
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
    private Duel actualDuel; //ha a kihívott játszik, amúgy null
    private String result;
    private LinearLayout navigationButtonsLayout;
    private String explanationText;
    private Button nextQuestionButton;
    private Button showExplanationButton;
    private boolean isLastQuestion;



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
        nextQuestionButton = findViewById(R.id.nextQuestionButton);
        showExplanationButton = findViewById(R.id.showExplanationButton);

        buttonsLinearLayout = findViewById(R.id.buttonsLinearLayout);
        navigationButtonsLayout = findViewById(R.id.navigationButtonsLayout);

        challengerUserResults = new ArrayList<>();
        challengedUserResults = new ArrayList<>();
        questionsList = new ArrayList<>();
        questionIdsList = new ArrayList<>();

        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                userRef = mUsers.document(currentUser.getId());
            }
            //ha a játékos a kihívó
            if(currentUser != null && currentUser.getId().equals(challengerUserId)){
                makeDuel();
            }
            //ha nem kihívó, akkor töltse be a kihívást
            else{
                buttonsLinearLayout.setVisibility(View.GONE);
                loadDuelData();
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

    private void loadDuelData() {
        mFirestore.collection("Duels").document(duelId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            actualDuel = documentSnapshot.toObject(Duel.class);
                            questionIdsList = actualDuel.getQuestionIds();
                            Log.d("questionIdsList", questionIdsList.toString());
                            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                            for (String questionId : questionIdsList) {
                                Task<DocumentSnapshot> task = mFirestore.collection("Questions").document(questionId).get();
                                tasks.add(task);
                            }

                            Tasks.whenAllSuccess(tasks).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
                                @Override
                                public void onSuccess(List<Object> list) {
                                    for (Object object : list) {
                                        DocumentSnapshot document = (DocumentSnapshot) object;
                                        Question question = document.toObject(Question.class);
                                        Log.d("questions", question.getId());
                                        questionsList.add(question);
                                    }
                                    startDuel();
                                }
                            });
                        }
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
        mCategoriesData.add(new Category(null, "Vegyes kategória", null));

        mAdapter = new DuelCategoryAdapter(this, mCategoriesData, new OnCategoryClickListener() {
            @Override
            public void onCategoryClicked(String categoryName) {
                if(!categoryName.equals("Vegyes kategória")) {
                    mFirestore.collection("Categories")
                            .whereEqualTo("name", categoryName)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                    categoryId = documentSnapshot.getId();
                                }
                            });
                    category = categoryName;
                } else{
                    categoryId = null;
                    category = "mixed";
                }
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
        if(categoryId == null){
            categoryId = "mixed";
        }


        buttonsLinearLayout.setVisibility(View.GONE);

        Query query = mQuestions;

        if (!category.equals("mixed")) {
            query = query.whereEqualTo("category", categoryId);
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
                        makeDuel();
                        Toast.makeText(getApplicationContext(), "Nincs elég kérdés ebben a kategóriában!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Nincs több kérdés ebben a kategóriában!", Toast.LENGTH_LONG).show();
                });

    }

    private void startDuel() {
        actualQuestionNumber++;
        questionNumberTextView.setText(actualQuestionNumber + "/" + questionsList.size());
        if(actualQuestionNumber == questionIdsList.size()){
            isLastQuestion = true;
        }
        displayQuestion(questionsList.get(actualQuestionNumber - 1));
    }

    private void displayQuestion(Question question) {
        navigationButtonsLayout.setVisibility(View.GONE);

        LinearLayout answersLayout = findViewById(R.id.answersLayout);
        answersLayout.removeAllViews();

        if (question.getImage() == null) {
            questionImageView.setVisibility(View.GONE);
        } else{
            questionImageView.setVisibility(View.VISIBLE);
        }

        questionTextView.setText(question.getQuestionText());

        if(question.getExplanationText() != null) {
            explanationText = question.getExplanationText();
        } else{
            explanationText = "Sajnos nincs megjelenítendő magyarázat ehhez a kérdéshez";
        }

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
                    isCorrect = false;

                    if (clickedIndex == correctAnswerIndex) {
                        isCorrect = true;
                        if(currentUser != null && currentUser.getId().equals(challengerUserId)) {
                            challengerUserResults.add(true);
                        } else{
                            challengedUserResults.add(true);
                        }
                    } else {
                        if(currentUser != null && currentUser.getId().equals(challengerUserId)) {
                            challengerUserResults.add(false);
                        } else{
                            challengedUserResults.add(false);
                        }
                    }

                    if (isCorrect) {
                        cardView.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
                    } else {
                        cardView.setCardBackgroundColor(getResources().getColor(R.color.incorrect_red));

                        View correctAnswerView = answersLayout.getChildAt(correctAnswerIndex);
                        CardView correctCardView = correctAnswerView.findViewById(R.id.cardView);
                        correctCardView.setCardBackgroundColor(getResources().getColor(R.color.correct_green));
                    }

                    navigationButtonsLayout.setVisibility(View.VISIBLE);
                    if(isLastQuestion){
                        nextQuestionButton.setText("Befejezés");
                    }
                } else {
                    //lekezelni, hogyha nem jött fel az új ablak és már nem kattinthat újat
                }
            });

            nextQuestionButton.setOnClickListener(v -> {
                isSelectedAnswer = false;
                if(actualQuestionNumber == questionsList.size()){
                    finishDuel();
                } else {
                    startDuel();
                }
            });

            showExplanationButton.setOnClickListener(v -> {
                popUpExplanation();
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

        if(actualQuestionNumber == questionIdsList.size()) {
            if(currentUser.getId().equals(challengerUserId)) {
                int challengerUserCorrectAnswerNumber = 0;
                for (int i = 0; i < challengerUserResults.size(); i++) {
                    if (challengerUserResults.get(i)) {
                        challengerUserCorrectAnswerNumber++;
                    }
                }
                result = "Eredményed: " + challengerUserCorrectAnswerNumber + "/" + questionIdsList.size();
                resultTextView.setText(result);
            } else{
                AtomicInteger challengerUserCorrectAnswerNumber = new AtomicInteger();
                AtomicInteger challengedUserCorrectAnswerNumber = new AtomicInteger();

                mFirestore.collection("Duels")
                        .document(duelId)
                        .get()
                        .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot questionDocument = task.getResult();
                                        if (questionDocument.exists()) {
                                            challengerUserResults = (ArrayList<Boolean>) questionDocument.get("challengerUserResults");
                                            for (int i = 0; i < questionIdsList.size(); i++) {
                                                if (challengerUserResults.get(i)) {
                                                    challengerUserCorrectAnswerNumber.getAndIncrement();
                                                }
                                                if (challengedUserResults.get(i)) {
                                                    challengedUserCorrectAnswerNumber.getAndIncrement();
                                                }
                                            }
                                            result = "Eredményed: " + challengedUserCorrectAnswerNumber + "/" + questionIdsList.size() + "\n" + "A kihívó eredménye: " + challengerUserCorrectAnswerNumber + "/" + questionIdsList.size();
                                            resultTextView.setText(result);
                                        }
                                    }
                                }
                        );
            }
        }

        Button finishDuelButton  = popupView.findViewById(R.id.finishDuelButton);

        finishDuelButton.setOnClickListener(v -> {
            Intent intent = new Intent(DuelActivity.this, DuelListingActivity.class);
            startActivity(intent);
        });

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    private void finishDuel() {
        if(currentUser != null && currentUser.getId().equals(challengerUserId)) {
            Duel duel = new Duel(null, challengerUserId, challengedUserId, categoryId, questionIdsList, challengerUserResults, null);

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
                    .update(
                            "challengedUserResults", challengedUserResults,
                            "finished", true
                    );
        }
        popUpResult();
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