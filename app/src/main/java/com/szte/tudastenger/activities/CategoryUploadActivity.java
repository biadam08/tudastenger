package com.szte.tudastenger.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.R;
import com.szte.tudastenger.databinding.ActivityCategoryUploadBinding;
import com.szte.tudastenger.models.Category;

public class CategoryUploadActivity extends DrawerBaseActivity {

    private ActivityCategoryUploadBinding activityCategoryUploadBinding;

    private FirebaseFirestore mFirestore;

    private CollectionReference mCategories;

    private EditText categoryNameEditText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCategoryUploadBinding = ActivityCategoryUploadBinding.inflate(getLayoutInflater());
        setContentView(activityCategoryUploadBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mCategories = mFirestore.collection("Categories");

        categoryNameEditText = findViewById(R.id.categoryName);
    }

    public void addNewCategory(View view) {
        String categoryName = categoryNameEditText.getText().toString();
        
        // Ellenőrzés, hogy a kategória név mező nem üres-e
        if (categoryName.isEmpty()) {
            categoryNameEditText.setError("A kategória neve nem lehet üres");
            return;
        }

        // Új kategória hozzáadása Firestore-hoz
        Category category = new Category(categoryName);
        mCategories.add(category).addOnSuccessListener(documentReference -> {
            clearInputField();
            showSuccessDialog();
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
}