package com.lambourg.webgallery.client.events;

import com.google.gwt.event.shared.GwtEvent;
import com.lambourg.webgallery.client.folderview.NavItem;

public class LoadFolderEvent extends GwtEvent<LoadFolderEventHandler> {

    public static final Type<LoadFolderEventHandler> TYPE =
            new Type<LoadFolderEventHandler>();
    private String dirId;
    private String dirName;

    public LoadFolderEvent(NavItem source)
    {
        this.dirId = source.getDirId();
        this.dirName = source.getDirName();
    }

    public LoadFolderEvent(String dirId, String dirName)
    {
        this.dirId = dirId;
        this.dirName = dirName;
    }

    @Override
    public Type<LoadFolderEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(LoadFolderEventHandler handler) {
        handler.onLoadFolder(this);
    }

    public String getDirId() {
        return this.dirId;
    }
    
    public String getDirName() {
        return this.dirName;
    }
}
