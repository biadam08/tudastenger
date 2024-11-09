package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.R;

public class DrawerBaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private FirebaseFirestore mFirestore;

    private CollectionReference mUsers;

    private String currentUserRole;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setContentView(View view) {
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_drawer_base, null);
        FrameLayout container = drawerLayout.findViewById(R.id.activityContainer);
        container.addView(view);
        super.setContentView(drawerLayout);

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        Toolbar toolbar = drawerLayout.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setHomeButtonEnabled(true);

        Menu menu = navigationView.getMenu();

        menu.findItem(R.id.nav_admin).setVisible(false);

        if(user == null) {
            menu.findItem(R.id.nav_home).setVisible(false);
            menu.findItem(R.id.nav_newgame).setVisible(false);
            menu.findItem(R.id.nav_logout).setVisible(false);
            menu.findItem(R.id.nav_profile).setVisible(false);
            menu.findItem(R.id.nav_history).setVisible(false);
            menu.findItem(R.id.nav_leaderboard).setVisible(false);
            menu.findItem(R.id.nav_duels).setVisible(false);
            menu.findItem(R.id.nav_friends).setVisible(false);
            menu.findItem(R.id.nav_saved_questions).setVisible(false);
        } else {
            menu.findItem(R.id.nav_login).setVisible(false);
            menu.findItem(R.id.nav_registration).setVisible(false);


            String email = user.getEmail();
            mUsers = mFirestore.collection("Users");
            mFirestore.collection("Users").whereEqualTo("email", email).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                if (document.exists()) {
                                    currentUserRole = document.getString("role");
                                    if(currentUserRole.equals("admin")){
                                        menu.findItem(R.id.nav_admin).setVisible(true);
                                    }
                                }
                            }
                        }
                    });
        }

    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();

        if(itemId == R.id.nav_home) {
            Intent intent = new Intent(DrawerBaseActivity.this, MainActivity.class);
            startActivity(intent);
        }
        if(itemId == R.id.nav_newgame) {
            Intent intent = new Intent(DrawerBaseActivity.this, QuizGameActivity.class);
            intent.putExtra("mixed", "true");
            startActivity(intent);
        }
        if(itemId == R.id.nav_challenge) {
            Intent intent = new Intent(DrawerBaseActivity.this, ChallengeActivity.class);
            startActivity(intent);
        }
        if(itemId == R.id.nav_login){
            startActivity(new Intent(DrawerBaseActivity.this, LoginActivity.class));
        }
        if(itemId == R.id.nav_profile){
            startActivity(new Intent(DrawerBaseActivity.this, ProfileActivity.class));
        }
        if(itemId == R.id.nav_friends){
            startActivity(new Intent(DrawerBaseActivity.this, FriendsActivity.class));
        }
        if(itemId == R.id.nav_duels){
            startActivity(new Intent(DrawerBaseActivity.this, DuelListingActivity.class));
        }
        if(itemId == R.id.nav_history){
            startActivity(new Intent(DrawerBaseActivity.this, HistoryActivity.class));
        }
        if(itemId == R.id.nav_saved_questions){
            startActivity(new Intent(DrawerBaseActivity.this, SavedQuestionsActivity.class));
        }
        if(itemId == R.id.nav_registration){
            startActivity(new Intent(DrawerBaseActivity.this, RegistrationActivity.class));
        }

        if(itemId == R.id.nav_leaderboard){
            startActivity(new Intent(DrawerBaseActivity.this, LeaderboardActivity.class));
        }

        if(itemId == R.id.nav_add_a_category){
            if(user != null && currentUserRole.equals("admin")) {
                startActivity(new Intent(DrawerBaseActivity.this, CategoryUploadActivity.class));
            }
        }
        if(itemId == R.id.nav_add_a_question){
            if(user != null && currentUserRole.equals("admin")) {
                startActivity(new Intent(DrawerBaseActivity.this, QuestionEditUploadActivity.class));
            }
        }
        if(itemId == R.id.nav_list_questions){
            if(user != null && currentUserRole.equals("admin")) {
                startActivity(new Intent(DrawerBaseActivity.this, QuestionListActivity.class));
            }
        }
        if(itemId == R.id.nav_list_categories){
            if(user != null && currentUserRole.equals("admin")) {
                startActivity(new Intent(DrawerBaseActivity.this, CategoryListActivity.class));
            }
        }
        if(itemId == R.id.nav_add_a_rank){
            if(user != null && currentUserRole.equals("admin")) {
                startActivity(new Intent(DrawerBaseActivity.this, RankEditUploadActivity.class));
            }
        }
        if(itemId == R.id.nav_list_ranks){
            if(user != null && currentUserRole.equals("admin")) {
                startActivity(new Intent(DrawerBaseActivity.this, RankListActivity.class));
            }
        }

        if(itemId == R.id.nav_logout){
            FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if (firebaseAuth.getCurrentUser() == null){
                        user = null;
                        startActivity(new Intent(DrawerBaseActivity.this, LoginActivity.class));
                    }
                }
            };
            mAuth.addAuthStateListener(authStateListener);
            mAuth.signOut();
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}