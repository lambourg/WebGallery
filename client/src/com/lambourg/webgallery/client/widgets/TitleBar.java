package com.lambourg.webgallery.client.widgets;

import java.util.ArrayList;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.lambourg.webgallery.client.folderview.Style;

public class TitleBar extends LayoutPanel {
    private ArrayList<TitleBarIcon> icons;
    private FlowPanel linkPanel;

    public TitleBar() {
        super();
        
        this.addAttachHandler(new AttachEvent.Handler () {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (event.isAttached()) {
                    Element elt = TitleBar.this.getElement().getParentElement();
                    elt.setClassName("wg-titlebar");                    
                }
            }
        });

        this.icons = new ArrayList<TitleBarIcon>();
        this.linkPanel = new FlowPanel();
        this.add(this.linkPanel);
    }
    
    public void add(TitleBarIcon icon) {
        this.icons.add(icon);
        super.add(icon);
        int right, top;

        top = (TitleBar.getHeight() - Style.titleIconSize) / 2;
        this.setWidgetTopHeight(icon, top, Unit.PX, Style.titleIconSize, Unit.PX);

        right = 10;
        
        for (TitleBarIcon icn : this.icons) {
            this.setWidgetRightWidth(icn, right, Unit.PX, Style.titleIconSize, Unit.PX);
            right += Style.titleIconSize + 10;
        }
        this.setWidgetLeftRight(this.linkPanel, 30, Unit.PX, right, Unit.PX);
    }
    
    public void add(TitleBarLink link) {
        this.linkPanel.add(link);
    }
    
    public void remove(TitleBarLink link) {
        this.linkPanel.remove(link);
    }
    
    public static int getHeight() {
        return Style.titleIconSize + 6;
    }
}
