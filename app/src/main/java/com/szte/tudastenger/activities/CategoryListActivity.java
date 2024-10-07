package com.szte.tudastenger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.SearchView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.CategoryAdapter;
import com.szte.tudastenger.adapters.CategoryEditAdapter;
import com.szte.tudastenger.adapters.QuestionAdapter;
import com.szte.tudastenger.databinding.ActivityCategoryListBinding;
import com.szte.tudastenger.databinding.ActivityQuestionListBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;

public class CategoryListActivity extends AppCompatActivity {
    private ActivityCategoryListBinding activityCategoryListBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private ArrayList<Category> mCategoriesData;
    private RecyclerView mRecyclerView;
    private CategoryEditAdapter mAdapter;
    private CollectionReference mCategories;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCategoryListBinding = ActivityCategoryListBinding.inflate(getLayoutInflater());
        setContentView(activityCategoryListBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        searchView = findViewById(R.id.searchView);

        mFirestore = FirebaseFirestore.getInstance();
        mCategories = mFirestore.collection("Categories");

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        mCategoriesData = new ArrayList<>();
        mAdapter = new CategoryEditAdapter(this, mCategoriesData);
        mRecyclerView.setAdapter(mAdapter);

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
        mCategoriesData.clear();

        mCategories.orderBy("name").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Category category = document.toObject(Category.class);
                mCategoriesData.add(category);
            }
            mAdapter.notifyDataSetChanged();
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
                filterCategories();
                return false;
            }
        });
    }

    private void filterCategories() {
        String searchText = searchView.getQuery().toString().toLowerCase();
        mAdapter.getFilter().filter(searchText);
    }

}