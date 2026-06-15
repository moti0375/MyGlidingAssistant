package com.dunihuliapps.myglidingassistnat;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dunihuliapps.myglidingassistant.R;

public class GpsRecLicense extends AppCompatActivity {

    TextView tvLicense;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.googlemap_license);
        tvLicense = findViewById(R.id.gps_license_text);
        toolbar = findViewById(R.id.app_bar);
        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
            // WTF SAMSUNG!
        }        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_back);


//        String license = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
//
//        if (license != null) {
//            tvLicense.setText(license);
//        } else {
//            tvLicense.setText(getResources().getString(R.string.GooglePlayServicesUnavailable));
//        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            finish();
        }
        return true;
    }
}
