package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.Widget;

public class ToggledEvent extends GwtEvent<ToggledEventHandler> {

    public static final Type<ToggledEventHandler> TYPE =
            new Type<ToggledEventHandler>();
    private Widget origin;
    private boolean state;

    public ToggledEvent(Widget origin, boolean state)
    {
        this.origin = origin;
        this.state = state;
    }

    @Override
    public Type<ToggledEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ToggledEventHandler handler) {
        handler.onToggled(this);
    }

    public Widget getOrigin() {
        return this.origin;
    }
    
    public boolean getState() {
        return this.state;
    }
}
