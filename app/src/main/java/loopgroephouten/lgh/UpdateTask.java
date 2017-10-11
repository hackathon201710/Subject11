package loopgroephouten.lgh;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import okhttp3.Response;
/**
 * Created by eddyspreeuwers on 10/11/17.
 */

public class UpdateTask extends AsyncTask<Void, Void, String> {

    private String url;
    private String version;
    private IAmReady iAmReady;

    UpdateTask(String url,IAmReady iAmReady ) {
       this.iAmReady = iAmReady;
       this.url = url;
    }



    @Override
    protected String doInBackground(Void... params) {

        try {

            //String url = "";

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = okHttpClient.newCall(request).execute();

            return response.body().string();

        } catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);
        }

        return null;
    }



    @Override
    protected void onPostExecute(final String response) {
        super.onPostExecute(response);
        try{
            Gson gson = new Gson();
            Version version = gson.fromJson(response, Version.class);
            Log.d("onPostExecute VERSION",version.version );
            iAmReady.onNewVersion(url, version.version);
        } catch(Exception e){
            Log.e("ERROR", e.getMessage(), e);
        }




    }

    @Override
    protected void onCancelled() {

    }
}
