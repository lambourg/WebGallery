package com.lambourg.webgallery.client.rpc;

import java.util.Date;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.lambourg.webgallery.shared.PictureDescriptor;

public abstract class RequestsBuilder {
    private Request request;
    
    private static String getTimeZone() {
        Date today = new Date();
        String timezone = DateTimeFormat.getFormat("v").format(today); //like "GMT-07:00"
        return timezone;
    }
    
    public abstract void onResponse(String content);
    
    public void cancel() {
        if (this.request != null)
            this.request.cancel();
    }

    public void doGetRequest(String url) {
        this.doRequest(url, RequestBuilder.GET, null);
    }
    
    public void doPostRequest(String url, String data) {
        this.doRequest(url, RequestBuilder.POST, data);        
    }
    
    private void doRequest(String url, RequestBuilder.Method method, String data) {
        RequestBuilder builder = new RequestBuilder(method, url);
        try {
            this.request = builder.sendRequest(data, new RequestCallback() {
                @Override
                public void onError(Request request, Throwable e) {
                    Window.alert(e.getMessage());
                }
                
                @Override
                public void onResponseReceived(Request request,
                        Response response) {
                    if (200 == response.getStatusCode()) {
                        RequestsBuilder.this.request = null;
                        RequestsBuilder.this.onResponse(response.getText());
                    } else {
                        Window.alert("Received HTTP status code other than 200 : "
                                + response.getStatusText());
                    }
                }
            });
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }
    }
    
    public void getDirectories() {
        String postUrl = "/folders/";
        String requestData = "?tz=" + URL.encodeQueryString(RequestsBuilder.getTimeZone());
        this.doGetRequest(postUrl + requestData);
    }

    public void getPictures(String dirId) {
        String postUrl = "/folders/" + dirId + "/";
        this.doGetRequest(postUrl);
    }
    
    public static void downloadDir(String dirId) {
        String postUrl = "/folders/" + dirId + "/download/";
        Window.open(postUrl, "_self", "");
    }

    public void getPictureInfo(PictureDescriptor desc) {
        String postUrl = "/files/" + desc.getPictureId() + "/infos/";
        String requestData;
        if (desc.isPicture()) {
            requestData = "?media=picture";
        } else {
            requestData = "?media=video";
        }
        this.doGetRequest(postUrl + requestData);
    }
    
    public void setPictureLike(PictureDescriptor desc, boolean likeState) {
        String postUrl = "/files/" + desc.getPictureId() + "/like/";
        String requestData = "?like=" + likeState;
        if (desc.isPicture()) {
            requestData += "&media=picture";
        } else {
            requestData += "&media=video";
        }
        this.doGetRequest(postUrl + requestData);
    }
    
    public void postPictureComment(PictureDescriptor desc, String content) {
        String postUrl = "/files/" + desc.getPictureId() + "/comment/";
        String requestData;
        if (desc.isPicture()) {
            requestData = "?media=picture";
        } else {
            requestData = "?media=video";
        }
        this.doPostRequest(postUrl + requestData, content);
    }
    
    public void ensurePicture(String fileId, String size) {
        String postUrl = "/files/" + fileId + "/ensure/";
        String requestData = "?size_name=" + size;
        this.doGetRequest(postUrl + requestData);
    }
    
    public static void downloadPicture(PictureDescriptor desc) {
        String postUrl = "/files/" + desc.getPictureId() + "/download/";
        String requestData;
        if (desc.isPicture()) {
            requestData = "?media=picture";
        } else {
            requestData = "?media=video";
        }
        Window.open(postUrl + requestData, "_self", "");
    }

}