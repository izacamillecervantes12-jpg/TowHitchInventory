import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class CustomerApp {
    private static final Color GREEN = new Color(0x16A34A);
    private static final Color RED = new Color(0xDC2626);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TowHitchCore.ThemeController theme = new TowHitchCore.ThemeController();
    private final ImageIcon lightWallpaper = TowHitchCore.loadImageAsset("securitybg_light.jpg");
    private final ImageIcon darkWallpaper = TowHitchCore.loadImageAsset("securitybg_dark.jpg");
    private final ImageIcon loginLogo = TowHitchCore.loadScaledLoginLogo("logo_darknlight.png", 64);
    private final ImageIcon sidebarLogoLight = TowHitchCore.loadImageAsset("sidebar_logo_light.png");
    private final ImageIcon sidebarLogoDark = TowHitchCore.loadImageAsset("sidebar_logo_dark.png");
    private final ImageIcon dashboardIcon = TowHitchCore.loadScaledImageAsset("dashboard_icon.png", 20, 20);
    private final ImageIcon inventoryIcon = TowHitchCore.loadScaledImageAsset("inventory_icon.png", 20, 20);
    private final ImageIcon notificationIcon = TowHitchCore.loadScaledImageAsset("notification_icon.png", 24, 24);
    private final ImageIcon settingsIcon = TowHitchCore.loadScaledImageAsset("settings_icon.png", 24, 24);
    private JFrame frame;
    private TowHitchCore.UserAccount activeUser;
    private JPanel pageHost;
    private String currentPage = "Order Catalog";
    private String searchText = "";
    private JLabel loginTitle;
    private JLabel loginSubtitle;
    private JLabel loginMessage;
    private JLabel loginFooter;
    private JPopupMenu notificationPopup;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CustomerApp().start());
    }

    private void start() {
        try {
            TowHitchCore.ensureUsersFile();
            TowHitchCore.loadInventory();
            TowHitchCore.loadCustomerOrders();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Unable to initialize portal data: "
                    + e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        frame = new JFrame("Client Ordering Portal | Tow Hitch by 5-21");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1000, 670));
        frame.setSize(1240, 800);
        frame.setLocationRelativeTo(null);
        showLogin();
        frame.setVisible(true);
    }

    private void showLogin() {
        LoginBackgroundPanel root = new LoginBackgroundPanel();
        GlassLoginCard card = new GlassLoginCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 61, 16, 61));
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        JLabel logo = new JLabel(loginLogo, SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(64, 64));
        loginRow(content, logo, 0, 0, 5);
        loginTitle = loginHeading("Welcome", 34, true);
        loginRow(content, loginTitle, 1, 0, 4);
        loginSubtitle = loginHeading("Browse and order tow hitch products", 17, false);
        loginRow(content, loginSubtitle, 2, 0, 8);

        JTextField username = loginTextField();
        JPasswordField password = loginPasswordField();
        loginRow(content, loginLabel("Username"), 3, 10, 4);
        loginRow(content, username, 4, 0, 10);
        loginRow(content, loginLabel("Password"), 5, 0, 4);
        loginRow(content, password, 6, 0, 7);
        loginMessage = loginHeading(" ", 12, false);
        loginMessage.setForeground(theme.danger());
        loginRow(content, loginMessage, 7, 0, 6);

        TowHitchCore.RoundedButton signIn = new TowHitchCore.RoundedButton(
                "Log in", theme.primaryButton(), Color.WHITE, 12, theme);
        signIn.setFont(new Font("SansSerif", Font.BOLD, 14));
        signIn.setPreferredSize(new Dimension(394, 50));
        signIn.addActionListener(event -> authenticate(username, password));
        password.addActionListener(event -> authenticate(username, password));
        loginRow(content, signIn, 8, 8, 4);

        JButton signup = new JButton("Create Customer Account (Sign Up)");
        signup.setFont(new Font("SansSerif", Font.BOLD, 13));
        signup.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        signup.setOpaque(false);
        signup.setContentAreaFilled(false);
        signup.setBorderPainted(false);
        signup.setFocusPainted(false);
        signup.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        signup.addActionListener(event -> openSignUpDialog());
        loginRow(content, signup, 9, 8, 2);
        content.setPreferredSize(new Dimension(394, content.getPreferredSize().height));
        card.add(Box.createVerticalGlue());
        card.add(content);
        loginFooter = loginHeading("Tow Hitch by 5-21 | Customer Portal", 16, false);
        card.add(Box.createVerticalGlue());
        card.add(loginFooter);
        card.setPreferredSize(new Dimension(516, 610));
        root.setContent(card, new ThemeSwitchButton());
        frame.setContentPane(root);
        frame.revalidate();
        frame.repaint();
    }

    private void loginRow(JPanel panel, Component component, int row, int top, int bottom) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(top, 0, bottom, 0);
        panel.add(component, c);
    }

    private JLabel loginHeading(String text, int size, boolean bold) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        return label;
    }

    private JLabel loginLabel(String text) {
        JLabel label = loginHeading(text, 21, true);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private JTextField loginTextField() {
        return new LoginTextField("Enter your username");
    }

    private JPasswordField loginPasswordField() {
        return new LoginPasswordField("Enter password");
    }

    private void authenticate(JTextField username, JPasswordField password) {
        try {
            String name = username.getText().trim();
            String secret = new String(password.getPassword());
            for (TowHitchCore.UserAccount user : TowHitchCore.loadUsers()) {
                if (user.username.equals(name) && user.password.equals(secret)) {
                    if (!"CUSTOMER".equals(user.role)) {
                        loginMessage.setText("<html><div style='width:380px;text-align:center'>"
                                + "Access denied: This portal is restricted to Customer accounts only."
                                + "</div></html>");
                        return;
                    }
                    activeUser = user;
                    currentPage = "Order Catalog";
                    buildApplication();
                    return;
                }
            }
            loginMessage.setText("Invalid username or password.");
        } catch (IOException e) {
            loginMessage.setText("Unable to read users.txt: " + e.getMessage());
        }
    }

    private void openSignUpDialog() {
        JDialog dialog = themedDialog("Create Customer Account", 470, 440);
        JPanel form = dialogContent();
        JTextField fullName = dialogField("");
        JTextField username = dialogField("");
        JPasswordField password = dialogPasswordField();
        JPasswordField confirm = dialogPasswordField();
        addFormRow(form, "Full Name", fullName, 0);
        addFormRow(form, "Username", username, 1);
        addFormRow(form, "Password", password, 2);
        addFormRow(form, "Confirm Password", confirm, 3);
        dialog.add(form, BorderLayout.CENTER);
        JPanel actions = dialogActions();
        TowHitchCore.RoundedButton close = actionButton("Cancel", theme.sidebar(), theme.text());
        close.addActionListener(event -> dialog.dispose());
        TowHitchCore.RoundedButton create = actionButton("Create Account",
                theme.primaryButton(), Color.WHITE);
        create.addActionListener(event -> {
            String name = fullName.getText().trim();
            String userName = username.getText().trim();
            String secret = new String(password.getPassword());
            String confirmation = new String(confirm.getPassword());
            if (!validField(name) || !validField(userName) || !validField(secret)
                    || !validField(confirmation)) {
                error("Complete every field. Tabs and line breaks are not allowed.");
                return;
            }
            if (!secret.equals(confirmation)) {
                error("Passwords do not match.");
                return;
            }
            try {
                List<TowHitchCore.UserAccount> users = TowHitchCore.loadUsers();
                for (TowHitchCore.UserAccount user : users) {
                    if (user.username.equalsIgnoreCase(userName)) {
                        error("That username is already in use.");
                        return;
                    }
                }
                users.add(new TowHitchCore.UserAccount(userName, secret, name, "CUSTOMER"));
                TowHitchCore.saveUsers(users);
                dialog.dispose();
                JOptionPane.showMessageDialog(frame,
                        "Customer account created. You can now sign in.",
                        "Account Created", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                showDataError("Unable to save users.txt", e);
            }
        });
        actions.add(close);
        actions.add(create);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void buildApplication() {
        JPanel root = new GradientPanel();
        root.setLayout(new BorderLayout(16, 0));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);
        main.add(buildTopBar(), BorderLayout.NORTH);
        pageHost = new JPanel(new CardLayout());
        pageHost.setOpaque(false);
        pageHost.add(buildCatalogPage(), "Order Catalog");
        pageHost.add(buildMyOrdersPage(), "My Orders");
        main.add(pageHost, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        frame.setContentPane(root);
        ((CardLayout) pageHost.getLayout()).show(pageHost, currentPage);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel buildSidebar() {
        TowHitchCore.RoundedPanel sidebar = new TowHitchCore.RoundedPanel(24);
        sidebar.setPreferredSize(new Dimension(220, 100));
        sidebar.setBackground(theme.sidebar());
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 16, 18, 16));
        sidebar.setLayout(new BorderLayout(0, 18));
        JPanel upper = new JPanel(new BorderLayout(0, 22));
        upper.setOpaque(false);
        ImageIcon logo = theme.isDark() ? sidebarLogoDark : sidebarLogoLight;
        if (logo != null) {
            JLabel brand = new JLabel(TowHitchCore.scaleToFitIcon(logo, 172, 112));
            brand.setHorizontalAlignment(SwingConstants.CENTER);
            upper.add(brand, BorderLayout.NORTH);
        }
        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 8));
        nav.setOpaque(false);
        for (String page : new String[] {"Order Catalog", "My Orders"}) {
            boolean selected = page.equals(currentPage);
            Color foreground = selected ? Color.WHITE : theme.secondaryText();
            TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(page,
                    selected ? theme.primaryButton() : theme.sidebar(), foreground, 12, theme);
            button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 14));
            button.setPreferredSize(new Dimension(184, 42));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            ImageIcon icon = "Order Catalog".equals(page) ? inventoryIcon : dashboardIcon;
            if (icon != null) {
                button.setIcon(TowHitchCore.loadTintedIcon(icon, 20, foreground));
                button.setIconTextGap(9);
            }
            button.addActionListener(event -> navigate(page));
            nav.add(button);
        }
        upper.add(nav, BorderLayout.CENTER);
        sidebar.add(upper, BorderLayout.CENTER);
        TowHitchCore.RoundedButton logout = new TowHitchCore.RoundedButton(
                "Logout", theme.sidebar(), theme.secondaryText(), 12, theme);
        logout.setPreferredSize(new Dimension(184, 42));
        logout.addActionListener(event -> {
            activeUser = null;
            currentPage = "Order Catalog";
            showLogin();
        });
        JPanel logoutHolder = new JPanel(new GridBagLayout());
        logoutHolder.setOpaque(false);
        logoutHolder.add(logout);
        sidebar.add(logoutHolder, BorderLayout.SOUTH);
        return sidebar;
    }

    private void navigate(String page) {
        currentPage = page;
        buildApplication();
    }

    private JPanel buildTopBar() {
        TowHitchCore.RoundedPanel top = new TowHitchCore.RoundedPanel(20);
        top.setBackground(theme.panel());
        top.setLayout(new BorderLayout(12, 0));
        top.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JPanel searchArea = new JPanel(new BorderLayout(10, 0));
        searchArea.setOpaque(false);
        TowHitchCore.FlatIconButton menu = new TowHitchCore.FlatIconButton("\u2630", "Navigation", theme);
        menu.setIconButtonSize(30);
        menu.addActionListener(event -> navigate("Order Catalog"));
        searchArea.add(menu, BorderLayout.WEST);
        RoundedSearchPanel searchPanel = new RoundedSearchPanel();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.setPreferredSize(new Dimension(360, 42));
        TowHitchCore.RoundedTextField search = new TowHitchCore.RoundedTextField("Search products", 24, theme);
        search.setText(searchText);
        search.setOpaque(false);
        search.addActionListener(event -> {
            searchText = search.getText().trim();
            navigate("Order Catalog");
        });
        searchPanel.add(buildSearchLeading(), BorderLayout.WEST);
        searchPanel.add(search, BorderLayout.CENTER);
        searchArea.add(searchPanel, BorderLayout.CENTER);
        top.add(searchArea, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        actions.setOpaque(false);
        JLabel role = new JLabel("CUSTOMER \u2022 " + activeUser.fullName);
        role.setFont(new Font("SansSerif", Font.BOLD, 12));
        role.setForeground(theme.success());
        actions.add(role);
        actions.add(new ThemeSwitchButton());
        TowHitchCore.FlatIconButton bell = new TowHitchCore.FlatIconButton(
                TowHitchCore.loadTintedIcon(notificationIcon, 24, theme.text()),
                "Low-stock notifications", theme);
        bell.setIconButtonSize(24);
        bell.setUnreadCount(lowStockCount());
        bell.addActionListener(event -> showNotifications(bell));
        TowHitchCore.FlatIconButton settings = new TowHitchCore.FlatIconButton(
                TowHitchCore.loadTintedIcon(settingsIcon, 24, theme.text()), "Settings", theme);
        settings.setIconButtonSize(24);
        settings.addActionListener(event -> showSettings(settings));
        actions.add(bell);
        actions.add(settings);
        top.add(actions, BorderLayout.EAST);
        return top;
    }

    private JPanel buildCatalogPage() {
        JPanel page = pageShell("Order Catalog", "Browse finished goods available from Tow Hitch by 5-21.");
        JPanel rows = verticalPanel();
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            int visible = 0;
            for (TowHitchCore.InventoryItem item : inventory) {
                if (!"F".equals(item.type) || !matchesSearch(item.itemName)) continue;
                rows.add(productCard(item));
                rows.add(Box.createVerticalStrut(10));
                visible++;
            }
            if (visible == 0) rows.add(textLabel("No finished products match your search.", 14,
                    false, theme.secondaryText()));
        } catch (IOException e) {
            showDataError("Unable to load finished goods", e);
        }
        page.add(scroll(rows), BorderLayout.CENTER);
        return page;
    }

    private JPanel productCard(TowHitchCore.InventoryItem item) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(16);
        card.setBackground(theme.panel());
        card.setBorder(BorderFactory.createEmptyBorder(17, 18, 17, 18));
        card.setLayout(new BorderLayout(12, 0));
        long reserved = pendingForProduct(item.itemName);
        int maxOrderable = (int) Math.max(0L, (long) item.quantity - reserved);
        JPanel information = new JPanel();
        information.setOpaque(false);
        information.setLayout(new BoxLayout(information, BoxLayout.Y_AXIS));
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        heading.setOpaque(false);
        heading.add(textLabel(item.itemName, 17, true, theme.text()));
        String status = item.quantity == 0 ? "OUT OF STOCK"
                : item.quantity <= item.minAlert ? "LOW STOCK" : "IN STOCK";
        heading.add(new TowHitchCore.StatusPillBadge(status, theme));
        information.add(heading);
        information.add(Box.createVerticalStrut(7));
        information.add(textLabel("Available Stock: " + item.quantity
                + "  \u2022  Orderable now: " + maxOrderable, 13, false,
                theme.secondaryText()));
        card.add(information, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        if (item.quantity == 0) {
            JLabel out = textLabel("Out of Stock", 12, true, theme.danger());
            controls.add(out);
        } else {
            JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1,
                    Math.max(1, maxOrderable), 1));
            quantity.setEnabled(maxOrderable > 0);
            quantity.setPreferredSize(new Dimension(72, 36));
            controls.add(quantity);
            TowHitchCore.RoundedButton order = actionButton("Place Order",
                    theme.primaryButton(), Color.WHITE);
            order.setEnabled(maxOrderable > 0);
            order.addActionListener(event -> placeOrder(item, quantity));
            controls.add(order);
            if (maxOrderable == 0) {
                controls.add(textLabel("Pending orders use all available units", 11,
                        false, theme.secondaryText()));
            }
        }
        card.add(controls, BorderLayout.EAST);
        return card;
    }

    private void placeOrder(TowHitchCore.InventoryItem product, JSpinner quantity) {
        int requested = ((Number) quantity.getValue()).intValue();
        if (requested < 1 || requested > product.quantity) {
            error("Order quantity must be between 1 and available stock.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(frame,
                "Place order for " + requested + " x " + product.itemName + "?",
                "Confirm Order", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            TowHitchCore.InventoryItem current = null;
            for (TowHitchCore.InventoryItem item : inventory) {
                if ("F".equals(item.type) && item.itemName.equalsIgnoreCase(product.itemName)) {
                    current = item;
                    break;
                }
            }
            long reserved = pendingForProduct(product.itemName);
            if (current == null || requested > current.quantity
                    || reserved > (long) current.quantity - requested) {
                error("Insufficient unreserved Finished Goods stock for this order.");
                buildApplication();
                return;
            }
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            String id = "CO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            orders.add(new TowHitchCore.CustomerOrder(id, now(), activeUser.fullName,
                    current.itemName, requested, "PENDING"));
            TowHitchCore.saveCustomerOrders(orders);
            TowHitchCore.appendHistory("Placed customer order: " + id + " - " + requested
                    + " " + current.itemName + " for " + activeUser.fullName);
            JOptionPane.showMessageDialog(frame,
                    "Your order has been submitted and is pending confirmation.",
                    "Order Placed", JOptionPane.INFORMATION_MESSAGE);
            currentPage = "My Orders";
            buildApplication();
        } catch (IOException e) {
            showDataError("Unable to place customer order", e);
        }
    }

    private JPanel buildMyOrdersPage() {
        JPanel page = pageShell("My Orders", "Track and manage your order requests.");
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        double[] weights = {.14, .18, .18, .07, .16, .27};
        list.add(tableHeader(new String[] {"Status", "Order ID", "Product", "Qty", "Order Date", "Action"},
                weights), BorderLayout.NORTH);
        JPanel rows = verticalPanel();
        try {
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            orders.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
            int count = 0;
            for (TowHitchCore.CustomerOrder order : orders) {
                if (!order.customerName.equalsIgnoreCase(activeUser.fullName)) continue;
                rows.add(orderRow(order, weights));
                rows.add(Box.createVerticalStrut(7));
                count++;
            }
            if (count == 0) rows.add(textLabel("You have no orders yet.", 13, false,
                    theme.secondaryText()));
        } catch (IOException e) {
            showDataError("Unable to load your orders", e);
        }
        list.add(scroll(rows), BorderLayout.CENTER);
        page.add(list, BorderLayout.CENTER);
        return page;
    }

    private JPanel orderRow(TowHitchCore.CustomerOrder order, double[] weights) {
        JPanel row = rowPanel();
        addCell(row, new TowHitchCore.StatusPillBadge(order.status, theme), 0, weights[0]);
        addCell(row, tableCell(order.orderId), 1, weights[1]);
        addCell(row, tableCell(order.productName), 2, weights[2]);
        addCell(row, tableCell(Integer.toString(order.quantity)), 3, weights[3]);
        addCell(row, tableCell(order.timestamp), 4, weights[4]);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);
        if ("PENDING".equals(order.status)) {
            TowHitchCore.RoundedButton cancel = actionButton("Cancel", RED, Color.WHITE);
            cancel.addActionListener(event -> cancelOwnOrder(order));
            actions.add(cancel);
        }
        addCell(row, actions, 5, weights[5]);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        return row;
    }

    private void cancelOwnOrder(TowHitchCore.CustomerOrder selected) {
        int answer = JOptionPane.showConfirmDialog(frame,
                "Cancel your order " + selected.orderId + "?",
                "Cancel Order", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            boolean found = false;
            for (TowHitchCore.CustomerOrder order : orders) {
                if (order.orderId.equals(selected.orderId)
                        && order.customerName.equalsIgnoreCase(activeUser.fullName)
                        && "PENDING".equals(order.status)) {
                    order.setStatus("CANCELLED");
                    found = true;
                }
            }
            if (!found) {
                error("This order is no longer pending or does not belong to your account.");
                return;
            }
            TowHitchCore.saveCustomerOrders(orders);
            TowHitchCore.appendHistory("Cancelled customer order: " + selected.orderId
                    + " - Cancelled by Customer " + activeUser.fullName);
            buildApplication();
        } catch (IOException e) {
            showDataError("Unable to cancel order", e);
        }
    }

    private long pendingForProduct(String productName) {
        try {
            long pending = 0;
            for (TowHitchCore.CustomerOrder order : TowHitchCore.loadCustomerOrders()) {
                if ("PENDING".equals(order.status)
                        && order.productName.equalsIgnoreCase(productName)) {
                    pending = Math.min(Long.MAX_VALUE - order.quantity, pending) + order.quantity;
                }
            }
            return pending;
        } catch (IOException e) {
            showDataError("Unable to verify pending orders", e);
            return Long.MAX_VALUE;
        }
    }

    private JPanel pageShell(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(textLabel(title, 24, true, theme.text()));
        labels.add(Box.createVerticalStrut(3));
        labels.add(textLabel(subtitle, 12, false, theme.secondaryText()));
        heading.add(labels, BorderLayout.CENTER);
        page.add(heading, BorderLayout.NORTH);
        return page;
    }

    private JPanel tableHeader(String[] labels, double[] weights) {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(theme.sidebar());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < labels.length; i++) {
            JLabel label = tableCell(labels[i]);
            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setForeground(theme.secondaryText());
            addCell(header, label, i, weights[i]);
        }
        return header;
    }

    private JPanel rowPanel() {
        JPanel row = new TowHitchCore.RoundedPanel(12);
        row.setBackground(theme.row());
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        return row;
    }

    private void addCell(JPanel row, Component component, int column, double weight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = 0;
        c.weightx = weight;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = column == 0 ? GridBagConstraints.CENTER : GridBagConstraints.WEST;
        c.insets = new Insets(0, 4, 0, 4);
        row.add(component, c);
    }

    private JLabel tableCell(String text) {
        JLabel label = textLabel(text, 12, false, theme.text());
        label.setToolTipText(text);
        return label;
    }

    private JLabel textLabel(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private JScrollPane scroll(Component component) {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.getVerticalScrollBar().setUI(new TowHitchCore.MinimalScrollBarUI(theme));
        pane.getHorizontalScrollBar().setUI(new TowHitchCore.MinimalScrollBarUI(theme));
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return pane;
    }

    private TowHitchCore.RoundedButton actionButton(String text, Color fill, Color foreground) {
        TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(
                text, fill, foreground, 12, theme);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setMargin(new Insets(5, 10, 5, 10));
        return button;
    }

    private JDialog themedDialog(String title, int width, int height) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(frame);
        dialog.getContentPane().setBackground(theme.workspaceCanvas());
        dialog.setLayout(new BorderLayout(0, 10));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(16, 18, 0, 18));
        heading.add(textLabel(title, 18, true, theme.text()));
        dialog.add(heading, BorderLayout.NORTH);
        return dialog;
    }

    private JPanel dialogContent() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        return form;
    }

    private JTextField dialogField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(theme.text());
        field.setBackground(theme.input());
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(theme.row()),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setPreferredSize(new Dimension(230, 38));
        return field;
    }

    private JPasswordField dialogPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(theme.text());
        field.setBackground(theme.input());
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(theme.row()),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setPreferredSize(new Dimension(230, 38));
        return field;
    }

    private void addFormRow(JPanel form, String label, Component field, int row) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(7, 0, 7, 12);
        form.add(textLabel(label, 12, true, theme.text()), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(7, 0, 7, 0);
        form.add(field, right);
    }

    private JPanel dialogActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 14, 16));
        return panel;
    }

    private boolean validField(String value) {
        return value != null && !value.trim().isEmpty() && value.indexOf('\t') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0;
    }

    private boolean matchesSearch(String name) {
        return searchText.isEmpty() || name.toLowerCase().contains(searchText.toLowerCase());
    }

    private String now() {
        return LocalDateTime.now().format(TIMESTAMP);
    }

    private int lowStockCount() {
        try {
            int count = 0;
            for (TowHitchCore.InventoryItem item : TowHitchCore.loadInventory()) {
                if (item.quantity <= item.minAlert) count++;
            }
            return count;
        } catch (IOException e) {
            showDataError("Unable to load inventory alerts", e);
            return 0;
        }
    }

    private void showNotifications(Component invoker) {
        if (notificationPopup != null && notificationPopup.isVisible()) {
            notificationPopup.setVisible(false);
            return;
        }
        JPopupMenu popup = new RoundedPopup();
        JPanel content = verticalPanel();
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        title.setOpaque(false);
        title.add(textLabel("\u25CF", 14, true, theme.danger()));
        title.add(textLabel("Low Stocks Alert", 14, true, theme.text()));
        content.add(title);
        int count = 0;
        try {
            for (TowHitchCore.InventoryItem item : TowHitchCore.loadInventory()) {
                if (item.quantity <= item.minAlert) {
                    JPanel row = new TowHitchCore.RoundedPanel(10);
                    row.setBackground(theme.row());
                    row.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
                    row.setLayout(new BorderLayout(8, 0));
                    row.add(textLabel(item.itemName + " \u2022 "
                            + ("F".equals(item.type) ? "Finished Goods" : "Unfinished Goods"),
                            12, true, theme.text()), BorderLayout.CENTER);
                    row.add(textLabel(item.quantity + " on hand (min alert: " + item.minAlert + ")",
                            11, true, theme.danger()), BorderLayout.EAST);
                    row.setMaximumSize(new Dimension(380, 44));
                    content.add(row);
                    content.add(Box.createVerticalStrut(4));
                    count++;
                }
            }
        } catch (IOException e) {
            showDataError("Unable to load low-stock notifications", e);
        }
        if (count == 0) content.add(textLabel("No low-stock items.", 12, false, theme.success()));
        JScrollPane pane = scroll(content);
        pane.setPreferredSize(new Dimension(390, Math.min(300, Math.max(70, count * 50 + 45))));
        popup.add(pane);
        notificationPopup = popup;
        popup.show(invoker, 0, invoker.getHeight());
    }

    private void showSettings(Component invoker) {
        JPopupMenu popup = new RoundedPopup();
        JMenuItem logout = new JMenuItem("Logout");
        logout.addActionListener(event -> {
            activeUser = null;
            currentPage = "Order Catalog";
            showLogin();
        });
        popup.add(logout);
        popup.show(invoker, 0, invoker.getHeight());
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(frame, message, "Action Required", JOptionPane.ERROR_MESSAGE);
    }

    private void showDataError(String context, IOException e) {
        JOptionPane.showMessageDialog(frame, context + ": " + e.getMessage(),
                "Data Error", JOptionPane.ERROR_MESSAGE);
    }

    private class ThemeSwitchButton extends JButton {
        private static final long serialVersionUID = 1L;
        private float position = theme.isDark() ? 1f : 0f;
        private Timer timer;
        private ThemeSwitchButton() {
            setPreferredSize(new Dimension(84, 40));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setToolTipText("Toggle light and dark themes");
            addActionListener(event -> {
                if (timer != null) timer.stop();
                float start = position;
                float end = theme.isDark() ? 0f : 1f;
                long started = System.currentTimeMillis();
                timer = new Timer(15, tick -> {
                    float progress = Math.min(1f, (System.currentTimeMillis() - started) / 180f);
                    position = start + (end - start) * progress;
                    repaint();
                    if (progress >= 1f) {
                        ((Timer) tick.getSource()).stop();
                        theme.toggle();
                        if (activeUser == null) refreshLoginTheme();
                        else buildApplication();
                    }
                });
                timer.start();
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(theme.isDark() ? new Color(0x34312F) : theme.sidebar());
            g.fillRoundRect(0, 1, width, height - 2, height - 2, height - 2);
            int size = height - 10;
            int x = 5 + Math.round((width - size - 10) * position);
            g.setColor(theme.isDark() ? new Color(0x303856) : Color.WHITE);
            g.fillOval(x, 5, size, size);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(theme.isDark() ? new Color(0x969DAD) : new Color(0x23211F));
            g.drawString("\u2600", 11, 28);
            g.setColor(theme.isDark() ? new Color(0xF5E9B8) : new Color(0x8B949E));
            g.drawString("\u263E", width - 25, 28);
            g.dispose();
        }

        private void refreshLoginTheme() {
            loginTitle.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
            loginSubtitle.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x625E5C));
            loginFooter.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x686361));
            loginMessage.setForeground(theme.danger());
            updateLoginLabels(frame.getContentPane());
            frame.getContentPane().repaint();
        }

        private void updateLoginLabels(Component component) {
            if (component instanceof JLabel && component != loginTitle && component != loginSubtitle
                    && component != loginFooter && component != loginMessage) {
                component.setForeground(theme.isDark()
                        ? new Color(0xF5F1ED) : new Color(0x3A3534));
            } else if (component instanceof JButton
                    && !(component instanceof TowHitchCore.RoundedButton)) {
                component.setForeground(theme.isDark()
                        ? new Color(0xF5F1ED) : new Color(0x3A3534));
            }
            if (component instanceof java.awt.Container) {
                for (Component child : ((java.awt.Container) component).getComponents()) {
                    updateLoginLabels(child);
                }
            }
        }
    }

    private class LoginBackgroundPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private JPanel card;
        private JButton switchButton;
        private LoginBackgroundPanel() { setOpaque(true); setLayout(null); }
        private void setContent(JPanel card, JButton button) {
            this.card = card;
            this.switchButton = button;
            add(card);
            add(button);
        }
        @Override public void doLayout() {
            if (card != null) {
                int width = Math.min(516, getWidth() - 32);
                int height = Math.min(card.getPreferredSize().height, getHeight() - 32);
                card.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
            }
            if (switchButton != null) switchButton.setBounds(getWidth() - 106, 16, 84, 40);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            ImageIcon wallpaper = theme.isDark() ? darkWallpaper : lightWallpaper;
            if (wallpaper != null) g.drawImage(wallpaper.getImage(), 0, 0, getWidth(), getHeight(), this);
            else {
                g.setPaint(new GradientPaint(0, 0,
                        theme.isDark() ? new Color(0x1F1C1A) : new Color(0xF2E3CE),
                        getWidth(), getHeight(),
                        theme.isDark() ? new Color(0x4A2E2B) : new Color(0xD9C5B2)));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.dispose();
        }
    }

    private class GlassLoginCard extends JPanel {
        private static final long serialVersionUID = 1L;
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.isDark() ? new Color(45, 42, 38, 195) : new Color(255, 255, 255, 175));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
            g.setColor(theme.isDark() ? new Color(255, 255, 255, 32) : new Color(255, 255, 255, 115));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private class LoginTextField extends JTextField {
            private static final long serialVersionUID = 1L;
            private final String placeholder;

            private LoginTextField(String placeholder) {
                this.placeholder = placeholder;
                setFont(new Font("SansSerif", Font.PLAIN, 14));
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 16));
                setPreferredSize(new Dimension(394, 51));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(theme.isDark() ? new Color(0xB6B1AA) : new Color(0xF4EFE6));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g.dispose();
                setForeground(new Color(0x242221));
                setCaretColor(new Color(0x242221));
                super.paintComponent(graphics);
                if (getText().isEmpty() && !hasFocus()) paintPlaceholder(graphics, placeholder, this);
            }
        }

    private class LoginPasswordField extends JPasswordField {
            private static final long serialVersionUID = 1L;
            private final String placeholder;

            private LoginPasswordField(String placeholder) {
                this.placeholder = placeholder;
                setFont(new Font("SansSerif", Font.PLAIN, 14));
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 16));
                setPreferredSize(new Dimension(394, 51));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(theme.isDark() ? new Color(0xB6B1AA) : new Color(0xF4EFE6));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g.dispose();
                setForeground(new Color(0x242221));
                setCaretColor(new Color(0x242221));
                super.paintComponent(graphics);
                if (getPassword().length == 0 && !hasFocus()) paintPlaceholder(graphics, placeholder, this);
            }
        }

    private void paintPlaceholder(Graphics graphics, String placeholder, Component field) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(theme.isDark() ? new Color(0x5D5954) : new Color(0x77716C));
            java.awt.FontMetrics metrics = g.getFontMetrics();
            int baseline = (field.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(placeholder, 28, baseline);
            g.dispose();
    }

    private class GradientPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            graphics.setColor(theme.workspaceCanvas());
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private class RoundedPopup extends JPopupMenu {
        private static final long serialVersionUID = 1L;
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.dashboardPanel());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.setColor(theme.row());
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.dispose();
        }
    }

    private JPanel buildSearchLeading() {
            JPanel leading = new JPanel(new GridBagLayout());
            leading.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0;
            c.anchor = GridBagConstraints.CENTER;
            c.insets = new Insets(0, 0, 0, 10);
            leading.add(new SearchGlass(), c);
            c.gridx = 1;
            c.insets = new Insets(0, 0, 0, 0);
            leading.add(new SearchDivider(), c);
            return leading;
        }

    private class RoundedSearchPanel extends JPanel {
            private static final long serialVersionUID = 1L;
            private RoundedSearchPanel() {
                setOpaque(false);
                setLayout(new BorderLayout());
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            }
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(theme.input());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g.dispose();
            }
        }

    private class SearchGlass extends JComponent {
            private static final long serialVersionUID = 1L;
            private SearchGlass() { setOpaque(false); setPreferredSize(new Dimension(24, 24)); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(theme.isDark() ? new Color(0xF4EFE6) : theme.secondaryText());
                g.setStroke(new java.awt.BasicStroke(1.8f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int x = (getWidth() - 15) / 2;
                int y = (getHeight() - 15) / 2;
                g.drawOval(x, y, 10, 10);
                g.drawLine(x + 8, y + 8, x + 14, y + 14);
                g.dispose();
            }
        }

    private class SearchDivider extends JComponent {
            private static final long serialVersionUID = 1L;
            private SearchDivider() { setOpaque(false); setPreferredSize(new Dimension(1, 22)); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(theme.isDark() ? new Color(244, 239, 230, 45)
                        : new Color(127, 122, 117, 45));
                int x = getWidth() / 2;
                g.drawLine(x, 2, x, getHeight() - 2);
                g.dispose();
        }
    }
}
