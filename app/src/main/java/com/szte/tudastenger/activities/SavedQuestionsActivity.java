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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.SavedQuestionAdapter;
import com.szte.tudastenger.databinding.ActivitySavedQuestionsBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.List;

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
    private Spinner spinner;

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

        noSavedQuestionTextView = findViewById(R.id.noSavedQuestion);
        noSavedQuestionTextView.setVisibility(View.VISIBLE);
        spinner = findViewById(R.id.spinner);


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

            ArrayAdapter<Category> adapter = new ArrayAdapter<>(SavedQuestionsActivity.this, android.R.layout.simple_spinner_item, categoryList);
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

                            mQuestions.document(questionId).get().addOnCompleteListener(task2 -> {
                                if (task2.isSuccessful()) {
                                    DocumentSnapshot questionDocument = task2.getResult();
                                    if (questionDocument.exists()) {
                                        String questionText = questionDocument.getString("questionText");
                                        ArrayList<String> answers = (ArrayList<String>) questionDocument.get("answers");
                                        int correctAnswerIndex = questionDocument.getLong("correctAnswerIndex").intValue();
                                        String categoryId = questionDocument.getString("category");
                                        String image = questionDocument.getString("image");
                                        String explanationText = questionDocument.getString("explanationText");

                                        mFirestore.collection("Categories").document(categoryId).get().addOnSuccessListener(categoryDoc -> {
                                            if (categoryDoc.exists()) {
                                                String categoryName = categoryDoc.getString("name");

                                                if (sortCategory.equals("Összes kategória") || sortCategory.equals(categoryName)) {

                                                    Question question = new Question(questionId, questionText, categoryName, answers, correctAnswerIndex, image, explanationText);
                                                    mQuestionsData.add(question);

                                                    mAdapter.notifyDataSetChanged();

                                                    if (mQuestionsData.isEmpty()) {
                                                        noSavedQuestionTextView.setVisibility(View.VISIBLE);
                                                    } else {
                                                        noSavedQuestionTextView.setVisibility(View.GONE);
                                                    }
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