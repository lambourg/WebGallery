package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class DownloadEvent extends GwtEvent<DownloadEventHandler> {

    public static final Type<DownloadEventHandler> TYPE =
            new Type<DownloadEventHandler>();

    public DownloadEvent() {}

    @Override
    public Type<DownloadEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(DownloadEventHandler handler) {
        handler.onDownload(this);
    }
}