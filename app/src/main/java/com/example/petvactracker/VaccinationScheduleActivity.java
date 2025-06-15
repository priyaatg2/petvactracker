package com.example.petvactracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class VaccinationScheduleActivity extends AppCompatActivity {

    private ListView vaccinationRecordsView;
    private DBHelper dbHelper;
    private EditText inputVaccineName, inputVaccineDate, inputNextDueDate, inputDoctorName, inputClinicArea;
    private Button btnAddVaccination;
    private String petEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vaccination_schedule);

        dbHelper = new DBHelper(this);
        vaccinationRecordsView = findViewById(R.id.vaccinationRecordsView);

        // Retrieve pet email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        petEmail = sharedPreferences.getString("loggedInPetEmail", null);

        if (petEmail == null || petEmail.isEmpty()) {
            Log.e("VaccinationScheduleActivity", "Error: No pet email found!");
            return;
        }

        // Initialize UI elements
        inputVaccineName = findViewById(R.id.inputVaccineName);
        inputVaccineDate = findViewById(R.id.inputVaccineDate);
        inputNextDueDate = findViewById(R.id.inputNextDueDate);
        inputDoctorName = findViewById(R.id.inputDoctorName);
        inputClinicArea = findViewById(R.id.inputClinicArea);
        btnAddVaccination = findViewById(R.id.btnAddVaccination);

        // Setup DatePicker for Date Fields
        inputVaccineDate.setOnClickListener(view -> showDatePickerDialog(inputVaccineDate));
        inputNextDueDate.setOnClickListener(view -> showDatePickerDialog(inputNextDueDate));

        // Load vaccination records
        loadVaccinationRecords();

        // Add vaccination record when button is clicked
        btnAddVaccination.setOnClickListener(view -> addVaccinationRecord());
    }

    private void showDatePickerDialog(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    targetEditText.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.show();
    }

    private void addVaccinationRecord() {
        String vaccineName = inputVaccineName.getText().toString().trim();
        String vaccineDate = inputVaccineDate.getText().toString().trim();
        String nextDueDate = inputNextDueDate.getText().toString().trim();
        String doctorName = inputDoctorName.getText().toString().trim();
        String clinicArea = inputClinicArea.getText().toString().trim();

        if (petEmail == null || petEmail.isEmpty()) {
            Toast.makeText(this, "Error: Cannot store records without pet email!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (vaccineName.isEmpty() || vaccineDate.isEmpty() || nextDueDate.isEmpty() || doctorName.isEmpty() || clinicArea.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isInserted = dbHelper.addVaccinationRecord(petEmail, vaccineName, vaccineDate, nextDueDate, doctorName, clinicArea);
        if (isInserted) {
            Toast.makeText(this, "Record saved successfully!", Toast.LENGTH_SHORT).show();

            // Clear input fields after record insertion
            inputVaccineName.setText("");
            inputVaccineDate.setText("");
            inputNextDueDate.setText("");
            inputDoctorName.setText("");
            inputClinicArea.setText("");

            loadVaccinationRecords(); // Refresh records list
        } else {
            Toast.makeText(this, "Failed to save record!", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadVaccinationRecords() {
        Cursor cursor = dbHelper.getVaccinationRecords(petEmail);
        List<String> records = new ArrayList<>();
        final List<String> vaccineNames = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String vaccineName = cursor.getString(cursor.getColumnIndex("vaccine_name"));
                String vaccineDate = cursor.getString(cursor.getColumnIndex("vaccine_date"));
                String nextDueDate = cursor.getString(cursor.getColumnIndex("next_due_date"));
                String doctorName = cursor.getString(cursor.getColumnIndex("doctor_name"));
                String clinicArea = cursor.getString(cursor.getColumnIndex("clinic_area"));

                // Include all details in the record display
                records.add("💉 Vaccine: " + vaccineName +
                        "\n📅 Date: " + vaccineDate +
                        "\n🔜 Next Due: " + nextDueDate +
                        "\n👨‍⚕️ Doctor: " + doctorName +
                        "\n📍 Clinic Area: " + clinicArea +
                        "\n\nTap and hold to delete");

                vaccineNames.add(vaccineName);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            records.add("No vaccination records found.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, records);
        vaccinationRecordsView.setAdapter(adapter);

        // Set long-click listener for deleting a record
        vaccinationRecordsView.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedVaccine = vaccineNames.get(position);
            confirmDeleteRecord(selectedVaccine);
            return true;
        });
    }
    private void confirmDeleteRecord(String vaccineName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete " + vaccineName + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean isDeleted = dbHelper.deleteVaccinationRecord(vaccineName, petEmail);
                    if (isDeleted) {
                        Toast.makeText(this, "Record deleted successfully!", Toast.LENGTH_SHORT).show();
                        loadVaccinationRecords(); // Refresh list after deletion
                    } else {
                        Toast.makeText(this, "Failed to delete record!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }   }

/*package com.example.petvactracker;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;

public class VaccinationScheduleActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private EditText inputVaccineName, inputVaccineDate, inputNextDueDate, inputDoctorName, inputClinicArea;
    private Button btnAddVaccination;
    private TextView vaccinationRecordsView;
    private String petEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vaccination_schedule);

        dbHelper = new DBHelper(this);

        // Retrieve pet email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        petEmail = sharedPreferences.getString("loggedInPetEmail", null);

        if (petEmail == null || petEmail.isEmpty()) {
            Log.e("VaccinationScheduleActivity", "Error: No pet email found!");
            vaccinationRecordsView = findViewById(R.id.vaccinationRecords);
            vaccinationRecordsView.setText("Error: No pet email found.");
            return; // Prevent execution if pet email is missing
        }

        // Initialize UI elements
        vaccinationRecordsView = findViewById(R.id.vaccinationRecords);
        inputVaccineName = findViewById(R.id.inputVaccineName);
        inputVaccineDate = findViewById(R.id.inputVaccineDate);
        inputNextDueDate = findViewById(R.id.inputNextDueDate);
        inputDoctorName = findViewById(R.id.inputDoctorName);
        inputClinicArea = findViewById(R.id.inputClinicArea);
        btnAddVaccination = findViewById(R.id.btnAddVaccination);

        // Setup DatePicker for Date Fields
        inputVaccineDate.setOnClickListener(view -> showDatePickerDialog(inputVaccineDate));
        inputNextDueDate.setOnClickListener(view -> showDatePickerDialog(inputNextDueDate));

        // Load vaccination records
        loadVaccinationRecords();

        // Add vaccination record when button is clicked
        btnAddVaccination.setOnClickListener(view -> addVaccinationRecord());
    }

    private void showDatePickerDialog(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    targetEditText.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.show();
    }

    private void addVaccinationRecord() {
        String vaccineName = inputVaccineName.getText().toString().trim();
        String vaccineDate = inputVaccineDate.getText().toString().trim();
        String nextDueDate = inputNextDueDate.getText().toString().trim();
        String doctorName = inputDoctorName.getText().toString().trim();
        String clinicArea = inputClinicArea.getText().toString().trim();

        if (petEmail == null || petEmail.isEmpty()) {
            Log.e("VaccinationScheduleActivity", "Error: Pet email is missing when saving record!");
            vaccinationRecordsView.setText("Error: Cannot store records without pet email!");
            return;
        }

        if (vaccineName.isEmpty() || vaccineDate.isEmpty() || nextDueDate.isEmpty() || doctorName.isEmpty() || clinicArea.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isInserted = dbHelper.addVaccinationRecord(petEmail, vaccineName, vaccineDate, nextDueDate, doctorName, clinicArea);
        if (isInserted) {
            vaccinationRecordsView.setText("Record saved successfully!");
            inputVaccineName.setText("");
            inputVaccineDate.setText("");
            inputNextDueDate.setText("");
            inputDoctorName.setText("");
            inputClinicArea.setText("");
            loadVaccinationRecords();
        } else {
            vaccinationRecordsView.setText("Failed to save record!");
        }
    }

    private void loadVaccinationRecords() {
        Cursor cursor = dbHelper.getVaccinationRecords(petEmail);
        StringBuilder records = new StringBuilder();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                records.append("💉 Vaccine: ").append(cursor.getString(cursor.getColumnIndex("vaccine_name")))
                        .append("\n📅 Date: ").append(cursor.getString(cursor.getColumnIndex("vaccine_date")))
                        .append("\n🔜 Next Due: ").append(cursor.getString(cursor.getColumnIndex("next_due_date")))
                        .append("\n👨‍⚕️ Doctor: ").append(cursor.getString(cursor.getColumnIndex("doctor_name")))
                        .append("\n📍 Clinic Area: ").append(cursor.getString(cursor.getColumnIndex("clinic_area")))
                        .append("\n\n");
            } while (cursor.moveToNext());
        } else {
            records.append("No vaccination records found.");
        }

        if (cursor != null) {
            cursor.close();
        }

        vaccinationRecordsView.setText(records.toString());
    }
} */