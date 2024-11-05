package com.szte.tudastenger.activities;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Toast;

import com.szte.tudastenger.adapters.QuestionAdapter;
import com.szte.tudastenger.databinding.ActivityQuestionListBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.viewmodels.QuestionListViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuestionListActivity extends DrawerBaseActivity {
    private ActivityQuestionListBinding binding;
    private QuestionListViewModel viewModel;
    private QuestionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(QuestionListViewModel.class);

        setupViews();
        setupObservers();

        viewModel.checkAdminAndLoadData();
    }

    private void setupViews() {
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        adapter = new QuestionAdapter(this, new ArrayList<>());
        binding.recyclerView.setAdapter(adapter);

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

        binding.categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterQuestions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupObservers() {
        viewModel.getIsAdmin().observe(this, isAdmin -> {
            if (!isAdmin) {
                finish();
            }
        });

        viewModel.getQuestions().observe(this, questions -> {
            adapter = new QuestionAdapter(this, new ArrayList<>(questions));
            binding.recyclerView.setAdapter(adapter);
        });

        viewModel.getCategories().observe(this, categories -> {
            ArrayAdapter<Category> spinnerAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categories);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.categorySpinner.setAdapter(spinnerAdapter);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterQuestions() {
        String selectedCategory = binding.categorySpinner.getSelectedItem().toString();
        String searchText = binding.searchView.getQuery().toString().toLowerCase();

        if (selectedCategory.equals("Összes kategória")) {
            adapter.setFilterCategory("Összes kategória");
        } else {
            String categoryId = viewModel.getCategoryIdByName(selectedCategory);
            adapter.setFilterCategory(categoryId);
        }
        adapter.getFilter().filter(searchText);
    }
}