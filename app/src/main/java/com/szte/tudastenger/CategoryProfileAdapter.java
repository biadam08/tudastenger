package com.szte.tudastenger;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class CategoryProfileAdapter extends RecyclerView.Adapter<CategoryProfileAdapter.ViewHolder> {
    private ArrayList<Category> mCategoriesData;
    private Context mContext;
    private User currentUser;
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

    CategoryProfileAdapter(Context context, ArrayList<Category> categoriesData, User currentUser){
        this.mCategoriesData = categoriesData;
        this.mContext = context;
        this.currentUser = currentUser;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_category_profile, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category currentCategory = mCategoriesData.get(position);

        mFirestore.collection("AnsweredQuestions")
                .whereEqualTo("userId", currentUser.getId())
                .whereEqualTo("category", currentCategory.getName())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int correctAnswers = 0;
                            int totalAnswers = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                totalAnswers++;
                                if (document.getBoolean("correct")) {
                                    correctAnswers++;
                                }
                            }
                            String categoryScore = correctAnswers + "/" + totalAnswers;
                            holder.bindTo(currentCategory, categoryScore);
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return mCategoriesData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mCategoryName;
        private TextView mScoreTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mCategoryName = itemView.findViewById(R.id.categoryNameTextView);
            mScoreTextView = itemView.findViewById(R.id.scoreTextView);
        }

        public void bindTo(Category currentCategory, String score) {
            mCategoryName.setText(currentCategory.getName());
            mScoreTextView.setText(score);
        }
    }
}

