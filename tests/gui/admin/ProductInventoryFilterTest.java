package gui.admin;

import java.lang.reflect.Field;
import javax.swing.*;
import service.StoreService;

/** Exercises combined catalog filters and numeric, reversible stock sorting. */
public class ProductInventoryFilterTest {
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void sorted(JTable table, boolean ascending) {
        for (int row = 1; row < table.getRowCount(); row++) {
            int before = (Integer) table.getValueAt(row - 1, 4);
            int after = (Integer) table.getValueAt(row, 4);
            check(ascending ? before <= after : before >= after, "Stock order is not numeric");
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                StoreService store = StoreService.getInstance();
                store.addProduct("Filter test empty", "Men", "Tops", 1, 0, "");
                store.addProduct("Filter test small", "Men", "Tops", 1, 2, "");
                store.addProduct("Filter test large", "Men", "Tops", 1, 100, "");
                ProductManagementPanel panel = new ProductManagementPanel();
                JTable table = (JTable) field(panel, "table");
                JComboBox<?> category = (JComboBox<?>) field(panel, "categoryFilter");
                JComboBox<?> subcategory = (JComboBox<?>) field(panel, "subcategoryFilter");
                JComboBox<?> status = (JComboBox<?>) field(panel, "statusFilter");
                JTextField search = (JTextField) field(panel, "search");
                sorted(table, true);
                check((Integer) table.getValueAt(0, 4) == 0, "Out-of-stock item must be first");
                category.setSelectedItem("Men");
                subcategory.setSelectedItem("Tops");
                status.setSelectedItem("Available");
                search.setText("Filter test");
                check(table.getRowCount() == 2, "Combined filters returned wrong results");
                sorted(table, true);
                table.getRowSorter().toggleSortOrder(4);
                sorted(table, false);
                panel.refresh();
                sorted(table, false);
                check(table.getRowCount() == 2, "Refresh lost filter selection");
                status.setSelectedItem("Out of stock");
                check(table.getRowCount() == 1 && (Integer) table.getValueAt(0, 4) == 0,
                        "Out-of-stock filter failed");
                search.setText("No matching product");
                check(table.getRowCount() == 0, "Unmatched search should be empty");
                search.setText("");
                category.setSelectedIndex(0);
                subcategory.setSelectedIndex(0);
                status.setSelectedIndex(0);
                store.addProduct("New category", "Custom", "Special", 1, 3, "");
                panel.refresh();
                category.setSelectedItem("Custom");
                check(table.getRowCount() == 1, "New category missing from filter choices");
                System.out.println("PASS: combined filters, numeric stock sorting, sort persistence and dynamic choices");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
