package com.szte.tudastenger.activities;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.CategoryAdapter;
import com.szte.tudastenger.databinding.ActivityMainBinding;
import com.szte.tudastenger.models.Category;

import java.util.ArrayList;

public class MainActivity extends DrawerBaseActivity {
    private static final String LOG_TAG = MainActivity.class.getName();
    private ActivityMainBinding activityMainBinding;

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private CollectionReference mCategories;

    private ArrayList<Category> mCategoriesData;

    private RecyclerView mRecyclerView;

    private CategoryAdapter mAdapter;
    private Button startMixedGameButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        mFirestore = FirebaseFirestore.getInstance();
        mCategories = mFirestore.collection("Categories");

        startMixedGameButton = findViewById(R.id.startMixedGameButton);
        startMixedGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, QuizGameActivity.class);
                startActivity(intent);
            }
        });

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        mCategoriesData = new ArrayList<>();
        mAdapter = new CategoryAdapter(this, mCategoriesData);
        mRecyclerView.setAdapter(mAdapter);

        queryData();
    }

    private void queryData() {
        mCategoriesData.clear();

        mCategories.orderBy("name").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                Category category = document.toObject(Category.class);
                mCategoriesData.add(category);
            }

            mAdapter.notifyDataSetChanged();
        });

    }
}