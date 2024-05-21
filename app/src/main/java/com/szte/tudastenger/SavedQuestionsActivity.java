package com.szte.tudastenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.databinding.ActivityMainBinding;
import com.szte.tudastenger.databinding.ActivitySavedQuestionsBinding;

import java.util.ArrayList;

public class SavedQuestionsActivity extends DrawerBaseActivity {

    private ActivitySavedQuestionsBinding activitySavedQuestionsBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private User currentUser;
    private CollectionReference mUsers;
    private CollectionReference mQuestions;
    private CollectionReference mSavedQuestions;

    private ArrayList<Question> mQuestionsData;
    private RecyclerView mRecyclerView;

    private SavedQuestionAdapter mAdapter;

    private TextView noSavedQuestionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySavedQuestionsBinding = ActivitySavedQuestionsBinding.inflate(getLayoutInflater());
        setContentView(activitySavedQuestionsBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
        }

        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
        mSavedQuestions = mFirestore.collection("SavedQuestions");
        mQuestions = mFirestore.collection("Questions");

        noSavedQuestionTextView = findViewById(R.id.noSavedQuestionTextView);
        noSavedQuestionTextView.setVisibility(View.VISIBLE);


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

        mQuestionsData = new ArrayList<>();
        mAdapter = new SavedQuestionAdapter(this, mQuestionsData, currentUser.getId());
        mRecyclerView.setAdapter(mAdapter);

        mQuestionsData.clear();

        mSavedQuestions
                .whereEqualTo("userId", currentUser.getId())
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
                                                String questionText = questionDocument.getString("questionText");
                                                ArrayList<String> answers = (ArrayList<String>) questionDocument.get("answers");
                                                int correctAnswerIndex = questionDocument.getLong("correctAnswerIndex").intValue();
                                                String category = questionDocument.getString("category");
                                                String image = questionDocument.getString("image");
                                                if(sortCategory.equals("Összes kategória") || sortCategory.equals(category)) {
                                                    Question question = new Question(questionId, questionText, category, answers, correctAnswerIndex, image);
                                                    mQuestionsData.add(question);
                                                }
                                                noSavedQuestionTextView.setVisibility(View.GONE);
                                            }
                                        }
                                        mAdapter.notifyDataSetChanged();
                                    });
                        }
                    } else {
                        Log.d("Error", task.getException().getMessage());
                    }
                });
    }
}