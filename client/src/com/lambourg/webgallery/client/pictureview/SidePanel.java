package com.lambourg.webgallery.client.pictureview;

import java.util.ArrayList;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import com.lambourg.webgallery.client.resources.Resources;
import com.lambourg.webgallery.client.rpc.RequestsBuilder;
import com.lambourg.webgallery.shared.PictureDescriptor;
import com.lambourg.webgallery.client.widgets.ExpandingTextArea;

public class SidePanel extends LayoutPanel implements
        HasValueChangeHandlers<Boolean>
{
    class LinkLabel extends Label {
        private LinkLabel(String label) {
            super(label);
            this.setStyleName("wg-sidepanel-link");
        }
    }

    class LikePanel extends FlowPanel {
        private boolean you = false;
        private ArrayList<String> others;

        private LikePanel() {
            super();
            this.setStyleName("wg-sidepanel-likes");
            this.update(false, new ArrayList<String>());
        }

        private void update(boolean you, NodeList people) {
            ArrayList<String> list = new ArrayList<String>();
            for (int j = 0; j < people.getLength(); ++j) {
                Element child = (Element) people.item(j);
                list.add(child.getAttribute("full_name"));
            }
            this.update(you, list);
        }

        public void reset() {
            this.update(false, new ArrayList<String>());
        }

        private void update(boolean you, ArrayList<String> others) {
            this.you = you;
            this.others = others;

            this.clear();

            if (!this.you && this.others.isEmpty()) {
                this.setVisible(false);
            } else {
                this.setVisible(true);

                if (this.you) {
                    String text = "You";
                    if (!this.others.isEmpty()) {
                        text += "&nbsp;and&nbsp;";
                    }
                    HTML label = new HTML(text);
                    this.add(label);
                }
                if (!this.others.isEmpty()) {
                    String text;
                    if (this.others.size() == 1) {
                        text = this.others.get(0);
                    } else {
                        text = Integer.toString(this.others.size());

                        if (this.you) {
                            text += " other";
                        }

                        text += " people";
                    }

                    HTML label = new HTML(text);

                    if (this.others.size() == 1) {
                        this.add(label);
                    } else {
                        FlowPanel peopleLink = new FlowPanel();
                        this.add(peopleLink);
                        peopleLink.setStyleName("wg-sidepanel-link");

                        Image img = new Image(Resources.INST.popupCallout());
                        img.setStyleName("wg-popup-callout");
                        peopleLink.add(img);

                        FlowPanel popup = new FlowPanel();
                        popup.setStyleName("wg-popup");
                        peopleLink.add(popup);

                        for (String name : this.others) {
                            popup.add(new Label(name));
                        }
                        peopleLink.add(label);
                    }
                }
                if (!this.you && this.others.size() == 1) {
                    HTML label = new HTML("&nbsp;likes it.");
                    this.add(label);
                } else {
                    HTML label = new HTML("&nbsp;like it.");
                    this.add(label);
                }

                FlowPanel sep = new FlowPanel();
                sep.setStyleName("wg-sidepanel-delimiter");
                this.add(sep);
            }
        }
    }

    private class Comment extends FlowPanel {
        public Comment(String user, String date, String content) {
            super();
            this.setStyleName("wg-sidepanel-comment");

            Label label;

            label = new Label(user);
            label.setStyleName("user");
            this.add(label);

            label = new Label(date);
            label.setStyleName("date");
            this.add(label);

            label = new Label(content);
            label.setStyleName("content");
            this.add(label);
        }
    }

    private PictureDescriptor desc;
    private ScrollPanel scroll;
    private FlowPanel flowPanel;
    private Label titleLabel;
    private Label countLabel;
    private Label folderLabel;
    private ExifBox exifBox;
    private int width;
    private static final int PADDING = 10;
    private LinkLabel likeLink;
    private LikePanel likes;
    private FlowPanel commentsBox;
    private ExpandingTextArea commentEntry;

    public SidePanel(int width) {
        super();
        this.setStyleName("wg-sidepanel");

        this.setWidth(width + "px");
        this.width = width - 2 * PADDING;

        this.scroll = new ScrollPanel();
        this.add(this.scroll);
        this.setWidgetTopBottom(this.scroll, 0, Unit.PX, 0, Unit.PX);
        this.setWidgetLeftRight(this.scroll, 0, Unit.PX, 0, Unit.PX);

        this.flowPanel = new FlowPanel();
        scroll.add(this.flowPanel);
        this.flowPanel.getElement().getStyle().setPadding(PADDING, Unit.PX);

        this.titleLabel = new Label();
        this.flowPanel.add(this.titleLabel);
        this.titleLabel.setStyleName("wg-sidepanel-title");

        this.folderLabel = new Label();
        this.flowPanel.add(this.folderLabel);
        this.folderLabel.setStyleName("wg-sidepanel-folder");

        this.countLabel = new Label();
        this.flowPanel.add(this.countLabel);
        this.countLabel.setStyleName("wg-sidepanel-count");

        this.exifBox = new ExifBox(this.width);
        this.flowPanel.add(this.exifBox);

        FlowPanel linkList = new FlowPanel();
        this.flowPanel.add(linkList);
        linkList.setStyleName("wg-sidepanel-linklist");

        {
            this.likeLink = new LinkLabel("");
            this.likeLink.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    boolean newLikeState;
                    if (SidePanel.this.likes.you) {
                        newLikeState = false;
                    } else {
                        newLikeState = true;
                    }

                    RequestsBuilder request = new RequestsBuilder() {
                        @Override
                        public void onResponse(String content) {
                            SidePanel.this.setInfo(content);
                        }
                    };
                    request.setPictureLike(SidePanel.this.desc, newLikeState);
                }
            });
            linkList.add(this.likeLink);

            /*
             * Label separator = new Label("-"); linkList.add(separator);
             * 
             * LinkLabel link = new LinkLabel("identify people");
             * linkList.add(link);
             */
        }

        FlowPanel sep = new FlowPanel();
        sep.setStyleName("wg-sidepanel-delimiter");
        this.flowPanel.add(sep);

        this.likes = new LikePanel();
        this.flowPanel.add(this.likes);
        this.likes.getElement().getStyle().setMarginTop(4, Unit.PX);

        this.commentsBox = new FlowPanel();
        this.flowPanel.add(this.commentsBox);

        this.commentEntry = new ExpandingTextArea(this.width);
        this.add(this.commentEntry);
        this.setWidgetBottomHeight(this.commentEntry, 5, Unit.PX,
                this.commentEntry.getHeight(), Unit.PX);
        this.setWidgetLeftRight(this.commentEntry, PADDING, Unit.PX, PADDING,
                Unit.PX);
        this.commentEntry.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                int h = commentEntry.getHeight();
                SidePanel.this.setWidgetBottomHeight(
                        commentEntry, PADDING, Unit.PX, h, Unit.PX);
                SidePanel.this.setWidgetTopBottom(
                        scroll, 0, Unit.PX, h + 2 * PADDING, Unit.PX);
            }
        });
        this.commentEntry.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                GWT.log("onKeyDown");
                int keyCode = event.getNativeKeyCode();

                if (event.isShiftKeyDown() || keyCode != KeyCodes.KEY_ENTER) {
                    return;
                }
                event.preventDefault();
                event.stopPropagation();

                ExpandingTextArea entry = (ExpandingTextArea) event.getSource();
                String text = entry.getText();

                if (text.isEmpty()) {
                    return;
                }
                ;

                RequestsBuilder request = new RequestsBuilder() {
                    @Override
                    public void onResponse(String content) {
                        SidePanel.this.setInfo(content);
                    }
                };
                request.postPictureComment(SidePanel.this.desc, text);
                entry.reset();
            }
        });
    }

    public void reset() {
        this.desc = null;
        this.titleLabel.setText("");
        this.countLabel.setText("");
        this.folderLabel.setText("");
        this.exifBox.reset();
        this.exifBox.setVisible(false);
        this.likeLink.setText("");
        this.likes.reset();
        this.commentsBox.setVisible(false);
        this.commentsBox.clear();
        this.commentEntry.reset();
    }

    public void setDesc(PictureDescriptor desc) {
        this.desc = desc;
        this.titleLabel.setText(desc.getName());
        this.folderLabel.setText(desc.getDirPath());
        this.exifBox.setVisible(true);
        this.exifBox.setData(desc.getExif());
        this.commentsBox.clear();
    }

    public void setCount(int count, int total) {
        this.countLabel.setText(count + " of " + total);
    }

    public void setInfo(String content) {
        Document doc = XMLParser.parse(content);
        Element root = doc.getDocumentElement();
        NodeList likesList = root.getElementsByTagName("likes");
        if (likesList.getLength() == 1) {
            Element likes = (Element) likesList.item(0);
            boolean me = "true".equals(likes.getAttribute("includes_me"));
            if (!me) {
                this.likeLink.setText("I like it");
            } else {
                this.likeLink.setText("I don't like it");
            }
            this.likes.update(me, likes.getElementsByTagName("people"));
        }

        this.commentsBox.clear();
        this.commentsBox.setVisible(true);
        Element comments =
                (Element) root.getElementsByTagName("comments").item(0);
        NodeList commentList = comments.getElementsByTagName("comment");
        for (int j = 0; j < commentList.getLength(); ++j) {
            Element elt = (Element) commentList.item(j);
            String user = elt.getAttribute("author");
            String date = elt.getAttribute("date");
            String cnt;
            if (elt.getFirstChild() != null) {
                cnt = elt.getFirstChild().getNodeValue();
            } else {
                cnt = "<empty>";
            }
            this.commentsBox.add(new Comment(user, date, cnt));
        }

        ValueChangeEvent.fire(this, true);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<Boolean> handler) {
        return this.addHandler(handler, ValueChangeEvent.getType());
    }
}
