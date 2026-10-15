package gui.components;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import model.*;

public final class PaymentDialogSizingTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static java.util.List<Component> children(Container parent){
        java.util.List<Component> all=new java.util.ArrayList<>();for(Component child:parent.getComponents()){all.add(child);if(child instanceof Container c)all.addAll(children(c));}return all;
    }
    static void fits(JComponent child,Container root){
        Rectangle bounds=SwingUtilities.convertRectangle(child.getParent(),child.getBounds(),root);
        check(child.getWidth()>0&&child.getHeight()>0&&new Rectangle(0,0,root.getWidth(),root.getHeight()).contains(bounds),"Clipped control: "+child.getClass().getSimpleName());
    }
    static void render(JDialog dialog,String filename)throws Exception {
        Container content=dialog.getContentPane();BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();content.printAll(g);g.dispose();new java.io.File("bin/review").mkdirs();
        javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/"+filename));
    }
    public static void main(String[] args)throws Exception {
        JDialog[] dialog={null};JTextField amount=new JTextField("699.00"),reference=new JTextField();
        try {
            SwingUtilities.invokeAndWait(()->{
                AppTheme.install();JOptionPane pane=new JOptionPane(PaymentReview.form(amount,reference),JOptionPane.QUESTION_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
                dialog[0]=pane.createDialog(null,"Verify payment sizing check");dialog[0].setModal(false);dialog[0].setVisible(true);
            });
            SwingUtilities.invokeAndWait(()->{try{
                JDialog window=dialog[0];fits(amount,window.getContentPane());fits(reference,window.getContentPane());
                check(reference.getHeight()>=30,"Reference field remains readable");
                long actions=children(window.getContentPane()).stream().filter(c->c instanceof JButton b && (b.getText().equals("OK")||b.getText().equals("Cancel"))).peek(c->fits((JComponent)c,window.getContentPane())).count();
                check(actions==2&&window.isResizable(),"Both dialog actions are visible and resizing is enabled");
                Rectangle screen=window.getGraphicsConfiguration().getBounds();check(screen.contains(window.getBounds()),"Payment dialog stays on screen");
                render(window,"payment-dialog-fixed.png");window.dispose();
                String city=DeliveryAddress.cities("Laguna")[0];
                CheckoutDetails details=CheckoutDetails.structured("Test Customer","test@example.com","09123456789","Delivery",
                        new DeliveryAddress("Laguna",city,DeliveryAddress.barangays("Laguna",city)[0],"123 Test Street"),"GCash","");
                Order order=new Order(1,1,1,"Test Customer",java.util.List.of(new CartItem(new Product(1,"Shirt","Men","Tops",699,1,""),1)),java.time.LocalDateTime.now(),details);
                JComboBox<OrderStatus> choice=new RoundedComboBox<>(OrderWorkflow.targets(order).toArray(OrderStatus[]::new));
                var method=Class.forName("gui.staff.StaffOrderActions").getDeclaredMethod("stageForm",Order.class,JComboBox.class);method.setAccessible(true);
                JPanel form=(JPanel)method.invoke(null,order,choice);
                check(children(form).stream().noneMatch(c->c instanceof JTextArea),"Stage dialog uses a compact combo box without the large text list");
                check(choice.getItemCount()==1&&choice.getItemAt(0)==OrderStatus.PREPARING,"Only next legal stage is selectable");
                JOptionPane pane=new JOptionPane(form,JOptionPane.QUESTION_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
                dialog[0]=pane.createDialog(null,"Update stage sizing check");dialog[0].setModal(false);dialog[0].setVisible(true);
            }catch(Exception ex){throw new RuntimeException(ex);} });
            SwingUtilities.invokeAndWait(()->{try{
                for(Component component:children(dialog[0].getContentPane()))if(component instanceof JComboBox<?> || component instanceof JButton b && ("OK".equals(b.getText())||"Cancel".equals(b.getText())))fits((JComponent)component,dialog[0].getContentPane());
                render(dialog[0],"stage-dialog-fixed.png");
            }catch(Exception ex){throw new RuntimeException(ex);} });
            System.out.println("PASS: payment fields and OK/Cancel fit after theming; compact stage combo retains strict next-stage selection");
        }finally{SwingUtilities.invokeAndWait(()->{if(dialog[0]!=null)dialog[0].dispose();});}
    }
}
