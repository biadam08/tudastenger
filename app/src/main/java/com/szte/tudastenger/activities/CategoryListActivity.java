package com.szte.tudastenger.activities;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import android.os.Bundle;
import android.widget.SearchView;
import android.widget.Toast;

import com.szte.tudastenger.adapters.CategoryEditAdapter;
import com.szte.tudastenger.databinding.ActivityCategoryListBinding;
import com.szte.tudastenger.viewmodels.CategoryListViewModel;

import java.util.ArrayList;

public class CategoryListActivity extends DrawerBaseActivity {
    private ActivityCategoryListBinding binding;
    private CategoryListViewModel viewModel;
    private CategoryEditAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CategoryListViewModel.class);

        setupViews();
        setupObservers();

        viewModel.checkAdmin();
        viewModel.loadCategories();
    }

    private void setupViews() {
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (adapter != null) {
                    adapter.getFilter().filter(s);
                }
                return false;
            }
        });
    }

    private void setupObservers() {
        viewModel.getIsAdmin().observe(this, isAdmin -> {
            if (!isAdmin) {
                finish();
            }
        });

        viewModel.getCategoriesData().observe(this, categories -> {
            adapter = new CategoryEditAdapter(this, new ArrayList<>(categories));
            binding.recyclerView.setAdapter(adapter);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
