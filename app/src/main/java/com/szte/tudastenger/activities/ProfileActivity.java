package com.szte.tudastenger.activities;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.szte.tudastenger.adapters.CategoryProfileAdapter;
import com.szte.tudastenger.databinding.ActivityProfileBinding;
import com.szte.tudastenger.viewmodels.ProfileViewModel;

import java.util.ArrayList;


public class ProfileActivity extends DrawerBaseActivity {
    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;
    private CategoryProfileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupViews();
        setupObservers();

        viewModel.loadUserData();
    }

    private void setupViews() {
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        adapter = new CategoryProfileAdapter(this, new ArrayList<>());
        binding.recyclerView.setAdapter(adapter);

        binding.settingsImageView.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        binding.bookmarkLinearLayout.setOnClickListener(v ->
                startActivity(new Intent(this, SavedQuestionsActivity.class)));

        binding.friendsLinearLayout.setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));

        binding.leaderboardLinearLayout.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));
    }

    private void setupObservers() {
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                binding.userNameTextView.setText(user.getUsername());
            }
        });

        viewModel.getCategories().observe(this, categories -> {
            adapter = new CategoryProfileAdapter(this, new ArrayList<>(categories));
            binding.recyclerView.setAdapter(adapter);
        });

        viewModel.getCategoryScores().observe(this, scores -> {
            if (adapter != null) {
                adapter.setCategoryScores(scores);
            }
        });

        viewModel.getProfilePictureUrl().observe(this, url -> {
            if (url != null) {
                Glide.with(this)
                        .load(url)
                        .into(binding.profilePicture);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
