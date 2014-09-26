package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.GwtEvent;
import com.lambourg.webgallery.shared.PictureDescriptor;

public class PictureLoadedEvent extends GwtEvent<PictureLoadedEventHandler> {

    public static final Type<PictureLoadedEventHandler> TYPE =
            new Type<PictureLoadedEventHandler>();
    private PictureDescriptor desc;

    public PictureLoadedEvent(PictureDescriptor desc)
    {
        this.desc = desc;
    }

    @Override
    public Type<PictureLoadedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(PictureLoadedEventHandler handler) {
        handler.onPictureLoaded(this);
    }

    public PictureDescriptor getDescriptor() {
        return this.desc;
    }
}
