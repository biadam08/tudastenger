package com.szte.tudastenger.activities;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import android.os.Bundle;
import android.widget.SearchView;
import android.widget.Toast;

import com.szte.tudastenger.adapters.CategoryEditAdapter;
import com.szte.tudastenger.adapters.RankEditAdapter;
import com.szte.tudastenger.databinding.ActivityCategoryListBinding;
import com.szte.tudastenger.databinding.ActivityRankListBinding;
import com.szte.tudastenger.viewmodels.CategoryListViewModel;
import com.szte.tudastenger.viewmodels.RankListViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RankListActivity extends DrawerBaseActivity {
    private ActivityRankListBinding binding;
    private RankListViewModel viewModel;
    private RankEditAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRankListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RankListViewModel.class);

        setupViews();
        setupObservers();

        viewModel.checkAdmin();
        viewModel.loadCategories();
    }

    private void setupViews() {
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
    }

    private void setupObservers() {
        viewModel.getIsAdmin().observe(this, isAdmin -> {
            if (!isAdmin) {
                finish();
            }
        });

        viewModel.getRanksData().observe(this, ranks -> {
            adapter = new RankEditAdapter(this, new ArrayList<>(ranks));
            binding.recyclerView.setAdapter(adapter);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
