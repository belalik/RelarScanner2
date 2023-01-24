package com.example.android.relarscanner2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import relarscanner2.BuildConfig;
import relarscanner2.R;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "thomas - MainActivity";

    private Activity context;

    private TextView txtViewResults;

    RequestQueue queue;

    DownloadManager manager;

    private RELARPreferencesManager prefManager;

    // previous test, not used anymore
    private String SHEETY_TEST = "";

    // read from local 
    private String AK = BuildConfig.AK;

    private String VARIABLE_GOOGLE_SHEET_FULL_URL = "";

    // IMPORTANT heroku was being used initially - now only available as paid service, now using a free alternative but potentially unreliable service..
    // see https://techcrunch.com/2022/08/25/heroku-announces-plans-to-eliminate-free-plans-blaming-fraud-and-abuse/
    // and https://help.heroku.com/RSBRUH58/removal-of-heroku-free-product-plans-faq
    private String HEROKU_APP_URL = "";

    private String QRCodeRead;

    Button exitButton;
    Button btnScan;
    Button loadButton;

    ArrayList<RelarDataItem> rowItems;

    boolean savedListLoaded;

    RelarDataItem itemToUse;

    String filename;
    private URL url = null;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    int filetype = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        initialiseViews();
        initializePreferences();
        attemptToLoadList();

        verifyStoragePermissions(this);

        rowItems = new ArrayList<RelarDataItem>();



        //googleSheetParse();

        // optional - uncomment this if you want to delete saved list from shared preferences.
        // useful for testing purposes.
        //prefManager.deleteAllPreferences();


        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadButtonPressed();
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: in here");
                scanButtonPressed();
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //System.exit(0);
                //finishAndRemoveTask();
                finishAffinity();
                System.exit(0);
            }
        });

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA}, PackageManager.PERMISSION_GRANTED);

    }

    /**
     * Initialize all basic views
     */
    private void initialiseViews() {
        txtViewResults = findViewById(R.id.tv_result);
        exitButton = findViewById(R.id.btn_exit);
        btnScan = findViewById(R.id.btn_scan);
        loadButton = findViewById(R.id.btn_load);
    }

    /**
     * Initialize shared prefs manager. Used to save/load saved list of steps.
     */
    private void initializePreferences() {
        prefManager = RELARPreferencesManager.instance(this);
    }

    /**
     * Attempt to load saved list from shared prefs.
     * List will be saved in string format.
     * If list is found (stepsListJson is not NULL} then enable load key.
     */
    private void attemptToLoadList() {
        String stepsListJson = prefManager.fetchValueString(RELARPreferencesManager.LIST_OF_STEPS);
        if (stepsListJson == null || stepsListJson == "") {
            Log.i(TAG, "attemptToLoadList: NO LIST FOUND");
            savedListLoaded = false;
            loadButton.setEnabled(false);
            loadButton.setClickable(false);
            loadButton.setBackgroundColor(Color.GRAY);
            txtViewResults.setText(R.string.initial_message_no_saved_list);
        }
        else {
            Log.i(TAG, "attemptToLoadList: LIST FOUND !");
            savedListLoaded = true;
            loadButton.setEnabled(true);
            txtViewResults.setText(R.string.initial_message_saved_list_found);
        }

    }

    /**
     * If load button was clickable, that means that a list was indeed saved and found.
     * Hence load data from shared prefs, and call StepsListActivity.
     */
    private void loadButtonPressed() {
        String stepsListJson = prefManager.fetchValueString(RELARPreferencesManager.LIST_OF_STEPS);

        Intent i = new Intent(context, StepsListActivity.class);

        i.putExtra(RELARPreferencesManager.LIST_OF_STEPS, stepsListJson);

        // if loading saved list, do not ask user to save list (it's already saved).  This boolean
        // is read in StepsListActivity and controls whether that dialog is shown.
        i.putExtra(StepsListActivity.SHOW_DIALOG, false);

        Toast.makeText(this, "Loading saved list", Toast.LENGTH_LONG).show();

        context.startActivity(i);
    }

    /**
     * Initiates Scanning for QR Code.  todo need to update deprecated code here!
     * help - documentation is lacking, some links:
     * - https://github.com/journeyapps/zxing-android-embedded/issues/628
     * - (example?) - https://github.com/journeyapps/zxing-android-embedded/blob/master/sample/src/main/java/example/zxing/MainActivity.java
     */
    public void scanButtonPressed(){
        Log.i(TAG, "scanButtonPressed: OK, scanButtonPressed called");
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        //intentIntegrator.
        intentIntegrator.initiateScan();

        //googleSheetParse();
    }


    public void scanButtonPressed2() {
        // todo replace deprecated code here and test ...
    }

    /**
     * Deprecated code that still works but should be updated at some point .. see previous notes.
     * At the time being, it gets the result of the Scan and then calls {@link #googleSheetParse()}
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (intentResult != null){
            if (intentResult.getContents() == null){
                txtViewResults.setText("Cancelled");
            }else {
                //results.setText(intentResult.getContents());
                txtViewResults.setText("QR Code detected\nConnecting...");
                exitButton.setVisibility(View.VISIBLE);

                QRCodeRead = intentResult.getContents();

                Log.i(TAG, "onActivityResult: QRCode read, it is: \n"+QRCodeRead);

                googleSheetParse();

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private void googleSheetParse() {

        // emptying arrayList if it already has items from previous scan.
        if (!rowItems.isEmpty()) {
            rowItems.clear();
        }

        queue = Volley.newRequestQueue(this);

        // todo need to update this deprecated code.
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        //update


        VARIABLE_GOOGLE_SHEET_FULL_URL = "https://gsx2json.com/api?api_key="+AK+"&id="+QRCodeRead+"&sheet=Sheet1";

        // heroku IS NOT used anymore - only available as paid service..
        HEROKU_APP_URL = "https://relar-datakeeper.herokuapp.com/api?api_key="+AK+"&id="+QRCodeRead+"&sheet=Sheet1";

        //Log.i(TAG, "googleSheetParse: Variable google sheet url is: "+HEROKU_APP_URL);

        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                VARIABLE_GOOGLE_SHEET_FULL_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //progressDialog.dismiss();

                Log.i(TAG, "onResponse: starting");

                try {
                    JSONObject jsonObject = new JSONObject(response);

                    JSONArray rows = jsonObject.getJSONArray("rows");

                    //Log.i(TAG, "onResponse: rows length is: "+rows.length());

                    for (int i=0; i<rows.length(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        int flag = row.getInt("flag");

                        // we reached an empty row, break loop !!!
                        if (flag == 99) {
                            //Log.i(TAG, " *** break reached at i="+i);
                            break;
                        }
                        if (flag == 0) {
                            //Log.i(TAG, "*** continue reached at i="+i);
                            continue;
                        }

                        // not using serial number on last version
                        //int serialNumber = row.getInt("serial_number");

                        //String zoomTitle = row.getString("zoom_title");
                        //String zoomURL = row.getString("zoom_url");
                        
                        String title = row.getString("TITLE");
                        String url = row.getString("URL");

                        // todo simplify this - not using useThis value in last version
                        //String useThisStringValue = row.getString("use_this");
                        //boolean useThis;

                        // google sheets wants capitals !!!
                        // not using useThis in last version
//                        if (useThisStringValue.equalsIgnoreCase("true")) {
//                            useThis = true;
//                        }
//                        else {
//                            useThis = false;
//                        }

                        //int notUsedSomeNumber = row.getInt("not_used_some_number");

                        //rowItems.add(new RelarDataItem(flag, serialNumber, zoomTitle, zoomURL, fileTitle, fileURL, useThis));
                        //rowItems.add(new RelarDataItem(flag, serialNumber, title, url, useThis));
                        rowItems.add(new RelarDataItem(flag, title, url));

                    } // for loop reading rows ends here...

                    progressDialog.dismiss();

                    Intent i = new Intent(context, StepsListActivity.class);

                    Gson gson = new Gson();
                    String allSteps = gson.toJson(rowItems);

                    //showSimpleDialog();

                    //prefManager.storeValueString(RELARPreferencesManager.LIST_OF_STEPS, allSteps);
                    
                    i.putExtra(RELARPreferencesManager.LIST_OF_STEPS, allSteps);

                    i.putExtra(StepsListActivity.SHOW_DIALOG, true);

                    context.startActivity(i);

                    //googleSheetParseContinue();


                } catch (JSONException e) {
                    e.printStackTrace();
                    //Log.i(TAG, "onResponse: JSON exception, "+e);
                    Log.e("JSON exception: ","exception "+e);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.i(TAG, "onErrorResponse: volley error: "+error);
                Log.e(TAG, "VolleyError caught: "+error);
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

    }

    /*private void googleSheetParseContinue() {

        itemToUse = findFirstUsable();

        if (itemToUse.getFlag() == 1) {
            //Log.i(TAG, "onActivityResult: it must be a zoom link, here: \n"+itemToUse.getZoomURL());

            String correctLinkToGiveToZoom = "zoomus://zoom.us/join?action=join&confno=";

            //long meetingId = findMeetingId(QRCodeRead);
            //String meetingId2 = findMeetingId2(QRCodeRead);
            String meetingId2 = findMeetingId2(itemToUse.getURL());

            txtViewResults.setText("ZOOM link detected on previous scan\nPlease Re-Scan or Exit.");


            //Log.i(TAG, "onActivityResult: with alternative method, link would be \n" + correctLinkToGiveToZoom+meetingId2);

            //correctLinkToGiveToZoom += meetingId;
            correctLinkToGiveToZoom += meetingId2;

            //Log.i(TAG, "onActivityResult: meeting ID read is: "+meetingId);
            //Log.i(TAG, "onActivityResult: final string sent is: "+correctLinkToGiveToZoom);

            //todo IMPORTANT - following should be used when link is a zoom URL.
            openZoomDirectly(correctLinkToGiveToZoom);


        }
        else if (itemToUse.getFlag() == 2) {
            txtViewResults.setText("FILE link detected on previous scan\nPlease Re-Scan or Exit.");
            //Log.i(TAG, "onActivityResult: it must be a file link, here: \n"+itemToUse.getFileURL());

            //openFile(itemToUse.getFileURL());
            handleFile(itemToUse.getURL());
        }
        else if (itemToUse.getFlag() == 3 || itemToUse.getFlag() == 4) {
            Log.i(TAG, "onResponse: testing youtube link:  .. "+itemToUse.getURL());
            openOtherLink(itemToUse.getURL());
        }
        else {
            //Log.i(TAG, "onActivityResult: its empty..");
        }
    }*/


    /**
     * Not used anymore - left here for reference.  Was used in previous app versions when column "use_this"
     * in google sheet identified which step was selected.
     * @return RelarDataItem to use
     */
    private RelarDataItem findFirstUsable() {

        Log.i(TAG, "findFirstUsable: runs ");

        for (int i=0; i<rowItems.size(); i++) {
            //Log.i(TAG, "findFirstUsable: item at pos. "+i+" has isUseThis: "+rowItems.get(i).isUseThis());
        }

        RelarDataItem toUse = rowItems.stream()
                .filter(RelarDataItem::isUseThis)
                .findAny()
                .orElse(null);

        if (toUse == null) {
            Log.i(TAG, "findFirstUsable: toUse is NULL");
            //toUse = new RelarDataItem(1, 100, "explicit zoom title", "zoomExplicit", "fileEx", "fileu", true);
            //toUse = new RelarDataItem();
        }
        //Log.i(TAG, "findFirstUsable: rowItems length is: "+rowItems.size());
        //Log.i(TAG, "findFirstUsable: flag is: "+toUse.getFlag());
        return toUse;
    }




    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

}