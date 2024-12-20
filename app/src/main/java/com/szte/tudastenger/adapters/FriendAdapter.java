package com.szte.tudastenger.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.szte.tudastenger.activities.DrawerBaseActivity;
import com.szte.tudastenger.activities.DuelActivity;
import com.szte.tudastenger.activities.QuizGameActivity;
import com.szte.tudastenger.interfaces.OnFriendAdded;
import com.szte.tudastenger.interfaces.OnFriendRemoved;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.viewmodels.FriendsViewModel;


import java.util.ArrayList;
import java.util.Collections;


public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private ArrayList<User> mFriends;
    private Context mContext;
    private String mCurrentUserId;
    private FriendsViewModel viewModel;

    public FriendAdapter(Context context, ArrayList<User> friends, String currentUserId, FriendsViewModel viewModel) {
        this.mFriends = friends;
        this.mContext = context;
        this.mCurrentUserId = currentUserId;
        this.viewModel = viewModel;
    }

    public void updateData(ArrayList<User> newFriends) {
        mFriends.clear();
        mFriends.addAll(newFriends);
        notifyDataSetChanged();
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

        private ImageButton challangeFriendImageButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mUsernameText = itemView.findViewById(R.id.listFriendUsernameTextView);
            deleteFriendImageButton = itemView.findViewById(R.id.deleteFriendImageButton);
            challangeFriendImageButton = itemView.findViewById(R.id.challangeFriendImageButton);
        }

        public void bindTo(User currentUser) {
            mUsernameText.setText(currentUser.getUsername());
            deleteFriendImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDeleteConfirmationDialog(currentUser);
                }
            });

            challangeFriendImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    challangeFriend(currentUser);     
                }
            });
        }
    }

    private void challangeFriend(User currentUser) {
        Intent intent = new Intent(mContext, DuelActivity.class);
        intent.putExtra("challengerUserId", mCurrentUserId);
        intent.putExtra("challengedUserId", currentUser.getId());
        mContext.startActivity(intent);
    }


    private void showDeleteConfirmationDialog(User currentUser) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Barát törlése");
        builder.setMessage("Biztosan törölni szeretnéd a barátaid közül " + currentUser.getUsername() + " felhasználót?");

        builder.setPositiveButton("Igen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                viewModel.deleteFriend(currentUser);
            }
        });

        builder.setNegativeButton("Nem", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

