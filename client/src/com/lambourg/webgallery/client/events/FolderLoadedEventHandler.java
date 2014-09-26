package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface FolderLoadedEventHandler extends EventHandler {
    public void onFolderLoaded(FolderLoadedEvent event);
}
