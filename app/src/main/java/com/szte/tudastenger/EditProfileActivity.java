package com.szte.tudastenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.activities.DrawerBaseActivity;
import com.szte.tudastenger.databinding.ActivityEditProfileBinding;
import com.szte.tudastenger.databinding.ActivityMainBinding;

public class EditProfileActivity extends DrawerBaseActivity {
    private ActivityEditProfileBinding activityEditProfileBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityEditProfileBinding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(activityEditProfileBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        mFirestore = FirebaseFirestore.getInstance();

    }
}