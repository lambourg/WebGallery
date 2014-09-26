package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface CloseEventHandler extends EventHandler {
    abstract public void onClose(CloseEvent event);
}
