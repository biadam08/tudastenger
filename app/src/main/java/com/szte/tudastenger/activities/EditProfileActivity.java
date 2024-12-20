package com.szte.tudastenger.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.szte.tudastenger.databinding.ActivityEditProfileBinding;
import com.szte.tudastenger.viewmodels.EditProfileViewModel;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditProfileActivity extends DrawerBaseActivity {
    private EditProfileViewModel viewModel;
    private ActivityEditProfileBinding binding;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);

        initializeViews();
        observeViewModel();
        viewModel.loadUserProfile();
    }

    private void initializeViews() {
        binding.changePasswordButton.setOnClickListener(v -> {
            String currentPassword = binding.currentPasswordEditText.getText().toString().trim();
            String newPassword = binding.newPasswordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();
            viewModel.changePassword(currentPassword, newPassword, confirmPassword);
        });

        binding.deleteAccountButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        binding.profilePicture.setOnClickListener(v -> {
                Intent pickIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, 1001);
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                showProgressDialog("Kérlek várj...");
            } else {
                hideProgressDialog();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                showDialog("Hiba", error);
            }
        });

        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null) {
                showDialog("Sikeres módosítás", message);
                clearPasswordFields();
            }
        });

        viewModel.getUsername().observe(this, username -> {
            binding.userNameTextView.setText(username);
        });

        viewModel.getProfilePictureUrl().observe(this, url -> {
            if (url != null) {
                Glide.with(this).load(url).into(binding.profilePicture);
            }
        });

        viewModel.getUploadProgress().observe(this, progress -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.setMessage("Feltöltve: " + progress + "%");
            }
        });

        viewModel.getAccountDeleted().observe(this, isDeleted -> {
            if (isDeleted) {
                startActivity(new Intent(this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                startCrop(selectedImageUri);
            }
        } else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            viewModel.uploadProfilePicture(resultUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Hiba a kép vágásakor: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startCrop(Uri imageUri) {
        String destinationFileName = UUID.randomUUID().toString() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));

        UCrop.of(imageUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800)
                .start(this);
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
        builder.setTitle("Fiók törlése");
        builder.setMessage("Biztosan törölni szeretnéd véglegesen a fiókodat?");

        builder.setPositiveButton("Igen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                viewModel.deleteAccount();
            }
        });

        builder.setNegativeButton("Nem", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void clearPasswordFields() {
        binding.currentPasswordEditText.setText("");
        binding.newPasswordEditText.setText("");
        binding.confirmPasswordEditText.setText("");
    }
}
