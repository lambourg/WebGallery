package com.lambourg.webgallery.client.folderview;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.xml.client.Document;
import com.lambourg.webgallery.client.events.ItemClickedEvent;
import com.lambourg.webgallery.client.events.ItemClickedEventHandler;
import com.lambourg.webgallery.client.events.LoadFolderEvent;
import com.lambourg.webgallery.client.events.LoadFolderEventHandler;

public class Nav
        extends FlowPanel
{
    private NavItem rootItem;
    private NavItem selected;
    private String selectedDir;
    private static final int LEFT = 10;
    private int width = Style.NAV_WIDTH + LEFT;

    public Nav() {
        super();
        this.setStyleName("wg-nav");
        this.selectedDir = null;
      
        this.setWidth(this.width + "px");
        this.setHeight("100%");

        this.addAttachHandler(new Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (!event.isAttached())
                    return;
            }
        });
    }
    
    public void unselect() {
        if (this.selected != null) {
            this.selected.setSelected(false);
            this.selected = null;
        }
    }
    
    public void bind(Document directories) {
        this.rootItem = new NavItem(directories.getDocumentElement(), 0);
        this.add(this.rootItem);
        this.rootItem.setWidth(this.width);
        if (this.selectedDir != null) {
            this.setSelected(this.selectedDir);
            this.selectedDir = null;
        }
        this.rootItem.addItemClickedEventHandler(
                new ItemClickedEventHandler() {
                    @Override
                    public void onItemClicked(ItemClickedEvent event) {
                        if (Nav.this.selected != null) {
                            Nav.this.selected.setSelected(false);
                        }
                        Nav.this.selected = event.getSource();
                        Nav.this.selected.setSelected(true);
                        Nav.this.fireEvent(new LoadFolderEvent(event.getSource()));
                    }
                });
        //  This updates the widths of the rootItem and its children
        this.setWidth(this.width);
    }
    
    public String getDirName(String dirId) {
        if (this.rootItem != null) {
            return this.rootItem.getDirName(dirId);
        } else {
            return null;
        }
    }
    
    public void setSelected(String dirId) {
        if (this.rootItem != null) {
            NavItem item = this.rootItem.expand(dirId);
            if (this.selected != null) {
                this.selected.setSelected(false);
            }
            this.selected = item;
            item.setSelected(true);
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    Nav.this.rootItem.refreshChildren();                    
                }
            });
        } else {
            this.selectedDir = dirId;
        }
    }

    public void setWidth(int width) {
        if (this.width == width) {
            return;
        }
        this.width = width;
        this.setWidth(width + "px");
        this.rootItem.setWidth(width - LEFT);
    }
    
    public int getWidth(int width) {
        return this.width;
    }
    
    public static int getDesiredWidth() {
        return Style.NAV_WIDTH + LEFT;
    }

    public HandlerRegistration addLoadFolderEventHandler(
            LoadFolderEventHandler handler) {
        return this.addHandler(handler, LoadFolderEvent.TYPE);
    }
}
