package com.szte.tudastenger.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.interfaces.OnFriendRequestAdded;
import com.szte.tudastenger.R;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.viewmodels.FriendsViewModel;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> implements Filterable {
    private ArrayList<User> mUsers;
    private ArrayList<User> mUsersAll;
    private Context mContext;
    private String mCurrentUserId;
    private OnFriendRequestAdded mListener;
    private FriendsViewModel viewModel;

    public UserAdapter(Context context, ArrayList<User> users, String currentUserId, OnFriendRequestAdded listener, FriendsViewModel viewModel) {
        this.mUsers = users;
        this.mUsersAll = users;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
        this.mListener = listener;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_users, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User currentUser = mUsers.get(position);
        viewModel.checkFriendship(mCurrentUserId, currentUser.getId());
        viewModel.getFriendButtonStates().observe((LifecycleOwner) mContext, states -> {
            Boolean isVisible = states.get(currentUser.getId());
            if (isVisible != null) {
                holder.mAddFriendButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            }
        });
        holder.bindTo(currentUser);
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private Filter userFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<User> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if(charSequence == null || charSequence.length() == 0) {
                results.count = mUsersAll.size();
                results.values = mUsersAll;
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();

                for(User user : mUsersAll) {
                    if(user.getUsername().toLowerCase().contains(filterPattern)) {
                        filteredList.add(user);
                    }
                }

                results.count = filteredList.size();
                results.values = filteredList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mUsers = (ArrayList) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mUsernameText;
        private ImageButton mAddFriendButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mUsernameText = itemView.findViewById(R.id.listUsernameTextView);
            mAddFriendButton = itemView.findViewById(R.id.addFriendButton);
        }

        public void bindTo(User currentUser) {
            mUsernameText.setText(currentUser.getUsername());
            mAddFriendButton.setOnClickListener(view -> {
                viewModel.sendFriendRequest(mCurrentUserId, currentUser);
            });
        }
    }
}