package gui.components;
import javax.swing.*;
import java.awt.*;
public final class ProductImages {
    private ProductImages() { }
    public static ImageIcon load(String path,int width,int height) {
        if(path==null || path.isBlank() || path.equals("None"))return null;
        java.net.URL url=ProductImages.class.getResource(path.startsWith("/")?path:"/"+path);
        ImageIcon icon=url==null?new ImageIcon(path):new ImageIcon(url);
        if(icon.getIconWidth()<=0)return null;
        double scale=Math.min((double)width/icon.getIconWidth(),(double)height/icon.getIconHeight());
        return new ImageIcon(icon.getImage().getScaledInstance(Math.max(1,(int)(icon.getIconWidth()*scale)),Math.max(1,(int)(icon.getIconHeight()*scale)),Image.SCALE_SMOOTH));
    }
}
