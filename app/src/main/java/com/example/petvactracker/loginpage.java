package com.example.petvactracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class loginpage extends AppCompatActivity {

    private EditText etPetEmail, etPassword;
    private Button btnLogin;
    private TextView registertv;
    private DBHelper dbHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginpage);

        dbHelper = new DBHelper(this);
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        etPetEmail = findViewById(R.id.et_pet_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        registertv= findViewById(R.id.textView2);

        btnLogin.setOnClickListener(view -> {
            String petEmail = etPetEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (dbHelper.validateUser(petEmail, password)) {
                sharedPreferences.edit().putString("loggedInPetEmail", petEmail).apply(); // Store pet email
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Userdashboard.class));
            } else {
                Toast.makeText(this, "Invalid pet email or password", Toast.LENGTH_SHORT).show();
            }
        });
        registertv.setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));
    }
}