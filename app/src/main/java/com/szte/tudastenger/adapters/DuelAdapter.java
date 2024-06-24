package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.tudastenger.R;
import com.szte.tudastenger.activities.DuelActivity;
import com.szte.tudastenger.models.Duel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class DuelAdapter extends RecyclerView.Adapter<DuelAdapter.ViewHolder> {
    private ArrayList<Duel> mDuelsData;
    private Context mContext;
    private String mCurrentUserId;
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();


    public DuelAdapter(Context context, ArrayList<Duel> duelsData, String currentUserId){
        this.mDuelsData = duelsData;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
    }

    @NonNull
    @Override
    public DuelAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DuelAdapter.ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_pending_duels, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DuelAdapter.ViewHolder holder, int position) {
        Duel currentDuel = mDuelsData.get(position);
        holder.bindTo(currentDuel);
    }

    @Override
    public int getItemCount() {
        return mDuelsData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mPlayersUsernameTextView;
        private TextView mCategoryAndQuestionNumberTextView;
        private TextView mDuelDate;
        private TextView mPointsTextView;
        private RelativeLayout mDuelRelativeLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mPlayersUsernameTextView = itemView.findViewById(R.id.playersUsernameTextView);
            mCategoryAndQuestionNumberTextView = itemView.findViewById(R.id.categoryAndQuestionNumberTextView);
            mDuelDate = itemView.findViewById(R.id.duelDateTextView);
            mPointsTextView = itemView.findViewById(R.id.pointsTextView);
            mDuelRelativeLayout = itemView.findViewById(R.id.duelRelativeLayout);
        }

        public void bindTo(Duel currentDuel) {
            mFirestore.collection("Users")
                    .document(currentDuel.getChallengerUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            mFirestore.collection("Users")
                                    .document(currentDuel.getChallengedUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot2 -> {
                                        if (documentSnapshot2.exists()) {
                                            String challengedUsername = documentSnapshot2.getString("username");
                                            String challengerUsername = documentSnapshot.getString("username");
                                            String players = challengerUsername + " - " + challengedUsername;
                                            mPlayersUsernameTextView.setText(players);
                                        }
                                    });
                        }
                    });

            if(Objects.equals(currentDuel.getCategory(), "mixed")){
                String mCategoryAndQuestionNumber = "Vegyes / " + currentDuel.getQuestionIds().size() + " db";
                mCategoryAndQuestionNumberTextView.setText(mCategoryAndQuestionNumber);
            } else {
                mFirestore.collection("Categories")
                        .document(currentDuel.getCategory())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String mCategoryAndQuestionNumber = documentSnapshot.getString("name") + " / " + currentDuel.getQuestionIds().size() + " db";
                                mCategoryAndQuestionNumberTextView.setText(mCategoryAndQuestionNumber);
                            }
                        });
            }


            SimpleDateFormat sdf = new SimpleDateFormat("yyyy. MMMM dd. HH:mm:ss", new Locale("hu", "HU"));
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
            String formattedDate = sdf.format(currentDuel.getDate().toDate());
            mDuelDate.setText(formattedDate);

            if(mCurrentUserId.equals(currentDuel.getChallengedUid()) && currentDuel.getChallengedUserResults() == null){
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(mContext, DuelActivity.class);
                        intent.putExtra("duelId", currentDuel.getId());
                        mContext.startActivity(intent);
                    }
                });
            }

            if(currentDuel.getChallengedUserResults() != null){
                mPointsTextView.setVisibility(View.VISIBLE);
                int challengerUserPoints = 0;
                int challengedUserPoints = 0;

                for(int i = 0; i < currentDuel.getQuestionIds().size(); i++){
                    if(currentDuel.getChallengedUserResults().get(i)){
                        challengedUserPoints++;
                    }
                    if(currentDuel.getChallengerUserResults().get(i)){
                        challengerUserPoints++;
                    }
                }

                String points = challengerUserPoints + " : " + challengedUserPoints;
                mPointsTextView.setText(points);

                if(currentDuel.getChallengerUid().equals(mCurrentUserId)) {
                    if(challengerUserPoints > challengedUserPoints) {
                        mDuelRelativeLayout.setBackgroundResource(R.color.correct_green);
                    } else if(challengerUserPoints < challengedUserPoints){
                        mDuelRelativeLayout.setBackgroundResource(R.color.wrong_red);
                    } else {
                        mDuelRelativeLayout.setBackgroundResource(R.color.draw);
                    }
                } else{
                    if(challengedUserPoints > challengerUserPoints) {
                        mDuelRelativeLayout.setBackgroundResource(R.color.correct_green);
                    } else if(challengedUserPoints < challengerUserPoints){
                        mDuelRelativeLayout.setBackgroundResource(R.color.wrong_red);
                    } else {
                        mDuelRelativeLayout.setBackgroundResource(R.color.draw);
                    }
                }
            }
        }
    }
}
