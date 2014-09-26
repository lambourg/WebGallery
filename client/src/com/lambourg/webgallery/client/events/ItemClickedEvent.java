package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.GwtEvent;
import com.lambourg.webgallery.client.folderview.NavItem;

public class ItemClickedEvent extends GwtEvent<ItemClickedEventHandler> {

    public static final Type<ItemClickedEventHandler> TYPE =
            new Type<ItemClickedEventHandler>();
    private NavItem source;

    public ItemClickedEvent(NavItem source)
    {
        this.source = source;
    }

    @Override
    public Type<ItemClickedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ItemClickedEventHandler handler) {
        handler.onItemClicked(this);
    }

    public NavItem getSource() {
        return this.source;
    }
}
