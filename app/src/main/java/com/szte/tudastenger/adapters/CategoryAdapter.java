package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.szte.tudastenger.R;
import com.szte.tudastenger.activities.QuizGameActivity;
import com.szte.tudastenger.models.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private ArrayList<Category> mCategoriesData;
    private Context mContext;

    public CategoryAdapter(Context context, ArrayList<Category> categoriesData) {
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

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mCategoryName;
        private RelativeLayout mCardLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mCategoryName = itemView.findViewById(R.id.categoryName);
            mCardLayout = itemView.findViewById(R.id.card);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Category clickedCategory = mCategoriesData.get(position);
                        Intent intent = new Intent(mContext, QuizGameActivity.class);
                        intent.putExtra("intentCategoryId", clickedCategory.getId());
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        public void bindTo(Category currentCategory) {
            mCategoryName.setText(currentCategory.getName());
            fetchAndSetBackground(currentCategory);
        }

        private void fetchAndSetBackground(Category category) {
            String fileName = category.getImage();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference().child("images/" + fileName);

            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(mContext)
                        .load(uri)
                        .centerCrop()
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                mCardLayout.setBackground(resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            }).addOnFailureListener(e -> {
                Log.e("CategoryAdapter", "Nem sikerült betölteni a képet: ", e);
                setDefaultBackground();
            });
        }

        private void setDefaultBackground() {
            Glide.with(mContext)
                    .load(R.drawable.default_category_bg)
                    .centerCrop()
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            mCardLayout.setBackground(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        }
    }

}
