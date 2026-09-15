import gui.staff.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import javax.swing.*;
import model.*;
import service.StoreService;

/** Run in a fresh JVM: java -Djava.awt.headless=true -cp bin StaffWorkflowTest */
public final class StaffWorkflowTest {
    public static void main(String[] args) throws Exception {
        StoreService store = StoreService.getInstance();
        SwingUtilities.invokeAndWait(() -> {
            new OrderQueuePanel().refresh();
            new OrderDetailsPanel().refresh();
            new CompletedOrdersPanel().refresh();
            new StaffOverviewPanel(() -> {}).refresh();
            new QueueStatusPanel().refresh();
        });
        int product = store.getProducts().get(0).getId();
        int stock = store.findProduct(product).get().getStock();
        Order first = checkout(store, 101, product, "Alice", "Pickup");
        Order second = checkout(store, 102, product, "Bob", "Delivery");
        check(store.findProduct(product).get().getStock() == stock - 2, "Checkout reserves stock");
        expectFailure(() -> store.advanceOrder(second.getId()), "Cannot skip earliest waiting order");
        store.advanceOrder(first.getId(), OrderStatus.CONFIRMED);
        expectFailure(() -> store.advanceOrder(first.getId(), OrderStatus.CONFIRMED), "Reject stale status");
        store.advanceOrder(second.getId());
        check(second.getStatus() == OrderStatus.PREPARING, "Next order may start while first is processing");
        SwingUtilities.invokeAndWait(() -> {
            try {
                OrderQueuePanel queue = new OrderQueuePanel();
                JTable table = field(queue, "table", JTable.class);
                JTextField search = find(queue, JTextField.class);
                search.setText("Bob");
                check(table.getRowCount() == 1, "Search filters customer");
                table.setRowSelectionInterval(0, 0);
                queue.refresh();
                check(table.getSelectedRow() == 0, "Refresh preserves filtered selection");
                check(table.getValueAt(0, 2).equals("Bob"), "Filtered selection maps to correct order");
                search.setText("[");
                check(table.getRowCount() == 0, "Search treats regex characters literally");
                search.setText("");
                check(table.getRowCount() == 2, "Clearing search restores queue");
                new StaffProfilePanel(new User(9, "staff-test", "staff@example.com", "unused",
                        UserRole.STAFF, true, AccountStatus.ACTIVE));
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        store.advanceOrder(first.getId());
        check(first.getStatus() == OrderStatus.READY_FOR_PICKUP, "Preparation advances to ready");
        store.advanceOrder(first.getId());
        check(first.getCompletedAt() != null, "Completion records time");
        expectFailure(() -> store.advanceOrder(first.getId()), "Cannot complete twice");
        check(store.getCompletedOrders().size() == 1 && store.getActiveOrders().size() == 1, "Completed order leaves active queue");
        check(store.getOrdersForCustomer(101).get(0).getStatus() == OrderStatus.COMPLETED, "Customer sees staff update");
        SwingUtilities.invokeAndWait(() -> {
            try {
                CompletedOrdersPanel completed = new CompletedOrdersPanel();
                check(field(completed, "table", JTable.class).getRowCount() == 1, "Completed history displays order");
                StaffOverviewPanel overview = new StaffOverviewPanel(() -> {});
                check(field(overview, "activity", JTable.class).getValueAt(1, 3).equals("Completed"), "Overview reflects completion");
                new OrderDetailsPanel().refresh();
                new QueueStatusPanel().refresh();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("PASS: staff workflow, FIFO, stale updates, customer tracking, search and panel refresh");
    }

    private static Order checkout(StoreService store, int customer, int product, String name, String method) {
        store.addToCart(customer, product);
        return store.checkout(customer, new CheckoutDetails(name, "test@example.com", "09123456789",
                method, "Test address", "Cash", "Test notes"));
    }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    private static void expectFailure(Runnable action, String label) {
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError(label);
    }
    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(owner));
    }
    private static <T> T find(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container) {
                T result = find((Container) child, type);
                if (result != null) return result;
            }
        }
        return null;
    }
}
