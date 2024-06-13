package com.szte.tudastenger.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    private ArrayList<User> mFriendRequests;
    private Context mContext;
    private String mCurrentUserId;
    private OnFriendAdded mOnFriendAddedListener;
    private OnFriendRequestRemoved mOnFriendRequestRemovedListener;
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();


    public FriendRequestAdapter(Context context, ArrayList<User> friends, String currentUserId, OnFriendAdded onFriendAddedListener, OnFriendRequestRemoved onFriendRequestRemovedListener){
        this.mFriendRequests = friends;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
        this.mOnFriendAddedListener = onFriendAddedListener;
        this.mOnFriendRequestRemovedListener = onFriendRequestRemovedListener;
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
    class ViewHolder extends RecyclerView.ViewHolder{
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
                    declineFriendRequest(currentUser);
                }
            });

            mFirestore.collection("FriendRequests")
                    .whereEqualTo("user_uid1", currentUser.getId())
                    .whereEqualTo("user_uid2", mCurrentUserId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    mApproveFriendRequestImageButton.setVisibility(View.GONE);
                                } else {
                                    mApproveFriendRequestImageButton.setVisibility(View.VISIBLE);
                                    mApproveFriendRequestImageButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            approveFriendRequest(currentUser);
                                        }
                                    });
                                }
                            }
                        }
                    });
        }
    }

    private void approveFriendRequest(User currentUser) {
        // Töröljük a FriendRequests kollekcióból az adott adatot
        declineFriendRequest(currentUser);

        // Adjuk hozzá a Friends kollekcióba az adott uid-jú felhasználóhoz a barátságot
        Map<String, Object> friend = new HashMap<>();
        friend.put(currentUser.getId(), true);
        mFirestore.collection("Friends")
                .document(mCurrentUserId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            mFirestore.collection("Friends")
                                    .document(mCurrentUserId)
                                    .update(friend);
                        } else {
                            mFirestore.collection("Friends")
                                    .document(mCurrentUserId)
                                    .set(friend);
                        }
                    }
                });

        // Frissítjük a barátságot a meghívó felhasználónál is
        Map<String, Object> friend2 = new HashMap<>();
        friend2.put(mCurrentUserId, true);
        mFirestore.collection("Friends")
                .document(currentUser.getId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            mFirestore.collection("Friends")
                                    .document(currentUser.getId())
                                    .update(friend2);
                        } else {
                            mFirestore.collection("Friends")
                                    .document(currentUser.getId())
                                    .set(friend2);
                        }
                    }
                });

        mOnFriendAddedListener.onFriendAdded(currentUser);
        mOnFriendRequestRemovedListener.onFriendRequestRemoved(currentUser);
       // mApproveFriendRequestImageButton.setVisibility(View.GONE);
    }

    private void declineFriendRequest(User currentUser) {
        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", mCurrentUserId)
                .whereEqualTo("user_uid2", currentUser.getId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                mFirestore.collection("FriendRequests").document(document.getId()).delete();
                            }
                        }
                    }
                });

        mFirestore.collection("FriendRequests")
                .whereEqualTo("user_uid1", currentUser.getId())
                .whereEqualTo("user_uid2", mCurrentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                mFirestore.collection("FriendRequests").document(document.getId()).delete();
                            }
                        }
                    }
                });

        mOnFriendRequestRemovedListener.onFriendRequestRemoved(currentUser);
    }
}

