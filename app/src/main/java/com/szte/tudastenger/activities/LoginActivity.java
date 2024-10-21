package com.szte.tudastenger.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityLoginBinding;
import com.szte.tudastenger.models.User;

public class LoginActivity extends DrawerBaseActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private TextView forgotPasswordTextView;

    private TextView regRedirectTextView;
    private EditText emailEditText;

    private EditText passwordEditText;

    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private ActivityLoginBinding activityLoginBinding;
    private SignInButton loginWithGoogleButton;
    private GoogleSignInClient googleSignInClient;

    private FirebaseFirestore mFirestore;
    private CollectionReference mUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityLoginBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(activityLoginBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("388914753240-3qctbu9dt09cigfnqj5e8jecs4u6oss2.apps.googleusercontent.com")
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(LoginActivity.this, googleSignInOptions);

        loginWithGoogleButton = findViewById(R.id.loginWithGoogleButton);

        loginWithGoogleButton.setOnClickListener(view -> {
            Intent intent = googleSignInClient.getSignInIntent();
            launcher.launch(intent);
        });

        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        regRedirectTextView = findViewById(R.id.regRedirectTextView);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);



        regRedirectTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        });

        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private ActivityResultLauncher<Intent> launcher
            = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),

            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);

                    if (signInAccountTask.isSuccessful()) {
                        try {
                            GoogleSignInAccount googleSignInAccount = signInAccountTask.getResult(ApiException.class);

                            if (googleSignInAccount != null) {
                                AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);

                                mAuth.signInWithCredential(authCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            // sikeres bejelentkezés esetén
                                            FirebaseUser firebaseUser = mAuth.getCurrentUser();

                                            if (firebaseUser != null) {
                                                String uid = firebaseUser.getUid();
                                                String email = firebaseUser.getEmail();
                                                String displayName = googleSignInAccount.getDisplayName();

                                                // először lépett-e be a google fiókjával vagy már korábban is
                                                mUsers.document(uid).get().addOnCompleteListener(userTask -> {
                                                    if (userTask.isSuccessful()) {
                                                        if (!userTask.getResult().exists()) {
                                                            // ha először, adjuk hozzá a Users táblához
                                                            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(fcmTask -> {
                                                                if (fcmTask.isSuccessful()) {
                                                                    String fcmToken = fcmTask.getResult();

                                                                    User user = new User(uid, displayName, email, fcmToken);

                                                                    mUsers.document(uid).set(user)
                                                                            .addOnSuccessListener(documentReference -> {
                                                                                startActivity(new Intent(LoginActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                                                            });
                                                                }
                                                            });
                                                        } else {
                                                            // ha már máskor is belépett, átirányítjuk
                                                            startActivity(new Intent(LoginActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                                        }
                                                    }
                                                });
                                            }
                                        } else {
                                            displayToast("Hiba a bejelentkezés során: " + task.getException().getMessage());
                                        }
                                    }
                                });
                            }
                        } catch (ApiException e) {
                            e.printStackTrace();
                            displayToast("Hiba a bejelentkezés során: " + e.getMessage());
                        }
                    }
                }
            }
    );

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    public void login(View view) {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Ellenőrzés, hogy az email üres-e
        if (email.isEmpty()) {
            emailEditText.setError("Az email mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy az email formátum helyes-e
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Hibás email formátum");
            return;
        }

        // Ellenőrzés, hogy a jelszó üres-e
        if (password.isEmpty()) {
            passwordEditText.setError("A jelszó mező nem lehet üres");
            return;
        }


        // Bejelentkezés
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else{
                    Toast.makeText(LoginActivity.this, "Sikertelen belépés!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}