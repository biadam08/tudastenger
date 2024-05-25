package com.szte.tudastenger.activities;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.CategoryProfileAdapter;
import com.szte.tudastenger.databinding.ActivityProfileBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.User;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.UUID;


public class ProfileActivity extends DrawerBaseActivity {

    private ActivityProfileBinding activityProfileBinding;

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private CollectionReference mUsers;
    private FirebaseUser user;

    private User currentUser;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private StorageReference storageReference2;

    private ImageView profilePicture;
    private Uri uri;

    private TextView userName;
    private CollectionReference mCategories;

    private ArrayList<Category> mCategoriesData;
    private RecyclerView mRecyclerView;

    private CategoryProfileAdapter mAdapter;

    private LinearLayout bookmarkButton;
    private LinearLayout settingsButton;
    private LinearLayout rankingButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityProfileBinding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(activityProfileBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
        }

        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        storageReference2 = storage.getReference();

        profilePicture = findViewById(R.id.profilePicture);

        mUsers.whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                currentUser = doc.toObject(User.class);
                userName = findViewById(R.id.userNameTextView);
                userName.setText(currentUser.getUsername());

                initializeButtons();
                displayCategories();
                displayProfilePicture();
                setProfilePicture();
            }
        });
    }

    private void initializeButtons() {
        bookmarkButton = findViewById(R.id.bookmarkLinearLayout);
        settingsButton = findViewById(R.id.settingsLinearLayout);
        rankingButton = findViewById(R.id.rankLinearLayout);

        bookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, SavedQuestionsActivity.class);
                startActivity(intent);
            }
        });

       /*
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        rankingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, RankingActivity.class);
                startActivity(intent);
            }
        });
        */
    }

    private void displayCategories() {
        mCategories = mFirestore.collection("Categories");


        mRecyclerView = findViewById(R.id.recyclerView);

        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        mCategoriesData = new ArrayList<>();
        mAdapter = new CategoryProfileAdapter(this, mCategoriesData, currentUser);
        mRecyclerView.setAdapter(mAdapter);

        queryData();
    }

    private void queryData() {
        mCategoriesData.clear();

        mCategories.orderBy("name").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                Category category = document.toObject(Category.class);
                mCategoriesData.add(category);
            }

            mAdapter.notifyDataSetChanged();
        });

    }

    private void setProfilePicture() {
        profilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            if(CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)){
                uri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
            } else {
                startCrop(imageUri);
            }
        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK) {
                uploadProfilePicture(result.getUri());
            }
        }
    }

    private void startCrop(Uri imageUri) {
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMultiTouchEnabled(true)
                .start(this);
    }



    private void uploadProfilePicture(Uri imageUri) {
        if (imageUri != null) {

            ProgressDialog progressDialog  = new ProgressDialog(this);
            progressDialog.setTitle("Feltöltés...");
            progressDialog.show();

            String fileName = UUID.randomUUID().toString();
            StorageReference ref = storageReference.child("profile-pictures/" + fileName);
            Log.d("USERNAME PROFILE", imageUri.toString());

            ref.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override public void onSuccess(UploadTask.TaskSnapshot taskSnapshot){
                                progressDialog.dismiss();
                                DocumentReference userDocRef = mUsers.document(user.getUid());
                                userDocRef.update("profilePicture", fileName);
                                profilePicture.setImageURI(imageUri);
                            }
                        })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            // Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast
                                    .makeText(ProfileActivity.this,
                                            "Hiba a feltöltés során",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {

                                // Progress Listener for loading
                                // percentage on the dialog box
                                @Override
                                public void onProgress(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    double progress
                                            = (100.0
                                            * taskSnapshot.getBytesTransferred()
                                            / taskSnapshot.getTotalByteCount());
                                    progressDialog.setMessage(
                                            "Feltöltve: "
                                                    + (int)progress + "%");

                                }
                            });
        }
    }

    private void displayProfilePicture() {
        ProgressDialog progressDialog  = new ProgressDialog(this);
        progressDialog.setTitle("Kép betöltése...");
        progressDialog.show();

        String imagePath;

        if(!currentUser.getProfilePicture().equals("")) {
            imagePath = "profile-pictures/" + currentUser.getProfilePicture();
        } else{
            imagePath = "profile-pictures/default.jpg";
        }

        storageReference2 = FirebaseStorage.getInstance().getReference().child(imagePath);

        storageReference2.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri.toString())
                    .into(profilePicture);
        }).addOnFailureListener(exception -> {

        });

        progressDialog.dismiss();
    }
}