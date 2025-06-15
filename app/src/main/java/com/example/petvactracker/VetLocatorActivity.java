package com.example.petvactracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.LocationRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class VetLocatorActivity extends AppCompatActivity {

    private Button btnFetchLocation;
    private ListView vetListView, previousVaccinationsList;
    private ArrayList<String> vetList, vaccinationList;
    private ArrayAdapter<String> vetAdapter, vaccinationAdapter;
    private DBHelper dbHelper;
    private FusedLocationProviderClient locationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_locator);

        dbHelper = new DBHelper(this);

        btnFetchLocation = findViewById(R.id.btnFetchLocation);
        vetListView = findViewById(R.id.vetListView);
        previousVaccinationsList = findViewById(R.id.previousVaccinationsList);

        vetList = new ArrayList<>();
        vaccinationList = new ArrayList<>();

        vetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vetList);
        vaccinationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vaccinationList);

        vetListView.setAdapter(vetAdapter);
        previousVaccinationsList.setAdapter(vaccinationAdapter);

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        btnFetchLocation.setOnClickListener(view -> getCurrentLocation());

        vetListView.setOnItemClickListener((parent, view, position, id) -> {
            String vetDetails = vetList.get(position);
            openGoogleMaps(vetDetails);
        });

        previousVaccinationsList.setOnItemClickListener((parent, view, position, id) -> {
            String clinicDetails = vaccinationList.get(position);
            openGoogleMaps(clinicDetails);
        });

        fetchPreviousVaccinations();
    }

    private void getCurrentLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);

        // 🌍 First Attempt: Get Last Known Location
        locationClient.getLastLocation().addOnCompleteListener(task -> {
            Location location = task.getResult();
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d("VetLocatorActivity", "User's GPS Location: " + latitude + ", " + longitude);
                fetchVetClinics(latitude, longitude);
            } else {
                Log.e("VetLocatorActivity", "Failed to get last location. Requesting fresh location update...");
                requestNewLocation(locationClient);  // Fallback to real-time GPS
            }
        });
    }

    // 🌍 Second Attempt: Request Fresh Location If First Attempt Fails
    private void requestNewLocation(FusedLocationProviderClient locationClient) {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e("VetLocatorActivity", "Failed to retrieve fresh location!");
                    Toast.makeText(VetLocatorActivity.this, "Unable to retrieve GPS location!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Location location = locationResult.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d("VetLocatorActivity", "Updated GPS Location: " + latitude + ", " + longitude);
                fetchVetClinics(latitude, longitude);
            }
        };

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }



    private void fetchPreviousVaccinations() {
        String petEmail = getSharedPreferences("UserSession", MODE_PRIVATE).getString("loggedInPetEmail", null);
        if (petEmail == null) {
            vaccinationList.add("No pet email found. Cannot retrieve past vaccinations.");
            vaccinationAdapter.notifyDataSetChanged();
            return;
        }

        Cursor cursor = dbHelper.getPreviousVaccinations(petEmail);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String vaccineName = cursor.getString(cursor.getColumnIndex("vaccine_name"));
                String doctorName = cursor.getString(cursor.getColumnIndex("doctor_name"));
                String clinicArea = cursor.getString(cursor.getColumnIndex("clinic_area"));

                vaccinationList.add("💉 " + vaccineName + "\n👨‍⚕️ Dr. " + doctorName + "\n📍 " + clinicArea);
            } while (cursor.moveToNext());
        } else {
            vaccinationList.add("No previous vaccinations found.");
        }

        if (cursor != null) {
            cursor.close();
        }

        vaccinationAdapter.notifyDataSetChanged();
    }

    private void fetchVetClinics(double latitude, double longitude) {
        new Thread(() -> {
            try {
                String urlString = "https://overpass-api.de/api/interpreter?data=[out:json];node[amenity=veterinary](around:30000," + latitude + "," + longitude + ");out;";
                Log.d("VetLocatorActivity", "Fetching vet clinics from: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("VetLocatorActivity", "API Response: " + response.toString());

                JSONArray results = new JSONObject(response.toString()).getJSONArray("elements");

                vetList.clear();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject vet = results.getJSONObject(i);
                    String name = vet.optJSONObject("tags") != null && vet.optJSONObject("tags").has("name")
                            ? vet.getJSONObject("tags").optString("name", "Unknown Clinic") : "Unnamed Veterinary Clinic";
                    vetList.add(name);
                }

                // ✅ Apply UI Refresh After Updating Vet List
                runOnUiThread(() -> {
                    vetAdapter.notifyDataSetChanged(); // Refresh ListView Data
                    vetListView.invalidateViews(); // Force ListView to Re-render
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("VetLocatorActivity", "Error fetching vet clinics: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(VetLocatorActivity.this, "Failed to fetch vet clinics!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void openGoogleMaps(String clinicDetails) {
        String query = Uri.encode(clinicDetails);
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + query);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }
}

/* package com.example.petvactracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class VetLocatorActivity extends AppCompatActivity {

    private EditText searchLocation;
    private Button btnSearch;
    private ListView vetListView, previousVaccinationsList;
    private ArrayList<String> vetList, vaccinationList;
    private ArrayAdapter<String> vetAdapter, vaccinationAdapter;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_locator);

        dbHelper = new DBHelper(this);

        searchLocation = findViewById(R.id.searchLocation);
        btnSearch = findViewById(R.id.btnSearch);
        vetListView = findViewById(R.id.vetListView);
        previousVaccinationsList = findViewById(R.id.previousVaccinationsList);

        vetList = new ArrayList<>();
        vaccinationList = new ArrayList<>();

        vetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vetList);
        vaccinationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vaccinationList);

        vetListView.setAdapter(vetAdapter);
        previousVaccinationsList.setAdapter(vaccinationAdapter);

        btnSearch.setOnClickListener(view -> {
            String location = searchLocation.getText().toString().trim();
            Log.d("VetLocatorActivity", "Searching for vet clinics in: " + location);
            fetchVetClinics(location);
        });

        vetListView.setOnItemClickListener((parent, view, position, id) -> {
            String vetDetails = vetList.get(position);
            openGoogleMaps(vetDetails);
        });

        previousVaccinationsList.setOnItemClickListener((parent, view, position, id) -> {
            String clinicDetails = vaccinationList.get(position);
            openGoogleMaps(clinicDetails);
        });

        fetchPreviousVaccinations();
    }

    private void fetchPreviousVaccinations() {
        String petEmail = getSharedPreferences("UserSession", MODE_PRIVATE).getString("loggedInPetEmail", null);
        if (petEmail == null) {
            vaccinationList.add("No pet email found. Cannot retrieve past vaccinations.");
            vaccinationAdapter.notifyDataSetChanged();
            return;
        }

        Cursor cursor = dbHelper.getPreviousVaccinations(petEmail);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String vaccineName = cursor.getString(cursor.getColumnIndex("vaccine_name"));
                String doctorName = cursor.getString(cursor.getColumnIndex("doctor_name"));
                String clinicArea = cursor.getString(cursor.getColumnIndex("clinic_area"));

                vaccinationList.add("💉 " + vaccineName + "\n👨‍⚕️ Dr. " + doctorName + "\n📍 " + clinicArea);
            } while (cursor.moveToNext());
        } else {
            vaccinationList.add("No previous vaccinations found.");
        }

        if (cursor != null) {
            cursor.close();
        }

        vaccinationAdapter.notifyDataSetChanged();
    }

    private void fetchVetClinics(String location) {
        if (location.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a location!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String urlString = "https://nominatim.openstreetmap.org/search?format=json&q=veterinary+care+in+" + Uri.encode(location);
                Log.d("VetLocatorActivity", "Fetching vet clinics from: " + urlString); // Debug log

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("VetLocatorActivity", "API Response: " + response.toString()); // Debug log

                JSONArray results = new JSONArray(response.toString());

                vetList.clear();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject vet = results.getJSONObject(i);
                    String name = vet.optString("display_name", "Unknown Clinic");
                    vetList.add(name);
                }

                runOnUiThread(() -> vetAdapter.notifyDataSetChanged());

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("VetLocatorActivity", "Error fetching vet clinics: " + e.getMessage()); // Debug log
                runOnUiThread(() -> Toast.makeText(VetLocatorActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void openGoogleMaps(String clinicDetails) {
        String query = Uri.encode(clinicDetails);
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + query);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }
} */