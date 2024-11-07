package com.szte.tudastenger.activities;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.szte.tudastenger.databinding.ActivityRankEditUploadBinding;
import com.szte.tudastenger.viewmodels.RankEditUploadViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RankEditUploadActivity extends DrawerBaseActivity {
    private ActivityRankEditUploadBinding binding;
    private RankEditUploadViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRankEditUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RankEditUploadViewModel.class);

        viewModel.checkAdmin();

        String rankId = getIntent().getStringExtra("rankId");

        viewModel.init(rankId);

        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        binding.backButton.setOnClickListener(v ->
                startActivity(new Intent(this, RankListActivity.class)));

        binding.deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        binding.addRankButton.setOnClickListener(v ->
                viewModel.uploadRank(binding.rankName.getText().toString(), binding.threshold.getText().toString()));
    }

    private void observeViewModel() {
        viewModel.getIsAdmin().observe(this, isAdmin -> {
            if (!isAdmin) {
                finish();
            }
        });

        viewModel.getRankData().observe(this, rank -> {
            if (rank != null) {
                binding.addRankTextView.setText("Rang szerkesztése");
                binding.addRankButton.setText("Módosítás");
                binding.editBarLinearLayout.setVisibility(View.VISIBLE);
                binding.rankName.setText(rank.getRankName());
                binding.threshold.setText(rank.getThreshold().toString());
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                showErrorDialog("Hiba", message);
            }
        });

        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null) {
                showSuccessDialog("Sikeres módosítás", message);
            }
        });
    }

   private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Rang törlése")
                .setMessage("Biztosan törölni szeretnéd ezt a rangot?")
                .setPositiveButton("Igen", (dialog, which) -> viewModel.deleteRank())
                .setNegativeButton("Nem", null)
                .show();
    }


    private void showSuccessDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", (dialog, which) -> {
                    Intent intent = new Intent(RankEditUploadActivity.this, RankListActivity.class);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void showErrorDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}