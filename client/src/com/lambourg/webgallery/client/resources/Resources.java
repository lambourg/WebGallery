package com.lambourg.webgallery.client.resources;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundle {
    public static final Resources INST = GWT
            .create(Resources.class);
   
    @Source("fullscreen.png")
    public ImageResource iconFullscreen();

    @Source("unfullscreen.png")
    public ImageResource iconUnFullscreen();
    
    @Source("close.png")
    public ImageResource close();
    
    @Source("play.png")
    public ImageResource play();
    
    @Source("stop.png")
    public ImageResource stop();
    
    @Source("backward.png")
    public ImageResource backward();

    @Source("forward.png")
    public ImageResource forward();
    
    @Source("thumbs.png")
    public ImageResource thumbs();

    @Source("zoom.png")
    public ImageResource zoom();

    @Source("video-overlay.png")
    public ImageResource videoOverlay();
    
    @Source("callout_black.gif")
    public ImageResource popupCallout();
}

interface Styles extends CssResource {
    String withRepeatedBackground();
}
