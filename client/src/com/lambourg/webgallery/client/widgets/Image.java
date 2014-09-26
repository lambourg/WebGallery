package com.lambourg.webgallery.client.widgets;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;

public class Image extends com.google.gwt.user.client.ui.Image {

    public Image(ImageResource resource) {
        this(resource, ImageResource.RepeatStyle.None);
    }
    
    public Image(SafeUri uri) {
        super(uri);
    }

    public Image(ImageResource resource, ImageResource.RepeatStyle style) {
        super();

        switch (style) {
        case None:
            this.setResource(resource);
            break;
        case Horizontal:
            this.setUrl(resource.getSafeUri());
            this.setWidth("100%");
            this.setHeight(resource.getHeight() + "px");
            break;
        case Vertical:
            this.setUrl(resource.getSafeUri());
            this.setWidth(resource.getWidth() + "px");
            this.setHeight("100%");
            break;
        case Both:
            this.setUrl(resource.getSafeUri());
            this.setWidth("100%");
            this.setHeight("100%");
            break;
        }
    }

}
