package com.example.indoornavigation;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MapsActivity extends FragmentActivity implements SeekBar.OnSeekBarChangeListener, OnMapReadyCallback, GoogleMap.OnCameraMoveListener, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;

    private static final LatLng UC = new LatLng(40.443400, -79.942187);

    private GroundOverlay mGroundOverlay;

    private SeekBar mRotationBar;

    private SeekBar mScaleBar;

    private CheckBox toggleOverlayLock;

    private CheckBox toggleMarkerAdjustment;

    private UiSettings mUiSettings;

    private Stack<Marker> markersStack;

    private RadioButton radioCorners;
    private RadioButton radioDoors;
    private RadioButton radioStairs;
    private RadioButton radioElevators;

    private Button undoMarker;

    private Button undoPath;
    private CheckBox addPath;
    private Polyline mapPath;

    private Polygon validSpace;



    public void onMapSearch(View view) {
        EditText locationSearch = findViewById(R.id.locationText);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18.0f));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mRotationBar = findViewById(R.id.rotationSeekBar);
        mRotationBar.setMax(360);
        mRotationBar.setProgress(0);

        mScaleBar = findViewById(R.id.scaleSeekBar);
        mScaleBar.setMax(500);
        mScaleBar.setProgress(50);

        toggleOverlayLock = findViewById(R.id.toggleLock);

        toggleOverlayLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggleOverlayLock.isChecked()) {
                    mScaleBar.setVisibility(View.INVISIBLE);
                    mRotationBar.setVisibility(View.INVISIBLE);
                } else {
                    mScaleBar.setVisibility(View.VISIBLE);
                    mRotationBar.setVisibility(View.VISIBLE);
                }
            }
        });

        findViewById(R.id.markers).setVisibility(View.INVISIBLE);
        findViewById(R.id.buttonUndo).setVisibility(View.INVISIBLE);

        toggleMarkerAdjustment = findViewById(R.id.toggleMarkers);
        toggleMarkerAdjustment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggleMarkerAdjustment.isChecked()) {
                    findViewById(R.id.markers).setVisibility(View.VISIBLE);
                    findViewById(R.id.buttonUndo).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.markers).setVisibility(View.INVISIBLE);
                    findViewById(R.id.buttonUndo).setVisibility(View.INVISIBLE);
                }
            }
        });

        markersStack = new Stack();
        radioCorners = (RadioButton) findViewById(R.id.radioCorners);
        radioDoors = (RadioButton) findViewById(R.id.radioDoors);
        radioStairs = (RadioButton) findViewById(R.id.radioStairs);
        radioElevators = (RadioButton) findViewById(R.id.radioElevators);

        undoMarker = (Button) findViewById(R.id.buttonUndo);
        undoMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!markersStack.isEmpty()) {
                    markersStack.pop().remove();
                }
            }
        });

        findViewById(R.id.undoPath).setVisibility(View.INVISIBLE);

        addPath = findViewById(R.id.togglePath);
        addPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (addPath.isChecked()) {
                    findViewById(R.id.undoPath).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.undoPath).setVisibility(View.INVISIBLE);
                }
            }
        });

        undoPath = (Button) findViewById(R.id.undoPath);
        undoPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<LatLng> coords = mapPath.getPoints();
                if (!coords.isEmpty()) {
                    coords.remove(coords.size()-1);
                    mapPath.setPoints(coords);
                }
            }
        });

        findViewById(R.id.convertLine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<LatLng> coords = mapPath.getPoints();
                if (!coords.isEmpty()) {
                    mapPath.setPoints(new ArrayList<LatLng>());
                    coords.add(coords.get(0));
                    if (validSpace == null) {
                        validSpace = mMap.addPolygon(new PolygonOptions()
                                .add(new LatLng(0, 0)).fillColor(Color.argb(20, 255, 0, 0)).strokeColor(3).strokeColor(Color.RED));
                        validSpace.setPoints(coords);
                    } else {
                        List<List<LatLng>> holes = validSpace.getHoles();
                        holes.add(coords);
                        validSpace.setHoles(holes);
                    }
                }
            }
        });
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mGroundOverlay != null && !((CheckBox) findViewById(R.id.toggleLock)).isChecked()) {
            if (seekBar == findViewById(R.id.rotationSeekBar)){
                mGroundOverlay.setBearing(progress);
            }
            else {
                mGroundOverlay.setDimensions((float)progress);
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        EditText locationSearch = findViewById(R.id.locationText);
        locationSearch.bringToFront();

        mMap = googleMap;

        mUiSettings = mMap.getUiSettings();

        mUiSettings.setZoomControlsEnabled(false);
        mUiSettings.setCompassEnabled(false);
        mUiSettings.setMyLocationButtonEnabled(false);
        mUiSettings.setTiltGesturesEnabled(false);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(UC)      // Sets the center of the map to UC
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        mGroundOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.uc_floor1))
                .bearing(0)
                .transparency(0.5f)
                .position(UC, 100f));

        mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.mapstyle));


        mRotationBar.setOnSeekBarChangeListener(this);
        mScaleBar.setOnSeekBarChangeListener(this);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnMapLongClickListener(this);

        mapPath = mMap.addPolyline(new PolylineOptions()
                .width(5).color(Color.RED));

    }

    @Override
    public void onCameraMove() {
        if (!((CheckBox) findViewById(R.id.toggleLock)).isChecked()) {
            mGroundOverlay.setPosition(mMap.getCameraPosition().target);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (toggleMarkerAdjustment.isChecked()) {
            MarkerOptions newMarker = new MarkerOptions().position(latLng);
            if (radioCorners.isChecked()) {
                newMarker.title("Corner");
                markersStack.push(mMap.addMarker(newMarker));
            }
            else if (radioDoors.isChecked()) {
                newMarker.title("Door");
                markersStack.push(mMap.addMarker(newMarker));
            }
            else if (radioElevators.isChecked()) {
                newMarker.title("Elevator");
                markersStack.push(mMap.addMarker(newMarker));
            }
            else if (radioStairs.isChecked()) {
                newMarker.title("Stairs");
                markersStack.push(mMap.addMarker(newMarker));
            }
        }

        if (addPath.isChecked()) {
            List<LatLng> coord = mapPath.getPoints();
            coord.add(latLng);
            mapPath.setPoints(coord);
        }
    }
}
