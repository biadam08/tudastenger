package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.R;
import com.szte.tudastenger.activities.DuelActivity;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.viewmodels.DuelListViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class DuelAdapter extends RecyclerView.Adapter<DuelAdapter.ViewHolder> {
    private ArrayList<Duel> mDuelsData;
    private Context mContext;
    private String mCurrentUserId;
    private DuelListViewModel viewModel;


    public DuelAdapter(Context context, ArrayList<Duel> duelsData, String currentUserId, DuelListViewModel viewModel){
        this.mDuelsData = duelsData;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public DuelAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DuelAdapter.ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_duels, parent, false));
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
        private CardView mDuelCardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mPlayersUsernameTextView = itemView.findViewById(R.id.playersUsernameTextView);
            mCategoryAndQuestionNumberTextView = itemView.findViewById(R.id.categoryAndQuestionNumberTextView);
            mDuelDate = itemView.findViewById(R.id.duelDateTextView);
            mPointsTextView = itemView.findViewById(R.id.pointsTextView);
            mDuelCardView = itemView.findViewById(R.id.duelCardView);
        }

        public void bindTo(Duel currentDuel) {

            viewModel.getUsernames(currentDuel).observe(
                    (LifecycleOwner) mContext, usernames -> {
                        if (usernames != null) {
                            mPlayersUsernameTextView.setText(usernames);
                            Log.d("ADAPTERGETUS", usernames);
                        }
                    }
            );

            if(Objects.equals(currentDuel.getCategory(), "mixed")){
                String mCategoryAndQuestionNumber = "Vegyes / " + currentDuel.getQuestionIds().size() + " db";
                mCategoryAndQuestionNumberTextView.setText(mCategoryAndQuestionNumber);
            } else {
                viewModel.loadCategoryAndQuestionNumber(currentDuel.getCategory(), currentDuel.getQuestionIds().size());

                viewModel.getCategoryAndQuestionNumber().observe((LifecycleOwner) mContext, categoryAndQuestionNumber -> {
                    mCategoryAndQuestionNumberTextView.setText(categoryAndQuestionNumber);
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
                        mDuelCardView.setBackgroundResource(R.color.correct_green);
                    } else if(challengerUserPoints < challengedUserPoints){
                        mDuelCardView.setBackgroundResource(R.color.incorrect_red);
                    } else {
                        mDuelCardView.setBackgroundResource(R.color.draw);
                    }
                } else{
                    if(challengedUserPoints > challengerUserPoints) {
                        mDuelCardView.setBackgroundResource(R.color.correct_green);
                    } else if(challengedUserPoints < challengerUserPoints){
                        mDuelCardView.setBackgroundResource(R.color.incorrect_red);
                    } else {
                        mDuelCardView.setBackgroundResource(R.color.draw);
                    }
                }
            }
        }
    }
    public void updateData(List<Duel> duels) {
        mDuelsData.clear();
        mDuelsData.addAll(duels);
        notifyDataSetChanged();
    }

}
