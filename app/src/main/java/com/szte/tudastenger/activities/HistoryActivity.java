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
import java.util.List;

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
        List<Category> categoryList = new ArrayList<>();
        Category allCategories = new Category("0", "Összes kategória", null);
        categoryList.add(allCategories);

        mFirestore.collection("Categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Category category = documentSnapshot.toObject(Category.class);
                        category.setId(documentSnapshot.getId());
                        categoryList.add(category);
                    }

                    ArrayAdapter<Category> adapter = new ArrayAdapter<>(HistoryActivity.this, android.R.layout.simple_spinner_item, categoryList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                });

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
                .orderBy("date", Query.Direction.DESCENDING) // firestore összetett index kellett hozzá
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String questionId = document.getString("questionId");

                            mQuestions.document(questionId).get().addOnCompleteListener(task2 -> {
                                if (task2.isSuccessful()) {
                                    DocumentSnapshot questionDocument = task2.getResult();
                                    if (questionDocument.exists()) {
                                        String answer = document.getString("answer");
                                        boolean correct = document.getBoolean("correct");
                                        Timestamp timestamp = document.getTimestamp("date");
                                        String categoryId = questionDocument.getString("category");

                                        mFirestore.collection("Categories").document(categoryId).get().addOnSuccessListener(categoryDoc -> {
                                            if (categoryDoc.exists()) {
                                                String categoryName = categoryDoc.getString("name");

                                                if (sortCategory.equals("Összes kategória") || sortCategory.equals(categoryName)) {
                                                    AnsweredQuestion answeredQuestion = new AnsweredQuestion(questionId, categoryName, currentUser.getId(), timestamp, answer, correct);
                                                    mAnsweredQuestionsData.add(answeredQuestion);
                                                }

                                                // A legújabb kitöltés legyen legfelül
                                                mAnsweredQuestionsData.sort((q1, q2) -> q2.getDate().compareTo(q1.getDate()));
                                                mAdapter.notifyDataSetChanged();

                                                // "Nincs kérdés" szöveg megjelenítése/eltüntetése
                                                if (mAnsweredQuestionsData.isEmpty()) {
                                                    noSolvedQuestionTextView.setVisibility(View.VISIBLE);
                                                } else {
                                                    noSolvedQuestionTextView.setVisibility(View.GONE);
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    } else {
                        Log.d("Error", task.getException().getMessage());
                    }
                });
    }
}