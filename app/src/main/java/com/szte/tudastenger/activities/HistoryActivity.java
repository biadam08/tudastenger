package com.szte.tudastenger.activities;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.AnsweredQuestionAdapter;
import com.szte.tudastenger.databinding.ActivityHistoryBinding;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;

public class HistoryActivity extends DrawerBaseActivity {
    private ActivityHistoryBinding activityHistoryBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseUser user;
    private User currentUser;
    private CollectionReference mUsers;
    private CollectionReference mQuestions;
    private CollectionReference mAnsweredQuestions;

    private ArrayList<AnsweredQuestion> mAnsweredQuestionsData;
    private RecyclerView mRecyclerView;
    private TextView noSolvedQuestionTextView;
    private AnsweredQuestionAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityHistoryBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(activityHistoryBinding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
        }

        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
        mAnsweredQuestions = mFirestore.collection("AnsweredQuestions");
        mQuestions = mFirestore.collection("Questions");
        noSolvedQuestionTextView = findViewById(R.id.noSolvedQuestion);
        noSolvedQuestionTextView.setVisibility(View.GONE);

        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                displayQuestions();
            }
        });

    }

    private void displayQuestions() {
        Spinner spinner = findViewById(R.id.spinner);
        ArrayList<String> categories = new ArrayList<>();
        categories.add("Összes kategória");

        mFirestore.collection("Categories").orderBy("name").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Category category = document.toObject(Category.class);
                categories.add(category.getName());
            }
        });

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                queryData(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void queryData(String sortCategory) {
        mRecyclerView = findViewById(R.id.recyclerView);

        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        mAnsweredQuestionsData = new ArrayList<>();
        mAdapter = new AnsweredQuestionAdapter(this, mAnsweredQuestionsData);
        mRecyclerView.setAdapter(mAdapter);

        mAnsweredQuestionsData.clear();

        mAnsweredQuestions
                .whereEqualTo("userId", currentUser.getId())
                .orderBy("date", Query.Direction.ASCENDING) // firestore összetett index kellett hozzá
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String questionId = document.getString("questionId");
                            mQuestions
                                    .document(questionId)
                                    .get()
                                    .addOnCompleteListener(task2 -> {
                                        if (task2.isSuccessful()) {
                                            DocumentSnapshot questionDocument = task2.getResult();
                                            if (questionDocument.exists()) {
                                                String answer = document.getString("answer");
                                                boolean correct = document.getBoolean("correct");
                                                Timestamp timestamp = document.getTimestamp("date");
                                                String category = questionDocument.getString("category");
                                                if(sortCategory.equals("Összes kategória") || sortCategory.equals(category)) {
                                                    AnsweredQuestion answeredQuestion = new AnsweredQuestion(questionId, category, currentUser.getId(), timestamp, answer, correct);
                                                    mAnsweredQuestionsData.add(answeredQuestion);
                                                }
                                            }
                                        }
                                        mAdapter.notifyDataSetChanged();
                                        if(mAnsweredQuestionsData.isEmpty()){
                                            noSolvedQuestionTextView.setVisibility(View.VISIBLE);
                                        } else{
                                            noSolvedQuestionTextView.setVisibility(View.GONE);
                                        }
                                    });
                        }
                    } else {
                        Log.d("Error", task.getException().getMessage());
                    }
                });


    }
}