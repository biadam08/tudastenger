package com.szte.tudastenger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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
    private SearchView searchView;
    private Spinner categorySpinner;
    private ArrayList<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityQuestionListBinding = ActivityQuestionListBinding.inflate(getLayoutInflater());
        setContentView(activityQuestionListBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        searchView = findViewById(R.id.searchView);
        categorySpinner = findViewById(R.id.categorySpinner);

        mQuestions = mFirestore.collection("Questions");

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        mQuestionsData = new ArrayList<>();
        mAdapter = new QuestionAdapter(this, mQuestionsData);
        mRecyclerView.setAdapter(mAdapter);


        initCategorySpinner();
        initSearchFunctionality();


        if (user != null && user.getEmail() != null) {
            DocumentReference userDocRef = mFirestore.collection("Users").document(user.getUid());
            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.getString("role").equals("admin")) {
                    queryData();
                } else {
                    finish();
                }
            });
        }
    }

    private void queryData() {
        mQuestionsData.clear();

        mQuestions.orderBy("questionText").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Question question = document.toObject(Question.class);
                mQuestionsData.add(question);
            }
            mAdapter.notifyDataSetChanged();
        });
    }

    private void filterQuestions() {
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        Log.d("SelectedCat", selectedCategory);
        String searchText = searchView.getQuery().toString().toLowerCase();

        if(selectedCategory.equals("Összes kategória")){
            mAdapter.setFilterCategory("Összes kategória");
            mAdapter.getFilter().filter(searchText);
        } else {
            mFirestore.collection("Categories")
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                String categoryName = document.getString("name");
                                if (categoryName != null && categoryName.equals(selectedCategory)) {
                                    String categoryId = document.getId();
                                    mAdapter.setFilterCategory(categoryId);
                                    mAdapter.getFilter().filter(searchText);
                                    break;
                                }
                            }
                        }
                    });
            mAdapter.getFilter().filter(searchText);
        }
    }

    private void initCategorySpinner() {
        categories = new ArrayList<>();
        Category allCategories = new Category("0", "Összes kategória", null);
        categories.add(allCategories);

        mFirestore.collection("Categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                Category category = documentSnapshot.toObject(Category.class);
                category.setId(documentSnapshot.getId());
                categories.add(category);
            }

            ArrayAdapter<Category> adapter = new ArrayAdapter<>(QuestionListActivity.this, android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(adapter);
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterQuestions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initSearchFunctionality() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                filterQuestions();
                return false;
            }
        });
    }
}