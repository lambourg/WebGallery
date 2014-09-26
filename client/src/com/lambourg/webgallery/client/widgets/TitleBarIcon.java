/* Copyright Jerome Lambourg, 2014 
 * Licensed under GPL v3
 */
package com.lambourg.webgallery.client.widgets;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.Composite;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.lambourg.webgallery.client.events.ToggledEvent;
import com.lambourg.webgallery.client.events.ToggledEventHandler;
import com.lambourg.webgallery.client.folderview.Style;

public class TitleBarIcon
        extends LayoutPanel
        implements HasHandlers
{
    private Canvas canvas;
    private Image imgNormal;
    private Image imgActive;
    private boolean active;
    private boolean over;

    public TitleBarIcon(ImageResource normal) {
        this(normal, null);
    }
    
    public TitleBarIcon(ImageResource normal, ImageResource active) {
        super();
        this.setStyleName("wg-titlebar-icon");

        this.setWidth(Style.titleIconSize + "px");
        this.setHeight(Style.titleIconSize + "px");

        this.imgNormal = new Image(normal.getSafeUri());
        if (active != null) {
            this.imgActive = new Image(active.getSafeUri());
        } else {
            this.imgActive = null;
        }
        
        this.canvas = Canvas.createIfSupported();
        this.add(this.canvas);
        this.canvas.setWidth(Style.titleIconSize + "px");
        this.canvas.setHeight(Style.titleIconSize + "px");

        this.active = false;
        this.over = false;
        
        this.setWidgetLeftRight(this.canvas, 0, Unit.PX, 0, Unit.PX);
        this.setWidgetTopBottom(this.canvas, 0, Unit.PX, 0, Unit.PX);

        this.canvas.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                TitleBarIcon.this.over = true;
                TitleBarIcon.this.draw();
            } 
        });
        this.canvas.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                TitleBarIcon.this.over = false;
                TitleBarIcon.this.draw();
            } 
        });
        this.canvas.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                TitleBarIcon.this.clicked(event);
            }
        });
        this.addAttachHandler(new AttachEvent.Handler() {            
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (!event.isAttached()) {
                    return;
                }
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                    @Override
                    public void execute() {
                        TitleBarIcon.this.draw();
                    }
                });
            }
        });
    }
    
    public void setActive(boolean active) {
        if (this.imgActive != null) {
            this.active = active;
            this.draw();
        }
    }

    public boolean getActive() {
        return this.active;
    }

    private void clicked(ClickEvent event) {
        if (this.imgActive != null) {
            this.active = !this.active;
            this.draw();
            this.fireEvent(new ToggledEvent(this, this.active));
        } else {
            this.fireEvent(event);
        }
        
    }

    /* Draws the icon using only its shape, and replacing the color by the
     * appropriate foreground color regarding the icon state
     */
    private void draw() {
        Context2d ctx;
        Canvas buffer = Canvas.createIfSupported();
        Image img;
        int size = Style.toPixelRatio(Style.titleIconSize);

        this.canvas.setCoordinateSpaceWidth(size);
        this.canvas.setCoordinateSpaceHeight(size + Style.toPixelRatio(1));
        buffer.setCoordinateSpaceWidth(size);
        buffer.setCoordinateSpaceHeight(size);

        if (this.active) {
            img = this.imgActive;
        } else {
            img = this.imgNormal;
        }

        //  We need to draw the icon itself in a separate buffer, and only
        //  after apply the shadow.
        //  The reason is that in order to draw the icon using the color we
        //  want, we need to use composite operations, and this is not
        //  compatible with the shadow effect we want.
        ctx = buffer.getContext2d();
        if (this.over) {
            ctx.setFillStyle("#fff");
        } else {
            ctx.setFillStyle("#ddd");
        }
        ctx.fillRect(0, 0, size, size);
        ctx.setGlobalCompositeOperation(Composite.DESTINATION_ATOP);
        ctx.drawImage(ImageElement.as(img.getElement()), 0, 0, size, size);
        ctx.restore();

        ctx = this.canvas.getContext2d();
        ctx.setShadowBlur(0.0);
        ctx.setShadowColor("#333");
        ctx.setShadowOffsetY(Style.toPixelRatio(1));

        ctx.drawImage(buffer.getCanvasElement(), 0, 0, size, size);
    }

    public int getWidth() {
        return Style.titleIconSize;
    }

    public int getHeight() {
        return Style.titleIconSize;
    }

    public HandlerRegistration addToggledHandler(ToggledEventHandler handler) {
        return this.addHandler(handler, ToggledEvent.TYPE);
    }
    
    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return this.addHandler(handler, ClickEvent.getType());
    }
}