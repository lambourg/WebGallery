package com.lambourg.webgallery.client.rpc;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.ImageElement;

public abstract class ImageLoader {
    public static final int UNSENT = 0;
    public static final int OPENED = 1;
    public static final int HEADERS_RECEIVED = 2;
    public static final int LOADING = 3;
    public static final int DONE = 4;
    
    private JavaScriptObject xhr;
    private RequestsBuilder rb;
    
    private native JavaScriptObject internal(String url) /*-{
        var xhr = new XMLHttpRequest();
        xhr.loader = this;
        xhr.img = null;
        xhr.aborted = false;
        xhr.open('GET', url, true);
        xhr.responseType = 'blob';
        xhr.onprogress = function(e) {
            xhr.loader.@com.lambourg.webgallery.client.rpc.ImageLoader::onProgress(II)(e.loaded, e.total);
        }
        xhr.onabort = function(e) {
            xhr.aborted = true;
            xhr.loader.@com.lambourg.webgallery.client.rpc.ImageLoader::onError()();
        }
        xhr.onerror = function(e) {
            xhr.aborted = true;
            xhr.loader.@com.lambourg.webgallery.client.rpc.ImageLoader::onError()();
        }
        xhr.onload = function(e) {
            if (xhr.status == 200) {
                var blob = xhr.response;
                xhr.img = document.createElement('img');
                xhr.img.onload = function(e) {
                    $wnd.URL.revokeObjectURL(xhr.img.src);
                    xhr.loader.@com.lambourg.webgallery.client.rpc.ImageLoader::onLoad(Lcom/google/gwt/dom/client/ImageElement;)(xhr.img);
                    xhr.img = null;
                };
                xhr.img.src = $wnd.URL.createObjectURL(blob);
            }
        }
        
        xhr.send();
        
        return xhr;
    }-*/;
    
    private static native void abort(JavaScriptObject xhr) /*-{
        if (xhr.readyState == 4 && xhr.img) {
           $wnd.URL.revokeObjectURL(xhr.img.src);
           xhr.img = null;
           xhr.loader.@com.lambourg.webgallery.client.rpc.ImageLoader::onError()();
        }
        xhr.abort();
    }-*/;
    
    private static native int getReadyState(JavaScriptObject xhr) /*-{
        return xhr.readyState;
    }-*/;
    
    public void load(String fileId, String size, final String url) {
        this.rb = new RequestsBuilder() {
            @Override
            public void onResponse(String content) {
                ImageLoader.this.rb = null;
                ImageLoader.this.xhr = ImageLoader.this.internal(url);
            }
        };
        this.rb.ensurePicture(fileId, size);
    }
    
    public void cancel() {
        if (this.rb != null) {
            this.rb.cancel();
        }
        if (this.xhr != null) {
            abort(this.xhr);
        }
    }
    
    public int getReadyState() {
        if (this.xhr == null) return UNSENT;
        return getReadyState(this.xhr);
    }
    
    public boolean isLoading() {
        int state = this.getReadyState();
        
        return state == OPENED || state == HEADERS_RECEIVED || state == LOADING;
    }
    
    public abstract void onProgress(int loaded, int total);
    
    public abstract void onLoad(ImageElement img);
    
    public abstract void onError();
    
}
