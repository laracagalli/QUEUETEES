package gui.components;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.*;
import service.*;

public final class ReturnsPanel extends JPanel {
    private final User user;
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"ID","Control number","Item / size","Qty","Date","Status","Expected refund","Refund payment"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model);
    private List<ReturnRequest> rows=List.of();
    private final JLabel summary=new JLabel();
    private final java.util.Map<String,JButton> actionsByStatus=new java.util.HashMap<>();
    private final Timer timer=new Timer(2000,e->{if(isShowing())refresh();});
    private String lastState="";
    private final JTextArea nextStep=new JTextArea(3,40);
    private final JButton orderRefunds=ActionButtons.create("Cancelled / lost-order refunds",ActionButtons.NEUTRAL);
    @Override public void addNotify(){super.addNotify();timer.start();}
    @Override public void removeNotify(){timer.stop();super.removeNotify();}
    public ReturnsPanel(User user){
        this.user=user;setOpaque(false);setLayout(new BorderLayout(0,14));
        if(user==null||user.getStatus()!=AccountStatus.ACTIVE){add(new JLabel("An active account is required to review returns."));return;}
        JPanel header=new JPanel(new GridLayout(3,1,0,6));header.setOpaque(false);JLabel title=new JLabel("Order Returns / Refunds");title.setFont(new Font("Segoe UI",Font.BOLD,25));header.add(title);header.add(new JLabel("Return requests must be submitted within 30 days of recorded delivery."));header.add(summary);add(header,BorderLayout.NORTH);
        table.setRowHeight(48);table.setAutoCreateRowSorter(true);table.setFont(new Font("Segoe UI",Font.PLAIN,13));
        table.getColumnModel().getColumn(0).setMinWidth(70);table.getColumnModel().getColumn(0).setMaxWidth(75);table.getColumnModel().getColumn(3).setMaxWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(190);table.getColumnModel().getColumn(2).setPreferredWidth(230);table.getColumnModel().getColumn(5).setPreferredWidth(230);table.getColumnModel().getColumn(7).setMinWidth(170);table.getColumnModel().getColumn(7).setPreferredWidth(190);
        table.setDefaultRenderer(Object.class,new javax.swing.table.DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable t,Object v,boolean selected,boolean focus,int row,int column){super.getTableCellRendererComponent(t,v,selected,focus,row,column);setToolTipText(String.valueOf(v));return this;}});
        add(new ModernScrollPane(table));
        JPanel footer=new JPanel(new BorderLayout(0,6));footer.setOpaque(false);
        nextStep.setEditable(false);nextStep.setFocusable(false);nextStep.setOpaque(false);nextStep.setLineWrap(true);nextStep.setWrapStyleWord(true);nextStep.setFont(new Font("Segoe UI",Font.PLAIN,13));nextStep.setForeground(new Color(38,51,40));footer.add(nextStep,BorderLayout.NORTH);
        JPanel actions=new JPanel(new GridLayout(1,0,10,0));actions.setOpaque(false);
        JPanel workflow=new JPanel(new GridLayout(1,0,10,0));workflow.setOpaque(false);
        JButton refresh=ActionButtons.create("Refresh",ActionButtons.NEUTRAL),details=ActionButtons.create("View request / proof",ActionButtons.BLUE);refresh.addActionListener(e->refresh());details.addActionListener(e->details());actions.add(refresh);actions.add(details);orderRefunds.addActionListener(e->OrderRefundPanel.show(this,user));orderRefunds.setToolTipText("Only for cancelled orders or lost parcels. Returned-item refunds are processed in this table.");actions.add(orderRefunds);
        if(user.getRole()!=UserRole.CUSTOMER){for(String action:new String[]{"APPROVED","REJECTED","RECEIVED","REFUNDED"}){
            JButton button=ActionButtons.create(switch(action){case "APPROVED"->"Approve return";case "REJECTED"->"Reject return";case "RECEIVED"->"Confirm item returned";default->user.getRole()==UserRole.ADMIN?"Record refund payment":"Admin records refund";},switch(action){case "REJECTED"->ActionButtons.RED;case "REFUNDED"->ActionButtons.BLUE;default->ActionButtons.GREEN;});
            button.addActionListener(e->review(action));workflow.add(button);actionsByStatus.put(action,button);button.setEnabled(false);
            if(action.equals("REFUNDED"))button.setToolTipText("Available to administrators after staff confirm receipt of the returned items.");
        }}
        details.setEnabled(false);table.getSelectionModel().addListSelectionListener(e->{ReturnRequest selected=selected();details.setEnabled(selected!=null);updateActions();});
        JPanel buttonRows=new JPanel(new GridLayout(0,1,0,10));buttonRows.setOpaque(false);
        buttonRows.add(actions);
        if(workflow.getComponentCount()>0)buttonRows.add(workflow);
        buttonRows.setPreferredSize(new Dimension(700,workflow.getComponentCount()>0?90:40));
        JPanel actionArea=new JPanel(new BorderLayout(0,8));actionArea.setOpaque(false);
        if(user.getRole()!=UserRole.CUSTOMER)actionArea.add(createActionKey(),BorderLayout.NORTH);
        actionArea.add(buttonRows,BorderLayout.CENTER);
        footer.add(actionArea,BorderLayout.CENTER);add(footer,BorderLayout.SOUTH);refresh();
    }
    private JPanel createActionKey(){
        JPanel key=new JPanel(new WrappingFlowLayout());key.setOpaque(false);
        key.setName("return.action.legend");
        String[] meanings={"Approve / confirm","Reject","Details / payment","Other actions","Unavailable"};
        Color[] colors={ActionButtons.GREEN,ActionButtons.RED,ActionButtons.BLUE,ActionButtons.NEUTRAL,new Color(218,224,217)};
        for(int i=0;i<meanings.length;i++){
            Color color=colors[i];
            JLabel label=new JLabel(meanings[i],new Icon(){
                public int getIconWidth(){return 12;}
                public int getIconHeight(){return 12;}
                public void paintIcon(Component c,Graphics graphics,int x,int y){
                    Graphics2D g=(Graphics2D)graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(color);g.fillOval(x+1,y+1,10,10);g.dispose();
                }
            },SwingConstants.LEFT);
            label.setFont(new Font("Segoe UI",Font.PLAIN,12));
            label.setForeground(new Color(60,70,65));
            label.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
            key.add(label);
        }
        return key;
    }
    private void updateActions(){ReturnRequest r=selected();
        actionsByStatus.forEach((status,button)->button.setEnabled(r!=null&&(r.status().equals("SUBMITTED")&&(status.equals("APPROVED")||status.equals("REJECTED"))||r.status().equals("APPROVED")&&status.equals("RECEIVED")||r.status().equals("RECEIVED")&&status.equals("REFUNDED")&&user.getRole()==UserRole.ADMIN)));
        String message=r==null?"Select a return request to see the next action. Unavailable actions stay disabled until the request reaches the required stage.":"Request #"+r.id()+" — "+ReturnPresentation.nextStep(r);
        if(r!=null&&r.status().equals("RECEIVED"))message=user.getRole()==UserRole.ADMIN?"Request #"+r.id()+": returned items received. Select Record refund payment, enter the payment reference and confirmation note, then confirm. The amount and status will update to Refunded.":"Request #"+r.id()+": staff work is complete. An ADMIN must sign in → Returns → select this request → Record refund payment. The refund stays pending until the admin records it.";
        nextStep.setText(message);
    }
    public void refresh(){
        if(user==null)return;
        List<ReturnRequest> updated=ReturnsService.requests(user);
        List<RefundRecord> orderRefundRecords=RefundService.records(user);
        String state=orderRefundRecords.toString()+updated.stream().map(r->r.id()+":"+r.status()+":"+r.resolution()+":"+r.refundReference()+":"+r.refundAmount()).collect(java.util.stream.Collectors.joining("|"));
        if(state.equals(lastState)&&!lastState.isEmpty())return;
        lastState=state;ReturnRequest selected=selected();rows=updated;model.setRowCount(0);
        List<Order> orders=user.getRole()==UserRole.CUSTOMER?StoreService.getInstance().getOrdersForCustomer(user.getId()):StoreService.getInstance().getOrders();
        for(ReturnRequest r:rows){Order order=orders.stream().filter(o->o.getId()==r.orderId()).findFirst().orElseThrow();String item=order.getItems().stream().filter(i->i.getProduct().getId()==r.productId()).findFirst().orElseThrow().getProduct().getDisplayName();
            model.addRow(new Object[]{r.id(),order.getControlNumber(),item,r.quantity(),r.requestedAt().toLocalDate(),ReturnPresentation.status(r),r.status().equals("REJECTED")?"Not approved":String.format("₱%,.2f",ReturnPresentation.expected(order,r)),ReturnPresentation.refundLabel(r)});
        }
        long pending=rows.stream().filter(r->r.status().equals("SUBMITTED")||r.status().equals("APPROVED")||r.status().equals("RECEIVED")).count();
        long orderPending=orderRefundRecords.stream().filter(r->r.source().startsWith("ORDER:")&&r.status().equals("PENDING")).count();
        orderRefunds.setText("Cancelled / lost-order refunds ("+orderRefundRecords.stream().filter(r->r.source().startsWith("ORDER:")).count()+")");
        summary.setText(user.getRole()==UserRole.CUSTOMER?orderPending+" order refunds pending; "+pending+" requests in progress. Select a request to see instructions, review notes and refund payment details.":orderPending+" order refunds pending; "+pending+" requests need action. Approve → receive returned items → admin records refund payment.");
        if(selected!=null)for(int i=0;i<rows.size();i++)if(rows.get(i).id()==selected.id()){TablePagination.showModelRow(table,i);int view=table.convertRowIndexToView(i);if(view>=0)table.setRowSelectionInterval(view,view);break;}
        updateActions();
    }
    private ReturnRequest selected(){return table.getSelectedRow()<0?null:rows.get(table.convertRowIndexToModel(table.getSelectedRow()));}
    private void details(){ReturnRequest r=selected();if(r==null)return;
        Order order=StoreService.getInstance().getOrders().stream().filter(o->o.getId()==r.orderId()).findFirst().orElseThrow();
        JPanel content=ReturnRequestDetails.create(order,r);
        JOptionPane.showMessageDialog(this,content,"Return request #"+r.id(),JOptionPane.PLAIN_MESSAGE);
    }
    private void review(String action){ReturnRequest r=selected();if(r==null)return;
        JTextArea note=new JTextArea(4,32);JTextField reference=new JTextField();JPanel form=new JPanel(new GridLayout(0,1,0,6));form.add(new JLabel(action.equals("APPROVED")?"Instructions: where/how to send the items back":action.equals("REFUNDED")?"Payment method / recipient and refund confirmation note":"Review / resolution note"));form.add(new ModernScrollPane(note));
        if(action.equals("REFUNDED")){Order order=StoreService.getInstance().getOrders().stream().filter(o->o.getId()==r.orderId()).findFirst().orElseThrow();form.add(new JLabel("Refund amount: "+String.format("₱%,.2f",ReturnPresentation.expected(order,r))+" — record only, no money transfer"));form.add(new JLabel("Reference for the refund already paid / simulated for demo"));form.add(reference);}
        if(JOptionPane.showConfirmDialog(this,form,"Confirm "+action.toLowerCase(),JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{ReturnsService.review(user,r.id(),action,note.getText(),reference.getText());refresh();}catch(IllegalArgumentException|IllegalStateException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Return review",JOptionPane.WARNING_MESSAGE);}
    }
    public static void submit(Component parent,User user,Order order,Runnable refresh){
        List<ReturnRequest> existing=ReturnsService.requests(user);java.util.Map<Integer,Integer> available=new java.util.HashMap<>();
        for(CartItem purchased:order.getItems()){int reserved=existing.stream().filter(r->r.orderId()==order.getId()&&r.productId()==purchased.getProduct().getId()&&!r.status().equals("REJECTED")).mapToInt(ReturnRequest::quantity).sum();available.put(purchased.getProduct().getId(),purchased.getQuantity()-reserved);}
        List<CartItem> eligible=order.getItems().stream().filter(i->available.get(i.getProduct().getId())>0).toList();
        if(eligible.isEmpty()){JOptionPane.showMessageDialog(parent,"All purchased quantities already have a return request. View Returns for their progress.");return;}
        JComboBox<CartItem> item=new RoundedComboBox<>(eligible.toArray(new CartItem[0]));item.setRenderer((list,value,index,selected,focus)->new JLabel(value==null?"":value.getProduct().getDisplayName()+" / "+available.get(value.getProduct().getId())+" available to return"));
        JSpinner quantity=new JSpinner(new SpinnerNumberModel(1,1,available.get(eligible.get(0).getProduct().getId()).intValue(),1));item.addActionListener(e->quantity.setModel(new SpinnerNumberModel(1,1,available.get(((CartItem)item.getSelectedItem()).getProduct().getId()).intValue(),1)));
        JTextArea reason=new JTextArea(4,35);reason.setLineWrap(true);reason.setWrapStyleWord(true);JLabel proofLabel=new JLabel("No proof attached");byte[][] proof={null};String[] filename={null};
        JButton attach=new JButton("Attach JPEG / PNG proof (up to 5 MB)");attach.addActionListener(e->{JFileChooser chooser=new JFileChooser();chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Proof images","png","jpg","jpeg"));if(chooser.showOpenDialog(parent)!=JFileChooser.APPROVE_OPTION)return;
            try{byte[] bytes=ReturnsService.proof(chooser.getSelectedFile().toPath());proof[0]=bytes;filename[0]=chooser.getSelectedFile().getName();proofLabel.setText(filename[0]);}catch(Exception ex){JOptionPane.showMessageDialog(parent,ex.getMessage(),"Invalid proof",JOptionPane.WARNING_MESSAGE);}
        });
        JPanel form=new JPanel(new GridLayout(0,1,0,6));form.add(new JLabel("Item / size"));form.add(item);form.add(new JLabel("Quantity to return"));form.add(quantity);form.add(new JLabel("Reason (10–1000 characters)"));form.add(new ModernScrollPane(reason));form.add(attach);form.add(proofLabel);
        while(JOptionPane.showConfirmDialog(parent,form,"Return request for "+order.getControlNumber(),JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
            try{quantity.commitEdit();ReturnsService.submit(user,order.getId(),((CartItem)item.getSelectedItem()).getProduct().getId(),(Integer)quantity.getValue(),reason.getText(),proof[0],filename[0]);refresh.run();JOptionPane.showMessageDialog(parent,"Request submitted for staff review.");return;}
            catch(Exception ex){JOptionPane.showMessageDialog(parent,ex.getMessage(),"Unable to submit return",JOptionPane.WARNING_MESSAGE);}
        }
    }
}
