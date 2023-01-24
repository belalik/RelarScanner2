package com.example.android.relarscanner2;

public class RelarDataItem {

    private int flag;
    private int serialNumber;

    //private String zoomTitle;
    //private String zoomURL;

    private String title;
    private String URL;

    private boolean useThis;

    private String mimeType;

    private int stepNumber;

    /**
     * new constructor. we only need these values now..
     * @param flag
     * @param title
     * @param URL
     */
    public RelarDataItem(int flag, String title, String URL) {
        this.flag = flag;
        this.title = title;
        this.URL = URL;
        this.useThis = false;
        this.mimeType = "undefined";
    }

    /**
     * old constructor that used useThis flag.  Not needed anymore. Left for reference..
     * @param flag
     * @param serialNumber
     * @param fileTitle
     * @param fileURL
     * @param useThis
     */
    public RelarDataItem(int flag, int serialNumber, String fileTitle, String fileURL, boolean useThis) {
        this.flag = flag;
        this.serialNumber = serialNumber;
        this.title = fileTitle;
        this.URL = fileURL;
        this.useThis = useThis;
        this.mimeType = "undefined";
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

//    public String getZoomTitle() {
//        return zoomTitle;
//    }
//
//    public void setZoomTitle(String zoomTitle) {
//        this.zoomTitle = zoomTitle;
//    }

//    public String getZoomURL() {
//        return zoomURL;
//    }
//
//    public void setZoomURL(String zoomURL) {
//        this.zoomURL = zoomURL;
//    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public boolean isUseThis() {
        return useThis;
    }

    public void setUseThis(boolean useThis) {
        this.useThis = useThis;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }
}
