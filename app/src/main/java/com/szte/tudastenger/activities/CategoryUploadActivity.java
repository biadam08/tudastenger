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
import com.szte.tudastenger.databinding.ActivityCategoryUploadBinding;
import com.szte.tudastenger.viewmodels.CategoryEditUploadViewModel;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;


public class CategoryUploadActivity extends DrawerBaseActivity {
    private ActivityCategoryUploadBinding binding;
    private CategoryEditUploadViewModel viewModel;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CategoryEditUploadViewModel.class);

        viewModel.checkAdmin();

        String categoryId = getIntent().getStringExtra("categoryId");
        viewModel.init(categoryId);

        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        binding.uploadImageButton.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
        });

        binding.deleteImageButton.setOnClickListener(v -> {
            viewModel.clearImage();
            binding.categoryImagePreview.setVisibility(View.GONE);
            binding.uploadImageButton.setVisibility(View.VISIBLE);
            binding.manageImageLinearLayout.setVisibility(View.GONE);
        });

        binding.modifyImageButton.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
        });

        binding.backButton.setOnClickListener(v ->
                startActivity(new Intent(this, CategoryListActivity.class)));

        binding.deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        binding.addCategoryButton.setOnClickListener(v ->
                viewModel.uploadCategory(binding.categoryName.getText().toString()));
    }

    private void observeViewModel() {
        viewModel.getIsAdmin().observe(this, isAdmin -> {
            if (!isAdmin) {
                finish();
            }
        });

        viewModel.getCategoryData().observe(this, category -> {
            if (category != null) {
                binding.addCategoryTextView.setText("Kategória szerkesztése");
                binding.addCategoryButton.setText("Módosítás");
                binding.editBarLinearLayout.setVisibility(View.VISIBLE);
                binding.categoryName.setText(category.getName());
            }
        });

        viewModel.getImageUrl().observe(this, url -> {
            if (url != null) {
                Glide.with(this)
                        .load(url)
                        .into(binding.categoryImagePreview);

                binding.categoryImagePreview.setVisibility(View.VISIBLE);
                binding.uploadImageButton.setVisibility(View.GONE);
                binding.manageImageLinearLayout.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null) {
                showSuccessDialog("Sikeres művelet", message);
            }
        });

        viewModel.getIsImageUploading().observe(this, isUploading -> {
            if (isUploading) {
                showProgressDialog("Feltöltés...");
            } else {
                hideProgressDialog();
            }
        });

        viewModel.getUploadProgress().observe(this, progress ->
                updateProgressDialog("Feltöltve: " + progress + "%"));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri selectedImageUri = CropImage.getPickImageResultUri(this, data);
            if (CropImage.isReadExternalStoragePermissionsRequired(this, selectedImageUri)) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            } else {
                startCrop(selectedImageUri);
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                viewModel.setImageUri(result.getUri());
                Glide.with(this)
                        .load(result.getUri())
                        .into(binding.categoryImagePreview);

                binding.categoryImagePreview.setVisibility(View.VISIBLE);
                binding.uploadImageButton.setVisibility(View.GONE);
                binding.manageImageLinearLayout.setVisibility(View.VISIBLE);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, "Hiba a kép vágásakor: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCrop(Uri imageUri) {
        CropImage.activity(imageUri)
                .setCropMenuCropButtonTitle("Beállítás")
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMultiTouchEnabled(true)
                .start(this);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Kategória törlése")
                .setMessage("Biztosan törölni szeretnéd ezt a kategóriát? " +
                        "A kategóriához rendelt kérdések Besorolatlan kategóriába kerülnek!")
                .setPositiveButton("Igen", (dialog, which) -> viewModel.deleteCategory())
                .setNegativeButton("Nem", null)
                .show();
    }

    private void showSuccessDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", (dialog, which) -> {
                    Intent intent = new Intent(CategoryUploadActivity.this, CategoryListActivity.class);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setTitle(message);
        progressDialog.show();
    }

    private void updateProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}