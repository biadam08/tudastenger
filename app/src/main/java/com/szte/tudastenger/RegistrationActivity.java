package com.szte.tudastenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.databinding.ActivityMainBinding;
import com.szte.tudastenger.databinding.ActivityRegistrationBinding;

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
        String username = usernameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String passwordAgain = passwordAgainEditText.getText().toString();

        if(!password.equals(passwordAgain)){
            Toast.makeText(RegistrationActivity.this, "Nem egyezik a két jelszó!", Toast.LENGTH_SHORT).show();
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    String uid = task.getResult().getUser().getUid();
                    User user = new User(uid, username, email);

                    mUsers.document(uid).set(user)
                            .addOnSuccessListener(documentReference -> {
                                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                                startActivity(intent);
                                Toast.makeText(RegistrationActivity.this, "Sikeres!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RegistrationActivity.this, "Sikertelen1!", Toast.LENGTH_SHORT).show();
                            });
                } else{
                    Toast.makeText(RegistrationActivity.this, "Sikertelen2!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}