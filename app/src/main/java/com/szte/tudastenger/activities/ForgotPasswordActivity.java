package com.szte.tudastenger.activities;


import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.szte.tudastenger.databinding.ActivityForgotPasswordBinding;
import com.szte.tudastenger.viewmodels.ForgotPasswordViewModel;

public class ForgotPasswordActivity extends DrawerBaseActivity {
    private ActivityForgotPasswordBinding binding;
    private ForgotPasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        binding.resetPasswordButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString();
            viewModel.resetPassword(email);
        });

        binding.logRedirect.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void observeViewModel() {
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                if (error.contains("Add meg az e-mail címedet!")) {
                    binding.emailEditText.setError(error);
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getResetSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Az e-mailt elküldtük a megadott e-mail címre!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });
    }
}
