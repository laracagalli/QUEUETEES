package gui;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import service.AuthService;

/** Shared QueueTees design system and CardLayout navigation for every role. */
public final class DashboardLayout {
    public static final int FRAME_WIDTH = 1500;
    public static final int FRAME_HEIGHT = 733;
    private static final Color INK = new Color(28, 31, 27), MUTED = new Color(99, 106, 96);
    private static final Color FOREST = new Color(55, 70, 56), SAGE = new Color(145, 155, 145);
    private static final Color CREAM = new Color(241, 241, 232), PAPER = new Color(252, 252, 247);
    private static final Color LINE = new Color(218, 220, 209);
    private static final String FONT = "Fira Code";

    private DashboardLayout() { }

    public static JPanel create(AuthService auth, String role) {
        return "CUSTOMER".equals(role) ? customer(auth) : operations(auth, role);
    }

    private static JPanel operations(AuthService auth, String role) {
        JPanel root = background(); root.setLayout(new BorderLayout());
        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards); content.setOpaque(false);
        content.setBorder(new EmptyBorder(18, 22, 22, 22));
        Map<String, NavButton> navButtons = new LinkedHashMap<>();
        String[][] nav = "ADMIN".equals(role) ? new String[][] {
            {"Overview", "overview"}, {"Staff Approvals", "staff"}, {"Products & Stock", "products"},
            {"Customers", "customers"}, {"Order Queue", "queue"}, {"Sales Reports", "reports"},
            {"My Account", "profile"}
        } : new String[][] {
            {"Overview", "overview"}, {"Order Queue", "queue"}, {"Order Details", "orders"},
            {"Completed Orders", "completed"}, {"Queue Status", "status"}, {"My Account", "profile"}
        };
        JPanel sidebar = sidebar(auth, role, nav, navButtons);
        root.add(sidebar, BorderLayout.WEST);
        java.util.function.Consumer<String> show = key -> {
            cards.show(content, key); navButtons.forEach((k, b) -> b.select(k.equals(key)));
        };
        for (Map.Entry<String, NavButton> e : navButtons.entrySet())
            e.getValue().addActionListener(x -> show.accept(e.getKey()));

        content.add(overview(role, show), "overview");
        if ("ADMIN".equals(role)) {
            content.add(tablePage("STAFF", "Staff account approvals", "Review staff registrations before granting access.",
                new String[]{"Name", "Email", "Date requested", "Status"}, "No staff accounts are awaiting approval.", "Review Selected"), "staff");
            content.add(products(true), "products");
            content.add(tablePage("CUSTOMERS", "Customer accounts", "View registered customers and their access status.",
                new String[]{"Customer", "Email", "Contact", "Status"}, "No customer records are available yet.", null), "customers");
            content.add(queue(true), "queue"); content.add(reports(), "reports");
        } else {
            content.add(queue(false), "queue");
            content.add(tablePage("ORDERS", "Order details", "Select an order to review its fulfillment details.",
                new String[]{"Queue no.", "Customer", "Items", "Total", "Status"}, "Select an order from the queue to see its details.", "Update Status"), "orders");
            content.add(tablePage("ORDERS", "Completed orders", "A record of orders completed by the staff team.",
                new String[]{"Queue no.", "Customer", "Completed", "Total"}, "No completed orders are available yet.", null), "completed");
            content.add(queueStatus(), "status");
        }
        content.add(profile(role), "profile"); root.add(content, BorderLayout.CENTER); show.accept("overview");
        return root;
    }

    private static JPanel sidebar(AuthService auth, String role, String[][] nav, Map<String, NavButton> buttons) {
        JPanel bar = new JPanel(); bar.setBackground(FOREST); bar.setPreferredSize(new Dimension(218, 733));
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS)); bar.setBorder(new EmptyBorder(25, 20, 20, 20));
        addLeft(bar, text("QUEUETEES", 20, Font.BOLD, Color.WHITE));
        addLeft(bar, text("QUEUE WITH EASE", 10, Font.PLAIN, new Color(213, 220, 207)));
        bar.add(Box.createVerticalStrut(28));
        for (String[] item : nav) {
            NavButton button = new NavButton(item[0]); button.setMaximumSize(new Dimension(178, 40));
            button.setAlignmentX(Component.LEFT_ALIGNMENT); buttons.put(item[1], button); bar.add(button);
            bar.add(Box.createVerticalStrut(7));
        }
        bar.add(Box.createVerticalGlue());
        addLeft(bar, text(role + "  •  ONLINE", 10, Font.BOLD, new Color(221, 230, 216)));
        bar.add(Box.createVerticalStrut(14));
        RoundedButton logout = new RoundedButton("Log Out", CREAM, INK); logout.setFont(font(13, Font.BOLD));
        logout.setHoverColor(new Color(218, 225, 211)); logout.setMaximumSize(new Dimension(178, 40));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT); logout.addActionListener(e -> logout(bar, auth)); bar.add(logout);
        return bar;
    }

    private static JComponent overview(String role, java.util.function.Consumer<String> show) {
        JPanel page = page("Good day!", role.equals("ADMIN") ? "Administrator dashboard" : "Staff dashboard",
            role.equals("ADMIN") ? "Monitor QueueTees operations from one place." : "Keep customer orders moving in first-come, first-served order.");
        JPanel hero = card(FOREST, 24, false); hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(new EmptyBorder(22, 25, 22, 25));
        JPanel copy = box(); copy.add(text(role.equals("ADMIN") ? "Order queue overview" : "Ready for the next order?", 17, Font.BOLD, Color.WHITE));
        copy.add(Box.createVerticalStrut(8)); copy.add(text(role.equals("ADMIN") ? "Operational summaries appear when data is connected." : "Open the queue to review the next confirmed order.", 11, Font.PLAIN, new Color(220, 227, 216)));
        hero.add(copy); RoundedButton open = lightButton(role.equals("ADMIN") ? "View Queue" : "Open Queue");
        open.setPreferredSize(new Dimension(140, 40)); open.addActionListener(e -> show.accept("queue")); hero.add(open, BorderLayout.EAST);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110)); hero.setPreferredSize(new Dimension(1000, 110)); page.add(hero); page.add(Box.createVerticalStrut(17));
        JPanel metrics = new JPanel(new GridLayout(1, 3, 15, 0)); metrics.setOpaque(false);
        String[] names = role.equals("ADMIN") ? new String[]{"Pending orders", "Staff approvals", "Low-stock items"} : new String[]{"Waiting", "Processing", "Completed today"};
        for (String name : names) metrics.add(metric("—", name)); metrics.setAlignmentX(Component.LEFT_ALIGNMENT); metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108)); metrics.setPreferredSize(new Dimension(1000, 108));
        page.add(metrics); page.add(Box.createVerticalStrut(17)); page.add(empty("Recent activity", "No operational activity is available yet."));
        return scroll(page);
    }

    private static JComponent queue(boolean admin) {
        JPanel page = page("ORDERS", admin ? "Order queue monitoring" : "Order queue", admin ? "Monitor confirmed orders as they move through the queue." : "Process confirmed orders in first-come, first-served order.");
        JPanel banner = card(FOREST, 24, false); banner.setLayout(new BorderLayout()); banner.setBorder(new EmptyBorder(19, 24, 19, 24));
        JPanel left = box(); left.add(text(admin ? "QUEUE STATUS" : "NOW SERVING", 10, Font.BOLD, new Color(215, 225, 210)));
        left.add(Box.createVerticalStrut(5)); left.add(text("No active order", 23, Font.BOLD, Color.WHITE)); banner.add(left);
        banner.add(text("●  READY", 10, Font.BOLD, new Color(193, 223, 187)), BorderLayout.EAST);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96)); banner.setPreferredSize(new Dimension(1000, 96)); page.add(banner); page.add(Box.createVerticalStrut(16));
        page.add(tableCard(new String[]{"Queue no.", "Customer", "Placed", "Items", "Total", "Status"}, "The order queue is currently empty.", admin ? null : "Process Next"));
        return scroll(page);
    }

    private static JComponent products(boolean manage) {
        JPanel page = page("CATALOG", manage ? "Products & inventory" : "Browse Hiraya Clothing", manage ? "Maintain the catalog and monitor availability." : "Discover available pieces from Hiraya Clothing.");
        page.add(toolbar("Search products", manage ? "Add Product" : null)); page.add(Box.createVerticalStrut(16));
        if (manage) page.add(tableCard(new String[]{"Product", "Category", "Price", "Stock", "Status"}, "No products have been added to the catalog yet.", "Edit Selected"));
        else {
            JPanel items = new JPanel(new GridLayout(1, 3, 16, 0)); items.setOpaque(false);
            items.add(productCard("/Gui_Images/model1.png")); items.add(productCard("/Gui_Images/model3.png")); items.add(productCard(null));
            items.setMaximumSize(new Dimension(Integer.MAX_VALUE, 370)); page.add(items);
        }
        return scroll(page);
    }

    private static JComponent reports() {
        JPanel page = page("REPORTS", "Sales reports", "Review summaries after transaction data is connected.");
        JPanel metrics = new JPanel(new GridLayout(1, 3, 15, 0)); metrics.setOpaque(false);
        metrics.add(metric("—", "Gross sales")); metrics.add(metric("—", "Completed orders")); metrics.add(metric("—", "Average order value"));
        metrics.setAlignmentX(Component.LEFT_ALIGNMENT); metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115)); metrics.setPreferredSize(new Dimension(1000, 115)); page.add(metrics); page.add(Box.createVerticalStrut(18));
        page.add(empty("Sales activity", "No completed transaction data is available yet.")); return scroll(page);
    }

    private static JComponent queueStatus() {
        JPanel page = page("QUEUE", "Queue status", "See the order being served and those waiting next.");
        JPanel status = card(PAPER, 24, true); status.setLayout(new GridLayout(1, 3)); status.setBorder(new EmptyBorder(25, 20, 25, 20));
        status.add(centerMetric("—", "NOW SERVING")); status.add(centerMetric("—", "WAITING")); status.add(centerMetric("—", "COMPLETED"));
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140)); status.setPreferredSize(new Dimension(1000, 140)); page.add(status); page.add(Box.createVerticalStrut(18));
        page.add(empty("Live queue", "Queue information appears when confirmed orders are available.")); return scroll(page);
    }

    private static JComponent profile(String role) {
        JPanel page = page("ACCOUNT", "My account", "Review the signed-in " + role.toLowerCase() + " account.");
        JPanel p = card(PAPER, 24, true); p.setLayout(new BorderLayout(22, 0)); p.setBorder(new EmptyBorder(24, 26, 24, 26));
        JLabel avatar = text(role.substring(0, 1), 28, Font.BOLD, Color.WHITE); avatar.setOpaque(true); avatar.setBackground(FOREST);
        avatar.setHorizontalAlignment(SwingConstants.CENTER); avatar.setPreferredSize(new Dimension(76, 76)); p.add(avatar, BorderLayout.WEST);
        JPanel details = box(); details.add(text(role.substring(0, 1) + role.substring(1).toLowerCase() + " account", 16, Font.BOLD, INK));
        details.add(Box.createVerticalStrut(8)); details.add(text("Profile information loads from the authenticated user record.", 11, Font.PLAIN, MUTED)); p.add(details);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130)); page.add(p); return scroll(page);
    }

    private static JPanel customer(AuthService auth) {
        JPanel root = background(); root.setLayout(new BorderLayout()); CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards); content.setOpaque(false); content.setBorder(new EmptyBorder(18, 42, 24, 42));
        Map<String, CustomerNav> buttons = new LinkedHashMap<>();
        java.util.function.Consumer<String> show = key -> { cards.show(content, key); buttons.forEach((k,b) -> b.select(k.equals(key))); };
        root.add(customerHeader(auth, buttons, show), BorderLayout.NORTH);
        content.add(shop(show), "shop"); content.add(cart(), "cart"); content.add(tracking(), "tracking");
        content.add(tablePage("ORDERS", "Order history", "Review your previous QueueTees orders.", new String[]{"Order", "Placed", "Items", "Total", "Status"}, "You have no previous orders yet.", null), "history");
        content.add(profile("CUSTOMER"), "profile"); root.add(content); show.accept("shop"); return root;
    }

    private static JPanel customerHeader(AuthService auth, Map<String, CustomerNav> buttons, java.util.function.Consumer<String> show) {
        JPanel header = new JPanel(new BorderLayout(20, 0)); header.setBackground(new Color(250, 250, 244));
        header.setBorder(new EmptyBorder(15, 42, 14, 42)); header.setPreferredSize(new Dimension(1100, 72));
        header.add(text("QUEUETEES", 19, Font.BOLD, INK), BorderLayout.WEST); JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0)); nav.setOpaque(false);
        String[][] items = {{"Shop","shop"},{"My Cart","cart"},{"Track Order","tracking"},{"Order History","history"},{"Account","profile"}};
        for (String[] item : items) { CustomerNav b = new CustomerNav(item[0]); b.addActionListener(e -> show.accept(item[1])); buttons.put(item[1], b); nav.add(b); }
        header.add(nav); OutlineButton logout = new OutlineButton("Log Out", INK, INK); logout.setFont(font(11, Font.BOLD));
        logout.setPreferredSize(new Dimension(92, 36)); logout.setBgColor(new Color(145,155,145,90)); logout.addActionListener(e -> logout(header, auth)); header.add(logout, BorderLayout.EAST); return header;
    }

    private static JComponent shop(java.util.function.Consumer<String> show) {
        JPanel page = page("HIRAYA CLOTHING", "Everyday pieces, ordered with ease.", "Browse the catalog, build your cart, and follow your place in the queue.");
        JPanel hero = card(FOREST, 26, false); hero.setLayout(new BorderLayout(20,0)); hero.setBorder(new EmptyBorder(21,26,21,26));
        JPanel copy = box(); copy.add(text("A calmer way to shop Hiraya.", 21, Font.BOLD, Color.WHITE)); copy.add(Box.createVerticalStrut(7));
        copy.add(text("Confirmed orders keep their place in the QueueTees line.", 11, Font.PLAIN, new Color(221,228,216))); hero.add(copy);
        RoundedButton cart = lightButton("View My Cart"); cart.setPreferredSize(new Dimension(140,40)); cart.addActionListener(e -> show.accept("cart")); hero.add(cart, BorderLayout.EAST);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,105)); hero.setPreferredSize(new Dimension(1000,105)); page.add(hero); page.add(Box.createVerticalStrut(17));
        addLeft(page, text("Featured catalog", 16, Font.BOLD, INK)); page.add(Box.createVerticalStrut(11));
        JPanel items = new JPanel(new GridLayout(1,3,16,0)); items.setOpaque(false); items.add(productCard("/Gui_Images/model1.png")); items.add(productCard("/Gui_Images/model3.png")); items.add(productCard(null));
        items.setAlignmentX(Component.LEFT_ALIGNMENT); items.setMaximumSize(new Dimension(Integer.MAX_VALUE,350)); items.setPreferredSize(new Dimension(1000,350)); page.add(items); return scroll(page);
    }

    private static JComponent cart() {
        JPanel page = page("CART", "My cart", "Review selected products before checkout.");
        page.add(empty("Your cart is empty", "Browse the catalog to add your first Hiraya Clothing item.")); return scroll(page);
    }

    private static JComponent tracking() {
        JPanel page = page("QUEUE TRACKING", "Track my order", "Your queue number appears after checkout confirmation.");
        JPanel ticket = card(PAPER, 26, true); ticket.setLayout(new GridBagLayout()); JPanel center = box();
        for (JLabel l : new JLabel[]{text("NO ACTIVE TICKET",10,Font.BOLD,MUTED), text("—",45,Font.BOLD,FOREST), text("Complete checkout to receive a queue number.",11,Font.PLAIN,MUTED)}) { l.setAlignmentX(Component.CENTER_ALIGNMENT); center.add(l); center.add(Box.createVerticalStrut(9)); }
        ticket.add(center); ticket.setMaximumSize(new Dimension(Integer.MAX_VALUE,210)); page.add(ticket); return scroll(page);
    }

    private static JComponent tablePage(String eyebrow, String title, String description, String[] columns, String message, String action) {
        JPanel page = page(eyebrow, title, description); page.add(toolbar("Search records", null)); page.add(Box.createVerticalStrut(16)); page.add(tableCard(columns, message, action)); return scroll(page);
    }

    private static JPanel page(String eyebrow, String title, String description) {
        JPanel p = box(); addLeft(p, text(eyebrow, 10, Font.BOLD, FOREST)); p.add(Box.createVerticalStrut(4));
        addLeft(p, text(title, 25, Font.BOLD, INK)); p.add(Box.createVerticalStrut(5)); addLeft(p, text(description, 11, Font.PLAIN, MUTED)); p.add(Box.createVerticalStrut(18)); return p;
    }

    private static JPanel toolbar(String placeholder, String action) {
        JPanel p = new JPanel(new BorderLayout(12,0)); p.setOpaque(false); p.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JTextField search = new JTextField(placeholder); search.setFont(font(11,Font.PLAIN)); search.setForeground(MUTED); search.setBackground(PAPER);
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(0,13,0,13))); p.add(search);
        if (action != null) { RoundedButton b = primary(action); b.setPreferredSize(new Dimension(140,40)); p.add(b, BorderLayout.EAST); } return p;
    }

    private static JPanel tableCard(String[] columns, String message, String action) {
        JPanel card = card(PAPER,22,true); card.setLayout(new BorderLayout()); card.setBorder(new EmptyBorder(0,0,action==null?0:12,0));
        DefaultTableModel model = new DefaultTableModel(columns,0){ public boolean isCellEditable(int r,int c){return false;} };
        JTable table = new JTable(model); style(table); JScrollPane scroll = new JScrollPane(table); scroll.setBorder(null); scroll.getViewport().setBackground(PAPER); card.add(scroll);
        JLabel empty = text(message,11,Font.PLAIN,MUTED); empty.setHorizontalAlignment(SwingConstants.CENTER); empty.setBorder(new EmptyBorder(12,8,12,8)); card.add(empty,BorderLayout.NORTH);
        if (action != null) { RoundedButton b=primary(action); b.setEnabled(false); b.setToolTipText("Select a record to enable this action."); JPanel wrap=new JPanel(new FlowLayout(FlowLayout.RIGHT,15,0)); wrap.setOpaque(false); wrap.add(b); card.add(wrap,BorderLayout.SOUTH); }
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,350)); card.setPreferredSize(new Dimension(800,350)); return card;
    }

    private static JPanel productCard(String path) {
        JPanel card=card(PAPER,22,true); card.setLayout(new BorderLayout()); JLabel image=new JLabel(path==null?"Catalog data will appear here":"",SwingConstants.CENTER);
        image.setOpaque(true); image.setBackground(new Color(229,230,220)); image.setForeground(MUTED); image.setFont(font(10,Font.PLAIN));
        if(path!=null){ URL url=DashboardLayout.class.getResource(path); if(url!=null) image.setIcon(new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(220,245,Image.SCALE_SMOOTH))); }
        card.add(image); JPanel cap=box(); cap.setBorder(new EmptyBorder(12,15,13,15)); cap.add(text("Catalog item",13,Font.BOLD,INK)); cap.add(Box.createVerticalStrut(4)); cap.add(text("Details load from the product catalog",9,Font.PLAIN,MUTED)); card.add(cap,BorderLayout.SOUTH); return card;
    }

    private static JPanel empty(String title,String message){ JPanel p=card(PAPER,22,true); p.setLayout(new GridBagLayout()); JPanel c=box();
        JLabel icon=text("○",29,Font.PLAIN,SAGE); icon.setAlignmentX(Component.CENTER_ALIGNMENT); c.add(icon); c.add(Box.createVerticalStrut(6));
        JLabel h=text(title,15,Font.BOLD,INK); h.setAlignmentX(Component.CENTER_ALIGNMENT); c.add(h); c.add(Box.createVerticalStrut(6));
        JLabel d=text(message,11,Font.PLAIN,MUTED); d.setAlignmentX(Component.CENTER_ALIGNMENT); c.add(d); p.add(c); p.setMaximumSize(new Dimension(Integer.MAX_VALUE,205)); p.setPreferredSize(new Dimension(1000,205)); return p; }
    private static JPanel metric(String value,String name){ JPanel p=card(PAPER,20,true); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setBorder(new EmptyBorder(17,19,15,19)); p.add(text(value,24,Font.BOLD,FOREST)); p.add(Box.createVerticalStrut(6)); p.add(text(name,11,Font.PLAIN,MUTED)); return p; }
    private static JPanel centerMetric(String value,String name){ JPanel p=box(); JLabel v=text(value,27,Font.BOLD,FOREST); v.setAlignmentX(Component.CENTER_ALIGNMENT); p.add(v); p.add(Box.createVerticalStrut(6)); JLabel n=text(name,10,Font.BOLD,MUTED); n.setAlignmentX(Component.CENTER_ALIGNMENT); p.add(n); return p; }
    private static JComponent scroll(JPanel page){ page.setAlignmentX(Component.LEFT_ALIGNMENT); return page; }
    private static JPanel box(){ JPanel p=new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); return p; }
    private static void addLeft(JPanel p,JComponent c){ c.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(c); }
    private static JLabel text(String s,int size,int style,Color color){ JLabel l=new JLabel(s); l.setFont(font(size,style)); l.setForeground(color); return l; }
    private static Font font(int size,int style){ return new Font(FONT,style,size); }
    private static RoundedButton primary(String s){ RoundedButton b=new RoundedButton(s,INK,Color.WHITE); b.setFont(font(11,Font.BOLD)); b.setHoverColor(new Color(74,91,74)); b.setPreferredSize(new Dimension(135,38)); return b; }
    private static RoundedButton lightButton(String s){ RoundedButton b=new RoundedButton(s,CREAM,INK); b.setFont(font(11,Font.BOLD)); b.setHoverColor(new Color(218,225,211)); return b; }
    private static JPanel background(){ return new JPanel(){ protected void paintComponent(Graphics g){ super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create(); g2.setPaint(new GradientPaint(0,0,CREAM,0,getHeight(),SAGE)); g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose(); }}; }
    private static JPanel card(Color color,int radius,boolean line){ JPanel p=new JPanel(){ protected void paintComponent(Graphics g){ Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius); if(line){g2.setColor(LINE);g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);} g2.dispose(); super.paintComponent(g); }}; p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT); return p; }
    private static void style(JTable t){ t.setFont(font(11,Font.PLAIN)); t.setForeground(INK); t.setBackground(PAPER); t.setSelectionBackground(new Color(222,229,217)); t.setRowHeight(38); t.setShowGrid(false); t.setFillsViewportHeight(true); JTableHeader h=t.getTableHeader(); h.setFont(font(10,Font.BOLD)); h.setForeground(MUTED); h.setBackground(new Color(238,239,230)); h.setPreferredSize(new Dimension(0,38)); h.setReorderingAllowed(false); DefaultTableCellRenderer r=new DefaultTableCellRenderer(); r.setBorder(new EmptyBorder(0,12,0,12)); t.setDefaultRenderer(Object.class,r); }
    private static void logout(Component parent,AuthService auth){ int c=JOptionPane.showConfirmDialog(parent,"Log out of QueueTees?","Confirm Log Out",JOptionPane.YES_NO_OPTION); if(c==JOptionPane.YES_OPTION){Window w=SwingUtilities.getWindowAncestor(parent);if(w!=null)w.dispose();new LoginFrame(auth).setVisible(true);} }

    private static final class NavButton extends JButton {
        boolean selected,hovered; NavButton(String s){super(s);setFont(font(12,Font.PLAIN));setForeground(new Color(224,230,219));setHorizontalAlignment(LEFT);setBorder(new EmptyBorder(0,13,0,13));setOpaque(false);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){hovered=true;repaint();}public void mouseExited(MouseEvent e){hovered=false;repaint();}});}
        void select(boolean s){selected=s;setFont(font(12,s?Font.BOLD:Font.PLAIN));setForeground(s?Color.WHITE:new Color(224,230,219));repaint();}
        protected void paintComponent(Graphics g){if(selected||hovered){Graphics2D x=(Graphics2D)g.create();x.setColor(new Color(255,255,255,selected?34:20));x.fillRoundRect(0,0,getWidth(),getHeight(),14,14);x.dispose();}super.paintComponent(g);}
    }
    private static final class CustomerNav extends JButton {
        boolean selected; CustomerNav(String s){super(s);setFont(font(11,Font.PLAIN));setForeground(MUTED);setBorder(new EmptyBorder(8,11,8,11));setOpaque(false);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}
        void select(boolean s){selected=s;setFont(font(11,s?Font.BOLD:Font.PLAIN));setForeground(s?INK:MUTED);repaint();}
        protected void paintComponent(Graphics g){if(selected){Graphics2D x=(Graphics2D)g.create();x.setColor(new Color(218,224,211));x.fillRoundRect(0,0,getWidth(),getHeight(),18,18);x.dispose();}super.paintComponent(g);}
    }
}
