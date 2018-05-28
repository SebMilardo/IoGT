package it.unict.dieei.iogt;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;


public class Main extends Activity implements
        SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CompoundButton.OnCheckedChangeListener
{

    static final String TAG = "IoGT";
    private static final int MY_PERMISSIONS_REQUEST = 27;
    private Intent mServiceIntent;
    private SensorManager mSensorManager;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private ResultReceiver mResultReceiver;
    static final int ACTIVATION_REQUEST = 47;
    DevicePolicyManager devicePolicyManager;
    ComponentName demoDeviceAdmin;
    ToggleButton cameraButton;
    static Activity mainActivity;

    public static boolean is_sdn = false;

    static Handler h = new Handler();
    static int delay = 10000; //milliseconds


    public final static float[] orientationBounds = new float[]
            {-1.1f, -0.7f, -1.2f, -1.4f, -0.1f, -0.2f};
    private final float[] mRotationMatrix = new float[9];
    private final double[] mLocationArray = new double[2];
    private final float[] mOrientationAngles = new float[3];
    private final String[] OrientationLabels = new String[]{
            " azimuth, rotation around the -Z axis\n",
            " pitch, rotation around the -X axis\n",
            " roll, rotation around the Y axis\n"};

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        if (mResultReceiver!=null) {
            unregisterReceiver(mResultReceiver);
            mResultReceiver=null;
        }
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultReceiver = new ResultReceiver(new Handler());
        IntentFilter inf = new IntentFilter("get.result");
        registerReceiver(mResultReceiver,inf);

        cameraButton = (ToggleButton) super.findViewById(R.id.toggle_camera);
        cameraButton.setOnCheckedChangeListener(this);

        mainActivity = this;
        // Initialize Device Policy Manager service and our receiver class
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        demoDeviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        if (!devicePolicyManager.isAdminActive(demoDeviceAdmin)) {
            // Activate device administration
            Intent intent = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    demoDeviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.IoGT_description));
            startActivityForResult(intent, ACTIVATION_REQUEST);
        }

        if (devicePolicyManager.getCameraDisabled(demoDeviceAdmin)){
            cameraButton.setChecked(false);
        }

        h.postDelayed(new Runnable(){
            public void run(){
                //do something
                mServiceIntent = new Intent(mainActivity, BackgroundService.class);
                mServiceIntent.putExtra("Rotation", mOrientationAngles);
                mServiceIntent.putExtra("Position", mLocationArray);
                startService(mServiceIntent);
                if (is_sdn) {
                    h.postDelayed(this, delay);
                }
            }
        }, delay);
    }


    /**
     * Called when the state of toggle button changes. In this case, we send an
     * intent to activate the device policy administration.
     */
    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {

        switch(button.getId()){
            case R.id.toggle_camera:
                if (isChecked) {
                    Log.d(TAG, "Enabling camera now");
                    devicePolicyManager.setCameraDisabled(demoDeviceAdmin, false);
                } else {
                    Log.d(TAG, "Disabling camera now");
                    devicePolicyManager.setCameraDisabled(demoDeviceAdmin, true);
                }
                break;
        }

        Log.d(TAG, "onCheckedChanged to: " + isChecked);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        Sensor magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't receive any more updates from either sensor.
        mSensorManager.unregisterListener(this);
    }


    /**
     * Called when startActivityForResult() call is completed. The result of
     * activation could be success of failure, mostly depending on user okaying
     * this app's request to administer the device.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVATION_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Administration enabled!");
                } else {
                    Log.i(TAG, "Administration enable FAILED!");
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

        updateOrientationAngles();
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.
        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < mOrientationAngles.length; i++) {
            stringBuilder.append(String.format("%.1f", mOrientationAngles[i])).append(' ').append(OrientationLabels[i]);
        }
        TextView textOrientation = (TextView) findViewById(R.id.textOrientation);
        textOrientation.setText(stringBuilder.toString());

        if (is_sdn) {
            if (checkOrientation()) {
                cameraButton.setChecked(false);
            } else {
                cameraButton.setChecked(true);
            }
        }
        stringBuilder = new StringBuilder();

        for (int i = 0; i < mRotationMatrix.length; i++) {
            stringBuilder.append(String.format("%.1f", mRotationMatrix[i]));
        }
        TextView textRotation = (TextView) findViewById(R.id.textRotation);
        textRotation.setText(stringBuilder.toString());


    }

    private boolean checkOrientation(){
        return mOrientationAngles[0] >= orientationBounds[0] &&
                mOrientationAngles[0] < orientationBounds[1] &&
                mOrientationAngles[1] >= orientationBounds[2] &&
                mOrientationAngles[1] < orientationBounds[3] &&
                mOrientationAngles[2] >= orientationBounds[4] &&
                mOrientationAngles[2] < orientationBounds[5];
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    MY_PERMISSIONS_REQUEST);
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            TextView mLatitudeText = (TextView)findViewById(R.id.textLatitude);
            TextView mLongitudeText = (TextView)findViewById(R.id.textLongitude);
            mLocationArray[0] = mLastLocation.getLatitude();
            mLocationArray[1] = mLastLocation.getLongitude();
            mLatitudeText.setText("Latitude " + String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText("Longitude " + String.valueOf(mLastLocation.getLongitude()));
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    public static class ResultReceiver extends BroadcastReceiver {

        private final Handler handler; // Handler used to execute code on the UI thread

        public ResultReceiver (Handler handler) {
            this.handler = handler;
        }

        @Override
        public void onReceive(final Context context, final Intent intento) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ToggleButton cameraButton;
                    cameraButton = (ToggleButton) mainActivity.findViewById(R.id.toggle_camera);

                    String result = intento.getStringExtra("result");
                    switch(result) {
                        case "ENABLE_CAMERA":
                            cameraButton.setChecked(true);
                            break;

                        case "DISABLE_CAMERA":
                            cameraButton.setChecked(false);
                            break;

                        case "DISABLE_SDN":
                            is_sdn = false;
                            break;

                        default:
                            is_sdn = true;
                            String[] rule = result.split(" ");
                            if (rule.length > orientationBounds.length) {
                                for (int i = 0; i < orientationBounds.length; i++) {
                                    orientationBounds[i] = Float.parseFloat(rule[i + 1]);
                                }
                            }
                            break;
                    }
                }
            });
        }
    }


    public void tee(View view) {

        KeyPairGenerator kpg = null;
        try {

            kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(new KeyGenParameterSpec.Builder(
                    "key2",
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .build());

            // Dopo aver impostato tutti i parametri necessari posso generare una coppia di chiavi
            kpg.generateKeyPair();
            //--------------------------------------------------------------------------------------


            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(
                    "key1",
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(5 * 60)
                    .build());

            // Posso adesso generare la chiave
            kg.generateKey();
            //--------------------------------------------------------------------------------------

            // key retrieval
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            Enumeration<String> aliases = ks.aliases();
            Log.i(TAG, "Elements in the KeyStore");
            Log.i(TAG, "-------------------------------------------------------------------");
            KeyInfo keyInfo;

            while (aliases.hasMoreElements()) {
                KeyStore.Entry entry = ks.getEntry(aliases.nextElement(), null);
                Key current;


                if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                    Log.i(TAG, "Type: SECRET");
                    KeyStore.SecretKeyEntry pentry = (KeyStore.SecretKeyEntry) entry;
                    SecretKey secretKey = pentry.getSecretKey();
                    SecretKeyFactory secretFactory = SecretKeyFactory
                            .getInstance(secretKey.getAlgorithm(), "AndroidKeyStore");
                    keyInfo = (KeyInfo) secretFactory.getKeySpec(secretKey, KeyInfo.class);
                    current = secretKey;
                } else {
                    Log.i(TAG, "Type: PUBLIC/PRIVATE");
                    PrivateKey pkey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
                    KeyFactory factory = KeyFactory.getInstance(pkey.getAlgorithm(), "AndroidKeyStore");
                    keyInfo = factory.getKeySpec(pkey, KeyInfo.class);
                    current = pkey;
                    Log.i(TAG, "Public Key: " + new BigInteger(((KeyStore.PrivateKeyEntry) entry)
                            .getCertificate().getPublicKey().getEncoded()).toString(16));
                }

                Log.i(TAG, "Keystore Alias: " + keyInfo.getKeystoreAlias());
                Log.i(TAG, "Algorithm: " + current.getAlgorithm());
                Log.i(TAG, "isInsideSecureHardware: " + keyInfo.isInsideSecureHardware());
                Log.i(TAG, "isUserAuthenticationRequirementEnforcedBySecureHardware: " +
                        keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware());
                Log.i(TAG, "BlockModes: " + Arrays.toString(keyInfo.getBlockModes()));
                Log.i(TAG, "Digest: " + Arrays.toString(keyInfo.getDigests()));
                Log.i(TAG, "-------------------------------------------------------------------");
            }


            //--------------------------------------------------------------------------------------

            // firmo la stringa "Ciao"
            KeyStore.Entry entry = ks.getEntry("key2", null);
            Signature s;
            byte[] signature = new byte[0];

            String string = "Ciao";
            Log.i(TAG, "String: " + string);

            if (entry instanceof KeyStore.PrivateKeyEntry) {
                s = Signature.getInstance("SHA256withECDSA");
                s.initSign(((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
                s.update("Ciao".getBytes());
                signature = s.sign();
                Log.i(TAG, "Signature: " + new BigInteger(signature).toString(16));
            }

            //--------------------------------------------------------------------------------------

            // verifico signature sulla stringa "Ciao"
            s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(((KeyStore.PrivateKeyEntry) entry).getCertificate());
            s.update("Ciao".getBytes());
            boolean valid = s.verify(signature);

            //--------------------------------------------------------------------------------------

            Log.e(TAG, "isValid: " + valid);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}