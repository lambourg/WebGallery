package com.lambourg.webgallery.client.folderview;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;

public class Welcome extends ScrollPanel {
    private FlowPanel panel;

    public Welcome() {
        super();

        SimplePanel container = new SimplePanel();
        container.setStyleName("wg-Intro");
        this.add(container);

        this.panel = new FlowPanel();
        this.panel.setStyleName("wg-Intro-bg");
        container.add(this.panel);

        this.addTitle("Welcome to the AdaCore pictures web site!");
        this.addImage("static/images/group.jpg");
        this.addSubTitle("Presentation:");
        this.addText("This internal website aims at compiling in one place " +
                "all the pictures that were gathered during the various " +
                "events people at AdaCore were involved in, and where " +
                "pictures were taken. 'Events' means of course annual events " +
                "such as the summer gathering or the christmas dinners, but " +
                "also any other external or internal event.");
        this.addText("To do so, we only rely on the contributions of people " +
                "who participated to those events. If you don't send your " +
                "pictures, they won't be here. Please contribute! (see " +
                "below to know how to submit your pictures).");
        this.addSubTitle("Instructions:");
        this.addText("In order to view the pictures, you need to select a directory using the navigation box, on the left side of this page. Once the directory is selected, the thumbnails of the pictures available in this directory are displayed. A click on any thumbnail will show the picture.");
        this.addSubTitle("How to submit new pictures:");
        this.addText("Please go to <a href=\"upload.html\">this page</a> and follow the instructions. In case of trouble, please contact <a href=\"mailto:lambourg@adacore.com\">lambourg@adacore.com</a>");
        this.addTitle("Have a good visit!");
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
