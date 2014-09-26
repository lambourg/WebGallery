package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface LoadFolderEventHandler extends EventHandler {
    abstract public void onLoadFolder(LoadFolderEvent event);
}
