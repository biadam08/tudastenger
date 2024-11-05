package com.szte.tudastenger.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SearchView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.interfaces.OnFriendAdded;
import com.szte.tudastenger.interfaces.OnFriendRemoved;
import com.szte.tudastenger.interfaces.OnFriendRequestRemoved;
import com.szte.tudastenger.interfaces.OnFriendRequestAdded;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.FriendAdapter;
import com.szte.tudastenger.adapters.FriendRequestAdapter;
import com.szte.tudastenger.adapters.UserAdapter;
import com.szte.tudastenger.databinding.ActivityFriendsBinding;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.viewmodels.FriendsViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FriendsActivity extends DrawerBaseActivity implements OnFriendRequestAdded, OnFriendAdded, OnFriendRequestRemoved, OnFriendRemoved {
    private ActivityFriendsBinding binding;
    private FriendsViewModel viewModel;
    private RecyclerView mUserListRecyclerView;
    private ArrayList<User> mUsersData;
    private UserAdapter mUserAdapter;
    private FriendAdapter mFriendAdapter;
    private FriendRequestAdapter mFriendRequestAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        initializeViews();
        setupRecyclerViews();
        observeViewModel();

        binding.popUpShowUsersButton.setOnClickListener(view -> showUsers());
    }

    private void initializeViews() {
        binding.friendListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.friendRequestListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRecyclerViews() {
        viewModel.getCurrentUser().observe(this, currentUser -> {
            if (currentUser != null) {
                mFriendRequestAdapter = new FriendRequestAdapter(this, new ArrayList<>(), currentUser.getId(), viewModel);
                binding.friendRequestListRecyclerView.setAdapter(mFriendRequestAdapter);

                mFriendAdapter = new FriendAdapter(this, new ArrayList<>(), currentUser.getId(), viewModel);
                binding.friendListRecyclerView.setAdapter(mFriendAdapter);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getFriendsData().observe(this, friends -> {
            if (mFriendAdapter != null) {
                mFriendAdapter.updateData(friends);
                binding.noFriendsYetTextView.setVisibility(View.GONE);
            } else{
                binding.noFriendsYetTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getFriendRequestsData().observe(this, requests -> {
            if (mFriendRequestAdapter != null) {
                mFriendRequestAdapter.updateData(requests);
            }
        });


        viewModel.getNoFriendsVisibility().observe(this, showNoFriends -> {
            binding.noFriendsYetTextView.setVisibility(showNoFriends ? View.VISIBLE : View.GONE);
        });

        viewModel.getNoRequestsVisibility().observe(this, showNoRequests -> {
            binding.noFriendRequestsTextView.setVisibility(showNoRequests ? View.VISIBLE : View.GONE);

        });

        viewModel.getFriendRequestSent().observe(this, user -> {
            if (user != null) {
                onFriendRequestAdded(user);
            }
        });

    }

    public void showUsers() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_show_users, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);

        mUserListRecyclerView = popupView.findViewById(R.id.userListRecyclerView);
        Button mPopUpCloseButton = popupView.findViewById(R.id.popUpCloseButton);

        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mUsersData = new ArrayList<>();

        viewModel.getCurrentUser().observe(this, currentUser -> {
            if (currentUser != null) {
                mUserAdapter = new UserAdapter(this, mUsersData, currentUser.getId(), this, viewModel);
                mUserListRecyclerView.setAdapter(mUserAdapter);
                viewModel.loadUsers();
            }
        });

        viewModel.getUsersData().observe(this, users -> {
            mUsersData.clear();
            mUsersData.addAll(users);
            mUserAdapter.notifyDataSetChanged();
        });

        mPopUpCloseButton.setOnClickListener(view -> popupWindow.dismiss());

        SearchView searchView = popupView.findViewById(R.id.searchByName);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mUserAdapter.getFilter().filter(s);
                return false;
            }
        });

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        dimBehind(popupWindow);
    }

    public static void dimBehind(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(container, p);
    }

    @Override
    public void onFriendRequestAdded(User user) {
        viewModel.onFriendRequestAdded(user);
    }

    @Override
    public void onFriendAdded(User user) {
        viewModel.onFriendAdded(user);
    }

    @Override
    public void onFriendRequestRemoved(User user) {
        viewModel.onFriendRequestRemoved(user);
    }

    @Override
    public void onFriendRemoved(User user) {
        viewModel.onFriendRemoved(user);
    }
}