package com.lixtracking.lt.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lixtracking.lt.R;
import com.lixtracking.lt.VehicleDetail;
import com.lixtracking.lt.common.URL;
import com.lixtracking.lt.parsers.GpsData;
import com.lixtracking.lt.parsers.ParseGpsData;
import com.lixtracking.lt.parsers.VehicleData;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by saiber on 01.04.2014.
 */
public class FragmentTracking extends Fragment {
    private static final String TID = "tid";
    private GoogleMap map = null;
    View view = null;
    private MapView mapView;
    VehicleData vehicleData = null;
    List<GpsData> gpsDatas = null;
    GpsData firstActive = null;
    List<Marker>markerList = new ArrayList<Marker>();
    Timer timer;

    private boolean updateIsRunning = false;
    Context context = null;
    Marker marker;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceSatte) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = layoutInflater.inflate(R.layout.fragment_map, container, false);
            context = getActivity();
            //Setup google here
            if (map == null) {
                // Try to obtain the map from the SupportMapFragment.
                map = ((SupportMapFragment) VehicleDetail.fragmentManager
                        .findFragmentById(R.id.map)).getMap();
                if(map != null) {
                    map.setMapType(1);
                    map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                        @Override
                        public View getInfoWindow(Marker marker) {
                            return null;
                        }
                        @Override
                        public View getInfoContents(Marker marker) {
                            View view = getLayoutInflater(null).inflate(R.layout.map_info_window, null);
                            TextView title = ((TextView)view.findViewById(R.id.textView));
                            title.setText(marker.getTitle());
                            ((TextView)view.findViewById(R.id.textView2)).setText(marker.getSnippet());
                            return view;
                        }
                    });
                }
            }
        } catch (InflateException e) {
            /* map is already there, just return view as it is */
        }
        return view;
    }

    @Override
    public void onResume() {
        vehicleData = ((VehicleDetail)getActivity()).vehicleData;
        super.onResume();
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TID," Start thread");
                while (true) {
                    new getRealTimeGpsData().execute(((VehicleDetail)getActivity()).vehicleData.gps_id);
                    while (updateIsRunning == true){
                        SystemClock.sleep(1000);
                    }
                }
            }
        }).start();*/
        timer = new Timer();
        TimerTask timerTask = new Task();
        timer.scheduleAtFixedRate(timerTask, 1, 5000);
    }
    class Task extends TimerTask {
        @Override
        public void run() {
            if(updateIsRunning == false){
                if(vehicleData != null)
                    new getRealTimeGpsData().execute(((VehicleDetail)getActivity()).vehicleData.gps_id);
            }
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        timer.cancel();
    }

    /**********************************************************************************************/
    /**/
    /**********************************************************************************************/
    class getRealTimeGpsData extends AsyncTask<String, Void, String> {
        private String resultString = "...";
        private String message = "";
        //private String index = "";
        @Override
        protected String doInBackground(String... params) {
            updateIsRunning = true;
            //index = params[1];
            Log.i("info", " START: getVehiclesTask");
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 25000);
            HttpConnectionParams.setSoTimeout(httpParams, 25000);

            DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
            HttpPost httpPost = new HttpPost(URL.getRealTimeGpsDataUrl);

            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>(1);
            Log.i("info"," terminal_id = " + params[0]);
            nameValuePairList.add(new BasicNameValuePair("terminal_id", params[0]));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairList));
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                resultString = EntityUtils.toString(httpEntity);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                message = "Server does not respond";
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return resultString;
        }
        @Override
        protected void onPostExecute(String result) {
            if(resultString == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                //builder.setTitle("Mesage: " + index);
                builder.setMessage(message);
                builder.setCancelable(true);
                builder.setPositiveButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }else {
                Log.i("info","--------------------------------------------------------------------");
                Log.i("info","result : " + resultString);
                Log.i("info","--------------------------------------------------------------------");
                List<GpsData> tmpData = new ParseGpsData(context).parceXml(resultString);
                if(gpsDatas == null)
                    gpsDatas = new ArrayList<GpsData>();
                if(tmpData != null)
                    gpsDatas.addAll(tmpData);

                if(map != null) {
                    if((marker != null)) {
                        map.clear();
                    }
                    for(int i = 0; i<tmpData.size(); i++) {
                        float lat = Float.parseFloat(tmpData.get(i).lat);
                        float lng = Float.parseFloat(tmpData.get(i).lng);

                        if((lng != 0) && (lng != 0)) {
                            LatLng latLon = new LatLng(lat,lng);
                            int r = R.drawable.car_na_32x32;
                            if(vehicleData.status == 1) {
                                r = R.drawable.car_32x32;
                            }
                            marker = map.addMarker(new MarkerOptions()
                                            .position(latLon)
                                            .title(" GPS ID : " + tmpData.get(i).gps_id)
                                            .icon(BitmapDescriptorFactory.fromResource(r))
                                            .snippet("speed : " + tmpData.get(i).speed)
                            );
                            //markerList.add(marker);
                            if(marker.isInfoWindowShown() == false)
                                map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
                        }
                    }
                }
            }
            updateIsRunning = false;
        }
    }
}