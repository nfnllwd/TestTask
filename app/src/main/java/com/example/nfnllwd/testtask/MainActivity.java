package com.example.nfnllwd.testtask;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    JSONArray coords;
    private GoogleMap mMap;
    Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                coords = (JSONArray) msg.obj;
                drawRoute();
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
    }

    public void onClickGetCrds(View view){

        DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
        downloadFilesTask.execute(mHandler);
    }

    private void drawRoute() {
        double laMax = -90, laMin = 90, loMax = -180, loMin = 180;
        JSONObject crd = null;
        PolylineOptions line = new PolylineOptions();
        LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
        for (int i = 0; i < this.coords.length(); i++) {
            try {
                crd = this.coords.getJSONObject(i);
                LatLng wayPoint = new LatLng(crd.getDouble("la"), crd.getDouble("lo"));

                if (crd.getDouble("la") > laMax) laMax = crd.getDouble("la");
                if (crd.getDouble("la") < laMin) laMin = crd.getDouble("la");
                if (crd.getDouble("lo") > loMax) loMax = crd.getDouble("lo");
                if (crd.getDouble("lo") < loMin) loMin = crd.getDouble("lo");

                if (i == 0) {
                    MarkerOptions startMarkerOptions = new MarkerOptions()
                            .position(wayPoint).title("Start");
                    this.mMap.addMarker(startMarkerOptions);
                } else if (i ==  this.coords.length() - 1) {
                    MarkerOptions endMarkerOptions = new MarkerOptions()
                            .position(wayPoint).title("Finish");
                    this.mMap.addMarker(endMarkerOptions);
                }
                line.add(wayPoint);
                latLngBuilder.include(wayPoint);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        this.mMap.addPolyline(line);

        LatLng southWest = new LatLng(laMin,loMin);
        LatLng northEast = new LatLng(laMax,loMax);

        CameraUpdate newPos = CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWest, northEast), 50);
        mMap.moveCamera(newPos);
    }

    private  class DownloadFilesTask extends AsyncTask<Handler, Void, Void> {
        private Handler h;
        private String URL = "http://test.www.estaxi.ru/route.txt";

        @Override
        protected Void doInBackground(Handler...handler) {
            h = handler[0];
            ArrayList<String> strings=new ArrayList<String>();
            try {
                URL url = new URL(this.URL);
                HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String str;
                while ((str = in.readLine()) != null) {
                    strings.add(str);
                }
                in.close();
            } catch (Exception e) {
                Log.d("MyTag",e.toString());
            }

            //Возможно это и костыль, но toString почему-то добавляет запятые
            String temp = "";
            for (int i = 0; i <  strings.size(); i++) temp+=strings.get(i);


            try {
                JSONObject jsonObject = new JSONObject(temp);
                JSONArray coords = new JSONArray();
                coords = jsonObject.getJSONArray("coords");
                Message msg = h.obtainMessage(1,coords);
                h.sendMessage(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return  null;
        }

        @Override
        protected void onPostExecute(Void result) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Путь получен")
                    .setNegativeButton("Ок",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();

        }
    }
}
