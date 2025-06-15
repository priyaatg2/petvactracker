package com.example.petvactracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RemindersActivity extends AppCompatActivity {

    private ListView remindersListView;
    private DBHelper dbHelper;
    private List<String> remindersList;
    private String petEmail, petSpecies, petDOB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        dbHelper = new DBHelper(this);

        petEmail = getSharedPreferences("UserSession", MODE_PRIVATE).getString("loggedInPetEmail", null);
        remindersListView = findViewById(R.id.reminders_list);
        remindersList = new ArrayList<>();

        if (petEmail != null) {
            fetchPetDetails();
            fetchUpcomingReminders();
            suggestVaccinations();
        } else {
            remindersList.add("No pet profile found.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, remindersList);
        remindersListView.setAdapter(adapter);
    }

    private void fetchPetDetails() {
        Cursor cursor = dbHelper.getPetDetails(petEmail);
        if (cursor != null && cursor.moveToFirst()) {
            petSpecies = cursor.getString(cursor.getColumnIndex("pet_species"));
            petDOB = cursor.getString(cursor.getColumnIndex("pet_dob"));
            cursor.close();
        }
    }

    private void fetchUpcomingReminders() {
        Cursor cursor = dbHelper.getUpcomingVaccinations(petEmail);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar today = Calendar.getInstance();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String vaccineName = cursor.getString(cursor.getColumnIndex("vaccine_name"));
                String nextDueDate = cursor.getString(cursor.getColumnIndex("next_due_date"));
                remindersList.add("Upcoming: " + vaccineName + " (Due: " + nextDueDate + ")");

                scheduleNotification(vaccineName, nextDueDate);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private void suggestVaccinations() {
        int petAgeInMonths = calculateAgeInMonths(petDOB);

        List<String> recommendedVaccines = getVaccinationSuggestions(petSpecies, petAgeInMonths);

        for (String vaccine : recommendedVaccines) {
            remindersList.add("💡 Suggested: " + vaccine);
        }
    }

    private int calculateAgeInMonths(String dob) {
        if (dob == null || dob.isEmpty()) return 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Calendar dobCalendar = Calendar.getInstance();
            dobCalendar.setTime(sdf.parse(dob));

            Calendar today = Calendar.getInstance();
            int ageInMonths = (today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)) * 12 +
                    today.get(Calendar.MONTH) - dobCalendar.get(Calendar.MONTH);

            return ageInMonths;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private List<String> getVaccinationSuggestions(String species, int ageInMonths) {
        List<String> suggestedVaccines = new ArrayList<>();

        if ("Dog".equalsIgnoreCase(species)) {
            if (ageInMonths < 3) {
                suggestedVaccines.add("Distemper");
                suggestedVaccines.add("Parvovirus");
            } else if (ageInMonths < 6) {
                suggestedVaccines.add("Rabies");
            } else {
                suggestedVaccines.add("Annual Booster Shots");
            }
        } else if ("Cat".equalsIgnoreCase(species)) {
            if (ageInMonths < 3) {
                suggestedVaccines.add("Feline Distemper");
                suggestedVaccines.add("Rhinotracheitis");
            } else if (ageInMonths < 6) {
                suggestedVaccines.add("Feline Leukemia");
            } else {
                suggestedVaccines.add("Annual Booster Shots");
            }
        }
        return suggestedVaccines;
    }

    private void scheduleNotification(String vaccineName, String nextDueDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(nextDueDate + " 10:00")); // notification at 10 AM

            // Make sure the alarm time is in the future
            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                Intent intent = new Intent(this, ReminderReceiver.class);
                intent.putExtra("reminderText", "Upcoming: " + vaccineName + " (Due: " + nextDueDate + ")");

                int requestCode = (int) System.currentTimeMillis() % 10000; // Unique request code
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                Log.d("ReminderActivity", "Scheduled notification for: " + nextDueDate + " at 22:40");
            } else {
                Log.e("ReminderActivity", "Skipping notification; time is in the past.");
            }
        } catch (Exception e) {
            Log.e("ReminderActivity", "Error scheduling notification: " + e.getMessage());
        }
    }
}

/* package com.example.petvactracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RemindersActivity extends AppCompatActivity {

    private ListView remindersListView;
    private DBHelper dbHelper;
    private List<String> remindersList;
    private String petEmail, petSpecies, petDOB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        dbHelper = new DBHelper(this);

        petEmail = getSharedPreferences("UserSession", MODE_PRIVATE).getString("loggedInPetEmail", null);
        remindersListView = findViewById(R.id.reminders_list);
        remindersList = new ArrayList<>();

        if (petEmail != null) {
            fetchPetDetails();
            fetchUpcomingReminders();
            suggestVaccinations();
        } else {
            remindersList.add("No pet profile found.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, remindersList);
        remindersListView.setAdapter(adapter);
    }

    private void fetchPetDetails() {
        Cursor cursor = dbHelper.getPetDetails(petEmail);
        if (cursor != null && cursor.moveToFirst()) {
            petSpecies = cursor.getString(cursor.getColumnIndex("pet_species"));
            petDOB = cursor.getString(cursor.getColumnIndex("pet_dob"));
            cursor.close();
        }
    }

    private void fetchUpcomingReminders() {
        Cursor cursor = dbHelper.getUpcomingVaccinations(petEmail);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar today = Calendar.getInstance();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String vaccineName = cursor.getString(cursor.getColumnIndex("vaccine_name"));
                String nextDueDate = cursor.getString(cursor.getColumnIndex("next_due_date"));
                remindersList.add("Upcoming: " + vaccineName + " (Due: " + nextDueDate + ")");

                scheduleNotification(vaccineName, nextDueDate);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private void suggestVaccinations() {
        int petAgeInMonths = calculateAgeInMonths(petDOB);

        List<String> recommendedVaccines = getVaccinationSuggestions(petSpecies, petAgeInMonths);

        for (String vaccine : recommendedVaccines) {
            remindersList.add("💡 Suggested: " + vaccine);
        }
    }
    private int calculateAgeInMonths(String dob) {
        if (dob == null || dob.isEmpty()) return 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Calendar dobCalendar = Calendar.getInstance();
            dobCalendar.setTime(sdf.parse(dob));

            Calendar today = Calendar.getInstance();
            int ageInMonths = (today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)) * 12 +
                    today.get(Calendar.MONTH) - dobCalendar.get(Calendar.MONTH);

            return ageInMonths;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private List<String> getVaccinationSuggestions(String species, int ageInMonths) {
        List<String> suggestedVaccines = new ArrayList<>();

        if (species.equalsIgnoreCase("Dog")) {
            if (ageInMonths < 3) {
                suggestedVaccines.add("Distemper");
                suggestedVaccines.add("Parvovirus");
            } else if (ageInMonths < 6) {
                suggestedVaccines.add("Rabies");
            } else {
                suggestedVaccines.add("Annual Booster Shots");
            }
        } else if (species.equalsIgnoreCase("Cat")) {
            if (ageInMonths < 3) {
                suggestedVaccines.add("Feline Distemper");
                suggestedVaccines.add("Rhinotracheitis");
            } else if (ageInMonths < 6) {
                suggestedVaccines.add("Feline Leukemia");
            } else {
                suggestedVaccines.add("Annual Booster Shots");
            }   }
        return suggestedVaccines;
    }
    private void scheduleNotification(String vaccineName, String nextDueDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(nextDueDate + " 22:40"));// Set to 10:00 AM on the due date

            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("reminderText", "Upcoming: " + vaccineName + " (Due: " + nextDueDate + ")");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

            Log.d("ReminderActivity", "Scheduled notification for: " + nextDueDate + " at 10:00 AM");

        } catch (Exception e) {
            Log.e("ReminderActivity", "Error scheduling notification: " + e.getMessage());
        }
    }
} */