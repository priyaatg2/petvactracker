package com.example.petvactracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PetVaccinationDB";
    private static final int DATABASE_VERSION = 6;
    private static final String TABLE_USERS = "users";
    private static final String TABLE_VACCINATIONS = "vaccinations";
    private static final String COLUMN_ID = "user_id";

    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_USER_EMAIL = "user_email";
    private static final String COLUMN_PET_EMAIL = "pet_email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_PHONE = "phone";

    private static final String COLUMN_PET_NAME = "pet_name";
    private static final String COLUMN_PET_SPECIES = "pet_species";
    private static final String COLUMN_PET_BREED = "pet_breed";
    private static final String COLUMN_PET_DOB = "pet_dob";
    // Vaccination Table Columns
    private static final String COLUMN_VACCINE_ID = "id";
    private static final String COLUMN_VACCINE_NAME = "vaccine_name";
    private static final String COLUMN_VACCINE_DATE = "vaccine_date";
    private static final String COLUMN_NEXT_DUE_DATE = "next_due_date";
    private static final String COLUMN_DOCTOR_NAME = "doctor_name"; // Added doctor name column
    private static final String COLUMN_CLINIC_AREA = "clinic_area";

    public DBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT, "
                + COLUMN_USER_EMAIL + " TEXT, "
                + COLUMN_PET_EMAIL + " TEXT UNIQUE, "
                + COLUMN_PASSWORD + " TEXT, "
                + COLUMN_PHONE + " TEXT, "
                + COLUMN_PET_NAME + " TEXT, "
                + COLUMN_PET_SPECIES + " TEXT, "
                + COLUMN_PET_BREED + " TEXT, "
                + COLUMN_PET_DOB + " TEXT)";
        db.execSQL(createUsersTable);

        String createVaccinationTable = "CREATE TABLE " + TABLE_VACCINATIONS + " ("
                + COLUMN_VACCINE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PET_EMAIL + " TEXT, "
                + COLUMN_VACCINE_NAME + " TEXT, "
                + COLUMN_VACCINE_DATE + " TEXT, "
                + COLUMN_NEXT_DUE_DATE + " TEXT, "
                + COLUMN_DOCTOR_NAME + " TEXT, "
                + COLUMN_CLINIC_AREA + " TEXT, "
                + "FOREIGN KEY (" + COLUMN_PET_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_PET_EMAIL + "))";
        db.execSQL(createVaccinationTable);
    }

    public boolean registerUser(String name, String userEmail, String petEmail, String password, String phone, String petName, String petSpecies, String petBreed, String petDOB) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_PET_EMAIL + " = ?", new String[]{petEmail});
        if (cursor.getCount() > 0) {
            cursor.close();
            return false; // Pet email must be unique
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_USER_EMAIL, userEmail);
        values.put(COLUMN_PET_EMAIL, petEmail);
        values.put(COLUMN_PASSWORD, hashPassword(password, petEmail));
        values.put(COLUMN_PHONE, phone);
        values.put(COLUMN_PET_NAME, petName);
        values.put(COLUMN_PET_SPECIES, petSpecies);
        values.put(COLUMN_PET_BREED, petBreed);
        values.put(COLUMN_PET_DOB, petDOB);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }
    public boolean validateUser(String petEmail, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT password FROM " + TABLE_USERS + " WHERE " + COLUMN_PET_EMAIL + "=?", new String[]{petEmail});

        if (cursor.moveToFirst()) {
            String storedHash = cursor.getString(0);
            String inputHash = hashPassword(password, petEmail);
            cursor.close();
            return storedHash.equals(inputHash);
        }
        cursor.close();
        return false;
    }

    private String hashPassword(String password, String salt) {
        try {
            int iterations = 10000;
            int keyLength = 256;
            char[] passwordChars = password.toCharArray();
            byte[] saltBytes = salt.getBytes();

            PBEKeySpec spec = new PBEKeySpec(passwordChars, saltBytes, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return android.util.Base64.encodeToString(hash, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VACCINATIONS);
        onCreate(db);
    }
    public boolean addVaccinationRecord(String petEmail, String vaccineName, String vaccineDate, String nextDueDate, String doctorName, String clinicArea) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PET_EMAIL, petEmail);
        values.put(COLUMN_VACCINE_NAME, vaccineName);
        values.put(COLUMN_VACCINE_DATE, vaccineDate);
        values.put(COLUMN_NEXT_DUE_DATE, nextDueDate);
        values.put(COLUMN_DOCTOR_NAME, doctorName);
        values.put(COLUMN_CLINIC_AREA, clinicArea);

        long result = db.insert(TABLE_VACCINATIONS, null, values);
        return result != -1;
    }

    public Cursor getPreviousVaccinations(String petEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT vaccine_name, vaccine_date, doctor_name, clinic_area FROM vaccinations WHERE pet_email=? ORDER BY vaccine_date DESC", new String[]{petEmail});
    }
    public Cursor getVaccinationRecords(String petEmail) {
        if (petEmail == null || petEmail.isEmpty()) {
            Log.e("DBHelper", "Error: petEmail is NULL when querying vaccinations!");
            return null;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("DBHelper", "Fetching vaccinations for petEmail: " + petEmail);
        return db.rawQuery("SELECT * FROM " + TABLE_VACCINATIONS + " WHERE " + COLUMN_PET_EMAIL + "=? ORDER BY " + COLUMN_VACCINE_ID + " DESC", new String[]{petEmail});
    }

    public Cursor getPetDetails(String petEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT pet_name, pet_species, pet_breed, pet_dob FROM " + TABLE_USERS + " WHERE " + COLUMN_PET_EMAIL + "=?", new String[]{petEmail});

        if (cursor != null && cursor.moveToFirst()) {
            Log.d("DBHelper", "Pet details retrieved for petEmail: " + petEmail);
        } else {
            Log.e("DBHelper", "No pet details found for petEmail: " + petEmail);
        }
        return cursor;
    }

    public Cursor getUpcomingVaccinations(String petEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT vaccine_name, next_due_date FROM vaccinations WHERE pet_email=? ORDER BY next_due_date ASC", new String[]{petEmail});
    }

    public boolean deleteVaccinationRecord(String vaccineName, String petEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete("vaccinations", "vaccine_name = ? AND pet_email = ?", new String[]{vaccineName, petEmail});
        db.close();
        return rowsAffected > 0; // Returns true if deletion was successful
    }

}