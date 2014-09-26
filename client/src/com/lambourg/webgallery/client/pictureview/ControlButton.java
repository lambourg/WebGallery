package com.lambourg.webgallery.client.pictureview;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.lambourg.webgallery.client.widgets.Image;

/**
 * This class is used to manage buttons enclosed within the control widget of
 * the picture view.
 */
abstract class ControlButton extends Image {
    private int w, h;
    private ImageResource resource;
    private ImageResource alt;
    private boolean disabled;

    /**
     * Creates a push button using resource as image
     * 
     * @param resource
     *            the image of the button
     */
    public ControlButton(ImageResource resource) {
        this(resource, null);
    }

    /**
     * Creates a toggle button, using alt as alternate image when clicked.
     * 
     * @param resource
     *            the default image to display
     * @param alt
     *            the alternate image to display when toggled
     */
    public ControlButton(ImageResource resource, ImageResource alt) {
        super(resource, RepeatStyle.Both);
        this.resource = resource;
        this.alt = alt;
        this.disabled = false;

        /*
         * By default, we always load pictures in @2 size: this is to support
         * HiDPI displays, and switching back and forth from a regular display
         * and a HiDPI one.
         */
        this.w = resource.getWidth() / 2;
        this.h = resource.getHeight() / 2;
        this.setDisabled(false);

        this.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ControlButton.this.clicked();
            }
        });
        this.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (!ControlButton.this.disabled) {
                    ControlButton.this.getElement().getStyle()
                            .setOpacity(1);
                }
            }
        });
        this.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                if (!ControlButton.this.disabled) {
                    ControlButton.this.getElement().getStyle()
                            .setOpacity(0.8);
                }
            }
        });
    }

    /**
     * Called when the mouse clicked on the button.
     */
    public void clicked() {
        if (!ControlButton.this.disabled) {
            this.onClick();
        }
    }

    /**
     * Marks the button as toggled, if supported
     * 
     * @param toggled
     *            the toggled state to set.
     */
    public void setToggled(boolean toggled) {
        if (toggled && this.alt != null) {
            this.setUrl(this.alt.getSafeUri());
        } else {
            this.setUrl(this.resource.getSafeUri());
        }
    }

    /**
     * Marks the button as disabled. Once disabled, a button is grayed out and
     * won't react to mouse events.
     * 
     * @param disabled
     *            the disabled state to set.
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (disabled) {
            this.getElement().getStyle().setOpacity(0.5);
            this.getElement().getStyle().setCursor(Cursor.DEFAULT);
        } else {
            this.getElement().getStyle().setOpacity(0.8);
            this.getElement().getStyle().setCursor(Cursor.POINTER);
        }
    }

    /**
     * To be implemented: this callback is called when the button is clicked and
     * is not disabled.
     */
    public abstract void onClick();

    /**
     * Retrieves the width of the button, in pixel
     */
    public int getWidth() {
        return this.w;
    }

    /**
     * Retrieves the height of the button, in pixel
     */
    public int getHeight() {
        return this.h;
    }
}
