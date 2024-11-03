package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityRegistrationBinding;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.viewmodels.RegistrationViewModel;

public class RegistrationActivity extends DrawerBaseActivity {
    private RegistrationViewModel viewModel;
    private ActivityRegistrationBinding binding;
    private EditText usernameEditText;
    private TextView emailEditText, passwordEditText, passwordAgainEditText;
    private TextView loginRedirectText;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        viewModel = new ViewModelProvider(this).get(RegistrationViewModel.class);

        initializeViews();
        observeViewModel();
    }

    private void initializeViews() {
        loginRedirectText = findViewById(R.id.loginRedirect);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        passwordAgainEditText = findViewById(R.id.passwordAgainEditText);

        loginRedirectText.setOnClickListener(view -> {
            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                showDialog("Sikertelen regisztráció", errorMessage);
            }
        });

        viewModel.getRegistrationSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void registration(View view) {
        String username = usernameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String passwordAgain = passwordAgainEditText.getText().toString();

        viewModel.register(username, email, password, passwordAgain);

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "Registration");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }
}

