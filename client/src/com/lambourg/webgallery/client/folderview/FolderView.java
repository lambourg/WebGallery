package com.lambourg.webgallery.client.folderview;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;
import com.lambourg.webgallery.client.events.FolderLoadedEvent;
import com.lambourg.webgallery.client.events.FolderLoadedEventHandler;
import com.lambourg.webgallery.client.events.LoadFolderEvent;
import com.lambourg.webgallery.client.events.LoadFolderEventHandler;
import com.lambourg.webgallery.client.events.LoadPictureEvent;
import com.lambourg.webgallery.client.events.LoadPictureEventHandler;
import com.lambourg.webgallery.client.events.ToggledEvent;
import com.lambourg.webgallery.client.events.ToggledEventHandler;
import com.lambourg.webgallery.client.resources.Resources;
import com.lambourg.webgallery.client.rpc.RequestsBuilder;
import com.lambourg.webgallery.client.widgets.TitleBar;
import com.lambourg.webgallery.client.widgets.TitleBarIcon;
import com.lambourg.webgallery.client.widgets.TitleBarLink;

public class FolderView
    extends LayoutPanel
    implements RequiresResize, ProvidesResize
{    
    private TitleBar title;
    private ScrollPanel scroll;
    private FlowPanel line;
    private Nav nav;
    private Boolean duringStartup;
    private Widget child;
    private String dirId;
    private Boolean visible;
    private Thumbnails thumbs;
    private TitleBarLink downloadLink;

    public FolderView() {
        super();
        this.dirId = null;
        this.visible = true;

        this.scroll = new ScrollPanel();
        this.add(this.scroll);
        this.setWidgetTopBottom(this.scroll, 0, 0);
        this.scroll.getElement().getParentElement().setClassName("wg-folderview-navbg");
        
        this.line = new FlowPanel();
        this.add(this.line);
        this.setWidgetTopBottom(this.line, 0, 0);
        this.line.setStyleName("wg-folderview-navborder");

        this.nav = new Nav();
        this.nav.getElement().getStyle().setZIndex(Style.Z_INDEX_NAV + 1);
        this.scroll.add(this.nav);

        this.title = new TitleBar();
        this.add(this.title);
        this.title.getElement().getStyle().setZIndex(Style.Z_INDEX_TITLEBAR);
        this.setWidgetTopHeight(this.title, 0, TitleBar.getHeight());
        this.setWidgetLeftRight(this.title, Nav.getDesiredWidth() + 5, 0);
        
        if (Style.fullscreenEnabled()) {
            TitleBarIcon icon = new TitleBarIcon(
                    Resources.INST.iconFullscreen(),
                    Resources.INST.iconUnFullscreen());
            this.title.add(icon);
            icon.addToggledHandler(new ToggledEventHandler() {
                @Override
                public void onToggled(ToggledEvent event) {
                    if (event.getState()) {
                        Style.requestFullscreen();
                    } else {
                        Style.exitFullscreen();
                    }
                }
            });
        }
        
        this.child = new Welcome();
        this.add(this.child);
        this.child.getElement().getStyle().setZIndex(Style.Z_INDEX_CENTRAL);

        this.duringStartup = true;

        this.nav.addLoadFolderEventHandler(new LoadFolderEventHandler() {
            @Override
            public void onLoadFolder(LoadFolderEvent event) {
                FolderView.this.fireEvent(event);
            }
        });

        // We need to delay the update of the layout to make sure that the
        // underlying
        // Elements are properly attached to the browser's window.
        //
        // For this, we wait for the attach event, and then schedule a deferred
        // command
        // so that this attach event finishes.
        this.addAttachHandler(new Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (!event.isAttached())
                    return;

                Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                    @Override
                    public void execute() {
                        FolderView.this.update(0.0);
                        FolderView.this.forceLayout();
                    }
                });
            }
        });

        // Now that everything is in place, let's retrieve the directory list
        // and bind it to the navigator. Upon reception, we also animate the
        // layout to make title bar and navigator appearing
        this.update(0.0);

        RequestsBuilder request = new RequestsBuilder() {
            @Override
            public void onResponse(String content) {
                Document root = XMLParser.parse(content);
                FolderView.this.setDirectories(root);
            }            
        };
        request.getDirectories();
    }
    
    public void setDirectories(Document root) {
        this.nav.bind(root);
        this.update(1.0);

        Animation anim = new Animation() {
            @Override
            protected void onUpdate(double progress) {
                FolderView.this.update(progress);
                FolderView.this.onResize();
            }

            @Override
            protected void onComplete() {
                super.onComplete();
                FolderView.this.duringStartup = false;
            }
        };
        anim.run(1000);        
    }
    
    @Override
    public void onResize() {
        super.onResize();
        
        if (!this.duringStartup)
            this.update(1.0);
    }

    private void update(double animValue) {
        int x;
        int navWidth = Nav.getDesiredWidth() + 5;

        x = (int)(Math.round((double)(navWidth + 5) * (animValue - 1.0)));

        this.setWidgetLeftWidth(this.scroll, x, navWidth);
        this.setWidgetLeftWidth(this.line, x + navWidth - 2, 1);
        this.setWidgetLeftRight(this.title, x + navWidth, 0);

        if (this.child != null) {
            this.child.getElement().getStyle().setOpacity(animValue);
            this.setWidgetTopBottom(this.child, TitleBar.getHeight(), 0);
            this.setWidgetLeftRight(this.child, navWidth, 0);
        }
    }

    private void setWidgetTopHeight(Widget child, int top, int height) {
        super.setWidgetTopHeight(child, top, Unit.PX, height, Unit.PX);
    }

    private void setWidgetTopBottom(Widget child, int top, int bottom) {
        super.setWidgetTopBottom(child, top, Unit.PX, bottom, Unit.PX);
    }

    private void setWidgetLeftWidth(Widget child, int left, int width) {
        super.setWidgetLeftWidth(child, left, Unit.PX, width, Unit.PX);
    }

    private void setWidgetLeftRight(Widget child, int left, int right) {
        super.setWidgetLeftRight(child, left, Unit.PX, right, Unit.PX);
    }
    
    public void loadFolder(String dirId, String dirName, boolean expandNav) {
        if (dirId == null) {
            if (this.dirId == null) {
                return;
            }
            if (this.downloadLink != null) {
                this.title.remove(this.downloadLink);
                this.downloadLink = null;
            }
            this.remove(this.child);
            this.dirId = null;
            this.thumbs = null;
            this.child = new Welcome();
            this.add(this.child);
            this.onResize();
            this.nav.unselect();

            return;
        }
        if (dirId.equals(this.dirId)) {
            return;
        }
        
        if (this.downloadLink != null) {
            this.title.remove(this.downloadLink);
        }
        this.downloadLink = new TitleBarLink("download all");
        this.downloadLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                RequestsBuilder.downloadDir(FolderView.this.dirId);
            }
        });
        this.title.add(this.downloadLink);
        this.remove(this.child);
        this.thumbs = new Thumbnails(dirId, dirName, this.visible);
        this.dirId = dirId;
        this.child = thumbs;
        this.add(this.child);
        this.thumbs.getElement().getStyle().setZIndex(Style.Z_INDEX_CENTRAL);
        this.thumbs.addLoadPictureEventHandler(new LoadPictureEventHandler() {
            @Override
            public void onLoadPicture(LoadPictureEvent event) {
                FolderView.this.fireEvent(event);
            }
        });
        this.thumbs.addFolderLoadedEventHandler(new FolderLoadedEventHandler() {
            @Override
            public void onFolderLoaded(FolderLoadedEvent event) {
                FolderView.this.fireEvent(event);
            }
        });
        if (expandNav) {
            this.thumbs.addFolderLoadedEventHandler(new FolderLoadedEventHandler() {
                @Override
                public void onFolderLoaded(FolderLoadedEvent event) {
                    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                        @Override
                        public void execute() {
                            FolderView.this.nav.setSelected(FolderView.this.dirId);
                        }
                    });
                }
            });
        }
        
        this.onResize();
    }
    
    public void setVisibleState(boolean visible) {
        if (visible == this.visible) {
            return;
        }
        
        this.visible = visible;
        if (this.thumbs != null) {
            this.thumbs.setVisibleState(visible);            
        }
    }
    
    public HandlerRegistration addLoadPictureEventHandler(
            LoadPictureEventHandler handler) {
        return this.addHandler(handler, LoadPictureEvent.TYPE);
    }
    
    public HandlerRegistration addLoadFolderEventHandler(
            LoadFolderEventHandler handler) {
        return this.addHandler(handler, LoadFolderEvent.TYPE);
    }
    
    public HandlerRegistration addFolderLoadedEventHandler(
            FolderLoadedEventHandler handler) {
        return this.addHandler(handler, FolderLoadedEvent.TYPE);
    }
    
}
