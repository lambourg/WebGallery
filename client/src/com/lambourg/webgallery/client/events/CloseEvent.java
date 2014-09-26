package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.Widget;

public class CloseEvent extends GwtEvent<CloseEventHandler> {

    public static final Type<CloseEventHandler> TYPE =
            new Type<CloseEventHandler>();
    private Widget origin;

    public CloseEvent(Widget origin)
    {
        this.origin = origin;
    }

    @Override
    public Type<CloseEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(CloseEventHandler handler) {
        handler.onClose(this);
    }

    public Widget getOrigin() {
        return this.origin;
    }
}
