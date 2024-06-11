package com.szte.tudastenger.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.R;
import com.szte.tudastenger.models.User;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> implements Filterable {
    private ArrayList<User> mUsers;
    private ArrayList<User> mUsersAll;
    private Context mContext;

    public UserAdapter(Context context, ArrayList<User> users){
        this.mUsers = users;
        this.mUsersAll = users;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_users, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User currentUser = mUsers.get(position);
        Log.d("USERC", currentUser.getUsername());
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

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mUsernameText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mUsernameText = itemView.findViewById(R.id.usernameTextView10);
        }

        public void bindTo(User currentUser) {
            mUsernameText.setText(currentUser.getUsername());
            Log.d("USER22", currentUser.getUsername());

        }
    }
}

