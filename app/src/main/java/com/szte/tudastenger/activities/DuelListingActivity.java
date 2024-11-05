package com.szte.tudastenger.activities;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import android.os.Bundle;
import android.view.View;

import com.szte.tudastenger.adapters.DuelAdapter;
import com.szte.tudastenger.databinding.ActivityDuelListingBinding;
import com.szte.tudastenger.viewmodels.DuelListViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DuelListingActivity extends DrawerBaseActivity {
    private ActivityDuelListingBinding binding;
    private DuelListViewModel viewModel;
    private DuelAdapter pendingDuelsAdapter;
    private DuelAdapter finishedDuelsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDuelListingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(DuelListViewModel.class);

        setupRecyclerViews();
        observeViewModel();
    }

    private void setupRecyclerViews() {
        binding.pendingDuelListRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.finishedDuelListRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void observeViewModel() {
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                pendingDuelsAdapter = new DuelAdapter(this, new ArrayList<>(), user.getId(), viewModel);
                finishedDuelsAdapter = new DuelAdapter(this, new ArrayList<>(), user.getId(), viewModel);

                binding.pendingDuelListRecyclerView.setAdapter(pendingDuelsAdapter);
                binding.finishedDuelListRecyclerView.setAdapter(finishedDuelsAdapter);
            }
        });

        viewModel.getPendingDuels().observe(this, duels -> {
            if (pendingDuelsAdapter != null) {
                pendingDuelsAdapter.updateData(duels);
            }
            if (duels.size() == 0) {
                binding.noPendingDuelsTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getFinishedDuels().observe(this, duels -> {
            if (finishedDuelsAdapter != null) {
                finishedDuelsAdapter.updateData(duels);
            }
            if (duels.size() == 0) {
                binding.noFinishedDuelsTextView.setVisibility(View.VISIBLE);
            }
        });
    }
}
