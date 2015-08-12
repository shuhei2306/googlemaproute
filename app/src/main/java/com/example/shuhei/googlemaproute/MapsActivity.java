package com.example.shuhei.googlemaproute;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity  extends FragmentActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    SeekBar seekBar;

    GoogleMap gMap;
    GoogleMap gMap2;

    private GoogleApiClient mLocationClient = null;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
    private LocationRequest locationRequest;

    public boolean sw = false;
    public boolean firstset = false;

    public boolean map1sw = false;
    public boolean map2sw = false;

    private static final int MENU_A = 0;
    private static final int MENU_B = 1;
    private static final int MENU_c = 2;

    public static String posinfo = "";
    public static String info_A = "";
    public static String info_B = "";

    public static String posinfo2 = "";
    public static String info_A2 = "";
    public static String info_B2 = "";

    ArrayList<LatLng> markerPoints;
    ArrayList<LatLng> markerPoints2;

    public static MarkerOptions options;
    public static MarkerOptions options2;

    public ProgressDialog progressDialog;

    public String travelMode = "driving";//default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //プログレス
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("検索中......");
        progressDialog.hide();


        //初期化
        markerPoints = new ArrayList<LatLng>();
        markerPoints2 = new ArrayList<LatLng>();


        SupportMapFragment mapfragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        SupportMapFragment mapfragment2 = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map2);


        gMap = mapfragment.getMap();
        gMap2 = mapfragment2.getMap();

        //初期位置
        LatLng latLng = new LatLng(34.71985,135.234388);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        gMap2.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));


        if(gMap!=null){

            gMap.setMyLocationEnabled(true);

            gMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    gMap2.animateCamera(CameraUpdateFactory.zoomTo(gMap.getCameraPosition().zoom));
                }
            });

            //クリックリスナー
            gMap.setOnMapClickListener(new OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {

                    //３度目クリックでスタート地点を再設定
                    if(markerPoints.size()>1){
                        markerPoints.clear();
                        gMap.clear();
                        map1sw = false;
                    }


                    markerPoints.add(point);


                    options = new MarkerOptions();
                    options.position(point);


                    if(markerPoints.size()==1){
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                       // options.icon(BitmapDescriptorFactory.fromResource(R.drawable.green));
                        options.title("A");


                    }else if(markerPoints.size()==2){
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        //options.icon(BitmapDescriptorFactory.fromResource(R.drawable.red));
                        options.title("B");


                    }


                    gMap.addMarker(options);


                    gMap.setOnMarkerClickListener(new OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            // TODO Auto-generated method stub


                            String title = marker.getTitle();
                            if (title.equals("A")){
                                marker.setSnippet(info_A);

                            }else if (title.equals("B")){
                                marker.setSnippet(info_B);
                            }


                            return false;
                        }
                        });



                    if(markerPoints.size() >= 2){
                        map1sw = true;
                        //ルート検索
                        routeSearch();

                    }
                }
            });
        }

        if(gMap2!=null){

            gMap2.setMyLocationEnabled(true);

            //クリックリスナー
            gMap2.setOnMapClickListener(new OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {

                    //３度目クリックでスタート地点を再設定
                    if (markerPoints2.size() > 1) {
                        markerPoints2.clear();
                        gMap2.clear();
                        map2sw = true;
                    }


                    markerPoints2.add(point);


                    options2 = new MarkerOptions();
                    options2.position(point);


                    if (markerPoints2.size() == 1) {
                        options2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        // options.icon(BitmapDescriptorFactory.fromResource(R.drawable.green));
                        options2.title("A");


                    } else if (markerPoints2.size() == 2) {
                        options2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        //options.icon(BitmapDescriptorFactory.fromResource(R.drawable.red));
                        options2.title("B");


                    }


                    gMap2.addMarker(options2);


                    gMap2.setOnMarkerClickListener(new OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            // TODO Auto-generated method stub


                            String title = marker.getTitle();
                            if (title.equals("A")) {
                                marker.setSnippet(info_A2);

                            } else if (title.equals("B")) {
                                marker.setSnippet(info_B2);
                            }


                            return false;
                        }
                    });


                    if (markerPoints2.size() >= 2) {
                        //ルート検索
                        map2sw = true;
                        routeSearch2();
                    }
                }
            });
        }




        seekBar = (SeekBar)findViewById(R.id.SeekBar01);

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        // ツマミをドラッグしたときに呼ばれる
                        //tv1.setText("Current Value:"+progress);
                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
                        fragment.getView().setAlpha((float) seekBar.getProgress() / (float) 10.0);
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる
                    }
                }
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.getView().setAlpha((float) seekBar.getProgress() / (float) 10);

    }

    public void changeLayout(View v) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
            fragment.getView().setAlpha((float) 1.0);

            Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.map2);

            fragment.getView().setTop(544);
            fragment.getView().setBottom(1088);
            fragment2.getView().setBottom(0);
            fragment2.getView().setBottom(544);

            sw=true;

    }
    public void changeLayout2(View v) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
            fragment.getView().setAlpha((float) seekBar.getProgress() / (float) 10.0);

            Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.map2);

            fragment.getView().setTop(0);
            fragment.getView().setBottom(1088);
            fragment2.getView().setTop(0);
            fragment2.getView().setBottom(1088);

    }
    public void showMap1(View v) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.getView().setAlpha(1.0f);

        Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.map2);

        fragment.getView().setTop(0);
        fragment.getView().setBottom(1088);
        fragment2.getView().setTop(0);
        fragment2.getView().setBottom(0);
    }
    public void showMap2(View v) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
        //fragment.getView().setAlpha(1.0f);

        Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.map2);

        fragment.getView().setTop(0);
        fragment.getView().setBottom(0);
        fragment2.getView().setTop(0);
        fragment2.getView().setBottom(1088);
    }
    public void setZoom(View v) {
        gMap2.animateCamera(CameraUpdateFactory.zoomTo(gMap.getCameraPosition().zoom));
    }

    private void routeSearch(){
        progressDialog.show();

        LatLng origin = markerPoints.get(0);
        LatLng dest = markerPoints.get(1);


        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();


        downloadTask.execute(url);

    }

    private void routeSearch2(){
        progressDialog.show();

        LatLng origin = markerPoints2.get(0);
        LatLng dest = markerPoints2.get(1);


        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();


        downloadTask.execute(url);

    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){


        String str_origin = "origin="+origin.latitude+","+origin.longitude;


        String str_dest = "destination="+dest.latitude+","+dest.longitude;


        String sensor = "sensor=false";

        //パラメータ
        String parameters = str_origin+"&"+str_dest+"&"+sensor + "&language=ja" + "&mode=" + travelMode;

        //JSON指定
        String output = "json";


        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }

    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);


            urlConnection = (HttpURLConnection) url.openConnection();


            urlConnection.connect();


            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if(firstset == false) {
            CameraPosition cameraPos = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(gMap.getCameraPosition().zoom)
                    .bearing(0).build();
            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos));

            firstset=true;
        }
        else
        {
            // gMap2.animateCamera(CameraUpdateFactory.zoomBy(gMap.getCameraPosition().zoom));
        }
        gMap2.animateCamera(CameraUpdateFactory.zoomTo(gMap.getCameraPosition().zoom));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    private class DownloadTask extends AsyncTask<String, Void, String>{
        //非同期で取得

        @Override
        protected String doInBackground(String... url) {


            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }


        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);
        }
    }

    /*parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{


        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);

                parseJsonpOfDirectionAPI parser = new parseJsonpOfDirectionAPI();
                parser.sw1 = map1sw;
                parser.sw2 = map2sw;
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        //ルート検索で得た座標を使って経路表示
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {


            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            if(result.size() != 0){

                for(int i=0;i<result.size();i++){
                    points = new ArrayList<LatLng>();
                    lineOptions = new PolylineOptions();


                    List<HashMap<String, String>> path = result.get(i);


                    for(int j=0;j<path.size();j++){
                        HashMap<String,String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    //ポリライン
                    lineOptions.addAll(points);
                    lineOptions.width(10);
                    lineOptions.color(0x550000ff);

                }

                //描画
                if(map1sw == true)
                gMap.addPolyline(lineOptions);

                if(map2sw == true)
                    gMap2.addPolyline(lineOptions);
            }else{
                if(map1sw == true)
                gMap.clear();

                if(map1sw == true)
                    gMap2.clear();

                Toast.makeText(MapsActivity.this, "ルート情報を取得できませんでした", Toast.LENGTH_LONG).show();
            }
            progressDialog.hide();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        menu.add(0, MENU_A,   0, "Info");
        menu.add(0, MENU_B,   0, "Legal Notices");
        menu.add(0, MENU_c,   0, "Mode");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() )
        {
            case MENU_A:
                //show_mapInfo();
                return true;

            case MENU_B:
                //Legal Notices(免責事項)

                String LicenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getApplicationContext());
                AlertDialog.Builder LicenseDialog = new AlertDialog.Builder(MapsActivity.this);
                LicenseDialog.setTitle("Legal Notices");
                LicenseDialog.setMessage(LicenseInfo);
                LicenseDialog.show();

                return true;

            case MENU_c:
                //show_settings();
                return true;

        }
        return false;
    }

    //リ･ルート検索
    private void re_routeSearch(){
        progressDialog.show();

        LatLng origin = markerPoints.get(0);
        LatLng dest = markerPoints.get(1);

        //
        gMap.clear();

        //マーカー
        //A
        options = new MarkerOptions();
        options.position(origin);
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        options.title("A");
        options.draggable(true);
        gMap.addMarker(options);
        //B
        options = new MarkerOptions();
        options.position(dest);
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        options.title("B");
        options.draggable(true);
        gMap.addMarker(options);


        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();


        downloadTask.execute(url);

    }
    private void re_routeSearch2(){
        progressDialog.show();

        LatLng origin = markerPoints2.get(0);
        LatLng dest = markerPoints2.get(1);

        //
        gMap2.clear();

        //マーカー
        //A
        options2 = new MarkerOptions();
        options2.position(origin);
        options2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        options2.title("A");
        options2.draggable(true);
        gMap2.addMarker(options2);
        //B
        options2 = new MarkerOptions();
        options2.position(dest);
        options2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        options2.title("B");
        options2.draggable(true);
        gMap2.addMarker(options2);


        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();


        downloadTask.execute(url);

    }
}
