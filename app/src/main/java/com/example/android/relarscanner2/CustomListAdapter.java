package com.example.android.relarscanner2;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import relarscanner2.BuildConfig;
import relarscanner2.R;

import static android.content.Context.DOWNLOAD_SERVICE;


public class CustomListAdapter extends ArrayAdapter {

    private static final String TAG = "Thomas - CustomListAdapter";

    private final Activity context;

    private ArrayList<RelarDataItem> listOfSteps;

    String filename;

    public static final int ZOOM_REQUEST = 99;

    ProgressDialog progressDialog;

    public CustomListAdapter(@NonNull Activity context, int resource, @NonNull ArrayList listOfSteps) {
        super(context, resource, listOfSteps);

        this.listOfSteps = listOfSteps;
        this.context = context;

        //initializePreferences();
    }



    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.listview_row, null, true);

        TextView stepTitle = rowView.findViewById(R.id.stepTitle);
        ImageView stepIcon = rowView.findViewById(R.id.stepIcon);

        stepTitle.setText(listOfSteps.get(position).getTitle());

        if (listOfSteps.get(position).getFlag() == 1) {
            stepIcon.setImageResource(R.drawable.zoom_icon);
        }
        else if (listOfSteps.get(position).getFlag() == 2) {
            // get correct icon - so far identifying pdf, ppt/pptx and 'other' as recognizable file types.
            // remember it has to be an actual physical file URL (no onedrive / google drive URLs)

            stepIcon.setImageResource(getFiletypeIcon(listOfSteps.get(position).getURL()));
        }
        else if (listOfSteps.get(position).getFlag() == 3) {
            stepIcon.setImageResource(R.drawable.youtube_icon3);
        }
        else if (listOfSteps.get(position).getFlag() == 4) {
            stepIcon.setImageResource(R.drawable.teams_icon);
        }
        else if (listOfSteps.get(position).getFlag() == 5) {
            stepIcon.setImageResource(R.drawable.blue_question_mark);
        }

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int flag = listOfSteps.get(position).getFlag();
                String url = listOfSteps.get(position).getURL();
                Log.i(TAG, "onClick: flag is: "+flag+", url: "+url);

                handleClick(listOfSteps.get(position));
            }
        });

        return rowView;
    }

    private int getFiletypeIcon(String url) {
        filename = url;
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        String filenameExtension = filename.substring(filename.length()-3);
        if (filenameExtension.equalsIgnoreCase("pdf")) {
            return R.drawable.round_pdf_icon;
        }
        else if (filenameExtension.equalsIgnoreCase("ppt") || filenameExtension.equalsIgnoreCase("ptx")) {
            return R.drawable.round_ppt_icon;
        }
        else if (filenameExtension.equalsIgnoreCase("jpg") ||
                filenameExtension.equalsIgnoreCase("png") ||
                filenameExtension.equalsIgnoreCase("gif") ||
                        filenameExtension.equalsIgnoreCase("jpeg"))
        {
            return R.drawable.image_icon;
        }
        else  {
            return R.drawable.blue_question_mark;
        }

    }

    private void handleClick(RelarDataItem selectedStep) {
        int flag = selectedStep.getFlag();
        String url = selectedStep.getURL();

        if (flag == 1) {
            openZoom(selectedStep);
        }
        else if (flag == 2) {
            handleFile(selectedStep);
        }
        else if (flag == 3 || flag == 4 || flag == 5) {
            openOtherLink(selectedStep.getURL());
        }
    }

    private void handleFile(RelarDataItem selectedStep) {
        try {
            URL url = new URL(selectedStep.getURL());
            Log.i(TAG, "handleFile: file link is: "+url);
            filename = url.getPath();
            filename = filename.substring(filename.lastIndexOf('/') + 1);

            // use appId as part of filename
            String appId = BuildConfig.APPLICATION_ID;

            //todo check this
            //filename = appId+"_"+filename;

            downloadFile(selectedStep);
        } catch (MalformedURLException e) {
            //Log.i(TAG, "handlePDF: exception firing");
            e.printStackTrace();
        }

    }

    private void downloadFile(RelarDataItem selectedStep) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(selectedStep.getURL() + ""));
        request.setTitle(filename);
        //filetype = getFileType();
        Log.i(TAG, "downloadFile: filename is: "+filename);

        Log.i(TAG, "downloadFile: mimeType from url is: "+getMimeFromFileURL(selectedStep.getURL()));
        String mimeType = getMimeFromFileURL(selectedStep.getURL());

        if (mimeType == null) {
            selectedStep.setMimeType("undefined");
        }
        else {
            selectedStep.setMimeType(mimeType);
        }
        request.setMimeType(mimeType);


        request.allowScanningByMediaScanner();
        request.setAllowedOverMetered(true);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);



        if (filename != null && !TextUtils.isEmpty(filename)) {
            File file = new File(Environment.getExternalStorageDirectory().toString() + File.separator + Environment.DIRECTORY_DOWNLOADS,"/"+filename);
            if (file.exists()) {
                //Log.i(TAG, "downloadFile: - found file: "+file.getPath());
                //file.delete();
                openFile2(selectedStep);
                return;
            } else {
                //Log.i(TAG, "downloadFile: new file - downloading...");
                //Toast.makeText(context, "Downloading file, please wait...", Toast.LENGTH_LONG).show();

                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage("Downloading file");
                progressDialog.show();

                dm.enqueue(request);
                // write here code for download new file
            }
        }

        //dm.enqueue(request);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {

                    progressDialog.dismiss();
                    /** Do your coding here **/
                    openFile2(selectedStep);
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        //openFile2(selectedStep);
    }


    private void openFile2(RelarDataItem selectedStep) {
        //Log.i(TAG, "openFile2: thomas trying to open file...");
        File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+filename);
        //Uri uri= FileProvider.getUriForFile(MainActivity.this,"com.example.pdftester"+".provider",file);
        Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(context.getApplicationContext()),
                BuildConfig.APPLICATION_ID + ".provider", file);
        Intent i=new Intent(Intent.ACTION_VIEW);

        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //String filetype = filename.substring(filename.length()-3);

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString()));
        Log.i(TAG, "openFile2: mimeType found is: "+mimeType);
        Uri myUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);

        //Log.i(TAG, "openFile2: filename: "+filetype);

        i.setDataAndType(uri, selectedStep.getMimeType());

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //Log.i(TAG, "openFile2: thomas - getting here, trying to start new activity");
        //todo add exception file, see last bookmarked URL

        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> packages = packageManager.queryIntentActivities(i,0);

        // following is only for documentation/logging purposes.  Logging package names.
        for(ResolveInfo res : packages){

            String package_name = res.activityInfo.packageName;
            Log.i("Package Name: ",package_name+", class name: "+ res.activityInfo.packageName.getClass());

        }

        // package name for hmt-1 document viewer: com.realwear.documentviewer

        //todo should open next activity
        //Log.i(TAG, "openFile2: ok, should open next activity here...");


        try {
            context.startActivity(i);
        }
        catch (android.content.ActivityNotFoundException e) {
            i.setPackage("com.realwear.documentviewer");
            context.startActivity(i);
        }

    }

    /**
     * Tried to use Realwear's 'custom' native zoom implementation ("HandsFree for Zoom)
     * more info here: https://support.realwear.com/knowledge/handsfreeforzoom
     *
     * But could not find a way to call it through an intent (and pass the meeting ID).
     *
     * Emailed Realwear's support but apparently this kind of feedback is considered 'developer feedback'
     * and is only provided as paid support !
     * @param selectedStep
     */
    private void testingRealwearZoom(RelarDataItem selectedStep) {

        Log.i(TAG, "testingRealwearZoom: running free ...");
        String correctLinkToGiveToZoom = "com.realwear.zoom/join?action=join&confno=";

        String meetingID = findMeetingID(selectedStep.getURL());

        correctLinkToGiveToZoom += meetingID;
        
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage("com.realwear.zoom");
        
        if (intent != null) {
            context.startActivity(intent);
        }

    }

    private void openZoom(RelarDataItem selectedStep) {
        //testingRealwearZoom(selectedStep);
        Log.i(TAG, "openZoom: trying to open zoom link");

        String correctLinkToGiveToZoom = "zoomus://zoom.us/join?action=join&confno=";

        String meetingID = findMeetingID(selectedStep.getURL());

        // todo from "https://us04web.zoom.us/j/77108513466?pwd=8v37uLBuX9TrnayAw13D4bIbHXKF6Z.1" keep only meeting number

        correctLinkToGiveToZoom += meetingID;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(correctLinkToGiveToZoom));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            Log.i(TAG, "openZoom: resolveActivity result: "+intent.resolveActivity(context.getPackageManager()).getPackageName());
            context.startActivity(intent);
        }
        else {
            //Toast.makeText(context.getApplicationContext(), "debug: resolveActivity returns null", Toast.LENGTH_LONG).show();
            // IMPORTANT new for android 12:
            try {
                context.startActivityForResult(intent, ZOOM_REQUEST);
            }
            catch (ActivityNotFoundException ex) {
                Log.i(TAG, "openZoom: "+ex.getMessage());
            }
        }


    }


    /**
     * The meeting ID can be a 10 or 11-digit number. The 11-digit number is used for instant,
     * scheduled or recurring meetings. The 10-digit number is used for Personal Meeting IDs.
     * from here: https://support.zoom.us/hc/en-us/articles/201362373-Meeting-and-Webinar-IDs
     *
     * So we keep first 10 or 11 digit numbers after '/j/' substring.
     *
     * Tested both with free and licensed zoom meetings...
     *
     * todo should probably include the possibility of /s/ instead of /j/ in meeting URL.
     * @param url
     * @return
     */
    private String findMeetingID(String url) {
        // // yourString.substring(yourString.indexOf("no") + 3 , yourString.length());
        int startIndex = url.indexOf("/j/");
        if (startIndex == -1) {
            startIndex = url.indexOf("/s/");
        }
        String meetingID = "";

        Log.i(TAG, "findMeetingID: startIndex = "+startIndex+", startIndex+1 char is: "+url.charAt(startIndex+1));
        if (Character.isDigit(url.charAt(startIndex+13))) {
            meetingID = url.substring(startIndex+3, startIndex+14);
        }
        else {
            meetingID = url.substring(startIndex+3, startIndex+13);
        }


        Log.i(TAG, "findMeetingID: startIndex="+startIndex+", meetingID="+meetingID);

        return meetingID;
        // old code, worked but only for licensed links (only digits were meeting IDs)
        /*
        Matcher m = Pattern.compile("[^0-9]*([0-9]+).*").matcher(url);
        if (m.matches()) {
            Log.i(TAG, "findMeetingId: "+m.group(1));
            return (m.group(1));
        }
        else {
            //Log.i(TAG, "findMeetingId: no meeting id found");
            return "";
        }
        */

    }

    private String getMimeFromFileURL(String url) {
        MimeTypeMap map = MimeTypeMap.getSingleton();
        String ext = MimeTypeMap.getFileExtensionFromUrl(url);
        return map.getMimeTypeFromExtension(ext);
    }


    //todo needs refactoring
    private void openOtherLink(String link) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        Log.i(TAG, "openOtherLink: trying with url: "+link);
        context.startActivity(intent);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            Log.i(TAG, "openOtherLink: resolveActivity result: "+intent.resolveActivity(context.getPackageManager()).getPackageName());
            context.startActivity(intent);
        }
        else {
            Toast.makeText(context, "null here", Toast.LENGTH_LONG).show();
        }
    }


}
