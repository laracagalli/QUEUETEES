package gui.components;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/** Ten rows on screen; reports use all matching rows in the same filter and sort order. */
public final class TablePagination {
    private TablePagination() { }
    public static java.util.List<Integer> matchingRows(JTable table) {
        if(table.getRowSorter() instanceof PagedSorter p)return p.matching();
        java.util.List<Integer> rows=new ArrayList<>();for(int i=0;i<table.getRowCount();i++)rows.add(table.convertRowIndexToModel(i));return rows;
    }
    public static int matchingCount(JTable table){return matchingRows(table).size();}
    public static JPanel install(JTable table) {
        if(Boolean.TRUE.equals(table.getClientProperty("report.snapshot")))return null;
        if(table.getClientProperty("pagination.controls") instanceof JPanel p)return p;
        RowSorter<? extends TableModel> previous=table.getRowSorter();
        PagedSorter sorter=new PagedSorter(table.getModel());
        if(previous instanceof TableRowSorter<?> old){sorter.setSortKeys(old.getSortKeys());
            @SuppressWarnings("unchecked") RowFilter<? super TableModel,? super Integer> filter=(RowFilter<? super TableModel,? super Integer>)old.getRowFilter();sorter.setRowFilter(filter);
        }
        table.setRowSorter(sorter);sorter.setSortsOnUpdates(true);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,5));controls.setOpaque(false);
        JButton back=new JButton("Previous"),next=new JButton("Next");JLabel label=new JLabel();
        controls.setBackground(AppTheme.PAPER);
        back.setFont(new Font("Segoe UI",Font.PLAIN,13));next.setFont(back.getFont());label.setFont(back.getFont());
        back.getAccessibleContext().setAccessibleName("Previous table page");next.getAccessibleContext().setAccessibleName("Next table page");
        controls.add(back);controls.add(label);controls.add(next);
        sorter.changed=()->{int total=sorter.matching().size();int pages=Math.max(1,(total+9)/10);label.setText("Page "+(sorter.page+1)+" / "+pages+" · "+total+" records");back.setEnabled(sorter.page>0);next.setEnabled(sorter.page+1<pages);};
        back.addActionListener(e->sorter.page(sorter.page-1));next.addActionListener(e->sorter.page(sorter.page+1));
        table.putClientProperty("pagination.controls",controls);sorter.refresh();return controls;
    }
    public static void showModelRow(JTable table,int row) {
        if(table.getRowSorter() instanceof PagedSorter p){int index=p.matching().indexOf(row);if(index>=0)p.page(index/10);}
    }
    private static final class PagedSorter extends TableRowSorter<TableModel> {
        private RowFilter<? super TableModel,? super Integer> filter;
        private java.util.List<Integer> all=java.util.List.of();
        private int page;
        private boolean building;
        private Runnable changed=()->{};
        PagedSorter(TableModel model){super(model);refresh();}
        java.util.List<Integer> matching(){return all;}
        void page(int page){this.page=Math.max(0,page);refresh();}
        @Override public void setRowFilter(RowFilter<? super TableModel,? super Integer> filter){this.filter=filter;page=0;refresh();}
        @Override public void setSortKeys(java.util.List<? extends RowSorter.SortKey> keys){super.setSortKeys(keys);if(all!=null)refresh();}
        void refresh(){
            if(building || all==null)return;building=true;
            try {
                TableRowSorter<TableModel> complete=new TableRowSorter<>(getModel());complete.setRowFilter(filter);
                for(int c=0;c<getModel().getColumnCount();c++)complete.setComparator(c,getComparator(c));
                complete.setSortKeys(getSortKeys());java.util.List<Integer> rows=new ArrayList<>();
                for(int i=0;i<complete.getViewRowCount();i++)rows.add(complete.convertRowIndexToModel(i));all=java.util.List.copyOf(rows);
                page=Math.min(page,Math.max(0,(all.size()-1)/10));
                Set<Integer> visible=new HashSet<>(all.subList(Math.min(page*10,all.size()),Math.min(page*10+10,all.size())));
                super.setRowFilter(new RowFilter<TableModel,Integer>(){public boolean include(Entry<? extends TableModel,? extends Integer> e){return visible.contains(e.getIdentifier());}});
                changed.run();
            } finally{building=false;}
        }
        @Override public void allRowsChanged(){super.allRowsChanged();refresh();}
        @Override public void rowsInserted(int first,int end){super.rowsInserted(first,end);refresh();}
        @Override public void rowsDeleted(int first,int end){super.rowsDeleted(first,end);refresh();}
        @Override public void rowsUpdated(int first,int end){super.rowsUpdated(first,end);refresh();}
        @Override public void rowsUpdated(int first,int end,int column){super.rowsUpdated(first,end,column);refresh();}
        @Override public void modelStructureChanged(){super.modelStructureChanged();refresh();}
    }
}
