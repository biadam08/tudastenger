package com.szte.tudastenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.databinding.ActivityCategoryUploadBinding;

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
        Category category = new Category(categoryName);
        mCategories.add(category).addOnSuccessListener(documentReference -> {
            Toast.makeText(CategoryUploadActivity.this, "Sikeres kategória hozzáadás!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(CategoryUploadActivity.this, "Sikertelen kategória hozzáadás!", Toast.LENGTH_SHORT).show();

        });
    }
}