package com.example.android.relarscanner2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import relarscanner2.R;

public class StepsListActivity extends AppCompatActivity {

    private static final String TAG = "StepsListActivity - thomas";

    public static final String SHOW_DIALOG = "show dialog";

    ArrayList<RelarDataItem> rowItems;

    CustomListAdapter adapter;

    ListView listview;

    private RELARPreferencesManager prefManager;

    boolean showDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_steps);

        initializePreferences();

        listview = findViewById(R.id.listview);

        Intent i = getIntent();

        showDialog = i.getBooleanExtra(SHOW_DIALOG, false);

        Gson gson = new Gson();
        //String json = prefs.getString("MyPhotos", null);
        Type type = new TypeToken<ArrayList<RelarDataItem>>() {}.getType();
        rowItems = gson.fromJson(i.getStringExtra(RELARPreferencesManager.LIST_OF_STEPS), type);

        Log.i(TAG, "onCreate: size is: "+rowItems.size());

        adapter = new CustomListAdapter(this, R.layout.listview_row, rowItems);

        listview.setAdapter(adapter);

        if (showDialog) {
            showSimpleDialog();
        }


    }


    private void initializePreferences() {
        prefManager = RELARPreferencesManager.instance(this);

    }

    private void showSimpleDialog(){

        Gson gson = new Gson();
        String allSteps = gson.toJson(rowItems);

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setMessage("Saving this list will overwrite previously saved list");

        //Setting message manually and performing action on button click
        builder.setMessage("Saving this list will overwrite previously saved list")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'Yes' Button - save the list in shared prefs
                        prefManager.storeValueString(RELARPreferencesManager.LIST_OF_STEPS, allSteps);

                        Toast.makeText(getApplicationContext(),"List saved",Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'No' Button

                        Toast.makeText(getApplicationContext(),"List NOT saved",Toast.LENGTH_SHORT).show();
                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        //Setting the title manually
        alert.setTitle("Save Current List?");
        alert.show();
    }
}
