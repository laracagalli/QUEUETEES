package gui.admin;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Administrator page for registered customer accounts. */
public final class CustomerManagementPanel extends JPanel {

    private final service.AuthService authService;
    private final model.User adminUser;
    private java.util.List<model.User> customers = new java.util.ArrayList<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"User", "Email", "Contact", "Joined", "Role", "Status"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel footer = AdminUi.label("", 11, false);
    private final JButton suspendBtn = new gui.components.RoundedButton("Suspend", new Color(180, 45, 35), Color.WHITE) {{
        setFont(new Font("Fira Code", Font.BOLD, 11));
        setHoverColor(new Color(155, 35, 25));
        setPreferredSize(new java.awt.Dimension(100, 40));
    }};
    private final JButton unsuspendBtn = new gui.components.RoundedButton("Unsuspend", new Color(29, 113, 75), Color.WHITE) {{
        setFont(new Font("Fira Code", Font.BOLD, 11));
        setHoverColor(new Color(22, 90, 58));
        setPreferredSize(new java.awt.Dimension(115, 40));
    }};
    private final JPanel metrics = new JPanel(new BorderLayout());
    private final JButton permanentBtn = AdminUi.button("Permanently suspend");
    private final Timer accountRefreshTimer=new Timer(5000,e->refreshIfChanged());
    private boolean refreshingAccounts;

    public CustomerManagementPanel(service.AuthService authService, model.User adminUser) {
        this.authService = authService;
        this.adminUser = adminUser;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Ui.addLeft(this, Ui.label("USERS", 10, Font.BOLD, Ui.FOREST));
        add(Box.createVerticalStrut(4));
        Ui.addLeft(this, Ui.label("User Management", 25, Font.BOLD, Ui.INK));
        add(Box.createVerticalStrut(5));
        Ui.addLeft(this, Ui.label("Review customer and staff access. Usage warnings require human review.", 11, Font.PLAIN, Ui.MUTED));
        add(Box.createVerticalStrut(18));

        metrics.setOpaque(false);
        metrics.setAlignmentX(0);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        add(metrics);
        add(Box.createVerticalStrut(16));

        AdminUi.style(table);
        add(AdminUi.filters(table, "All statuses", "ACTIVE", "UNVERIFIED", "SUSPENDED", "PENDING_APPROVAL", "REJECTED", "PERMANENTLY SUSPENDED"));
        add(Box.createVerticalStrut(16));

        JPanel card = AdminUi.tableCard(table, "Account records");

        suspendBtn.setEnabled(false);
        unsuspendBtn.setEnabled(false);
        suspendBtn.addActionListener(e -> toggleSuspend(true));
        unsuspendBtn.addActionListener(e -> toggleSuspend(false));

        JButton refresh = AdminUi.button("Refresh");
        refresh.addActionListener(e -> refresh());

        JPanel actions = new JPanel(new gui.components.WrappingFlowLayout());
        actions.setOpaque(false);
        actions.add(unsuspendBtn);
        actions.add(suspendBtn);
        permanentBtn.setEnabled(false);
        permanentBtn.addActionListener(e->permanentlySuspend());
        actions.add(permanentBtn);
        actions.add(refresh);
        JButton warnings=AdminUi.button("Review usage warnings");warnings.addActionListener(e->{gui.components.IncidentReview.show(this,adminUser);refreshIfChanged();});actions.add(warnings);
        JButton flag=AdminUi.button("Flag for review");flag.setEnabled(false);table.getSelectionModel().addListSelectionListener(e->flag.setEnabled(selected()!=null));
        flag.addActionListener(e->{model.User target=selected();if(target==null)return;String reason=JOptionPane.showInputDialog(this,"Describe the observed usage issue (10–1000 characters):");if(reason==null)return;
            if(reason.trim().length()<10||reason.length()>1000){JOptionPane.showMessageDialog(this,"Provide a specific observation (10–1000 characters).");return;}
            service.SecurityIncidents.flag(target,"Reported by "+adminUser.getUsername()+": "+reason.trim());service.ActivityLogger.record(adminUser,"Usage flagged for review",target.getUsername()+": "+reason.trim());JOptionPane.showMessageDialog(this,"Observation saved for review. Account status is unchanged.");
        });actions.add(flag);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bottom.add(footer, BorderLayout.NORTH);
        bottom.add(actions, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        add(card);

        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        refresh();
    }

    private model.User selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        return modelRow < customers.size() ? customers.get(modelRow) : null;
    }

    private void updateButtons() {
        model.User u = selected();
        suspendBtn.setEnabled(u != null && u.getStatus() == model.AccountStatus.ACTIVE);
        unsuspendBtn.setEnabled(u != null && u.getStatus() == model.AccountStatus.SUSPENDED);
        permanentBtn.setEnabled(u != null && (u.getStatus()==model.AccountStatus.ACTIVE || u.getStatus()==model.AccountStatus.SUSPENDED));
    }

    private void permanentlySuspend() {
        model.User user=selected();if(user==null)return;
        String reason=JOptionPane.showInputDialog(this,"Reason for permanent suspension (5–1000 characters):");if(reason==null)return;
        if(reason.trim().length()<5 || reason.length()>1000){JOptionPane.showMessageDialog(this,"Provide a reason (5–1000 characters).");return;}
        JTextArea preview=new JTextArea("Permanently suspend "+user.getUsername()+"?\nThis blocks sign-in and cannot be reversed with Unsuspend.\n\nUser message:\n"+service.SecurityIncidents.PERMANENT_SUSPENSION_MESSAGE);
        preview.setEditable(false);preview.setLineWrap(true);preview.setWrapStyleWord(true);preview.setColumns(55);preview.setRows(7);
        if(JOptionPane.showConfirmDialog(this,new JScrollPane(preview),"Confirm permanent suspension",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        try{authService.permanentlySuspendUser(adminUser,user.getId(),reason);refresh();
            JOptionPane.showMessageDialog(this,"Account permanently suspended. The user will see the suspension message while signed in or when attempting to sign in.","Account permanently suspended",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Unable to suspend account",JOptionPane.WARNING_MESSAGE);}
    }

    private void toggleSuspend(boolean suspend) {
        model.User u = selected();
        if (u == null) return;
        String action = suspend ? "Suspend" : "Unsuspend";
        int confirm = JOptionPane.showConfirmDialog(this,
                action + " account for " + u.getUsername() + "?",
                action + " account", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            String reason=JOptionPane.showInputDialog(this,"Reason (5–1000 characters):");if(reason==null)return;
            authService.suspendUser(adminUser, u.getId(), suspend,reason);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override public void addNotify(){super.addNotify();accountRefreshTimer.start();}
    @Override public void removeNotify(){accountRefreshTimer.stop();super.removeNotify();}
    private static String accountSnapshot(java.util.List<model.User> users){
        return users.stream().map(u->u.getId()+"|"+u.getUsername()+"|"+u.getFullName()+"|"+u.getEmail()+"|"+u.getContactNumber()+"|"+u.getRegisteredAt()+"|"+u.getRole()+"|"+u.getStatus()+"|"+u.isEmailVerified()).collect(java.util.stream.Collectors.joining("\n"));
    }
    private String lastAccountSnapshot="";
    private void refreshIfChanged(){
        if(!isShowing() || refreshingAccounts)return;
        refreshingAccounts=true;
        new SwingWorker<java.util.List<model.User>,Void>(){
            protected java.util.List<model.User> doInBackground(){return authService.getManagedUsers(adminUser);}
            protected void done(){try{var latest=get();if(isShowing() && !accountSnapshot(latest).equals(lastAccountSnapshot)){
                model.User selected=selected();int selectedId=selected==null?-1:selected.getId();
                displayAccounts(latest);
                for(int i=0;i<customers.size();i++)if(customers.get(i).getId()==selectedId){int row=table.convertRowIndexToView(i);if(row>=0)table.setRowSelectionInterval(row,row);break;}
            }}catch(Exception ex){/* Retain the last account list during a connection interruption. */}finally{refreshingAccounts=false;}}
        }.execute();
    }
    public void refresh() {
        try{displayAccounts(authService.getManagedUsers(adminUser));}
        catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Unable to refresh accounts",JOptionPane.WARNING_MESSAGE);}
    }
    private void displayAccounts(java.util.List<model.User> latest) {
        table.clearSelection();
        tableModel.setRowCount(0);
        customers=latest;lastAccountSnapshot=accountSnapshot(latest);

        long activeCount = customers.stream()
                .filter(u -> u.getStatus() == model.AccountStatus.ACTIVE && u.isEmailVerified()).count();
        long suspendedCount = customers.stream()
                .filter(u -> u.getStatus() == model.AccountStatus.SUSPENDED || u.getStatus()==model.AccountStatus.BANNED).count();

        metrics.removeAll();
        metrics.add(AdminUi.metrics(
                String.valueOf(customers.stream().filter(u->u.getStatus()==model.AccountStatus.PENDING_APPROVAL||!u.isEmailVerified()&&u.getStatus()==model.AccountStatus.ACTIVE).count()), "Awaiting approval / verification",
                String.valueOf(activeCount), "Active accounts",
                String.valueOf(suspendedCount), "Suspended accounts"));
        metrics.revalidate();
        metrics.repaint();

        for (model.User u : customers) {
            String statusDisplay = u.getStatus() != null ? u.getStatus().name() : "UNKNOWN";
            if(u.getStatus()==model.AccountStatus.BANNED)statusDisplay="PERMANENTLY SUSPENDED";
            if (!u.isEmailVerified() && model.AccountStatus.ACTIVE == u.getStatus()) statusDisplay = "UNVERIFIED";
            String name = (u.getFullName() == null || u.getFullName().isEmpty()) ? u.getUsername() : u.getFullName();
            if(!name.equals(u.getUsername()))name+=" ("+u.getUsername()+")";
            String contact = (u.getContactNumber() == null || u.getContactNumber().isEmpty()) ? "N/A" : u.getContactNumber();
            String joined = u.getRegisteredAt() != null ? u.getRegisteredAt().toLocalDate().toString() : "Unknown";
            tableModel.addRow(new Object[]{name, u.getEmail(), contact, joined,u.getRole().name(), statusDisplay});
        }

        footer.setText(customers.size() + " customer / staff account(s).");
        updateButtons();
    }

    private static final class Ui {
        static final Color INK   = new Color(28, 31, 27);
        static final Color MUTED = new Color(99, 106, 96);
        static final Color FOREST = new Color(55, 70, 56);

        static Font font(int size, int style) { return new Font("Fira Code", style, size); }
        static JLabel label(String v, int size, int style, Color color) {
            JLabel l = new JLabel(v); l.setFont(font(size, style)); l.setForeground(color); return l;
        }
        static void addLeft(JPanel p, JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(c); }
    }
}
