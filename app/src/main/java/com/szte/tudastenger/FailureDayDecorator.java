package com.szte.tudastenger;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;

import androidx.core.content.ContextCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

public class FailureDayDecorator implements DayViewDecorator {
    private CalendarDay date;
    private Context context;

    public FailureDayDecorator(Context context, CalendarDay date) {
        this.context = context;
        this.date = date;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(date);
    }

    @Override
    public void decorate(DayViewFacade view) {
        int color = ContextCompat.getColor(context, R.color.incorrect_red);
        view.setBackgroundDrawable(new ColorDrawable(color));
    }
}

