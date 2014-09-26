package com.lambourg.webgallery.client;

import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.lambourg.webgallery.client.events.CloseEvent;
import com.lambourg.webgallery.client.events.CloseEventHandler;
import com.lambourg.webgallery.client.events.FolderLoadedEvent;
import com.lambourg.webgallery.client.events.FolderLoadedEventHandler;
import com.lambourg.webgallery.client.events.LoadFolderEvent;
import com.lambourg.webgallery.client.events.LoadFolderEventHandler;
import com.lambourg.webgallery.client.events.LoadPictureEvent;
import com.lambourg.webgallery.client.events.LoadPictureEventHandler;
import com.lambourg.webgallery.client.events.PictureLoadedEvent;
import com.lambourg.webgallery.client.events.PictureLoadedEventHandler;
import com.lambourg.webgallery.client.folderview.FolderView;
import com.lambourg.webgallery.client.folderview.Style;
import com.lambourg.webgallery.client.pictureview.PictureView;
import com.lambourg.webgallery.shared.PictureDescriptor;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebGallery
        implements EntryPoint, LoadPictureEventHandler, LoadFolderEventHandler,
        FolderLoadedEventHandler, PictureLoadedEventHandler
{
    LayoutPanel main;
    FolderView folderView;
    PictureView pictureView;
    Widget current;
    List<PictureDescriptor> descriptors;
    String dirId;
    String pictureId;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        this.main = new LayoutPanel();
        RootLayoutPanel.get().add(this.main);

        this.folderView = new FolderView();
        this.folderView.addLoadFolderEventHandler(this);
        this.folderView.addLoadPictureEventHandler(this);
        this.folderView.addFolderLoadedEventHandler(this);
        this.main.add(this.folderView);
        this.current = this.folderView;
        this.pictureView = new PictureView();
        this.pictureView.addPictureLoadedEventHandler(this);
        this.pictureView.addCloseEventHandler(new CloseEventHandler() {
            @Override
            public void onClose(CloseEvent event) {
                WebGallery.this.ensureFolderView();
            }
        });
        this.pictureId = null;
        this.pictureView.getElement().getStyle()
                .setZIndex(Style.Z_INDEX_FULLSCREEN);

        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                WebGallery.this.onHistoryToken(event.getValue());
            }
        });

        this.onHistoryToken(History.getToken());
    }

    public void onHistoryToken(String token) {
        if ("".equals(token)) {
            this.loadFolder(null, null, false);
        } else {
            String[] parts = token.split("/");
            if (parts.length == 2) {
                this.loadFolder(parts[1], null, true);
            } else if (parts.length == 3) {
                this.loadPicture(parts[1], parts[2]);
            }
        }
    }

    private void ensureFolderView() {
        if (this.current != this.folderView) {
            this.main.remove(this.pictureView);
            this.current = this.folderView;
            this.pictureView.reset();
            this.pictureId = null;
            this.folderView.setVisibleState(true);
            if (this.dirId != null) {
                History.newItem("/" + this.dirId, false);
            }
        }
    }

    private void ensurePictureView() {
        if (this.current != this.pictureView) {
            this.main.add(this.pictureView);
            this.current = this.pictureView;
            this.folderView.setVisibleState(false);
        }
    }

    /**
     * Loads the picture from an history token
     * 
     * @param dirId
     * @param pictureId
     */
    private void loadPicture(String dirId, String pictureId) {
        this.ensurePictureView();
        if (dirId != this.dirId) {
            this.pictureId = pictureId;
            this.folderView.loadFolder(dirId, null, true);
        } else {
            this.pictureId = null;
            for (PictureDescriptor desc : this.descriptors) {
                if (desc.getPictureId().equals(pictureId)) {
                    this.loadPicture(desc);
                    return;
                }
            }
            Window.alert("Cannot find the picture " + pictureId);
            this.ensureFolderView();
            this.pictureId = null;
        }
    }

    private void loadPicture(PictureDescriptor desc) {
        this.ensurePictureView();
        this.pictureView.load(desc);
    }

    private void loadFolder(String dirId, String dirName, boolean expandNav) {
        if (this.current != this.folderView) {
            this.main.remove(this.pictureView);
            this.current = this.folderView;
        }
        this.folderView.loadFolder(dirId, dirName, expandNav);
    }

    @Override
    public void onLoadPicture(LoadPictureEvent event) {
        this.loadPicture(event.getDescriptor());
    }

    /**
     * Load folder request
     */
    @Override
    public void onLoadFolder(LoadFolderEvent event) {
        this.loadFolder(event.getDirId(), event.getDirName(), false);
    }

    /**
     * Called when a folder has been loaded by the folder view
     */
    @Override
    public void onFolderLoaded(FolderLoadedEvent event) {
        this.descriptors = event.getDescriptors();
        this.pictureView.setDescriptorsList(this.descriptors);
        this.dirId = this.descriptors.get(0).getDirId();
        
        if (this.pictureId != null) {
            this.loadPicture(this.dirId, this.pictureId);
        } else {
            History.newItem("/" + this.dirId, false);
        }
    }

    /**
     * Called when a picture has been loaded by the picture view
     */
    @Override
    public void onPictureLoaded(PictureLoadedEvent event) {
        PictureDescriptor desc = event.getDescriptor();
        History.newItem(
                "/" + desc.getDirId() + "/" + desc.getPictureId(), false);
    }
}
