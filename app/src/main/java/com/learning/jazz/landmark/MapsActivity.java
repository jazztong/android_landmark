package com.learning.jazz.landmark;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.amplify.generated.graphql.CreateLandmarkMutation;
import com.amazonaws.amplify.generated.graphql.ListLandmarksQuery;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import models.RemarkModel;
import models.SignInConstants;
import models.SignInType;
import type.CreateLandmarkInput;
import type.TableIDFilterInput;
import type.TableLandmarkFilterInput;
import type.TableStringFilterInput;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMapLongClickListener {

    private static final float DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private GoogleMap mMap;
    private boolean mLocationPermissionGranted;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private List<Marker> allMarker = new ArrayList<>();
    private AWSAppSyncClient mAWSAppSyncClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //Init appsync client
        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getLocationPermission();
        updateLocationUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu,menu);
        menu.findItem(R.id.option_login_as).setTitle(getLoginEmail());
        return true;
    }

    private String getLoginEmail() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account == null){
            startActivity(new Intent(this,SignInActivity.class));
        }
        return account.getEmail();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_logout:
                signOut();
                break;
        }
        return true;
    }

    private void signOut() {
        Intent signIn = new Intent(this,SignInActivity.class);
        signIn.putExtra(SignInConstants.SIGN_IN_COMMAND,SignInType.LOGOUT);
        startActivity(signIn);
    }

    private void updateLocationUI() {
        if(mMap == null){
            return;
        }
        try{
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
                mMap.setOnMapLongClickListener(this);
                showCurrentLocation();
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mMap.getUiSettings().setZoomControlsEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        }catch (SecurityException e){
            show(e.getMessage());
        }
    }

    private void showCurrentLocation() {
        try {
            if(mLocationPermissionGranted){
                mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful()){
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        }else {
                            show("Current location is null. Using defaults.");
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        }catch (SecurityException e){
            show(e.getMessage());
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void show(final String msg) {
        Log.w(this.getClass().getName(),msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onMyLocationButtonClick() {
        showCurrentLocation();
        return true;
    }

    private void showRemarkInput(LatLng loc) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Whats your remark");
        final EditText input = new EditText(this);
        input.setTag(loc);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        //setup button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                LatLng lastLoc = (LatLng) input.getTag();
                mutation(lastLoc,getLoginEmail(),input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void addMarker(final LatLng loc, final String email,final String snippet) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .title(email)
                        .position(loc)
                        .snippet(snippet));
                allMarker.add(marker);
                marker.showInfoWindow();
            }
        });

    }

    private void query(String comment){
        TableLandmarkFilterInput filterInput = TableLandmarkFilterInput.builder()
                .comment(TableStringFilterInput.builder()
                        .contains(comment)
                        .build())
                .build();
        mAWSAppSyncClient.query(ListLandmarksQuery.builder().filter(filterInput).build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_FIRST)
                .enqueue(queryCallback);
    }
    private GraphQLCall.Callback<ListLandmarksQuery.Data> queryCallback = new GraphQLCall.Callback<ListLandmarksQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListLandmarksQuery.Data> response) {
            //show(response.data().listLandmarks().toString());
            if(!response.data().listLandmarks().items().isEmpty()){
                clearMarker();
                for(ListLandmarksQuery.Item i: response.data().listLandmarks().items()){
                    addMarker(new LatLng(i.lat(),i.lng()),i.email(),i.comment());
                }
                moveToMarkerCenter();
            }else {
                show("Sorry please try again");
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            show(e.getMessage());
        }
    };

    private void clearMarker() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Marker marker : allMarker) {
                    marker.remove();
                }
            }
        });
    }

    private void mutation(LatLng location, String user, String comment) {
        CreateLandmarkInput input = CreateLandmarkInput.builder()
                .comment(comment)
                .lat(location.latitude)
                .lng(location.longitude)
                .email(user)
                .build();

        mAWSAppSyncClient.mutate(CreateLandmarkMutation.builder().input(input).build())
                .enqueue(mutationCallback);
    }

    private GraphQLCall.Callback<CreateLandmarkMutation.Data> mutationCallback = new GraphQLCall.Callback<CreateLandmarkMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<CreateLandmarkMutation.Data> response) {
            CreateLandmarkMutation.CreateLandmark data = response.data().createLandmark();
            addMarker(new LatLng(data.lat(),data.lng()),data.email(),data.comment());
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            show(e.getMessage());
        }
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode){
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mLocationPermissionGranted = true;
                    showCurrentLocation();
                }
                break;
        }
    }

    public void onMapSearch(View v) {
        String searchInput = getSearchInput();
        query(searchInput);
    }

    private void moveToMarkerCenter() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : allMarker) {
                    builder.include(marker.getPosition());
                }
                LatLngBounds bounds = builder.build();
                int padding = 0;
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,padding));
            }
        });

    }

    private RemarkModel[] getSearchResult() {
        return new RemarkModel[]{
            new RemarkModel("abc@email.com","something",new LatLng(12,20)),
            new RemarkModel("abcd@email.com","ffsomething",new LatLng(15,27)),
            new RemarkModel("abc3@email.com","sodfdmething",new LatLng(52,80)),
        };
    }

    private String getSearchInput() {
        EditText text = findViewById(R.id.editText_search);
        return text.getText().toString();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        showRemarkInput(latLng);
    }
}
