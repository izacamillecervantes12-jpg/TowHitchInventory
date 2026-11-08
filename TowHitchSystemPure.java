import java.awt.BorderLayout;
import java.awt.AWTEvent;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class TowHitchSystemPure {
    private static final String BUSINESS_NAME = "Tow Hitch by 5-21";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "1234";
    private static final Path DATA_FILE = Paths.get("inventory_data.txt");
    private static final Path HISTORY_FILE = Paths.get("history_logs.txt");
    private static final DateTimeFormatter HISTORY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int DASHBOARD_WARNING_THRESHOLD = 15;
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);

    private final Map<String, Integer> finishedGoods = new LinkedHashMap<>();
    private final Map<String, Integer> rawMaterials = new LinkedHashMap<>();
    private final Map<String, Integer> pendingStockUpdates = new LinkedHashMap<>();
    private final Map<String, List<BOMItem>> billOfMaterials = new LinkedHashMap<>();
    private final ThemeController theme = new ThemeController();
    private final ImageIcon lightWallpaper;
    private final ImageIcon darkWallpaper;
    private final ImageIcon loginLogo;
    private final ImageIcon sidebarLogoLight;
    private final ImageIcon sidebarLogoDark;
    private final ImageIcon dashboardMenuIcon;
    private final ImageIcon inventoryMenuIcon;
    private final ImageIcon productionMenuIcon;
    private final ImageIcon historyMenuIcon;
    private final ImageIcon notificationIcon;
    private final ImageIcon settingsIcon;

    private JFrame frame;
    private JPanel applicationRoot;
    private JPanel pageHost;
    private JPanel sidebarPanel;
    private JTextField searchField;
    private FlatIconButton notificationButton;
    private FlatIconButton settingsButton;
    private JPopupMenu notificationPopup;
    private JPopupMenu settingsPopup;
    private JDialog exitConfirmationDialog;
    private AWTEventListener popupDismissListener;
    private AWTEventListener globalFocusClearListener;
    private boolean isNotificationOpen;
    private boolean isSettingsOpen;
    private Boolean notificationOpenAtButtonPress;
    private Boolean settingsOpenAtButtonPress;
    private JLabel settingsStatus;
    private JLabel stockSaveStatus;
    private RoundedPanel searchHighlightComponent;
    private String searchHighlightName = "";
    private Timer searchFlashTimer;
    private Color searchFlashOriginalColor;
    private JLabel loginMessage;
    private JLabel loginTitle;
    private JLabel loginSubtitle;
    private JLabel loginFooter;
    private LoginBackgroundPanel loginBackground;
    private String currentPage = "Dashboard";
    private String searchText = "";
    private boolean loggedIn;
    private boolean sidebarExpanded = true;
    private int sidebarWidth = 220;
    private Timer sidebarAnimation;
    private final Map<String, Boolean> expandedBOMCards = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TowHitchSystemPure().start());
    }

    private TowHitchSystemPure() {
        initializeInventory();
        initializeBOM();
        lightWallpaper = loadImageAsset("securitybg_light.jpg");
        darkWallpaper = loadImageAsset("securitybg_dark.jpg");
        loginLogo = loadScaledLoginLogo("logo_darknlight.png", 64);
        sidebarLogoLight = loadImageAsset("sidebar_logo_light.png");
        sidebarLogoDark = loadImageAsset("sidebar_logo_dark.png");
        dashboardMenuIcon = loadScaledImageAsset("dashboard_icon.png", 20, 20);
        inventoryMenuIcon = loadScaledImageAsset("inventory_icon.png", 20, 20);
        productionMenuIcon = loadScaledImageAsset("production_icon.png", 20, 20);
        historyMenuIcon = loadScaledImageAsset("history_icon.png", 20, 20);
        notificationIcon = loadScaledImageAsset("notification_icon.png", 24, 24);
        settingsIcon = loadScaledImageAsset("settings_icon.png", 24, 24);
        installGlobalFocusClearListener();
    }

    private void installGlobalFocusClearListener() {
        globalFocusClearListener = event -> {
            if (!(event instanceof MouseEvent)
                    || ((MouseEvent) event).getID() != MouseEvent.MOUSE_PRESSED) {
                return;
            }
            Component source = event.getSource() instanceof Component
                    ? (Component) event.getSource() : null;
            if (source == null || hasTextInputAncestor(source)) {
                return;
            }
            java.awt.Window window = SwingUtilities.getWindowAncestor(source);
            boolean applicationWindow = window == frame
                    || window == exitConfirmationDialog
                    || isDescendantOf(source, notificationPopup)
                    || isDescendantOf(source, settingsPopup);
            if (!applicationWindow
                    || hasAncestorOfType(source, AbstractButton.class)
                    || hasAncestorOfType(source, JScrollBar.class)
                    || !(window instanceof javax.swing.RootPaneContainer)) {
                return;
            }
            JComponent rootPane =
                    ((javax.swing.RootPaneContainer) window).getRootPane();
            rootPane.setFocusable(true);
            rootPane.setRequestFocusEnabled(true);
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .clearGlobalFocusOwner();
            rootPane.requestFocusInWindow();
            SwingUtilities.invokeLater(() -> {
                if (window.isShowing()) {
                    rootPane.requestFocusInWindow();
                }
            });
        };
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
                globalFocusClearListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private boolean hasTextInputAncestor(Component component) {
        for (Component current = component; current != null; current = current.getParent()) {
            if (current instanceof JTextField || current instanceof JPasswordField) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAncestorOfType(Component component, Class<?> componentType) {
        for (Component current = component; current != null; current = current.getParent()) {
            if (componentType.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private void configureFocusablePanels(Component component) {
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            panel.setFocusable(true);
            panel.setRequestFocusEnabled(true);
        }
        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) {
                configureFocusablePanels(child);
            }
        }
    }

    private void start() {
        frame = new JFrame(BUSINESS_NAME + " | Inventory");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980, 680));
        frame.setSize(1208, 720);
        frame.setLocationRelativeTo(null);
        installPopupDismissListener();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeApplication();
            }
        });

        try {
            loadInventory();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to load inventory data:\n" + e.getMessage()
                            + "\nThe application will continue with default stock levels.",
                    "Inventory Load Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            initializeHistoryFile();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to initialize production history:\n" + e.getMessage(),
                    "History Initialization Error", JOptionPane.ERROR_MESSAGE);
        }

        showLogin();
        frame.setVisible(true);
    }

    private void showLogin() {
        loggedIn = false;
        LoginBackgroundPanel root = new LoginBackgroundPanel();
        root.setFocusable(true);
        loginBackground = root;

        GlassLoginCard loginCard = new GlassLoginCard();
        loginCard.setLayout(new javax.swing.BoxLayout(loginCard, javax.swing.BoxLayout.Y_AXIS));
        loginCard.setBorder(BorderFactory.createEmptyBorder(0, 61, 12, 61));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new GridBagLayout());
        content.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        JLabel badge = new JLabel(loginLogo);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(64, 64));
        badge.setMaximumSize(new Dimension(64, 64));
        badge.setOpaque(false);
        addLoginGridRow(content, badge, 0, 0, 4, 0, GridBagConstraints.CENTER);

        loginTitle = new JLabel("Welcome Back", SwingConstants.CENTER);
        loginTitle.setFont(new Font("SansSerif", Font.BOLD, 34));
        loginTitle.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        loginTitle.setPreferredSize(new Dimension(394, 41));
        loginTitle.setMaximumSize(new Dimension(394, 41));
        addLoginGridRow(content, loginTitle, 1, 0, 3, 0, GridBagConstraints.CENTER);

        loginSubtitle = new JLabel("Access your inventory dashboard", SwingConstants.CENTER);
        loginSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 20));
        loginSubtitle.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x625E5C));
        loginSubtitle.setPreferredSize(new Dimension(394, 25));
        loginSubtitle.setMaximumSize(new Dimension(394, 25));
        addLoginGridRow(content, loginSubtitle, 2, 0, 5, 0, GridBagConstraints.CENTER);

        JLabel usernameLabel = loginLabel("Username");
        LoginTextField usernameField = new LoginTextField("Enter your username");
        JLabel passwordLabel = loginLabel("Password");
        LoginPasswordField passwordField = new LoginPasswordField("Enter password");
        addLoginGridRow(content, javax.swing.Box.createVerticalStrut(6),
                3, 0, 0, 0, GridBagConstraints.CENTER);
        addLoginGridRow(content, usernameLabel, 4, 31, 5, 0, GridBagConstraints.WEST);
        addLoginGridRow(content, usernameField, 5, 0, 15, 16, GridBagConstraints.WEST);
        addLoginGridRow(content, passwordLabel, 6, 5, 5, 0, GridBagConstraints.WEST);
        addLoginGridRow(content, passwordField, 7, 0, 0, 16, GridBagConstraints.WEST);

        loginMessage = new JLabel(" ");
        loginMessage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        loginMessage.setForeground(new Color(0x9D3932));
        loginMessage.setPreferredSize(new Dimension(394, 25));
        loginMessage.setMaximumSize(new Dimension(394, 25));
        addLoginGridRow(content, loginMessage, 8, 0, 0, 0, GridBagConstraints.WEST);

        LoginButton signIn = new LoginButton("Log in");
        signIn.setMaximumSize(new Dimension(394, 72));
        signIn.setPreferredSize(new Dimension(394, 52));
        signIn.addActionListener(event -> attemptLogin(usernameField, passwordField));
        passwordField.addActionListener(event -> attemptLogin(usernameField, passwordField));
        addLoginGridRow(content, signIn, 9, 35, 10, 20, GridBagConstraints.CENTER);

        Dimension contentSize = content.getPreferredSize();
        contentSize.width = 394;
        content.setPreferredSize(contentSize);
        content.setMaximumSize(contentSize);

        loginFooter = new LoginFooterLabel(
                "Tow Hitch by 5-21 | Inventory Management", SwingConstants.CENTER);
        loginFooter.setFont(new Font("SansSerif", Font.PLAIN, 16));
        loginFooter.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x686361));
        loginFooter.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        loginFooter.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        loginFooter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginCard.add(content);
        loginCard.add(javax.swing.Box.createVerticalGlue());
        loginCard.add(loginFooter);
        loginCard.setPreferredSize(new Dimension(516, loginCard.getPreferredSize().height));
        ThemeSwitchButton themeSwitch = new ThemeSwitchButton();
        themeSwitch.setToolTipText("Toggle light and dark themes");
        root.setLoginComponents(loginCard, themeSwitch);
        frame.setContentPane(root);
        configureFocusablePanels(root);
        frame.revalidate();
        frame.repaint();
    }

    private JLabel loginLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 21));
        label.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        label.setPreferredSize(new Dimension(394, 27));
        label.setMaximumSize(new Dimension(394, 27));
        return label;
    }

    private void addLoginGridRow(JPanel panel, java.awt.Component component,
            int row, int topInset, int bottomInset, int verticalPadding, int anchor) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = verticalPadding > 0
                ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        constraints.anchor = anchor;
        constraints.insets = new Insets(topInset, 0, bottomInset, 0);
        constraints.ipady = verticalPadding;
        panel.add(component, constraints);
    }

    private void attemptLogin(JTextField usernameField, JPasswordField passwordField) {
        String enteredPassword = new String(passwordField.getPassword());
        if (USERNAME.equals(usernameField.getText().trim()) && PASSWORD.equals(enteredPassword)) {
            loggedIn = true;
            currentPage = "Dashboard";
            searchText = "";
            buildApplication();
        } else {
            loginMessage.setText("Incorrect username or password.");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    private void buildApplication() {
        if (sidebarAnimation != null && sidebarAnimation.isRunning()) {
            sidebarAnimation.stop();
            sidebarWidth = sidebarExpanded ? 220 : 0;
        }
        closeNotificationDropdown();
        closeSettingsDropdown();
        applicationRoot = new GradientPanel();
        applicationRoot.setFocusable(true);
        applicationRoot.setLayout(new BorderLayout(14, 0));
        applicationRoot.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel mainArea = new JPanel(new BorderLayout(0, 16));
        mainArea.setOpaque(false);
        mainArea.add(buildTopBar(), BorderLayout.NORTH);
        pageHost = new JPanel(new CardLayout());
        pageHost.setOpaque(false);
        mainArea.add(pageHost, BorderLayout.CENTER);

        sidebarPanel = buildSidebar();
        if (sidebarExpanded) {
            sidebarPanel.setPreferredSize(new Dimension(sidebarWidth, 100));
            applicationRoot.add(sidebarPanel, BorderLayout.WEST);
        }
        applicationRoot.add(mainArea, BorderLayout.CENTER);
        frame.setContentPane(applicationRoot);
        showPage(currentPage);
        configureFocusablePanels(applicationRoot);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel buildTopBar() {
        RoundedPanel bar = new RoundedPanel(20);
        bar.setLayout(new BorderLayout(12, 0));
        bar.setBackground(theme.panel());
        bar.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        searchField = new RoundedTextField("Search", 26);
        searchField.setText(searchText);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                searchText = searchField.getText().trim();
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                changed();
            }
        });
        searchField.addActionListener(event -> performGlobalSearch());
        RoundedSearchPanel searchInput = new RoundedSearchPanel();
        JPanel searchLeading = new JPanel(new GridBagLayout());
        searchLeading.setOpaque(false);
        GridBagConstraints leadingConstraints = new GridBagConstraints();
        leadingConstraints.gridy = 0;
        leadingConstraints.anchor = GridBagConstraints.CENTER;
        leadingConstraints.insets = new Insets(0, 0, 0, 10);
        searchLeading.add(new SearchGlass(), leadingConstraints);
        leadingConstraints.gridx = 1;
        leadingConstraints.insets = new Insets(0, 0, 0, 0);
        searchLeading.add(new SearchDivider(), leadingConstraints);
        searchInput.add(searchLeading, BorderLayout.WEST);
        searchInput.add(searchField, BorderLayout.CENTER);
        JPanel searchArea = new JPanel(new BorderLayout(10, 0));
        searchArea.setOpaque(false);
        FlatIconButton menuButton = new FlatIconButton("\u2630", "Toggle navigation");
        menuButton.addActionListener(event -> toggleSidebar());
        searchArea.add(menuButton, BorderLayout.WEST);
        searchArea.add(searchInput, BorderLayout.CENTER);
        bar.add(searchArea, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        actions.setOpaque(false);
        ThemeSwitchButton themeButton = new ThemeSwitchButton();
        themeButton.setToolTipText("Toggle light and dark themes");
        settingsButton = new FlatIconButton(
                loadTintedIcon(settingsIcon, 24, theme.text()), "Settings");
        settingsButton.addActionListener(event ->
                toggleSettings(consumeSettingsOpenAtButtonPress()));
        notificationButton = new FlatIconButton(
                loadTintedIcon(notificationIcon, 24, theme.text()), "Raw material notifications");
        settingsButton.setIconButtonSize(24);
        notificationButton.setIconButtonSize(24);
        notificationButton.setUnreadCount(getLowStockMaterials().size());
        notificationButton.addActionListener(event ->
                toggleNotificationDropdown(consumeNotificationOpenAtButtonPress()));
        actions.add(themeButton);
        actions.add(notificationButton);
        actions.add(settingsButton);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSidebar() {
        RoundedPanel sidebar = new RoundedPanel(24);
        sidebar.setLayout(new BorderLayout(0, 20));
        sidebar.setBackground(theme.sidebar());
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 18, 16));
        sidebar.setPreferredSize(new Dimension(220, 100));

        JPanel top = new JPanel(new BorderLayout(0, 24));
        top.setOpaque(false);
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setOpaque(false);
        ImageIcon sidebarLogo = theme.isDark() ? sidebarLogoDark : sidebarLogoLight;
        if (sidebarLogo != null) {
            JLabel logo = new JLabel(scaleToFitIcon(sidebarLogo, 172, 112));
            brand.add(logo);
        }
        top.add(brand, BorderLayout.NORTH);
        JPanel navigation = new JPanel(new GridLayout(0, 1, 0, 8));
        navigation.setOpaque(false);
        String[][] navigationItems = {
            {"Dashboard", "dashboard"},
            {"Stocks", "inventory"},
            {"Production", "production"},
            {"History / Archive", "history"}
        };
        for (String[] item : navigationItems) {
            String page = item[0];
            ImageIcon sourceIcon;
            switch (item[1]) {
                case "dashboard":
                    sourceIcon = dashboardMenuIcon;
                    break;
                case "inventory":
                    sourceIcon = inventoryMenuIcon;
                    break;
                case "production":
                    sourceIcon = productionMenuIcon;
                    break;
                default:
                    sourceIcon = historyMenuIcon;
                    break;
            }
            RoundedButton button = navButton(page, page.equals(currentPage), sourceIcon);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.addActionListener(event -> {
                navigateTo(page);
            });
            navigation.add(button);
        }
        top.add(navigation, BorderLayout.CENTER);
        sidebar.add(top, BorderLayout.NORTH);

        JPanel logoutArea = new JPanel(new java.awt.GridBagLayout());
        logoutArea.setOpaque(false);
        RoundedButton logout = navButton("Logout", false);
        logout.setHorizontalAlignment(SwingConstants.CENTER);
        logout.addActionListener(event -> logout());
        logoutArea.add(logout);
        sidebar.add(logoutArea, BorderLayout.SOUTH);
        return sidebar;
    }

    private void showPage(String page) {
        if (pageHost == null) {
            return;
        }
        stopSearchFlash();
        searchHighlightComponent = null;
        currentPage = page;
        pageHost.removeAll();
        JPanel screen;
        if ("Stocks".equals(page)) {
            screen = buildInventoryPage();
        } else if ("Production".equals(page)) {
            screen = buildProductionPage();
        } else if ("History / Archive".equals(page)) {
            screen = buildHistoryPage();
        } else {
            screen = buildDashboardPage();
            currentPage = "Dashboard";
        }
        pageHost.add(screen, "current");
        configureFocusablePanels(screen);
        ((CardLayout) pageHost.getLayout()).show(pageHost, "current");
        pageHost.revalidate();
        pageHost.repaint();
    }

    private void performGlobalSearch() {
        String query = searchField.getText().trim();
        searchText = query;
        if (query.isEmpty()) {
            searchHighlightName = "";
            showPage(currentPage);
            return;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        String matchedProduct = findCatalogMatch(finishedGoods, normalizedQuery);
        String matchedMaterial = findCatalogMatch(rawMaterials, normalizedQuery);
        String targetPage;
        String matchedName;
        if (matchedProduct != null && matchedProduct.equalsIgnoreCase(query)) {
            targetPage = "Production";
            matchedName = matchedProduct;
        } else if (matchedMaterial != null) {
            targetPage = "Stocks";
            matchedName = matchedMaterial;
        } else if (matchedProduct != null) {
            targetPage = "Production";
            matchedName = matchedProduct;
        } else {
            searchHighlightName = "";
            showPage(currentPage);
            return;
        }

        searchHighlightName = matchedName;
        if (targetPage.equals(currentPage)) {
            showPage(targetPage);
        } else {
            navigateTo(targetPage);
        }
        if (targetPage.equals(currentPage)) {
            SwingUtilities.invokeLater(this::scrollToSearchHighlight);
        }
    }

    private String findCatalogMatch(Map<String, Integer> catalog, String query) {
        for (String item : catalog.keySet()) {
            if (item.equalsIgnoreCase(query)) {
                return item;
            }
        }
        for (String item : catalog.keySet()) {
            if (item.toLowerCase(Locale.ROOT).contains(query)) {
                return item;
            }
        }
        return null;
    }

    private void scrollToSearchHighlight() {
        RoundedPanel match = searchHighlightComponent;
        if (match == null || !match.isShowing()) {
            return;
        }
        java.awt.Container ancestor = match.getParent();
        while (ancestor != null && !(ancestor instanceof JScrollPane)) {
            ancestor = ancestor.getParent();
        }
        if (ancestor instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) ancestor;
            java.awt.Rectangle bounds = SwingUtilities.convertRectangle(match,
                    new java.awt.Rectangle(0, 0, match.getWidth(), match.getHeight()),
                    scroll.getViewport());
            int centeredY = bounds.y - Math.max(0,
                    (scroll.getViewport().getHeight() - bounds.height) / 2);
            int maxY = Math.max(0, scroll.getViewport().getViewSize().height
                    - scroll.getViewport().getExtentSize().height);
            scroll.getViewport().setViewPosition(new java.awt.Point(
                    Math.max(0, bounds.x), Math.max(0, Math.min(centeredY, maxY))));
        } else {
            match.scrollRectToVisible(new java.awt.Rectangle(0, 0,
                    match.getWidth(), match.getHeight()));
        }
        flashSearchHighlight(match);
    }

    private void flashSearchHighlight(RoundedPanel component) {
        stopSearchFlash();
        Color original = component.getBackground();
        searchHighlightComponent = component;
        searchFlashOriginalColor = original;
        Color highlight = new Color(0xE7B84B);
        final int duration = 500;
        final int interval = 25;
        final int[] elapsed = {0};
        component.setBackground(highlight);
        searchFlashTimer = new Timer(interval, event -> {
            elapsed[0] += interval;
            if (elapsed[0] <= 150) {
                return;
            }
            float amount = Math.min(1.0f, (float) (elapsed[0] - 150) / duration);
            component.setBackground(interpolateColor(highlight, original, amount));
            component.repaint();
            if (amount >= 1.0f) {
                searchFlashTimer.stop();
                component.setBackground(original);
                component.repaint();
                searchFlashOriginalColor = null;
            }
        });
        searchFlashTimer.start();
    }

    private void stopSearchFlash() {
        if (searchFlashTimer != null && searchFlashTimer.isRunning()) {
            searchFlashTimer.stop();
        }
        if (searchHighlightComponent != null && searchFlashOriginalColor != null) {
            searchHighlightComponent.setBackground(searchFlashOriginalColor);
            searchHighlightComponent.repaint();
        }
        searchFlashOriginalColor = null;
    }

    private Color interpolateColor(Color start, Color end, float amount) {
        int red = Math.round(start.getRed() + (end.getRed() - start.getRed()) * amount);
        int green = Math.round(start.getGreen() + (end.getGreen() - start.getGreen()) * amount);
        int blue = Math.round(start.getBlue() + (end.getBlue() - start.getBlue()) * amount);
        return new Color(red, green, blue);
    }

    private JPanel buildDashboardPage() {
        JPanel page = createPage("Dashboard", "Overview for today.");
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        JPanel summaryCards = new JPanel(new GridLayout(1, 2, 16, 0));
        summaryCards.setOpaque(false);
        String finishedUnitCount;
        try {
            finishedUnitCount = String.valueOf(getFinishedUnitCount());
        } catch (IOException e) {
            finishedUnitCount = "Unavailable";
            JOptionPane.showMessageDialog(frame,
                    "Unable to calculate finished units from production history:\n" + e.getMessage(),
                    "Finished Units Error", JOptionPane.ERROR_MESSAGE);
        }
        summaryCards.add(buildSummaryCard("Finished Units", finishedUnitCount,
                "Produced in the last 72 hours"));
        summaryCards.add(buildSummaryCard("Low Stocks", String.valueOf(getLowStockMaterials().size()),
                "Raw materials at or below " + LOW_STOCK_THRESHOLD));
        body.add(summaryCards, BorderLayout.NORTH);

        RoundedPanel warnings = new RoundedPanel(24);
        warnings.setBackground(theme.dashboardPanel());
        warnings.setLayout(new BorderLayout(0, 12));
        warnings.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JPanel warningHeading = new JPanel();
        warningHeading.setOpaque(false);
        warningHeading.setLayout(new javax.swing.BoxLayout(
                warningHeading, javax.swing.BoxLayout.Y_AXIS));
        warningHeading.add(sectionHeading("\uD83D\uDEA8 Warnings"));
        JLabel warningContext = new JLabel(
                "Raw materials at or below 15 items alert level");
        warningContext.setFont(SMALL_FONT);
        warningContext.setForeground(theme.secondaryText());
        warningContext.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        warningHeading.add(warningContext);
        warnings.add(warningHeading, BorderLayout.NORTH);
        JPanel alertList = new JPanel();
        alertList.setOpaque(false);
        alertList.setLayout(new javax.swing.BoxLayout(alertList, javax.swing.BoxLayout.Y_AXIS));
        List<String> warningMaterials = getDashboardWarningMaterials();
        warningMaterials.sort((first, second) -> {
            int stockComparison = Integer.compare(
                    getStockQuantity(first), getStockQuantity(second));
            return stockComparison != 0 ? stockComparison
                    : String.CASE_INSENSITIVE_ORDER.compare(first, second);
        });
        for (String materialName : warningMaterials) {
            int quantity = getStockQuantity(materialName);
            if (quantity <= DASHBOARD_WARNING_THRESHOLD) {
                alertList.add(dashboardWarningRow(materialName, quantity));
                alertList.add(dashboardWarningDivider());
            }
        }
        if (alertList.getComponentCount() == 0) {
            alertList.add(emptyState("No stocks are at or below alert level."));
        }
        alertList.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        alertList.setAlignmentY(JComponent.TOP_ALIGNMENT);
        JScrollPane scroll = createScrollPane(alertList);
        scroll.getViewport().setViewPosition(new java.awt.Point(0, 0));
        warnings.add(scroll, BorderLayout.CENTER);
        body.add(warnings, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildSummaryCard(String title, String value, String description) {
        RoundedPanel card = new RoundedPanel(24);
        card.setBackground(theme.dashboardPanel());
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(BODY_FONT);
        titleLabel.setForeground(theme.secondaryText());
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        valueLabel.setForeground(theme.text());
        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(SMALL_FONT);
        descriptionLabel.setForeground(theme.secondaryText());
        JPanel valueBlock = new JPanel(new BorderLayout(0, 6));
        valueBlock.setOpaque(false);
        valueBlock.add(valueLabel, BorderLayout.NORTH);
        valueBlock.add(descriptionLabel, BorderLayout.CENTER);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueBlock, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInventoryPage() {
        JPanel page = createPage("Stocks", "Manage your raw materials.");
        RoundedPanel inventoryPanel = new RoundedPanel(22);
        inventoryPanel.setBackground(theme.panel());
        inventoryPanel.setLayout(new BorderLayout(0, 12));
        inventoryPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        inventoryPanel.add(buildInventoryRows(rawMaterials), BorderLayout.CENTER);
        JPanel saveControls = new JPanel(new BorderLayout(12, 0));
        saveControls.setOpaque(false);
        stockSaveStatus = new JLabel(" ");
        stockSaveStatus.setFont(SMALL_FONT);
        stockSaveStatus.setForeground(theme.success());
        saveControls.add(stockSaveStatus, BorderLayout.CENTER);
        RoundedButton saveButton = new RoundedButton(
                "Save", new Color(0x23211F), Color.WHITE);
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        saveButton.setPreferredSize(new Dimension(120, 44));
        saveButton.addActionListener(event -> {
            if (commitPendingStockUpdates()) {
                stockSaveStatus.setForeground(theme.success());
                stockSaveStatus.setText("Changes saved successfully!");
            } else {
                stockSaveStatus.setForeground(theme.danger());
                stockSaveStatus.setText("Unable to save changes. See the error message.");
            }
        });
        JPanel saveButtonHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        saveButtonHolder.setOpaque(false);
        saveButtonHolder.add(saveButton);
        saveControls.add(saveButtonHolder, BorderLayout.EAST);
        inventoryPanel.add(saveControls, BorderLayout.SOUTH);
        page.add(inventoryPanel, BorderLayout.CENTER);
        return page;
    }

    private JScrollPane buildInventoryRows(Map<String, Integer> inventory) {
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));
        rows.add(inventoryColumnHeader());
        rows.add(javax.swing.Box.createVerticalStrut(8));
        int visibleRows = 0;
        List<String> itemNames = new ArrayList<>(inventory.keySet());
        itemNames.sort(String.CASE_INSENSITIVE_ORDER);
        for (String itemName : itemNames) {
            rows.add(inventoryRow(itemName, inventory));
            rows.add(javax.swing.Box.createVerticalStrut(8));
            visibleRows++;
        }
        if (visibleRows == 0) {
            rows.add(emptyState("No inventory items match your search."));
        }
        return createScrollPane(rows);
    }

    private JPanel inventoryColumnHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 12));

        JLabel materialName = new JLabel("Material Name", SwingConstants.LEFT);
        materialName.setFont(new Font("SansSerif", Font.BOLD, 13));
        materialName.setForeground(theme.secondaryText());
        header.add(materialName, BorderLayout.CENTER);

        JPanel quantityColumns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        quantityColumns.setOpaque(false);
        quantityColumns.add(javax.swing.Box.createHorizontalStrut(36));

        JLabel quantity = new JLabel("Qty", SwingConstants.CENTER);
        quantity.setFont(new Font("SansSerif", Font.BOLD, 13));
        quantity.setForeground(theme.secondaryText());
        int quantityWidth = new JTextField("", 5).getPreferredSize().width;
        quantity.setPreferredSize(new Dimension(quantityWidth, quantity.getPreferredSize().height));
        quantityColumns.add(quantity);
        quantityColumns.add(javax.swing.Box.createHorizontalStrut(36));
        header.add(quantityColumns, BorderLayout.EAST);
        return header;
    }

    private JPanel inventoryRow(String itemName, Map<String, Integer> inventory) {
        RoundedPanel row = new RoundedPanel(14);
        row.setBackground(theme.row());
        if (itemName.equalsIgnoreCase(searchHighlightName)) {
            searchHighlightComponent = row;
        }
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 12));

        JLabel name = new JLabel(itemName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        JTextField quantity = new JTextField(String.valueOf(getStockQuantity(itemName)), 5);
        quantity.setHorizontalAlignment(SwingConstants.CENTER);
        quantity.setFont(new Font("SansSerif", Font.BOLD, 15));
        quantity.setForeground(theme.text());
        quantity.setBackground(theme.isDark() ? new Color(0x3A3734) : theme.input());
        quantity.setCaretColor(theme.text());
        quantity.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.row(), 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        quantity.setToolTipText("Click to enter an exact stock quantity");
        quantity.addActionListener(event -> quantity.transferFocus());
        quantity.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                quantity.selectAll();
            }

            @Override
            public void focusLost(FocusEvent event) {
                stageManualStockOverride(itemName, quantity);
            }
        });
        JPanel itemLabels = new JPanel(new BorderLayout(0, 4));
        itemLabels.setOpaque(false);
        itemLabels.add(name, BorderLayout.NORTH);
        JLabel stockStatus = new JLabel();
        stockStatus.setFont(SMALL_FONT);
        updateStockStatus(itemName, stockStatus);
        itemLabels.add(stockStatus, BorderLayout.SOUTH);
        row.add(itemLabels, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        RoundedButton minus = secondaryButton("-");
        RoundedButton plus = secondaryButton("+");
        minus.setPreferredSize(new Dimension(36, 36));
        plus.setPreferredSize(new Dimension(36, 36));
        installStockHoldBehavior(minus, itemName, inventory, -1, quantity, stockStatus);
        installStockHoldBehavior(plus, itemName, inventory, 1, quantity, stockStatus);
        controls.add(minus);
        controls.add(quantity);
        controls.add(plus);
        row.add(controls, BorderLayout.EAST);
        return row;
    }

    private JPanel buildProductionPage() {
        JPanel page = createPage("Production", "Produce products using their bill of materials.");
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);
        List<String> products = new ArrayList<>(finishedGoods.keySet());
        products.sort(String.CASE_INSENSITIVE_ORDER);
        if (products.isEmpty()) {
            GridBagConstraints emptyConstraints = new GridBagConstraints();
            emptyConstraints.gridx = 0;
            emptyConstraints.gridy = 0;
            emptyConstraints.weightx = 1.0;
            emptyConstraints.anchor = GridBagConstraints.NORTHWEST;
            emptyConstraints.fill = GridBagConstraints.HORIZONTAL;
            cards.add(emptyState("No products match your search."), emptyConstraints);
        } else {
            JPanel[] columns = {productionCardColumn(), productionCardColumn()};
            for (int index = 0; index < products.size(); index++) {
                JPanel column = columns[index % columns.length];
                JPanel card = buildProductionCard(products.get(index));
                column.add(card);
                column.add(javax.swing.Box.createVerticalStrut(14));
            }
            for (int columnIndex = 0; columnIndex < columns.length; columnIndex++) {
                GridBagConstraints columnConstraints = new GridBagConstraints();
                columnConstraints.gridx = columnIndex;
                columnConstraints.gridy = 0;
                columnConstraints.weightx = 1.0;
                columnConstraints.weighty = 1.0;
                columnConstraints.fill = GridBagConstraints.HORIZONTAL;
                columnConstraints.anchor = GridBagConstraints.NORTH;
                columnConstraints.insets = new Insets(0,
                        columnIndex == 0 ? 0 : 7, 0,
                        columnIndex == 0 ? 7 : 0);
                cards.add(columns[columnIndex], columnConstraints);
            }
        }
        JScrollPane scroll = createScrollPane(cards);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel productionCardColumn() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));
        column.setAlignmentY(JComponent.TOP_ALIGNMENT);
        return column;
    }

    private JPanel buildHistoryPage() {
        JPanel page = createPage("History / Archive",
                "Production and raw material consumption records.");
        JPanel panels = new JPanel(new GridLayout(1, 2, 16, 0));
        panels.setOpaque(false);
        List<HistoryEntry> productLogs = new ArrayList<>();
        List<HistoryEntry> consumptionLogs = new ArrayList<>();
        try {
            List<String> historyLines = Files.readAllLines(HISTORY_FILE, StandardCharsets.UTF_8);
            Collections.reverse(historyLines);
            for (String line : historyLines) {
                List<HistoryEntry> parsed = parseHistoryEntries(line);
                if (line.contains("] Produced:")) {
                    productLogs.addAll(parsed);
                } else if (line.contains("] Consumed for ")) {
                    consumptionLogs.addAll(parsed);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to read production history:\n" + e.getMessage(),
                    "History Read Error", JOptionPane.ERROR_MESSAGE);
        }
        panels.add(buildHistoryLogCard("Product Logs", productLogs));
        panels.add(buildHistoryLogCard("Material Consumption", consumptionLogs));
        page.add(panels, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildHistoryLogCard(String title, List<HistoryEntry> entries) {
        RoundedPanel card = new RoundedPanel(24);
        card.setBackground(theme.panel());
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.add(sectionHeading(title), BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));
        int visibleEntries = 0;
        for (HistoryEntry entry : entries) {
            RoundedPanel row = buildHistoryRow(entry);
            row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            rows.add(row);
            rows.add(javax.swing.Box.createVerticalStrut(8));
            visibleEntries++;
        }
        if (visibleEntries == 0) {
            rows.add(emptyState("No history records yet."));
        }
        card.add(createScrollPane(rows, true), BorderLayout.CENTER);
        return card;
    }

    private List<HistoryEntry> parseHistoryEntries(String line) {
        List<HistoryEntry> parsedEntries = new ArrayList<>();
        if (!line.startsWith("[") || !line.contains("] ")) {
            return parsedEntries;
        }
        int timestampEnd = line.indexOf("] ");
        String timestamp = line.substring(1, timestampEnd);
        int timestampSpace = timestamp.indexOf(' ');
        if (timestampSpace <= 0 || timestampSpace == timestamp.length() - 1) {
            return parsedEntries;
        }
        String date = timestamp.substring(0, timestampSpace);
        String time = timestamp.substring(timestampSpace + 1);
        String details = line.substring(timestampEnd + 2);

        if (details.startsWith("Produced: ")) {
            String itemText = parseQuantityAndName(details.substring("Produced: ".length()));
            if (itemText != null) {
                parsedEntries.add(new HistoryEntry(itemText, date, time));
            }
        } else if (details.startsWith("Consumed for ")) {
            int separator = details.indexOf(": ");
            if (separator < 0 || separator + 2 >= details.length()) {
                return parsedEntries;
            }
            String[] materials = details.substring(separator + 2).split(",\\s*");
            for (String material : materials) {
                String itemText = parseQuantityAndName(material);
                if (itemText != null) {
                    parsedEntries.add(new HistoryEntry(itemText, date, time));
                }
            }
        }
        return parsedEntries;
    }

    private String parseQuantityAndName(String itemDescription) {
        int quantityStart = itemDescription.lastIndexOf(" (");
        if (quantityStart < 0 || !itemDescription.endsWith(")")) {
            return null;
        }
        String quantity = itemDescription.substring(quantityStart + 2, itemDescription.length() - 1);
        String name = itemDescription.substring(0, quantityStart).trim();
        if (name.isEmpty() || quantity.isEmpty()) {
            return null;
        }
        return quantity + " " + name;
    }

    private RoundedPanel buildHistoryRow(HistoryEntry entry) {
        RoundedPanel row = new RoundedPanel(12);
        row.setBackground(theme.row());
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel product = historyCell(entry.itemText, false);
        JLabel dateLabel = historyCell(entry.date, true);
        JLabel separator = historyCell("|", true);
        JLabel timeLabel = historyCell(entry.time, true);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        row.add(product, constraints);
        constraints.weightx = 0.0;
        constraints.gridx = 1;
        constraints.ipadx = 12;
        row.add(dateLabel, constraints);
        constraints.gridx = 2;
        constraints.ipadx = 0;
        row.add(separator, constraints);
        constraints.gridx = 3;
        constraints.ipadx = 8;
        row.add(timeLabel, constraints);
        return row;
    }

    private JLabel historyCell(String text, boolean fixedColumn) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(fixedColumn ? new Font(Font.MONOSPACED, Font.PLAIN, 12) : SMALL_FONT);
        label.setForeground(theme.text());
        return label;
    }

    private JPanel buildProductionCard(String product) {
        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(theme.panel());
        if (product.equalsIgnoreCase(searchHighlightName)) {
            searchHighlightComponent = card;
        }
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(17, 18, 17, 18));

        JLabel name = new JLabel(product);
        name.setFont(new Font("SansSerif", Font.BOLD, 17));
        name.setForeground(theme.text());
        boolean expanded = Boolean.TRUE.equals(expandedBOMCards.get(product));
        RoundedButton expandButton = secondaryButton(expanded ? "\u25B2" : "\u25BC");
        expandButton.setPreferredSize(new Dimension(38, 34));
        expandButton.setToolTipText(expanded ? "Hide bill of materials" : "Show bill of materials");
        expandButton.addActionListener(event -> toggleProductionCard(product));
        JPanel productTitle = new JPanel(new BorderLayout(8, 0));
        productTitle.setOpaque(false);
        productTitle.add(name, BorderLayout.CENTER);
        productTitle.add(expandButton, BorderLayout.EAST);
        JLabel onHand = new JLabel("Finished units on hand: " + finishedGoods.get(product));
        onHand.setFont(SMALL_FONT);
        onHand.setForeground(theme.secondaryText());
        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setOpaque(false);
        heading.add(productTitle, BorderLayout.NORTH);
        heading.add(onHand, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);

        JPanel requirements = new JPanel();
        requirements.setOpaque(false);
        requirements.setLayout(new javax.swing.BoxLayout(requirements, javax.swing.BoxLayout.Y_AXIS));
        requirements.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        List<String> missingItems = getUnavailableBOMItems(product);
        for (BOMItem bomItem : billOfMaterials.get(product)) {
            int available = getStockQuantity(bomItem.materialName);
            JLabel line = new JLabel(bomItem.materialName + "   " + available + " available / "
                    + bomItem.quantityRequired + " required");
            line.setFont(SMALL_FONT);
            line.setForeground(available < bomItem.quantityRequired ? theme.danger() : theme.secondaryText());
            requirements.add(line);
        }
        JScrollPane requirementsScroll = createScrollPane(requirements);
        requirementsScroll.setPreferredSize(new Dimension(300, 150));
        requirementsScroll.setVisible(expanded);
        card.add(requirementsScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        JLabel status = new JLabel();
        status.setFont(SMALL_FONT);
        boolean materialsAvailable = missingItems.isEmpty();
        boolean finishedCapacityAvailable = finishedGoods.get(product) < Integer.MAX_VALUE;
        boolean stockAvailable = materialsAvailable && finishedCapacityAvailable;
        if (stockAvailable) {
            status.setText("Materials available");
            status.setForeground(theme.success());
        } else if (!materialsAvailable) {
            status.setText("Not Available for Production: Insufficient Raw Materials");
            status.setForeground(theme.danger());
        } else {
            status.setText("Not Available for Production: Finished stock limit reached");
            status.setForeground(theme.danger());
        }
        RoundedButton produce = primaryButton("Produce");
        produce.setEnabled(stockAvailable);
        produce.addActionListener(event -> produceProduct(product));
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(produce, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        card.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                card.getPreferredSize().height));
        return card;
    }

    private void toggleProductionCard(String product) {
        if (Boolean.TRUE.equals(expandedBOMCards.get(product))) {
            expandedBOMCards.remove(product);
        } else {
            expandedBOMCards.put(product, Boolean.TRUE);
        }
        showPage("Production");
    }

    private JPanel createPage(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(theme.text());
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(theme.secondaryText());
        heading.add(titleLabel, BorderLayout.NORTH);
        heading.add(subtitleLabel, BorderLayout.SOUTH);
        page.add(heading, BorderLayout.NORTH);
        return page;
    }

    private JPanel alertRow(String message) {
        RoundedPanel row = new RoundedPanel(13);
        row.setBackground(theme.row());
        row.setLayout(new BorderLayout());
        row.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        JLabel label = new JLabel(message);
        label.setFont(BODY_FONT);
        label.setForeground(theme.text());
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    private JPanel dashboardWarningRow(String materialName, int quantity) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.insets = new Insets(12, 0, 12, 0);
        constraints.weighty = 0.0;

        JLabel name = new JLabel(materialName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        name.setPreferredSize(new Dimension(200, name.getPreferredSize().height));
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        row.add(name, constraints);

        JLabel stock = new JLabel(quantity + " stocks");
        stock.setFont(BODY_FONT);
        stock.setForeground(theme.secondaryText());
        constraints.gridx = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        row.add(stock, constraints);
        constraints.gridx = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        row.add(new JPanel() {
            private static final long serialVersionUID = 1L;

            {
                setOpaque(false);
            }
        }, constraints);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private JComponent dashboardWarningDivider() {
        JComponent divider = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(new Color(127, 122, 117, 45));
                int y = getHeight() / 2;
                g.drawLine(12, y, getWidth() - 12, y);
                g.dispose();
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(1, 5));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        return divider;
    }

    private JPanel emptyState(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 8, 18, 8));
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(BODY_FONT);
        label.setForeground(theme.secondaryText());
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createScrollPane(JComponent content) {
        return createScrollPane(content, false);
    }

    private JScrollPane createScrollPane(JComponent content, boolean disableHorizontalScroll) {
        JScrollPane scroll = new JScrollPane(content);
        if (disableHorizontalScroll) {
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        }
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scroll.getVerticalScrollBar().setUI(new MinimalScrollBarUI());
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setOpaque(false);
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        scroll.getHorizontalScrollBar().setUI(new MinimalScrollBarUI());
        return scroll;
    }

    private final class MinimalScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createHiddenScrollButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createHiddenScrollButton();
        }

        private JButton createHiddenScrollButton() {
            JButton button = new JButton();
            Dimension hidden = new Dimension(0, 0);
            button.setPreferredSize(hidden);
            button.setMinimumSize(hidden);
            button.setMaximumSize(hidden);
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            return button;
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, java.awt.Rectangle bounds) {
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, java.awt.Rectangle bounds) {
            if (bounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color thumb = theme.isDark()
                    ? (isThumbRollover() ? new Color(0x85817C) : new Color(0x4A4643))
                    : (isThumbRollover() ? new Color(0x7F7A75) : new Color(0xB6B1AA));
            g.setColor(thumb);
            if (scrollbar.getOrientation() == javax.swing.JScrollBar.HORIZONTAL) {
                int height = 8;
                int y = bounds.y + (bounds.height - height) / 2;
                g.fillRoundRect(bounds.x, y, bounds.width, height, 6, 6);
            } else {
                int width = 8;
                int x = bounds.x + (bounds.width - width) / 2;
                g.fillRoundRect(x, bounds.y, width, bounds.height, 6, 6);
            }
            g.dispose();
        }
    }

    private JLabel sectionHeading(String text) {
        JLabel heading = new JLabel(text);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(theme.text());
        return heading;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(theme.text());
        return label;
    }

    private RoundedButton primaryButton(String text) {
        RoundedButton button = new RoundedButton(text, theme.primaryButton(), Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(150, 42));
        return button;
    }

    private RoundedButton secondaryButton(String text) {
        RoundedButton button = new RoundedButton(text, theme.input(), theme.text());
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(110, 38));
        return button;
    }

    private final class FlatIconButton extends JButton {
        private static final long serialVersionUID = 1L;
        private int unreadCount;

        private FlatIconButton(String icon, String tooltip) {
            super(icon);
            initialize(tooltip);
        }

        private FlatIconButton(javax.swing.Icon icon, String tooltip) {
            super(icon);
            initialize(tooltip);
        }

        private void initialize(String tooltip) {
            setToolTipText(tooltip);
            setFont(new Font("SansSerif", Font.PLAIN, 22));
            setForeground(theme.text());
            setPreferredSize(new Dimension(42, 42));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        private void setIconButtonSize(int size) {
            Dimension dimensions = new Dimension(size, size);
            setPreferredSize(dimensions);
            setMinimumSize(dimensions);
            setMaximumSize(dimensions);
            setMargin(new Insets(0, 0, 0, 0));
        }

        private void setUnreadCount(int count) {
            unreadCount = count;
            setToolTipText(count == 0 ? "No raw material alerts"
                    : count + " raw material alert" + (count == 1 ? "" : "s"));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            setForeground(theme.text());
            super.paintComponent(graphics);
            if (unreadCount > 0) {
                Graphics2D badge = (Graphics2D) graphics.create();
                badge.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                badge.setColor(theme.danger());
                badge.fillOval(getWidth() - 13, 3, 9, 9);
                badge.dispose();
            }
        }
    }

    private final class RoundedNotificationPopup extends JPopupMenu {
        private static final long serialVersionUID = 1L;
        private static final int CORNER_RADIUS = 18;

        private RoundedNotificationPopup() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.dashboardPanel());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    CORNER_RADIUS, CORNER_RADIUS);
            g.setColor(theme.row());
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    CORNER_RADIUS, CORNER_RADIUS);
            g.dispose();
        }
    }

    private final class ThemeSwitchButton extends JButton {
        private static final long serialVersionUID = 1L;
        private float position = theme.isDark() ? 1.0f : 0.0f;
        private Timer animation;

        private ThemeSwitchButton() {
            setPreferredSize(new Dimension(84, 40));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            addActionListener(event -> animateThemeChange());
        }

        private void animateThemeChange() {
            if (animation != null && animation.isRunning()) {
                animation.stop();
            }
            final float start = position;
            final float target = theme.isDark() ? 0.0f : 1.0f;
            final int[] step = {0};
            animation = new Timer(15, event -> {
                step[0]++;
                float fraction = Math.min(1.0f, step[0] / 10.0f);
                position = start + (target - start) * fraction;
                repaint();
                if (fraction >= 1.0f) {
                    animation.stop();
                    theme.toggle();
                    if (loggedIn) {
                        buildApplication();
                    } else {
                        refreshLoginTheme();
                    }
                }
            });
            animation.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(theme.isDark() ? new Color(0x34312F) : theme.sidebar());
            g.fillRoundRect(0, 1, width, height - 2, height - 2, height - 2);

            int knobSize = height - 10;
            int travel = width - knobSize - 10;
            int knobX = 5 + Math.round(travel * position);
            g.setColor(theme.isDark() ? new Color(0x303856) : new Color(0xFFFFFF));
            g.fillOval(knobX, 5, knobSize, knobSize);

            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            if (position < 0.5f) {
                g.setColor(new Color(0x23211F));
                g.drawString("\u2600", knobX + 8, 28);
                g.setColor(new Color(0x8B949E));
                g.setFont(new Font("SansSerif", Font.PLAIN, 18));
                g.drawString("\u263E", width - 25, 27);
            } else {
                g.setColor(new Color(0x969DAD));
                g.drawString("\u2600", 11, 28);
                g.setColor(new Color(0xF5E9B8));
                g.setFont(new Font("SansSerif", Font.PLAIN, 18));
                g.drawString("\u263E", knobX + 8, 28);
            }
            g.dispose();
        }
    }

    private void refreshLoginTheme() {
        if (loginTitle != null) {
            loginTitle.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
            loginSubtitle.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x625E5C));
            loginFooter.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x686361));
            loginMessage.setForeground(theme.danger());
            updateLoginLabelColors(loginBackground);
            loginBackground.repaint();
        }
    }

    private void updateLoginLabelColors(java.awt.Component component) {
        if (component instanceof JLabel && component != loginTitle
                && component != loginSubtitle && component != loginFooter
                && component != loginMessage) {
            component.setForeground(theme.isDark()
                        ? new Color(0xF5F1ED) : new Color(0x3A3534));
        }
        if (component instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) component).getComponents()) {
                updateLoginLabelColors(child);
            }
        }
    }

    private RoundedButton navButton(String text, boolean selected) {
        return navButton(text, selected, null);
    }

    private RoundedButton navButton(String text, boolean selected, ImageIcon sourceIcon) {
        Color background = selected ? theme.primaryButton() : theme.sidebar();
        Color foreground = selected ? Color.WHITE : theme.secondaryText();
        RoundedButton button = new RoundedButton(text, background, foreground);
        button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 14));
        button.setPreferredSize(new Dimension(184, 42));
        if (sourceIcon != null) {
            button.setIcon(loadTintedIcon(sourceIcon, 20, foreground));
            button.setIconTextGap(10);
        }
        return button;
    }

    private void installStockHoldBehavior(RoundedButton button, String itemName,
            Map<String, Integer> inventory, int delta, JTextField quantityField,
            JLabel stockStatus) {
        final boolean[] mousePressTriggered = {false};
        Timer repeatTimer = new Timer(75, event -> {
            if (!adjustStock(itemName, inventory, delta, quantityField, stockStatus)) {
                ((Timer) event.getSource()).stop();
            }
        });
        repeatTimer.setInitialDelay(400);

        button.addActionListener(event -> {
            if (!mousePressTriggered[0]) {
                adjustStock(itemName, inventory, delta, quantityField, stockStatus);
            }
        });
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                mousePressTriggered[0] = true;
                if (adjustStock(itemName, inventory, delta, quantityField, stockStatus)) {
                    repeatTimer.restart();
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                repeatTimer.stop();
                SwingUtilities.invokeLater(() -> mousePressTriggered[0] = false);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                repeatTimer.stop();
            }
        });
    }

    private boolean adjustStock(String itemName, Map<String, Integer> inventory, int delta,
            JTextField quantityField, JLabel stockStatus) {
        Integer pendingQuantity = pendingStockUpdates.get(itemName);
        int previous = pendingQuantity == null ? inventory.get(itemName) : pendingQuantity;
        long updated = (long) previous + delta;
        if (updated < 0) {
            JOptionPane.showMessageDialog(frame, "Stock cannot be reduced below zero.",
                    "Invalid Stock Adjustment", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (updated > Integer.MAX_VALUE) {
            JOptionPane.showMessageDialog(frame, "Stock quantity is at the maximum supported value.",
                    "Invalid Stock Adjustment", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        pendingStockUpdates.put(itemName, (int) updated);
        updateNotificationBadge();
        quantityField.setText(String.valueOf(updated));
        updateStockStatus(itemName, stockStatus);
        applicationRoot.repaint();
        return true;
    }

    private void updateStockStatus(String itemName, JLabel stockStatus) {
        if (getStockQuantity(itemName) <= LOW_STOCK_THRESHOLD) {
            stockStatus.setText("Low stock");
            stockStatus.setForeground(theme.danger());
        } else {
            stockStatus.setText("Raw material");
            stockStatus.setForeground(theme.secondaryText());
        }
    }

    private int getStockQuantity(String itemName) {
        Integer pending = pendingStockUpdates.get(itemName);
        return pending == null ? rawMaterials.get(itemName) : pending;
    }

    private void stageManualStockOverride(String itemName, JTextField quantityField) {
        String entered = quantityField.getText().trim();
        final int quantity;
        try {
            quantity = Integer.parseInt(entered);
        } catch (NumberFormatException e) {
            quantityField.setText(String.valueOf(getStockQuantity(itemName)));
            quantityField.setToolTipText("Enter a whole number from 0 to " + Integer.MAX_VALUE);
            return;
        }
        if (quantity < 0) {
            quantityField.setText(String.valueOf(getStockQuantity(itemName)));
            quantityField.setToolTipText("Stock cannot be reduced below zero.");
            return;
        }
        if (quantity == rawMaterials.get(itemName)) {
            pendingStockUpdates.remove(itemName);
        } else {
            pendingStockUpdates.put(itemName, quantity);
        }
        quantityField.setToolTipText(pendingStockUpdates.containsKey(itemName)
                ? "Unsaved stock update" : "Click to enter an exact stock quantity");
        updateNotificationBadge();
    }

    private boolean resolvePendingStockUpdates() {
        if (pendingStockUpdates.isEmpty()) {
            return true;
        }
        int choice = showThemedChoiceDialog("Unsaved Stock Updates",
                "You have unsaved stock updates. Save or Discard changes?",
                "Save", "Discard");
        if (choice == 1) {
            pendingStockUpdates.clear();
            updateNotificationBadge();
            return true;
        }
        if (choice != 0) {
            return false;
        }

        return commitPendingStockUpdates();
    }

    private boolean commitPendingStockUpdates() {
        if (pendingStockUpdates.isEmpty()) {
            return true;
        }
        Map<String, Integer> previousValues = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> update : pendingStockUpdates.entrySet()) {
            previousValues.put(update.getKey(), rawMaterials.get(update.getKey()));
            rawMaterials.put(update.getKey(), update.getValue());
        }
        try {
            saveInventory();
        } catch (IOException e) {
            for (Map.Entry<String, Integer> previous : previousValues.entrySet()) {
                rawMaterials.put(previous.getKey(), previous.getValue());
            }
            showSaveError(e);
            return false;
        }
        pendingStockUpdates.clear();
        updateNotificationBadge();
        return true;
    }

    private void navigateTo(String page) {
        if (page.equals(currentPage)) {
            return;
        }
        if ("Stocks".equals(currentPage) && !resolvePendingStockUpdates()) {
            return;
        }
        currentPage = page;
        buildApplication();
    }

    private void produceProduct(String product) {
        List<String> unavailable = getUnavailableBOMItems(product);
        if (!unavailable.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Not Available for Production: Insufficient Raw Materials\n"
                            + String.join("\n", unavailable),
                    "Production Unavailable", JOptionPane.WARNING_MESSAGE);
            showPage("Production");
            return;
        }
        if (finishedGoods.get(product) == Integer.MAX_VALUE) {
            JOptionPane.showMessageDialog(frame, "Finished stock has reached the maximum supported quantity.",
                    "Production Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, Integer> finishedSnapshot = new LinkedHashMap<>(finishedGoods);
        Map<String, Integer> rawSnapshot = new LinkedHashMap<>(rawMaterials);
        for (BOMItem bomItem : billOfMaterials.get(product)) {
            rawMaterials.put(bomItem.materialName,
                    rawMaterials.get(bomItem.materialName) - bomItem.quantityRequired);
        }
        finishedGoods.put(product, finishedGoods.get(product) + 1);
        try {
            saveInventory();
        } catch (IOException e) {
            finishedGoods.clear();
            finishedGoods.putAll(finishedSnapshot);
            rawMaterials.clear();
            rawMaterials.putAll(rawSnapshot);
            showSaveError(e);
            return;
        }
        try {
            appendProductionHistory(product);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Production was saved, but its history records could not be written:\n"
                            + e.getMessage(),
                    "Production History Error", JOptionPane.ERROR_MESSAGE);
        }
        updateNotificationBadge();
        showPage("Production");
        applicationRoot.repaint();
    }

    private void initializeHistoryFile() throws IOException {
        Path absoluteHistoryFile = HISTORY_FILE.toAbsolutePath();
        Path parent = absoluteHistoryFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.notExists(absoluteHistoryFile)) {
            Files.createFile(absoluteHistoryFile);
        }
    }

    private void appendProductionHistory(String product) throws IOException {
        String timestamp = "[" + LocalDateTime.now().format(HISTORY_TIMESTAMP_FORMAT) + "] ";
        StringJoiner consumedMaterials = new StringJoiner(", ");
        for (BOMItem item : billOfMaterials.get(product)) {
            consumedMaterials.add(item.materialName + " (" + item.quantityRequired + ")");
        }
        String productRecord = timestamp + "Produced: " + product + " (1)";
        String consumptionRecord = timestamp + "Consumed for " + product + ": "
                + consumedMaterials;
        Files.write(HISTORY_FILE, Arrays.asList(productRecord, consumptionRecord),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void updateNotificationBadge() {
        if (notificationButton != null) {
            notificationButton.setUnreadCount(getLowStockMaterials().size());
        }
        if (notificationPopup != null) {
            closeNotificationDropdown();
        }
    }

    private List<String> getUnavailableBOMItems(String product) {
        List<String> unavailable = new ArrayList<>();
        for (BOMItem bomItem : billOfMaterials.get(product)) {
            int available = getStockQuantity(bomItem.materialName);
            if (available < bomItem.quantityRequired) {
                unavailable.add(bomItem.materialName + " (need " + bomItem.quantityRequired
                        + ", have " + available + ")");
            }
        }
        return unavailable;
    }

    private void toggleNotificationDropdown(boolean wasOpenAtButtonPress) {
        if (wasOpenAtButtonPress || isNotificationOpen) {
            closeNotificationDropdown();
            return;
        }
        closeSettingsDropdown();
        showNotificationDropdown();
    }

    private void showNotificationDropdown() {
        JPanel notifications = new JPanel(new BorderLayout(0, 10));
        notifications.setOpaque(false);
        notifications.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        notifications.setPreferredSize(new Dimension(330, 290));
        JLabel heading = new JLabel("Raw material alerts");
        heading.setFont(new Font("SansSerif", Font.BOLD, 16));
        heading.setForeground(theme.text());
        notifications.add(heading, BorderLayout.NORTH);

        JPanel alertRows = new JPanel(new GridBagLayout());
        alertRows.setOpaque(false);
        List<String> alerts = getLowStockMaterials();
        Collections.reverse(alerts);
        GridBagConstraints rowConstraints = new GridBagConstraints();
        rowConstraints.gridx = 0;
        rowConstraints.weightx = 1.0;
        rowConstraints.weighty = 0.0;
        rowConstraints.anchor = GridBagConstraints.NORTHWEST;
        rowConstraints.fill = GridBagConstraints.HORIZONTAL;
        rowConstraints.insets = new Insets(0, 0, 7, 0);
        int rowIndex = 0;
        if (alerts.isEmpty()) {
            rowConstraints.gridy = rowIndex++;
            rowConstraints.fill = GridBagConstraints.NONE;
            alertRows.add(emptyState("No unread stock warnings."), rowConstraints);
        } else {
            for (String material : alerts) {
                RoundedPanel alert = new RoundedPanel(12);
                alert.setBackground(theme.row());
                alert.setLayout(new BorderLayout(8, 0));
                alert.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
                JLabel marker = new JLabel("\u25CF");
                marker.setForeground(theme.accent());
                JLabel detail = new JLabel("<html><b>" + material + "</b><br>"
                        + getStockQuantity(material) + " on hand (alert level "
                        + LOW_STOCK_THRESHOLD + ")</html>");
                detail.setFont(SMALL_FONT);
                detail.setForeground(theme.text());
                alert.add(marker, BorderLayout.WEST);
                alert.add(detail, BorderLayout.CENTER);
                alert.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                rowConstraints.gridy = rowIndex++;
                rowConstraints.fill = GridBagConstraints.HORIZONTAL;
                alertRows.add(alert, rowConstraints);
            }
        }
        GridBagConstraints bottomSpace = new GridBagConstraints();
        bottomSpace.gridx = 0;
        bottomSpace.gridy = rowIndex;
        bottomSpace.weightx = 1.0;
        bottomSpace.weighty = 1.0;
        bottomSpace.fill = GridBagConstraints.BOTH;
        JPanel emptySpace = new JPanel();
        emptySpace.setOpaque(false);
        alertRows.add(emptySpace, bottomSpace);
        JScrollPane list = createScrollPane(alertRows);
        list.getViewport().setViewPosition(new java.awt.Point(0, 0));
        notifications.add(list, BorderLayout.CENTER);
        JPopupMenu popup = new RoundedNotificationPopup();
        notificationPopup = popup;
        popup.setOpaque(false);
        popup.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        popup.setLayout(new BorderLayout());
        popup.add(notifications, BorderLayout.CENTER);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                isNotificationOpen = true;
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
                if (notificationPopup == popup) {
                    isNotificationOpen = false;
                    notificationPopup = null;
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event) {
                if (notificationPopup == popup) {
                    isNotificationOpen = false;
                    notificationPopup = null;
                }
            }
        });
        popup.show(notificationButton,
                notificationButton.getWidth() - notifications.getPreferredSize().width,
                notificationButton.getHeight());
        isNotificationOpen = popup.isVisible();
    }

    private void closeNotificationDropdown() {
        isNotificationOpen = false;
        if (notificationPopup != null) {
            notificationPopup.setVisible(false);
            notificationPopup = null;
        }
    }

    private void installPopupDismissListener() {
        popupDismissListener = event -> {
            if (!(event instanceof MouseEvent)
                    || ((MouseEvent) event).getID() != MouseEvent.MOUSE_PRESSED) {
                return;
            }
            Component source = event.getSource() instanceof Component
                    ? (Component) event.getSource() : null;
            if (source == null) {
                return;
            }
            notificationOpenAtButtonPress = null;
            settingsOpenAtButtonPress = null;
            if (isDescendantOf(source, notificationButton)) {
                notificationOpenAtButtonPress = isNotificationOpen
                        || (notificationPopup != null && notificationPopup.isShowing());
                return;
            }
            if (isDescendantOf(source, settingsButton)) {
                settingsOpenAtButtonPress = isSettingsOpen
                        || (settingsPopup != null && settingsPopup.isShowing());
                return;
            }
            if (isPopupControl(source)) {
                return;
            }
            closeNotificationDropdown();
            closeSettingsDropdown();
        };
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
                popupDismissListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private boolean isPopupControl(Component component) {
        if (component == notificationButton || component == settingsButton
                || isDescendantOf(component, notificationPopup)
                || isDescendantOf(component, settingsPopup)) {
            return true;
        }
        return false;
    }

    private boolean isDescendantOf(Component component, Component ancestor) {
        for (Component current = component; current != null; current = current.getParent()) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }

    private void closeSettingsDropdown() {
        isSettingsOpen = false;
        if (settingsPopup != null) {
            settingsPopup.setVisible(false);
            settingsPopup = null;
        }
    }

    private boolean consumeNotificationOpenAtButtonPress() {
        boolean wasOpen = notificationOpenAtButtonPress != null
                ? notificationOpenAtButtonPress
                : isNotificationOpen
                        || (notificationPopup != null && notificationPopup.isShowing());
        notificationOpenAtButtonPress = null;
        return wasOpen;
    }

    private boolean consumeSettingsOpenAtButtonPress() {
        boolean wasOpen = settingsOpenAtButtonPress != null
                ? settingsOpenAtButtonPress
                : isSettingsOpen
                        || (settingsPopup != null && settingsPopup.isShowing());
        settingsOpenAtButtonPress = null;
        return wasOpen;
    }

    private JPanel centeredEmptyState(String message) {
        JPanel panel = new JPanel(new java.awt.GridBagLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(BODY_FONT);
        label.setForeground(theme.secondaryText());
        panel.add(label);
        return panel;
    }

    private void toggleSidebar() {
        if (sidebarPanel == null) {
            return;
        }
        if (sidebarAnimation != null && sidebarAnimation.isRunning()) {
            sidebarAnimation.stop();
        }
        final int targetWidth;
        if (sidebarExpanded) {
            targetWidth = 0;
            sidebarExpanded = false;
        } else {
            targetWidth = 220;
            sidebarExpanded = true;
            if (sidebarPanel.getParent() != applicationRoot) {
                sidebarWidth = 0;
                sidebarPanel.setPreferredSize(new Dimension(0, 100));
                applicationRoot.add(sidebarPanel, BorderLayout.WEST);
            }
        }
        sidebarAnimation = new Timer(10, null);
        sidebarAnimation.addActionListener(event -> {
            int direction = targetWidth > sidebarWidth ? 1 : -1;
            sidebarWidth += direction * 22;
            if ((direction > 0 && sidebarWidth >= targetWidth)
                    || (direction < 0 && sidebarWidth <= targetWidth)) {
                sidebarWidth = targetWidth;
                sidebarAnimation.stop();
                if (targetWidth == 0) {
                    applicationRoot.remove(sidebarPanel);
                }
            }
            sidebarPanel.setPreferredSize(new Dimension(sidebarWidth, 100));
            applicationRoot.revalidate();
            applicationRoot.repaint();
        });
        sidebarAnimation.start();
    }

    private void toggleSettings(boolean wasOpenAtButtonPress) {
        if (wasOpenAtButtonPress
                || (settingsPopup != null && settingsPopup.isVisible())) {
            closeSettingsDropdown();
            return;
        }
        closeSettingsDropdown();
        closeNotificationDropdown();
        showSettings();
    }

    private void showSettings() {
        RoundedPanel card = new RoundedPanel(18);
        card.setBackground(theme.dashboardPanel());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setLayout(new BorderLayout(0, 12));

        JLabel title = new JLabel("Settings");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(theme.text());
        card.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.add(settingsMetadata("System Operator: admin (Access: Full Override)"));
        content.add(javax.swing.Box.createVerticalStrut(8));
        content.add(settingsMetadata("Application Status: v1.4.2 Stable Release"));
        content.add(javax.swing.Box.createVerticalStrut(8));
        content.add(settingsMetadata("Active Database Node: 'inventory_data.txt' (Local Core)"));
        content.add(javax.swing.Box.createVerticalStrut(16));

        settingsStatus = new JLabel("");
        settingsStatus.setFont(SMALL_FONT);
        settingsStatus.setForeground(theme.success());
        settingsStatus.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        RoundedButton backupButton = settingsControlButton(
                "Create Data Backup", theme.primaryButton(), Color.WHITE);
        backupButton.addActionListener(event -> {
            try {
                Files.copy(DATA_FILE, Paths.get("inventory_backup.txt"),
                        StandardCopyOption.REPLACE_EXISTING);
                settingsStatus.setForeground(theme.success());
                settingsStatus.setText("Backup successfully archived!");
            } catch (IOException e) {
                settingsStatus.setForeground(theme.danger());
                settingsStatus.setText("Backup failed: " + e.getMessage());
            }
        });
        content.add(backupButton);
        content.add(javax.swing.Box.createVerticalStrut(12));

        RoundedButton resetButton = settingsControlButton(
                "Reset Memory Cache", theme.input(), theme.text());
        resetButton.addActionListener(event -> {
            pendingStockUpdates.clear();
            updateNotificationBadge();
            if ("Stocks".equals(currentPage)) {
                showPage(currentPage);
            }
            settingsStatus.setForeground(theme.secondaryText());
            settingsStatus.setText("Unsaved stock updates cleared.");
        });
        content.add(resetButton);
        content.add(javax.swing.Box.createVerticalStrut(12));

        RoundedButton logoutButton = settingsControlButton(
                "Logout", theme.input(), theme.text());
        logoutButton.addActionListener(event -> logout());
        content.add(logoutButton);
        content.add(javax.swing.Box.createVerticalStrut(8));
        content.add(settingsStatus);
        content.setPreferredSize(new Dimension(350, content.getPreferredSize().height));
        card.add(content, BorderLayout.CENTER);

        JPopupMenu popup = new RoundedNotificationPopup();
        settingsPopup = popup;
        popup.setOpaque(false);
        popup.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        popup.setLayout(new BorderLayout());
        popup.add(card, BorderLayout.CENTER);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                isSettingsOpen = true;
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
                if (settingsPopup == popup) {
                    isSettingsOpen = false;
                    settingsPopup = null;
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event) {
                if (settingsPopup == popup) {
                    isSettingsOpen = false;
                    settingsPopup = null;
                }
            }
        });
        Dimension popupSize = popup.getPreferredSize();
        popup.setPopupSize(popupSize);
        popup.show(settingsButton,
                settingsButton.getWidth() - popupSize.width, settingsButton.getHeight());
        isSettingsOpen = popup.isVisible();
    }

    private RoundedButton settingsControlButton(String text, Color fill, Color foreground) {
        RoundedButton button = new RoundedButton(text, fill, foreground, 9);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(240, 38));
        button.setMaximumSize(new Dimension(240, 38));
        return button;
    }

    private void logout() {
        if (!pendingStockUpdates.isEmpty()) {
            if (settingsPopup == null || !settingsPopup.isShowing()) {
                closeNotificationDropdown();
                showSettings();
            }
            settingsStatus.setForeground(theme.danger());
            settingsStatus.setText("Save or clear unsaved stock updates before logging out.");
            return;
        }
        loggedIn = false;
        searchText = "";
        closeSettingsDropdown();
        closeNotificationDropdown();
        showLogin();
    }

    private JLabel settingsMetadata(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(theme.text());
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return label;
    }

    private JDialog createThemedDialog(String title) {
        JDialog dialog = new JDialog(frame, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.getRootPane().setOpaque(false);
        return dialog;
    }

    private RoundedPanel createDialogCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setBackground(theme.dashboardPanel());
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        return card;
    }

    private int showThemedChoiceDialog(String title, String message, String primary, String secondary) {
        JDialog dialog = createThemedDialog(title);
        RoundedPanel card = createDialogCard();
        card.setLayout(new BorderLayout(0, 22));
        JLabel messageLabel = new JLabel("<html><div style='text-align:center;width:340px'>"
                + message.replace("\n", "<br>") + "</div></html>", SwingConstants.CENTER);
        messageLabel.setFont(BODY_FONT);
        messageLabel.setForeground(theme.text());
        card.add(messageLabel, BorderLayout.CENTER);

        int[] result = {-1};
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        RoundedButton primaryButton = new RoundedButton(
                primary, theme.primaryButton(), Color.WHITE, 9);
        primaryButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        primaryButton.setPreferredSize(new Dimension(112, 42));
        primaryButton.addActionListener(event -> {
            result[0] = 0;
            dialog.dispose();
        });
        RoundedButton secondaryButton = new RoundedButton(
                secondary, theme.input(), theme.text(), 9);
        secondaryButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        secondaryButton.setPreferredSize(new Dimension(112, 42));
        secondaryButton.addActionListener(event -> {
            result[0] = 1;
            dialog.dispose();
        });
        RoundedButton cancelButton = new RoundedButton(
                "Cancel", theme.input(), theme.text(), 9);
        cancelButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(112, 42));
        cancelButton.addActionListener(event -> {
            result[0] = 2;
            dialog.dispose();
        });
        buttons.add(primaryButton);
        buttons.add(secondaryButton);
        buttons.add(cancelButton);
        card.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(card);
        configureFocusablePanels(card);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(440, 170));
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        return result[0];
    }

    private void closeApplication() {
        if (exitConfirmationDialog != null && exitConfirmationDialog.isVisible()) {
            return;
        }
        JDialog dialog = createThemedDialog("Exit " + BUSINESS_NAME);
        exitConfirmationDialog = dialog;
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        RoundedPanel card = createDialogCard();
        card.setLayout(new BorderLayout(0, 24));
        JLabel message = new JLabel("Save inventory and exit?", SwingConstants.CENTER);
        message.setFont(BODY_FONT);
        message.setForeground(theme.text());
        message.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        card.add(message, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);
        RoundedButton yesButton = new RoundedButton(
                "Yes", new Color(0x23211F), Color.WHITE);
        yesButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        yesButton.setPreferredSize(new Dimension(112, 42));
        yesButton.addActionListener(event -> {
            boolean hadPendingUpdates = !pendingStockUpdates.isEmpty();
            if (!commitPendingStockUpdates()) {
                return;
            }
            if (!hadPendingUpdates) {
                try {
                    saveInventory();
                } catch (IOException e) {
                    showSaveError(e);
                    return;
                }
            }
            exitApplication(dialog);
        });

        RoundedButton noButton = new RoundedButton(
                "No", theme.input(), theme.text());
        noButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        noButton.setPreferredSize(new Dimension(112, 42));
        noButton.addActionListener(event -> {
            pendingStockUpdates.clear();
            exitApplication(dialog);
        });

        RoundedButton cancelButton = new RoundedButton(
                "Cancel", theme.input(), theme.text());
        cancelButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(112, 42));
        cancelButton.addActionListener(event -> dialog.dispose());

        actions.add(yesButton);
        actions.add(noButton);
        actions.add(cancelButton);
        card.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(card);
        configureFocusablePanels(card);
        dialog.setSize(500, 190);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void exitApplication(JDialog dialog) {
        closeNotificationDropdown();
        closeSettingsDropdown();
        if (popupDismissListener != null) {
            java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(popupDismissListener);
            popupDismissListener = null;
        }
        if (globalFocusClearListener != null) {
            java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(globalFocusClearListener);
            globalFocusClearListener = null;
        }
        exitConfirmationDialog = null;
        dialog.dispose();
        frame.dispose();
        System.exit(0);
    }

    private void showSaveError(IOException e) {
        JOptionPane.showMessageDialog(frame, "Unable to save inventory data. The change was rolled back.\n"
                + e.getMessage(), "Inventory Save Error", JOptionPane.ERROR_MESSAGE);
    }

    private long getFinishedUnitCount() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(72);
        long total = 0;
        List<String> historyLines = Files.readAllLines(HISTORY_FILE, StandardCharsets.UTF_8);
        for (String line : historyLines) {
            int timestampEnd = line.indexOf("] ");
            if (!line.startsWith("[") || timestampEnd <= 1) {
                continue;
            }

            LocalDateTime producedAt;
            try {
                producedAt = LocalDateTime.parse(
                        line.substring(1, timestampEnd), HISTORY_TIMESTAMP_FORMAT);
            } catch (java.time.format.DateTimeParseException e) {
                continue;
            }
            if (producedAt.isBefore(cutoff) || producedAt.isAfter(now)) {
                continue;
            }

            String details = line.substring(timestampEnd + 2);
            String producedPrefix = "Produced: ";
            if (!details.startsWith(producedPrefix)) {
                continue;
            }
            String quantityAndProduct = parseQuantityAndName(
                    details.substring(producedPrefix.length()));
            if (quantityAndProduct == null) {
                continue;
            }

            int separator = quantityAndProduct.indexOf(' ');
            if (separator <= 0 || separator == quantityAndProduct.length() - 1) {
                continue;
            }
            int quantity;
            try {
                quantity = Integer.parseInt(quantityAndProduct.substring(0, separator));
            } catch (NumberFormatException e) {
                continue;
            }
            String product = quantityAndProduct.substring(separator + 1);
            if (quantity >= 0 && finishedGoods.containsKey(product)) {
                total += quantity;
            }
        }
        return total;
    }

    private List<String> getLowStockMaterials() {
        List<String> alerts = new ArrayList<>();
        for (Map.Entry<String, Integer> material : rawMaterials.entrySet()) {
            if (getStockQuantity(material.getKey()) <= LOW_STOCK_THRESHOLD) {
                alerts.add(material.getKey());
            }
        }
        return alerts;
    }

    private List<String> getDashboardWarningMaterials() {
        List<String> warnings = new ArrayList<>();
        for (Map.Entry<String, Integer> material : rawMaterials.entrySet()) {
            if (getStockQuantity(material.getKey()) <= DASHBOARD_WARNING_THRESHOLD) {
                warnings.add(material.getKey());
            }
        }
        return warnings;
    }

    private void initializeInventory() {
        List<String> products = new ArrayList<>(Arrays.asList(
                "Hitch Accessories", "Cargo Basket", "Bike Rack", "Trailer",
                "Motor Rack Carrier", "Clamp", "Hitch Ball Assembly"));
        products.sort(String.CASE_INSENSITIVE_ORDER);
        for (String product : products) {
            finishedGoods.put(product, 0);
        }
        List<String> materials = new ArrayList<>(Arrays.asList(
                "Mild steel plates", "Square steel tubing", "Steel flat bars", "Steel rods",
                "Bolts", "Nuts", "Washers", "Welding rods", "Metal primer", "Paint",
                "Expanded metal mesh", "Round steel tubing", "Rubber straps", "Steel plates",
                "Trailer axle", "Wheels and tires", "Trailer hitch coupler", "Safety chains",
                "Electrical wires", "Trailer signal lights", "Wheel chock", "Tie-down rings",
                "Steel bars", "Steel hitch ball", "Steel mounting plate"));
        materials.sort(String.CASE_INSENSITIVE_ORDER);
        for (String material : materials) {
            rawMaterials.put(material, 50);
        }
    }

    private void initializeBOM() {
        addBOM("Hitch Accessories",
                bomItem("Mild steel plates", 1), bomItem("Square steel tubing", 1),
                bomItem("Steel flat bars", 1), bomItem("Steel rods", 1), bomItem("Bolts", 2),
                bomItem("Nuts", 2), bomItem("Washers", 2), bomItem("Welding rods", 1),
                bomItem("Metal primer", 1), bomItem("Paint", 1));
        addBOM("Cargo Basket",
                bomItem("Square steel tubing", 4), bomItem("Steel flat bars", 2),
                bomItem("Expanded metal mesh", 1), bomItem("Bolts", 4), bomItem("Nuts", 4),
                bomItem("Washers", 4), bomItem("Welding rods", 1), bomItem("Metal primer", 1),
                bomItem("Paint", 1));
        addBOM("Bike Rack",
                bomItem("Square steel tubing", 3), bomItem("Round steel tubing", 1),
                bomItem("Steel flat bars", 1), bomItem("Steel rods", 1), bomItem("Rubber straps", 2),
                bomItem("Bolts", 4), bomItem("Nuts", 4), bomItem("Washers", 4),
                bomItem("Welding rods", 1), bomItem("Metal primer", 1), bomItem("Paint", 1));
        addBOM("Trailer",
                bomItem("Square steel tubing", 12), bomItem("Steel plates", 2),
                bomItem("Steel flat bars", 3), bomItem("Trailer axle", 1),
                bomItem("Wheels and tires", 2), bomItem("Trailer hitch coupler", 1),
                bomItem("Safety chains", 2), bomItem("Bolts", 8), bomItem("Nuts", 8),
                bomItem("Washers", 8), bomItem("Welding rods", 2), bomItem("Electrical wires", 1),
                bomItem("Trailer signal lights", 2), bomItem("Metal primer", 1), bomItem("Paint", 1));
        addBOM("Motor Rack Carrier",
                bomItem("Square steel tubing", 5), bomItem("Steel plates", 2),
                bomItem("Steel flat bars", 2), bomItem("Steel rods", 1),
                bomItem("Expanded metal mesh", 1), bomItem("Wheel chock", 1),
                bomItem("Tie-down rings", 4), bomItem("Bolts", 6), bomItem("Nuts", 6),
                bomItem("Washers", 6), bomItem("Welding rods", 1), bomItem("Metal primer", 1),
                bomItem("Paint", 1));
        addBOM("Clamp",
                bomItem("Steel plates", 2), bomItem("Steel bars", 1), bomItem("Bolts", 1),
                bomItem("Nuts", 1), bomItem("Washers", 2), bomItem("Welding rods", 1),
                bomItem("Metal primer", 1), bomItem("Paint", 1));
        addBOM("Hitch Ball Assembly",
                bomItem("Steel hitch ball", 1), bomItem("Steel mounting plate", 1),
                bomItem("Bolts", 2), bomItem("Nuts", 2), bomItem("Washers", 2));
    }

    private BOMItem bomItem(String materialName, int quantityRequired) {
        return new BOMItem(materialName, quantityRequired);
    }

    private void addBOM(String product, BOMItem... items) {
        billOfMaterials.put(product,
                Collections.unmodifiableList(new ArrayList<>(Arrays.asList(items))));
    }

    private void loadInventory() throws IOException {
        if (!Files.exists(DATA_FILE)) {
            saveInventory();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(DATA_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 3 || (!"F".equals(fields[0]) && !"R".equals(fields[0]))) {
                    System.err.println("Ignoring malformed inventory data at line " + lineNumber + ".");
                    continue;
                }
                Map<String, Integer> inventory = "F".equals(fields[0]) ? finishedGoods : rawMaterials;
                if (!inventory.containsKey(fields[1])) {
                    System.err.println("Ignoring unknown inventory item at line " + lineNumber + ".");
                    continue;
                }
                try {
                    int quantity = Integer.parseInt(fields[2]);
                    if (quantity < 0) {
                        System.err.println("Ignoring negative quantity at line " + lineNumber + ".");
                        continue;
                    }
                    inventory.put(fields[1], quantity);
                } catch (NumberFormatException e) {
                    System.err.println("Ignoring invalid quantity at line " + lineNumber + ".");
                }
            }
        }
    }

    private void saveInventory() throws IOException {
        Path absoluteDataFile = DATA_FILE.toAbsolutePath();
        Path parent = absoluteDataFile.getParent();
        Path temporaryFile = Files.createTempFile(parent, "inventory_data", ".tmp");
        boolean moved = false;
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                writer.write("# Tow Hitch by 5-21 inventory data. Do not edit while the program is running.");
                writer.newLine();
                for (Map.Entry<String, Integer> item : finishedGoods.entrySet()) {
                    writer.write("F\t" + item.getKey() + "\t" + item.getValue());
                    writer.newLine();
                }
                for (Map.Entry<String, Integer> item : rawMaterials.entrySet()) {
                    writer.write("R\t" + item.getKey() + "\t" + item.getValue());
                    writer.newLine();
                }
            }
            try {
                Files.move(temporaryFile, absoluteDataFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, absoluteDataFile, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    private ImageIcon loadImageAsset(String assetName) {
        List<Path> candidates = new ArrayList<>();
        Path workspacePath = Paths.get("").toAbsolutePath();
        addAssetCandidates(candidates, workspacePath, assetName);
        try {
            Path codePath = Paths.get(TowHitchSystemPure.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath();
            if (Files.isRegularFile(codePath)) {
                codePath = codePath.getParent();
            }
            addAssetCandidates(candidates, codePath, assetName);
            if (codePath.getParent() != null) {
                addAssetCandidates(candidates, codePath.getParent(), assetName);
            }
        } catch (java.net.URISyntaxException | SecurityException e) {
            System.err.println("Unable to resolve application directory for UI asset "
                    + assetName + ": " + e.getMessage());
        }

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                ImageIcon image = new ImageIcon(candidate.toString());
                if (image.getIconWidth() > 0 && image.getIconHeight() > 0) {
                    return image;
                }
                System.err.println("Unable to decode UI image asset: " + candidate);
            }
        }
        System.err.println("UI image asset not found: " + assetName
                + " (the login screen will use its fallback artwork).");
        return null;
    }

    private ImageIcon loadScaledImageAsset(String assetName, int width, int height) {
        ImageIcon original = loadImageAsset(assetName);
        if (original == null) {
            return null;
        }
        Image scaled = original.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private ImageIcon scaleToFitIcon(ImageIcon source, int maxWidth, int maxHeight) {
        double scale = Math.min((double) maxWidth / source.getIconWidth(),
                (double) maxHeight / source.getIconHeight());
        int width = Math.max(1, (int) Math.round(source.getIconWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getIconHeight() * scale));
        return new ImageIcon(source.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private ImageIcon loadTintedIcon(ImageIcon source, int size, Color tint) {
        if (source == null) {
            return null;
        }
        ImageIcon scaled = scaleToFitIcon(source, size, size);
        BufferedImage tinted = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tinted.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = (size - scaled.getIconWidth()) / 2;
        int y = (size - scaled.getIconHeight()) / 2;
        g.drawImage(scaled.getImage(), x, y, null);
        g.setComposite(java.awt.AlphaComposite.SrcIn);
        g.setColor(tint);
        g.fillRect(0, 0, size, size);
        g.dispose();
        return new ImageIcon(tinted);
    }

    private void addAssetCandidates(List<Path> candidates, Path directory, String assetName) {
        Path candidate = directory.resolve(assetName).normalize();
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private ImageIcon loadScaledLoginLogo(String assetName, int size) {
        ImageIcon original = loadImageAsset(assetName);
        if (original == null) {
            return null;
        }
        int imageWidth = original.getIconWidth();
        int imageHeight = original.getIconHeight();
        BufferedImage source = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        graphics.drawImage(original.getImage(), 0, 0, null);
        graphics.dispose();

        int minX = imageWidth;
        int minY = imageHeight;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                if ((source.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return new ImageIcon(original.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
        }

        int cropSize = Math.max(maxX - minX + 1, maxY - minY + 1);
        int cropX = Math.max(0, Math.min(imageWidth - cropSize,
                (minX + maxX + 1 - cropSize) / 2));
        int cropY = Math.max(0, Math.min(imageHeight - cropSize,
                (minY + maxY + 1 - cropSize) / 2));
        BufferedImage squareCrop = new BufferedImage(cropSize, cropSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cropGraphics = squareCrop.createGraphics();
        cropGraphics.drawImage(source, -cropX, -cropY, null);
        cropGraphics.dispose();
        Image scaled = squareCrop.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private final class ThemeController {
        private boolean darkMode;

        private void toggle() {
            darkMode = !darkMode;
        }

        private boolean isDark() {
            return darkMode;
        }

        private Color canvasStart() {
            return darkMode ? new Color(0x34312F) : new Color(0xF0E5D8);
        }

        private Color canvasEnd() {
            return darkMode ? new Color(0x34312F) : new Color(0xF8F1E9);
        }

        private Color workspaceCanvas() {
            return darkMode ? new Color(0x1F1C1A) : new Color(0xF2E3CE);
        }

        private Color sidebar() {
            return darkMode ? new Color(0x2D2A26) : new Color(0xF4EFE6);
        }

        private Color dashboardPanel() {
            return darkMode ? new Color(0x34312F) : Color.WHITE;
        }

        private Color panel() {
            return darkMode ? new Color(0x4A4643) : new Color(0xFDFBF7);
        }

        private Color row() {
            return darkMode ? new Color(0x292725) : new Color(0xF8F4EE);
        }

        private Color input() {
            return darkMode ? new Color(0xB6B1AA) : new Color(0xF6F0E5);
        }

        private Color text() {
            return darkMode ? new Color(0xF7F4EF) : new Color(0x23211F);
        }

        private Color secondaryText() {
            return darkMode ? new Color(0xB6B1AA) : new Color(0x7F7A75);
        }

        private Color primaryButton() {
            return darkMode ? new Color(0x242221) : new Color(0x23211F);
        }

        private Color accent() {
            return new Color(0x3A63D6);
        }

        private Color danger() {
            return darkMode ? new Color(0xFF9A8F) : new Color(0xB23A32);
        }

        private Color success() {
            return darkMode ? new Color(0x9BD1A5) : new Color(0x37764A);
        }
    }

    private final class LoginBackgroundPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int CARD_WIDTH = 516;
        private GlassLoginCard glassCard;
        private ThemeSwitchButton themeSwitch;

        private LoginBackgroundPanel() {
            setOpaque(true);
            setLayout(null);
        }

        private void setLoginComponents(GlassLoginCard card, ThemeSwitchButton switchButton) {
            glassCard = card;
            themeSwitch = switchButton;
            add(glassCard);
            add(themeSwitch);
        }

        @Override
        public void doLayout() {
            if (glassCard != null) {
                int cardWidth = Math.min(CARD_WIDTH, Math.max(0, getWidth() - 32));
                int cardHeight = Math.min(glassCard.getPreferredSize().height,
                        Math.max(0, getHeight() - 32));
                glassCard.setBounds((getWidth() - cardWidth) / 2,
                        (getHeight() - cardHeight) / 2, cardWidth, cardHeight);
            }
            if (themeSwitch != null) {
                themeSwitch.setBounds(Math.max(12, getWidth() - 106), 16, 84, 40);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            ImageIcon wallpaper = theme.isDark() ? darkWallpaper : lightWallpaper;
            if (wallpaper != null) {
                g.drawImage(wallpaper.getImage(), 0, 0, getWidth(), getHeight(), this);
            } else {
                Color start = theme.isDark() ? new Color(0x1F1C1A) : new Color(0xF2E3CE);
                Color end = theme.isDark() ? new Color(0x4A2E2B) : new Color(0xD9C5B2);
                g.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.dispose();
        }
    }

    private final class GlassLoginCard extends JPanel {
        private static final long serialVersionUID = 1L;

        private GlassLoginCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(theme.isDark()
                    ? new Color(45, 42, 38, 195) : new Color(255, 255, 255, 175));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
            g.setColor(theme.isDark()
                    ? new Color(255, 255, 255, 32) : new Color(255, 255, 255, 115));
            g.setStroke(new java.awt.BasicStroke(1.0f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private final class LoginTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        private LoginTextField(String placeholder) {
            this.placeholder = placeholder;
            setFont(new Font("SansSerif", Font.PLAIN, 14));
            setOpaque(false);
            setAlignmentX(JComponent.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 16));
            setPreferredSize(new Dimension(394, 51));
            setMaximumSize(new Dimension(394, 67));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fieldColor = theme.isDark() ? new Color(0xB6B1AA) : new Color(0xF4EFE6);
            g.setColor(fieldColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.dispose();
            setForeground(new Color(0x242221));
            setCaretColor(new Color(0x242221));
            super.paintComponent(graphics);
            if (getText().isEmpty() && !hasFocus()) {
                paintLoginPlaceholder(graphics, placeholder, this);
            }
        }
    }

    private final class LoginPasswordField extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        private LoginPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setFont(new Font("SansSerif", Font.PLAIN, 14));
            setOpaque(false);
            setAlignmentX(JComponent.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 16));
            setPreferredSize(new Dimension(394, 51));
            setMaximumSize(new Dimension(394, 67));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fieldColor = theme.isDark() ? new Color(0xB6B1AA) : new Color(0xF4EFE6);
            g.setColor(fieldColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.dispose();
            setForeground(new Color(0x242221));
            setCaretColor(new Color(0x242221));
            super.paintComponent(graphics);
            if (getPassword().length == 0 && !hasFocus()) {
                paintLoginPlaceholder(graphics, placeholder, this);
            }
        }
    }

    private void paintLoginPlaceholder(Graphics graphics, String placeholder, JComponent field) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(theme.isDark() ? new Color(0x5D5954) : new Color(0x77716C));
        java.awt.FontMetrics metrics = g.getFontMetrics();
        int baseline = (field.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(placeholder, field.getInsets().left, baseline);
        g.dispose();
    }

    private final class LoginFooterLabel extends JLabel {
        private static final long serialVersionUID = 1L;

        private LoginFooterLabel(String text, int alignment) {
            super(text, alignment);
        }
    }

    private final class LoginButton extends JButton {
        private static final long serialVersionUID = 1L;

        private LoginButton(String text) {
            super(text);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.isDark() ? new Color(0x171615) : new Color(0x23211F));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            if (getModel().isRollover()) {
                g.setColor(new Color(255, 255, 255, 18));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private final class GradientPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private GradientPanel() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(theme.workspaceCanvas());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    private final class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int radius;

        private RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private final class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        private final Color foreground;
        private final int cornerRadius;

        private RoundedButton(String text, Color fill, Color foreground) {
            this(text, fill, foreground, 15);
        }

        private RoundedButton(String text, Color fill, Color foreground, int cornerRadius) {
            super(text);
            this.fill = fill;
            this.foreground = foreground;
            this.cornerRadius = cornerRadius;
            setForeground(foreground);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color actualFill = isEnabled() ? fill : theme.input();
            g.setColor(actualFill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(),
                    cornerRadius, cornerRadius);
            if (getModel().isRollover() && isEnabled()) {
                g.setColor(new Color(255, 255, 255, 25));
                g.fillRoundRect(0, 0, getWidth(), getHeight(),
                        cornerRadius, cornerRadius);
            }
            g.dispose();
            setForeground(isEnabled() ? foreground : theme.secondaryText());
            super.paintComponent(graphics);
        }
    }

    private final class RoundedTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        private RoundedTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setFont(BODY_FONT);
            setForeground(theme.text());
            setCaretColor(theme.text());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 12));
            setPreferredSize(new Dimension(220, 42));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            setForeground(theme.isDark() ? new Color(0x242221) : theme.text());
            super.paintComponent(graphics);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D placeholderGraphics = (Graphics2D) graphics.create();
                placeholderGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                placeholderGraphics.setFont(getFont());
                placeholderGraphics.setColor(theme.isDark()
                        ? new Color(0x5D5954) : theme.secondaryText());
                placeholderGraphics.drawString(placeholder, getInsets().left,
                        (getHeight() + getFont().getSize()) / 2 - 2);
                placeholderGraphics.dispose();
            }
        }
    }

    private final class RoundedSearchPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int CORNER_RADIUS = 12;

        private RoundedSearchPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setPreferredSize(new Dimension(360, 42));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.input());
            g.fillRoundRect(0, 0, getWidth(), getHeight(),
                    CORNER_RADIUS, CORNER_RADIUS);
            g.dispose();
        }
    }

    private final class SearchGlass extends JComponent {
        private static final long serialVersionUID = 1L;

        private SearchGlass() {
            setOpaque(false);
            setPreferredSize(new Dimension(24, 24));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
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

    private final class SearchDivider extends JComponent {
        private static final long serialVersionUID = 1L;

        private SearchDivider() {
            setOpaque(false);
            setPreferredSize(new Dimension(1, 22));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(theme.isDark()
                    ? new Color(244, 239, 230, 45)
                    : new Color(127, 122, 117, 45));
            int x = getWidth() / 2;
            g.drawLine(x, 2, x, getHeight() - 2);
            g.dispose();
        }
    }

    private final class RoundedPasswordField extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        private RoundedPasswordField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setFont(BODY_FONT);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 14));
            setPreferredSize(new Dimension(220, 42));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.input());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g.dispose();
            setForeground(theme.isDark() ? new Color(0x242221) : theme.text());
            setCaretColor(theme.isDark() ? new Color(0x242221) : theme.text());
            super.paintComponent(graphics);
            if (getPassword().length == 0 && !hasFocus()) {
                Graphics2D placeholderGraphics = (Graphics2D) graphics.create();
                placeholderGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                placeholderGraphics.setFont(getFont());
                placeholderGraphics.setColor(theme.isDark()
                        ? new Color(0x5D5954) : theme.secondaryText());
                placeholderGraphics.drawString(placeholder, getInsets().left,
                        (getHeight() + getFont().getSize()) / 2 - 2);
                placeholderGraphics.dispose();
            }
        }
    }

    private static final class BOMItem {
        private final String materialName;
        private final int quantityRequired;

        private BOMItem(String materialName, int quantityRequired) {
            this.materialName = materialName;
            this.quantityRequired = quantityRequired;
        }
    }

    private static final class HistoryEntry {
        private final String itemText;
        private final String date;
        private final String time;

        private HistoryEntry(String itemText, String date, String time) {
            this.itemText = itemText;
            this.date = date;
            this.time = time;
        }
    }

}
