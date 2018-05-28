package it.unict.dieei.iogt;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Seby on 27/07/16.
 */

    public class BackgroundService extends Service {



    static final String TAG = "IoGT - Background";

        @Override
        public IBinder onBind(Intent intent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            new DoBackgroundTask().execute(intent);

            return START_STICKY;
        }


    private class DoBackgroundTask extends AsyncTask<Intent, Void, String> {

        private String requestHttp(final float[] rotation, final double[] position){

            String result = "";
            JSONObject json = new JSONObject();

            try {
                JSONArray rot = new JSONArray(rotation);
                JSONArray pos = new JSONArray(position);
                json.put("rotation", rot);
                json.put("position", pos);
                Log.d(TAG, json.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }


            try {
                /*
                // default settings for all sockets
                IO.setDefaultSSLContext(mySSLContext);
                IO.setDefaultHostnameVerifier(myHostnameVerifier);

                // set as an option
                opts = new IO.Options();
                opts.sslContext = mySSLContext;
                opts.hostnameVerifier = myHostnameVerifier;
                socket = IO.socket("https://localhost", opts);
                */

                Socket socket = new Socket();
                //socket.connect(new InetSocketAddress("10.0.1.2",9100));
                socket.connect(new InetSocketAddress("18.189.88.87",9100));

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(json.toString());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                result = (String)ois.readObject();
                oos.close();
                socket.close();
                Log.i(TAG,result);
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {

            }
            return result;
        }

        @Override
        protected void onPostExecute(String result){
            Intent intent = new Intent("get.result");
            intent.putExtra("result",result);
            sendBroadcast(intent);
        }


        @Override
        protected String doInBackground(Intent... params) {

            if (params != null && params.length > 0 &&
                    params[0] != null &&
                    params[0].getFloatArrayExtra("Rotation") != null &&
                    params[0].getDoubleArrayExtra("Position") != null) {
                return requestHttp(params[0].getFloatArrayExtra("Rotation"),
                        params[0].getDoubleArrayExtra("Position"));
            }
            return "";
        }
    }
    }

