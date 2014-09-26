package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface LoadPictureEventHandler extends EventHandler {
    abstract public void onLoadPicture(LoadPictureEvent event);
}
