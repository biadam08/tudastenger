package com.szte.tudastenger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.CategoryProfileAdapter;
import com.szte.tudastenger.adapters.QuestionAdapter;
import com.szte.tudastenger.databinding.ActivityMainBinding;
import com.szte.tudastenger.databinding.ActivityQuestionListBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;

public class QuestionListActivity extends DrawerBaseActivity {
    private ActivityQuestionListBinding activityQuestionListBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private boolean isAdmin;
    private CollectionReference mQuestions;

    private ArrayList<Question> mQuestionsData;
    private RecyclerView mRecyclerView;

    private QuestionAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityQuestionListBinding = ActivityQuestionListBinding.inflate(getLayoutInflater());
        setContentView(activityQuestionListBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getEmail() != null) {
            DocumentReference userDocRef = mFirestore.collection("Users").document(user.getUid());

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String role = documentSnapshot.getString("role");
                    if(role.equals("admin")){
                        isAdmin = true;
                        listQuestions();
                    } else{
                        finish();
                    }
                }
            });
        }
    }

    private void listQuestions() {
        mQuestions = mFirestore.collection("Questions");

        mRecyclerView = findViewById(R.id.recyclerView);

        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        mQuestionsData = new ArrayList<>();
        mAdapter = new QuestionAdapter(this, mQuestionsData);
        mRecyclerView.setAdapter(mAdapter);

        queryData();
    }

    private void queryData() {
        mQuestionsData.clear();

        mQuestions.orderBy("questionText").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                Question question = document.toObject(Question.class);
                mQuestionsData.add(question);
                Log.d("QUESTIONACT", question.getQuestionText());
            }

            mAdapter.notifyDataSetChanged();
        });
    }
}