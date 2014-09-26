package com.lambourg.webgallery.shared;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

public class PictureDescriptor {
    public class PictureSizeDescriptor {
        private String name;
        private int width;
        private int height;
        private String url;
        
        public PictureSizeDescriptor(Element elt) {
            this.name = elt.getAttribute("name");
            this.width = Integer.valueOf(elt.getAttribute("width"));
            this.height = Integer.valueOf(elt.getAttribute("height"));
            this.url = elt.getAttribute("url");
            
        }
        
        public String getName() { return this.name; }
        public int getWidth() { return this.width; }
        public int getHeight() { return this.height; }
        public String getUrl() { return this.url; }
    }

    private String thumbUrl;
    private boolean isVideo;
    private String pictureId;
    private String dirId;
    private String dirName;
    private String folder;
    private String name;
    private ArrayList<PictureSizeDescriptor> sizes;
    private LinkedHashMap<String, String> exifData;
    
    private PictureDescriptor(Element elt, String dirId, String dirName, boolean isVideo) {
        GWT.log(elt.toString());
        this.isVideo = isVideo;
        this.thumbUrl = elt.getAttribute("thumb");
        this.pictureId = elt.getAttribute("fileid");
        this.name = elt.getAttribute("name");
        this.folder = elt.getAttribute("folder");
        this.dirId = dirId;
        this.dirName = dirName;
        
        this.sizes = new ArrayList<PictureSizeDescriptor>();
        NodeList children = elt.getElementsByTagName("size");
        for (int j = 0; j < children.getLength(); j++) {
            Element child = (Element)children.item(j);
            this.sizes.add(new PictureSizeDescriptor(child));
        }

        this.exifData = new LinkedHashMap<String, String>();
        children = elt.getElementsByTagName("exif");
        for (int j = 0; j < children.getLength(); j++) {
            Element child = (Element)children.item(j);
            String key = child.getAttribute("key");
            String val = child.getAttribute("value");
            this.exifData.put(key, val);
        }
    }
    
    public SafeUri getThumbnailUrl() {
        return UriUtils.fromTrustedString(this.thumbUrl);
    }
    
    public PictureSizeDescriptor getSizeDescriptor(int width, int height) {
        for (PictureSizeDescriptor size : this.sizes) {
            if (width <= size.getWidth() || height <= size.getHeight()) {
                if (this.isPicture()) {
                    return size;
                }
            }
        }
        return this.sizes.get(this.sizes.size() - 1);
    }
    
    public boolean isPicture() {
        return !this.isVideo;
    }
    
    public boolean isVideo() {
        return this.isVideo;
    }
    
    public String getName() {
        return this.name;
    }

    public String getPictureId() {
        return this.pictureId;
    }
    
    public String getDirId() {
        return this.dirId;
    }
    
    public String getDirName() {
        return this.dirName;
    }
    
    public String getDirPath() {
        return this.folder;
    }
    
    public LinkedHashMap<String, String> getExif() {
        return this.exifData;
    }
    
    public static List<PictureDescriptor> bind(Document xmldoc) {
        ArrayList<PictureDescriptor> ret = new ArrayList<PictureDescriptor> ();
        Element root = xmldoc.getDocumentElement();
        NodeList list = root.getChildNodes();
        String dirId = root.getAttribute("dirid");
        String dirName = root.getAttribute("dirname");
        
        for (int j = 0; j < list.getLength(); j++) {
            Node n = list.item(j);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element elt = (Element)n;
            
            PictureDescriptor thumb =
                    new PictureDescriptor((Element) list.item(j), dirId, dirName,
                            elt.getTagName().equals("video"));
            ret.add(thumb);
        }
        
        return ret;
    }
}
