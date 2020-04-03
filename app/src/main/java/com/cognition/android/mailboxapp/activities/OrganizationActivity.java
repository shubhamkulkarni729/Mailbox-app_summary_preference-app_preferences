package com.cognition.android.mailboxapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cognition.android.mailboxapp.R;
import com.cognition.android.mailboxapp.View_type;

public class OrganizationActivity extends AppCompatActivity {

    TextView labelOrganization;
    EditText textOrganization;
    Button btnOrganizationSave;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organization);

        labelOrganization = (TextView)findViewById(R.id.labelOrganization);
        textOrganization = (EditText)findViewById(R.id.textOrganizationName);
        btnOrganizationSave = (Button)findViewById(R.id.btnSaveOrganization);

        sharedPref = OrganizationActivity.this.getSharedPreferences(getString(R.string.preferences_file_name), Context.MODE_PRIVATE);

        btnOrganizationSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(labelOrganization.getText().toString().equals(null)) {
                    Toast.makeText(OrganizationActivity.this,"Enter Name",Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("organizationName",textOrganization.getText().toString());
                    editor.apply();

                    //Toast.makeText(OrganizationActivity.this,"ORGANIZATION NAME: "+sharedPref.getString("organizationName","ORG"),Toast.LENGTH_LONG).show();

                    startActivity(new Intent(OrganizationActivity.this, PreferenceListActivity.class));
                    ActivityCompat.finishAffinity(OrganizationActivity.this);

                }
            }
        });

    }
}
