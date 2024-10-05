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

import com.szte.tudastenger.R;
import com.szte.tudastenger.activities.QuestionEditUploadActivity;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> {
    private ArrayList<Question> mQuestionsData;
    private Context mContext;

    public QuestionAdapter(Context context, ArrayList<Question> questionsData){
        this.mQuestionsData = questionsData;
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
        Log.d("QUESTIONAD", currentQuestion.getQuestionText());

        holder.bindTo(currentQuestion);
    }

    @Override
    public int getItemCount() {
        return mQuestionsData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
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