package com.lambourg.webgallery.client.pictureview;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.lambourg.webgallery.client.folderview.Style;

public class ControlsPanel extends LayoutPanel {
    private int width, height;
    
    private int MARGIN = 10;
    
    public ControlsPanel() {
        super();
        this.setStyleName("wg-pictureview-controls");
        this.getElement().getStyle()
                .setZIndex(Style.Z_INDEX_FULLSCREEN + 10);
        this.width = MARGIN;
        this.height = 2 * MARGIN;
    }
    
    public void add(ControlButton button) {
        super.add(button);
        this.setWidgetLeftWidth(button, this.width, Unit.PX,
                button.getWidth(), Unit.PX);
        this.width += button.getWidth() + MARGIN;
        
        this.setWidgetBottomHeight(button, MARGIN, Unit.PX,
                button.getHeight(), Unit.PX);
        int w = button.getHeight() + 2 * MARGIN;
        if (w > this.height) this.height = w;
    }
    
    public int getWidth() {
        return this.width;
    }
    
    public int getHeight() {
        return this.height;
    }

}
