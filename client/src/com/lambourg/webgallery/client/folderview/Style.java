package com.lambourg.webgallery.client.folderview;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class Style {
    public static final int titleIconSize = 16;
    
    public static final int NAV_WIDTH = 180;
    public static final int NAV_ARROW_SIZE = 8;
    
    public static final int Z_INDEX_TITLEBAR = 20;
    public static final int Z_INDEX_NAV = 30;
    public static final int Z_INDEX_CENTRAL = 10;
    public static final int Z_INDEX_FULLSCREEN = 40;

    public final native static float pixelRatio() /*-{
        if ($wnd.devicePixelRatio)
            return $wnd.devicePixelRatio
        else
            return 1.0;
    }-*/;

    public static int toPixelRatio(int val) {
        return (int) (val * Style.pixelRatio());
    }
    
    public native static boolean fullscreenEnabled() /*-{
        return $doc.fullscreenEnabled || $doc.webkitFullscreenEnabled
            || $doc.mozFullScreenEnabled || $doc.msFullscreenEnabled;
    }-*/;

    private native static void requestFullscreen(Element elt) /*-{
        if (elt.requestFullscreen) {
            elt.requestFullscreen();
        } else if (elt.webkitRequestFullscreen) {
            elt.webkitRequestFullscreen();
        } else if (elt.mozRequestFullScreen) {
            elt.mozRequestFullScreen();
        } else if (elt.msRequestFullscreen) {
            elt.msRequestFullscreen();
        }
    }-*/;
    
    public static void requestFullscreen() {
        Style.requestFullscreen(RootLayoutPanel.get().getElement());
    }

    public native static void exitFullscreen() /*-{
        if ($doc.exitFullscreen) {
            $doc.exitFullscreen();
        } else if ($doc.webkitExitFullscreen) {
            $doc.webkitExitFullscreen();
        } else if (elt.mozCancelFullScreen) {
            $doc.mozCancelFullScreen();
        } else if (elt.msExitFullscreen) {
            $doc.msExitFullscreen();
        }
    }-*/;
    
    public native static boolean isFullscreen() /*-{
        var elem = $doc.fullscreenElement || $doc.mozFullScreenElement || $doc.webkitFullscreenElement || $doc.msFullscreenElement;
        return elem != null;
    }-*/;

}
