package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.activities.QuizGameActivity;
import com.szte.tudastenger.R;
import com.szte.tudastenger.models.Category;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private ArrayList<Category> mCategoriesData;
    private Context mContext;

    public CategoryAdapter(Context context, ArrayList<Category> categoriesData){
        this.mCategoriesData = categoriesData;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category currentCategory = mCategoriesData.get(position);
        holder.bindTo(currentCategory);
    }

    @Override
    public int getItemCount() {
        return mCategoriesData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mCategoryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mCategoryName = itemView.findViewById(R.id.categoryName);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Category clickedCategory = mCategoriesData.get(position);
                        Intent intent = new Intent(mContext, QuizGameActivity.class);
                        intent.putExtra("CategoryName", clickedCategory.getName());
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        public void bindTo(Category currentCategory) {
            mCategoryName.setText(currentCategory.getName());
        }
    }
}

