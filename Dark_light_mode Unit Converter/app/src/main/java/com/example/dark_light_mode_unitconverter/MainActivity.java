package com.example.dark_light_mode_unitconverter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    Spinner dropdown1, dropdown2;
    TextInputEditText editText, editText1;

    String[] units = {"Feet", "Inches", "Centimeters", "Meters", "Yards"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load saved theme preference
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views
        dropdown1 = findViewById(R.id.dropdown1);
        dropdown2 = findViewById(R.id.dropdown2);
        editText = findViewById(R.id.editText);
        editText1 = findViewById(R.id.editText1);

        // Spinner setup
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        dropdown1.setAdapter(adapter);
        dropdown2.setAdapter(adapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position1, long id) {
                convert();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        dropdown1.setOnItemSelectedListener(listener);
        dropdown2.setOnItemSelectedListener(listener);

        editText.setOnEditorActionListener((v, actionId, event) -> {
            convert();
            return false;
        });

        // Open settings activity when button is clicked
        findViewById(R.id.openSettingsBtn).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    void convert() {
        String from = dropdown1.getSelectedItem().toString();
        String to = dropdown2.getSelectedItem().toString();
        String inputStr = editText.getText().toString();

        if (inputStr.isEmpty()) {
            editText1.setText("");
            return;
        }

        double input = Double.parseDouble(inputStr);
        double meters = convertToMeters(input, from);
        double result = convertFromMeters(meters, to);

        editText1.setText(String.format("%.2f", result));
    }

    double convertToMeters(double value, String unit) {
        switch (unit) {
            case "Feet": return value * 0.3048;
            case "Inches": return value * 0.0254;
            case "Centimeters": return value / 100;
            case "Yards": return value * 0.9144;
            default: return value; // Meters
        }
    }

    double convertFromMeters(double value, String unit) {
        switch (unit) {
            case "Feet": return value / 0.3048;
            case "Inches": return value / 0.0254;
            case "Centimeters": return value * 100;
            case "Yards": return value / 0.9144;
            default: return value; // Meters
        }
    }
}
