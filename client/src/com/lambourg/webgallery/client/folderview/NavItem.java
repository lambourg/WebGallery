package com.lambourg.webgallery.client.folderview;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.lambourg.webgallery.client.events.ItemClickedEvent;
import com.lambourg.webgallery.client.events.ItemClickedEventHandler;

public class NavItem 
    extends FlowPanel
{
    private FlowPanel self;
    private Canvas arrow;
    private Label text;

    private String id;
    private String title;
    private Boolean hasPictures;
    private Boolean isOpen;
    private Boolean isOver;
    private Boolean isSelected;
    List<NavItem> children;

    public static List<NavItem> bind(Element doc, int indent) {
        ArrayList<NavItem> list = new ArrayList<NavItem>();

        assert (doc.getTagName().equals("folder"));

        NodeList nodes = doc.getChildNodes();

        for (int j = 0; j < nodes.getLength(); j++) {
            Node node = nodes.item(j);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                NavItem item = new NavItem((Element) nodes.item(j), indent);
                list.add(item);
            }
        }

        return list;
    }
    
    private static int indentWidth(int indent) {
        return indent * (Style.NAV_ARROW_SIZE + 5) + 10;
    }

    public NavItem(Element elt, int indent) {
        super();

        this.setWidth(Style.NAV_WIDTH + "px");
        this.id = elt.getAttribute("id");
        this.title = elt.getAttribute("name");
        if (this.title.equals("/")) {
            this.title = "Folders";
        }
        this.hasPictures = elt.getAttribute("haspictures").equals("1");
        this.children = NavItem.bind(elt, indent + 1);
        this.getElement().getStyle().setCursor(Cursor.POINTER);

        for (NavItem child : this.children) {
            child.addItemClickedEventHandler(new ItemClickedEventHandler() {
                @Override
                public void onItemClicked(ItemClickedEvent event) {
                    NavItem.this.fireEvent(event);
                }
            });
        }

        this.self = new FlowPanel();
        this.self.setStyleName("wg-navitem");
        this.self.setWidth(Style.NAV_WIDTH + "px");
        this.add(this.self);
        
        this.arrow = Canvas.createIfSupported();
        this.arrow.setWidth((Style.NAV_ARROW_SIZE) + "px");
        this.arrow.setHeight("1.3em");
        this.self.add(this.arrow);
        this.arrow.getElement().getStyle().setMarginLeft(indentWidth(indent), Unit.PX);
        this.arrow.getElement().getStyle().setMarginRight(5, Unit.PX);
        this.arrow.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
        this.arrow.getElement().getStyle().setVerticalAlign(VerticalAlign.TOP);

        this.text = new Label(this.title);
        this.text.setStyleName("wg-navitem-text");
        this.self.add(this.text);
        this.text.setWidth((Style.NAV_WIDTH - indentWidth(indent + 1)) + "px");
        this.text.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);

        this.isOpen = false;
        this.isSelected = false;
        this.isOver = false;
        if (indent == 0) {
            this.onClick();
        }

        // Wait for the item to be actually attached
        this.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (!event.isAttached()) {
                    return;
                }
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                    @Override
                    public void execute() {
                        NavItem.this.drawCanvas();
                    }
                });
            }
        });

        this.self.addDomHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                NavItem.this.isOver = true;
                NavItem.this.drawCanvas();
            }
        }, MouseOverEvent.getType());
        this.self.addDomHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                NavItem.this.isOver = false;
                NavItem.this.drawCanvas();
            }
        }, MouseOutEvent.getType());
        this.self.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                NavItem.this.onClick();
            }
        }, ClickEvent.getType());
    }
    
    public String getDirId() {
        return this.id;
    }
    
    public String getDirName() {
        return this.title;
    }
    
    public NavItem expand(String dirId) {
        if (this.id.equals(dirId)) {
            return this;
        }

        for (NavItem child: this.children) {
            NavItem expanded = child.expand(dirId);
            if (expanded != null) {
                if (!this.isOpen) {
                    this.onClick();
                }
                return expanded;
            }
        }
        
        return null;
    }
    
    public void refreshChildren() {
        if (this.isOpen) {
            for (NavItem child: this.children) {
                child.drawCanvas();
                child.refreshChildren();
            }
        }
    }

    public String getDirName(String dirId) {
        if (this.id.equals(dirId))
            return this.title;
        
        for (NavItem child: this.children) {
            String ret = child.getDirName(dirId);
            if (ret != null) {
                return ret;
            }
        }
        
        return null;
    }
    
    /**
     * callback upon click
     */
    private void onClick() {
        this.isOpen = !this.isOpen;
        this.drawCanvas();

        if (this.hasPictures) {
            this.fireEvent(new ItemClickedEvent(this));
        }
        if (!this.children.isEmpty()) {
            if (this.isOpen) {
                for (NavItem child : this.children) {
                    this.add(child);
                    child.drawCanvas();
                }
            } else {
                for (NavItem child : this.children) {
                    child.isOver = false;
                    this.remove(child);
                }
            }
        }
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        String stylename = "wg-navitem-selected";
        if (selected) {
            this.self.addStyleName(stylename);
        } else {
            this.self.removeStyleName(stylename);
        }
        this.drawCanvas();
    }

    public int getHeight() {
        int height = this.arrow.getOffsetHeight();
        if (this.isOpen) {
            for (NavItem child : this.children) {
                height += child.getHeight();
            }
        }
        return height;
    }
    
    private void drawArrow(Context2d ctxt, int x, int y) {
        int size = Style.toPixelRatio(Style.NAV_ARROW_SIZE);
        
        ctxt.save();
        ctxt.beginPath();
        if (this.isOpen) {
            // __
            // \/
            ctxt.moveTo(x, y);
            ctxt.lineTo(x + size, y);
            ctxt.lineTo(x + size / 2, y + size);
        } else {
            // |\
            // |/
            ctxt.moveTo(x, y);
            ctxt.lineTo(x, y + size);
            ctxt.lineTo(x + size, y + size / 2);
        }
        ctxt.closePath();
        ctxt.fill();
        ctxt.restore();
        
    }

    private void drawFolder(Context2d ctxt, int x, int y) {
        double size = Style.toPixelRatio(Style.NAV_ARROW_SIZE);
        double linew = Style.toPixelRatio(1);
        
        ctxt.save();
        ctxt.beginPath();
        ctxt.moveTo(x + linew, y + linew);
        ctxt.lineTo(x + linew, y + size - linew);
        ctxt.lineTo(x + size - linew, y + size / 2);
        ctxt.closePath();
        ctxt.stroke();
        ctxt.restore();
    }
    
    private void drawCanvas() {
        int height = this.arrow.getOffsetHeight();
        int width = this.arrow.getOffsetWidth();
        this.arrow.setCoordinateSpaceWidth(Style.toPixelRatio(width));
        this.arrow.setCoordinateSpaceHeight(Style.toPixelRatio(height));
        int x = (Style.toPixelRatio(width - Style.NAV_ARROW_SIZE)) / 2;
        int y = (Style.toPixelRatio(height - Style.NAV_ARROW_SIZE)) / 2;

        // Retrieve and setup the context
        Context2d ctxt = this.arrow.getContext2d();

        if (this.isOver || this.isSelected) {
            ctxt.setFillStyle("#fff");
            ctxt.setStrokeStyle("#fff");
        } else {
            ctxt.setFillStyle("#eee");
            ctxt.setStrokeStyle("#eee");
        }
        ctxt.setLineWidth(Style.toPixelRatio(1));
        ctxt.setShadowBlur(0.0);
        ctxt.setShadowColor("#333");
        ctxt.setShadowOffsetY(Style.toPixelRatio(1));

        // The arrow
        if (!this.children.isEmpty()) {
            this.drawArrow(ctxt, x, y);
        } else {
            this.drawFolder(ctxt, x, y);
        }
    }

    public HandlerRegistration addItemClickedEventHandler(
            ItemClickedEventHandler handler) {
        return this.addHandler(handler, ItemClickedEvent.TYPE);
    }
}
