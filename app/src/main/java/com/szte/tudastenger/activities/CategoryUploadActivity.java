package com.szte.tudastenger.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
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
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.checkerframework.checker.units.qual.C;

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
    private String categoryName;

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
        addCategoryButton = findViewById(R.id.addCategoryButton);

        mStorage = FirebaseStorage.getInstance();
        storageReference = mStorage.getReference();

        categoryId = getIntent().getStringExtra("categoryId");

        if (categoryId != null) {
            addCategoryTextView.setText("Kérdés szerkesztése");
            addCategoryButton.setText("Módosítás");
            editBarLinearLayout.setVisibility(View.VISIBLE);
            loadCategoryData(categoryId);
        }
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

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CategoryUploadActivity.this, CategoryListActivity.class);
                startActivity(intent);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(categoryId != null) {
                    showDeleteConfirmationDialog(categoryId);
                } else{
                    Toast.makeText(CategoryUploadActivity.this, "Nincs törlendő kategória", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void uploadCategory(View view) {
        if (imageUri != null) {
            uploadImage(imageUri);  // Kép feltöltés, majd ezután kategória mentése/frissítése
        } else {
            if (categoryId != null) {
                updateCategory(null);  // Kép nélkül kérdés frissítése
            } else {
                addNewCategory(null);  // Kép nélkül új kérdés mentése
            }
        }
    }
    private void updateCategory(String filename) {
        categoryName = ((EditText) findViewById(R.id.categoryName)).getText().toString();
        String imageToSave = (filename != null) ? filename : existingImageName;
        Category category = new Category(categoryId, categoryName, imageToSave);

        mFirestore.collection("Categories").document(categoryId)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    clearInputFields();
                    showSuccessDialog("Sikeres módosítás!", "A kérdés sikeresen módosítva lett!");
                    uploadImageButton.setVisibility(View.VISIBLE);
                    manageImageLinearLayout.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> Toast.makeText(CategoryUploadActivity.this, "Hiba történt a frissítés során.", Toast.LENGTH_SHORT).show());
    }

    private void clearInputFields() {
        ((EditText) findViewById(R.id.categoryName)).setText("");

        imageUri = null;
        categoryImagePreview.setVisibility(View.GONE);

        uploadImageButton.setVisibility(View.VISIBLE);
        manageImageLinearLayout.setVisibility(View.GONE);
    }

    private void loadCategoryData(String categoryId) {
        mCategories.document(categoryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Category category = documentSnapshot.toObject(Category.class);
                        categoryName = category.getName();
                        existingImageName = category.getImage();

                        ((EditText) findViewById(R.id.categoryName)).setText(category.getName());

                        if (existingImageName != null && !existingImageName.isEmpty()) {
                            uploadImageButton.setVisibility(View.GONE);
                            manageImageLinearLayout.setVisibility(View.VISIBLE);
                            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + existingImageName);
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                Glide.with(this)
                                        .load(imageUrl)
                                        .into(categoryImagePreview);
                                categoryImagePreview.setVisibility(View.VISIBLE);
                            }).addOnFailureListener(e -> {
                                categoryImagePreview.setVisibility(View.GONE);
                                Toast.makeText(CategoryUploadActivity.this, "Hiba a kép betöltésekor", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            categoryImagePreview.setVisibility(View.GONE);
                        }
                    }
                });
    }

    public void addNewCategory(String filename) {
        categoryName = categoryNameEditText.getText().toString();

        // Ellenőrzés, hogy a kategória név mező nem üres-e
        if (categoryName.isEmpty()) {
            categoryNameEditText.setError("A kategória neve nem lehet üres");
            return;
        }

        Category category = new Category(null, categoryName, null);

        if(imageUri != null) {
            uploadImage(imageUri);
            category.setImage(filename);
        }

        mCategories.add(category).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                String documentId = documentReference.getId();
                category.setId(documentId);

                mFirestore.collection("Categories").document(documentId)
                        .update("id", documentId);

                clearInputFields();
                showSuccessDialog("Sikeres hozzáadás", "A kategória sikeresen létrehozva!");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(CategoryUploadActivity.this, "Sikertelen kategória hozzáadás!", Toast.LENGTH_SHORT).show();

        });
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
                imageUri = result.getUri();
                categoryImagePreview.setImageURI(imageUri);
                categoryImagePreview.setVisibility(View.VISIBLE);
                uploadImageButton.setVisibility(View.GONE);
                manageImageLinearLayout.setVisibility(View.VISIBLE);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.e("CATUP_CROPIMAGE_ERROR", error.getMessage());
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

    private void showDeleteConfirmationDialog(String categoryId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kategória törlése");
        builder.setMessage("Biztosan törölni szeretnéd ezt a kérdést? A kategóriához rendelt kérdések Besorolatlan kategóriába kerülnek!");

        builder.setPositiveButton("Igen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteCategory(categoryId);
            }
        });

        builder.setNegativeButton("Nem", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteCategory(String categoryId) {
        mFirestore.collection("Categories").document(categoryId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Kategória sikeresen törölve", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Hiba történt a törlés közben", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSuccessDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }

}