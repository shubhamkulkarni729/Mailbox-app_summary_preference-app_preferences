package com.cognition.android.mailboxapp.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.cognition.android.mailboxapp.R;

public class SummaryActivity extends AppCompatActivity {
    TextView txtSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        String summary = getIntent().getStringExtra("summary");

        Toast.makeText(SummaryActivity.this,"This is Summary Activity",Toast.LENGTH_LONG).show();

        txtSummary = (TextView) findViewById(R.id.txtSummary1);

        txtSummary.setText(summary);
    }
}
