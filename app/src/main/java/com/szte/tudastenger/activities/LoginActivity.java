package com.szte.tudastenger.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityLoginBinding;
import com.szte.tudastenger.viewmodels.LoginViewModel;

import java.util.concurrent.Executor;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends DrawerBaseActivity {
    private LoginViewModel viewModel;
    private ActivityLoginBinding binding;
    private GoogleSignInClient googleSignInClient;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                            viewModel.handleGoogleSignIn(credential);
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Sign In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        setupBiometric();
        viewModel.checkBiometricAvailability(this);
        setupGoogleSignIn();
        initializeViews();
        observeViewModel();
        viewModel.checkCurrentUser();
    }

    private void setupBiometric(){
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(LoginActivity.this, "Azonosítási hiba: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                viewModel.loadSavedCredentials();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginActivity.this,"Sikertelen azonosítás", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometrikus bejelentkezés")
                .setSubtitle("Jelentkezz be ujjlenyomattal")
                .setNegativeButtonText("Inkább jelszót használok")
                .build();


        Button biometricLoginButton = findViewById(R.id.biometricLoginButton);
        biometricLoginButton.setOnClickListener(view -> {
            biometricPrompt.authenticate(promptInfo);
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("388914753240-3qctbu9dt09cigfnqj5e8jecs4u6oss2.apps.googleusercontent.com")
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
    }

    private void initializeViews() {
        binding.regRedirectTextView.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            finish();
        });

        binding.forgotPasswordTextView.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            finish();
        });

        binding.loginWithGoogleButton.setOnClickListener(view -> {
            googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
        });

        binding.loginButton.setOnClickListener(view -> {
            String email = binding.emailEditText.getText().toString();
            String password = binding.loginPasswordEditText.getText().toString();
            viewModel.login(email, password);
        });
    }

    private void observeViewModel() {
        binding.biometricLoginButton.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));

        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoginSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });

        viewModel.getCanUseBiometric().observe(this, canUse -> {
            binding.biometricLoginButton.setVisibility(canUse ? View.VISIBLE : View.GONE);
        });

        viewModel.getSavedCredentials().observe(this, credentials -> {
            if (credentials != null) {
                String email = credentials.first;
                String password = credentials.second;
                viewModel.login(email, password);
            }
        });
    }
}
