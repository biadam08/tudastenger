package com.szte.tudastenger.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityEditProfileBinding;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.UUID;

public class EditProfileActivity extends DrawerBaseActivity {

    private ActivityEditProfileBinding activityEditProfileBinding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button changePasswordButton;
    private ImageView profilePicture;
    private TextView userNameTextView;

    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityEditProfileBinding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(activityEditProfileBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        profilePicture = findViewById(R.id.profilePicture);
        userNameTextView = findViewById(R.id.userNameTextView);

        loadUserProfile();

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = newPasswordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                    showDialog("Sikertelen módosítás", "A jelszómódosításhoz add meg az új jelszót!");
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    showDialog("Sikertelen módosítás", "Az új jelszó és annak megerősítése nem egyezik!");
                    return;
                }

                reauthenticateAndChangePassword(newPassword);
            }
        });

        setProfilePictureClickListener();
    }

    private void loadUserProfile() {
        // Felhasználói adatok betöltése
        if (user != null && user.getEmail() != null) {
            DocumentReference userDocRef = mFirestore.collection("Users").document(user.getUid());

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String userName = documentSnapshot.getString("username");
                    String profilePicturePath = documentSnapshot.getString("profilePicture");

                    userNameTextView.setText(userName);
                    displayProfilePicture(profilePicturePath);
                }
            });
        }
    }

    private void displayProfilePicture(String profilePicturePath) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Kép betöltése...");
        progressDialog.show();

        String imagePath = !profilePicturePath.equals("") ? "profile-pictures/" + profilePicturePath : "profile-pictures/default.jpg";

        StorageReference profilePicRef = storageReference.child(imagePath);
        profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this).load(uri.toString()).into(profilePicture);
        }).addOnFailureListener(exception -> {
            Toast.makeText(EditProfileActivity.this, "Nem sikerült betölteni a képet", Toast.LENGTH_SHORT).show();
        });

        progressDialog.dismiss();
    }

    private void setProfilePictureClickListener() {
        profilePicture.setOnClickListener(view -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {
                uri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            } else {
                startCrop(imageUri);
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
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
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Feltöltés...");
            progressDialog.show();

            String fileName = UUID.randomUUID().toString();
            StorageReference ref = storageReference.child("profile-pictures/" + fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        progressDialog.dismiss();
                        DocumentReference userDocRef = mFirestore.collection("Users").document(user.getUid());
                        userDocRef.update("profilePicture", fileName);
                        profilePicture.setImageURI(imageUri);
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Hiba történt a feltöltés során", Toast.LENGTH_SHORT).show();
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        progressDialog.setMessage("Feltöltve: " + (int) progress + "%");
                    });
        }
    }

    private void reauthenticateAndChangePassword(final String newPassword) {
        if (user != null && user.getEmail() != null) {
            String currentPassword = currentPasswordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(currentPassword)) {
                showDialog("Sikertelen módosítás", "A jelszómódosításhoz add meg a jelenlegi jelszavadat!");
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPassword).addOnCompleteListener(passwordUpdateTask -> {
                        if (passwordUpdateTask.isSuccessful()) {
                            showDialog("Sikeres módosítás", "A jelszavadat sikeresen megváltoztattad!");
                            newPasswordEditText.setText("");
                            confirmPasswordEditText.setText("");
                            currentPasswordEditText.setText("");
                        } else {
                            showDialog("Sikertelen módosítás", "A jelszavad megváltoztatása közben hiba lépett fel!");
                        }
                    });
                } else {
                    showDialog("Sikertelen hitelesítés", "Az általad beírt jelenlegi jelszó nem egyezik meg a jelszavaddal!");
                }
            });
        }
    }

    private void showDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }
}
