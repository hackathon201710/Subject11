package loopgroephouten.lgh;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


final class JavaScriptInterface {

    public static Map<String, String> urls = new HashMap<String, String>();
    public static Map<String, String> versionsURLs = new HashMap<String, String>();
    public static Map<String, String> versions = new HashMap<String, String>();

    public static Set<String> updated = new HashSet<String>();

    {
        urls.put("vehicle", "http://www.eddyspreeuwers.nl/ng4");
        urls.put("person", "http://hackathon20171011102444.azurewebsites.net/");
        urls.put("location", "http://www.eddyspreeuwers.nl/location.html");

        versionsURLs.put("http://www.eddyspreeuwers.nl/ng4", "http://www.eddyspreeuwers.nl/ng4/version.json");
        versionsURLs.put("http://hackathon20171011102444.azurewebsites.net/", "http://hackathon20171011102444.azurewebsites.net/version");
        versionsURLs.put("http://www.eddyspreeuwers.nl/location.html", "http://www.eddyspreeuwers.nl/version.json");



    }

    @android.webkit.JavascriptInterface
    void sayHi(String name) {
        Log.e("hi ", name);

    }

    @android.webkit.JavascriptInterface
    void execute(String command, String param) {
        Log.e("command ", command);
        Log.e("param ", param);
        String url = urls.get(command);
        if (param != null) {
            url = url + "?" + param;
        }

        MainActivity.instance.nav2Url(url);

    }
}


class WebContent {
    String contentType;

    byte[] content;

    WebContent(String contentType, byte[] content) {
        this.content = content;
        this.contentType = contentType;
    }

}




public class MainActivity extends AppCompatActivity implements IAmReady {

    boolean downloadUpdate;

    private TextView mTextMessage;
    private WebView mywebview;
    public static MainActivity instance;

    private LinearLayout linearLayout;

    private WebSettings webSettings;
    private Map<String, WebContent> resources = new HashMap<String, WebContent>();

    //invoked when pressing a menu button
    private boolean setUrl(int resId) {
        downloadUpdate = false;
        String navUrl = getResources().getString(resId);

        return setUrl(navUrl);
    }

    private boolean setUrl(String navUrl) {
        String versionURL = JavaScriptInterface.versionsURLs.get(navUrl);

        if (JavaScriptInterface.updated.contains(navUrl)) {
            downloadUpdate = true;
            JavaScriptInterface.updated.remove(navUrl);
        } else {
            new UpdateTask(versionURL, this).execute();
        }

        if (!mywebview.getOriginalUrl().equals(navUrl)) {
            mywebview.loadUrl(navUrl);
            Log.d("SET URL", navUrl);
            return true;
        }
        return false;
    }

    public void nav2Url(final String url) {
        Log.d("nav2Url", url);
        mywebview.post(new Runnable() {
            public void run() {
                mywebview.loadUrl(url);
            }
        });

    }

    public MainActivity() {
        this.instance = this;
    }


    private String getSoftwareVersion() {
        PackageInfo pi;
        try {
            pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return "version:" + pi.versionName + " package: " + pi.packageName;
        } catch (final PackageManager.NameNotFoundException e) {
            return "na";
        }
    }


    public String getAppLabel() {
        PackageManager packageManager = getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(getApplicationInfo().packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            String navUrl;
            switch (item.getItemId()) {
                case R.id.navigation_vehicle:
                    setUrl(R.string.url_vehicle);

                    //mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_person:
                    setUrl(R.string.url_person);
                    //mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_location:
                    setUrl(R.string.url_location);
                    //mTextMessage.setText(R.string.title_dashboard);
                    return true;
            }
            return false;
        }

    };


    WebContent getWebContent(final String requestURL) {

        //final String requestURL = url.toString();
        Log.e("request", requestURL);
        WebContent wc = resources.get(requestURL);

        if (!downloadUpdate && (wc != null && !getConnected())) {
             return wc;
        }

        new Thread(new Runnable() {
            public void run() {

                URL url;
                String line;
                String page;
                StringBuffer sb = new StringBuffer();
                byte[] buff;
                int nrOfBytes = 0;
                String contentType = "";
                HttpURLConnection urlConnection = null;
                try {
                    url = new URL(requestURL);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    BufferedInputStream bin = new BufferedInputStream(urlConnection.getInputStream());
                    contentType = "" + urlConnection.getContentType();
                    Log.d("Content-type: ", contentType);
                    contentType = contentType.split(";")[0];
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[1024 * 8];
                    while ((nRead = bin.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                        Log.d("reading....", requestURL);
                    }
                    buffer.flush();
                    byte[] byteArray = buffer.toByteArray();
                    resources.put(requestURL, new WebContent(contentType, byteArray));
                    Log.d("store: ", requestURL);
                    Log.d("NEW", requestURL);
                    //MainActivity.instance.mywebview.clearCache(true);

                } catch (Exception e) {
                    Log.e("error reading:", requestURL);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }


            }
        }).start();
        return resources.get(requestURL);
    }

    boolean getConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linearLayout = (LinearLayout) findViewById(R.id.container);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mywebview = (WebView) findViewById(R.id.mywebview);
        webSettings = mywebview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //webSettings.setDomStorageEnabled(true);
        //webSettings.setAllowFileAccessFromFileURLs(true);
        //webSettings.setAllowFileAccess(true);

        webSettings.setGeolocationEnabled(true);
        webSettings.setDatabaseEnabled(true);
        //webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        //ServiceWorkerController.getInstance().getServiceWorkerWebSettings().setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setAppCacheEnabled(false);

        Log.d("VERSION:", getSoftwareVersion() + " " + getAppLabel());

        mywebview.addJavascriptInterface(new JavaScriptInterface(), "webview");

        mywebview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e("eddy", consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }

        });

        mywebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.e("eddy", request.toString());
                Log.d("VERSION:", getSoftwareVersion());
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                //mywebview.clearCache(true);
                String requestURL = request.getUrl().toString();
                Log.d("request", requestURL);

                WebContent wc = getWebContent(requestURL);
                Log.d("retrieve: ", requestURL);
                if (!downloadUpdate && wc != null) {
                    Log.d("retrieved: ", requestURL);
                    InputStream data = new ByteArrayInputStream(wc.content);
                    return new WebResourceResponse(wc.contentType, "UTF-8", data);
                } else {
                    if (request.getUrl().toString().contains("activiteiten")) {
                        Log.e("activiteiten", request.getUrl().toString());
                        String offLinePage =
                                "<html>" +
                                        "<head></head>" +
                                        "<body>" +
                                        "<h1>Offline page! loading ....</h1>" +
                                        "<a href=\"javascript:webview.sayHi('eddy')\">Ok</a>" +
                                        "</body>" +
                                        "</html>";


                        InputStream data = new ByteArrayInputStream(offLinePage.getBytes());
                        return new WebResourceResponse("text/html", "UTF-8", data);
                    }

                    Log.e("requestvloading... ", request.getUrl().toString());
                    return super.shouldInterceptRequest(view, request);
                }
            }
        });

        //mywebview.clearCache(true);

        //String summary = "<html><body>You scored <b>192</b> points.</body></html>";
        //mywebview.loadData(summary, "text/html", null);
        String lghUrl = getResources().getString(R.string.url_vehicle);
        mywebview.loadUrl(lghUrl);

    }

    @Override
    public void onNewVersion(String navVersionUrl, VersionModel remoteVersion) {
        String localVersion = JavaScriptInterface.versions.get(navVersionUrl);

        if (localVersion == null) {
            JavaScriptInterface.versions.put(navVersionUrl, remoteVersion.version);
            return;
        }

        String navUrl = JavaScriptInterface.urls.get(remoteVersion.name);

        if (localVersion.equals(remoteVersion.version)) {
            JavaScriptInterface.updated.remove(navUrl);
        }   else {
            JavaScriptInterface.updated.add(navUrl);
            JavaScriptInterface.versions.put(navVersionUrl, remoteVersion.version);

            notifyUserOfUpdate(remoteVersion);
        }
    }

    private void notifyUserOfUpdate(final VersionModel versionModel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle(R.string.update_message_title);
        builder.setMessage(R.string.update_message_body);
        builder.setPositiveButton(R.string.menuUpdate, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String navUrl = JavaScriptInterface.urls.get(versionModel.name);
                setUrl(navUrl);
                Log.d("UPDATE URL", navUrl);
            }
        });

        builder.setNegativeButton(R.string.menuLater, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();
    }
}
