package gui.customer;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.User;
import service.SecurityIncidents;

/** Read-only message history. Selecting an unread message acknowledges just that message. */
public final class AdminInboxPanel extends JPanel {
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a",Locale.ENGLISH);
    private final User user;
    private final IntConsumer unreadChanged;
    private final DefaultListModel<SecurityIncidents.WarningMessage> model=new DefaultListModel<>();
    private final JList<SecurityIncidents.WarningMessage> messages=new JList<>(model);
    private final JTextArea body=new JTextArea();
    private final JLabel title=new JLabel("Your messages"),date=new JLabel(" "),status=new JLabel("Loading messages...");
    private final Set<Integer> opening=new HashSet<>();
    private boolean updating,loading;
    private final javax.swing.Timer timer=new javax.swing.Timer(5000,e->{if(isShowing())refreshMessages();});

    public AdminInboxPanel(User user,IntConsumer unreadChanged){
        super(new BorderLayout(0,14));this.user=user;this.unreadChanged=unreadChanged;
        setBackground(gui.components.AppTheme.PAPER);setPreferredSize(new Dimension(860,470));
        JPanel heading=new JPanel(new GridLayout(2,1,0,6));heading.setOpaque(false);
        JLabel label=new JLabel("Messages from admin");label.setFont(new Font("Segoe UI",Font.BOLD,22));heading.add(label);
        heading.add(new JLabel("Select a message to read it. Read messages are kept here."));add(heading,BorderLayout.NORTH);
        messages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);messages.setFixedCellHeight(96);
        messages.setBackground(Color.WHITE);messages.getAccessibleContext().setAccessibleName("Admin message history");
        messages.setCellRenderer(new DefaultListCellRenderer(){
            public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
                super.getListCellRendererComponent(list,value,index,selected,focus);var message=(SecurityIncidents.WarningMessage)value;
                String subject=message.read()?"Message from admin":"<b>Message from admin</b>";
                setText("<html>"+subject+"<br>"+DATE.format(message.sentAt())+"<br>"+(message.read()?"Read":"<b>Unread</b>")+"</html>");
                setFont(new Font("Segoe UI",Font.PLAIN,16));setForeground(gui.components.AppTheme.GREEN);setBorder(new EmptyBorder(12,14,12,14));
                setBackground(selected?new Color(219,232,209):Color.WHITE);return this;
            }
        });
        body.setEditable(false);body.setLineWrap(true);body.setWrapStyleWord(true);body.setFont(new Font("Segoe UI",Font.PLAIN,15));body.setBackground(Color.WHITE);body.setBorder(new EmptyBorder(16,16,16,16));
        body.getAccessibleContext().setAccessibleName("Selected admin message");title.setFont(new Font("Segoe UI",Font.BOLD,18));date.setFont(new Font("Segoe UI",Font.PLAIN,12));
        JPanel detailHeading=new JPanel(new GridLayout(2,1,0,6));detailHeading.setOpaque(false);detailHeading.add(title);detailHeading.add(date);
        JPanel detail=new JPanel(new BorderLayout(0,12));detail.setOpaque(false);detail.setBorder(new EmptyBorder(10,16,0,0));detail.add(detailHeading,BorderLayout.NORTH);detail.add(new JScrollPane(body));
        JPanel columns=new JPanel(new BorderLayout(0,0));columns.setOpaque(false);
        JScrollPane messageList=new JScrollPane(messages);messageList.setPreferredSize(new Dimension(280,0));
        columns.add(messageList,BorderLayout.WEST);columns.add(detail,BorderLayout.CENTER);add(columns);
        add(status,BorderLayout.SOUTH);
        messages.addListSelectionListener(e->{if(!e.getValueIsAdjusting() && !updating)openSelected();});
        refreshMessages();
    }
    @Override public void addNotify(){super.addNotify();timer.start();}
    @Override public void removeNotify(){timer.stop();super.removeNotify();}
    public void refreshMessages(){
        if(loading)return;loading=true;
        new SwingWorker<java.util.List<SecurityIncidents.WarningMessage>,Void>(){
            protected java.util.List<SecurityIncidents.WarningMessage> doInBackground(){return SecurityIncidents.warningMessages(user.getId());}
            protected void done(){try{display(get());}catch(Exception ex){status.setText("Unable to load messages. Retrying shortly.");}finally{loading=false;}}
        }.execute();
    }
    private void display(java.util.List<SecurityIncidents.WarningMessage> latest){
        var selected=messages.getSelectedValue();int id=selected==null?-1:selected.id();updating=true;
        try{model.clear();for(var message:latest)model.addElement(message);
            for(int i=0;i<model.size();i++)if(model.get(i).id()==id){messages.setSelectedIndex(i);showMessage(model.get(i));break;}
        }finally{updating=false;}
        int unread=(int)latest.stream().filter(m->!m.read()).count();unreadChanged.accept(unread);
        status.setText(latest.isEmpty()?"No messages from admin yet.":unread+" unread • "+latest.size()+" saved message(s)");
    }
    private void showMessage(SecurityIncidents.WarningMessage message){title.setText("Message from admin");date.setText(DATE.format(message.sentAt()));body.setText(message.message());body.setCaretPosition(0);}
    private void openSelected(){
        var selected=messages.getSelectedValue();if(selected==null)return;showMessage(selected);
        if(selected.read() || !opening.add(selected.id()))return;
        new SwingWorker<java.util.List<SecurityIncidents.WarningMessage>,Void>(){
            protected java.util.List<SecurityIncidents.WarningMessage> doInBackground(){SecurityIncidents.acknowledgeWarning(user.getId(),selected.id());return SecurityIncidents.warningMessages(user.getId());}
            protected void done(){try{display(get());}catch(Exception ex){status.setText("Unable to save read status. Select the message again to retry.");}finally{opening.remove(selected.id());}}
        }.execute();
    }
}
