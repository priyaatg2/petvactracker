package com.example.petvactracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PetProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView petImageView;
    private Button btnUploadImage;
    private TextView petNameTextView, petBreedTextView, petSpeciesTextView, petDOBTextView;
    private DBHelper dbHelper;
    private String petEmail;
    private SharedPreferences sharedPreferences;
    private File petImageFile; // Store image as a file

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_profile);

        dbHelper = new DBHelper(this);
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        petEmail = sharedPreferences.getString("loggedInPetEmail", null);

        // Initialize UI elements
        petImageView = findViewById(R.id.petImageView);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        petNameTextView = findViewById(R.id.petNameTextView);
        petSpeciesTextView = findViewById(R.id.petSpeciesTextView);
        petBreedTextView = findViewById(R.id.petBreedTextView);
        petDOBTextView = findViewById(R.id.petDOBTextView);

        // Load saved pet details & profile image
        loadPetDetails();

        // Image Upload Button Click Listener
        btnUploadImage.setOnClickListener(view -> openGallery());
    }

    // Opens Gallery for Image Selection
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            petImageView.setImageURI(imageUri);
            saveImageToFile(imageUri);
            Toast.makeText(this, "Pet image saved successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    // Saves Image as a File in Internal Storage
    private void saveImageToFile(Uri imageUri) {
        try {
            // Open InputStream from selected image URI
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            petImageFile = new File(getFilesDir(), "pet_profile_image.jpg"); // Store image locally
            FileOutputStream outputStream = new FileOutputStream(petImageFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            // Save file path instead of URI
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("petImagePath", petImageFile.getAbsolutePath());
            editor.apply();
            Log.d("PetProfileActivity", "Saved image path: " + petImageFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("PetProfileActivity", "Error saving image: " + e.getMessage());
        }
    }

    // Loads Pet Details & Image from Internal Storage
    private void loadPetDetails() {
        if (petEmail != null) {
            Cursor cursor = dbHelper.getPetDetails(petEmail);
            if (cursor != null && cursor.moveToFirst()) {
                petNameTextView.setText(cursor.getString(cursor.getColumnIndex("pet_name")));
                petBreedTextView.setText(cursor.getString(cursor.getColumnIndex("pet_breed")));
                petSpeciesTextView.setText(cursor.getString(cursor.getColumnIndex("pet_species")));
                petDOBTextView.setText(cursor.getString(cursor.getColumnIndex("pet_dob")));
                cursor.close();
            } else {
                petNameTextView.setText("No pet details found.");
            }
        } else {
            petNameTextView.setText("No pet profile found.");
        }

        // Load saved pet image from file
        String savedImagePath = sharedPreferences.getString("petImagePath", null);
        if (savedImagePath != null) {
            petImageFile = new File(savedImagePath);
            if (petImageFile.exists()) {
                petImageView.setImageURI(Uri.fromFile(petImageFile)); // Securely load image
            }
        } else {
            petImageView.setImageResource(R.drawable.default_pet_image); // Placeholder image
        }
    }
}


/*package com.example.petvactracker;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PetProfileActivity extends AppCompatActivity {

    private TextView petNameTextView, petBreedTextView, petSpeciesTextView, petDOBTextView;
    private DBHelper dbHelper;
    private String petEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_profile);

        dbHelper = new DBHelper(this);

        // Retrieve logged-in pet's email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        petEmail = sharedPreferences.getString("loggedInPetEmail", null); // Updated to pet email

        // Initialize TextViews
        petNameTextView = findViewById(R.id.petNameTextView);
        petBreedTextView = findViewById(R.id.petBreedTextView);
        petSpeciesTextView = findViewById(R.id.petSpeciesTextView);
        petDOBTextView = findViewById(R.id.petDOBTextView);

        // Load pet details if pet email exists
        if (petEmail != null) {
            loadPetDetails();
        } else {
            petNameTextView.setText("No pet profile found");
        }
    }

    private void loadPetDetails() {
        Cursor cursor = dbHelper.getPetDetails(petEmail); // Updated to use pet email
        if (cursor != null && cursor.moveToFirst()) {
            petNameTextView.setText(cursor.getString(cursor.getColumnIndex("pet_name")));
            petBreedTextView.setText(cursor.getString(cursor.getColumnIndex("pet_breed")));
            petSpeciesTextView.setText(cursor.getString(cursor.getColumnIndex("pet_species")));
            petDOBTextView.setText(cursor.getString(cursor.getColumnIndex("pet_dob")));
            cursor.close();
        } else {
            petNameTextView.setText("No pet details found.");
        }
    }
}

..package com.example.petvactracker;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PetProfileActivity extends AppCompatActivity {

    private TextView petNameTextView, petBreedTextView, petSpeciesTextView, petDOBTextView;
    private DBHelper dbHelper;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_profile);

        dbHelper = new DBHelper(this);

        // Retrieve logged-in user's email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        userEmail = sharedPreferences.getString("loggedInEmail", null);

        // Initialize TextViews
        petNameTextView = findViewById(R.id.petNameTextView);
        petBreedTextView = findViewById(R.id.petBreedTextView);
        petSpeciesTextView = findViewById(R.id.petSpeciesTextView);
        petDOBTextView = findViewById(R.id.petDOBTextView);

        // Load pet details if email exists
        if (userEmail != null) {
            loadPetDetails();
        } else {
            petNameTextView.setText("No user logged in");
        }
    }

    private void loadPetDetails() {
        Cursor cursor = dbHelper.getPetDetails(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            petNameTextView.setText(cursor.getString(cursor.getColumnIndex("pet_name")));
            petBreedTextView.setText(cursor.getString(cursor.getColumnIndex("pet_breed")));
            petSpeciesTextView.setText(cursor.getString(cursor.getColumnIndex("pet_species")));
            petDOBTextView.setText(cursor.getString(cursor.getColumnIndex("pet_dob")));
            cursor.close();
        }
    }
} */
