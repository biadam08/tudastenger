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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.interfaces.OnFriendRequestAdded;
import com.szte.tudastenger.R;
import com.szte.tudastenger.models.User;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> implements Filterable {
    private ArrayList<User> mUsers;
    private ArrayList<User> mUsersAll;
    private Context mContext;
    private String mCurrentUserId;
    private OnFriendRequestAdded mListener;
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();



    public UserAdapter(Context context, ArrayList<User> users, String currentUserId, OnFriendRequestAdded listener){
        this.mUsers = users;
        this.mUsersAll = users;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_users, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User currentUser = mUsers.get(position);
        checkFriendship(mCurrentUserId, currentUser.getId(), holder.mAddFriendButton);
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

    private Filter userFilter = new Filter(){

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<User> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if(charSequence == null || charSequence.length() == 0){
                results.count = mUsersAll.size();
                results.values = mUsersAll;
            } else{
                String filterPattern = charSequence.toString().toLowerCase().trim();

                for(User user : mUsersAll){
                    if(user.getUsername().toLowerCase().contains(filterPattern)){
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

    private void checkFriendship(final String currentUserId, final String userId, final ImageButton addFriendButton) {
        mFirestore.collection("Friends").document(currentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists() && document.get(userId) != null) {
                                addFriendButton.setVisibility(View.GONE); // Ha barátok, elrejtjük a gombot
                            } else {
                                // Ellenőrizzük az eredeti irányú kérelmet
                                mFirestore.collection("FriendRequests")
                                        .whereEqualTo("user_uid1", currentUserId)
                                        .whereEqualTo("user_uid2", userId)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    if (task.getResult().isEmpty()) {
                                                        // Ha nincs eredeti irányú kérelem, ellenőrizzük a fordított irányút
                                                        mFirestore.collection("FriendRequests")
                                                                .whereEqualTo("user_uid1", userId)
                                                                .whereEqualTo("user_uid2", currentUserId)
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if (task.isSuccessful()) {
                                                                            if (task.getResult().isEmpty()) {
                                                                                addFriendButton.setVisibility(View.VISIBLE); // Ha nincs függőben lévő kérelem, megjelenítjük a gombot
                                                                            } else {
                                                                                addFriendButton.setVisibility(View.GONE); // Ha van függőben lévő kérelem, elrejtjük a gombot
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                    } else {
                                                        addFriendButton.setVisibility(View.GONE); // Ha van eredeti irányú kérelem, elrejtjük a gombot
                                                    }
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });
    }


    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mUsernameText;
        private ImageButton mAddFriendButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mUsernameText = itemView.findViewById(R.id.listUsernameTextView);
            mAddFriendButton = itemView.findViewById(R.id.addFriendButton);
        }

        public void bindTo(User currentUser) {
            mUsernameText.setText(currentUser.getUsername());
            mAddFriendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("user_uid1", mCurrentUserId);
                    data.put("user_uid2", currentUser.getId());

                    mFirestore.collection("Friends").document(mCurrentUserId)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists() && document.getBoolean(currentUser.getId()) != null) {
                                            Log.d("barathozzaad", "Már barát");
                                        } else {
                                            mFirestore.collection("FriendRequests")
                                                    .whereEqualTo("user_uid1", mCurrentUserId)
                                                    .whereEqualTo("user_uid2", currentUser.getId())
                                                    .get()
                                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                            if (task.isSuccessful()) {
                                                                if (task.getResult().isEmpty()) {
                                                                    mFirestore.collection("FriendRequests")
                                                                            .whereEqualTo("user_uid1", currentUser.getId())
                                                                            .whereEqualTo("user_uid2", mCurrentUserId)
                                                                            .get()
                                                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        if (task.getResult().isEmpty()) {
                                                                                            mFirestore.collection("FriendRequests").add(data)
                                                                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                                                        @Override
                                                                                                        public void onSuccess(DocumentReference documentReference) {
                                                                                                            mAddFriendButton.setVisibility(View.GONE);
                                                                                                            mListener.onFriendRequestAdded(currentUser);
                                                                                                        }
                                                                                                    });
                                                                                        } else {
                                                                                            Log.d("barathozzaad", "A barátmeghívási kérelem már létezik");
                                                                                        }
                                                                                    }
                                                                                }
                                                                            });
                                                                } else {
                                                                    Log.d("barathozzaad", "A barátmeghívási kérelem már létezik");
                                                                }
                                                            }
                                                        }
                                                    });
                                        }
                                    } else {
                                        Log.d("barathozzaad", "get failed with ", task.getException());
                                    }
                                }
                            });
                }
            });
        }
    }
}

