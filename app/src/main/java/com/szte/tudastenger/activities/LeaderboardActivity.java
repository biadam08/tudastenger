package com.szte.tudastenger.activities;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.tudastenger.R;
import com.szte.tudastenger.adapters.UserLeaderboardAdapter;
import com.szte.tudastenger.databinding.ActivityLeaderboardBinding;
import com.szte.tudastenger.models.Category;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class LeaderboardActivity extends DrawerBaseActivity {
    private ActivityLeaderboardBinding activityLeaderboardBinding;

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private CollectionReference mUsers;
    private CollectionReference mAnsweredQuestions;
    private RecyclerView mRecyclerView;

    private UserLeaderboardAdapter mAdapter;
    private TextView selectedDate;
    private Button selectDateButton;
    private Spinner spinner;
    private TextView noDataForThisFilter;
    private String startDateString;
    private String endDateString;
    private Date startDateFromDialog;
    private Date endDateFromDialog;
    private String selectedCategory;
    private String selectedCategoryId;
    private ImageView clearDateRangeImageView;
    private List<Map.Entry<String, Integer>> sortedListWithUsernames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityLeaderboardBinding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(activityLeaderboardBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mUsers = mFirestore.collection("Users");
        mAnsweredQuestions = mFirestore.collection("AnsweredQuestions");
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        selectedDate = findViewById(R.id.selectedDate);
        selectDateButton = findViewById(R.id.selectDateButton);
        spinner = findViewById(R.id.spinner);
        noDataForThisFilter = findViewById(R.id.noDataForThisFilter);
        clearDateRangeImageView = findViewById(R.id.clearDateRangeImageView);

        clearDateRangeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDateString = null;
                endDateString = null;
                startDateFromDialog = null;
                endDateFromDialog = null;
                selectedDate.setVisibility(View.GONE);
                displayLeaderboard();
            }
        });

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        mAdapter = new UserLeaderboardAdapter(this, sortedListWithUsernames);
        mRecyclerView.setAdapter(mAdapter);

        Locale locale = new Locale("hu", "HU");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        List<Category> categoryList = new ArrayList<>();
        Category allCategories = new Category("0", "Összes kategória", null);
        categoryList.add(allCategories);

        mFirestore.collection("Categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                Category category = documentSnapshot.toObject(Category.class);
                category.setId(documentSnapshot.getId());
                categoryList.add(category);
            }

            ArrayAdapter<Category> adapter = new ArrayAdapter<>(LeaderboardActivity.this, android.R.layout.simple_spinner_item, categoryList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        });


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
                Category selectedCategory = (Category) parent.getItemAtPosition(position);
                selectedCategoryId = selectedCategory.getId();
                displayLeaderboard();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                displayLeaderboard();
            }
        });


        selectDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog();
            }
        });

        displayLeaderboard();

    }

    private void displayLeaderboard() {
        Query query = mAnsweredQuestions;

        Log.d("dátum", String.valueOf(startDateFromDialog));
        if (startDateFromDialog != null && endDateFromDialog != null) {
            selectedDate.setVisibility(View.VISIBLE);
            selectedDate.setText(startDateString + " - " + endDateString);

             /*
                Azért szükséges hozzáadni egy napot, mert csak az adott nap 00:00:00-ig nézné a kvízeket,
                nem pedig az adott napi 23:59:59-ig
             */

            Calendar cal = Calendar.getInstance();
            cal.setTime(endDateFromDialog);
            cal.add(Calendar.DATE, 1);
            Date endDatePlusOne = cal.getTime();

            query = query.whereGreaterThanOrEqualTo("date", startDateFromDialog)
                    .whereLessThanOrEqualTo("date", endDatePlusOne);
        }


        if (selectedCategory != null && !selectedCategory.isEmpty() && !selectedCategory.equals("Összes kategória")) {
            query = query.whereEqualTo("category", selectedCategoryId);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                HashMap<String, Integer> userPoints = new HashMap<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String userId = document.getString("userId");
                    boolean correct = document.getBoolean("correct");

                    if (correct) {
                        userPoints.put(userId, userPoints.getOrDefault(userId, 0) + 25);
                    } else {
                        userPoints.put(userId, userPoints.getOrDefault(userId, 0) - 25);
                    }
                }

                List<Map.Entry<String, Integer>> userList = new ArrayList<>(userPoints.entrySet());
                Collections.sort(userList, (entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                final CountDownLatch latch = new CountDownLatch(userList.size());
                HashMap <String, Integer> sortedMapWithUsernames = new HashMap <>();

                for (Map.Entry<String, Integer> entry : userList) {
                    mUsers.document(entry.getKey()).get().addOnCompleteListener(userTask -> {
                        if (userTask.isSuccessful()) {
                            DocumentSnapshot userDocument = userTask.getResult();
                            if (userDocument.exists()) {
                                String username = userDocument.getString("username");
                                sortedMapWithUsernames.put(username, sortedMapWithUsernames.getOrDefault(username, 0) + entry.getValue());
                            }
                        }
                        latch.countDown();
                    });
                }

                // A fő szálra váltás a UI frissítéséhez
                new Thread(() -> {
                    try {
                        latch.await();
                        runOnUiThread(() -> {
                            sortedListWithUsernames.clear();
                            sortedListWithUsernames.addAll(sortedMapWithUsernames.entrySet());

                            Collections.sort(sortedListWithUsernames, (entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                            if (sortedListWithUsernames.isEmpty()) {
                                noDataForThisFilter.setVisibility(View.VISIBLE);
                            } else {
                                noDataForThisFilter.setVisibility(View.GONE);
                            }

                            mAdapter.notifyDataSetChanged();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

            }

        });

    }


    private void datePickerDialog() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Válassz ki egy időtartamot");

        MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();
        datePicker.addOnPositiveButtonClickListener(selection -> {

            Long startDate = selection.first;
            Long endDate = selection.second;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            startDateString = sdf.format(new Date(startDate));
            endDateString = sdf.format(new Date(endDate));

            try {
                startDateFromDialog = sdf.parse(startDateString);
                endDateFromDialog = sdf.parse(endDateString);
                displayLeaderboard();
            } catch (ParseException e) {
                e.printStackTrace();
            }


        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }
}