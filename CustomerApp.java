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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class CustomerApp {
    private static final Color GREEN = new Color(0x2E6F40);
    private static final Color RED = new Color(0xA63A3A);
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
    private JPanel sidebarPanel;
    private TowHitchCore.FlatIconButton headerMenuButton;
    private JPanel topBar;
    private Timer sidebarAnimation;
    private int sidebarWidth = 255;
    private String currentPage = "Order Catalog";
    private String searchText = "";
    private boolean sidebarVisible = true;
    private boolean compactView;
    private boolean notificationAlerts = true;
    private boolean autoRefreshInventory = true;
    private JLabel loginTitle;
    private JLabel loginSubtitle;
    private JLabel loginMessage;
    private JLabel loginFooter;
    private javax.swing.JWindow notificationPopup;
    private javax.swing.JWindow profilePopup;
    private TowHitchCore.AnchoredPopover anchoredPopover;
    private TowHitchCore.FlatIconButton notificationButton;
    private final Set<String> readNotificationIds = new HashSet<>();

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
        anchoredPopover = new TowHitchCore.AnchoredPopover(frame);
        TowHitchCore.installGlobalMouseHandling();
        showLogin();
        frame.setVisible(true);
    }

    private void showLogin() {
        LoginBackgroundPanel root = new LoginBackgroundPanel();
        GlassLoginCard card = new GlassLoginCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 36, 32, 36));
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        JLabel logo = new JLabel(loginLogo, SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(64, 64));
        loginRow(content, logo, 0, 0, 12);
        loginTitle = loginHeading("Welcome, Customer", 34, true);
        loginRow(content, loginTitle, 1, 0, 6);
        loginSubtitle = loginHeading(
                "Browse and order tow hitch products and accessories.", 15, false);
        loginRow(content, loginSubtitle, 2, 0, 26);

        JTextField username = loginTextField();
        JPasswordField password = loginPasswordField();
        loginRow(content, loginLabel("Username"), 3, 0, 6);
        loginRow(content, username, 4, 0, 16);
        loginRow(content, loginLabel("Password"), 5, 0, 6);
        loginRow(content, password, 6, 0, 24);
        loginMessage = loginHeading(" ", 12, false);
        loginMessage.setForeground(theme.danger());
        loginMessage.setVisible(false);
        loginRow(content, loginMessage, 7, 0, 6);

        TowHitchCore.RoundedButton signIn = new TowHitchCore.RoundedButton(
                "Log in", theme.primaryButton(), Color.WHITE, 12, theme);
        signIn.setFont(new Font("SansSerif", Font.BOLD, 14));
        signIn.setPreferredSize(new Dimension(395, 44));
        signIn.setMinimumSize(new Dimension(395, 44));
        signIn.setMaximumSize(new Dimension(395, 44));
        signIn.addActionListener(event -> authenticate(username, password));
        password.addActionListener(event -> authenticate(username, password));
        loginRow(content, signIn, 8, 0, 14);

        JButton signup = new JButton("Create Customer Account (Sign Up)");
        signup.setFont(new Font("SansSerif", Font.BOLD, 13));
        signup.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        signup.setOpaque(false);
        signup.setContentAreaFilled(false);
        signup.setBorderPainted(false);
        signup.setFocusPainted(false);
        signup.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        signup.addActionListener(event -> openSignUpDialog());
        signup.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        loginRow(content, signup, 9, 0, 0);
        content.setPreferredSize(new Dimension(395, content.getPreferredSize().height));
        JPanel centeredContent = centerLoginContent(content);
        card.add(centeredContent);
        loginFooter = loginHeading("Tow Hitch by 5-21 | Customer Portal", 16, false);
        loginFooter.setHorizontalAlignment(SwingConstants.CENTER);
        loginFooter.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        loginFooter.setPreferredSize(new Dimension(394, 42));
        loginFooter.setMaximumSize(new Dimension(394, 42));
        card.add(Box.createVerticalStrut(24));
        card.add(loginFooter);
        card.setPreferredSize(new Dimension(468,
                centeredContent.getPreferredSize().height + 24 + 42 + 36 + 32));
        root.setContent(card, new ThemeSwitchButton());
        frame.setContentPane(root);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel centerLoginContent(JPanel content) {
        Dimension contentSize = new Dimension(395, content.getPreferredSize().height);
        content.setPreferredSize(contentSize);
        content.setMinimumSize(contentSize);
        content.setMaximumSize(contentSize);
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.CENTER;
        wrapper.add(content, constraints);
        Dimension wrapperSize = new Dimension(396, contentSize.height);
        wrapper.setPreferredSize(wrapperSize);
        wrapper.setMinimumSize(wrapperSize);
        wrapper.setMaximumSize(wrapperSize);
        return wrapper;
    }

    private void loginRow(JPanel panel, Component component, int row, int top, int bottom) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        boolean formLabel = component instanceof JLabel
                && ((JLabel) component).getHorizontalAlignment() == SwingConstants.LEFT;
        boolean fixedControl = component instanceof JTextField
                || component instanceof JPasswordField
                || component instanceof JButton
                        && component.getPreferredSize().width == 395;
        c.weightx = fixedControl || formLabel ? 0 : 1;
        c.fill = fixedControl || formLabel
                ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        c.anchor = fixedControl || formLabel
                ? GridBagConstraints.WEST : GridBagConstraints.CENTER;
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
        label.setPreferredSize(new Dimension(395, 27));
        label.setMinimumSize(new Dimension(395, 27));
        label.setMaximumSize(new Dimension(395, 27));
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
                        showLoginMessage("<html><div style='width:380px;text-align:center'>"
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
            showLoginMessage("Invalid username or password.");
        } catch (IOException e) {
            showLoginMessage("Unable to read users.txt: " + e.getMessage());
        }
    }

    private void showLoginMessage(String message) {
        loginMessage.setText(message);
        loginMessage.setVisible(message != null && !message.trim().isEmpty());
        java.awt.Container card = SwingUtilities.getAncestorOfClass(GlassLoginCard.class,
                loginMessage);
        if (card != null) {
            card.setPreferredSize(new Dimension(468,
                    card.getLayout().preferredLayoutSize(card).height));
        }
        loginMessage.getParent().revalidate();
        loginMessage.getParent().repaint();
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
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder());
        sidebarPanel = buildSidebar();
        sidebarPanel.setPreferredSize(new Dimension(sidebarVisible ? sidebarWidth : 0, 100));
        sidebarPanel.setVisible(sidebarVisible || sidebarWidth > 0);
        pageHost = new JPanel(new CardLayout());
        pageHost.setOpaque(false);
        pageHost.add(buildCatalogPage(), "Order Catalog");
        pageHost.add(buildMyOrdersPage(), "My Orders");
        root.add(sidebarPanel, BorderLayout.WEST);
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(buildTopBar(), BorderLayout.NORTH);
        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setOpaque(false);
        pageContent.setBorder(compactView
                ? BorderFactory.createEmptyBorder(14, 18, 14, 18)
                : BorderFactory.createEmptyBorder(24, 28, 24, 28));
        pageContent.add(pageHost, BorderLayout.CENTER);
        content.add(pageContent, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        frame.setContentPane(root);
        ((CardLayout) pageHost.getLayout()).show(pageHost, currentPage);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel buildSidebar() {
        TowHitchCore.RoundedPanel sidebar = new TowHitchCore.RoundedPanel(0);
        sidebar.setPreferredSize(new Dimension(255, 100));
        sidebar.setBackground(theme.sidebar());
        sidebar.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        sidebar.setLayout(new BorderLayout(0, 0));
        JPanel upper = new JPanel(new BorderLayout());
        upper.setOpaque(false);
        ImageIcon brandLogo = theme.isDark() ? sidebarLogoDark : sidebarLogoLight;
        JPanel logoHero = new JPanel(new GridBagLayout());
        logoHero.setOpaque(false);
        logoHero.setBorder(BorderFactory.createEmptyBorder(36, 16, 36, 16));
        logoHero.setPreferredSize(new Dimension(255, 202));
        logoHero.setMinimumSize(new Dimension(255, 202));
        logoHero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 202));
        if (brandLogo != null) {
            JLabel logo = new JLabel(TowHitchCore.loadScaledImageAssetFit(
                    theme.isDark() ? "sidebar_logo_dark.png" : "sidebar_logo_light.png",
                    210, 130));
            logo.setHorizontalAlignment(SwingConstants.CENTER);
            logoHero.add(logo);
        }
        upper.add(logoHero, BorderLayout.NORTH);
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String page : new String[] {"Order Catalog", "My Orders"}) {
            boolean selected = page.equals(currentPage);
            Color foreground = selected ? Color.WHITE : theme.secondaryText();
            TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(page,
                    selected ? theme.primaryButton() : theme.sidebar(), foreground, 12, theme);
            button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 14));
            button.setPreferredSize(new Dimension(231, 42));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            button.setMargin(new Insets(6, 14, 6, 14));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            ImageIcon icon = "Order Catalog".equals(page) ? inventoryIcon : dashboardIcon;
            if (icon != null) {
                button.setIcon(TowHitchCore.loadTintedIcon(icon, 20, foreground));
                button.setIconTextGap(9);
            }
            button.addActionListener(event -> navigate(page));
            nav.add(button);
            nav.add(Box.createVerticalStrut(6));
        }
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        upper.add(nav, BorderLayout.CENTER);
        sidebar.add(upper, BorderLayout.CENTER);
        TowHitchCore.RoundedButton logout = new TowHitchCore.RoundedButton(
                "Logout", theme.sidebar(), theme.secondaryText(), 12, theme);
        logout.setPreferredSize(new Dimension(184, 42));
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        logout.setMargin(new Insets(6, 14, 6, 14));
        logout.setIcon(TowHitchCore.loadTintedIcon("logout_icon.png", 18,
                theme.isDark() ? new Color(0xD1D5DB) : new Color(0x374151)));
        logout.setIconTextGap(7);
        logout.setHorizontalAlignment(SwingConstants.CENTER);
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.addActionListener(event -> {
            activeUser = null;
            currentPage = "Order Catalog";
            showLogin();
        });
        JPanel logoutHolder = new JPanel(new GridBagLayout());
        logoutHolder.setOpaque(false);
        GridBagConstraints logoutConstraints = new GridBagConstraints();
        logoutConstraints.anchor = GridBagConstraints.CENTER;
        logoutHolder.add(logout, logoutConstraints);
        sidebar.add(logoutHolder, BorderLayout.SOUTH);
        return sidebar;
    }

    private void navigate(String page) {
        currentPage = page;
        buildApplication();
    }

    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        if (sidebarPanel == null) {
            return;
        }
        sidebarPanel.setVisible(true);
        if (sidebarAnimation != null) {
            sidebarAnimation.stop();
        }
        int target = sidebarVisible ? 255 : 0;
        topBar.revalidate();
        topBar.repaint();
        sidebarAnimation = new Timer(15, event -> {
            int difference = target - sidebarWidth;
            if (Math.abs(difference) <= 2) {
                sidebarWidth = target;
                sidebarPanel.setPreferredSize(new Dimension(sidebarWidth, 100));
                sidebarPanel.setVisible(sidebarWidth > 0);
                topBar.revalidate();
                topBar.repaint();
                sidebarPanel.getParent().revalidate();
                sidebarPanel.getParent().repaint();
                sidebarAnimation.stop();
                return;
            }
            sidebarWidth += difference / 4;
            if (sidebarWidth == 0 && target > 0) {
                sidebarWidth = 1;
            }
            sidebarPanel.setPreferredSize(new Dimension(sidebarWidth, 100));
            sidebarPanel.getParent().revalidate();
            sidebarPanel.getParent().repaint();
        });
        sidebarAnimation.start();
    }

    private JPanel buildTopBar() {
        TowHitchCore.RoundedPanel top = new TowHitchCore.RoundedPanel(0);
        top.setBackground(theme.panel());
        top.setLayout(new GridBagLayout());
        top.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        top.setPreferredSize(new Dimension(100, 56));
        top.setMinimumSize(new Dimension(100, 56));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 6, 0, 6);
        headerMenuButton = new TowHitchCore.FlatIconButton("\u2630", "Navigation", theme);
        headerMenuButton.setIconButtonSize(32);
        headerMenuButton.setFont(new Font("SansSerif", Font.PLAIN, 15));
        headerMenuButton.addActionListener(event -> toggleSidebar());
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        top.add(headerMenuButton, constraints);

        RoundedSearchPanel searchPanel = new RoundedSearchPanel();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.setPreferredSize(new Dimension(360, 32));
        searchPanel.setMinimumSize(new Dimension(220, 32));
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        TowHitchCore.RoundedTextField search = new TowHitchCore.RoundedTextField(
                "Search", 28, theme);
        search.setPreferredSize(new Dimension(220, 32));
        search.setMinimumSize(new Dimension(100, 32));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        search.setText(searchText);
        search.setOpaque(false);
        search.addActionListener(event -> {
            searchText = search.getText().trim();
            navigate("Order Catalog");
        });
        searchPanel.add(buildSearchLeading(), BorderLayout.WEST);
        searchPanel.add(search, BorderLayout.CENTER);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        top.add(searchPanel, constraints);

        notificationButton = new TowHitchCore.FlatIconButton(
                TowHitchCore.loadTintedIcon(notificationIcon, 18, theme.text()),
                "Notifications", theme);
        notificationButton.setIconButtonSize(24);
        notificationButton.setUnreadCount(notificationAlerts ? unreadNotificationCount() : 0);
        notificationButton.addActionListener(event -> showNotifications(notificationButton));
        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        top.add(notificationButton, constraints);
        TowHitchCore.ProfileButton profile = new TowHitchCore.ProfileButton(theme);
        profile.addActionListener(event -> showProfileMenu(profile));
        constraints.gridx = 3;
        top.add(profile, constraints);
        topBar = top;
        return top;
    }

    private JPanel buildCatalogPage() {
        JPanel page = pageShell("Order Catalog",
                "Available tow hitch products and accessories with live stock status for ordering.");
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
        page.add(sectionContainer(scroll(rows)), BorderLayout.CENTER);
        return page;
    }

    private JPanel productCard(TowHitchCore.InventoryItem item) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(16);
        card.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 16),
                BorderFactory.createEmptyBorder(17, 18, 17, 18)));
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
            TowHitchCore.RoundedBorder.QuantityStepper quantity =
                    new TowHitchCore.RoundedBorder.QuantityStepper(1, 1,
                            Math.max(1, maxOrderable), theme);
            quantity.setEnabled(maxOrderable > 0);
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

    private void placeOrder(TowHitchCore.InventoryItem product,
                            TowHitchCore.RoundedBorder.QuantityStepper quantity) {
        int requested;
        try {
            requested = quantity.getValue();
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
            return;
        }
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
        JPanel page = pageShell("My Orders",
                "Personal order history showing real-time fulfillment status and pending order management.");
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        double[] weights = {.14, .18, .18, .07, .16, .27};
        list.add(tableHeader(new String[] {"Status", "Order ID", "Product", "Qty", "Order Date", ""},
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
        page.add(sectionContainer(list), BorderLayout.CENTER);
        return page;
    }

    private JPanel orderRow(TowHitchCore.CustomerOrder order, double[] weights) {
        JPanel row = rowPanel();
        addCell(row, new TowHitchCore.StatusPillBadge(order.status, theme), 0, weights[0]);
        addCell(row, tableCell(order.orderId), 1, weights[1]);
        addCell(row, tableNameCell(order.productName), 2, weights[2]);
        addCell(row, tableCell(Integer.toString(order.quantity)), 3, weights[3]);
        addCell(row, tableCell(TowHitchCore.formatSmartDate(order.timestamp)), 4, weights[4]);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);
        if ("PENDING".equals(order.status)) {
            TowHitchCore.RoundedButton cancel = actionButton(
                    "Cancel", RED, new Color(0xFAF8F5));
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
        JPanel header = new TowHitchCore.RoundedPanel(12);
        header.setLayout(new GridBagLayout());
        header.setBackground(theme.isDark() ? new Color(0x1E2026) : Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
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
        row.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
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

    private JLabel tableNameCell(String text) {
        JLabel label = textLabel(text, 12, true, theme.text());
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

    private JPanel sectionContainer(Component content) {
        TowHitchCore.RoundedPanel section = TowHitchCore.createSectionPanel(theme);
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    private JScrollPane scroll(Component component) {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.getVerticalScrollBar().setUI(new TowHitchCore.MinimalScrollBarUI(theme));
        pane.getHorizontalScrollBar().setUI(new TowHitchCore.MinimalScrollBarUI(theme));
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        TowHitchCore.configureSmoothScroll(pane);
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
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(frame);
        TowHitchCore.RoundedPanel surface = new TowHitchCore.RoundedPanel(20);
        surface.setBackground(theme.panel());
        surface.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder.RoundedShadowBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xDCD8CE), 20),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        surface.setLayout(new BorderLayout(0, 10));
        JPanel heading = new JPanel(new BorderLayout(8, 0));
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
        heading.add(textLabel(title, 18, true, theme.text()));
        JButton close = new JButton("\u2715");
        close.setFocusable(false);
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setForeground(theme.secondaryText());
        close.addActionListener(event -> dialog.dispose());
        heading.add(close, BorderLayout.EAST);
        surface.add(heading, BorderLayout.NORTH);
        java.awt.event.MouseAdapter drag = new java.awt.event.MouseAdapter() {
            private java.awt.Point origin;
            @Override public void mousePressed(java.awt.event.MouseEvent event) {
                origin = event.getPoint();
            }
            @Override public void mouseDragged(java.awt.event.MouseEvent event) {
                java.awt.Point location = dialog.getLocation();
                dialog.setLocation(location.x + event.getX() - origin.x,
                        location.y + event.getY() - origin.y);
            }
        };
        heading.addMouseListener(drag);
        heading.addMouseMotionListener(drag);
        dialog.setContentPane(surface);
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
        TowHitchCore.configureTextField(field);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(theme.text());
        field.setBackground(theme.input());
        field.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.row(), 10),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setPreferredSize(new Dimension(value != null && value.matches("\\d+")
                ? 120 : 230, 38));
        return field;
    }

    private JPasswordField dialogPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(theme.text());
        field.setBackground(theme.input());
        field.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.row(), 10),
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

    private void showNotifications(Component invoker) {
        if (notificationPopup != null && notificationPopup.isVisible()) {
            anchoredPopover.close();
            return;
        }
        javax.swing.JWindow popup = new javax.swing.JWindow(frame);
        popup.setBackground(new Color(0, 0, 0, 0));
        JPanel content = verticalPanel();
        List<TowHitchCore.SystemNotification> feed = loadNotifications();
        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        heading.add(textLabel("Notifications", 15, true, theme.text()), BorderLayout.WEST);
        JButton markAll = notificationLink("Mark all as read");
        heading.add(markAll, BorderLayout.EAST);
        content.add(heading);

        JPanel items = verticalPanel();
        JPanel headerDivider = new JPanel();
        headerDivider.setOpaque(true);
        headerDivider.setBackground(theme.isDark()
                ? new Color(0x32363F) : new Color(0xECEAE4));
        headerDivider.setPreferredSize(new Dimension(1, 1));
        headerDivider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        items.add(headerDivider);
        List<JPanel> cards = new ArrayList<>();
        List<JLabel> dots = new ArrayList<>();
        List<JLabel> labels = new ArrayList<>();
        for (int notificationIndex = 0; notificationIndex < feed.size(); notificationIndex++) {
            TowHitchCore.SystemNotification notification = feed.get(notificationIndex);
            TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(12);
            card.setLayout(new BorderLayout(9, 0));
            card.setOpaque(true);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new TowHitchCore.RoundedBorder(theme.isDark()
                            ? new Color(0x32363F) : new Color(0xECEAE4), 12),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            JLabel dot = textLabel("\u25CF", 8, true, notification.indicatorColor);
            String notificationHtml = notification.text.startsWith("<html>")
                    ? "<html><div style='width:340px'>"
                            + notification.text.substring(6, notification.text.length() - 7)
                            + "</div></html>" : notification.text;
            JLabel label = new JLabel(notificationHtml);
            label.setOpaque(false);
            label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            label.setToolTipText(notification.text);
            card.add(dot, BorderLayout.WEST);
            card.add(label, BorderLayout.CENTER);
            styleNotificationCard(notification, card, dot, label);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            MouseAdapter click = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    readNotificationIds.add(notificationReadKey(notification));
                    styleNotificationCard(notification, card, dot, label);
                    ((TowHitchCore.FlatIconButton) invoker)
                            .setUnreadCount(notificationAlerts
                                    ? countUnreadNotifications(feed) : 0);
                }
            };
            card.addMouseListener(click);
            dot.addMouseListener(click);
            label.addMouseListener(click);
            cards.add(card);
            dots.add(dot);
            labels.add(label);
            items.add(Box.createVerticalStrut(4));
            items.add(card);
            JPanel divider = new JPanel();
            divider.setOpaque(true);
            divider.setBackground(theme.isDark()
                    ? new Color(0x32363F) : new Color(0xECEAE4));
            divider.setPreferredSize(new Dimension(1, 1));
            divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            items.add(divider);
        }
        if (feed.isEmpty()) items.add(textLabel("You're all caught up.", 13,
                false, theme.secondaryText()));
        markAll.addActionListener(event -> {
            for (TowHitchCore.SystemNotification notification : feed) {
                readNotificationIds.add(notificationReadKey(notification));
            }
            for (int i = 0; i < feed.size(); i++) {
                styleNotificationCard(feed.get(i), cards.get(i), dots.get(i), labels.get(i));
            }
            ((TowHitchCore.FlatIconButton) invoker).setUnreadCount(0);
        });
        JScrollPane pane = scroll(items);
        pane.setPreferredSize(new Dimension(420,
                Math.min(360, Math.max(90, feed.size() * 62 + 20))));
        content.add(pane);
        TowHitchCore.RoundedPanel popover = new TowHitchCore.RoundedPanel(18);
        popover.setBackground(theme.isDark() ? new Color(0x1E2026) : Color.WHITE);
        popover.setLayout(new BorderLayout());
        popover.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder.RoundedShadowBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xDCD8CE), 18),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        popover.add(content, BorderLayout.CENTER);
        popup.setContentPane(popover);
        popup.pack();
        notificationPopup = popup;
        anchoredPopover.show(invoker, popup, true);
    }

    private JButton notificationLink(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setForeground(theme.accent());
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private List<TowHitchCore.SystemNotification> loadNotifications() {
        try {
            return TowHitchCore.loadSystemNotifications("CUSTOMER", activeUser.fullName);
        } catch (IOException e) {
            showDataError("Unable to load system notifications", e);
            return new ArrayList<>();
        }
    }

    private int countUnreadNotifications(List<TowHitchCore.SystemNotification> feed) {
        int unread = 0;
        for (TowHitchCore.SystemNotification notification : feed) {
            if (!readNotificationIds.contains(notificationReadKey(notification))) unread++;
        }
        return unread;
    }

    private int unreadNotificationCount() {
        return countUnreadNotifications(loadNotifications());
    }

    private String notificationReadKey(TowHitchCore.SystemNotification notification) {
        return activeUser.username + "\t" + notification.id;
    }

    private void styleNotificationCard(TowHitchCore.SystemNotification notification,
                                       JPanel card, JLabel dot, JLabel label) {
        boolean unread = !readNotificationIds.contains(notificationReadKey(notification));
        card.setBackground(unread
                ? (theme.isDark() ? new Color(0x282B33) : new Color(0xF7F5F0))
                : (theme.isDark() ? new Color(0x1E2026) : Color.WHITE));
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(unread ? (theme.isDark() ? new Color(0xF9FAFB)
                : new Color(0x111827))
                : (theme.isDark() ? new Color(0x9CA3AF) : new Color(0x6B7280)));
        dot.setForeground(notification.indicatorColor);
        dot.setVisible(unread);
    }

    private void showSettings(Component invoker) {
        showProfileMenu(invoker);
    }

    private void showProfileMenu(Component anchor) {
        if (profilePopup != null && profilePopup.isVisible()) {
            anchoredPopover.close();
            return;
        }
        showProfilePopover(anchor, "main");
    }

    private void showProfilePopover(Component anchor, String view) {
        if (anchoredPopover != null) anchoredPopover.close();
        javax.swing.JWindow popup = new javax.swing.JWindow(frame);
        popup.setBackground(new Color(0, 0, 0, 0));
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(18);
        card.setBackground(theme.panel());
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder.RoundedShadowBorder(
                        theme.isDark() ? new Color(0x383C45) : new Color(0xDCD8CE),
                        18, false),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));
        if ("account".equals(view)) {
            card.add(profileMenuButton("\u2190 Account & Profile",
                    () -> showProfilePopover(anchor, "main")));
            card.add(Box.createVerticalStrut(10));
            addProfileDetail(card, "Full Name", activeUser.fullName);
            addProfileDetail(card, "Username", "@" + activeUser.username);
            addProfileDetail(card, "Role", "Customer");
            addProfileDetail(card, "Branch", "Tow Hitch by 5-21");
            card.add(profileMenuButton("Change Password", this::changePassword));
        } else if ("settings".equals(view)) {
            card.add(profileMenuButton("\u2190 Settings",
                    () -> showProfilePopover(anchor, "main")));
            card.add(Box.createVerticalStrut(10));
            card.add(profilePreference("Notification Alerts", notificationAlerts,
                    this::setNotificationAlerts));
            card.add(profilePreference("Auto-Refresh Inventory", autoRefreshInventory,
                    value -> autoRefreshInventory = value));
            card.add(Box.createVerticalStrut(8));
            addProfileDetail(card, "Session & Security", "Signed in securely");
        } else if ("display".equals(view)) {
            card.add(profileMenuButton("\u2190 Display & Accessibility",
                    () -> showProfilePopover(anchor, "main")));
            card.add(Box.createVerticalStrut(12));
            card.add(textLabel("Dark Mode", 14, true, theme.text()));
            card.add(Box.createVerticalStrut(4));
            javax.swing.JRadioButton off = profileRadio("Off", !theme.isDark());
            javax.swing.JRadioButton on = profileRadio("On", theme.isDark());
            javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
            group.add(off);
            group.add(on);
            off.addActionListener(event -> applyProfileTheme(false));
            on.addActionListener(event -> applyProfileTheme(true));
            card.add(off);
            card.add(on);
            card.add(Box.createVerticalStrut(8));
            card.add(profilePreference("Compact View", compactView,
                    this::setCompactView));
        } else {
            JPanel identity = new JPanel(new BorderLayout(10, 0));
            identity.setOpaque(false);
            identity.setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 14));
            identity.add(new TowHitchCore.ProfileButton(theme), BorderLayout.WEST);
            JPanel labels = new JPanel();
            labels.setOpaque(false);
            labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
            labels.add(textLabel(activeUser.fullName, 13, true, theme.text()));
            labels.add(Box.createVerticalStrut(3));
            labels.add(textLabel("Customer \u2022 @" + activeUser.username,
                    12, false, theme.secondaryText()));
            identity.add(labels, BorderLayout.CENTER);
            card.add(identity);
            card.add(profileMenuRow("\u25C9", "Account & Profile",
                    () -> showProfilePopover(anchor, "account")));
            card.add(Box.createVerticalStrut(4));
            card.add(profileMenuRow("\u2699", "Settings",
                    () -> showProfilePopover(anchor, "settings")));
            card.add(Box.createVerticalStrut(4));
            card.add(profileMenuRow("\u263C", "Display & Accessibility",
                    () -> showProfilePopover(anchor, "display")));
            card.add(Box.createVerticalStrut(10));
            card.add(profileDivider());
            TowHitchCore.PopoverMenuButton logout = new TowHitchCore.PopoverMenuButton(
                    "Logout", "\u21AA", false, true, theme);
            logout.addActionListener(event -> logout());
            card.add(Box.createVerticalStrut(8));
            card.add(logout);
        }
        popup.setContentPane(card);
        popup.pack();
        profilePopup = popup;
        anchoredPopover.show(anchor, popup, true);
    }

    private TowHitchCore.PopoverMenuButton profileMenuButton(String text, Runnable action) {
        TowHitchCore.PopoverMenuButton button = new TowHitchCore.PopoverMenuButton(
                text, "", false, false, theme);
        button.addActionListener(event -> action.run());
        return button;
    }

    private TowHitchCore.PopoverMenuButton profileMenuRow(
            String icon, String label, Runnable action) {
        TowHitchCore.PopoverMenuButton button = new TowHitchCore.PopoverMenuButton(
                label, icon, true, false, theme);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void addProfileDetail(JPanel parent, String key, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 4, 5, 4));
        row.add(textLabel(key, 12, false, theme.secondaryText()), BorderLayout.WEST);
        if ("Role".equals(key)) {
            TowHitchCore.RoundedPanel badge = new TowHitchCore.RoundedPanel(10);
            badge.setBackground(theme.isDark() ? new Color(0x34312F) : new Color(0xEDE7DD));
            badge.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
            badge.add(textLabel(value.toUpperCase(java.util.Locale.ROOT),
                    10, true, theme.text()));
            row.add(badge, BorderLayout.EAST);
        } else {
            row.add(textLabel(value, 12, false, theme.text()), BorderLayout.EAST);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        parent.add(row);
    }

    private JCheckBox profilePreference(String label, boolean selected,
                                        java.util.function.Consumer<Boolean> update) {
        JCheckBox preference = new JCheckBox(label, selected);
        preference.setOpaque(false);
        preference.setForeground(theme.text());
        preference.setAlignmentX(Component.LEFT_ALIGNMENT);
        preference.addActionListener(event -> update.accept(preference.isSelected()));
        return preference;
    }

    private void setNotificationAlerts(boolean enabled) {
        notificationAlerts = enabled;
        if (notificationButton != null) {
            notificationButton.setUnreadCount(enabled ? unreadNotificationCount() : 0);
        }
    }

    private void setCompactView(boolean enabled) {
        compactView = enabled;
        anchoredPopover.close();
        buildApplication();
    }

    private void changePassword() {
        JPasswordField password = new JPasswordField();
        int result = JOptionPane.showConfirmDialog(frame, password, "Change Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        String updated = new String(password.getPassword());
        if (updated.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Password cannot be empty.",
                    "Change Password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            List<TowHitchCore.UserAccount> users = TowHitchCore.loadUsers();
            boolean accountFound = false;
            for (TowHitchCore.UserAccount user : users) {
                if (user.username.equalsIgnoreCase(activeUser.username)) {
                    user.setPassword(updated);
                    accountFound = true;
                }
            }
            if (!accountFound) throw new IOException("The signed-in account could not be found.");
            TowHitchCore.saveUsers(users);
            activeUser.setPassword(updated);
            JOptionPane.showMessageDialog(frame, "Password updated successfully.",
                    "Change Password", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            showDataError("Unable to update account password", e);
        }
    }

    private JPanel profileDivider() {
        JPanel divider = new JPanel();
        divider.setOpaque(true);
        divider.setBackground(theme.isDark() ? new Color(0x383C45) : new Color(0xE8E3DA));
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return divider;
    }

    private javax.swing.JRadioButton profileRadio(String text, boolean selected) {
        javax.swing.JRadioButton radio = new javax.swing.JRadioButton(text, selected);
        radio.setOpaque(false);
        radio.setForeground(theme.text());
        radio.setFont(new Font("SansSerif", Font.PLAIN, 13));
        radio.setAlignmentX(Component.LEFT_ALIGNMENT);
        radio.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return radio;
    }

    private void applyProfileTheme(boolean dark) {
        if (theme.isDark() != dark) {
            theme.setDark(dark);
            buildApplication();
        }
        if (anchoredPopover != null) anchoredPopover.close();
    }

    private void showAccountSettings() {
        JOptionPane.showMessageDialog(frame,
                "Display name: " + activeUser.fullName + "\nUsername: " + activeUser.username
                        + "\nRole: Customer",
                "Settings", JOptionPane.INFORMATION_MESSAGE);
    }

    private void positionProfilePopover(javax.swing.JWindow popup, Component anchor) {
        java.awt.Point point = anchor.getLocationOnScreen();
        java.awt.Rectangle bounds = java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int x = Math.min(point.x + anchor.getWidth() - popup.getWidth(),
                bounds.x + bounds.width - popup.getWidth() - 8);
        x = Math.max(bounds.x + 8, x);
        int y = point.y + anchor.getHeight() + 6;
        if (y + popup.getHeight() > bounds.y + bounds.height - 8) {
            y = point.y - popup.getHeight() - 6;
        }
        popup.setLocation(x, y);
    }

    private void logout() {
        activeUser = null;
        currentPage = "Order Catalog";
        if (anchoredPopover != null) anchoredPopover.close();
        showLogin();
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
            setPreferredSize(new Dimension(60, 30));
            setMinimumSize(new Dimension(60, 30));
            setMaximumSize(new Dimension(60, 30));
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
            TowHitchCore.paintThemeSwitch(g, getWidth(), getHeight(), position,
                    theme.isDark());
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
                int width = Math.min(468, getWidth() - 32);
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
        private GlassLoginCard() {
            setOpaque(false);
        }

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
                TowHitchCore.configureTextField(this);
                setFont(new Font("SansSerif", Font.PLAIN, 14));
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 16));
                setPreferredSize(new Dimension(395, 42));
                setMinimumSize(new Dimension(395, 42));
                setMaximumSize(new Dimension(395, 42));
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
                if (getText().isEmpty()) paintPlaceholder(graphics, placeholder, this);
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
                setPreferredSize(new Dimension(395, 42));
                setMinimumSize(new Dimension(395, 42));
                setMaximumSize(new Dimension(395, 42));
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
                if (getPassword().length == 0) paintPlaceholder(graphics, placeholder, this);
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
            g.setColor(theme.isDark() ? new Color(0x1E2026) : Color.WHITE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.setColor(theme.isDark() ? new Color(0x32363F) : new Color(0xECEAE4));
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
                g.setColor(theme.isDark() ? new Color(0x24272E) : new Color(0xFBF9F5));
                g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g.setColor(theme.isDark() ? new Color(0x383C45) : new Color(0xE2DDD3));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
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
