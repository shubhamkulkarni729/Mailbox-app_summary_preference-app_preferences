package com.cognition.android.mailboxapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.cognition.android.mailboxapp.R;
import com.cognition.android.mailboxapp.View_type;

public class PreferenceListActivity extends AppCompatActivity {

    CheckBox checkBoxPrimary;
    CheckBox checkBoxSocial;
    CheckBox checkBoxJobs;
    CheckBox checkBoxEducational;
    CheckBox checkBoxFinancial;
    CheckBox checkBoxOffers;
    Button btnSave;

    SharedPreferences sharedPref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference_list);

        checkBoxPrimary = (CheckBox)findViewById(R.id.checkBoxPrimary);
        checkBoxSocial = (CheckBox)findViewById(R.id.checkBoxSocial);
        checkBoxJobs = (CheckBox)findViewById(R.id.checkBoxJobs);
        checkBoxEducational = (CheckBox)findViewById(R.id.checkBoxEducational);
        checkBoxFinancial = (CheckBox)findViewById(R.id.checkBoxFinancial);
        checkBoxOffers = (CheckBox)findViewById(R.id.checkBoxOffers);

        btnSave = (Button)findViewById(R.id.btnSavePreferencesList);

        sharedPref = PreferenceListActivity.this.getSharedPreferences(getString(R.string.preferences_file_name), Context.MODE_PRIVATE);


        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPreferencesList();
            }
        });
    }

    private void setPreferencesList(){
        boolean primary = checkBoxPrimary.isChecked();
        boolean social = checkBoxSocial.isChecked();
        boolean jobs = checkBoxJobs.isChecked();
        boolean educational = checkBoxEducational.isChecked();
        boolean financial = checkBoxFinancial.isChecked();
        boolean offers = checkBoxOffers.isChecked();

        String preferencesList = new String();
        if(primary) preferencesList = preferencesList.concat("primary ");
        if(social) preferencesList = preferencesList.concat("social ");
        if(jobs) preferencesList = preferencesList.concat("jobs ");
        if(educational) preferencesList = preferencesList.concat("educational ");
        if(financial) preferencesList = preferencesList.concat("financial ");
        if(offers) preferencesList = preferencesList.concat("offers ");

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("preferencesList",preferencesList);
        editor.apply();

        Toast.makeText(PreferenceListActivity.this,"Saved",Toast.LENGTH_LONG).show();

        startActivity(new Intent(PreferenceListActivity.this, View_type.class));
        ActivityCompat.finishAffinity(PreferenceListActivity.this);
    }
}
