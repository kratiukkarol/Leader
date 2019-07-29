package com.kratiukkarol.leader;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(isServiceOK()){
            addButtons();
        }
    }

    public void addButtons(){
        Button schedulerButton = findViewById(R.id.scheduler_button);
        schedulerButton.setOnClickListener(this);
        Button runningButton = findViewById(R.id.running_button);
        runningButton.setOnClickListener(this);
        Button cyclingButton = findViewById(R.id.cycling_button);
        cyclingButton.setOnClickListener(this);
        Button swimmingButton = findViewById(R.id.swimming_button);
        swimmingButton.setOnClickListener(this);
        Button strengthButton = findViewById(R.id.strength_button);
        strengthButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.scheduler_button:
                Intent schedulerIntent = new Intent(this, SchedulerActivity.class);
                startActivity(schedulerIntent);
                break;
            case R.id.running_button:
                Intent runningIntent = new Intent(this, RunningActivity.class);
                startActivity(runningIntent);
                break;
            case R.id.cycling_button:
                Intent cyclingIntent = new Intent(this, CyclingActivity.class);
                startActivity(cyclingIntent);
                break;
            case R.id.swimming_button:
                Intent swimmingIntent = new Intent(this, SwimmingActivity.class);
                startActivity(swimmingIntent);
                break;
            case R.id.strength_button:
                Intent strengthIntent = new Intent(this, StrengthActivity.class);
                startActivity(strengthIntent);
                break;
        }
    }

    public boolean isServiceOK(){
        Log.d(TAG, "isServiceOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServiceOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG, "isServiceOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

}
