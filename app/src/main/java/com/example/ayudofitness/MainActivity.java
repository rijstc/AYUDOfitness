package com.example.ayudofitness;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    /*private static final String MYPREF = "myCustomSharedPref";
    private static final String PREF_KEY_FIRST_RUN = "firstrun";

    SharedPreferences customSharedPreferences;*/
    private Button buttonScan;
    private Button buttonPair;
    private Button buttonHeartRate;
    private Button buttonSteps;
    private Button buttonUser;
    private TextView textViewScan;
    private TextView textViewPair;
    private TextView textViewHeartRate;
    private TextView textViewSteps;
    private TextView textViewUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeView();
        initializeListeners();

       /* customSharedPreferences = getSharedPreferences(MYPREF, Context.MODE_PRIVATE);
        customSharedPreferences.registerOnSharedPreferenceChangeListener(this);*/

    }


    private void initializeView() {
        buttonScan = findViewById(R.id.buttonScan);
        buttonPair = findViewById(R.id.buttonPair);
        buttonHeartRate = findViewById(R.id.buttonHeartRate);
        buttonSteps = findViewById(R.id.buttonSteps);
        buttonUser = findViewById(R.id.buttonUser);
        textViewScan = findViewById(R.id.textViewScan);
        textViewPair = findViewById(R.id.textViewPair);
        textViewHeartRate = findViewById(R.id.textViewHeartRate);
        textViewSteps = findViewById(R.id.textViewSteps);
        textViewUser = findViewById(R.id.textViewUser);
    }

    private void initializeListeners() {
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        buttonPair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        buttonHeartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        buttonSteps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        buttonUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }
}