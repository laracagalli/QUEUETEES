package gui.components;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Small vector icons that follow each sidebar button's foreground color. */
public final class NavigationIcon implements Icon {
    private final String page;
    public NavigationIcon(String page){this.page=page;}
    public int getIconWidth(){return 18;}
    public int getIconHeight(){return 18;}
    public void paintIcon(Component component,Graphics graphics,int x,int y){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(component.getForeground());g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch(page){
            case "overview" -> {for(int row=0;row<2;row++)for(int column=0;column<2;column++)g.drawRoundRect(2+column*8,2+row*8,5,5,1,1);}
            case "queue" -> {for(int row=0;row<3;row++){g.fillOval(1,3+row*5,2,2);g.drawLine(6,4+row*5,16,4+row*5);}}
            case "orders" -> {g.drawRoundRect(3,3,12,14,2,2);g.drawRoundRect(6,1,6,4,1,1);g.drawLine(6,9,12,9);g.drawLine(6,13,11,13);}
            case "completed" -> {g.drawOval(1,1,16,16);g.drawLine(5,9,8,12);g.drawLine(8,12,13,6);}
            case "status", "tracking" -> {Path2D pin=new Path2D.Double();pin.moveTo(9,17);pin.curveTo(6,13,2,9,3,6);pin.curveTo(4,-1,14,-1,15,6);pin.curveTo(16,9,12,13,9,17);g.draw(pin);g.drawOval(7,4,4,4);}
            case "shop" -> {g.drawRect(3,7,12,10);g.drawRect(6,11,5,6);g.drawLine(2,7,4,2);g.drawLine(4,2,14,2);g.drawLine(14,2,16,7);g.drawLine(2,7,16,7);}
            case "cart" -> {g.drawLine(1,2,4,2);g.drawLine(4,2,6,12);g.drawLine(6,12,15,12);g.drawLine(5,5,17,5);g.drawLine(17,5,15,10);g.fillOval(6,15,2,2);g.fillOval(13,15,2,2);}
            case "history", "activity" -> {g.drawOval(1,1,16,16);g.drawLine(9,4,9,9);g.drawLine(9,9,13,11);}
            case "products" -> {g.drawRect(2,6,14,11);g.drawLine(2,6,9,2);g.drawLine(9,2,16,6);g.drawLine(9,6,9,11);}
            case "customers" -> {g.drawOval(2,2,5,5);g.drawOval(11,2,5,5);g.draw(new Arc2D.Double(0,9,9,12,0,180,Arc2D.OPEN));g.draw(new Arc2D.Double(9,9,9,12,0,180,Arc2D.OPEN));}
            case "staff" -> {g.drawOval(3,1,6,6);g.draw(new Arc2D.Double(0,9,12,14,0,180,Arc2D.OPEN));g.drawLine(11,10,13,12);g.drawLine(13,12,17,7);}
            case "reports" -> {g.drawLine(2,1,2,17);g.drawLine(2,17,17,17);g.drawRect(5,10,2,7);g.drawRect(10,6,2,11);g.drawRect(15,2,2,15);}
            case "returns" -> {g.drawRect(3,8,12,9);g.drawLine(3,8,9,5);g.drawLine(9,5,15,8);g.drawLine(9,8,9,13);g.drawLine(12,2,4,2);g.drawLine(4,2,6,0);g.drawLine(4,2,6,4);}
            case "profile" -> {g.drawOval(6,1,6,6);g.draw(new Arc2D.Double(2,9,14,14,0,180,Arc2D.OPEN));g.drawLine(2,16,16,16);}
            default -> g.drawOval(2,2,14,14);
        }
        g.dispose();
    }
}
