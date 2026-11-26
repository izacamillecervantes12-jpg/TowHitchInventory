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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class StaffApp {
    private static final Color GREEN = new Color(0x2E6F40);
    private static final Color GREEN_HOVER = new Color(0x245832);
    private static final Color RED = new Color(0xA63A3A);
    private static final Color RED_HOVER = new Color(0x8C2E2E);
    private static final int[] UNFINISHED_COLUMNS = {120, -1, 90, 140, 155};
    private static final int[] REQUEST_COLUMNS = {120, -1, 90, 160, 160};
    private static final int[] ORDER_COLUMNS = {120, -1, 80, 160, 160, 230};
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
    private final ImageIcon notificationIcon = TowHitchCore.loadScaledImageAsset("notification_icon.png", 18, 18);
    private final ImageIcon settingsIcon = TowHitchCore.loadScaledImageAsset("settings_icon.png", 18, 18);
    private final String[] pages = {"Dashboard", "Production", "Raw Materials",
            "Stock Requests", "Customer Orders", "Product Logs", "Stock Order Logs",
            "Material Consumption", "Trash"};
    private JFrame frame;
    private JPanel pageHost;
    private JPanel sidebarPanel;
    private TowHitchCore.FlatIconButton headerMenuButton;
    private JPanel topBar;
    private Timer sidebarAnimation;
    private int sidebarWidth = 255;
    private TowHitchCore.UserAccount activeUser;
    private String currentStaffName = "";
    private String currentDisplayName = "";
    private String currentPage = "Dashboard";
    private String searchText = "";
    private boolean sidebarVisible = true;
    private boolean historyExpanded;
    private boolean compactView;
    private boolean notificationAlerts = true;
    private boolean autoRefreshInventory = true;
    private LoginBackgroundPanel loginRoot;
    private JLabel loginTitle;
    private JLabel loginSubtitle;
    private JLabel loginMessage;
    private JLabel loginFooter;
    private javax.swing.JWindow notificationPopup;
    private javax.swing.JWindow profilePopup;
    private TowHitchCore.AnchoredPopover anchoredPopover;
    private TowHitchCore.FlatIconButton notificationButton;
    private final Set<String> readNotificationIds = new HashSet<>();
    private final Map<String, Integer> submittedRestockQuantities = new LinkedHashMap<>();

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
        anchoredPopover = new TowHitchCore.AnchoredPopover(frame);
        TowHitchCore.installGlobalMouseHandling();
        showLogin();
        frame.setVisible(true);
    }

    private void showLogin() {
        LoginBackgroundPanel root = new LoginBackgroundPanel();
        loginRoot = root;
        JPanel card = new GlassLoginCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 36, 32, 36));
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        JLabel logo = new JLabel(loginLogo, SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(64, 64));
        loginRow(content, logo, 0, 0, 12);
        loginTitle = loginHeading("Welcome, Crew", 34, true);
        loginRow(content, loginTitle, 1, 0, 6);
        loginSubtitle = loginHeading(
                "Access production tools and submit restock requests.", 15, false);
        loginRow(content, loginSubtitle, 2, 0, 26);
        JTextField crewName = loginField("Enter your username");
        JPasswordField password = loginPassword("Enter password");
        loginRow(content, loginLabel("Username"), 3, 0, 6);
        loginRow(content, crewName, 4, 0, 16);
        loginRow(content, loginLabel("Password"), 5, 0, 6);
        loginRow(content, password, 6, 0, 24);
        loginMessage = loginHeading(" ", 12, false);
        loginMessage.setForeground(theme.danger());
        loginMessage.setVisible(false);
        loginRow(content, loginMessage, 7, 0, 7);
        TowHitchCore.RoundedButton signIn = new TowHitchCore.RoundedButton(
                "Log in", theme.primaryButton(), Color.WHITE, 12, theme);
        signIn.setFont(new Font("SansSerif", Font.BOLD, 14));
        signIn.setPreferredSize(new Dimension(395, 44));
        signIn.setMinimumSize(new Dimension(395, 44));
        signIn.setMaximumSize(new Dimension(395, 44));
        signIn.addActionListener(event -> authenticate(crewName, password));
        password.addActionListener(event -> authenticate(crewName, password));
        loginRow(content, signIn, 8, 0, 14);
        JButton register = new JButton("Create Staff Account");
        register.setFont(new Font("SansSerif", Font.BOLD, 13));
        register.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        register.setOpaque(false);
        register.setContentAreaFilled(false);
        register.setBorderPainted(false);
        register.setFocusPainted(false);
        register.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        register.addActionListener(event -> openStaffRegistrationDialog());
        register.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        loginRow(content, register, 9, 0, 0);
        content.setPreferredSize(new Dimension(395, content.getPreferredSize().height));
        JPanel centeredContent = centerLoginContent(content);
        card.add(centeredContent);
        loginFooter = loginHeading("Tow Hitch by 5-21 | Staff Portal", 16, false);
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

    private JTextField loginField(String placeholder) {
        return new LoginTextField(placeholder);
    }

    private JPasswordField loginPassword(String placeholder) {
        return new LoginPasswordField(placeholder);
    }

    private void authenticate(JTextField crewNameField, JPasswordField password) {
        try {
            loginMessage.setForeground(theme.danger());
            String crewName = crewNameField.getText().trim();
            String secret = new String(password.getPassword());
            if (crewName.isEmpty()) {
                showLoginMessage("Enter your username.");
                return;
            }

            for (TowHitchCore.StaffAccount account : TowHitchCore.loadStaffAccounts()) {
                if (account.username.equalsIgnoreCase(crewName)
                        && account.password.equals(secret)) {
                    if ("PENDING".equals(account.status)) {
                        showLoginMessage("Your staff account is still pending Admin approval.");
                        return;
                    }
                    if ("REMOVED".equals(account.status)) {
                        showLoginMessage(
                                "Your staff account access has been revoked or declined.");
                        return;
                    }
                    activeUser = new TowHitchCore.UserAccount(account.username, account.password,
                            account.displayName, "STAFF");
                    currentDisplayName = account.displayName;
                    currentStaffName = currentDisplayName;
                    buildApplication();
                    return;
                }
            }
            for (TowHitchCore.UserAccount user : TowHitchCore.loadUsers()) {
                if ("STAFF".equals(user.role) && user.password.equals(secret)
                        && (user.username.equalsIgnoreCase(crewName)
                                || user.fullName.equalsIgnoreCase(crewName))) {
                    activeUser = user;
                    currentDisplayName = user.fullName;
                    currentStaffName = currentDisplayName;
                    buildApplication();
                    return;
                }
            }
            showLoginMessage("Invalid username or password.");
        } catch (IOException e) {
            showLoginMessage("Unable to read staff accounts: " + e.getMessage());
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

    private void openStaffRegistrationDialog() {
        JDialog dialog = themedDialog("Create Staff Account", 460, 370, true);
        JPanel content = dialogContent();
        JTextField displayName = registrationTextField("Enter display name");
        JTextField username = registrationTextField("Enter username");
        JPasswordField password = registrationPasswordField("Enter password");
        JPasswordField confirmPassword = registrationPasswordField("Confirm password");
        addFormRow(content, "Display Name", displayName);
        addFormRow(content, "Username", username);
        addFormRow(content, "Password", password);
        addFormRow(content, "Confirm Password", confirmPassword);
        dialog.add(content, BorderLayout.CENTER);

        JPanel actions = dialogActions();
        TowHitchCore.RoundedButton cancel = actionButton(
                "Cancel", theme.sidebar(), theme.text());
        cancel.addActionListener(event -> dialog.dispose());
        TowHitchCore.RoundedButton submit = actionButton(
                "Submit Account", theme.primaryButton(), Color.WHITE);
        submit.addActionListener(event -> {
            String display = displayName.getText().trim();
            String login = username.getText().trim();
            String secret = new String(password.getPassword());
            String confirmation = new String(confirmPassword.getPassword());
            if (display.isEmpty() || login.isEmpty() || secret.isEmpty()) {
                error("Enter a display name, username, and password.");
                return;
            }
            if (!secret.equals(confirmation)) {
                error("Password and confirmation do not match.");
                return;
            }
            try {
                TowHitchCore.registerStaffAccount(display, display, login, secret);
                dialog.dispose();
                loginMessage.setForeground(theme.success());
                showLoginMessage(
                        "Account submitted! Waiting for Admin approval before you can log in.");
            } catch (IOException e) {
                showDataError("Unable to submit staff account", e);
            }
        });
        actions.add(cancel);
        actions.add(submit);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JTextField registrationTextField(String placeholder) {
        return new RegistrationTextField(placeholder);
    }

    private JPasswordField registrationPasswordField(String placeholder) {
        TowHitchCore.RoundedPasswordField field = new TowHitchCore.RoundedPasswordField(
                placeholder, 20, theme);
        field.setBackground(theme.input());
        field.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xDCD8CE), 12),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setPreferredSize(new Dimension(220, 40));
        field.setMinimumSize(new Dimension(220, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return field;
    }

    @SuppressWarnings("this-escape")
    private class RegistrationTextField extends TowHitchCore.RoundedTextField {
        private static final long serialVersionUID = 1L;

        private RegistrationTextField(String placeholder) {
            super(placeholder, 20, theme);
            setBorder(BorderFactory.createCompoundBorder(
                    new TowHitchCore.RoundedBorder(theme.isDark()
                            ? new Color(0x383C45) : new Color(0xDCD8CE), 12),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            setPreferredSize(new Dimension(220, 40));
            setMinimumSize(new Dimension(220, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.input());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.dispose();
            super.paintComponent(graphics);
        }
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
        pageHost.add(buildDashboard(), "Dashboard");
        pageHost.add(buildProductionPage(), "Production");
        pageHost.add(buildUnfinishedPage(), "Raw Materials");
        pageHost.add(buildStockRequestsPage(), "Stock Requests");
        pageHost.add(buildCustomerOrdersPage(), "Customer Orders");
        pageHost.add(buildHistoryPage("Product Logs"), "Product Logs");
        pageHost.add(buildHistoryPage("Stock Order Logs"), "Stock Order Logs");
        pageHost.add(buildHistoryPage("Material Consumption"), "Material Consumption");
        pageHost.add(buildTrashPage(), "Trash");
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
        for (String page : pages) {
            if ("Product Logs".equals(page)) {
                addNavigationButton(nav, "History / Archive",
                        isHistoryPage(currentPage), historyIcon, 0, () -> {
                            historyExpanded = !historyExpanded;
                            buildApplication();
                        });
                if (historyExpanded) {
                    addNavigationButton(nav, page, page.equals(currentPage), historyIcon, 18,
                            () -> navigate(page));
                }
                continue;
            }
            if ("Stock Order Logs".equals(page) || "Material Consumption".equals(page)) {
                if (historyExpanded) {
                    addNavigationButton(nav, page, page.equals(currentPage), historyIcon, 18,
                            () -> navigate(page));
                }
                continue;
            }
            boolean selected = page.equals(currentPage);
            Color foreground = selected ? Color.WHITE : theme.secondaryText();
            TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(page,
                    selected ? theme.primaryButton() : theme.sidebar(), foreground, 12, theme);
            button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 13));
            button.setPreferredSize(new Dimension(231, 42));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            button.setMargin(new Insets(6, 14, 6, 14));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            ImageIcon icon = iconFor(page);
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
            currentStaffName = "";
            currentDisplayName = "";
            currentPage = "Dashboard";
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

    private ImageIcon iconFor(String page) {
        if ("Dashboard".equals(page)) return dashboardIcon;
        if ("Production".equals(page) || "Stock Requests".equals(page)
                || "Customer Orders".equals(page)) return productionIcon;
        if ("History / Archive".equals(page)) return historyIcon;
        return inventoryIcon;
    }

    private void navigate(String page) {
        currentPage = page;
        if (!isHistoryPage(page)) {
            historyExpanded = false;
        }
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

    private void addNavigationButton(JPanel navigation, String text, boolean selected,
                                     ImageIcon icon, int indent, Runnable action) {
        int rowHeight = "History / Archive".equals(text) ? 40 : indent > 0 ? 38 : 42;
        Color foreground = selected ? Color.WHITE : theme.secondaryText();
        TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(
                text, selected ? theme.primaryButton() : theme.sidebar(), foreground, 12, theme);
        button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 13));
        button.setPreferredSize(new Dimension(indent > 0 ? 213 : 231, rowHeight));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
        button.setMargin(new Insets(6, 14, 6, 14));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        if (icon != null) {
            button.setIcon(TowHitchCore.loadTintedIcon(icon, 18, foreground));
            button.setIconTextGap(8);
        }
        button.addActionListener(event -> action.run());
        if (indent > 0) {
            JPanel indented = new JPanel(new BorderLayout());
            indented.setOpaque(false);
            indented.setPreferredSize(new Dimension(213, rowHeight));
            indented.setMinimumSize(new Dimension(0, rowHeight));
            indented.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
            indented.setBorder(BorderFactory.createEmptyBorder(0, indent, 0, 0));
            indented.add(button, BorderLayout.CENTER);
            indented.setAlignmentX(Component.LEFT_ALIGNMENT);
            navigation.add(indented);
        } else {
            navigation.add(button);
        }
        navigation.add(Box.createVerticalStrut(
                indent > 0 || "History / Archive".equals(text) ? 4 : 6));
    }

    private boolean isHistoryPage(String page) {
        return "Product Logs".equals(page) || "Stock Order Logs".equals(page)
                || "Material Consumption".equals(page);
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
            for (TowHitchCore.InventoryItem item : safeInventory()) {
                if (item.itemName.toLowerCase().contains(searchText.toLowerCase())) {
                    navigate(("F".equals(item.type) ? "Production" : "Raw Materials"));
                    return;
                }
            }
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

    private JPanel buildDashboard() {
        JPanel page = pageShell("Staff Dashboard",
                "Real-time overview of inventory levels, stock alerts, and active shop transactions.");
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
                        && request.requestedBy.equalsIgnoreCase(currentStaffName)) myRequests++;
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
        cards.add(summaryCard("Low-Stock Raw Materials", Integer.toString(lowMaterials)));
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
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        theme.isDark() ? new Color(0x333740) : new Color(0xE5E2DC)),
                BorderFactory.createEmptyBorder(12, 4, 12, 4)));
        JLabel name = textLabel(item.itemName, 14, true, theme.text());
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        row.add(name, BorderLayout.WEST);
        JLabel quantity = textLabel(item.quantity + " stocks",
                14, true, theme.danger());
        quantity.setFont(new Font("SansSerif", Font.BOLD, 14));
        row.add(quantity, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        return row;
    }

    private JPanel buildProductionPage() {
        JPanel panel = pageShell("Production",
                "Product assembly workspace with live Bill of Materials availability checks and batch production.");
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
        panel.add(sectionContainer(scroll(rows)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel productionCard(TowHitchCore.InventoryItem product,
                                  List<TowHitchCore.InventoryItem> inventory,
                                  List<TowHitchCore.BOMItem> recipe) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(16);
        card.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 16),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        card.setLayout(new BorderLayout(12, 0));
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        heading.setOpaque(false);
        heading.add(textLabel(product.itemName, 16, true, theme.text()));
        TowHitchCore.ChevronButton bomToggle = new TowHitchCore.ChevronButton(theme);
        heading.add(bomToggle);
        TowHitchCore.RoundedPanel recipeGrid = new TowHitchCore.RoundedPanel(12);
        recipeGrid.setLayout(new GridLayout(0, 3, 8, 4));
        recipeGrid.setBackground(theme.isDark() ? new Color(0x22252C) : new Color(0xF8F6F1));
        recipeGrid.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        recipeGrid.setVisible(false);
        if (recipe.isEmpty()) {
            recipeGrid.add(textLabel("No BOM recipe configured.", 12,
                    false, theme.secondaryText()));
        } else {
            for (TowHitchCore.BOMItem component : recipe) {
                boolean insufficient = true;
                for (TowHitchCore.InventoryItem material : inventory) {
                    if (material.itemName.equalsIgnoreCase(component.materialName)
                            && material.quantity >= component.quantityRequired) {
                        insufficient = false;
                        break;
                    }
                }
                recipeGrid.add(textLabel(component.materialName + " \u00D7 "
                        + component.quantityRequired, 12, false,
                        insufficient ? theme.danger() : theme.secondaryText()));
            }
        }
        JPanel information = new JPanel(new BorderLayout(0, 8));
        information.setOpaque(false);
        information.add(heading, BorderLayout.NORTH);
        information.add(recipeGrid, BorderLayout.CENTER);
        bomToggle.addActionListener(event -> {
            bomToggle.setExpanded(!bomToggle.isExpanded());
            recipeGrid.setVisible(bomToggle.isExpanded());
            card.revalidate();
            card.repaint();
        });
        card.add(information, BorderLayout.CENTER);
        JPanel statusColumn = new JPanel(new GridBagLayout());
        statusColumn.setOpaque(false);
        statusColumn.setPreferredSize(new Dimension(120, 42));
        statusColumn.setMinimumSize(new Dimension(120, 42));
        statusColumn.setMaximumSize(new Dimension(120, Integer.MAX_VALUE));
        statusColumn.add(new TowHitchCore.StatusPillBadge(stockStatus(product), theme));
        card.add(statusColumn, BorderLayout.WEST);
        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        TowHitchCore.RoundedButton produce = actionButton(
                "Produce", GREEN, new Color(0xF8FAFC));
        produce.setPreferredSize(new Dimension(135, 34));
        boolean enough = canProduce(recipe, inventory, 1);
        produce.setText(enough ? "Produce" : "Insufficient Stock");
        produce.setEnabled(enough);
        produce.addActionListener(event -> openProductionDialog(product, recipe));
        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.anchor = GridBagConstraints.CENTER;
        actions.add(produce, actionConstraints);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private boolean canProduce(List<TowHitchCore.BOMItem> recipe,
                               List<TowHitchCore.InventoryItem> inventory, int quantity) {
        if (recipe.isEmpty() || quantity < 1) {
            return false;
        }
        Map<String, Long> needed = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (TowHitchCore.BOMItem component : recipe) {
            if (!validField(component.materialName) || component.quantityRequired < 1) {
                return false;
            }
            String key = component.materialName.toLowerCase(Locale.ROOT);
            needed.merge(key, (long) component.quantityRequired * quantity, Long::sum);
            names.putIfAbsent(key, component.materialName);
        }
        for (Map.Entry<String, Long> requirement : needed.entrySet()) {
            int matches = 0;
            int available = 0;
            for (TowHitchCore.InventoryItem item : inventory) {
                if (!"F".equals(item.type)
                        && item.itemName.equalsIgnoreCase(names.get(requirement.getKey()))) {
                    matches++;
                    available = item.quantity;
                }
            }
            if (matches != 1 || available < requirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void openProductionDialog(TowHitchCore.InventoryItem product,
                                      List<TowHitchCore.BOMItem> recipe) {
        JDialog dialog = themedDialog("Produce " + product.itemName, 560, 500);
        JPanel content = dialogContent();
        content.setLayout(new BorderLayout(0, 14));
        JPanel quantityRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        quantityRow.setOpaque(false);
        JButton decrement = stepperButton("\u2212");
        JTextField quantity = dialogField("1");
        quantity.setHorizontalAlignment(SwingConstants.CENTER);
        quantity.setPreferredSize(new Dimension(90, 38));
        JButton increment = stepperButton("+");
        quantityRow.add(decrement);
        quantityRow.add(quantity);
        quantityRow.add(increment);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(textLabel("Batch quantity", 14, true, theme.text()), BorderLayout.NORTH);
        top.add(quantityRow, BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);
        JPanel previewRows = verticalPanel();
        JScrollPane previewScroll = scroll(previewRows);
        content.add(previewScroll, BorderLayout.CENTER);
        JLabel warning = textLabel("", 12, true, theme.danger());
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(warning, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        TowHitchCore.RoundedButton cancel = actionButton("Cancel", theme.input(), theme.text());
        TowHitchCore.RoundedButton confirm = actionButton(
                "Confirm Production", GREEN, new Color(0xF8FAFC));
        buttons.add(cancel);
        buttons.add(confirm);
        bottom.add(buttons, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);

        Runnable refreshPreview = () -> {
            int amount = parseBatchQuantity(quantity.getText());
            previewRows.removeAll();
            boolean sufficient = amount > 0;
            List<TowHitchCore.InventoryItem> latest;
            try {
                latest = TowHitchCore.loadInventory();
            } catch (IOException e) {
                showDataError("Unable to load current inventory", e);
                latest = new ArrayList<>();
                sufficient = false;
            }
            Map<String, Long> required = new LinkedHashMap<>();
            Map<String, String> names = new LinkedHashMap<>();
            for (TowHitchCore.BOMItem component : recipe) {
                String key = component.materialName.toLowerCase(Locale.ROOT);
                required.merge(key, (long) component.quantityRequired * Math.max(1, amount),
                        Long::sum);
                names.putIfAbsent(key, component.materialName);
            }
            for (Map.Entry<String, Long> line : required.entrySet()) {
                int matches = 0;
                int available = 0;
                for (TowHitchCore.InventoryItem item : latest) {
                    if (!"F".equals(item.type)
                            && item.itemName.equalsIgnoreCase(names.get(line.getKey()))) {
                        matches++;
                        available = item.quantity;
                    }
                }
                boolean enough = matches == 1 && available >= line.getValue();
                sufficient &= enough;
                JLabel detail = textLabel(names.get(line.getKey()) + ": Needed "
                        + line.getValue() + " vs. Available "
                        + (matches == 1 ? available : matches == 0 ? "missing" : "duplicate"),
                        13, true, enough ? theme.success() : theme.danger());
                TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(10);
                row.setBackground(theme.row());
                row.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
                row.setLayout(new BorderLayout());
                row.add(detail, BorderLayout.CENTER);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
                previewRows.add(row);
                previewRows.add(Box.createVerticalStrut(6));
            }
            confirm.setEnabled(sufficient && !recipe.isEmpty());
            warning.setText(sufficient ? " " : "Insufficient raw material for this batch.");
            previewRows.revalidate();
            previewRows.repaint();
        };
        quantity.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { refreshPreview.run(); }
            @Override public void removeUpdate(DocumentEvent event) { refreshPreview.run(); }
            @Override public void changedUpdate(DocumentEvent event) { refreshPreview.run(); }
        });
        decrement.addActionListener(event -> adjustBatchQuantity(quantity, -1));
        increment.addActionListener(event -> adjustBatchQuantity(quantity, 1));
        cancel.addActionListener(event -> dialog.dispose());
        confirm.addActionListener(event -> {
            int amount = parseBatchQuantity(quantity.getText());
            if (amount < 1) {
                warning.setText("Enter a quantity of at least 1.");
                return;
            }
            if (JOptionPane.showConfirmDialog(dialog,
                    "Produce " + amount + " " + product.itemName
                            + " unit(s) and deduct the required raw materials?",
                    "Confirm Production", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
            produce(product.itemName, amount);
            dialog.dispose();
        });
        refreshPreview.run();
        dialog.add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private JButton stepperButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setForeground(theme.secondaryText());
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(24, 26));
        return button;
    }

    private void adjustBatchQuantity(JTextField quantity, int delta) {
        try {
            int current = Integer.parseInt(quantity.getText().trim());
            int next = Math.max(1, Math.min(9999, current + delta));
            quantity.setText(Integer.toString(next));
        } catch (NumberFormatException e) {
            quantity.setText("1");
        }
    }

    private int parseBatchQuantity(String value) {
        try {
            int quantity = Integer.parseInt(value.trim());
            return quantity >= 1 && quantity <= 9999 ? quantity : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
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
                if (component.quantityRequired <= 0 || !validField(component.materialName)) {
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
            TowHitchCore.appendHistory("Produced: " + batchQuantity + " " + productName
                    + " (by " + currentStaffName + ")");
            for (Map.Entry<String, Long> entry : deductions.entrySet()) {
                TowHitchCore.appendHistory("Deducted: " + entry.getValue() + " "
                        + materialNames.get(entry.getKey()) + " (by " + currentStaffName + ")");
            }
            currentPage = "Production";
            TowHitchCore.showToast(frame, batchQuantity + " " + productName
                    + " unit(s) produced", true);
            buildApplication();
        } catch (IOException e) {
            showDataError("Unable to save production batch", e);
        }
    }

    private JPanel buildUnfinishedPage() {
        JPanel page = pageShell("Raw Materials",
                "Raw material stock directory with live availability status and restock request submission.");
        TowHitchCore.RoundedButton create = actionButton("+ New Stock Request",
                theme.primaryButton(), Color.WHITE);
        create.addActionListener(event -> openRequestDialog(null));
        JPanel header = (JPanel) page.getComponent(0);
        header.add(create, BorderLayout.EAST);
        JPanel rows = verticalPanel();
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        JPanel columnHeader = tableHeader(new String[] {
                "Status", "Material Name", "On Hand", "Pending Request", ""
        }, UNFINISHED_COLUMNS);
        JPanel data = verticalPanel();
        Map<String, Long> pendingByMaterial = new LinkedHashMap<>();
        try {
            for (TowHitchCore.StockRequest request : TowHitchCore.loadStockRequests()) {
                if ("PENDING".equalsIgnoreCase(request.status)) {
                    String key = request.materialName.toLowerCase(Locale.ROOT);
                    pendingByMaterial.merge(key, (long) request.quantity, Long::sum);
                }
            }
        } catch (IOException e) {
            showDataError("Unable to load pending stock requests", e);
        }
        for (TowHitchCore.InventoryItem item : safeInventory()) {
            if ("F".equals(item.type) || !matchesSearch(item.itemName)) continue;
            String key = item.itemName.toLowerCase(Locale.ROOT);
            data.add(materialRow(item, pendingByMaterial.getOrDefault(key, 0L),
                    submittedRestockQuantities.get(key)));
            data.add(Box.createVerticalStrut(7));
        }
        if (data.getComponentCount() == 0) data.add(textLabel("No raw materials found.", 13,
                false, theme.secondaryText()));
        JScrollPane dataScroll = scroll(data);
        alignHeaderToTableViewport(columnHeader, dataScroll);
        list.add(columnHeader, BorderLayout.NORTH);
        list.add(dataScroll, BorderLayout.CENTER);
        rows.add(list);
        page.add(sectionContainer(rows), BorderLayout.CENTER);
        return page;
    }

    private JPanel materialRow(TowHitchCore.InventoryItem item, long pendingQuantity,
                               Integer submittedQuantity) {
        JPanel row = rowPanel();
        addCell(row, new TowHitchCore.StatusPillBadge(stockStatus(item), theme), 0,
                UNFINISHED_COLUMNS, SwingConstants.CENTER);
        JPanel nameDetails = new JPanel();
        nameDetails.setOpaque(false);
        nameDetails.setLayout(new BoxLayout(nameDetails, BoxLayout.Y_AXIS));
        nameDetails.add(tableNameCell(item.itemName));
        if (submittedQuantity != null) {
            JLabel confirmation = textLabel(
                    "\u2713 Restock request submitted (+" + submittedQuantity + ")",
                    12, true, GREEN);
            confirmation.setFont(confirmation.getFont().deriveFont(11.5f));
            nameDetails.add(confirmation);
        }
        addCell(row, nameDetails, 1, UNFINISHED_COLUMNS, SwingConstants.LEFT);
        addCell(row, tableCell(Integer.toString(item.quantity)), 2,
                UNFINISHED_COLUMNS, SwingConstants.CENTER);
        JLabel pending = tableCell(pendingQuantity > 0
                ? "+" + pendingQuantity + " pending" : "\u2014");
        pending.setForeground(pendingQuantity > 0
                ? (theme.isDark() ? new Color(0xD6B77A) : new Color(0x8A6A32))
                : theme.secondaryText());
        addCell(row, pending, 3,
                UNFINISHED_COLUMNS, SwingConstants.CENTER);
        Color requestFill = theme.isDark() ? new Color(0xE5E0D8) : new Color(0x3F3D39);
        Color requestText = theme.isDark() ? new Color(0x1C1B19) : new Color(0xFAF8F5);
        TowHitchCore.RoundedButton request = actionButton("Request Restock", requestFill, requestText);
        request.setPreferredSize(new Dimension(135, 32));
        request.setMinimumSize(new Dimension(135, 32));
        request.addActionListener(event -> openRequestDialog(item.itemName));
        addCell(row, request, 4, UNFINISHED_COLUMNS, SwingConstants.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        return row;
    }

    private void openRequestDialog(String materialName) {
        JDialog dialog = themedDialog("New Stock Request", 420, 290);
        JPanel content = dialogContent();
        JTextField material = dialogField(materialName == null ? "" : materialName);
        if (materialName != null) material.setEditable(false);
        TowHitchCore.RoundedPanel materialBox = new TowHitchCore.RoundedPanel(12);
        materialBox.setBackground(theme.input());
        materialBox.setLayout(new BorderLayout());
        materialBox.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.row(), 12),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));
        material.setOpaque(false);
        material.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        materialBox.add(material, BorderLayout.CENTER);
        materialBox.setPreferredSize(new Dimension(230, 38));
        TowHitchCore.RoundedBorder.QuantityStepper quantity =
                new TowHitchCore.RoundedBorder.QuantityStepper(1, 1, 1000000, theme);
        quantity.setPreferredWidth(150);
        addFormRow(content, "Material Name", materialBox);
        addFormRow(content, "Requested Quantity", quantity);
        dialog.add(content, BorderLayout.CENTER);
        JPanel actions = dialogActions();
        TowHitchCore.RoundedButton cancel = actionButton("Cancel", theme.sidebar(), theme.text());
        cancel.addActionListener(event -> dialog.dispose());
        TowHitchCore.RoundedButton submit = actionButton("Submit Request", theme.primaryButton(), Color.WHITE);
        submit.addActionListener(event -> {
            String name = material.getText().trim();
            int requestedQuantity;
            try {
                requestedQuantity = quantity.getValue();
            } catch (IllegalArgumentException e) {
                error(e.getMessage());
                return;
            }
            if (!validField(name) || requestedQuantity < 1) {
                error("Enter a material name and a quantity greater than zero.");
                return;
            }
            try {
                boolean exists = false;
                for (TowHitchCore.InventoryItem item : TowHitchCore.loadInventory()) {
                    if (!"F".equals(item.type) && item.itemName.equalsIgnoreCase(name)) exists = true;
                }
                if (!exists) {
                    error("That raw material does not exist.");
                    return;
                }
                List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
                String id = "SR-" + System.currentTimeMillis();
                requests.add(new TowHitchCore.StockRequest(id, now(), currentStaffName,
                        name, requestedQuantity, "PENDING"));
                TowHitchCore.saveStockRequests(requests);
                TowHitchCore.appendHistory("Requested restock: "
                        + requestedQuantity + " " + name
                        + " - Requested by " + currentStaffName);
                dialog.dispose();
                submittedRestockQuantities.put(name.toLowerCase(Locale.ROOT),
                        requestedQuantity);
                currentPage = "Raw Materials";
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
        JPanel page = pageShell("Stock Requests",
                "Restock request ledger tracking material replenishment approvals between Staff and Owner.");
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        JPanel header = tableHeader(new String[] {
                "Status", "Material", "Qty", "Requested By", "Date"
        }, REQUEST_COLUMNS);
        JPanel rows = verticalPanel();
        try {
            List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
            requests.sort(Comparator.comparing((TowHitchCore.StockRequest r) ->
                    !"PENDING".equals(r.status)).thenComparing((a, b) ->
                    b.timestamp.compareTo(a.timestamp)));
            for (TowHitchCore.StockRequest request : requests) {
                JPanel row = rowPanel();
                addCell(row, new TowHitchCore.StatusPillBadge(request.status, theme), 0,
                        REQUEST_COLUMNS, SwingConstants.CENTER);
                addCell(row, tableNameCell(request.materialName), 1,
                        REQUEST_COLUMNS, SwingConstants.LEFT);
                addCell(row, tableCell(Integer.toString(request.quantity)), 2,
                        REQUEST_COLUMNS, SwingConstants.CENTER);
                addCell(row, tableCell(request.requestedBy), 3,
                        REQUEST_COLUMNS, SwingConstants.CENTER);
                addCell(row, tableCell(TowHitchCore.formatSmartDate(request.timestamp)), 4,
                        REQUEST_COLUMNS, SwingConstants.CENTER);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
                rows.add(row);
                rows.add(Box.createVerticalStrut(7));
            }
        } catch (IOException e) {
            showDataError("Unable to load stock requests", e);
        }
        if (rows.getComponentCount() == 0) rows.add(textLabel("No stock requests yet.", 13,
                false, theme.secondaryText()));
        JScrollPane dataScroll = scroll(rows);
        alignHeaderToTableViewport(header, dataScroll);
        list.add(header, BorderLayout.NORTH);
        list.add(dataScroll, BorderLayout.CENTER);
        page.add(sectionContainer(list), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildCustomerOrdersPage() {
        JPanel page = pageShell("Customer Orders",
                "Client order queue for reviewing pending orders and confirming product releases.");
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        JPanel header = tableHeader(new String[] {
                "Status", "Product", "Qty", "Customer", "Order Date", ""
        }, ORDER_COLUMNS);
        JPanel rows = verticalPanel();
        try {
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            orders.sort(Comparator.comparing((TowHitchCore.CustomerOrder o) ->
                    !"PENDING".equals(o.status)).thenComparing((a, b) ->
                    b.timestamp.compareTo(a.timestamp)));
            for (TowHitchCore.CustomerOrder order : orders) {
                rows.add(orderRow(order));
                rows.add(Box.createVerticalStrut(7));
            }
        } catch (IOException e) {
            showDataError("Unable to load customer orders", e);
        }
        if (rows.getComponentCount() == 0) rows.add(textLabel("No customer orders.", 13,
                false, theme.secondaryText()));
        JScrollPane dataScroll = scroll(rows);
        alignHeaderToTableViewport(header, dataScroll);
        list.add(header, BorderLayout.NORTH);
        list.add(dataScroll, BorderLayout.CENTER);
        page.add(sectionContainer(list), BorderLayout.CENTER);
        return page;
    }

    private JPanel orderRow(TowHitchCore.CustomerOrder order) {
        JPanel row = rowPanel();
        addCell(row, new TowHitchCore.StatusPillBadge(order.status, theme), 0,
                ORDER_COLUMNS, SwingConstants.CENTER);
        addCell(row, tableNameCell(order.productName), 1, ORDER_COLUMNS, SwingConstants.LEFT);
        addCell(row, tableCell(Integer.toString(order.quantity)), 2,
                ORDER_COLUMNS, SwingConstants.CENTER);
        addCell(row, tableCell(order.customerName), 3, ORDER_COLUMNS, SwingConstants.LEFT);
        addCell(row, tableCell(TowHitchCore.formatSmartDate(order.timestamp)),
                4, ORDER_COLUMNS, SwingConstants.LEFT);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actions.setOpaque(false);
        if ("PENDING".equals(order.status)) {
            TowHitchCore.RoundedButton release = actionButton(
                    "Confirm & Release", GREEN, new Color(0xF8FAFC));
            release.addActionListener(event -> releaseOrder(order));
            TowHitchCore.RoundedButton cancel = actionButton(
                    "Cancel Order", RED, new Color(0xFAF8F5));
            cancel.addActionListener(event -> cancelOrder(order));
            actions.add(release);
            actions.add(cancel);
        }
        addCell(row, actions, 5, ORDER_COLUMNS, SwingConstants.CENTER);
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
                    + selected.customerName + " - Released by " + currentStaffName);
            TowHitchCore.showToast(frame, "Order released to " + selected.customerName, true);
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
                    + " - Cancelled by " + currentStaffName);
            buildApplication();
        } catch (IOException e) {
            showDataError("Unable to cancel customer order", e);
        }
    }

    private JPanel buildHistoryPage(String title) {
        String subtitle = "Product Logs".equals(title)
                ? "Newest-first completed product assemblies."
                : "Stock Order Logs".equals(title)
                        ? "Newest-first approved restocks and released customer orders."
                        : "Newest-first raw material deductions.";
        JPanel page = pageShell(title, subtitle);
        JPanel header = (JPanel) page.getComponent(0);
        TowHitchCore.RoundedTextField search = new TowHitchCore.RoundedTextField(
                "Search by item, action, or date...", 28, theme);
        search.setMargin(new Insets(0, 10, 0, 8));
        RoundedSearchPanel searchContainer = new RoundedSearchPanel();
        searchContainer.setPreferredSize(new Dimension(350, 38));
        searchContainer.add(buildSearchLeading(), BorderLayout.WEST);
        searchContainer.add(search, BorderLayout.CENTER);
        header.add(searchContainer, BorderLayout.EAST);
        JPanel result = new JPanel(new BorderLayout());
        result.setOpaque(false);
        Runnable refresh = () -> {
            List<HistoryRecord> filtered = new ArrayList<>();
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            for (HistoryRecord record : readHistory()) {
                String formattedDate = TowHitchCore.formatSmartDateOnly(record.date);
                if (!query.isEmpty() && !record.message.toLowerCase(Locale.ROOT).contains(query)
                        && !record.display.toLowerCase(Locale.ROOT).contains(query)
                        && !record.date.toLowerCase(Locale.ROOT).contains(query)
                        && !formattedDate.toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
                boolean matchesSection = "Product Logs".equals(title)
                        ? isProductionRecord(record.message)
                        : "Stock Order Logs".equals(title)
                                ? isStockOrderRecord(record.message)
                                : isMaterialConsumptionRecord(record.message);
                if (matchesSection) {
                    filtered.add(record);
                }
            }
            result.removeAll();
            result.add(historyCard(title, filtered), BorderLayout.CENTER);
            result.revalidate();
            result.repaint();
        };
        search.addActionListener(event -> refresh.run());
        refresh.run();
        page.add(sectionContainer(result), BorderLayout.CENTER);
        return page;
    }

    private boolean isProductionRecord(String message) {
        return message.startsWith("Produced:");
    }

    private boolean isStockOrderRecord(String message) {
        String value = message.toLowerCase(Locale.ROOT);
        return value.contains("approved stock request")
                || value.contains("completed customer order");
    }

    private boolean isMaterialConsumptionRecord(String message) {
        String value = message.toLowerCase(Locale.ROOT);
        return value.startsWith("deducted:") || value.startsWith("consumed for ");
    }

    private JPanel buildTrashPage() {
        JPanel page = pageShell("Trash",
                "30-day recovery archive for recently deleted inventory items and records.");
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        int[] widths = {-1, 130, 60, 130, 160, 90, 230};
        JPanel header = tableHeader(new String[] {
                "Item / Record", "Category", "Qty", "Deleted By", "Deleted Date", "Expires In", ""
        }, widths);
        JPanel rows = verticalPanel();
        try {
            for (TowHitchCore.TrashEntry entry : TowHitchCore.loadTrash()) {
                rows.add(trashRow(entry, widths));
                rows.add(Box.createVerticalStrut(7));
            }
        } catch (IOException e) {
            showDataError("Unable to load trash_bin.txt", e);
        }
        if (rows.getComponentCount() == 0) {
            rows.add(textLabel("Trash is empty.", 13, false, theme.secondaryText()));
        }
        JScrollPane data = scroll(rows);
        alignHeaderToTableViewport(header, data);
        list.add(header, BorderLayout.NORTH);
        list.add(data, BorderLayout.CENTER);
        page.add(sectionContainer(list), BorderLayout.CENTER);
        return page;
    }

    private JPanel trashRow(TowHitchCore.TrashEntry entry, int[] widths) {
        JPanel row = rowPanel();
        addCell(row, tableNameCell(entry.itemName), 0, widths, SwingConstants.LEFT);
        addCell(row, tableCell(trashCategoryLabel(entry.category)), 1, widths, SwingConstants.LEFT);
        addCell(row, tableCell(Integer.toString(entry.quantity)), 2, widths, SwingConstants.CENTER);
        addCell(row, tableCell(entry.deletedBy), 3, widths, SwingConstants.LEFT);
        addCell(row, tableCell(TowHitchCore.formatSmartDate(entry.deletedAtIso)),
                4, widths, SwingConstants.LEFT);
        long age;
        try {
            age = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDateTime.parse(entry.deletedAtIso),
                    java.time.LocalDateTime.now());
        } catch (RuntimeException e) {
            age = 30;
        }
        long daysRemaining = Math.min(30, Math.max(0, 30 - age));
        addCell(row, tableCell(daysRemaining + " days"), 5, widths,
                SwingConstants.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        actions.setOpaque(false);
        TowHitchCore.RoundedButton restore = actionButton(
                "Restore", GREEN, new Color(0xF8FAFC));
        TowHitchCore.RoundedButton delete = actionButton(
                "Delete Permanently", RED, new Color(0xFAF8F5));
        restore.addActionListener(event -> restoreTrashEntry(entry));
        delete.addActionListener(event -> permanentlyDeleteTrashEntry(entry));
        actions.add(restore);
        actions.add(delete);
        addCell(row, actions, 6, widths, SwingConstants.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private void restoreTrashEntry(TowHitchCore.TrashEntry entry) {
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            String type = entry.category.equals("F") ? "F"
                    : entry.category.equals("U") ? "U" : "R";
            for (TowHitchCore.InventoryItem item : inventory) {
                if (item.type.equals(type) && item.itemName.equalsIgnoreCase(entry.itemName)) {
                    error("An item with this name already exists in inventory.");
                    return;
                }

            }
            inventory.add(new TowHitchCore.InventoryItem(type, entry.itemName,
                    entry.quantity, entry.minAlert));
            TowHitchCore.saveInventory(inventory);
            if (!entry.extraData.isEmpty()) {
                byte[] decoded = java.util.Base64.getDecoder().decode(entry.extraData);
                Map<String, List<TowHitchCore.BOMItem>> bom = TowHitchCore.loadBOM();
                String[] lines = new String(decoded, StandardCharsets.UTF_8).split("\\n");
                if ("F".equals(type)) {
                    List<TowHitchCore.BOMItem> recipe = new ArrayList<>();
                    for (int i = 1; i < lines.length; i++) {
                        String[] fields = lines[i].split("\\t", -1);
                        if (fields.length == 2) {
                            recipe.add(new TowHitchCore.BOMItem(fields[0],
                                    Integer.parseInt(fields[1])));
                        }
                    }
                    bom.put(entry.itemName, recipe);
                } else if (lines.length > 0 && "RAW".equals(lines[0])) {
                    for (int i = 1; i < lines.length; i++) {
                        String[] fields = lines[i].split("\\t", -1);
                        if (fields.length == 2) {
                            bom.computeIfAbsent(fields[0], ignored -> new ArrayList<>())
                                    .add(new TowHitchCore.BOMItem(entry.itemName,
                                            Integer.parseInt(fields[1])));
                        }
                    }
                }
                TowHitchCore.saveBOM(bom);
            }
            TowHitchCore.removeFromTrash(entry.id);
            TowHitchCore.appendHistory("Restored from Trash: " + entry.itemName
                    + " by " + currentStaffName);
            TowHitchCore.showToast(frame, entry.itemName + " restored", true);
            buildApplication();
        } catch (IOException | RuntimeException e) {
            showDataError("Unable to restore trash item",
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    private void permanentlyDeleteTrashEntry(TowHitchCore.TrashEntry entry) {
        if (JOptionPane.showConfirmDialog(frame,
                "Permanently delete '" + entry.itemName + "'? This cannot be undone.",
                "Delete Permanently", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            TowHitchCore.removeFromTrash(entry.id);
            TowHitchCore.appendHistory("Permanently deleted from Trash: " + entry.itemName
                    + " by " + currentStaffName);
            buildApplication();
        } catch (IOException e) {
            showDataError("Unable to delete trash item", e);
        }
    }

    private String trashCategoryLabel(String category) {
        return "F".equals(category) ? "Finished Goods" : "Raw Materials";
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
                if (message.startsWith("Deducted: ")) {
                    result.add(new HistoryRecord(message, formatHistoryDisplay(message), date, time));
                    continue;
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
            String producedWithActor = produced;
            int actor = produced.lastIndexOf(" (by ");
            if (actor >= 0 && produced.endsWith(")")) {
                produced = produced.substring(0, actor).trim();
            }
            int legacyActor = produced.indexOf(") by ");
            if (legacyActor >= 0) {
                produced = produced.substring(0, legacyActor + 1);
            }
            int firstSpace = produced.indexOf(' ');
            if (firstSpace > 0) {
                String quantity = produced.substring(0, firstSpace);
                if (quantity.chars().allMatch(Character::isDigit)) {
                    return "Produced " + producedWithActor;
                }
            }
            int open = produced.lastIndexOf(" (");
            if (open > 0 && produced.endsWith(")")) {
                return produced.substring(open + 2, produced.length() - 1) + " "
                        + produced.substring(0, open) + " produced";
            }
        }
        if (message.startsWith("Deducted: ")) {
            return message.substring("Deducted: ".length());
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
        card.setBackground(theme.isDark() ? new Color(0x1C1F26) : new Color(0xFAF8F4));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.setLayout(new BorderLayout(0, 10));
        card.add(textLabel(title, 16, true, theme.text()), BorderLayout.NORTH);
        JPanel rows = verticalPanel();
        for (HistoryRecord record : records) {
            String dateLabel = TowHitchCore.formatSmartDateOnly(record.date);
            if (rows.getComponentCount() == 0
                    || !dateLabel.equals(rows.getClientProperty("currentHistoryDate"))) {
                JLabel dateHeading = textLabel(dateLabel.isEmpty() ? "Unknown date" : dateLabel,
                        13, true, theme.text());
                dateHeading.setFont(dateHeading.getFont().deriveFont(13.5f));
                dateHeading.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0));
                dateHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
                rows.add(dateHeading);
                rows.putClientProperty("currentHistoryDate", dateLabel);
            }
            JPanel pill = new TowHitchCore.RoundedPanel(12);
            pill.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
            pill.setBorder(BorderFactory.createCompoundBorder(
                    new TowHitchCore.RoundedBorder(theme.isDark()
                            ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                    BorderFactory.createEmptyBorder(9, 11, 9, 11)));
            pill.setLayout(new BorderLayout(8, 0));
            JLabel message = textLabel(record.display, 12, false, theme.text());
            pill.add(message, BorderLayout.CENTER);
            pill.add(textLabel(TowHitchCore.formatHistoryTime(record.time), 12, false,
                    theme.secondaryText()), BorderLayout.EAST);
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

    private JPanel tableHeader(String[] labels, int[] widths) {
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
            addCell(header, label, i, widths, tableAlignment(i, widths));
        }
        return header;
    }

    private void alignHeaderToTableViewport(JPanel header, JScrollPane scroll) {
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        int scrollbarWidth = scroll.getVerticalScrollBar().getPreferredSize().width;
        header.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(10, 10, 10, 10 + scrollbarWidth)));
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

    private int tableAlignment(int column, int[] widths) {
        if (widths == REQUEST_COLUMNS && (column == 3 || column == 4)) {
            return SwingConstants.CENTER;
        }
        return column == 0 || column == 2 || column == 3 || column == 5
                || widths == UNFINISHED_COLUMNS && column == 4
                ? SwingConstants.CENTER : SwingConstants.LEFT;
    }

    private void addCell(JPanel row, Component component, int column, int[] widths,
                         int alignment) {
        double[] weights = new double[widths.length];
        for (int i = 0; i < widths.length; i++) {
            weights[i] = widths[i] < 0 ? 1.0 : 0.0;
        }
        TowHitchCore.addAlignedTableCell(row, component, column, widths, weights, alignment);
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
        return themedDialog(title, width, height, false);
    }

    private JDialog themedDialog(String title, int width, int height, boolean borderless) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(frame);
        TowHitchCore.RoundedPanel surface = new TowHitchCore.RoundedPanel(
                borderless ? 22 : 20);
        surface.setBackground(theme.panel());
        surface.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder.RoundedShadowBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xDCD8CE),
                        borderless ? 22 : 20, !borderless),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        surface.setLayout(new BorderLayout(0, 12));
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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        return panel;
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
                ? 120 : 220, 38));
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
            return TowHitchCore.loadSystemNotifications("STAFF", currentStaffName);
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
            addProfileDetail(card, "Full Name", currentDisplayName);
            addProfileDetail(card, "Username", "@" + activeUser.username);
            addProfileDetail(card, "Role", "Crew");
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
            JLabel title = textLabel("Dark Mode", 14, true, theme.text());
            title.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 4));
            card.add(title);
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
            labels.add(textLabel(currentDisplayName, 13, true, theme.text()));
            labels.add(Box.createVerticalStrut(3));
            labels.add(textLabel("Crew \u2022 @" + activeUser.username,
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
            badge.add(textLabel(value.toUpperCase(Locale.ROOT), 10, true, theme.text()));
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
            List<TowHitchCore.StaffAccount> accounts = TowHitchCore.loadStaffAccounts();
            List<TowHitchCore.StaffAccount> updatedAccounts = new ArrayList<>();
            boolean staffAccountFound = false;
            for (TowHitchCore.StaffAccount account : accounts) {
                if (account.username.equalsIgnoreCase(activeUser.username)) {
                    staffAccountFound = true;
                    updatedAccounts.add(new TowHitchCore.StaffAccount(account.id,
                            account.fullName, account.displayName, account.username, updated,
                            account.status, account.createdAtIso));
                } else {
                    updatedAccounts.add(account);
                }
            }
            if (staffAccountFound) {
                TowHitchCore.saveStaffAccounts(updatedAccounts);
            } else {
                List<TowHitchCore.UserAccount> users = TowHitchCore.loadUsers();
                boolean userAccountFound = false;
                for (TowHitchCore.UserAccount user : users) {
                    if (user.username.equalsIgnoreCase(activeUser.username)) {
                        user.setPassword(updated);
                        userAccountFound = true;
                    }
                }
                if (!userAccountFound) {
                    throw new IOException("The signed-in account could not be found.");
                }
                TowHitchCore.saveUsers(users);
            }
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
                "Display name: " + currentDisplayName + "\nUsername: " + activeUser.username
                        + "\nRole: Crew",
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
        currentStaffName = "";
        currentDisplayName = "";
        currentPage = "Dashboard";
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
            TowHitchCore.paintThemeSwitch(g, getWidth(), getHeight(), position,
                    theme.isDark());
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
                int width = Math.min(468, getWidth() - 32);
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
