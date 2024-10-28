package com.szte.tudastenger.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.R;
import com.szte.tudastenger.models.AnsweredQuestion;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.viewmodels.HistoryViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AnsweredQuestionAdapter extends RecyclerView.Adapter<AnsweredQuestionAdapter.ViewHolder> {
    private ArrayList<AnsweredQuestion> mAnsweredQuestionsData;
    private Context mContext;
    private HistoryViewModel viewModel;

    public AnsweredQuestionAdapter(Context context, ArrayList<AnsweredQuestion> questionsData, HistoryViewModel viewModel){
        this.mAnsweredQuestionsData = questionsData;
        this.mContext = context;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_history_question, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnsweredQuestion currentQuestion = mAnsweredQuestionsData.get(position);

        if (currentQuestion.isCorrect()) {
            holder.linearLayoutAQ.setBackgroundResource(R.color.correct_green);
        } else {
            holder.linearLayoutAQ.setBackgroundResource(R.color.incorrect_red);
        }


        holder.bindTo(currentQuestion);
    }

    @Override
    public int getItemCount() {
        return mAnsweredQuestionsData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mQuestionTextTextView;
        private TextView mCategoryTextView;
        private TextView mAnswerTextView;
        private TextView mCorrectAnswerTextView;
        private TextView mDateTextView;
        private LinearLayout linearLayoutAQ;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayoutAQ = itemView.findViewById(R.id.linearLayoutAQ);
            mQuestionTextTextView = itemView.findViewById(R.id.questionTextTextView);
            mCategoryTextView = itemView.findViewById(R.id.categoryTextView);
            mAnswerTextView = itemView.findViewById(R.id.answerTextView);
            mCorrectAnswerTextView = itemView.findViewById(R.id.correctAnswerTextView);
            mDateTextView = itemView.findViewById(R.id.dateTextView);
        }

        public void bindTo(AnsweredQuestion currentQuestion) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy. MMMM dd. HH:mm:ss", new Locale("hu", "HU"));
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
            String formattedDate = sdf.format(currentQuestion.getDate().toDate());

            viewModel.getQuestionDetails(currentQuestion.getQuestionId()).observe(
                    (LifecycleOwner) mContext, questionDetails -> {
                        if (questionDetails != null) {
                            mDateTextView.setText(formattedDate);
                            mCategoryTextView.setText(currentQuestion.getCategory());
                            mAnswerTextView.setText("Válaszod: " + currentQuestion.getAnswer());
                            mQuestionTextTextView.setText(questionDetails.getQuestionText());
                            mCorrectAnswerTextView.setText("Helyes válasz: " + questionDetails.getAnswers().get(questionDetails.getCorrectAnswerIndex()));
                        }
                    }
            );
        }
    }

    public void updateData(List<AnsweredQuestion> newData) {
        mAnsweredQuestionsData.clear();
        mAnsweredQuestionsData.addAll(newData);
        notifyDataSetChanged();
    }
}
