package com.szte.tudastenger.activities;

import androidx.lifecycle.ViewModelProvider;
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
import com.szte.tudastenger.viewmodels.SavedQuestionsViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SavedQuestionsActivity extends DrawerBaseActivity {
    private SavedQuestionsViewModel viewModel;
    private ActivitySavedQuestionsBinding binding;
    private SavedQuestionAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SavedQuestionsViewModel.class);

        setupRecyclerView();
        setupSpinner();
        observeViewModel();
    }

    private void setupRecyclerView() {
        mAdapter = new SavedQuestionAdapter(this, new ArrayList<>(), "", viewModel);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        binding.recyclerView.setAdapter(mAdapter);
    }

    private void setupSpinner() {
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Category selectedCategory = (Category) parent.getItemAtPosition(position);
                viewModel.onCategorySelected(selectedCategory.getName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void observeViewModel() {
        viewModel.getCategories().observe(this, categories -> {
            ArrayAdapter<Category> spinnerAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categories);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinner.setAdapter(spinnerAdapter);
        });

        viewModel.getQuestions().observe(this, questions -> {
            mAdapter.updateData(questions);
        });

        viewModel.getShowNoQuestions().observe(this, showNoQuestions -> {
            binding.noSavedQuestion.setVisibility(showNoQuestions ? View.VISIBLE : View.GONE);
        });

        viewModel.getCurrentUserId().observe(this, userId -> {
            mAdapter = new SavedQuestionAdapter(this, new ArrayList<>(), userId, viewModel);
            binding.recyclerView.setAdapter(mAdapter);
        });
    }
}
