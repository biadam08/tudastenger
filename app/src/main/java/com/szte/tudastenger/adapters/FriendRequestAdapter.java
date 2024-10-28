package com.szte.tudastenger.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.interfaces.OnFriendAdded;
import com.szte.tudastenger.interfaces.OnFriendRequestRemoved;
import com.szte.tudastenger.R;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.viewmodels.FriendsViewModel;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    private ArrayList<User> mFriendRequests;
    private Context mContext;
    private String mCurrentUserId;
    private FriendsViewModel viewModel;

    public FriendRequestAdapter(Context context, ArrayList<User> friends, String currentUserId, FriendsViewModel viewModel) {
        this.mFriendRequests = friends;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
        this.viewModel = viewModel;
    }

    public void updateData(ArrayList<User> newRequests) {
        mFriendRequests.clear();
        mFriendRequests.addAll(newRequests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_friend_requests, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User currentUser = mFriendRequests.get(position);
        holder.bindTo(currentUser);
    }

    @Override
    public int getItemCount() {
        return mFriendRequests.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mUsernameText;
        private ImageButton mApproveFriendRequestImageButton;
        private ImageButton mDeclineFriendRequestImageButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mUsernameText = itemView.findViewById(R.id.listFriendUsernameTextView);
            mApproveFriendRequestImageButton = itemView.findViewById(R.id.approveFriendRequestImageButton);
            mDeclineFriendRequestImageButton = itemView.findViewById(R.id.declineFriendRequestImageButton);
        }

        public void bindTo(User currentUser) {
            mUsernameText.setText(currentUser.getUsername());
            mDeclineFriendRequestImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel.declineFriendRequest(currentUser);
                }
            });

            viewModel.checkCanApproveRequest(currentUser.getId(), mCurrentUserId)
                    .observe((LifecycleOwner) mContext, canApprove -> {
                        mApproveFriendRequestImageButton.setVisibility(canApprove ? View.VISIBLE : View.GONE);
                        if (canApprove) {
                            mApproveFriendRequestImageButton.setOnClickListener(v -> viewModel.approveFriendRequest(currentUser));
                        }
                    });

        }
    }
}

