package com.example.petvactracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class firstpage extends AppCompatActivity {
    Button btn1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firstpage);

        btn1=findViewById(R.id.button);
        btn1.setOnClickListener(view -> startActivity(new Intent(this, loginpage.class)));
    }
}