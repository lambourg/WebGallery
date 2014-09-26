package com.lambourg.webgallery.client.pictureview;

import java.util.LinkedHashMap;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class ExifBox extends FlowPanel {
    private Label arrow;
    private Label title;
    private FlowPanel dataBox;
    private boolean opened;
    
    // WHITE RIGHT-POINTING SMALL TRIANGLE
    private static final String CLOSED_LABEL = "\u25B9";
    // WHITE DOWN-POINTING SMALL TRIANGLE
    private static final String OPENED_LABEL = "\u25BF";
    
    public ExifBox(int width) {
        super();
        this.setStyleName("wg-sidepanel-exif");
        
        this.arrow = new Label();
        this.arrow.setStyleName("wg-sidepanel-exifheader");
        this.arrow.setWidth("15px");
        this.arrow.setHeight("1.3em");
        this.add(this.arrow);
        this.arrow.getElement().getStyle().setMarginLeft(0, Unit.PX);
        this.arrow.getElement().getStyle().setMarginRight(5, Unit.PX);
        this.arrow.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
        this.arrow.getElement().getStyle().setVerticalAlign(VerticalAlign.TOP);
        this.arrow.getElement().getStyle().setCursor(Cursor.POINTER);
        
        this.title = new Label("Exif data");
        this.title.setStyleName("wg-sidepanel-exifheader");
        this.title.setWidth((width - 20) + "px");
        this.add(this.title);
        this.title.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
        
        this.arrow.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ExifBox.this.onArrowClicked();
            }
        });
        
        this.dataBox = new FlowPanel();
        this.dataBox.setStyleName("wg-sidepanel-exifdata");
        
        this.opened = false;
        this.onArrowClicked();
    }
    
    public void reset() {
        this.dataBox.clear();
    }
    
    public void setData(LinkedHashMap<String, String> exifData) {
        this.reset();
        
        String date = null;
        String iso = null;
        String speed = null;
        String aperture = null;
        String focal = null;
        String focalEq = null;
        String lens = null;
        String camera = null;
        LinkedHashMap<String, String> others = new LinkedHashMap<String, String>();

        for (String name : exifData.keySet()) {
            String value = exifData.get(name);

            if (name.equals("Camera")) {
                camera = value;
            } else if (name.equals("Lens")) {
                lens = value;
            } else if (name.equals("Date")) {
                date = value;
            } else if (name.equals("ISO Speed")) {
                iso = value;
            } else if (name.equals("Exposure time")) {
                speed = value;
            } else if (name.equals("Aperture")) {
                aperture = value;
            } else if (name.equals("Focal Length")) {
                focal = value;
            } else if (name.equals("Focal Length 35mm eq.")) {
                focalEq = value;
            } else {
                others.put(name, value);
            }
        }

        if (date != null) {
            Label label = new Label(date);
            label.setStyleName("big");
            this.dataBox.add(label);
        }
        if (iso != null) {
            Label label = new Label(speed + ", " + aperture + ", " + iso);
            this.dataBox.add(label);
        }
        if (focal != null) {
            Label label = new Label();
            String text = focal;
            if (focalEq != null && !focal.equals(focalEq)) {
                text += ", eq. " + focalEq;
            }
            if (lens != null) {
                text += " (" + lens + ")";
            }
            label.setText(text);
            this.dataBox.add(label);
        }
        if (camera != null) {
            Label label = new Label(camera);
            label.setStyleName("small");
            this.dataBox.add(label);
        }

        for (String key : others.keySet()) {
            String value = others.get(key);
            Label label = new Label(key + ": " + value);
            label.setStyleName("small");
            this.dataBox.add(label);
        }
    }
    
    public void onArrowClicked() {
        this.opened = !this.opened;
        
        if (this.opened) {
            this.arrow.setText(OPENED_LABEL);
            this.add(this.dataBox);
        } else {
            this.arrow.setText(CLOSED_LABEL);
            this.remove(this.dataBox);
        }
    }
}
