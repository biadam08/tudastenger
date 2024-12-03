package com.szte.tudastenger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.szte.tudastenger.DefaultDayDecorator;
import com.szte.tudastenger.FailureDayDecorator;
import com.szte.tudastenger.R;
import com.szte.tudastenger.SuccessDayDecorator;
import com.szte.tudastenger.databinding.ActivityChallengeResultListingBinding;
import com.szte.tudastenger.databinding.ActivityMainBinding;
import com.szte.tudastenger.viewmodels.ChallengeViewModel;

import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChallengeResultListing extends DrawerBaseActivity {

    private ActivityChallengeResultListingBinding binding;
    private MaterialCalendarView calendarView;
    private ChallengeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChallengeResultListingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        calendarView = findViewById(R.id.calendarView);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_NONE);
        viewModel = new ViewModelProvider(this).get(ChallengeViewModel.class);
        viewModel.init();

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getChallengeResults().observe(this, challengeResults -> {
            for (Map.Entry<CalendarDay, String> entry : challengeResults.entrySet()) {
                CalendarDay day = entry.getKey();
                String result = entry.getValue();

                if ("successful".equals(result)) {
                    calendarView.addDecorator(new SuccessDayDecorator(this, day));
                } else if ("unsuccessful".equals(result)) {
                    calendarView.addDecorator(new FailureDayDecorator(this, day));
                } else {
                    calendarView.addDecorator(new DefaultDayDecorator(day));
                }
            }
        });

        viewModel.getUserHasCompleted().observe(this, hasCompleted -> {
            if(hasCompleted){
                binding.errorTextView.setVisibility(View.VISIBLE);
            } else{
                binding.errorTextView.setVisibility(View.GONE);
                binding.startDailyChallangeButton.setVisibility(View.VISIBLE);
                binding.startDailyChallangeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(ChallengeResultListing.this, ChallengeActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if(!error.isEmpty()) {
                binding.errorTextView.setVisibility(View.VISIBLE);
                binding.errorTextView.setText(error);
            } else{
                binding.errorTextView.setVisibility(View.GONE);
            }
        });
    }


}