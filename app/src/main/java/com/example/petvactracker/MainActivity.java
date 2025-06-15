package com.example.petvactracker;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etEmail,etPetEmail, etPassword, etPhone, etPetName, etPetBreed, etPetDOB;
    private Spinner spinnerSpecies, spinnerContactMethod;
    private Button btnRegister;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPetEmail = findViewById(R.id.et_pet_email);
        etPassword = findViewById(R.id.et_password);
        etPhone = findViewById(R.id.et_phone);
        etPetName = findViewById(R.id.et_pet_name);
        etPetBreed = findViewById(R.id.et_pet_breed);
        etPetDOB = findViewById(R.id.et_pet_dob);
        spinnerSpecies = findViewById(R.id.spinner_species);
        btnRegister = findViewById(R.id.btn_register);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.species_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpecies.setAdapter(adapter);

        etPetDOB.setOnClickListener(view -> showDatePickerDialog());

        btnRegister.setOnClickListener(view -> {
            if (etName.getText().toString().isEmpty() || etEmail.getText().toString().isEmpty() || etPetEmail.getText().toString().isEmpty() || etPassword.getText().toString().isEmpty() || etPhone.getText().toString().isEmpty() || etPetName.getText().toString().isEmpty() || etPetBreed.getText().toString().isEmpty() || etPetDOB.getText().toString().isEmpty() || spinnerSpecies.getSelectedItem().toString().isEmpty()) {
                Toast.makeText(this, "Enter all details!", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String petEmail = etPetEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String petName = etPetName.getText().toString().trim();
            String petSpecies = spinnerSpecies.getSelectedItem().toString().trim();
            String petBreed = etPetBreed.getText().toString().trim().toUpperCase();
            String petDOB = etPetDOB.getText().toString().trim();

            etPetBreed.setText(petBreed);
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Enter a valid email!", Toast.LENGTH_SHORT).show();
                    return;
                }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(petEmail).matches()) {
                Toast.makeText(this, "Enter a valid pet email!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.matches("\\d{8}")) {
                    Toast.makeText(this, "Password must be an 8-digit number!", Toast.LENGTH_SHORT).show();
                    return;
                }
            if (!phone.matches("\\d{10}")) {
                    Toast.makeText(this, "Phone number must be 10 digits!", Toast.LENGTH_SHORT).show();
                    return;
                }


            if (!dbHelper.registerUser(name, email, petEmail,password, phone, petName, petSpecies, petBreed, petDOB)) {
                Toast.makeText(this, "Pet Email ID already exists!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Registration successfull!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, loginpage.class));
            }   });
    }
    private void showDatePickerDialog(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    etPetDOB.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.show();
    }
}