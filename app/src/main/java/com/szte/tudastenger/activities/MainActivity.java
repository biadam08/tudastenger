package com.szte.tudastenger.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.szte.tudastenger.adapters.CategoryAdapter;
import com.szte.tudastenger.databinding.ActivityMainBinding;
import com.szte.tudastenger.viewmodels.MainViewModel;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends DrawerBaseActivity {
    private static final String LOG_TAG = MainActivity.class.getName();
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private ActivityMainBinding binding;

    private MainViewModel viewModel;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.loadCategories();

        setupViews();
        setupObservers();
        setupNotifications();

    }

    private void setupViews() {
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        binding.startMixedGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QuizGameActivity.class);
            intent.putExtra("mixed", "true");
            startActivity(intent);
        });
    }

    private void setupObservers() {
        viewModel.getCategoriesData().observe(this, categories -> {
            adapter = new CategoryAdapter(this, new ArrayList<>(categories));
            binding.recyclerView.setAdapter(adapter);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Hiba")
                        .setMessage(error)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void setupNotifications() {
        viewModel.createNotificationChannel();
        checkAndRequestNotificationPermission();
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 és újabb: Explicit értesítési engedély kérése
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        } else {
            // Android 12 és korábbi: Dialógus ablak felajánlása az értesítések engedélyezésére
            if (!viewModel.checkNotificationsEnabled()) {
                showNotificationSettingsDialog();
            }
        }
    }

    private void showNotificationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Engedélyezd az értesítéseket")
                .setMessage("A barátkérésekről és a kvízpárbajokról értesítéseket küldünk. Kérjük engedélyezd őket!")
                .setPositiveButton("Beállítások", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Értesítési engedély megadva.");
            } else {
                Log.d("MainActivity", "Értesítési engedély megtagadva.");
                showNotificationSettingsDialog();
            }
        }
    }
}