package gui.admin;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.User;

/** Immutable visible-table snapshot used for both preview and printing. */
public final class AdminTableReport implements Printable {
    private final JTable snapshot;
    private final String title, by, date=java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm z"));
    private final Image logo;
    final int rows;
    AdminTableReport(JTable source,String title,User user) {
        this.title=title;by=user==null?"Administrator (identity unavailable)":user.getUsername()+" | "+user.getEmail();rows=source.getRowCount();
        java.util.List<Integer> visibleColumns = new java.util.ArrayList<>();
        for(int c=0;c<source.getColumnCount();c++)
            if(!"stockActions".equals(source.getColumnModel().getColumn(c).getIdentifier()))visibleColumns.add(c);
        String[] columns=new String[visibleColumns.size()];Object[][] data=new Object[rows][columns.length];
        for(int c=0;c<columns.length;c++){int sourceColumn=visibleColumns.get(c);columns[c]=String.valueOf(source.getColumnModel().getColumn(sourceColumn).getHeaderValue());for(int r=0;r<rows;r++)data[r][c]=source.getValueAt(r,sourceColumn);}
        snapshot=new JTable(new DefaultTableModel(data,columns));AdminUi.style(snapshot);
        snapshot.setFont(new Font("SansSerif",Font.PLAIN,12));snapshot.setForeground(Color.BLACK);
        snapshot.setRowHeight(32);snapshot.setSize(782,Math.max(32,rows*32));
        snapshot.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));
        snapshot.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){
                super.getTableCellRendererComponent(t,value,false,false,row,col);setFont(t.getTableHeader().getFont());
                setForeground(Color.BLACK);setBackground(new Color(232,238,226));setBorder(BorderFactory.createEmptyBorder(6,8,6,8));return this;
            }
        });
        javax.swing.table.TableCellRenderer body=(t,value,selected,focus,row,col)->{
            JTextArea text=new JTextArea(String.valueOf(value));text.setFont(t.getFont());text.setForeground(Color.BLACK);
            text.setLineWrap(true);text.setWrapStyleWord(true);text.setBackground(row%2==0?Color.WHITE:new Color(245,247,241));
            text.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));return text;
        };
        snapshot.setDefaultRenderer(Object.class,body);
        snapshot.setDefaultRenderer(Integer.class,body);
        if(title.equals("Activity log")&&columns.length==5){
            int[] widths={145,100,60,130,347};
            for(int c=0;c<widths.length;c++)snapshot.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }
        snapshot.doLayout();
        int height=0;
        for(int r=0;r<rows;r++){
            int rowHeight=32;
            for(int c=0;c<columns.length;c++){
                Component cell=body.getTableCellRendererComponent(snapshot,data[r][c],false,false,r,c);
                cell.setSize(snapshot.getColumnModel().getColumn(c).getWidth(),Short.MAX_VALUE);
                rowHeight=Math.max(rowHeight,cell.getPreferredSize().height);
            }
            snapshot.setRowHeight(r,rowHeight);height+=rowHeight;
        }
        snapshot.setSize(782,Math.max(32,height));snapshot.getTableHeader().setSize(782,32);
        java.net.URL url=getClass().getResource("/Gui_Images/hirayalogo2.png");logo=url==null?null:new ImageIcon(url).getImage();
    }
    static PageFormat format(){Paper paper=new Paper();paper.setSize(842,595);paper.setImageableArea(30,30,782,535);PageFormat f=new PageFormat();f.setPaper(paper);return f;}
    public int print(Graphics graphics,PageFormat format,int page) throws PrinterException {
        PageFormat bodyFormat=(PageFormat)format.clone();Paper p=bodyFormat.getPaper();p.setImageableArea(format.getImageableX(),format.getImageableY()+110,format.getImageableWidth(),format.getImageableHeight()-110);bodyFormat.setPaper(p);
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);int result;
        if(rows==0){if(page>0){g.dispose();return NO_SUCH_PAGE;}result=PAGE_EXISTS;g.setColor(Color.DARK_GRAY);g.drawString("No records in this report.",(int)format.getImageableX(),(int)format.getImageableY()+135);}else result=snapshot.getPrintable(JTable.PrintMode.FIT_WIDTH,null,new java.text.MessageFormat("Page {0}")).print(g,bodyFormat,page);
        if(result==PAGE_EXISTS){int x=(int)format.getImageableX(),y=(int)format.getImageableY(),w=(int)format.getImageableWidth();g.setColor(Color.BLACK);if(logo!=null)g.drawImage(logo,x+w/2-60,y,120,42,null);g.setFont(new Font("Fira Code",Font.BOLD,13));g.drawString(title,x,y+60);g.setFont(new Font("Fira Code",Font.PLAIN,9));g.drawString("Printed: "+date+"   |   Prepared by: "+by,x,y+77);g.drawString("Visible table snapshot | "+rows+" records",x,y+92);}
        g.dispose();return result;
    }
    BufferedImage page(int index)throws PrinterException {BufferedImage image=new BufferedImage(1684,1190,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,1684,1190);g.scale(2,2);int result=print(g,format(),index);g.dispose();return result==PAGE_EXISTS?image:null;}
    public static void open(JPanel host,JTable table,String title,User user) {
        service.ActivityLogger.record(user, "Report previewed", title);
        AdminTableReport report=new AdminTableReport(table,title,user);
        Component[] originals=host.getComponents();LayoutManager layout=host.getLayout();
        Object[] constraints=new Object[originals.length];
        if(layout instanceof BorderLayout border)for(int i=0;i<originals.length;i++)constraints[i]=border.getConstraints(originals[i]);
        host.removeAll();host.setLayout(new BorderLayout(0,12));
        JPanel preview=new JPanel(new BorderLayout(0,12));preview.setOpaque(false);preview.setName("adminReportPreview");JLabel image=new JLabel();image.setHorizontalAlignment(SwingConstants.CENTER);JLabel count=AdminUi.label("",11,false);
        JButton back=AdminUi.button("Back to table"),previous=AdminUi.button("Previous"),next=AdminUi.button("Next"),print=AdminUi.button("Print report");int[] index={0};
        JComboBox<String> zoom=new gui.components.RoundedComboBox<>(new String[]{"100%","150%","200%"});
        zoom.getAccessibleContext().setAccessibleName("Report preview zoom");
        Runnable update=()->{try{
            BufferedImage page=report.page(index[0]);double scale=new double[]{1,1.5,2}[zoom.getSelectedIndex()];
            image.setIcon(new Icon(){public int getIconWidth(){return (int)(842*scale);}public int getIconHeight(){return (int)(595*scale);}
                public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(page,x,y,getIconWidth(),getIconHeight(),null);g.dispose();}});
            previous.setEnabled(index[0]>0);next.setEnabled(report.page(index[0]+1)!=null);count.setText("Page "+(index[0]+1));
        }catch(PrinterException e){JOptionPane.showMessageDialog(host,"Cannot preview report: "+e.getMessage());}};
        zoom.addActionListener(e->update.run());
        previous.addActionListener(e->{index[0]--;update.run();});next.addActionListener(e->{index[0]++;update.run();});back.addActionListener(e->{host.removeAll();host.setLayout(layout);for(int i=0;i<originals.length;i++)host.add(originals[i],constraints[i]);host.revalidate();host.repaint();});
        print.addActionListener(e->{PrinterJob job=PrinterJob.getPrinterJob();job.setJobName(title);job.setPrintable(report,format());if(job.getPrintService()==null){JOptionPane.showMessageDialog(host,"Set up a printer or Microsoft Print to PDF first.");return;}if(!job.printDialog())return;print.setEnabled(false);back.setEnabled(false);new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{job.print();return null;}protected void done(){print.setEnabled(true);back.setEnabled(true);try{get();service.ActivityLogger.record(user,"Report printed",title + " sent to printer");}catch(Exception ex){JOptionPane.showMessageDialog(host,"Printing failed: "+ex.getMessage());}}}.execute();});
        JPanel heading=new JPanel(new BorderLayout());heading.setOpaque(false);heading.add(AdminUi.label(title,25,true));heading.add(back,BorderLayout.EAST);preview.add(heading,BorderLayout.NORTH);preview.add(new gui.components.ModernScrollPane(image));JPanel actions=new JPanel(new FlowLayout());actions.add(previous);actions.add(count);actions.add(next);actions.add(new JLabel("Zoom"));actions.add(zoom);actions.add(print);preview.add(actions,BorderLayout.SOUTH);host.add(preview);update.run();host.revalidate();host.repaint();
    }
}
