package com.lambourg.webgallery.client.widgets;

import com.google.gwt.user.client.ui.Label;

public class TitleBarLink extends Label {
    
    public TitleBarLink(String label) {
        super(label);
        this.setStyleName("wg-titlebar-label");
    }

}
