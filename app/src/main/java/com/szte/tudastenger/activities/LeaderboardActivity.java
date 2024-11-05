package com.szte.tudastenger.activities;

import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.szte.tudastenger.adapters.UserLeaderboardAdapter;
import com.szte.tudastenger.databinding.ActivityLeaderboardBinding;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.viewmodels.LeaderboardViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LeaderboardActivity extends DrawerBaseActivity {
    private LeaderboardViewModel viewModel;
    private ActivityLeaderboardBinding binding;
    private UserLeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LeaderboardViewModel.class);

        setupRecyclerView();
        setupSpinner();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new UserLeaderboardAdapter(this, new ArrayList<>());
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSpinner() {
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Category selectedCategory = (Category) parent.getItemAtPosition(position);
                viewModel.setSelectedCategory(selectedCategory.getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        binding.selectDateButton.setOnClickListener(v -> showDatePickerDialog());
        binding.clearDateRangeImageView.setOnClickListener(v -> viewModel.clearDateRange());
    }

    private void observeViewModel() {
        viewModel.getCategories().observe(this, categories -> {
            ArrayAdapter<Category> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinner.setAdapter(adapter);
        });

        viewModel.getLeaderboardData().observe(this, data -> {
            adapter.updateData(data);
        });

        viewModel.getShowNoData().observe(this, showNoData -> {
            binding.noDataForThisFilter.setVisibility(showNoData ? View.VISIBLE : View.GONE);
        });

        viewModel.getDateRangeText().observe(this, dateRange -> {
            binding.selectedDate.setVisibility(dateRange != null ? View.VISIBLE : View.GONE);
            binding.selectedDate.setText(dateRange);
        });
    }

    private void showDatePickerDialog() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Válassz ki egy időtartamot");

        MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            String startDateString = sdf.format(new Date(selection.first));
            String endDateString = sdf.format(new Date(selection.second));

            try {
                Date startDate = sdf.parse(startDateString);
                Date endDate = sdf.parse(endDateString);
                viewModel.setDateRange(startDate, endDate, startDateString, endDateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }
}
