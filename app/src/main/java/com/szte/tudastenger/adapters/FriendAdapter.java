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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.R;
import com.szte.tudastenger.interfaces.OnFriendAdded;
import com.szte.tudastenger.interfaces.OnFriendRemoved;
import com.szte.tudastenger.models.User;


import java.util.ArrayList;
import java.util.Collections;


public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private ArrayList<User> mFriends;
    private Context mContext;
    private String mCurrentUserId;
    private OnFriendRemoved mOnFriendRemovedListener;
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

    public FriendAdapter(Context context, ArrayList<User> friends, String currentUserId, OnFriendRemoved onFriendRemovedListener){
        this.mFriends = friends;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
        this.mOnFriendRemovedListener = onFriendRemovedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_friends, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User currentUser = mFriends.get(position);
        holder.bindTo(currentUser);
    }

    @Override
    public int getItemCount() {
        return mFriends.size();
    }
    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mUsernameText;
        private ImageButton deleteFriendImageButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mUsernameText = itemView.findViewById(R.id.listFriendUsernameTextView);
            deleteFriendImageButton = itemView.findViewById(R.id.deleteFriendImageButton);
        }

        public void bindTo(User currentUser) {
            mUsernameText.setText(currentUser.getUsername());
            deleteFriendImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteFriend(currentUser);
                }
            });
        }
    }

    private void deleteFriend(User currentUser) {
        mFirestore.collection("Friends")
                .document(mCurrentUserId)
                .update(Collections.singletonMap(currentUser.getId(), FieldValue.delete()));

        mFirestore.collection("Friends")
                .document(currentUser.getId())
                .update(Collections.singletonMap(mCurrentUserId, FieldValue.delete()));

        mOnFriendRemovedListener.onFriendRemoved(currentUser);

    }
}

