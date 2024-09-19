package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityForgotPasswordBinding;
import com.szte.tudastenger.databinding.ActivityProfileBinding;

public class ForgotPasswordActivity extends DrawerBaseActivity {
    private ActivityForgotPasswordBinding activityForgotPasswordBinding;
    private FirebaseAuth mAuth;
    private EditText emailEditText;
    private Button resetPasswordButton;
    private TextView logRedirect;
    private ProgressBar forgetPasswordProgressbar;
    private String emailAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityForgotPasswordBinding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(activityForgotPasswordBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        logRedirect = findViewById(R.id.logRedirect);
        forgetPasswordProgressbar = findViewById(R.id.forgetPasswordProgressbar);

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailAddress = emailEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(emailAddress)) {
                    resetPassword();
                } else {
                    emailEditText.setError("Add meg az e-mail címedet!");
                }
            }
        });

        logRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void resetPassword() {
        forgetPasswordProgressbar.setVisibility(View.VISIBLE);
        resetPasswordButton.setVisibility(View.INVISIBLE);

        mAuth.sendPasswordResetEmail(emailAddress)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ForgotPasswordActivity.this, "Az e-mailt elküldtük a megadott e-mail címre!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        forgetPasswordProgressbar.setVisibility(View.INVISIBLE);
                        resetPasswordButton.setVisibility(View.VISIBLE);
                    }
                });
    }
}