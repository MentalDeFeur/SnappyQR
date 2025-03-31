package com.mentaldefer.snappyqr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mentaldefer.snappyqr.databinding.ActivityMainBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String QR_CONTENT_KEY = "qrContent";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startQRScanActivity();
                } else {
                    Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> qrScanLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String qrContent = result.getData().getStringExtra(QR_CONTENT_KEY);
                    addToCalendar(qrContent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.mentaldefer.snappyqr.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.validezButton.setOnClickListener(view -> checkCameraPermission());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startQRScanActivity();
        }
    }

    private void startQRScanActivity() {
        Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
        qrScanLauncher.launch(intent);
    }

    private void addToCalendar(String qrContent) {
        String dtStart = convertDate(qrContent.substring(6)); // Assuming the date is in the first 10 characters
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            assert dtStart != null;
            Date date = dateFormat.parse(dtStart);
            if (date != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE, "Evénement QR Code")
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendar.getTimeInMillis())
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendar.getTimeInMillis() + 60 * 60 * 1000)
                        .putExtra(CalendarContract.Events.ALL_DAY, true)
                        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().getTimeZone().getID());
                startActivity(intent);
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Format de date incorrect dans le code QR", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertDate(String dtend) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dtend);
            assert date != null;
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}