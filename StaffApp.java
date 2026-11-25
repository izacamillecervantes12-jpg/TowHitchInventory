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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

public class StaffApp {
    private static final Color GREEN = new Color(0x16A34A);
    private static final Color GREEN_HOVER = new Color(0x15803D);
    private static final Color RED = new Color(0xDC2626);
    private static final Color RED_HOVER = new Color(0xB91C1C);
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
    private final ImageIcon productionIcon = TowHitchCore.loadScaledImageAsset("production_icon.png", 20, 20);
    private final ImageIcon historyIcon = TowHitchCore.loadScaledImageAsset("history_icon.png", 20, 20);
    private final ImageIcon notificationIcon = TowHitchCore.loadScaledImageAsset("notification_icon.png", 24, 24);
    private final ImageIcon settingsIcon = TowHitchCore.loadScaledImageAsset("settings_icon.png", 24, 24);
    private final String[] pages = {"Dashboard", "Production", "Unfinished Goods",
            "Stock Requests", "Customer Orders", "History / Archive"};
    private JFrame frame;
    private JPanel pageHost;
    private TowHitchCore.UserAccount activeUser;
    private String currentPage = "Dashboard";
    private String searchText = "";
    private LoginBackgroundPanel loginRoot;
    private JLabel loginTitle;
    private JLabel loginSubtitle;
    private JLabel loginMessage;
    private JLabel loginFooter;
    private JPopupMenu notificationPopup;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StaffApp().start());
    }

    private void start() {
        try {
            TowHitchCore.ensureUsersFile();
            TowHitchCore.loadInventory();
            TowHitchCore.loadStockRequests();
            TowHitchCore.loadCustomerOrders();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Unable to initialize portal data: "
                    + e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        frame = new JFrame("Staff Portal | Tow Hitch by 5-21");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1100, 700));
        frame.setSize(1280, 820);
        frame.setLocationRelativeTo(null);
        showLogin();
        frame.setVisible(true);
    }

    private void showLogin() {
        LoginBackgroundPanel root = new LoginBackgroundPanel();
        loginRoot = root;
        JPanel card = new GlassLoginCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 61, 20, 61));
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        JLabel logo = new JLabel(loginLogo, SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(64, 64));
        loginRow(content, logo, 0, 0, 5);
        loginTitle = loginHeading("Welcome Back", 34, true);
        loginRow(content, loginTitle, 1, 0, 4);
        loginSubtitle = loginHeading("Access your staff dashboard", 18, false);
        loginRow(content, loginSubtitle, 2, 0, 10);
        JTextField username = loginField("Enter your username");
        JPasswordField password = loginPassword("Enter password");
        loginRow(content, loginLabel("Username"), 3, 10, 4);
        loginRow(content, username, 4, 0, 12);
        loginRow(content, loginLabel("Password"), 5, 0, 4);
        loginRow(content, password, 6, 0, 8);
        loginMessage = loginHeading(" ", 12, false);
        loginMessage.setForeground(theme.danger());
        loginRow(content, loginMessage, 7, 0, 7);
        TowHitchCore.RoundedButton signIn = new TowHitchCore.RoundedButton(
                "Log in", theme.primaryButton(), Color.WHITE, 12, theme);
        signIn.setFont(new Font("SansSerif", Font.BOLD, 14));
        signIn.setPreferredSize(new Dimension(394, 52));
        signIn.addActionListener(event -> authenticate(username, password));
        password.addActionListener(event -> authenticate(username, password));
        loginRow(content, signIn, 8, 8, 8);
        content.setPreferredSize(new Dimension(394, content.getPreferredSize().height));
        card.add(Box.createVerticalGlue());
        card.add(content);
        loginFooter = loginHeading("Tow Hitch by 5-21 | Staff Portal", 16, false);
        card.add(Box.createVerticalGlue());
        card.add(loginFooter);
        card.setPreferredSize(new Dimension(516, 570));
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
        c.anchor = GridBagConstraints.CENTER;
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

    private JTextField loginField(String placeholder) {
        return new LoginTextField(placeholder);
    }

    private JPasswordField loginPassword(String placeholder) {
        return new LoginPasswordField(placeholder);
    }

    private void authenticate(JTextField username, JPasswordField password) {
        try {
            String name = username.getText().trim();
            String secret = new String(password.getPassword());
            for (TowHitchCore.UserAccount user : TowHitchCore.loadUsers()) {
                if (user.username.equals(name) && user.password.equals(secret)) {
                    if (!"STAFF".equals(user.role)) {
                        loginMessage.setText("<html><div style='width:380px;text-align:center'>"
                                + "Access denied: This portal is restricted to Staff accounts only."
                                + "</div></html>");
                        return;
                    }
                    activeUser = user;
                    buildApplication();
                    return;
                }
            }
            loginMessage.setText("Invalid username or password.");
        } catch (IOException e) {
            loginMessage.setText("Unable to read users.txt: " + e.getMessage());
        }
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
        pageHost.add(buildDashboard(), "Dashboard");
        pageHost.add(buildProductionPage(), "Production");
        pageHost.add(buildUnfinishedPage(), "Unfinished Goods");
        pageHost.add(buildStockRequestsPage(), "Stock Requests");
        pageHost.add(buildCustomerOrdersPage(), "Customer Orders");
        pageHost.add(buildHistoryPage(), "History / Archive");
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
        for (String page : pages) {
            boolean selected = page.equals(currentPage);
            Color foreground = selected ? Color.WHITE : theme.secondaryText();
            TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(page,
                    selected ? theme.primaryButton() : theme.sidebar(), foreground, 12, theme);
            button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 13));
            button.setPreferredSize(new Dimension(184, 42));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            ImageIcon icon = iconFor(page);
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
            currentPage = "Dashboard";
            showLogin();
        });
        JPanel logoutHolder = new JPanel(new GridBagLayout());
        logoutHolder.setOpaque(false);
        logoutHolder.add(logout);
        sidebar.add(logoutHolder, BorderLayout.SOUTH);
        return sidebar;
    }

    private ImageIcon iconFor(String page) {
        if ("Dashboard".equals(page)) return dashboardIcon;
        if ("Production".equals(page) || "Stock Requests".equals(page)
                || "Customer Orders".equals(page)) return productionIcon;
        if ("History / Archive".equals(page)) return historyIcon;
        return inventoryIcon;
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
        menu.addActionListener(event -> navigate("Dashboard"));
        searchArea.add(menu, BorderLayout.WEST);
        RoundedSearchPanel searchPanel = new RoundedSearchPanel();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.setPreferredSize(new Dimension(360, 42));
        TowHitchCore.RoundedTextField search = new TowHitchCore.RoundedTextField("Search", 24, theme);
        search.setText(searchText);
        search.setOpaque(false);
        search.addActionListener(event -> {
            searchText = search.getText().trim();
            for (TowHitchCore.InventoryItem item : safeInventory()) {
                if (item.itemName.toLowerCase().contains(searchText.toLowerCase())) {
                    navigate(("F".equals(item.type) ? "Production" : "Unfinished Goods"));
                    return;
                }
            }
        });
        searchPanel.add(buildSearchLeading(), BorderLayout.WEST);
        searchPanel.add(search, BorderLayout.CENTER);
        searchArea.add(searchPanel, BorderLayout.CENTER);
        top.add(searchArea, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        actions.setOpaque(false);
        JLabel role = new JLabel("STAFF \u2022 " + activeUser.fullName);
        role.setFont(new Font("SansSerif", Font.BOLD, 12));
        role.setForeground(theme.success());
        actions.add(role);
        ThemeSwitchButton themeButton = new ThemeSwitchButton();
        actions.add(themeButton);
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

    private JPanel buildDashboard() {
        JPanel page = pageShell("Staff Dashboard", "Production, stock requests, and customer orders.");
        List<TowHitchCore.InventoryItem> inventory = safeInventory();
        int lowMaterials = 0;
        int finished = 0;
        for (TowHitchCore.InventoryItem item : inventory) {
            if ("F".equals(item.type)) finished += item.quantity;
            else if (item.quantity <= item.minAlert) lowMaterials++;
        }
        int myRequests = 0;
        int pendingOrders = 0;
        try {
            for (TowHitchCore.StockRequest request : TowHitchCore.loadStockRequests()) {
                if ("PENDING".equals(request.status)
                        && request.requestedBy.equalsIgnoreCase(activeUser.fullName)) myRequests++;
            }
            for (TowHitchCore.CustomerOrder order : TowHitchCore.loadCustomerOrders()) {
                if ("PENDING".equals(order.status)) pendingOrders++;
            }
        } catch (IOException e) {
            showDataError("Unable to load dashboard transactions", e);
        }
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(summaryCard("Low-Stock Unfinished Materials", Integer.toString(lowMaterials)));
        cards.add(summaryCard("Finished Goods On Hand", Integer.toString(finished)));
        cards.add(summaryCard("My Pending Stock Requests", Integer.toString(myRequests)));
        cards.add(summaryCard("Pending Customer Orders", Integer.toString(pendingOrders)));
        body.add(cards, BorderLayout.NORTH);
        body.add(lowStocksCard(inventory), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel summaryCard(String title, String value) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(18);
        card.setBackground(theme.panel());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel caption = textLabel(title, 12, false, theme.secondaryText());
        JLabel amount = textLabel(value, 28, true, theme.text());
        card.add(caption);
        card.add(Box.createVerticalStrut(12));
        card.add(amount);
        return card;
    }

    private TowHitchCore.RoundedPanel lowStocksCard(List<TowHitchCore.InventoryItem> inventory) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(18);
        card.setBackground(theme.panel());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.setLayout(new BorderLayout(0, 10));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = textLabel("\u26A0  Low Stocks Alert", 18, true, theme.text());
        JLabel subtitle = textLabel("Items at or below their configured minimum alert level",
                12, false, theme.secondaryText());
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);
        card.add(heading, BorderLayout.NORTH);
        JPanel rows = verticalPanel();
        int count = 0;
        for (TowHitchCore.InventoryItem item : inventory) {
            if (item.quantity <= item.minAlert) {
                rows.add(alertRow(item));
                rows.add(Box.createVerticalStrut(5));
                count++;
            }
        }
        if (count == 0) rows.add(textLabel("No low-stock items.", 13, false, theme.success()));
        card.add(scroll(rows), BorderLayout.CENTER);
        return card;
    }

    private JPanel alertRow(TowHitchCore.InventoryItem item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, theme.row()));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
        left.setOpaque(false);
        left.add(textLabel(item.itemName, 13, true, theme.text()));
        left.add(textLabel("F".equals(item.type) ? "Finished" : "Unfinished",
                11, false, theme.secondaryText()));
        row.add(left, BorderLayout.WEST);
        JLabel quantity = textLabel(item.quantity + " stocks (Min: " + item.minAlert + ")",
                12, true, theme.danger());
        row.add(quantity, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return row;
    }

    private JPanel buildProductionPage() {
        JPanel panel = pageShell("Production", "Assemble finished goods using their configured BOM recipe.");
        JPanel rows = verticalPanel();
        List<TowHitchCore.InventoryItem> inventory = safeInventory();
        Map<String, List<TowHitchCore.BOMItem>> bom;
        try {
            bom = TowHitchCore.loadBOM();
        } catch (IOException e) {
            showDataError("Unable to load BOM data", e);
            bom = new LinkedHashMap<>();
        }
        int count = 0;
        for (TowHitchCore.InventoryItem product : inventory) {
            if (!"F".equals(product.type)) continue;
            if (!matchesSearch(product.itemName)) continue;
            rows.add(productionCard(product, inventory, bom.getOrDefault(product.itemName, new ArrayList<>())));
            rows.add(Box.createVerticalStrut(10));
            count++;
        }
        if (count == 0) rows.add(textLabel("No finished goods are available for production.", 14,
                false, theme.secondaryText()));
        panel.add(scroll(rows), BorderLayout.CENTER);
        return panel;
    }

    private JPanel productionCard(TowHitchCore.InventoryItem product,
                                  List<TowHitchCore.InventoryItem> inventory,
                                  List<TowHitchCore.BOMItem> recipe) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(16);
        card.setBackground(theme.panel());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setLayout(new BorderLayout(12, 8));
        JPanel information = new JPanel();
        information.setOpaque(false);
        information.setLayout(new BoxLayout(information, BoxLayout.Y_AXIS));
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        heading.setOpaque(false);
        heading.add(textLabel(product.itemName, 16, true, theme.text()));
        heading.add(textLabel("On Hand: " + product.quantity + "   Min Alert: " + product.minAlert,
                12, false, theme.secondaryText()));
        heading.add(new TowHitchCore.StatusPillBadge(stockStatus(product), theme));
        information.add(heading);
        information.add(Box.createVerticalStrut(9));
        List<JLabel> recipeLabels = new ArrayList<>();
        if (recipe.isEmpty()) {
            information.add(textLabel("BOM: No recipe configured.", 12, false, theme.secondaryText()));
        } else {
            for (TowHitchCore.BOMItem component : recipe) {
                JLabel componentLabel = textLabel("", 12, false, theme.secondaryText());
                recipeLabels.add(componentLabel);
                information.add(componentLabel);
            }
        }
        card.add(information, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        quantity.setPreferredSize(new Dimension(76, 34));
        Runnable refreshRecipe = () -> {
            long batch = ((Number) quantity.getValue()).longValue();
            Map<String, Long> requiredByMaterial = new LinkedHashMap<>();
            for (TowHitchCore.BOMItem component : recipe) {
                requiredByMaterial.merge(component.materialName.toLowerCase(Locale.ROOT),
                        (long) component.quantityRequired * batch, Long::sum);
            }
            for (int i = 0; i < recipeLabels.size(); i++) {
                TowHitchCore.BOMItem component = recipe.get(i);
                long required = requiredByMaterial.get(component.materialName.toLowerCase(Locale.ROOT));
                TowHitchCore.InventoryItem material =
                        findInventory(inventory, component.materialName, false);
                boolean enough = material != null && material.quantity >= required;
                String detail = material == null ? "missing"
                        : "on hand: " + material.quantity + ", needed: " + required;
                recipeLabels.get(i).setText(component.quantityRequired + " \u00D7 "
                        + component.materialName + " (" + detail + ")");
                recipeLabels.get(i).setForeground(enough ? theme.secondaryText() : theme.danger());
            }
        };
        quantity.addChangeListener(event -> refreshRecipe.run());
        refreshRecipe.run();
        actions.add(quantity);
        TowHitchCore.RoundedButton produce = actionButton("Produce", GREEN, Color.WHITE);
        produce.setPreferredSize(new Dimension(112, 38));
        produce.addActionListener(event -> produce(product.itemName,
                ((Number) quantity.getValue()).intValue()));
        actions.add(produce);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private void produce(String productName, int batchQuantity) {
        if (batchQuantity < 1) return;
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            Map<String, List<TowHitchCore.BOMItem>> bom = TowHitchCore.loadBOM();
            List<TowHitchCore.BOMItem> recipe = bom.getOrDefault(productName, new ArrayList<>());
            if (recipe.isEmpty()) {
                error("No BOM recipe is configured for " + productName + ".");
                return;
            }
            Map<String, Long> deductions = new LinkedHashMap<>();
            Map<String, String> materialNames = new LinkedHashMap<>();
            for (TowHitchCore.BOMItem component : recipe) {
                if (component.quantityRequired < 0 || !validField(component.materialName)) {
                    error("The BOM contains an invalid material or quantity.");
                    return;
                }
                long total = (long) component.quantityRequired * batchQuantity;
                if (total > Integer.MAX_VALUE) {
                    error("The requested batch quantity is too large.");
                    return;
                }
                String key = component.materialName.toLowerCase(Locale.ROOT);
                long combined = deductions.getOrDefault(key, 0L) + total;
                if (combined > Integer.MAX_VALUE) {
                    error("The requested batch quantity is too large.");
                    return;
                }
                deductions.put(key, combined);
                materialNames.putIfAbsent(key, component.materialName);
            }
            for (Map.Entry<String, Long> entry : deductions.entrySet()) {
                String materialName = materialNames.get(entry.getKey());
                TowHitchCore.InventoryItem material = null;
                int matchCount = 0;
                for (TowHitchCore.InventoryItem candidate : inventory) {
                    if (!"F".equals(candidate.type)
                            && candidate.itemName.equalsIgnoreCase(materialName)) {
                        material = candidate;
                        matchCount++;
                    }
                }
                if (matchCount != 1) {
                    error(matchCount == 0 ? "Raw material not found: " + materialName + "."
                            : "Inventory has duplicate entries for " + materialName + ".");
                    return;
                }
                if (material.quantity < entry.getValue()) {
                    error("Insufficient raw material: " + materialName + ".");
                    return;
                }
            }
            int productCount = 0;
            TowHitchCore.InventoryItem selectedProduct = null;
            for (TowHitchCore.InventoryItem item : inventory) {
                if ("F".equals(item.type) && item.itemName.equalsIgnoreCase(productName)) {
                    productCount++;
                    selectedProduct = item;
                }
            }
            if (productCount != 1) {
                error(productCount == 0 ? "The selected finished good no longer exists."
                        : "Inventory has duplicate entries for " + productName + ".");
                return;
            }
            if ((long) selectedProduct.quantity + batchQuantity > Integer.MAX_VALUE) {
                error("Finished goods quantity would exceed the supported limit.");
                return;
            }
            List<TowHitchCore.InventoryItem> updated = new ArrayList<>();
            for (TowHitchCore.InventoryItem item : inventory) {
                int quantity = item.quantity;
                if (!"F".equals(item.type)) {
                    for (Map.Entry<String, Long> entry : deductions.entrySet()) {
                        if (item.itemName.equalsIgnoreCase(materialNames.get(entry.getKey()))) {
                            quantity -= entry.getValue().intValue();
                            break;
                        }
                    }
                }
                if ("F".equals(item.type) && item.itemName.equalsIgnoreCase(productName)) {
                    quantity += batchQuantity;
                }
                updated.add(new TowHitchCore.InventoryItem(item.type, item.itemName, quantity, item.minAlert));
            }
            TowHitchCore.saveInventory(updated);
            TowHitchCore.appendHistory("Produced: " + productName + " (" + batchQuantity + ")");
            for (Map.Entry<String, Long> entry : deductions.entrySet()) {
                TowHitchCore.appendHistory("Consumed for " + productName + ": "
                        + materialNames.get(entry.getKey()) + " (" + entry.getValue() + ")");
            }
            currentPage = "Production";
            buildApplication();
            JOptionPane.showMessageDialog(frame, "Production batch recorded successfully.",
                    "Production", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            showDataError("Unable to save production batch", e);
        }
    }

    private JPanel buildUnfinishedPage() {
        JPanel page = pageShell("Unfinished Goods", "View raw materials and request restocks from the Owner.");
        TowHitchCore.RoundedButton create = actionButton("+ New Stock Request",
                theme.primaryButton(), Color.WHITE);
        create.addActionListener(event -> openRequestDialog(null));
        JPanel header = (JPanel) page.getComponent(0);
        header.add(create, BorderLayout.EAST);
        JPanel rows = verticalPanel();
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        list.add(tableHeader(new String[] {"Status", "Material Name", "On Hand", "Min Alert", "Action"},
                new double[] {.18, .32, .16, .16, .18}), BorderLayout.NORTH);
        JPanel data = verticalPanel();
        for (TowHitchCore.InventoryItem item : safeInventory()) {
            if ("F".equals(item.type) || !matchesSearch(item.itemName)) continue;
            data.add(materialRow(item));
            data.add(Box.createVerticalStrut(7));
        }
        if (data.getComponentCount() == 0) data.add(textLabel("No unfinished goods found.", 13,
                false, theme.secondaryText()));
        list.add(scroll(data), BorderLayout.CENTER);
        rows.add(list);
        page.add(rows, BorderLayout.CENTER);
        return page;
    }

    private JPanel materialRow(TowHitchCore.InventoryItem item) {
        JPanel row = rowPanel();
        addCell(row, new TowHitchCore.StatusPillBadge(stockStatus(item), theme), 0, .18);
        addCell(row, tableCell(item.itemName), 1, .32);
        addCell(row, tableCell(Integer.toString(item.quantity)), 2, .16);
        addCell(row, tableCell(Integer.toString(item.minAlert)), 3, .16);
        TowHitchCore.RoundedButton request = actionButton("Request Restock",
                theme.primaryButton(), Color.WHITE);
        request.addActionListener(event -> openRequestDialog(item.itemName));
        addCell(row, request, 4, .18);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        return row;
    }

    private void openRequestDialog(String materialName) {
        JDialog dialog = themedDialog("New Stock Request", 420, 290);
        JPanel content = dialogContent();
        JTextField material = dialogField(materialName == null ? "" : materialName);
        if (materialName != null) material.setEditable(false);
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
        addFormRow(content, "Material Name", material);
        addFormRow(content, "Requested Quantity", quantity);
        dialog.add(content, BorderLayout.CENTER);
        JPanel actions = dialogActions();
        TowHitchCore.RoundedButton cancel = actionButton("Cancel", theme.sidebar(), theme.text());
        cancel.addActionListener(event -> dialog.dispose());
        TowHitchCore.RoundedButton submit = actionButton("Submit Request", theme.primaryButton(), Color.WHITE);
        submit.addActionListener(event -> {
            String name = material.getText().trim();
            if (!validField(name) || ((Number) quantity.getValue()).intValue() < 1) {
                error("Enter a material name and a quantity greater than zero.");
                return;
            }
            try {
                boolean exists = false;
                for (TowHitchCore.InventoryItem item : TowHitchCore.loadInventory()) {
                    if (!"F".equals(item.type) && item.itemName.equalsIgnoreCase(name)) exists = true;
                }
                if (!exists) {
                    error("That unfinished material does not exist.");
                    return;
                }
                List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
                String id = "SR-" + System.currentTimeMillis();
                requests.add(new TowHitchCore.StockRequest(id, now(), activeUser.fullName,
                        name, ((Number) quantity.getValue()).intValue(), "PENDING"));
                TowHitchCore.saveStockRequests(requests);
                TowHitchCore.appendHistory("Requested restock: "
                        + ((Number) quantity.getValue()).intValue() + " " + name
                        + " - Requested by " + activeUser.fullName);
                dialog.dispose();
                currentPage = "Stock Requests";
                buildApplication();
            } catch (IOException e) {
                showDataError("Unable to save stock request", e);
            }
        });
        actions.add(cancel);
        actions.add(submit);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel buildStockRequestsPage() {
        JPanel page = pageShell("Stock Requests", "Track Owner decisions on your material restock requests.");
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        double[] weights = {.17, .24, .08, .22, .29};
        list.add(tableHeader(new String[] {"Status", "Material", "Qty", "Requested By", "Date"}, weights),
                BorderLayout.NORTH);
        JPanel rows = verticalPanel();
        try {
            List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
            requests.sort(Comparator.comparing((TowHitchCore.StockRequest r) ->
                    !"PENDING".equals(r.status)).thenComparing((a, b) ->
                    b.timestamp.compareTo(a.timestamp)));
            for (TowHitchCore.StockRequest request : requests) {
                JPanel row = rowPanel();
                addCell(row, new TowHitchCore.StatusPillBadge(request.status, theme), 0, weights[0]);
                addCell(row, tableCell(request.materialName), 1, weights[1]);
                addCell(row, tableCell(Integer.toString(request.quantity)), 2, weights[2]);
                addCell(row, tableCell(request.requestedBy), 3, weights[3]);
                addCell(row, tableCell(request.timestamp), 4, weights[4]);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
                rows.add(row);
                rows.add(Box.createVerticalStrut(7));
            }
        } catch (IOException e) {
            showDataError("Unable to load stock requests", e);
        }
        if (rows.getComponentCount() == 0) rows.add(textLabel("No stock requests yet.", 13,
                false, theme.secondaryText()));
        list.add(scroll(rows), BorderLayout.CENTER);
        page.add(list, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildCustomerOrdersPage() {
        JPanel page = pageShell("Customer Orders", "Release available orders or cancel pending orders.");
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        double[] weights = {.12, .17, .06, .16, .12, .37};
        list.add(tableHeader(new String[] {"Status", "Product", "Qty", "Customer", "Order Date", "Actions"},
                weights), BorderLayout.NORTH);
        JPanel rows = verticalPanel();
        try {
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            orders.sort(Comparator.comparing((TowHitchCore.CustomerOrder o) ->
                    !"PENDING".equals(o.status)).thenComparing((a, b) ->
                    b.timestamp.compareTo(a.timestamp)));
            for (TowHitchCore.CustomerOrder order : orders) {
                rows.add(orderRow(order, weights));
                rows.add(Box.createVerticalStrut(7));
            }
        } catch (IOException e) {
            showDataError("Unable to load customer orders", e);
        }
        if (rows.getComponentCount() == 0) rows.add(textLabel("No customer orders.", 13,
                false, theme.secondaryText()));
        list.add(scroll(rows), BorderLayout.CENTER);
        page.add(list, BorderLayout.CENTER);
        return page;
    }

    private JPanel orderRow(TowHitchCore.CustomerOrder order, double[] weights) {
        JPanel row = rowPanel();
        addCell(row, new TowHitchCore.StatusPillBadge(order.status, theme), 0, weights[0]);
        addCell(row, tableCell(order.productName), 1, weights[1]);
        addCell(row, tableCell(Integer.toString(order.quantity)), 2, weights[2]);
        addCell(row, tableCell(order.customerName), 3, weights[3]);
        addCell(row, tableCell(order.timestamp), 4, weights[4]);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actions.setOpaque(false);
        if ("PENDING".equals(order.status)) {
            TowHitchCore.RoundedButton release = actionButton("Confirm & Release", GREEN, Color.WHITE);
            release.addActionListener(event -> releaseOrder(order));
            TowHitchCore.RoundedButton cancel = actionButton("Cancel Order", RED, Color.WHITE);
            cancel.addActionListener(event -> cancelOrder(order));
            actions.add(release);
            actions.add(cancel);
        }
        addCell(row, actions, 5, weights[5]);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private void releaseOrder(TowHitchCore.CustomerOrder selected) {
        int answer = JOptionPane.showConfirmDialog(frame,
                "Confirm and release " + selected.quantity + " x " + selected.productName
                        + " for " + selected.customerName + "?",
                "Confirm Order", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            List<TowHitchCore.InventoryItem> updated = new ArrayList<>();
            boolean found = false;
            for (TowHitchCore.InventoryItem item : inventory) {
                if ("F".equals(item.type) && item.itemName.equalsIgnoreCase(selected.productName)) {
                    if (item.quantity < selected.quantity) {
                        error("Insufficient Finished Goods stock to fulfill this order.");
                        return;
                    }
                    updated.add(new TowHitchCore.InventoryItem(item.type, item.itemName,
                            item.quantity - selected.quantity, item.minAlert));
                    found = true;
                } else updated.add(item);
            }
            if (!found) {
                error("The ordered Finished Good no longer exists.");
                return;
            }
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            boolean orderFound = false;
            for (TowHitchCore.CustomerOrder order : orders) {
                if (order.orderId.equals(selected.orderId) && "PENDING".equals(order.status)) {
                    order.setStatus("COMPLETED");
                    orderFound = true;
                }
            }
            if (!orderFound) {
                error("This order is no longer pending.");
                return;
            }
            TowHitchCore.saveInventory(updated);
            TowHitchCore.saveCustomerOrders(orders);
            TowHitchCore.appendHistory("Completed customer order: " + selected.orderId + " - "
                    + selected.quantity + " " + selected.productName + " for "
                    + selected.customerName + " - Released by Staff " + activeUser.fullName);
            buildApplication();
        } catch (IOException e) {
            showDataError("Unable to release customer order", e);
        }
    }

    private void cancelOrder(TowHitchCore.CustomerOrder selected) {
        int answer = JOptionPane.showConfirmDialog(frame,
                "Cancel customer order " + selected.orderId + "?",
                "Cancel Order", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            boolean found = false;
            for (TowHitchCore.CustomerOrder order : orders) {
                if (order.orderId.equals(selected.orderId) && "PENDING".equals(order.status)) {
                    order.setStatus("CANCELLED");
                    found = true;
                }
            }
            if (!found) {
                error("This order is no longer pending.");
                return;
            }
            TowHitchCore.saveCustomerOrders(orders);
            TowHitchCore.appendHistory("Cancelled customer order: " + selected.orderId
                    + " - Cancelled by Staff " + activeUser.fullName);
            buildApplication();
        } catch (IOException e) {
            showDataError("Unable to cancel customer order", e);
        }
    }

    private JPanel buildHistoryPage() {
        JPanel page = pageShell("History / Archive",
                "Production, material consumption, restock, and order records.");
        JPanel header = (JPanel) page.getComponent(0);
        TowHitchCore.RoundedTextField search =
                new TowHitchCore.RoundedTextField("Search audit log", 20, theme);
        search.setPreferredSize(new Dimension(240, 42));
        header.add(search, BorderLayout.EAST);
        JPanel columns = new JPanel(new GridLayout(1, 2, 14, 0));
        columns.setOpaque(false);
        Runnable refresh = () -> {
            columns.removeAll();
            List<HistoryRecord> products = new ArrayList<>();
            List<HistoryRecord> materials = new ArrayList<>();
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            for (HistoryRecord record : readHistory()) {
                if (!query.isEmpty() && !record.message.toLowerCase(Locale.ROOT).contains(query)
                        && !record.display.toLowerCase(Locale.ROOT).contains(query)) continue;
                if (record.message.toLowerCase(Locale.ROOT).contains("produced:")
                        || record.message.toLowerCase(Locale.ROOT).contains("approved stock request")
                        || record.message.toLowerCase(Locale.ROOT).contains("completed customer order")) {
                    products.add(record);
                } else {
                    materials.add(record);
                }
            }
            columns.add(historyCard("Product & Order Logs", products));
            columns.add(historyCard("Material Consumption & Audit", materials));
            columns.revalidate();
            columns.repaint();
        };
        search.addActionListener(event -> refresh.run());
        refresh.run();
        page.add(columns, BorderLayout.CENTER);
        return page;
    }

    private List<HistoryRecord> readHistory() {
        List<HistoryRecord> result = new ArrayList<>();
        Path file = Paths.get("history_logs.txt").toAbsolutePath();
        if (!Files.isRegularFile(file)) return result;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                int tab = line.indexOf('\t');
                String stamp = tab > 0 ? line.substring(0, tab) : "";
                String message = tab > 0 ? line.substring(tab + 1) : line;
                if (line.startsWith("[") && line.contains("] ")) {
                    int end = line.indexOf("] ");
                    stamp = line.substring(1, end);
                    message = line.substring(end + 2);
                }
                String date = stamp;
                String time = "";
                int space = stamp.indexOf(' ');
                if (space > 0) {
                    date = stamp.substring(0, space);
                    time = stamp.substring(space + 1);
                }
                if (message.startsWith("Consumed for ")) {
                    int separator = message.indexOf(": ");
                    if (separator >= 0) {
                        for (String part : message.substring(separator + 2).split(",\\s*")) {
                            int open = part.lastIndexOf(" (");
                            if (open > 0 && part.endsWith(")")) {
                                result.add(new HistoryRecord(message,
                                        part.substring(open + 2, part.length() - 1)
                                                + " " + part.substring(0, open), date, time));
                            }
                        }
                        continue;
                    }
                }
                result.add(new HistoryRecord(message, formatHistoryDisplay(message), date, time));
            }

        } catch (IOException e) {
            showDataError("Unable to load history_logs.txt", e);
        }
        return result;
    }

    private String formatHistoryDisplay(String message) {
        if (message.startsWith("Produced: ")) {
            String produced = message.substring("Produced: ".length());
            int open = produced.lastIndexOf(" (");
            if (open > 0 && produced.endsWith(")")) {
                return produced.substring(open + 2, produced.length() - 1) + " "
                        + produced.substring(0, open) + " produced";
            }
        }
        if (message.startsWith("Approved stock request: ")) {
            String request = message.substring("Approved stock request: ".length());
            int by = request.indexOf(" requested by ");
            if (by > 0) return request.substring(0, by) + " restocked";
        }
        if (message.startsWith("Completed customer order: ")) {
            String order = message.substring("Completed customer order: ".length());
            int separator = order.indexOf(" - ");
            if (separator >= 0) {
                int forCustomer = order.indexOf(" for ", separator + 3);
                String productAndQuantity = forCustomer > separator
                        ? order.substring(separator + 3, forCustomer)
                        : order.substring(separator + 3);
                return productAndQuantity + " completed";
            }
        }
        return message;
    }

    private JPanel historyCard(String title, List<HistoryRecord> records) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(18);
        card.setBackground(theme.panel());
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.setLayout(new BorderLayout(0, 10));
        card.add(textLabel(title, 16, true, theme.text()), BorderLayout.NORTH);
        JPanel rows = verticalPanel();
        for (HistoryRecord record : records) {
            JPanel pill = new TowHitchCore.RoundedPanel(12);
            pill.setBackground(theme.row());
            pill.setBorder(BorderFactory.createEmptyBorder(9, 11, 9, 11));
            pill.setLayout(new BorderLayout(8, 0));
            JLabel message = textLabel(record.display, 12, false, theme.text());
            pill.add(message, BorderLayout.CENTER);
            pill.add(textLabel(record.date + (record.time.isEmpty() ? "" : " | " + record.time),
                    11, false, theme.secondaryText()), BorderLayout.EAST);
            pill.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            rows.add(pill);
            rows.add(Box.createVerticalStrut(6));
        }
        if (records.isEmpty()) rows.add(textLabel("No records yet.", 12, false, theme.secondaryText()));
        card.add(scroll(rows), BorderLayout.CENTER);
        return card;
    }

    private JPanel pageShell(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout(8, 0));
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
        Color hover = fill.equals(GREEN) ? GREEN_HOVER : fill.equals(RED) ? RED_HOVER : null;
        TowHitchCore.RoundedButton button = hover == null
                ? new TowHitchCore.RoundedButton(text, fill, foreground, 12, theme)
                : new TowHitchCore.RoundedButton(text, fill, foreground, 12, theme) {
                    private static final long serialVersionUID = 1L;
                    @Override public Color getBackground() {
                        return getModel().isRollover() ? hover : fill;
                    }
                };
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setMargin(new Insets(5, 10, 5, 10));
        return button;
    }

    private JDialog themedDialog(String title, int width, int height) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(frame);
        dialog.getContentPane().setBackground(theme.workspaceCanvas());
        dialog.setLayout(new BorderLayout(0, 12));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(16, 18, 0, 18));
        heading.add(textLabel(title, 18, true, theme.text()));
        dialog.add(heading, BorderLayout.NORTH);
        return dialog;
    }

    private JPanel dialogContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        return panel;
    }

    private JTextField dialogField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(theme.text());
        field.setBackground(theme.input());
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(theme.row()),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setPreferredSize(new Dimension(220, 38));
        return field;
    }

    private void addFormRow(JPanel panel, String label, Component field) {
        int row = panel.getComponentCount() / 2;
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(6, 0, 6, 12);
        panel.add(textLabel(label, 12, true, theme.text()), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(6, 0, 6, 0);
        panel.add(field, right);
    }

    private JPanel dialogActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 14, 16));
        return panel;
    }

    private String stockStatus(TowHitchCore.InventoryItem item) {
        return item.quantity <= item.minAlert ? "LOW STOCK" : "IN STOCK";
    }

    private TowHitchCore.InventoryItem findInventory(List<TowHitchCore.InventoryItem> items,
                                                       String name, boolean finished) {
        for (TowHitchCore.InventoryItem item : items) {
            boolean isFinished = "F".equals(item.type);
            if (isFinished == finished && item.itemName.equalsIgnoreCase(name)) return item;
        }
        return null;
    }

    private boolean matchesSearch(String name) {
        return searchText.isEmpty() || name.toLowerCase().contains(searchText.toLowerCase());
    }

    private boolean validField(String value) {
        return value != null && !value.trim().isEmpty()
                && value.indexOf('\t') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0;
    }

    private String now() {
        return LocalDateTime.now().format(TIMESTAMP);
    }

    private List<TowHitchCore.InventoryItem> safeInventory() {
        try {
            return TowHitchCore.loadInventory();
        } catch (IOException e) {
            showDataError("Unable to load inventory_data.txt", e);
            return new ArrayList<>();
        }
    }

    private int lowStockCount() {
        int count = 0;
        for (TowHitchCore.InventoryItem item : safeInventory()) {
            if (item.quantity <= item.minAlert) count++;
        }
        return count;
    }

    private void showNotifications(Component invoker) {
        if (notificationPopup != null && notificationPopup.isVisible()) {
            notificationPopup.setVisible(false);
            return;
        }
        JPopupMenu popup = new RoundedPopup();
        JPanel content = verticalPanel();
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        heading.setOpaque(false);
        JLabel dot = textLabel("\u25CF", 14, true, theme.danger());
        heading.add(dot);
        heading.add(textLabel("Low Stocks Alert", 14, true, theme.text()));
        content.add(heading);
        int count = 0;
        for (TowHitchCore.InventoryItem item : safeInventory()) {
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
            currentPage = "Dashboard";
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

        private void refreshLoginTheme() {
            loginTitle.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
            loginSubtitle.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x625E5C));
            loginFooter.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x686361));
            loginMessage.setForeground(theme.danger());
            updateLoginLabels(loginRoot);
            loginRoot.repaint();
        }

        private void updateLoginLabels(Component component) {
            if (component instanceof JLabel && component != loginTitle && component != loginSubtitle
                    && component != loginFooter && component != loginMessage) {
                component.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
            }
            if (component instanceof java.awt.Container) {
                for (Component child : ((java.awt.Container) component).getComponents()) {
                    updateLoginLabels(child);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
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
            g.drawString("\u2600", position < .5f ? 11 : 11, 28);
            g.setColor(theme.isDark() ? new Color(0xF5E9B8) : new Color(0x8B949E));
            g.drawString("\u263E", position < .5f ? width - 25 : width - 25, 28);
            g.dispose();
        }
    }

    private class LoginBackgroundPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private JPanel card;
        private JButton switchButton;

        private LoginBackgroundPanel() {
            setOpaque(true);
            setLayout(null);
        }

        private void setContent(JPanel card, JButton button) {
            this.card = card;
            this.switchButton = button;
            add(card);
            add(button);
        }

        @Override
        public void doLayout() {
            if (card != null) {
                int width = Math.min(516, getWidth() - 32);
                int height = Math.min(card.getPreferredSize().height, getHeight() - 32);
                card.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
            }
            if (switchButton != null) switchButton.setBounds(getWidth() - 106, 16, 84, 40);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
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

    private static final class HistoryRecord {
        private final String message;
        private final String display;
        private final String date;
        private final String time;
        private HistoryRecord(String message, String display, String date, String time) {
            this.message = message;
            this.display = display;
            this.date = date;
            this.time = time;
        }
    }
}
