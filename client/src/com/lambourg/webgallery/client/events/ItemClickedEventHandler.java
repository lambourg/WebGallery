package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface ItemClickedEventHandler extends EventHandler {
    
    public void onItemClicked(ItemClickedEvent event);

}
