package com.lambourg.webgallery.client.folderview;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import com.lambourg.webgallery.client.rpc.RequestsBuilder;

public class Welcome extends ScrollPanel {
    private FlowPanel panel;

    public Welcome() {
        super();

        new RequestsBuilder() {
            @Override
            public void onResponse(String content) {
                Document xml = XMLParser.parse(content);
                Welcome.this.bind(xml);
            }
        }.getWelcomeMessage();
    }
    
    public void bind(Document xmldoc) {
        Element root = xmldoc.getDocumentElement();
        NodeList children = root.getChildNodes();

        this.clear();
        this.panel = new FlowPanel();
        this.panel.setStyleName("wg-Intro");
        this.add(this.panel);
        
        for (int j = 0; j < children.getLength(); ++j) {
            Node n = children.item(j);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element elt = (Element)n;
            String tag = elt.getNodeName();
            
            if ("title".equals(tag)) {
                this.addTitle(elt.getFirstChild().getNodeValue());
            } else if ("image".equals(tag)) {
                this.addImage(elt.getFirstChild().getNodeValue());
            } else if ("subtitle".equals(tag)) {
                this.addSubTitle(elt.getFirstChild().getNodeValue());
            } else if ("paragraph".equals(tag)) {
                this.addText(elt.getFirstChild().getNodeValue());
            }
        }
        
    }

    public void addTitle(String title) {
        Label label = new Label(title);
        label.setStyleName("wg-Intro-title");
        this.panel.add(label);
    }

    public void addSubTitle(String title) {
        Label label = new Label(title);
        label.setStyleName("wg-Intro-smalltitle");
        this.panel.add(label);
    }

    public void addImage(String url) {
        SimplePanel panel = new SimplePanel();
        Image img = new Image(url);

        panel.setStyleName("wg-Intro-img");
        panel.add(img);
        this.panel.add(panel);
    }
    
    public void addText(String text) {
        HTML label = new HTML(text);
        
        label.setStyleName("wg-Intro-paragraph");
        this.panel.add(label);
    }
}
