package gui.components;
import java.awt.*;
import javax.swing.*;
import model.*;
import service.StoreService;
public final class PaymentReview {
    private PaymentReview(){}
    static JPanel form(JTextField amount,JTextField reference){
        JPanel form=new JPanel(new GridBagLayout());
        GridBagConstraints row=new GridBagConstraints();row.gridx=0;row.gridy=0;row.weightx=1;row.fill=GridBagConstraints.HORIZONTAL;
        row.insets=new Insets(0,0,10,0);
        amount.setColumns(26);reference.setColumns(26);
        for(Component component:new Component[]{
                instruction("Check the actual wallet/bank payment before confirming."),
                instruction("This records verified payment; it does not charge a card."),
                new JLabel("Amount received (PHP)"),amount,
                new JLabel("Actual payment transaction reference"),reference}){
            form.add(component,row);row.gridy++;
        }
        AppTheme.apply(form);return form;
    }
    private static JTextArea instruction(String value){
        JTextArea text=new JTextArea(value,2,32);text.setLineWrap(true);text.setWrapStyleWord(true);
        text.setEditable(false);text.setOpaque(false);return text;
    }
    public static void show(Component parent,Order order,User actor,Runnable refresh){
        JTextField amount=new JTextField(String.format(java.util.Locale.ENGLISH,"%.2f",order.getTotal())),reference=new JTextField(order.getCheckoutDetails().getSubmittedPaymentReference());
        reference.setToolTipText("Entered by the customer. Check the actual payment and correct this reference if needed.");
        JPanel form=form(amount,reference);
        if(JOptionPane.showConfirmDialog(parent,form,"Verify payment • "+order.getControlNumber(),JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE)!=JOptionPane.OK_OPTION)return;
        try{StoreService.getInstance().verifyPayment(order.getId(),Double.parseDouble(amount.getText().trim()),reference.getText(),actor);refresh.run();JOptionPane.showMessageDialog(parent,"Payment verified. The order can enter the packing queue.");}
        catch(RuntimeException ex){JOptionPane.showMessageDialog(parent,ex.getMessage(),"Unable to verify payment",JOptionPane.WARNING_MESSAGE);}
    }
}
