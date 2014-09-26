package com.lambourg.webgallery.client.folderview;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;
import com.lambourg.webgallery.client.events.FolderLoadedEvent;
import com.lambourg.webgallery.client.events.FolderLoadedEventHandler;
import com.lambourg.webgallery.client.events.LoadPictureEvent;
import com.lambourg.webgallery.client.events.LoadPictureEventHandler;
import com.lambourg.webgallery.client.resources.Resources;
import com.lambourg.webgallery.client.rpc.RequestsBuilder;
import com.lambourg.webgallery.shared.PictureDescriptor;

public class Thumbnails
    extends LayoutPanel
    implements HasHandlers    
{
    private static int MARGIN = 2;
    private static int BORDERS = 30;

    private class Thumb extends LayoutPanel {
        private PictureDescriptor desc;
        private Image img;
        private int imgW, imgH;
        private LayoutPanel parent;
        private int x, y, w, h;
        private Image videoOverlay;

        public Thumb(PictureDescriptor desc) {
            this.desc = desc;
            this.getElement().getStyle().setBackgroundColor("#fff");

            this.img = new Image();
            this.add(this.img);
            this.setWidgetLeftRight(this.img, 0, Unit.PX, 0, Unit.PX);
            this.setWidgetTopBottom(this.img, 0, Unit.PX, 0, Unit.PX);
            
            if (this.desc.isVideo()) {
                this.videoOverlay = new Image(Resources.INST.videoOverlay().getSafeUri());
                this.add(this.videoOverlay);
                this.videoOverlay.getElement().getStyle().setOpacity(0.6);
                this.videoOverlay.getElement().getStyle().setProperty("pointerEvents", "none");
                this.videoOverlay.getElement().getParentElement().getStyle().setProperty("pointerEvents", "none");
                this.videoOverlay.setWidth("100%");
                this.videoOverlay.setHeight("100%");
            }

            this.getElement().getStyle().setCursor(Cursor.POINTER);
            this.getElement().getStyle().setZIndex(1);
            
            this.img.addMouseOverHandler(new MouseOverHandler() {
                @Override
                public void onMouseOver(MouseOverEvent event) {
                    Thumb.this.onMouseOver(true);
                }
            });
            this.img.addMouseOutHandler(new MouseOutHandler() {
                @Override
                public void onMouseOut(MouseOutEvent event) {
                    Thumb.this.onMouseOver(false);
                }
            });
            this.img.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    Thumb.this.onClick();
                }
            });
            this.img.addLoadHandler(new LoadHandler() {
                @Override
                public void onLoad(LoadEvent event) {
                    Thumb.this.onResize();
                    Thumb.this.imgW = Thumb.this.img.getWidth();
                    Thumb.this.imgH = Thumb.this.img.getHeight();
                    Thumb.this.img.setWidth("100%");
                    Thumb.this.img.setHeight("100%");
                    
                    Thumbnails.this.thumbLoaded();
                }
            });
            this.img.addAttachHandler(new AttachEvent.Handler() {
                @Override
                public void onAttachOrDetach(AttachEvent event) {
                    if (!event.isAttached()) {
                        Thumb.this.onMouseOver(false);
                    }
                }
            });
            this.updateVisibleState();
        }
        
        public void updateVisibleState() {
            if (Thumbnails.this.visible) {
                this.img.setUrl(this.desc.getThumbnailUrl());
            }
        }

        public void onMouseOver(boolean state) {
            //  Layout panels enclose widgets inside a div, with
            //  top/bottom/left/width of the widget itself forced to 0. This
            //  hides anything that lies outside of the widget's square.
            //  So if we want to have shadows as set by the CSS, we need to
            //  apply the z-index and style name to the enclosing element, not
            //  directly to the Thumb widget
            Element elt = this.getElement().getParentElement();

            if (state) {
                parent.setWidgetLeftWidth(this, this.x - 20, Unit.PX,
                        this.w + 40, Unit.PX);
                parent.setWidgetTopHeight(this, this.y - 20, Unit.PX,
                        this.h + 40, Unit.PX);
                this.resizeVideoOverlay(this.w + 40, this.h + 40);
                elt.getStyle().setZIndex(Style.Z_INDEX_CENTRAL + 1);
                elt.setClassName("wg-tumbnailsview-over");
                //  allow space to display a thin white border around the img
                this.setWidgetLeftRight(this.img, 1, Unit.PX, 1, Unit.PX);
                this.setWidgetTopBottom(this.img, 1, Unit.PX, 1, Unit.PX);
            } else {
                parent.setWidgetLeftWidth(this, this.x, Unit.PX, this.w,
                        Unit.PX);
                parent.setWidgetTopHeight(this, this.y, Unit.PX, this.h,
                        Unit.PX);
                this.resizeVideoOverlay(this.w, this.h);
                elt.getStyle().setZIndex(Style.Z_INDEX_CENTRAL);
                elt.removeClassName("wg-tumbnailsview-over");
                this.setWidgetLeftRight(this.img, 0, Unit.PX, 0, Unit.PX);
                this.setWidgetTopBottom(this.img, 0, Unit.PX, 0, Unit.PX);
            }
        }

        public void onClick() {
            Thumbnails.this.onClick(this);
        }
        
        private void resizeVideoOverlay(int w, int h) {
            if (this.videoOverlay == null) {
                return;
            }
            int size = Math.min(w, h) / 2;
            this.setWidgetLeftWidth(
                    this.videoOverlay,
                    (w - size) / 2, Unit.PX,
                    size, Unit.PX);
            this.setWidgetTopHeight(
                    this.videoOverlay,
                    (h - size) / 2, Unit.PX,
                    size, Unit.PX);
        }

        public void setPosition(LayoutPanel parent, int x, int y, int w, int h)
        {
            parent.setWidgetLeftWidth(this, x, Unit.PX, w, Unit.PX);
            parent.setWidgetTopHeight(this, y, Unit.PX, h, Unit.PX);
            this.parent = parent;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.resizeVideoOverlay(w, h);
        }

        public int getWidth() {
            if (this.img == null) {
                return 16;
            } else {
                /*  
                 * We always use the @2 thumbnail here, for several reasons:
                 * 1- more pixels: as we're resizing the thumbs on the fly
                 *    to achieve fixed lines width, more is better.
                 * 2- the thumbs are loaded only once, so if we switch to/from
                 *    a retina display. then the icon gets resized wich is not
                 *    desirable
                 */
                    
                return (int) (this.imgW / 2);
            }
        }

        public int getHeight() {
            if (this.img == null) {
                return 16;
            } else {
                return (int) (this.imgH / 2);
            }
        }
    }

    private String dirId;
    private ScrollPanel scroll;
    private LayoutPanel layout;
    private int loaded;
    private int total;
    private Label title;
    private Label loading;
    private ArrayList<Thumb> thumbs;
    private List<PictureDescriptor> descriptors;
    private RequestsBuilder requests;
    private boolean visible;

    /**
     * Creates a new Thumbnails view from the enclosing layout and the id of
     * the directory to be displayed.
     *
     * @param dirId the id of the directory to display
     * @param dirName the name of the directory to display
     * @param visible whether the thumbnails are visible and thus should be
     *        loaded immediately
     */
    public Thumbnails(String dirId, String dirName, boolean visible) {
        this.setStyleName("wg-Thumbs");
        this.dirId = dirId;
        this.total = 0;
        this.visible = visible;
        this.scroll = new ScrollPanel();
        this.add(this.scroll);
        this.setWidgetTopBottom(this.scroll, 0, Unit.PX, 0, Unit.PX);
        this.setWidgetLeftRight(this.scroll, 0, Unit.PX, 0, Unit.PX);
        
        this.layout = new LayoutPanel();
        this.scroll.add(layout);

        this.title = new Label(dirName);
        this.title.setStyleName("wg-thumbnailsview-title");
        this.layout.add(this.title);
        this.layout.setWidgetLeftRight(this.title,  0,  Unit.PX,  0,  Unit.PX);
        this.layout.setWidgetTopHeight(this.title,  BORDERS,  Unit.PX,  4.0,  Unit.EM);
        
        this.loading = new Label("Loading...");
        this.loading.setStyleName("wg-thumbnailsview-loading");
        this.layout.add(this.loading);
        this.layout.setWidgetLeftRight(this.loading,  0,  Unit.PX,  0,  Unit.PX);
        this.layout.setWidgetTopHeight(this.loading,  200,  Unit.PX,  3.0,  Unit.EM);
        
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
            @Override
            public void execute() {
                Thumbnails.this.update();
            }
        });
        this.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (event.isAttached()) return;
                if (Thumbnails.this.requests == null) return;
                
                Thumbnails.this.requests.cancel();
            }
        });
        this.requests = new RequestsBuilder() {
            @Override
            public void onResponse(String content) {
                Thumbnails.this.bind(XMLParser.parse(content));
                Thumbnails.this.requests = null;
            }            
        };
        this.requests.getPictures(dirId);
    }
    
    public void setVisibleState(boolean visible) {
        if (this.visible == visible) return;
        
        this.visible = visible;
        
        if (visible) {
            for (Thumb thumb: this.thumbs) {
                thumb.updateVisibleState();
            }
        }
    }

    /**
     * Binds the view to the data contained in doc
     * @param doc the XML document containing the thumbnails description data
     */
    private void bind(Document doc) {
        this.descriptors = PictureDescriptor.bind(doc);
        Thumbnails.this.fireEvent(new FolderLoadedEvent(this.descriptors));
        String dirname = this.descriptors.get(0).getDirName();

        this.title.setText(dirname);
        this.thumbs = new ArrayList<Thumb>();

        for (PictureDescriptor desc : this.descriptors) {
            Thumb thumb = new Thumb(desc);
            this.thumbs.add(thumb);
            this.layout.add(thumb);
            this.total += 1;
        }
        this.update();
    }

    private void thumbLoaded() {
        this.loaded = this.loaded + 1;
        double progress = (double)this.loaded / (double)this.total;
        
        this.loading.setText("Loading... (" + Math.round(progress * 100) + "%)");

        if (this.loaded == this.total) {
            this.layout.remove(this.loading);
            this.update();
        }
    }

    /**
     * Layouts the line of thumbnails to fill the view at the specified 
     * position and width.
     * @param line the line to position
     * @param y the y coordinate of the line
     * @param width the expected/maximal width of the layout
     * @param thumbWidth the cumulated widths of the line's thumbs
     * @param fill whether we should completely fill the width or not
     * @return the height of the line
     */
    private int drawLine(ArrayList<Thumb> line, int y, int width,
            int thumbWidth, boolean fill) {
        int x = 0;
        int currentWidth = thumbWidth;
        double ratio;
        
        if (!fill && (thumbWidth + ((line.size() - 1) * MARGIN) < width)) {
            //  The line does not fill the width
            ratio = 1.0;
        } else {
            ratio = (double)(width - ((line.size() - 1) * MARGIN)) / currentWidth;
        }

        for (int j = 0; j < line.size(); j++) {
            Thumb item = line.get(j);
            double itemW;
            double itemH = (double) item.getHeight() * ratio;
            int remainingMargins = (line.size() - 1 - j) * MARGIN;
            double ratioW;
            
            if (ratio == 1.0) {
                ratioW = 1.0;
            } else {
                //  We need to slightly adjust the width of each thumb to
                //  take into account the roundings that are performed when
                //  using a ratio /= 1
                ratioW = (double)(width - x - remainingMargins)
                    / currentWidth;
            }

            itemW = item.getWidth() * ratioW;
            item.setPosition(this.layout, (int) Math.round(x + BORDERS),
                    (int) y, (int) itemW, (int) itemH);
            x += Math.round(itemW) + MARGIN;
            currentWidth -= item.getWidth();
        }

        if (line.isEmpty()) {
            return 0;
        } else {
            return (int) (Math.round(line.get(0).getHeight() * ratio));
        }
    }

    /**
     * Updates the layout of the view.
     */
    private void update() {
        if (this.thumbs == null)
            return;

        int totalWidth = this.getOffsetWidth() - 2 * BORDERS;
        int currentWidth = 0;
        int y = 0;
        ArrayList<Thumb> line = new ArrayList<Thumb>();

        y = BORDERS + this.title.getOffsetHeight() + 20;
        
        currentWidth = 0;
        for (Thumb thumb : this.thumbs) {
            int w = thumb.getWidth();

            if (currentWidth + line.size() * MARGIN + w / 2 > totalWidth) {
                int height = this.drawLine(line, y, totalWidth, currentWidth,
                        true);
                y += height + MARGIN;
                currentWidth = 0;
                line.clear();
            }
            currentWidth += w;
            line.add(thumb);
        }

        y += this.drawLine(line, y, totalWidth, currentWidth, false);
        y += BORDERS;
        line.clear();
        
        if (y < this.getOffsetHeight()) {
            y = this.getOffsetHeight();
        }
        this.layout.setWidth(this.getOffsetWidth() + "px");
        this.layout.setHeight(y + "px");
    }
    
    public String getDirId() {
        return this.dirId;
    }
    
    private void onClick(Thumb thumb) {
        this.fireEvent(new LoadPictureEvent(thumb.desc));
    }

    @Override
    public void onResize() {
        super.onResize();
        this.update();
    }

    public HandlerRegistration addLoadPictureEventHandler(
            LoadPictureEventHandler handler) {
        return this.addHandler(handler, LoadPictureEvent.TYPE);
    }

    public HandlerRegistration addFolderLoadedEventHandler(
            FolderLoadedEventHandler handler) {
        return this.addHandler(handler, FolderLoadedEvent.TYPE);
    }
}
