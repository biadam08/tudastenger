package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

public class RegistrationActivity extends DrawerBaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private FirebaseFirestore mFirestore;

    private CollectionReference mUsers;

    private TextView loginRedirectText;
    private EditText usernameEditText;
    private TextView emailEditText;
    private TextView passwordEditText;
    private TextView passwordAgainEditText;

    private SharedPreferences sharedPreferences;
    private ActivityRegistrationBinding activityRegistrationBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityRegistrationBinding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(activityRegistrationBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");


        loginRedirectText = findViewById(R.id.loginRedirect);

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        passwordAgainEditText = findViewById(R.id.passwordAgainEditText);

    }

    public void registration(View view) {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String passwordAgain = passwordAgainEditText.getText().toString().trim();

        // Ellenőrzés, hogy a felhasználónév mező nem üres-e
        if (username.isEmpty()) {
            usernameEditText.setError("A felhasználónév mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy az email mező nem üres-e
        if (email.isEmpty()) {
            emailEditText.setError("Az email mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy az email formátum helyes-e
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Hibás email formátum");
            return;
        }

        // Ellenőrzés, hogy a jelszó mező nem üres-e
        if (password.isEmpty()) {
            passwordEditText.setError("A jelszó mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy a megerősítő jelszó mező nem üres-e
        if (passwordAgain.isEmpty()) {
            passwordAgainEditText.setError("A megerősítő jelszó mező nem lehet üres");
            return;
        }

        // Ellenőrzés, hogy a két jelszó megegyezik-e
        if (!password.equals(passwordAgain)) {
            passwordAgainEditText.setError("A két jelszó nem egyezik");
            return;
        }

        // Firebase regisztráció
        mUsers.whereEqualTo("username", username).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if(!task.getResult().isEmpty()){
                        showDialog("Sikertelen regisztráció", "A megadott felhasználónévvel már létezik felhasználó");
                    }  else {
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            String uid = task.getResult().getUser().getUid();
                                            FirebaseMessaging.getInstance().getToken()
                                                    .addOnCompleteListener(new OnCompleteListener<String>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<String> task) {
                                                            if (!task.isSuccessful()) {
                                                                Toast.makeText(RegistrationActivity.this, "FCM token lekérése sikertelen!", Toast.LENGTH_SHORT).show();
                                                                return;
                                                            }

                                                            String fcmToken = task.getResult();
                                                            User user = new User(uid, username, email, fcmToken);

                                                            mUsers.document(uid).set(user)
                                                                    .addOnSuccessListener(documentReference -> {
                                                                        Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                                                                        startActivity(intent);
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(RegistrationActivity.this, "Sikertelen regisztráció!", Toast.LENGTH_SHORT).show();
                                                                    });
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(RegistrationActivity.this, "Regisztráció sikertelen!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }
            }
        });
    }

    private void showDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }
}