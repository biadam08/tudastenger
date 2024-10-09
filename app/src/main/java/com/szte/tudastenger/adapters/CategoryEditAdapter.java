package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.R;
import com.szte.tudastenger.activities.CategoryListActivity;
import com.szte.tudastenger.activities.CategoryUploadActivity;
import com.szte.tudastenger.activities.QuestionEditUploadActivity;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;

public class CategoryEditAdapter extends RecyclerView.Adapter<CategoryEditAdapter.ViewHolder> implements Filterable {
    private ArrayList<Category> mCategoriesData;
    private ArrayList<Category> mCategoriesDataAll;
    private Context mContext;

    public CategoryEditAdapter(Context context, ArrayList<Category> categoriesData) {
        this.mCategoriesData = categoriesData;
        this.mCategoriesDataAll = categoriesData;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_edit_category, parent, false));
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
    @Override
    public Filter getFilter() {
        return categoryFilter;
    }

    private Filter categoryFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<Category> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();
            String filterPattern = "";

            if (charSequence != null) {
                filterPattern = charSequence.toString().toLowerCase().trim();
            }

            if (filterPattern.isEmpty()) {
                results.count = mCategoriesDataAll.size();
                results.values = mCategoriesDataAll;
            } else {
                for (Category category : mCategoriesDataAll) {
                    boolean matchesSearch = category.getName().toLowerCase().contains(filterPattern);

                    if (matchesSearch) {
                        filteredList.add(category);
                    }
                }
                results.count = filteredList.size();
                results.values = filteredList;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mCategoriesData = (ArrayList<Category>) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryName);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Category clickedCategory = mCategoriesData.get(position);
                        Intent intent = new Intent(mContext, CategoryUploadActivity.class);
                        intent.putExtra("categoryId", clickedCategory.getId());
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        public void bindTo(Category currentCategory) {
            categoryName.setText(currentCategory.getName());
        }
    }
}
