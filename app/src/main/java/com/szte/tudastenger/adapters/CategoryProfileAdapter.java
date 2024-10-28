package com.szte.tudastenger.adapters;

import android.content.Context;
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
import com.szte.tudastenger.R;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CategoryProfileAdapter extends RecyclerView.Adapter<CategoryProfileAdapter.ViewHolder> {
    private ArrayList<Category> categories;
    private Map<String, String> categoryScores;
    private Context context;

    public CategoryProfileAdapter(Context context, ArrayList<Category> categories) {
        this.context = context;
        this.categories = categories;
        this.categoryScores = new HashMap<>();
    }

    public void setCategoryScores(Map<String, String> scores) {
        this.categoryScores = scores;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.list_category_profile, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        String score = categoryScores.getOrDefault(category.getId(), "0/0");
        holder.bindTo(category, score);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryName;
        private TextView scoreTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryNameTextView);
            scoreTextView = itemView.findViewById(R.id.scoreTextView);
        }

        public void bindTo(Category category, String score) {
            categoryName.setText(category.getName());
            scoreTextView.setText(score);
        }
    }
}
