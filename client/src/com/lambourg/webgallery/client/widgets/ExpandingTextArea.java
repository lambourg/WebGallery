package com.lambourg.webgallery.client.widgets;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextArea;

/**
 * Expanding text area is a text area that automatically expands vertically
 * as more text is added to it.
 * 
 * @author lambourg
 *
 */
public class ExpandingTextArea extends TextArea
implements HasResizeHandlers
{
    private static final int PADDING = 4;
    private static final int BORDER = 1;
    private int currentHeight = 0;

    /**
     * Creates a new expanding text area of the specified width
     * @param width
     */
    public ExpandingTextArea(int width) {
        super();
        String placeholder = "Write a comment...";
        this.setTitle(placeholder);
        this.getElement().setPropertyString(
                "placeholder", placeholder);
        this.getElement().getStyle().setProperty("resize", "none");
        this.getElement().getStyle().setPadding(PADDING, Unit.PX);
        this.getElement().getStyle().setBorderWidth(BORDER, Unit.PX);
        this.getElement().getStyle().setOverflow(Overflow.HIDDEN);
        this.getElement().getStyle().setBackgroundColor("white");
        
        int w = width - 2 * PADDING - 2 * BORDER;
        this.setWidth(w + "px");
        
        this.sinkEvents(Event.ONPASTE);
        this.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                ExpandingTextArea.this.doResize();
            }
        });
        this.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                ExpandingTextArea.this.doResize();
            }
        });
        
        this.reset();
    }
    
    public void reset() {
        this.setText(" ");
        this.currentHeight = 0;
        this.setHeight("0px");
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {                
            @Override
            public void execute() {
                ExpandingTextArea.this.doResize();
                ExpandingTextArea.this.setText("");
            }
        });        
    }
    
    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        switch (DOM.eventGetType(event)) {
        case Event.ONPASTE:
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    ValueChangeEvent.fire(ExpandingTextArea.this, getText());
                }
            });
        }
    }
    
    private void doResize() {
        int height = (int)this.getElement().getScrollHeight() - 2 * PADDING;
        if (height != this.currentHeight && height > 0) {
            this.currentHeight = height;
            this.setHeight(this.currentHeight + "px");
            this.fireEvent(
                    new ResizeEvent(this.getOffsetWidth(), height) {});
        }
    }
    
    public int getHeight() {
        return this.currentHeight + 2 * PADDING + 2 * BORDER;
    }

    @Override
    public HandlerRegistration addResizeHandler(ResizeHandler handler) {
        return this.addHandler(handler, ResizeEvent.getType());
    }
}
