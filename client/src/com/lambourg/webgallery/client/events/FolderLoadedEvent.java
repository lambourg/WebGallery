package com.lambourg.webgallery.client.events;

import java.util.List;

import com.google.gwt.event.shared.GwtEvent;
import com.lambourg.webgallery.shared.PictureDescriptor;

public class FolderLoadedEvent extends GwtEvent<FolderLoadedEventHandler> {

    public static final Type<FolderLoadedEventHandler> TYPE =
            new Type<FolderLoadedEventHandler>();
    private List<PictureDescriptor> descriptors;

    public FolderLoadedEvent(List<PictureDescriptor> descriptors)
    {
        this.descriptors = descriptors;
    }

    @Override
    public Type<FolderLoadedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FolderLoadedEventHandler handler) {
        handler.onFolderLoaded(this);
    }

    public List<PictureDescriptor> getDescriptors() {
        return this.descriptors;
    }    
}
