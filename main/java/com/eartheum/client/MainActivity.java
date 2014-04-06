package com.eartheum.client;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;





public class MainActivity extends Activity implements SensorEventListener, LocationListener {

    private SensorManager sensorManager = null;
    private LocationManager locationManager = null;
    private float[] gravity;
    private float[] geomag;
    private float[] orientVals;
    private float[] inR;
    private float[] I;
    private double azimuth;
    private double pitch;
    private double lat;
    private double lng;
    private double azimuthSave;
    private double pitchSave;
    private double latSave;
    private double lngSave;
    private Button mGetButton;
    private Button mAddButton;
    private TextView mTextView;
    private EditText mEditText;
    private Camera mCamera;
    private CameraPreview mPreview;
    public static int mGetInfoState = 0;
    public static int mAddInfoState = 0;
    public static boolean canTakePicture = true;
    private String pictureName;
    private Bitmap pictureBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddButton = (Button) findViewById(R.id.button_add);
        mGetButton = (Button) findViewById(R.id.button_capture);
        mTextView = (TextView) findViewById(R.id.textView);
        mEditText = (EditText) findViewById(R.id.editText);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Register this class as a listener for the accelerometer sensor
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);

        lat = 0;
        lng = 0;
        azimuth = 0;
        pitch = 0;

        latSave = 0;
        lngSave = 0;
        azimuthSave = 0;
        pitchSave = 0;

        pictureName = "";
        pictureBitmap = null;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getLocation();

        gravity = new float[3];
        geomag = new float[3];
        orientVals = new float[3];
        inR = new float[16];
        I = new float[16];

    }

    private void getLocation()
    {
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
        }
        else
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 4, this); //1 second and 4 meters
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Log.d("pierre", "got camera");
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    public void getInfo(View view)
    {
        setState();
    }

    private Void getData(String action)
    {
        String message = mEditText.getText().toString();
        if (action == "add" && message.length() == 0)
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Error!");
            alertDialog.setMessage("Please enter something to add to this location!");
            alertDialog.show();
            return null;
        }

        try
        {
            message = URLEncoder.encode(message, "UTF-8");
            String url = "http://www.eartheum.com/post/?lat=" + latSave + "&lng=" + lngSave + "&yaw=" + azimuthSave + "&pitch=" + pitchSave + "&message=" + message + "&action=" + action + "&uid=" + pictureName + "&client=1";
            HttpGetter get = new HttpGetter();
            get.execute(url);
        }
        catch (UnsupportedEncodingException e)
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Bad Character!");
            alertDialog.setMessage("You entered a bad character: " + e.toString());
            alertDialog.show();
        }

        return null;
    }

    private void setState()
    {
        switch (mGetInfoState)
        {
            case 0: //User pressed the get info button => Freeze the camera, and get data from the server
                snapShotOrientation(false);
                getData("get");
                mTextView.setText("Loading... Please Wait");
                mEditText.setText("");
                mEditText.setVisibility(View.INVISIBLE);
                mTextView.setBackgroundColor(0xAA000000);
                mTextView.setVisibility(View.VISIBLE);
                mTextView.invalidate();
                mGetButton.setText("Cool!");
                mAddButton.setEnabled(false);
                mAddButton.setVisibility(View.GONE);
                mAddButton.setText("Add Info");
                mAddInfoState = 0;
                mGetInfoState = 1;
                break;
            case 1: //User pressed the "cool" button => Go back to camera mode
                if (canTakePicture)
                {
                    //Only do this once the JPEG callback has returned...
                    mCamera.startPreview();
                }
                mEditText.setText("");
                mTextView.setText("");
                mTextView.setBackgroundColor(0x00000000);
                mTextView.invalidate();
                mGetButton.setText("Get Info");
                mAddButton.setEnabled(true);
                mAddButton.setVisibility(View.VISIBLE);
                mGetInfoState = 0;
                break;
            case 2: //This means we're cancelling the add action
                mGetInfoState = 1;
                mAddInfoState = 0;
                mEditText.setText("");
                mAddButton.setText("Add Info");
                mEditText.setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                setState();
                break;
        }
        //mTextView.setAlpha((float)0.75);
    }

    public void addInfo(View view)
    {
        switch (mAddInfoState)
        {
            case 0:
                pictureName = UUID.randomUUID().toString();
                snapShotOrientation(true);
                mEditText.setText("");
                mTextView.setVisibility(View.GONE);
                mEditText.setVisibility(View.VISIBLE);
                mEditText.setBackgroundColor(0xAA000000);
                mEditText.setText("");
                mTextView.invalidate();
                mGetButton.setEnabled(true);
                mGetButton.setVisibility(View.VISIBLE);
                mAddButton.setText("Add This Info!");
                mAddInfoState = 1;
                mGetInfoState = 2;
                mGetButton.setText("Cancel");
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mEditText, 0);
                break;
            case 1:
                //Upload Data!
                getData("add");
                if (pictureBitmap != null)
                {
                    uploadPicture();
                }
                mGetInfoState = 2;
                mAddInfoState = 0;
                setState();
                break;
        }
    }

    private void snapShotOrientation(boolean doTakePicture)
    {   if (doTakePicture)
        {
            pictureBitmap = null;
            canTakePicture = false;
            mCamera.takePicture(shutterCallBack, null, pictureCallBack); //This stops the preview and takes a picture...
        }
        else
        {
            playShutterSound();
            mCamera.stopPreview(); //This freezes the current shot.
        }
        getLocation(); //This updates (lat, lng)
        latSave = lat;
        lngSave = lng;
        azimuthSave = azimuth;
        pitchSave = pitch;
    }


    private final Camera.ShutterCallback shutterCallBack = new Camera.ShutterCallback() {
        public void onShutter()
        {
            playShutterSound();
        }
    };

    public void playShutterSound()
    {
        AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
    }

    private final Camera.PictureCallback pictureCallBack = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            canTakePicture = true;

            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize = 5;

            pictureBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        }
    };

    public void uploadPicture()
    {
        ImageUploadTask uploader = new ImageUploadTask();
        uploader.execute();
    }

    class ImageUploadTask extends AsyncTask<Void, Void, String> {

        private String webAddressToPost = "http://www.eartheum.com/post/image.php?uid=";

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
                HttpPost httpPost = new HttpPost(webAddressToPost + pictureName);

                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

                //pictureName
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                pictureBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageBytes = baos.toByteArray();
                String file = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                entity.addPart("image", new StringBody(file));

                httpPost.setEntity(entity);
                HttpResponse response = httpClient.execute(httpPost,localContext);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent(), "UTF-8"));

                String sResponse = reader.readLine();
                return sResponse;

            } catch (Exception e) {
                // something went wrong. connection with the server error
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }


    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {
            // If the sensor data is unreliable return
            if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                return;

            // Gets the value of the sensor that has been changed
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    gravity = sensorEvent.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    geomag = sensorEvent.values.clone();
                    break;
            }

            // If gravity and geomag have values then find rotation matrix
            if (gravity != null && geomag != null) {

                // checks that the rotation matrix is found
                boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
                if (success) {
                    float[] outR = new float[16];
                    SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
                    SensorManager.getOrientation(outR, orientVals);
                    azimuth = Math.toDegrees(orientVals[0]);
                    pitch = Math.toDegrees(orientVals[1]);
                }
            }
        }
    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    protected void onResume() {
        super.onResume();
        // Register this class as a listener for the accelerometer sensor
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        // Unregister the listener
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
        }
    }

    public void onProviderEnabled(java.lang.String s)
    {

    }

    public void onStatusChanged(java.lang.String s, int i, android.os.Bundle bundle)
    {

    }

    public void onProviderDisabled(java.lang.String s)
    {

    }

    private class HttpGetter extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            // TODO Auto-generated method stub
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(urls[0]);

            try {
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    Log.v("Getter", "Your data: " + builder.toString()); //response data
                    return builder.toString();

                } else {
                    Log.e("Getter", "Failed to download file");
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "";
        }

        //This is what gets the JSON from the server
        //Looks like this:
        //{eartheumId: n, eartheumName: s, eartheumDateStamp: s,
        //   entries: [{entryid: n, datestamp: s, lat: n, lng: n, yaw: n, pitch: n,
        //      elements: [{elementId: n, element: s, elementType: s, elementTypeId n, displayType s, displayTypeid d}, ...]}, ...]}

        protected void onPostExecute(String result)
        {
            String display = "";

            try
            {
                JSONObject json = new JSONObject(result);

                String status = json.getString("status");

                if (status.equals("found"))
                {
                    boolean isProtected = json.getBoolean("isprotected");
                    JSONObject eartheum = json.getJSONObject("eartheum");
                    String eartheumName = "Eartheum Name Goes Here"; //json.get("eartheumName").toString(); This is empty for now - no UI to enter this yet
                    String eartheumDateStamp = eartheum.getString("eartheumDateStamp");
                    JSONArray entries = eartheum.getJSONArray("entries");

                    display = eartheumName + "\n\n";

                    for (int i = 0; i < entries.length(); i++)
                    {
                        JSONObject entry = entries.getJSONObject(i);
                        String dateStamp = entry.getString("datestamp");
                        JSONArray elements = entry.getJSONArray("elements");

                        String lat = entry.getString("lat");
                        String lng = entry.getString("lng");
                        String yaw = entry.getString("yaw");
                        String pitch = entry.getString("pitch");

                        display += dateStamp + "\n";
                        display += "Data: lat: " + lat + " lng: " + lng + "\n yaw: " + yaw + " pitch: " + pitch + "\n";

                        for (int j = 0; j < elements.length(); j++)
                        {
                            JSONObject element = elements.getJSONObject(j);
                            String elementText = element.getString("element");
                            String elementType = element.getString("elementType");

                            display += elementText + "\n\n";
                        }
                    }
                }
                else
                {
                    display += "Nothing found...\n\n\nBe the first to add some information here!";
                }

            }
            catch (JSONException e)
            {
                display += e.toString();
            }

            display += "\n\nData:\nLat:" + latSave + "\nLng: " + lngSave + "\nYaw: " + azimuthSave + "\nPitch: " + pitchSave;

            mTextView.setText(display);
        }
    }


}