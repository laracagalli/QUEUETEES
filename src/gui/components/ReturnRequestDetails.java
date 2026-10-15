package gui.components;

import java.awt.*;
import javax.swing.*;
import model.*;

public final class ReturnRequestDetails {
    private ReturnRequestDetails(){}
    private static String money(double value){return String.format("₱%,.2f",value);}
    public static JPanel create(Order order,ReturnRequest request){
        JPanel root=new JPanel(new BorderLayout(0,16));root.setBackground(AppTheme.PAPER);
        root.add(DetailCards.heading("Return request #"+request.id(),ReturnPresentation.status(request)),BorderLayout.NORTH);
        JPanel body=DetailCards.column();
        JPanel record=DetailCards.card("Request details");
        String item=order.getItems().stream().filter(i->i.getProduct().getId()==request.productId()).findFirst().orElseThrow().getProduct().getDisplayName();
        record.add(DetailCards.grid(DetailCards.field("Order",order.getControlNumber()),
                DetailCards.field("Requested",request.requestedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a"))),
                DetailCards.field("Item / size",item),DetailCards.field("Quantity",String.valueOf(request.quantity()))));
        DetailCards.add(body,record);
        JPanel expected=DetailCards.card("Expected refund");expected.add(DetailCards.label(money(ReturnPresentation.expected(order,request)),23,true));
        expected.add(DetailCards.label("Purchased price × quantity; subject to review",11,false),BorderLayout.SOUTH);
        JPanel paid=DetailCards.card("Refund payment");paid.add(DetailCards.label(request.status().equals("REFUNDED")?money(request.refundAmount()):"Not issued",23,true));
        paid.add(DetailCards.text(request.refundReference().isBlank()?"No payment reference yet":request.refundReference(),1),BorderLayout.SOUTH);
        DetailCards.add(body,DetailCards.grid(expected,paid));
        JPanel next=DetailCards.note("What happens next",ReturnPresentation.nextStep(request));next.setBackground(new Color(236,242,230));DetailCards.add(body,next);
        JPanel notes=DetailCards.column();
        DetailCards.add(notes,DetailCards.note("Customer's reason",request.reason()));
        DetailCards.add(notes,DetailCards.note("Review notes & return instructions",reviewText(request.resolution())));
        JPanel proof=DetailCards.card("Submitted proof");
        ImageIcon original=new ImageIcon(request.proof());
        if(original.getIconWidth()>0&&original.getIconHeight()>0){
            double scale=Math.min(1,Math.min(300.0/original.getIconWidth(),240.0/original.getIconHeight()));
            JLabel image=new JLabel(new ImageIcon(original.getImage().getScaledInstance(Math.max(1,(int)(original.getIconWidth()*scale)),Math.max(1,(int)(original.getIconHeight()*scale)),Image.SCALE_SMOOTH)));
            image.setHorizontalAlignment(SwingConstants.CENTER);proof.add(image);
        }else proof.add(DetailCards.text("Proof preview is unavailable.",2));
        proof.add(DetailCards.text(request.proofName(),1),BorderLayout.SOUTH);
        DetailCards.add(body,DetailCards.grid(notes,proof));
        root.add(DetailCards.scroll(body,820,540));root.setPreferredSize(new Dimension(840,620));return root;
    }
    private static String reviewText(String value){
        if(value==null||value.isBlank())return "Awaiting staff review.";
        var matcher=java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2}T[0-9:.]+) / ([A-Z]+):").matcher(value);
        StringBuffer formatted=new StringBuffer();
        while(matcher.find()){
            String heading=matcher.group();
            try{heading=java.time.LocalDateTime.parse(matcher.group(1)).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a"))+" • "+matcher.group(2).toLowerCase(java.util.Locale.ROOT)+"\n";}catch(java.time.format.DateTimeParseException ignored){}
            matcher.appendReplacement(formatted,java.util.regex.Matcher.quoteReplacement(heading));
        }
        matcher.appendTail(formatted);return formatted.toString();
    }
}
