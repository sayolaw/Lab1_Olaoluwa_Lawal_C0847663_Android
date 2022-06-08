package com.example.mapdemo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.mapdemo.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static final int REQUEST_CODE = 1;
    private String newText = "";
    private static final long UPDATE_INTERVAL = 5000;
    private static final long FASTEST_INTERVAL = 3000;
    private static int count;
    private static final int POLYGON_SIDES = 4;
    private FusedLocationProviderClient mClient;
    private static boolean cameraSet = false;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Polyline line;
    private Bitmap newBit;
    private List<LatLng> points,polygonPoints = new ArrayList<>();
    private Marker userMarker, favMarker,lastMarker;
    private List<Marker> markerList = new ArrayList<>();
    private Polygon shape;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mClient = LocationServices.getFusedLocationProviderClient(this);
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
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (!isGrantedLocationPermission()){
            requestLocationPermission();
        }
        else{
            startUpdatingLocation();


        }


        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
            @Override
            public void onPolygonClick(@NonNull Polygon polygon) {
                float[] distance1 = new float[1];
                float[] distance2 = new float[1];
                float[] distance3 = new float[1];

                polygonPoints = polygon.getPoints();
                Location.distanceBetween(polygonPoints.get(0).latitude, polygonPoints.get(0).longitude,polygonPoints.get(1).latitude,
                        polygonPoints.get(1).longitude,distance1);
                Location.distanceBetween(polygonPoints.get(1).latitude, polygonPoints.get(1).longitude,polygonPoints.get(2).latitude,
                        polygonPoints.get(2).longitude,distance2);
                Location.distanceBetween(polygonPoints.get(2).latitude, polygonPoints.get(2).longitude,polygonPoints.get(3).latitude,
                        polygonPoints.get(3).longitude,distance3);
                float totalD = distance1[0] + distance2[0] + distance3[0];

                Toast.makeText(MapsActivity.this, "total distance in polygon is : "+totalD/1000+"Km",Toast.LENGTH_LONG).show();
            }
        });

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(@NonNull Polyline polyline) {
                Log.d("hello","we are here");
                float[] results = new float[1];
                 points = polyline.getPoints();
                 Location.distanceBetween(points.get(0).latitude, points.get(0).longitude,points.get(1).latitude,
                         points.get(1).longitude,results);

                Toast.makeText(MapsActivity.this, "distance is "+results[0]/1000+"Km",Toast.LENGTH_LONG).show();
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle("Title");
                final EditText input = new EditText(MapsActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newText = input.getText().toString();
                        marker.setIcon(createPureTextIcon(newText));
                        marker .setTitle(newText);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.show();
                return false;
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {

                setMarker(latLng);
            }
        });
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener(){
            @Override
            public void onMapLongClick(@NonNull LatLng latLng){

                checkMarker(latLng);
            }
        });

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(43, -79);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
       mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
           @Override
           public void onMarkerDrag(@NonNull Marker marker) {

           }

           @Override
           public void onMarkerDragEnd(@NonNull Marker marker) {
            marker.remove();

           }

           @Override
           public void onMarkerDragStart(@NonNull Marker marker) {

           }
       });


    }
    public BitmapDescriptor createPureTextIcon(String text) {

        Paint textPaint = new Paint(); // Adapt to your needs
        textPaint.setTextSize(50);
        float textWidth = textPaint.measureText(text);
        float textHeight = textPaint.getTextSize();
        int width = (int) (textWidth);
        int height = (int) (textHeight);

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);

        canvas.translate(0, height);

        // For development only:
        // Set a background in order to see the
        // full size and positioning of the bitmap.
        // Remove that for a fully transparent icon.
        canvas.drawColor(Color.LTGRAY);

        canvas.drawText(text, 0, 0, textPaint);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(image);
        return icon;
    }
    private void checkMarker(LatLng latLng){
        MarkerOptions options = new MarkerOptions().position(latLng).title("check")
                .draggable(true)
                .icon(createPureTextIcon("A"))
                .snippet("Nice place");
        favMarker = mMap.addMarker(options);
        favMarker.remove();
        lastMarker = favMarker;
        for(Marker marker:markerList){
            if(marker == lastMarker){
                Log.d("found marker","marker here");
                marker.remove();

            }
        }
    }
    private void setMarker(LatLng latLng){
        count = count+1;
        String title ;
        if(count==1){
            title = "A";

        }
        else if(count==2){
            title ="B";
        }
        else if(count==3){
            title = "C";
        }
        else if(count==4){
            title = "D";
        }
        else{
            title = "Extra Points";
        }




        MarkerOptions options = new MarkerOptions().position(latLng).title(title)
                .draggable(true)
                .icon(createPureTextIcon(title))
                .snippet("Nice place");

                lastMarker = favMarker;
                favMarker = mMap.addMarker(options);


                favMarker.showInfoWindow();

                if(markerList.size()<4){
                    drawLine(favMarker);
                }

//                if(markerList.size() == POLYGON_SIDES){
//Log.d("size is", "this is the size: "+markerList.size());
//                    for(Marker marker:markerList){
//                        Log.d("removed with quickness","THATS Right");
//                        marker.remove();
//                    }
//                    markerList.clear();
//                    shape.remove();
//                }
                markerList.add(favMarker);
                if(markerList.size() == POLYGON_SIDES){
                    Log.d("here:","it logged"+markerList.size());
                    lastMarker = markerList.get(0);
                    drawLine(favMarker);
                    drawShape();
        }

    }
    private void drawShape(){
        PolygonOptions options = new PolygonOptions()
                .strokeColor(Color.RED)
                .fillColor(0x3500ff00)
                .clickable(true)
                .strokeWidth(7);

        for(Marker marker: markerList){
            options.add(marker.getPosition());

        }
    mMap.addPolygon(options);
    }
    private void drawLine(Marker marker){
        if(lastMarker!=null){
      PolylineOptions options = new PolylineOptions().color(Color.RED)
              .width(10)
              .clickable(true)
              .add(lastMarker.getPosition(), marker.getPosition())
              ;
      line = mMap.addPolyline(options);}
    }
    @SuppressLint("MissingPermission")
    private void startUpdatingLocation(){
        locationRequest = com.google.android.gms.location.LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult){
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            LatLng userlocation = new LatLng(location.getLatitude()
                    ,location.getLongitude());
//            userMarker = mMap.addMarker(new MarkerOptions().position(userlocation).title("My Location").snippet("I am here"));
            if(!cameraSet){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userlocation,16.0f));
            cameraSet = true;
            }
            locationRequest.setSmallestDisplacement(1);
            }
        };
        mClient.requestLocationUpdates(locationRequest,
                locationCallback,
               null
        );
    }
    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE);
    }
    private boolean isGrantedLocationPermission(){
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    };
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE){
if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
    new AlertDialog.Builder(this).setMessage("Accessing the location is Mandatory")
            .setPositiveButton("OK", (dialogInterface, i) -> requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE
            )).setNegativeButton("Cancel",null)
            .create().show();
}
else{
    startUpdatingLocation();
}
        }
    }

}
