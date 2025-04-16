package com.mentaldefer.snappyqr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        try {
            Log.i("MESSAGE", qrContent);
            String[] dt = extractDatesFromQRContent(qrContent);
            String dtStart = dt[0];
            Log.i("DTSTART",dtStart);
            String dtEnd = dt[1];
            String location = dt[3];
            String title = dt[2];
            // Assuming the date format is YYYYMMDD
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            Date startDate = sdf.parse(dtStart);
            Date endDate = sdf.parse(dtEnd);
            assert startDate != null;
            assert endDate != null;
            Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE, title)
                        .putExtra(CalendarContract.Events.EVENT_LOCATION,location)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDate.getTime())
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDate.getTime())
                        .putExtra(CalendarContract.Events.ALL_DAY, true)
                        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().getTimeZone().getID());
                startActivity(intent);
        } catch (ParseException e) {
            Toast.makeText(this, "Format de date incorrect dans le code QR", Toast.LENGTH_SHORT).show();
        }
    }

    private String[] extractDatesFromQRContent(String qrContent) {
        String dtStart = null;
        String dtEnd = null;
        String title = null;
        String location = null;

        // Regex pour extraire DTSTART et DTEND
        String dtStartRegex = "DTSTART:(\\d+)";
        String dtEndRegex = "DTEND:(\\d+)";
        String summaryRegex = "SUMMARY:(\\D+)";
        String locationRegex = "LOCATION:([^;]+)";

        // Création des objets Pattern
        Pattern dtStartPattern = Pattern.compile(dtStartRegex);
        Pattern dtEndPattern = Pattern.compile(dtEndRegex);
        Pattern summaryPattern = Pattern.compile(summaryRegex);
        Pattern locationPattern = Pattern.compile(locationRegex);

        Matcher dtStartMatcher = dtStartPattern.matcher(qrContent);
        Matcher dtEndMatcher = dtEndPattern.matcher(qrContent);
        Matcher summaryMatcher = summaryPattern.matcher(qrContent);
        Matcher locationMatcher = locationPattern.matcher(qrContent);

        // Extraction des informations
        if (dtStartMatcher.find()) {
            dtStart = dtStartMatcher.group(1);
        }
        if (dtEndMatcher.find()) {
            dtEnd = dtEndMatcher.group(1);
        }
        if (summaryMatcher.find()) {
            title = summaryMatcher.group(1);
        }
        if (locationMatcher.find()) {
            location = locationMatcher.group(1);
        }

        String[] dt = new String[5];
        dt[0] = dtStart;
        dt[1] = dtEnd;
        dt[2] = title;
        dt[3] = location;
        dt[4] = qrContent;

        // Affichage des résultats
        Log.i("EXTRACTION", "DTSTART: " + dtStart + ", DTEND: " + dtEnd + ", TITLE: " + title + ", LOCATION: " + location);
        // Conversion des dates
        return dt;
    }
}