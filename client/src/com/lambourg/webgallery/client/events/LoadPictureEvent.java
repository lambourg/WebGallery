package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.GwtEvent;
import com.lambourg.webgallery.shared.PictureDescriptor;

public class LoadPictureEvent extends GwtEvent<LoadPictureEventHandler> {

    public static final Type<LoadPictureEventHandler> TYPE =
            new Type<LoadPictureEventHandler>();
    private PictureDescriptor desc;

    public LoadPictureEvent(PictureDescriptor desc)
    {
        this.desc = desc;
    }

    @Override
    public Type<LoadPictureEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(LoadPictureEventHandler handler) {
        handler.onLoadPicture(this);
    }

    public PictureDescriptor getDescriptor() {
        return this.desc;
    }
}
