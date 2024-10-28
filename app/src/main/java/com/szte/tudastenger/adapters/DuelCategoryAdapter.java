package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.activities.QuizGameActivity;
import com.szte.tudastenger.R;
import com.szte.tudastenger.interfaces.OnCategoryClickListener;
import com.szte.tudastenger.models.Category;

import java.util.ArrayList;
import java.util.List;

public class DuelCategoryAdapter extends RecyclerView.Adapter<DuelCategoryAdapter.ViewHolder> {
    private ArrayList<Category> mCategoriesData;
    private Context mContext;
    private OnCategoryClickListener mOnCategoryClickListener;

    private int selectedPosition = RecyclerView.NO_POSITION;

    public DuelCategoryAdapter(Context context, ArrayList<Category> categoriesData, OnCategoryClickListener listener){
        this.mCategoriesData = categoriesData;
        this.mContext = context;
        this.mOnCategoryClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_edit_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category currentCategory = mCategoriesData.get(position);

        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.drawable.custom_shape_selected_item_border);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.custom_shape_item_default_border);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);

                String categoryName = mCategoriesData.get(holder.getAdapterPosition()).getName();
                mOnCategoryClickListener.onCategoryClicked(categoryName);
            }
        });

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
        }

        public void bindTo(Category currentCategory) {
            mCategoryName.setText(currentCategory.getName());
        }
    }

    public void updateCategories(List<Category> categories) {
        mCategoriesData.clear();
        mCategoriesData.addAll(categories);
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }
}

