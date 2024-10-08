package com.szte.tudastenger.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityCategoryUploadBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.UUID;

public class CategoryUploadActivity extends DrawerBaseActivity {

    private ActivityCategoryUploadBinding activityCategoryUploadBinding;

    private FirebaseFirestore mFirestore;

    private CollectionReference mCategories;
    private StorageReference storageReference;
    private EditText categoryNameEditText;
    private TextView addCategoryTextView;
    private Button addCategoryButton;

    private String categoryId; // Kategória ID szerkesztés esetén

    private Button backButton;
    private Button deleteButton;
    private Button uploadImageButton;
    private Button deleteImageButton;
    private Button modifyImageButton;
    private LinearLayout editBarLinearLayout;
    private LinearLayout manageImageLinearLayout;
    private ImageView categoryImagePreview;
    private Uri imageUri;
    private FirebaseStorage mStorage;
    private String existingImageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCategoryUploadBinding = ActivityCategoryUploadBinding.inflate(getLayoutInflater());
        setContentView(activityCategoryUploadBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mCategories = mFirestore.collection("Categories");

        categoryNameEditText = findViewById(R.id.categoryName);
        addCategoryTextView = findViewById(R.id.addCategoryTextView);
        editBarLinearLayout = findViewById(R.id.editBarLinearLayout);
        manageImageLinearLayout = findViewById(R.id.manageImageLinearLayout);
        backButton = findViewById(R.id.backButton);
        deleteButton = findViewById(R.id.deleteButton);
        categoryImagePreview = findViewById(R.id.categoryImagePreview);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        modifyImageButton = findViewById(R.id.modifyImageButton);
        deleteImageButton = findViewById(R.id.deleteImageButton);

        mStorage = FirebaseStorage.getInstance();
        storageReference = mStorage.getReference();

        categoryId = getIntent().getStringExtra("categoryId");

        if (categoryId != null) {
            addCategoryTextView.setText("Kérdés szerkesztése");
        }

        addCategoryButton = findViewById(R.id.addCategoryButton);
        addCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(categoryId == null) {
                    addNewCategory(null);
                } else{
                    updateCategory(null);
                }
            }
        });

        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
            }
        });
        deleteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageUri = null;
                existingImageName = null;

                categoryImagePreview.setVisibility(View.GONE);
                uploadImageButton.setVisibility(View.VISIBLE);
                manageImageLinearLayout.setVisibility(View.GONE);
            }
        });

        modifyImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE);
            }
        });
    }

    private void updateCategory(String filename) {
        loadCategoryData(categoryId);
    }

    private void loadCategoryData(String categoryId) {
        mCategories.document(categoryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Category category = documentSnapshot.toObject(Category.class);
                        ((EditText) findViewById(R.id.categoryName)).setText(category.getName());
                    }
                });
    }

    public void addNewCategory(String filename) {
        String categoryName = categoryNameEditText.getText().toString();

        // Ellenőrzés, hogy a kategória név mező nem üres-e
        if (categoryName.isEmpty()) {
            categoryNameEditText.setError("A kategória neve nem lehet üres");
            return;
        }

        // Új kategória hozzáadása Firestore-hoz
        Category category = new Category(null, categoryName);

        mCategories.add(category).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                String documentId = documentReference.getId();
                category.setId(documentId);

                mFirestore.collection("Categories").document(documentId)
                        .update("id", documentId);

                clearInputField();
                showSuccessDialog();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(CategoryUploadActivity.this, "Sikertelen kategória hozzáadás!", Toast.LENGTH_SHORT).show();

        });
    }

    private void clearInputField() {
        categoryNameEditText.setText("");
    }

    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sikeres feltöltés")
                .setMessage("A kategória sikeresen létrehozva!")
                .setPositiveButton("Rendben", null)
                .show();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            if(CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
            } else {
                startCrop(imageUri);
            }
        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK) {
                imageUri = result.getUri();
                categoryImagePreview.setImageURI(imageUri);
                categoryImagePreview.setVisibility(View.VISIBLE);
                uploadImageButton.setVisibility(View.GONE);
                manageImageLinearLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startCrop(Uri imageUri) {
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMultiTouchEnabled(true)
                .start(this);
    }
    private void uploadImage(Uri imageUri) {
        if(imageUri != null) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Feltöltés...");
            progressDialog.show();

            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());

            ref.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            String filename = taskSnapshot.getMetadata().getReference().getName();
                            if (categoryId != null) {
                                updateCategory(filename);
                            } else {
                                addNewCategory(filename);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(CategoryUploadActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Feltöltve: " + (int) progress + "%");
                        }
                    });
        }
    }


}