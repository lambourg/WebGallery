package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface PictureLoadedEventHandler extends EventHandler {
    
    public void onPictureLoaded(PictureLoadedEvent event);

}
