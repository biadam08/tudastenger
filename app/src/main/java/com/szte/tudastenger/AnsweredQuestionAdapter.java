package com.szte.tudastenger;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

public class AnsweredQuestionAdapter extends RecyclerView.Adapter<AnsweredQuestionAdapter.ViewHolder> {
    private ArrayList<AnsweredQuestion> mAnsweredQuestionsData;
    private Context mContext;
    private String mCurrentUserId;
    private LinearLayout linearLayoutAQ;

    AnsweredQuestionAdapter(Context context, ArrayList<AnsweredQuestion> questionsData, String currentUserId){
        this.mAnsweredQuestionsData = questionsData;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_history_question, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnsweredQuestion currentQuestion = mAnsweredQuestionsData.get(position);

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

        private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

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
            if (currentQuestion.isCorrect()) {
                linearLayoutAQ.setBackgroundResource(R.color.correct_green);
            } else {
                linearLayoutAQ.setBackgroundResource(R.color.wrong_red);
            }
            Question question = new Question();
            mFirestore.collection("Questions")
                    .document(currentQuestion.getQuestionId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot questionDocument = task.getResult();
                            if (questionDocument.exists()) {
                                String questionText = questionDocument.getString("questionText");
                                ArrayList<String> answers = (ArrayList<String>) questionDocument.get("answers");
                                int correctAnswerIndex = questionDocument.getLong("correctAnswerIndex").intValue();
                                String category = questionDocument.getString("category");
                                String image = questionDocument.getString("image");
                                question.setId(currentQuestion.getQuestionId());
                                question.setQuestionText(questionText);
                                question.setAnswers(answers);
                                question.setCorrectAnswerIndex(correctAnswerIndex);
                                question.setCategory(category);
                                question.setImage(image);

                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy. MMMM dd. HH:mm:ss", new Locale("hu", "HU"));
                                sdf.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
                                String formattedDate = sdf.format(currentQuestion.getDate().toDate());

                                mQuestionTextTextView.setText(question.getQuestionText());
                                mCategoryTextView.setText(question.getCategory());
                                mAnswerTextView.setText("Válaszod: " + currentQuestion.getAnswer());
                                mCorrectAnswerTextView.setText("Helyes válasz: " + question.getAnswers().get(question.getCorrectAnswerIndex()));
                                mDateTextView.setText(formattedDate);
                            }
                        }
                    });
        }
    }


}

