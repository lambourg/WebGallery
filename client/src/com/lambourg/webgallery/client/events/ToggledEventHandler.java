package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface ToggledEventHandler extends EventHandler {
    abstract public void onToggled(ToggledEvent event);
}
