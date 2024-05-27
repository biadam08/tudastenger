package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.activities.QuizGameActivity;
import com.szte.tudastenger.R;
import com.szte.tudastenger.activities.SavedQuestionGameActivity;
import com.szte.tudastenger.models.Question;

import java.util.ArrayList;

public class SavedQuestionAdapter extends RecyclerView.Adapter<SavedQuestionAdapter.ViewHolder> {
    private ArrayList<Question> mQuestionsData;
    private Context mContext;
    private String mCurrentUserId;

    public SavedQuestionAdapter(Context context, ArrayList<Question> questionsData, String currentUserId){
        this.mQuestionsData = questionsData;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_saved_question, parent, false));
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

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mCategoryName;
        private TextView mQuestionText;
        private Button mSolveButton;
        private ImageView mDeleteSavedQuestion;
        private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mQuestionText = itemView.findViewById(R.id.questionTextTextView);
            mCategoryName = itemView.findViewById(R.id.categoryTextView);
            mSolveButton = itemView.findViewById(R.id.solveButton);
            mDeleteSavedQuestion = itemView.findViewById(R.id.deleteSavedQuestion);
        }

        public void bindTo(Question currentQuestion) {
            mQuestionText.setText(currentQuestion.getQuestionText());
            mCategoryName.setText(currentQuestion.getCategory());

            mSolveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, SavedQuestionGameActivity.class);
                    intent.putExtra("questionId", currentQuestion.getId());
                    mContext.startActivity(intent);
                }
            });

            mDeleteSavedQuestion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mFirestore.collection("SavedQuestions")
                            .whereEqualTo("userId", mCurrentUserId)
                            .whereEqualTo("questionId", currentQuestion.getId())
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                            mFirestore.collection("SavedQuestions").document(documentSnapshot.getId())
                                                    .delete();
                                        }
                                    }
                                    mQuestionsData.remove(currentQuestion);
                                    notifyDataSetChanged();
                                }
                            });
                }
            });
        }
    }
}

