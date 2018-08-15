package org.example.bepuravida;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.example.bepuravida.models.PlaceInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "MapMain";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));

    //widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGps, mInfo, mPlacePicker, mCostaRica;

    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutoCompleteAdapter mPlaceAutoCompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private PlaceInfo mPlace;
    private Marker mMarker;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            init();

            touristPlacesCostaRica();
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                String searchString = marker.getTitle();

                Geocoder geocoder = new Geocoder(MapMain.this);
                List<Address> list = new ArrayList<>();

                try{
                    list = geocoder.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 1);

                    if(list.size() > 0){
                        Address address = list.get(0);
                    }
                }catch(IOException e){
                    Log.d(TAG, "geoLocate: IOException: " + e.getMessage());
                }
                return true;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        mGps = (ImageView) findViewById(R.id.ic_gps);
        mInfo = (ImageView) findViewById(R.id.place_info);
        mPlacePicker = (ImageView) findViewById(R.id.place_picker);
        mCostaRica = (ImageView) findViewById(R.id.ic_costa_rica);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if(isServicesOK()){
            getLocationPermission();
        }
    }

    private void init(){
        Log.d(TAG, "init: initializing");

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        mSearchText.setOnItemClickListener(mAutoCompleteClickListener);

        mPlaceAutoCompleteAdapter = new PlaceAutoCompleteAdapter(this, mGoogleApiClient,
                LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutoCompleteAdapter);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == keyEvent.ACTION_DOWN
                        || keyEvent.getAction() == keyEvent.KEYCODE_ENTER){

                    //execute our method for searching
                    geoLocate();
                }

                return false;
            }
        });

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

        mCostaRica.setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v){
                Log.d(TAG, "onClick: Go to Costa Rica");
                moveCameraCostaRica();
            }
        });

        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Clicked place info");
                try{
                    if(mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();
                    } else{
                        Log.d(TAG, "onClick: Place Info: " + mPlace.toString());
                        mMarker.showInfoWindow();
                    }
                } catch(NullPointerException e){
                    Log.e(TAG, "onClick: NullPointerException: " + e.getMessage());
                }
            }
        });

        mPlacePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(MapMain.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    Log.d(TAG, "onClick: GooglePlayServicesRepairableException" + e.getMessage());
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.d(TAG, "onClick: GooglePlayServicesNotAvailableException" + e.getMessage());
                }
            }
        });

        hideSoftKeyboard();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, place.getId());
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        }
    }

    private void touristPlacesCostaRica(){
        //Manzanillo
        double lat = 9.629838;
        double lng = -82.6577963;

        LatLng latLng = new LatLng(lat, lng);

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("Manzanillo")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Punta Cahuita
        lat = 9.7347856;
        lng = -82.84521459999999;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Punta Cahuita")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Playa Cocles
        lat = 9.6480186;
        lng = -82.7326372;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Cocles")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Tortuguero
        lat = 10.4488767;
        lng = -83.5069226;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Tortuguero")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Playa Santa Teresa
        lat = 9.6421091;
        lng = -85.1688563;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Santa Teresa")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Playa Malpais
        lat = 9.5997423;
        lng = -85.1423456;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Malpais")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Isla Tortuga
        lat = 9.7701083;
        lng = -84.8919664;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Isla Tortuga")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Playa Samara
        lat = 9.8794448;
        lng = -85.51741449999999;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Sámara")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Playa Carrillo
        lat = 9.870346;
        lng = -85.4959707;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Carrillo")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Playa Nosara
        lat = 9.973152;
        lng = -85.6841007;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Nosara")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Playa Tamarindo
        lat = 10.299996799999999;
        lng = -85.842061;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Tamarindo")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Playa Flamingo
        lat = 10.433238;
        lng = -85.79217179999999;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Flamingo")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Playas Del Coco
        lat = 10.551046;
        lng = -85.7002587;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playas Del Coco")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Playa Nacascolo
        lat = 10.6276;
        lng = -85.6756;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Nacascolo")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Parque Nacional Santa Rosa
        lat = 10.837930799999999;
        lng = -85.7051116;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Parque Nacional Santa Rosa")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Volcán Rincón de la Vieja
        lat = 10.831528599999999;
        lng = -85.3363521;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Volcán Rincón de la Vieja")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Monteverde
        lat = 10.274968200000002;
        lng = -84.8255097;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Monteverde")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //La Fortuna
        lat = 10.467833500000001;
        lng = -84.64268059999999;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("La Fortuna")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Rio Celeste
        lat = 10.7046925;
        lng = -84.9902481;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Río Celeste")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Playa Jacó
        lat = 9.612088900000002;
        lng = -84.628665;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Jacó")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Playa Manuel Antonio
        lat = 9.381300800000002;
        lng = -84.14509079999999;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Manuel Antonio")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_fav_place));
        mMap.addMarker(options);

        //Playa Dominical
        lat = 9.2626518;
        lng = -83.8801636;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Playa Dominical")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Bahia Ballena
        lat = 9.1550517;
        lng = -83.7480951;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Parque Nacional Marino Ballena")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Parque Nacional Corcovado
        lat = 8.540835399999999;
        lng = -83.57096399999999;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Parque Nacional Corcovado")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);

        //Volcán Irazú
        lat = 9.9799813;
        lng = -83.84907609999999;

        latLng = new LatLng(lat, lng);

        options = new MarkerOptions()
                .position(latLng)
                .title("Volcán Irazú")
                .icon(BitmapDescriptorFactory.
                        fromResource(R.mipmap.ic_add_place));
        mMap.addMarker(options);
    }

    private void geoLocate(){
        Log.d(TAG, "geoLocate: geolocating");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapMain.this);
        List<Address> list = new ArrayList<>();

        try{
            list = geocoder.getFromLocationName(searchString, 1);

            if(list.size() > 0){
                Address address = list.get(0);

                String snippet = "Address: " + address.getAddressLine(0) + "\n" +
                        "Phone Number: " + address.getPhone() + "\n" +
                        "Website: " + address.getUrl() + "\n";


            }
        }catch(IOException e){
            Log.d(TAG, "geoLocate: IOException: " + e.getMessage());
        }
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted){
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM,
                                    "My Location");
                        } else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapMain.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        mMap.clear();//Clean all the markets on the map

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapMain.this));

        touristPlacesCostaRica();

        if(placeInfo != null){
            try{
                String snippet = "Address: " + placeInfo.getAddress() + "\n" +
                        "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
                        "Website: " + placeInfo.getWebsiteUri() + "\n" +
                        "Price Rating: " + placeInfo.getRating() + "\n";

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .snippet(snippet);

                mMarker = mMap.addMarker(options);
            } catch (NullPointerException e){
                Log.d(TAG, "moveCamera: NullPointerException " + e.getMessage());
            }
        } else{
            mMap.addMarker(new MarkerOptions().position(latLng));
        }

        hideSoftKeyboard();
    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }

        hideSoftKeyboard();
    }

    private void moveCameraCostaRica(){
        double latitude = 9.9129727;
        double longitude = -84.1768294;
        float zoom = 7.3f;

        LatLng latLng = new LatLng(latitude, longitude);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        Toast.makeText(this, "Costa Rica Map", Toast.LENGTH_SHORT).show();
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapMain.this);

        if (available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error ocurred but we can resolve it
            Log.d(TAG, "isServicesOK: An error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MapMain.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }
        else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void initMap(){
        Log.d(TAG, "initMap: Initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapMain.this);
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: Getting location permissions");
        String[] permission = {FINE_LOCATION, COURSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }
            else{
                ActivityCompat.requestPermissions(this,
                        permission,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
        else{
            ActivityCompat.requestPermissions(this,
                    permission,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: Called.");
        mLocationPermissionsGranted = false;

        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if (grantResults.length > 0){
                    for (int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: Permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: Permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /*private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }*/

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    /*
    -----------------------------------------------------------------------------------------
    ----------------------- Google Places API Autocomplete Suggestions ----------------------
    -----------------------------------------------------------------------------------------
    * */

    private AdapterView.OnItemClickListener mAutoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutoCompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try{
                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                Log.d(TAG, "onResult: name: " + place.getName());
                mPlace.setAddress(place.getAddress().toString());
                Log.d(TAG, "onResult: address: " + place.getAddress());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                Log.d(TAG, "onResult: phone number: " + place.getPhoneNumber());
                mPlace.setId(place.getId());
                Log.d(TAG, "onResult: id: " + place.getId());
                mPlace.setWebsiteUri(place.getWebsiteUri());
                Log.d(TAG, "onResult: website uri: " + place.getWebsiteUri());
                mPlace.setLatlng(place.getLatLng());
                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
                mPlace.setRating(place.getRating());
                Log.d(TAG, "onResult: rating: " + place.getRating());
                //mPlace.setAttributions(place.getAttributions().toString());
                //Log.d(TAG, "onResult: attributions: " + place.getAttributions());

                Log.d(TAG, "onResult: Place: " + mPlace.toString());
            } catch (NullPointerException e){
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }

            moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace);

            places.release();
        }
    };
}
