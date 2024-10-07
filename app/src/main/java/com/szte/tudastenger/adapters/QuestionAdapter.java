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
import com.szte.tudastenger.activities.QuestionEditUploadActivity;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> implements Filterable {
    private ArrayList<Question> mQuestionsData;
    private ArrayList<Question> mQuestionsDataAll;
    private Context mContext;
    private String filterCategory = "Összes kategória";

    public QuestionAdapter(Context context, ArrayList<Question> questionsData) {
        this.mQuestionsData = questionsData;
        this.mQuestionsDataAll = questionsData;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_question, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question currentQuestion = mQuestionsData.get(position);
        holder.bindTo(currentQuestion);
    }

    @Override
    public int getItemCount() {
        return mQuestionsData.size();
    }

    public void setFilterCategory(String category) {
        this.filterCategory = category;
        getFilter().filter("");
    }

    @Override
    public Filter getFilter() {
        return questionFilter;
    }

    private Filter questionFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<Question> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();
            String filterPattern = "";

            if (charSequence != null) {
                filterPattern = charSequence.toString().toLowerCase().trim();
            }

            if (filterPattern.isEmpty() && filterCategory.equals("Összes kategória")) {
                results.count = mQuestionsDataAll.size();
                results.values = mQuestionsDataAll;
            } else {
                for (Question question : mQuestionsDataAll) {
                    boolean matchesCategory = filterCategory.equals("Összes kategória") ||
                            question.getCategory().equals(filterCategory);

                    boolean matchesSearch = question.getQuestionText().toLowerCase().contains(filterPattern);

                    if (matchesCategory && matchesSearch) {
                        filteredList.add(question);
                    }
                }
                results.count = filteredList.size();
                results.values = filteredList;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mQuestionsData = (ArrayList<Question>) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mQuestionText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mQuestionText = itemView.findViewById(R.id.questionText);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Question clickedQuestion = mQuestionsData.get(position);
                        Intent intent = new Intent(mContext, QuestionEditUploadActivity.class);
                        intent.putExtra("questionId", clickedQuestion.getId());
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        public void bindTo(Question currentQuestion) {
            mQuestionText.setText(currentQuestion.getQuestionText());
        }
    }
}
