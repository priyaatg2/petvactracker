package com.example.petvactracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Userdashboard extends AppCompatActivity {

    private CardView petProfileCard, vaccinationScheduleCard, remindersCard, vetLocatorCard;
    private Button logout, buttonFeedback;
    private DBHelper dbHelper;
    private String userEmail; // Stores logged-in user's email

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userdashboard);

        dbHelper = new DBHelper(this);

        petProfileCard = findViewById(R.id.petProfileCard);
        vaccinationScheduleCard = findViewById(R.id.vaccinationScheduleCard);
        remindersCard = findViewById(R.id.remindersCard);
        vetLocatorCard = findViewById(R.id.vetLocatorCard);
        logout = findViewById(R.id.button2);
        buttonFeedback = findViewById(R.id.buttonFeedback); // Feedback Button

        // Fetch user email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        userEmail = sharedPreferences.getString("loggedInPetEmail", null);

        // Set click listeners to navigate to respective pages
        petProfileCard.setOnClickListener(view -> startActivity(new Intent(this, PetProfileActivity.class)));
        vaccinationScheduleCard.setOnClickListener(view -> startActivity(new Intent(this, VaccinationScheduleActivity.class)));
        remindersCard.setOnClickListener(view -> startActivity(new Intent(this, RemindersActivity.class)));
        vetLocatorCard.setOnClickListener(view -> startActivity(new Intent(this, VetLocatorActivity.class)));

        logout.setOnClickListener(view -> {
            Intent intent = new Intent(Userdashboard.this, loginpage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears back stack
            startActivity(intent);
            finish(); // Ensures Userdashboard is closed completely
        });

        // Handle Feedback Button Click
        buttonFeedback.setOnClickListener(view -> openFeedbackPopup());
    }

    private void openFeedbackPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Submit Feedback");

        // Create input field
        final EditText inputFeedback = new EditText(this);
        inputFeedback.setHint("Enter your feedback...");
        builder.setView(inputFeedback);

        // Submit Feedback
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String feedbackText = inputFeedback.getText().toString().trim();
            if (!feedbackText.isEmpty()) {
                sendFeedbackToAdmin(userEmail, feedbackText);
                Toast.makeText(this, "Feedback submitted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Feedback cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void sendFeedbackToAdmin(String userEmail, String feedback) {
        String adminEmail = "halwaipriyad@gmail.com"; // Your email
        String subject = "User Feedback of pet email as : " + userEmail;
        String message = "User Pet Email: " + userEmail + "\nFeedback: " + feedback;

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{adminEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send feedback via..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

}