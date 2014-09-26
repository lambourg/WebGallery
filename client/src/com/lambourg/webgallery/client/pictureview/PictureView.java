package com.lambourg.webgallery.client.pictureview;

import java.util.List;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.media.client.Video;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.lambourg.webgallery.client.events.CloseEvent;
import com.lambourg.webgallery.client.events.CloseEventHandler;
import com.lambourg.webgallery.client.events.PictureLoadedEvent;
import com.lambourg.webgallery.client.events.PictureLoadedEventHandler;
import com.lambourg.webgallery.client.events.ToggledEvent;
import com.lambourg.webgallery.client.events.ToggledEventHandler;
import com.lambourg.webgallery.client.folderview.Style;
import com.lambourg.webgallery.client.resources.Resources;
import com.lambourg.webgallery.client.rpc.ImageLoader;
import com.lambourg.webgallery.client.rpc.RequestsBuilder;
import com.lambourg.webgallery.client.widgets.TitleBar;
import com.lambourg.webgallery.client.widgets.TitleBarIcon;
import com.lambourg.webgallery.client.widgets.TitleBarLink;
import com.lambourg.webgallery.shared.PictureDescriptor;
import com.lambourg.webgallery.shared.PictureDescriptor.PictureSizeDescriptor;

public class PictureView
        extends LayoutPanel
        implements RequiresResize, ProvidesResize
{
    /**
     * 
     */
    class InternalLoader extends ImageLoader {
        private static final int CREATED = 0;
        private static final int LOADING = 1;
        private static final int ERROR = 2;
        private static final int LOADED = 3;
                        
        private PictureDescriptor desc;
        private int index;
        private boolean showOnLoad;
        private ImageElement elt;
        private int state;

        public InternalLoader() {
            super();
            this.initState();
        }

        private void initState() {
            this.desc = null;
            this.index = -1;
            this.showOnLoad = false;
            this.state = CREATED;
            this.elt = null;            
        }
        
        public void load(final int imgIdx, final boolean showOnLoad) {
            if (imgIdx == this.index) {
                return;
            }
            if (this.state == LOADING) {
                this.cancel();

                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        InternalLoader.this.load(imgIdx, showOnLoad);
                    }
                });
                
                return;
            }

            this.initState();
            this.state = LOADING;
                        
            this.index = imgIdx;
            this.desc = PictureView.this.descriptors.get(imgIdx);

            this.setShowOnLoad(showOnLoad);
            PictureSizeDescriptor size = this.desc.getSizeDescriptor(
                    Style.toPixelRatio(PictureView.this.getOffsetWidth()),
                    Style.toPixelRatio(PictureView.this.getOffsetHeight()));

            super.load(this.desc.getPictureId(), size.getName(), size.getUrl());
        }

        public void setShowOnLoad(Boolean show) {
            this.showOnLoad = show;

            if (show) {
                if (PictureView.this.loadingLabel == null) {
                    PictureView.this.createLoading(this.desc.getName());
                }
            }
        }
        
        public int getIndex() {
            return this.index;
        }
        
        public boolean isLoaded() {
            return this.state == LOADED;
        }

        public boolean isCanceled() {
            return this.state == ERROR;
        }
        
        public ImageElement getImageElement() {
            return this.elt;
        }
        
        @Override
        public void onProgress(int loaded, int total) {
            if (!this.showOnLoad) {
                return;
            }
            
            int progress = (int) Math.round((double) loaded
                    / (double) total * 100);
            PictureView.this.loadingLabel.setText(
                    "Loading " + this.desc.getName() + " (" + progress + "%)");
        }

        @Override
        public void onError() {
            this.index = -1;
            this.desc = null;
            this.state = ERROR;
        }

        @Override
        public void onLoad(ImageElement elt) {
            this.state = LOADED;
            this.elt = elt;

            if (this.showOnLoad) {
                PictureView.this.onLoaded(elt);
            }
        }
        
        @Override
        public void cancel() {
            super.cancel();
            this.setShowOnLoad(false);
        }
    }

    private static int MARGIN_SIDE = 250;
    private static int DIAPORAMA_TIMER = 5000;
    private static int HIDE_CONTROLS_TIMER = 4000;

    private HandlerRegistration keydownhandler;
    private InternalLoader loader;
    private boolean attached;
    private boolean isFullscreen;
    private List<PictureDescriptor> descriptors;
    private int current;
    private int top;
    private int right;
    private int controlsHeight;

    private Label loadingLabel;
    private ImageElement img;
    private ImageElement old;
    private Animation fadeAnim;
    private Canvas canvas;
    private Canvas cachePrev, cacheNext;
    private Video video;

    private TitleBar title;
    private TitleBarLink downloadLabel;
    private TitleBarIcon imgFullscreen;
    private TitleBarIcon imgClose;

    private ControlsPanel controls;
    private ControlButton imgPlay;
    private ControlButton imgPrev;
    private ControlButton imgNext;
    private ControlButton imgThumbs;
    private ControlButton imgZoom;

    private SidePanel sidePanel;
    private RequestsBuilder infoRequest;
    
    private Timer diapoTimer;
    private Timer hideControlsTimer;

    /**
     * Creates a new picture view to display pictures described in a list of
     * descriptors.
     * 
     * @param descriptors
     */
    public PictureView() {
        super();
        this.getElement().getStyle().setBackgroundColor("#000");
        this.attached = false;
        this.descriptors = null;
        this.current = -1;
        this.isFullscreen = false;
        this.top = TitleBar.getHeight();
        this.right = MARGIN_SIDE;
        
        this.loader = new InternalLoader();

        /**********
         * CANVAS *
         **********/

        this.canvas = Canvas.createIfSupported();
        this.add(this.canvas);
        this.canvas.getElement().getStyle().setBackgroundColor("#000");
        this.setWidgetLeftRight(this.canvas, 0, Unit.PX, MARGIN_SIDE, Unit.PX);
        this.setWidgetTopBottom(this.canvas, TitleBar.getHeight(), Unit.PX,
                0, Unit.PX);
        this.canvas.addMouseMoveHandler(new MouseMoveHandler() {
            @Override
            public void onMouseMove(MouseMoveEvent event) {
                PictureView.this.onMouseMove();
            }
        });
        this.canvas.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (PictureView.this.diapoTimer != null) {
                    PictureView.this.imgPlay.clicked();
                } else {
                    PictureView.this.imgNext.clicked();
                }
            }
        });

        /*************
         * TITLE BAR *
         *************/

        this.title = new TitleBar();
        this.add(this.title);
        this.setWidgetLeftRight(this.title, 0, Unit.PX, 0, Unit.PX);
        this.setWidgetTopHeight(this.title, 0, Unit.PX, 
                TitleBar.getHeight(), Unit.PX);

        this.downloadLabel = new TitleBarLink("download");
        this.title.add(this.downloadLabel);
        this.downloadLabel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                PictureView.this.onDownloadClicked();
            }
        });

        this.imgClose = new TitleBarIcon(Resources.INST.close());        
        this.imgClose.addClickHandler(new ClickHandler() {            
            @Override
            public void onClick(ClickEvent event) {
                PictureView.this.onCloseClicked();
            }
        });
        this.title.add(this.imgClose);

        if (Style.fullscreenEnabled()) {
            this.imgFullscreen = new TitleBarIcon(
                    Resources.INST.iconFullscreen(),
                    Resources.INST.iconUnFullscreen());
            this.imgFullscreen.addToggledHandler(new ToggledEventHandler() {
                @Override
                public void onToggled(ToggledEvent event) {
                    if (event.getState()) {
                        Style.requestFullscreen();                        
                    } else {
                        Style.exitFullscreen();
                    }
                }
            });
            if (Style.isFullscreen()) {
                this.imgFullscreen.setActive(true);
            }
            this.title.add(this.imgFullscreen);
        }
        
        /******************
         * CONTROLS PANEL *
         ******************/

        this.controls = new ControlsPanel();
        this.controls.addDomHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (PictureView.this.hideControlsTimer != null) {
                    PictureView.this.hideControlsTimer.cancel();
                }
            }
        }, MouseOverEvent.getType());
        this.controls.addDomHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                if (PictureView.this.hideControlsTimer != null) {
                    PictureView.this.hideControlsTimer
                            .schedule(HIDE_CONTROLS_TIMER);
                }
            }
        }, MouseOutEvent.getType());

        this.imgThumbs = new ControlButton(Resources.INST.thumbs()) {
            @Override
            public void onClick() {
                PictureView.this.onThumbsClicked();
            }
        };
        this.imgThumbs.setTitle("Select pictures from thumbnails");
        //this.controls.add(this.imgThumbs);

        this.imgZoom = new ControlButton(Resources.INST.zoom()) {
            @Override
            public void onClick() {
                PictureView.this.onZoomClicked();
            }
        };
        this.imgZoom.setTitle("Make the picture fullscreen (\u23ce)");
        this.controls.add(this.imgZoom);

        this.imgPrev = new ControlButton(Resources.INST.backward()) {
            @Override
            public void onClick() {
                PictureView.this.onPrevClicked();
            }
        };
        this.imgPrev.setTitle("Previous picture (\u2190)");
        this.controls.add(this.imgPrev);

        this.imgPlay = new ControlButton(Resources.INST.play(),
                Resources.INST.stop()) {
            @Override
            public void onClick() {
                PictureView.this.onPlayClicked();
            }
        };
        this.imgPlay.setTitle("Start/Stop the diaporama (space)");
        this.controls.add(this.imgPlay);

        this.imgNext = new ControlButton(Resources.INST.forward()) {
            @Override
            public void onClick() {
                PictureView.this.onNextClicked(false);
            }
        };
        this.imgNext.setTitle("Next picture (\u2192)");
        this.controls.add(this.imgNext);

        this.add(this.controls);
        this.setWidgetBottomHeight(this.controls, 5,
                Unit.PX,
                this.controls.getHeight(), Unit.PX);
        this.setWidgetLeftWidth(this.controls, 0, Unit.PX, 0, Unit.PX);
        this.controlsHeight = this.controls.getHeight() + 10;
        this.controls.getElement().getStyle().setOpacity(0);
        
        /**************
         * SIDE PANEL *
         **************/

        this.sidePanel = new SidePanel(MARGIN_SIDE);
        this.add(this.sidePanel);
        this.setWidgetTopBottom(this.sidePanel, TitleBar.getHeight(), Unit.PX,
                0, Unit.PX);
        this.setWidgetRightWidth(this.sidePanel, 0, Unit.PX, MARGIN_SIDE, Unit.PX);
        this.sidePanel.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                PictureView.this.canvas.setFocus(true);
            }
        });
    }
        
    public void reset()
    {
        // Upon detach, we reinitialize everything
        this.sidePanel.reset();
        this.attached = false;
        this.isFullscreen = false;        

        if (this.diapoTimer != null) {
            this.diapoTimer.cancel();
            this.diapoTimer = null;
        }
        
        if (this.loadingLabel != null) {
            this.remove(this.loadingLabel);
            this.loadingLabel = null;
        }
        
        this.loader.cancel();
        this.img = null;
        this.old = null;
        this.drawCanvas(0);
        this.keydownhandler.removeHandler();
        this.keydownhandler = null;
    }

    @Override
    public void onResize()
    {
        // redraw the canvas
        int w = this.getOffsetWidth() - this.right;
        int h = this.getOffsetHeight() - this.top;
        this.canvas.setWidth(w + "px");
        this.canvas.setHeight(h + "px");
        this.canvas.setCoordinateSpaceWidth(Style.toPixelRatio(w));
        this.canvas.setCoordinateSpaceHeight(Style.toPixelRatio(h));
        if (this.fadeAnim == null) {
            this.drawCanvas(1.0);
        }
        
        if (this.video != null) {
            this.video.setWidth(
                    (this.getOffsetWidth() - this.right) + "px");
            this.video.setHeight(
                    (this.getOffsetHeight() - this.top - this.controlsHeight) + "px");            
        }
        
        this.setWidgetLeftWidth(
                this.controls,
                (this.getOffsetWidth() - this.controls.getWidth()) / 2, Unit.PX,
                this.controls.getWidth(), Unit.PX);
    }
    
    public void setDescriptorsList(List<PictureDescriptor> descriptors) {
        this.descriptors = descriptors;
    }

    /**
     * Loads a picture from its picture descriptor
     * 
     * @param desc
     *            the picture descriptor
     */
    public void load(PictureDescriptor desc) {
        for (int j = 0; j < this.descriptors.size(); j++) {
            if (this.descriptors.get(j).getPictureId() == desc.getPictureId()) {
                this.load(j);

                return;
            }
        }
    }

    /**
     * Loads a picture from its index in the picture descriptor list
     * 
     * @param index
     *            the index
     */
    private void load(int index) {
        this.current = index;
        this.sidePanel.reset();
        if (this.infoRequest != null) {
            this.infoRequest.cancel();
            this.infoRequest = null;
        }
 
        if (this.attached) {
            /*
             * We need to wait for the view to be attached before loading a
             * picture: we need to know the view's size in order to be able to
             * fetch a picture of the appropriate size, and can't do so unless
             * we're attached to the DOM tree.
             */
            this.actuallyLoad();

            if (this.keydownhandler == null) {
                this.keydownhandler = this.canvas.addDomHandler(
                        new KeyDownHandler() {
                            @Override
                            public void onKeyDown(KeyDownEvent event) {
                                PictureView.this.onKeyDown(event.getNativeKeyCode());
                            }
                        }, KeyDownEvent.getType());            
            }
            this.canvas.setFocus(true);

        } else {
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    PictureView.this.attached = true;
                    PictureView.this.onResize();
                    PictureView.this.load(PictureView.this.current);
                }
            });
        }
    }
    
    /**
     * Creates and displays the 'loading' label on screen
     * 
     * @param name
     *            the name of the picture that is loading
     */
    private void createLoading(String name) {
        if (this.loadingLabel != null) {
            this.remove(this.loadingLabel);
        }
        this.loadingLabel = new Label("Loading " + name);
        this.loadingLabel.setStyleName("wg-pictureview-loading");

        this.add(this.loadingLabel);
        this.setWidgetLeftRight(this.loadingLabel, 0, Unit.PX, 0, Unit.PX);
        this.setWidgetTopBottom(this.loadingLabel,
                this.getOffsetHeight() / 2,
                Unit.PX, 0, Unit.PX);
    }

    /**
     * Prefetches the next picture in the list. This allows fast load of the
     * image for the logical user action (viewing the next picture)
     */
    private void prefetchNext() {
        if (this.current < 0)
            return;

        int nextIndex;
        
        if (this.current == this.descriptors.size() - 1) {
            nextIndex = 0;
        } else {
            nextIndex = this.current + 1;
        }

        if (this.descriptors.get(nextIndex).isPicture()) {
            this.loader.load(nextIndex, false);
        }
    }

    /**
     * Actually load the current picture, assuming that the widget is attached
     * to the DOM (to be able to retrieve its width and height)
     */
    private void actuallyLoad() {
        if (this.current < 0)
            return;

        if (this.current == this.loader.getIndex()) {
            // We are in the process of pre-fetching the image we want to
            // display.
            if (this.loader.isLoaded()) {
                this.onLoaded(this.loader.getImageElement());
                
                return;
                
            } else if (!this.loader.isCanceled ()) {
                this.loader.setShowOnLoad(true);
                
                return;
            }
        }

        if (this.loader.isLoading()) {
            this.loader.cancel();
        }
        
        PictureDescriptor desc = this.descriptors.get(this.current);
        
        if (desc.isPicture()) {
            if (this.video != null) {
                this.remove(this.video);
                this.video = null;
                this.imgPlay.setDisabled(false);
            }
            this.loader.load(this.current, true);
        } else {
            this.loader.index = this.current;
            this.loader.state = InternalLoader.ERROR;
            this.old = this.img;
            this.img = null;
            if (this.old != null) {
                this.fadeAnim = new Animation() {
                    @Override
                    protected void onUpdate(double progress) {
                        PictureView.this.drawCanvas(progress);
                        if (progress == 1) {
                            PictureView.this.old = null;
                            PictureView.this.fadeAnim = null;
                            PictureView.this.showVideo();
                        }
                    }
                };
                this.fadeAnim.run(800);                
            } else {
                this.showVideo();
            }
        }
    }

    /**
     * Called when the image element has been successfully loaded
     * 
     * @param elt
     *            the image element to display
     */
    private void onLoaded(ImageElement elt) {
        if (this.fadeAnim != null) {
            this.fadeAnim.cancel();
        }

        PictureDescriptor desc = this.descriptors.get(this.current);
        
        this.old = this.img;
        this.img = elt;
        
        this.sidePanel.setDesc(this.descriptors.get(this.current));
        this.sidePanel.setCount(this.current + 1, this.descriptors.size());
        this.infoRequest = new RequestsBuilder() {
            @Override
            public void onResponse(String content) {
                PictureView.this.sidePanel.setInfo(content);
                PictureView.this.infoRequest = null;
            }
        };
        this.infoRequest.getPictureInfo(desc);
        
        this.fireEvent(new PictureLoadedEvent(desc));
        if (this.loadingLabel != null) {
            this.remove(this.loadingLabel);
            this.loadingLabel = null;
        }

        PictureView.this.prefetchNext();

        if (this.diapoTimer != null) {
            this.diapoTimer.schedule(DIAPORAMA_TIMER);
        }
        this.fadeAnim = new Animation() {
            @Override
            protected void onUpdate(double progress) {
                PictureView.this.drawCanvas(progress);
                if (progress == 1) {
                    PictureView.this.old = null;
                    PictureView.this.fadeAnim = null;
                }
            }
        };
        this.fadeAnim.run(800);
    }
    
    private void showVideo() {
        if (this.video == null) {
            this.video = Video.createIfSupported();
            this.add(this.video);
            this.video.setControls(true);
            this.video.setAutoplay(true);
            this.setWidgetTopBottom(this.video, this.top, Unit.PX, this.controlsHeight, Unit.PX);
            this.setWidgetLeftRight(this.video, 0, Unit.PX, this.right, Unit.PX);
            this.imgPlay.setDisabled(true);
            this.video.addMouseMoveHandler(new MouseMoveHandler() {
                @Override
                public void onMouseMove(MouseMoveEvent event) {
                    PictureView.this.onMouseMove();
                }
            });
            this.onResize();
        }

        PictureDescriptor desc = this.descriptors.get(this.current);
        this.video.setSrc(desc.getSizeDescriptor(this.getOffsetWidth(), this.getOffsetHeight()).getUrl());
        this.sidePanel.setDesc(this.descriptors.get(this.current));
        this.sidePanel.setCount(this.current + 1, this.descriptors.size());
        this.infoRequest = new RequestsBuilder() {
            @Override
            public void onResponse(String content) {
                PictureView.this.sidePanel.setInfo(content);
                PictureView.this.infoRequest = null;
            }
        };
        this.infoRequest.getPictureInfo(desc);
        this.fireEvent(new PictureLoadedEvent(desc));
        if (this.loadingLabel != null) {
            this.remove(this.loadingLabel);
            this.loadingLabel = null;
        }
    }

    /**
     * Called upon mouse move event on the canvas
     */
    private void onMouseMove() {
        if (this.hideControlsTimer == null) {
            this.showControls();

            this.hideControlsTimer = new Timer() {
                @Override
                public void run() {
                    PictureView.this.hideControlsTimer = null;
                    PictureView.this.hideControls();
                }
            };

        } else {
            this.hideControlsTimer.cancel();
        }

        this.hideControlsTimer.schedule(HIDE_CONTROLS_TIMER);
    }

    /**
     * Shows the control using animation
     */
    private void showControls() {
        if (!PictureView.this.controls.getElement().getStyle().getOpacity()
                .equals("0"))
            return;
        Animation anim = new Animation() {
            @Override
            protected void onUpdate(double progress) {
                PictureView.this.controls.getElement().getStyle()
                        .setOpacity(progress);
            }
        };
        anim.run(100);
    }

    /**
     * Hides the control when mouse is inactive and the picture is zoomed (e.g.
     * takes the full browser window)
     */
    private void hideControls() {
        Animation anim = new Animation() {
            @Override
            protected void onUpdate(double progress) {
                PictureView.this.controls.getElement().getStyle()
                        .setOpacity(1.0 - progress);
            }
        };
        anim.run(100);
    }

    /**
     * Handles keyboard events
     * 
     * @param keyCode
     *            the code of the pressed key
     */
    private void onKeyDown(int keyCode) {
        switch (keyCode) {
        case KeyCodes.KEY_LEFT:
            this.imgPrev.clicked();
            break;
        case KeyCodes.KEY_RIGHT:
            this.imgNext.clicked();
            break;
        case KeyCodes.KEY_ENTER:
            this.imgZoom.clicked();
            break;
        case KeyCodes.KEY_ESCAPE:
            this.onCloseClicked();
            break;
        case ' ':
            this.imgPlay.clicked();
            break;
        }
    }

    /**
     * Called when the 'close' button is clicked
     */
    private void onCloseClicked() {
        this.keydownhandler.removeHandler();
        this.fireEvent(new CloseEvent(PictureView.this));
    }

    /**
     * Called when the 'play' button is clicked
     */
    private void onPlayClicked() {
        boolean running;
        if (this.diapoTimer == null) {
            running = true;
            this.diapoTimer = new Timer() {
                @Override
                public void run() {
                    PictureView.this.onNextClicked(true);
                }
            };
            this.diapoTimer.schedule(DIAPORAMA_TIMER);
            if (!this.isFullscreen) {
                this.onZoomClicked();
            }
        } else {
            running = false;
            this.diapoTimer.cancel();
            this.diapoTimer = null;
            if (this.isFullscreen) {
                this.onZoomClicked();
            }
        }
        this.imgPlay.setToggled(running);
        this.imgZoom.setDisabled(running);
        this.imgNext.setDisabled(running);
        this.imgPrev.setDisabled(running);
        this.imgThumbs.setDisabled(running);
    }

    /**
     * Called when the 'next' button is clicked
     */
    private void onNextClicked(boolean diaporama) {
        int next = this.current + 1;
        if (next == this.descriptors.size()) {
            next = 0;
        }
        while (diaporama && this.descriptors.get(next).isVideo()) {
            next += 1;
            if (next == this.descriptors.size()) {
                next = 0;
            }
        }
        this.load(next);
    }

    /**
     * Called when the 'previous' button is clicked
     */
    private void onPrevClicked() {
        if (this.current == 0) {
            this.load(this.descriptors.size() - 1);
        } else {
            this.load(this.current - 1);
        }
    }

    private void onThumbsClicked() {
        // TODO: implement
    }

    /**
     * Called when the zoom icon is clicked
     */
    private void onZoomClicked() {
        this.isFullscreen = !this.isFullscreen;

        this.imgThumbs.setDisabled(this.isFullscreen);

        Animation anim = new Animation() {
            @Override
            protected void onUpdate(double progress) {
                double anim;
                if (PictureView.this.isFullscreen)
                    anim = 1.0 - progress;
                else
                    anim = progress;
                PictureView.this.top = (int) (TitleBar.getHeight() * anim);
                PictureView.this.right = (int) (MARGIN_SIDE * anim);
                PictureView.this.setWidgetTopHeight(PictureView.this.title,
                        0, Unit.PX, top, Unit.PX);
                
                PictureView.this.setWidgetTopBottom(PictureView.this.sidePanel,
                        top, Unit.PX, 0, Unit.PX);                
                PictureView.this.setWidgetRightWidth(PictureView.this.sidePanel,
                        PictureView.this.right - MARGIN_SIDE, Unit.PX, MARGIN_SIDE, Unit.PX);

                PictureView.this.setWidgetTopBottom(PictureView.this.canvas,
                        top, Unit.PX, 0, Unit.PX);
                PictureView.this.setWidgetLeftRight(PictureView.this.canvas,
                        0, Unit.PX, PictureView.this.right, Unit.PX);

                if (PictureView.this.video != null) {
                    PictureView.this.video.setHeight(
                            (PictureView.this.getOffsetHeight() - top - controlsHeight) + "px");
                    PictureView.this.video.setWidth(
                            (PictureView.this.getOffsetWidth() - right) + "px");
                    PictureView.this.setWidgetTopBottom(PictureView.this.video,
                            top, Unit.PX, controlsHeight, Unit.PX);
                    PictureView.this.setWidgetLeftRight(PictureView.this.video,
                            0, Unit.PX, PictureView.this.right, Unit.PX);
                }
                PictureView.this.onResize();
            }
        };
        anim.run(300);
    }

    /**
     * Called when the download menu is clicked
     */
    private void onDownloadClicked() {
        RequestsBuilder.downloadPicture(this.descriptors
                .get(this.current));
    }

    /**
     * Draws the canvas, according to the progress of the transition between the
     * potential old image and the new one
     * 
     * @param progress
     *            progress of the transition between this.old and this.img
     */
    private void drawCanvas(double progress) {
        Context2d ctx = this.canvas.getContext2d();
        ctx.save();

        ctx.clearRect(0, 0, this.canvas.getCoordinateSpaceWidth(),
                this.canvas.getCoordinateSpaceHeight());

        if (this.old != null) {
            //  We most of the time need to resize the image to fit the
            //  canvas. Resize operations being quite costly, we do it only
            //  once per transition, and save the resized result in a cache
            //  During transition, we then just draw the cache directly at
            //  1:1 pixel ratio
            if (this.cachePrev == null) {
                this.cachePrev = Canvas.createIfSupported();
                this.cachePrev.setCoordinateSpaceWidth(
                        this.canvas.getCoordinateSpaceWidth());
                this.cachePrev.setCoordinateSpaceHeight(
                        this.canvas.getCoordinateSpaceHeight());
                
                Context2d cachectxt = this.cachePrev.getContext2d();
                this.drawImage(cachectxt, this.old);
            }
            
            ctx.setGlobalAlpha(1.0 - progress);
            ctx.drawImage(this.cachePrev.getCanvasElement(), 0, 0);
            
            //  Remove the cache when done
            if (progress == 1) {
                this.cachePrev = null;
            }
        }

        if (this.img != null) {
            if (this.cacheNext == null) {
                this.cacheNext = Canvas.createIfSupported();
                this.cacheNext.setCoordinateSpaceWidth(
                        this.canvas.getCoordinateSpaceWidth());
                this.cacheNext.setCoordinateSpaceHeight(
                        this.canvas.getCoordinateSpaceHeight());
                
                Context2d cachectxt = this.cacheNext.getContext2d();
                this.drawImage(cachectxt, this.img);
            }
            
            ctx.setGlobalAlpha(progress);
            ctx.drawImage(this.cacheNext.getCanvasElement(), 0, 0);
            
            if (progress == 1) {
                this.cacheNext = null;
            }
        }
        ctx.restore();
        
        if (progress == 1 && this.old == null) {
            /* finished the first appearance of a picture: let's show the
               controls.
               Calling onMouseMove will initiate the timer that shows/hides the
               controls box.
            */
            this.onMouseMove();

        }
    }

    /**
     * draws a scaled img in the context of ctx.
     * 
     * @param ctx
     *            the Context2d used to draw on the canvas
     * @param img
     *            the image to draw
     */
    private void drawImage(Context2d ctx, ImageElement img) {
        int w = this.canvas.getCoordinateSpaceWidth();
        int h = this.canvas.getCoordinateSpaceHeight();
        int imgw, imgh;
        double ratioW, ratioH, ratio;

        ratioW = (double) w / (double) img.getWidth();
        ratioH = (double) h / (double) img.getHeight();

        ratio = Math.min(ratioW, ratioH);
        imgw = (int) Math.round(img.getWidth() * ratio);
        imgh = (int) Math.round(img.getHeight() * ratio);

        ctx.drawImage(img, (w - imgw) / 2, (h - imgh) / 2, imgw, imgh);
    }

    public HandlerRegistration addCloseEventHandler(
            CloseEventHandler handler) {
        return this.addHandler(handler, CloseEvent.TYPE);
    }

    public HandlerRegistration addPictureLoadedEventHandler(
            PictureLoadedEventHandler handler) {
        return this.addHandler(handler, PictureLoadedEvent.TYPE);
    }
}
