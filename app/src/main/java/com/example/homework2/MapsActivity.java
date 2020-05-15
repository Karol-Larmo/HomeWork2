package com.example.homework2;

import androidx.core.app.ActivityCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener, SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    static private SensorManager mSensorManaer;
    private Sensor sensor;
    private TextView text_sensor;
    Boolean button_show;
    Boolean recording;

    private GoogleMap mMap; // przetrzymuje zaladowaną mapę
    private FusedLocationProviderClient fusedLocationClient; //dostęp do serwisu lokacji
    private LocationRequest mLocationRequest; // update lokalizacji z lokalnych serwisów
    private LocationCallback locationCallback; // update lokalizacji (globalby ?)
    Marker gpsMarker = null; // current position of the device
    List<Marker> markerList;
    //zapis
    private final String JSON_FILE = "markers.json";
    List<Double> save_double_latitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markerList = new ArrayList<>();
        save_double_latitude = new ArrayList<>();

        //Sensory
        text_sensor = findViewById(R.id.text_ac);
        mSensorManaer = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManaer.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {

            sensor = mSensorManaer.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            Toast.makeText(getApplicationContext(), R.string.no_accelerometer, Toast.LENGTH_SHORT).show();
        }

        //Animacje

        FloatingActionButton fab_h = findViewById(R.id.fab_hide);
        fab_h.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FlingAnimation flingAnimation = new FlingAnimation(findViewById(R.id.buttons), DynamicAnimation.SCROLL_Y);
                flingAnimation.setStartVelocity(-1000).setMinValue(0).setFriction(1.1f).start();
                flingAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
               Hide_Buttons();
            } });

        FloatingActionButton fab_r = findViewById(R.id.fab_recording);
        fab_r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Show_Sensor();
            } });


        button_show = false;
        recording = false;



    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        if(sensor != null)
            mSensorManaer.unregisterListener(this,sensor);
    }

    @Override
    protected void onDestroy() {

        try {
            saveMarkersToJson();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJson();
    }

    private void stopLocationUpdates() {
        if (locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();


        createLocationRequest();
        createLocationCallBack();
        startLocationUpdates();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {


        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                .alpha(0.8f)
                .title(String.format("Position(%.3f, %.3f) ", latLng.latitude, latLng.longitude)));

        markerList.add(marker);

        save_double_latitude.add(latLng.latitude);
        save_double_latitude.add(latLng.longitude);


    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.getUiSettings().setMapToolbarEnabled(false);
        CameraPosition cameraPos = mMap.getCameraPosition();
        if (cameraPos.zoom < 14f)
            mMap.moveCamera(CameraUpdateFactory.zoomTo(7f));

        button_show = true;

            FlingAnimation flingAnimation = new FlingAnimation(findViewById(R.id.buttons), DynamicAnimation.SCROLL_Y);
            flingAnimation.setStartVelocity(1000).setMinValue(-300).setMaxValue(200).setFriction(1.1f).start();
            flingAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);





        return false;
    }

    public void Show_Sensor()
    {
            recording = !recording;
            if(recording == true)
            {
                FlingAnimation flingAnimation = new FlingAnimation(findViewById(R.id.text_ac), DynamicAnimation.TRANSLATION_Y);
                flingAnimation.setStartVelocity(300).setMinValue(0).setMaxValue(200).setFriction(0.5f).start();
                flingAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);

            } else {
                FlingAnimation flingAnimation = new FlingAnimation(findViewById(R.id.text_ac), DynamicAnimation.TRANSLATION_Y);
                flingAnimation.setStartVelocity(-300).setMinValue(0).setMaxValue(150).setFriction(0.5f).start();
                flingAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);

            }
    }
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        double X = event.values[0];
        double Y = event.values[1];
        text_sensor.setText(String.format("\t\t\tAcceleration: \n \tX: %.5f\t Y: %.5f\t", X,Y));

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(sensor!=null)
            mSensorManaer.registerListener(this,sensor,100000);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); // in ms
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    if (gpsMarker != null)
                        gpsMarker.remove();

                    Location location = locationResult.getLastLocation();
                    /*          gpsMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                            .alpha(0.8f)
                            .title("Current Location"));
                    */

                }
            }
        };
    }

    private void saveMarkersToJson() throws JSONException {
        Gson gson = new Gson();
        String listJson = gson.toJson(save_double_latitude);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void restoreFromJson() {
        FileInputStream inputStream;
        int size = 1000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[size];
            int n;
            StringBuilder builder = new StringBuilder();

            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < size) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collecionType = new TypeToken<List<Double>>() {}.getType();
            List<Double> o = gson.fromJson(readJson, collecionType);
            save_double_latitude.clear();
            if (o != null) {
                save_double_latitude.addAll(o);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        markerList.clear();


        try {
            for (int i = 0; i < save_double_latitude.size(); i += 2) {
                @SuppressLint("DefaultLocale") Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(save_double_latitude.get(i), save_double_latitude.get(i + 1)))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                        .alpha(0.8f)
                        .title(String.format("Position:(%.2f ; %.2f)", save_double_latitude.get(i), save_double_latitude.get(i + 1))));
                markerList.add(marker);
            }
        }
        catch (NullPointerException e){
            return;
        }


}


    public void Clear_memory(View view) {
        markerList.clear();
        mMap.clear();
        save_double_latitude.clear();

        if(button_show) {
            FlingAnimation flingAnimation = new FlingAnimation(findViewById(R.id.buttons), DynamicAnimation.SCROLL_Y);
            flingAnimation.setStartVelocity(-1000).setMinValue(0).setFriction(1.1f).start();
            flingAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
            button_show=false;
        }

        if(recording)
        {
            FlingAnimation flingAnimation2 = new FlingAnimation(findViewById(R.id.text_ac), DynamicAnimation.TRANSLATION_Y);
            flingAnimation2.setStartVelocity(-300).setMinValue(0).setMaxValue(150).setFriction(0.5f).start();
            flingAnimation2.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
            recording = false;
        }

    }


    public void Hide_Buttons() {
        button_show = false;
        if(recording)
        {
            FlingAnimation flingAnimation2 = new FlingAnimation(findViewById(R.id.text_ac), DynamicAnimation.TRANSLATION_Y);
            flingAnimation2.setStartVelocity(-300).setMinValue(0).setMaxValue(150).setFriction(0.5f).start();
            flingAnimation2.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
            recording = false;
        }

    }
}



