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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.undo.UndoManager;

public class TowHitchSystemPure {
    private static final String BUSINESS_NAME = "Tow Hitch by 5-21";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "1234";
    private static final Path DATA_FILE = Paths.get("inventory_data.txt");
    private static final Path HISTORY_FILE = Paths.get("history_logs.txt");
    private static final Path PRODUCTION_LOG_FILE = Paths.get("production_logs.txt");
    private static final Path MATERIAL_CONSUMPTION_FILE = Paths.get("material_consumption.txt");
    private static final Path PRODUCT_ALIAS_FILE = Paths.get("product_aliases.txt");
    private static final DateTimeFormatter HISTORY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter PRODUCTION_TIMESTAMP_FORMAT =
            DateTimeFormatter.ISO_INSTANT;
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int DASHBOARD_WARNING_THRESHOLD = 15;
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);

    private final Map<String, Integer> finishedGoods = new LinkedHashMap<>();
    private final Map<String, Integer> rawMaterials = new LinkedHashMap<>();
    private final Map<String, Integer> pendingStockUpdates = new LinkedHashMap<>();
    private final List<MaterialDraft> pendingMaterialDrafts = new ArrayList<>();
    private final Map<String, Integer> stagedMaterialAdditions = new LinkedHashMap<>();
    private final Map<String, JTextField> materialNameEdits = new LinkedHashMap<>();
    private final Set<String> selectedMaterialsForDeletion = new LinkedHashSet<>();
    private final UndoManager stockTextUndoManager = new UndoManager();
    private final List<Runnable> stockActionUndoHistory = new ArrayList<>();
    private final Map<String, Integer> stagedProductAdditions = new LinkedHashMap<>();
    private final Map<String, JTextField> productNameEdits = new LinkedHashMap<>();
    private final Set<String> selectedProductsForDeletion = new LinkedHashSet<>();
    private final UndoManager productTextUndoManager = new UndoManager();
    private final Map<String, Integer> productionOpeningBalances = new LinkedHashMap<>();
    private final Map<String, Long> lifetimeProductionUnits = new LinkedHashMap<>();
    private final Map<String, Integer> activeProductionUnits = new LinkedHashMap<>();
    private final List<ProductionBatch> activeProductionBatches = new ArrayList<>();
    private final ScheduledExecutorService productionBackend =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "production-history-worker");
                thread.setDaemon(true);
                return thread;
            });
    private ScheduledFuture<?> productionExpiryTask;
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
    private JButton stockUndoButton;
    private JLabel productSaveStatus;
    private JButton productUndoButton;
    private StockFooterMode stockFooterMode = StockFooterMode.NORMAL;
    private ProductCatalogMode productCatalogMode = ProductCatalogMode.NORMAL;
    private ProductDraft pendingProductDraft;
    private boolean stockFooterSaveInProgress;
    private boolean productionHistoryLoaded;
    private boolean productionTransactionInProgress;
    private boolean productAliasUpdateInProgress;
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
    private final Map<String, Boolean> productExpansionMap = new LinkedHashMap<>();

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
        Map<String, Integer> inventoryAtStartup = new LinkedHashMap<>(finishedGoods);
        productionBackend.execute(() -> loadProductionLedger(inventoryAtStartup));

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
        String finishedUnitCount = productionHistoryLoaded
                ? String.valueOf(getFinishedUnitCount()) : "Loading";
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
        Color saveFill = stockFooterMode == StockFooterMode.DELETE
                ? theme.danger() : new Color(0x23211F);
        String saveLabel = stockFooterMode == StockFooterMode.ADD ? "Add"
                : stockFooterMode == StockFooterMode.DELETE ? "Delete" : "Save";
        RoundedButton saveButton = new RoundedButton(saveLabel, saveFill, Color.WHITE);
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        saveButton.setPreferredSize(new Dimension(120, 44));
        saveButton.addActionListener(event -> {
            if (stockFooterMode == StockFooterMode.DELETE) {
                int choice = showThemedChoiceDialog("Confirm Deletion",
                        "Are you sure you want to delete the selected items?",
                        "Yes", "No");
                if (choice != 0) {
                    return;
                }
            }
            if (stockFooterMode == StockFooterMode.ADD) {
                if (stageActiveMaterialAddition()) {
                    showPage("Stocks");
                    stockSaveStatus.setForeground(theme.secondaryText());
                    stockSaveStatus.setText(
                            "Material staged. Select Save to commit changes.");
                }
                return;
            }
            if (stockFooterMode == StockFooterMode.NORMAL
                    && pendingStockUpdates.isEmpty()
                    && stagedMaterialAdditions.isEmpty()
                    && pendingMaterialDrafts.isEmpty()
                    && materialNameEdits.isEmpty()
                    && selectedMaterialsForDeletion.isEmpty()) {
                stockSaveStatus.setForeground(theme.success());
                stockSaveStatus.setText("Changes saved successfully!");
                return;
            }
            stockFooterSaveInProgress = true;
            boolean saved;
            try {
                saved = commitPendingStockUpdates();
            } finally {
                stockFooterSaveInProgress = false;
            }
            if (saved) {
                stockSaveStatus.setForeground(theme.success());
                stockSaveStatus.setText("Changes saved successfully!");
            } else {
                stockSaveStatus.setForeground(theme.danger());
                stockSaveStatus.setText("Unable to save changes. See the error message.");
            }
        });
        JPanel saveButtonHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        saveButtonHolder.setOpaque(false);
        RoundedButton editButton = new RoundedButton(
                stockFooterMode == StockFooterMode.NORMAL ? "Edit" : "Cancel",
                theme.input(), theme.text(), 10);
        editButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        editButton.setPreferredSize(new Dimension(100, 44));
        editButton.addActionListener(event -> {
            if (stockFooterMode == StockFooterMode.NORMAL) {
                enterStockToolbarMode();
            } else {
                cancelStockFooterMode();
            }
        });
        if (stockFooterMode != StockFooterMode.NORMAL) {
            RoundedButton undoButton = new RoundedButton(
                    "Undo", theme.input(), theme.text(), 10);
            undoButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            undoButton.setPreferredSize(new Dimension(92, 44));
            undoButton.setEnabled(stockTextUndoManager.canUndo()
                    || !stockActionUndoHistory.isEmpty());
            undoButton.addActionListener(event -> undoStockAction());
            stockUndoButton = undoButton;
            saveButtonHolder.add(undoButton);
        } else {
            stockUndoButton = null;
        }
        saveButtonHolder.add(editButton);
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
        for (MaterialDraft draft : pendingMaterialDrafts) {
            rows.add(materialDraftRow(draft));
            rows.add(javax.swing.Box.createVerticalStrut(8));
            visibleRows++;
        }
        Map<String, Integer> visibleInventory = new LinkedHashMap<>(inventory);
        visibleInventory.putAll(stagedMaterialAdditions);
        List<String> itemNames = new ArrayList<>(visibleInventory.keySet());
        itemNames.sort(String.CASE_INSENSITIVE_ORDER);
        for (String itemName : itemNames) {
            rows.add(inventoryRow(itemName, visibleInventory,
                    stockFooterMode == StockFooterMode.DELETE));
            JPanel separator = new JPanel();
            separator.setOpaque(true);
            Color separatorColor = theme.secondaryText();
            separator.setBackground(new Color(separatorColor.getRed(), separatorColor.getGreen(),
                    separatorColor.getBlue(), 55));
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            separator.setPreferredSize(new Dimension(1, 1));
            rows.add(separator);
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

        JLabel quantity = new JLabel("Qty", SwingConstants.CENTER);
        quantity.setFont(new Font("SansSerif", Font.BOLD, 13));
        quantity.setForeground(theme.secondaryText());
        int quantityWidth = new JTextField("", 5).getPreferredSize().width;
        quantity.setPreferredSize(new Dimension(quantityWidth, quantity.getPreferredSize().height));
        quantityColumns.add(quantity);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        headerActions.setOpaque(false);
        if (stockFooterMode != StockFooterMode.NORMAL) {
            StockActionIconButton editAction =
                    new StockActionIconButton(StockHeaderAction.EDIT);
            editAction.addActionListener(event -> beginMaterialNameEdit());
            StockActionIconButton addAction =
                    new StockActionIconButton(StockHeaderAction.ADD);
            addAction.addActionListener(event -> beginMaterialAddition());
            StockActionIconButton deleteAction =
                    new StockActionIconButton(StockHeaderAction.DELETE);
            deleteAction.addActionListener(event -> beginMaterialDeletion());
            headerActions.add(editAction);
            headerActions.add(addAction);
            headerActions.add(deleteAction);
        }
        JPanel headerRight = new JPanel(new BorderLayout(0, 2));
        headerRight.setOpaque(false);
        if (stockFooterMode != StockFooterMode.NORMAL) {
            headerRight.add(headerActions, BorderLayout.NORTH);
        }
        headerRight.add(quantityColumns, BorderLayout.SOUTH);
        header.add(headerRight, BorderLayout.EAST);
        return header;
    }

    private JPanel inventoryRow(String itemName, Map<String, Integer> inventory,
            boolean showSelection) {
        RoundedPanel row = new RoundedPanel(14);
        row.setBackground(theme.row());
        if (itemName.equalsIgnoreCase(searchHighlightName)) {
            searchHighlightComponent = row;
        }
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createEmptyBorder(12, showSelection ? 10 : 15, 12, 12));

        JComponent nameDisplay;
        if (stockFooterMode == StockFooterMode.EDIT_NAMES) {
            JTextField nameField = materialNameEdits.get(itemName);
            if (nameField != null) {
                nameField.setFont(new Font("SansSerif", Font.BOLD, 14));
                nameField.setForeground(theme.text());
                nameField.setCaretColor(theme.text());
                nameField.setOpaque(false);
                nameField.setBorder(null);
                nameField.setMargin(new Insets(0, 8, 0, 8));
                nameField.setPreferredSize(new Dimension(220, 28));
                nameField.setToolTipText("Edit material name");
                nameDisplay = nameField;
            } else {
                nameDisplay = materialNameLabel(itemName);
            }
        } else {
            nameDisplay = materialNameLabel(itemName);
        }
        JTextField quantity = new JTextField(String.valueOf(getStockQuantity(itemName)), 5);
        quantity.setHorizontalAlignment(SwingConstants.CENTER);
        quantity.setFont(new Font("SansSerif", Font.BOLD, 15));
        quantity.setForeground(theme.text());
        quantity.setBackground(theme.isDark() ? new Color(0x3A3734) : theme.input());
        quantity.setCaretColor(theme.text());
        quantity.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.row(), 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
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
        itemLabels.add(nameDisplay, BorderLayout.NORTH);
        JLabel stockStatus = new JLabel();
        stockStatus.setFont(SMALL_FONT);
        updateStockStatus(itemName, stockStatus);
        itemLabels.add(stockStatus, BorderLayout.SOUTH);
        row.add(itemLabels, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        RoundedButton minus = new RoundedButton("-", theme.input(), theme.text(), 8);
        RoundedButton plus = new RoundedButton("+", theme.input(), theme.text(), 8);
        minus.setFont(new Font("SansSerif", Font.BOLD, 14));
        plus.setFont(new Font("SansSerif", Font.BOLD, 14));
        minus.setPreferredSize(new Dimension(36, 36));
        plus.setPreferredSize(new Dimension(36, 36));
        installStockHoldBehavior(minus, itemName, inventory, -1, quantity, stockStatus);
        installStockHoldBehavior(plus, itemName, inventory, 1, quantity, stockStatus);
        controls.add(minus);
        controls.add(quantity);
        controls.add(plus);
        row.add(controls, BorderLayout.EAST);
        if (showSelection) {
            SelectionCircleButton select = new SelectionCircleButton();
            select.setSelected(selectedMaterialsForDeletion.contains(itemName));
            select.setToolTipText("Mark " + itemName + " for deletion");
            select.addActionListener(event -> {
                boolean wasSelected = !select.isSelected();
                pushStockActionUndo(() -> {
                    if (wasSelected) {
                        selectedMaterialsForDeletion.add(itemName);
                    } else {
                        selectedMaterialsForDeletion.remove(itemName);
                    }
                    showPage("Stocks");
                });
                if (select.isSelected()) {
                    selectedMaterialsForDeletion.add(itemName);
                } else {
                    selectedMaterialsForDeletion.remove(itemName);
                }
            });
            row.add(select, BorderLayout.WEST);
        }
        return row;
    }

    private JLabel materialNameLabel(String itemName) {
        JLabel name = new JLabel(itemName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        return name;
    }

    private JPanel materialDraftRow(MaterialDraft draft) {
        RoundedPanel row = new RoundedPanel(14);
        row.setBackground(theme.row());
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 12));

        draft.nameField.setFont(new Font("SansSerif", Font.BOLD, 14));
        draft.nameField.setForeground(theme.text());
        draft.nameField.setCaretColor(theme.text());
        draft.nameField.setOpaque(false);
        draft.nameField.setBorder(null);
        draft.nameField.setMargin(new Insets(0, 8, 0, 8));
        draft.nameField.setPreferredSize(new Dimension(220, 28));
        draft.nameField.setToolTipText("Enter the new material name");

        draft.quantityField.setHorizontalAlignment(SwingConstants.CENTER);
        draft.quantityField.setFont(new Font("SansSerif", Font.BOLD, 15));
        draft.quantityField.setForeground(theme.text());
        draft.quantityField.setCaretColor(theme.text());
        draft.quantityField.setOpaque(false);
        draft.quantityField.setBorder(null);

        JLabel stockStatus = new JLabel("New material");
        stockStatus.setFont(SMALL_FONT);
        stockStatus.setForeground(theme.secondaryText());
        JPanel itemLabels = new JPanel(new BorderLayout(0, 4));
        itemLabels.setOpaque(false);
        itemLabels.add(draft.nameField, BorderLayout.NORTH);
        itemLabels.add(stockStatus, BorderLayout.SOUTH);
        row.add(itemLabels, BorderLayout.CENTER);

        JPanel quantityHolder = new JPanel();
        quantityHolder.setOpaque(false);
        quantityHolder.setLayout(new javax.swing.BoxLayout(quantityHolder, javax.swing.BoxLayout.X_AXIS));
        RoundedButton minus = secondaryButton("-");
        RoundedButton plus = secondaryButton("+");
        minus.setPreferredSize(new Dimension(36, 36));
        minus.setMinimumSize(new Dimension(36, 36));
        minus.setMaximumSize(new Dimension(36, 36));
        plus.setPreferredSize(new Dimension(36, 36));
        plus.setMinimumSize(new Dimension(36, 36));
        plus.setMaximumSize(new Dimension(36, 36));
        draft.quantityField.setPreferredSize(new Dimension(54, 36));
        draft.quantityField.setMinimumSize(new Dimension(54, 36));
        draft.quantityField.setMaximumSize(new Dimension(54, 36));
        quantityHolder.add(javax.swing.Box.createHorizontalStrut(8));
        installDraftHoldBehavior(minus, draft, -1);
        installDraftHoldBehavior(plus, draft, 1);
        quantityHolder.add(minus);
        quantityHolder.add(javax.swing.Box.createHorizontalStrut(6));
        quantityHolder.add(draft.quantityField);
        quantityHolder.add(javax.swing.Box.createHorizontalStrut(6));
        quantityHolder.add(plus);
        quantityHolder.add(javax.swing.Box.createHorizontalStrut(8));
        row.add(quantityHolder, BorderLayout.EAST);
        return row;
    }

    private void installDraftHoldBehavior(RoundedButton button, MaterialDraft draft, int delta) {
        final boolean[] pressed = {false};
        Timer repeatTimer = new Timer(75, event -> adjustDraftQuantity(draft, delta));
        repeatTimer.setInitialDelay(400);
        button.addActionListener(event -> {
            if (!pressed[0]) {
                adjustDraftQuantity(draft, delta);
            }
        });
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                pressed[0] = true;
                adjustDraftQuantity(draft, delta);
                repeatTimer.restart();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                repeatTimer.stop();
                SwingUtilities.invokeLater(() -> pressed[0] = false);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                repeatTimer.stop();
            }
        });
    }

    private void adjustDraftQuantity(MaterialDraft draft, int delta) {
        int current;
        try {
            current = Integer.parseInt(draft.quantityField.getText().trim());
        } catch (NumberFormatException e) {
            current = 0;
        }
        long updated = (long) current + delta;
        if (updated < 0 || updated > Integer.MAX_VALUE) {
            return;
        }
        draft.quantityField.setText(String.valueOf(updated));
    }

    private JPanel buildProductionPage() {
        JPanel page = createPage("Production", "Produce products using their bill of materials.");
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);
        List<String> products = new ArrayList<>(finishedGoods.keySet());
        for (String product : stagedProductAdditions.keySet()) {
            if (!products.contains(product)) {
                products.add(product);
            }
        }
        products.sort(String.CASE_INSENSITIVE_ORDER);
        if (products.isEmpty() && pendingProductDraft == null) {
            GridBagConstraints emptyConstraints = new GridBagConstraints();
            emptyConstraints.gridx = 0;
            emptyConstraints.gridy = 0;
            emptyConstraints.weightx = 1.0;
            emptyConstraints.anchor = GridBagConstraints.NORTHWEST;
            emptyConstraints.fill = GridBagConstraints.HORIZONTAL;
            cards.add(emptyState("No products match your search."), emptyConstraints);
        } else {
            JPanel[] columns = {productionCardColumn(), productionCardColumn()};
            int cardIndex = 0;
            if (pendingProductDraft != null) {
                columns[0].add(buildProductDraftCard());
                columns[0].add(javax.swing.Box.createVerticalStrut(14));
                cardIndex = 1;
            }
            for (String product : products) {
                JPanel column = columns[cardIndex % columns.length];
                JPanel card = buildProductionCard(product);
                column.add(card);
                column.add(javax.swing.Box.createVerticalStrut(14));
                cardIndex++;
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
        RoundedPanel catalog = new RoundedPanel(22);
        catalog.setBackground(theme.panel());
        catalog.setLayout(new BorderLayout(0, 12));
        catalog.setBorder(BorderFactory.createEmptyBorder(15, 17, 15, 17));
        JPanel catalogHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        catalogHeader.setOpaque(false);
        if (productCatalogMode != ProductCatalogMode.NORMAL) {
            StockActionIconButton editAction =
                    new StockActionIconButton(StockHeaderAction.EDIT);
            editAction.setEnabled(!productionTransactionInProgress);
            editAction.addActionListener(event -> beginProductNameEdit());
            StockActionIconButton addAction =
                    new StockActionIconButton(StockHeaderAction.ADD);
            addAction.setEnabled(!productionTransactionInProgress);
            addAction.addActionListener(event -> beginProductAddition());
            StockActionIconButton deleteAction =
                    new StockActionIconButton(StockHeaderAction.DELETE);
            deleteAction.setEnabled(!productionTransactionInProgress);
            deleteAction.addActionListener(event -> beginProductDeletion());
            catalogHeader.add(editAction);
            catalogHeader.add(addAction);
            catalogHeader.add(deleteAction);
        }
        catalog.add(catalogHeader, BorderLayout.NORTH);
        catalog.add(createScrollPane(cards), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        productSaveStatus = new JLabel(" ");
        productSaveStatus.setFont(SMALL_FONT);
        productSaveStatus.setForeground(theme.secondaryText());
        footer.add(productSaveStatus, BorderLayout.CENTER);

        JPanel footerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footerButtons.setOpaque(false);
        if (productCatalogMode == ProductCatalogMode.NORMAL) {
            RoundedButton editButton = new RoundedButton("Edit", theme.input(), theme.text(), 10);
            editButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            editButton.setPreferredSize(new Dimension(100, 44));
            editButton.setEnabled(!productionTransactionInProgress);
            editButton.addActionListener(event -> enterProductToolbarMode());
            footerButtons.add(editButton);
        } else {
            RoundedButton undoButton = new RoundedButton("Undo", theme.input(), theme.text(), 10);
            undoButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            undoButton.setPreferredSize(new Dimension(92, 44));
            undoButton.setEnabled(!productionTransactionInProgress
                    && productTextUndoManager.canUndo());
            undoButton.addActionListener(event -> undoProductAction());
            productUndoButton = undoButton;

            RoundedButton cancelButton = new RoundedButton(
                    "Cancel", theme.input(), theme.text(), 10);
            cancelButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            cancelButton.setPreferredSize(new Dimension(100, 44));
            cancelButton.setEnabled(!productionTransactionInProgress);
            cancelButton.addActionListener(event -> cancelProductCatalogChanges());
            footerButtons.add(undoButton);
            footerButtons.add(cancelButton);
        }
        String primaryLabel = productCatalogMode == ProductCatalogMode.ADD ? "Add"
                : productCatalogMode == ProductCatalogMode.DELETE ? "Delete" : "Save";
        Color primaryFill = productCatalogMode == ProductCatalogMode.DELETE
                ? theme.danger() : new Color(0x23211F);
        RoundedButton primaryButton = new RoundedButton(primaryLabel, primaryFill, Color.WHITE);
        primaryButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        primaryButton.setPreferredSize(new Dimension(120, 44));
        primaryButton.setEnabled(!productionTransactionInProgress);
        primaryButton.addActionListener(event -> handleProductFooterAction());
        footerButtons.add(primaryButton);
        if (productCatalogMode == ProductCatalogMode.NORMAL) {
            productUndoButton = null;
        }
        footer.add(footerButtons, BorderLayout.EAST);
        catalog.add(footer, BorderLayout.SOUTH);
        page.add(catalog, BorderLayout.CENTER);
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
        card.setBorder(BorderFactory.createEmptyBorder(
                17, productCatalogMode == ProductCatalogMode.DELETE ? 10 : 18, 17, 18));

        JComponent name = productNameComponent(product);
        boolean expanded = Boolean.TRUE.equals(productExpansionMap.get(product));
        RoundedButton expandButton = secondaryButton(expanded ? "\u25B2" : "\u25BC");
        expandButton.setPreferredSize(new Dimension(38, 34));
        expandButton.setToolTipText(expanded ? "Hide bill of materials" : "Show bill of materials");
        expandButton.addActionListener(event -> toggleProductionCard(product));
        JPanel productTitle = new JPanel(new BorderLayout(8, 0));
        productTitle.setOpaque(false);
        productTitle.add(name, BorderLayout.CENTER);
        productTitle.add(expandButton, BorderLayout.EAST);
        if (productCatalogMode == ProductCatalogMode.DELETE) {
            SelectionCircleButton select = new SelectionCircleButton();
            select.setSelected(selectedProductsForDeletion.contains(product));
            select.addActionListener(event -> {
                boolean selectedBeforeClick = !select.isSelected();
                pushProductActionUndo(() -> {
                    if (selectedBeforeClick) {
                        selectedProductsForDeletion.add(product);
                    } else {
                        selectedProductsForDeletion.remove(product);
                    }
                    showPage("Production");
                });
                if (select.isSelected()) {
                    selectedProductsForDeletion.add(product);
                } else {
                    selectedProductsForDeletion.remove(product);
                }
            });
            productTitle.add(select, BorderLayout.WEST);
        }
        int quantity = finishedGoods.getOrDefault(
                product, stagedProductAdditions.getOrDefault(product, 0));
        JLabel onHand = new JLabel("Finished units on hand: " + quantity);
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
        for (BOMItem bomItem : billOfMaterials.getOrDefault(product, Collections.emptyList())) {
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
        boolean isStagedProduct = stagedProductAdditions.containsKey(product);
        boolean finishedCapacityAvailable = quantity < Integer.MAX_VALUE;
        boolean stockAvailable = !isStagedProduct && materialsAvailable && finishedCapacityAvailable
                && productionHistoryLoaded && !productionTransactionInProgress
                && productCatalogMode == ProductCatalogMode.NORMAL;
        if (isStagedProduct) {
            status.setText("New product profile — Save to commit");
            status.setForeground(theme.secondaryText());
        } else if (!productionHistoryLoaded) {
            status.setText("Loading production history...");
            status.setForeground(theme.secondaryText());
        } else if (productionTransactionInProgress) {
            status.setText("Recording production...");
            status.setForeground(theme.secondaryText());
        } else if (stockAvailable) {
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

    private JPanel buildProductDraftCard() {
        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(theme.panel());
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(17, 18, 17, 18));
        pendingProductDraft.nameField.setFont(new Font("SansSerif", Font.BOLD, 17));
        pendingProductDraft.nameField.setForeground(theme.text());
        pendingProductDraft.nameField.setCaretColor(theme.text());
        pendingProductDraft.nameField.setOpaque(false);
        pendingProductDraft.nameField.setBorder(null);
        pendingProductDraft.nameField.setMargin(new Insets(0, 8, 0, 8));
        pendingProductDraft.nameField.setPreferredSize(new Dimension(220, 30));
        JLabel status = new JLabel("New product profile");
        status.setFont(SMALL_FONT);
        status.setForeground(theme.secondaryText());
        card.add(pendingProductDraft.nameField, BorderLayout.NORTH);
        card.add(status, BorderLayout.CENTER);
        card.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private JComponent productNameComponent(String product) {
        JTextField editField = productNameEdits.get(product);
        if (productCatalogMode == ProductCatalogMode.EDIT_NAMES && editField != null) {
            editField.setFont(new Font("SansSerif", Font.BOLD, 17));
            editField.setForeground(theme.text());
            editField.setCaretColor(theme.text());
            editField.setOpaque(false);
            editField.setBorder(null);
            editField.setMargin(new Insets(0, 8, 0, 8));
            editField.setPreferredSize(new Dimension(220, 30));
            return editField;
        }
        JLabel name = new JLabel(product);
        name.setFont(new Font("SansSerif", Font.BOLD, 17));
        name.setForeground(theme.text());
        return name;
    }

    private void enterProductToolbarMode() {
        if (!productionHistoryLoaded) {
            showCatalogError("Production history is still loading. Please try again shortly.");
            return;
        }
        productCatalogMode = ProductCatalogMode.TOOLBAR;
        showPage("Production");
    }

    private void beginProductAddition() {
        if (pendingProductDraft != null) {
            return;
        }
        pendingProductDraft = new ProductDraft();
        attachProductTextUndo(pendingProductDraft.nameField);
        productCatalogMode = ProductCatalogMode.ADD;
        pushProductActionUndo(() -> {
            pendingProductDraft = null;
            productCatalogMode = ProductCatalogMode.TOOLBAR;
            showPage("Production");
        });
        showPage("Production");
        if (productSaveStatus != null) {
            productSaveStatus.setText("Enter the product name, then select Add.");
        }
        pendingProductDraft.nameField.requestFocusInWindow();
    }

    private boolean stageProductAddition() {
        if (pendingProductDraft == null) {
            showCatalogError("There is no new product card to add.");
            return false;
        }
        String name = pendingProductDraft.nameField.getText().trim();
        if (!isValidCatalogName(name)) {
            showCatalogError("Enter a product name without tabs or line breaks.");
            pendingProductDraft.nameField.requestFocusInWindow();
            return false;
        }
        if (containsItemIgnoreCase(finishedGoods, name)
                || containsItemIgnoreCase(stagedProductAdditions, name)) {
            showCatalogError("A product with the name \"" + name + "\" already exists.");
            pendingProductDraft.nameField.requestFocusInWindow();
            return false;
        }
        ProductDraft draft = pendingProductDraft;
        stagedProductAdditions.put(name, 0);
        pendingProductDraft = null;
        productCatalogMode = ProductCatalogMode.TOOLBAR;
        pushProductActionUndo(() -> {
            stagedProductAdditions.remove(name);
            productNameEdits.remove(name);
            productExpansionMap.remove(name);
            pendingProductDraft = draft;
            productCatalogMode = ProductCatalogMode.ADD;
            showPage("Production");
        });
        showPage("Production");
        return true;
    }

    private void beginProductNameEdit() {
        for (String product : finishedGoods.keySet()) {
            if (!productNameEdits.containsKey(product)) {
                JTextField field = new JTextField(product, 24);
                attachProductTextUndo(field);
                productNameEdits.put(product, field);
            }
        }
        for (String product : stagedProductAdditions.keySet()) {
            if (!productNameEdits.containsKey(product)) {
                JTextField field = new JTextField(product, 24);
                attachProductTextUndo(field);
                productNameEdits.put(product, field);
            }
        }
        productCatalogMode = ProductCatalogMode.EDIT_NAMES;
        showPage("Production");
        if (!productNameEdits.isEmpty()) {
            JTextField firstField = productNameEdits.values().iterator().next();
            SwingUtilities.invokeLater(() -> {
                firstField.requestFocusInWindow();
                firstField.setCaretPosition(firstField.getText().length());
            });
        }
    }

    private void beginProductDeletion() {
        productCatalogMode = ProductCatalogMode.DELETE;
        showPage("Production");
        if (productSaveStatus != null) {
            productSaveStatus.setText("Select products to delete, then select Delete.");
        }
    }

    private void handleProductFooterAction() {
        if (productCatalogMode == ProductCatalogMode.ADD) {
            if (stageProductAddition()) {
                if (productSaveStatus != null) {
                    productSaveStatus.setText("Product staged. Select Save to commit changes.");
                }
            }
            return;
        }
        if (productCatalogMode == ProductCatalogMode.DELETE) {
            int choice = showThemedChoiceDialog("Confirm Deletion",
                    "Are you sure you want to delete the selected items?", "Yes", "No");
            if (choice != 0) {
                return;
            }
        }
        if (productCatalogMode == ProductCatalogMode.NORMAL
                && stagedProductAdditions.isEmpty() && productNameEdits.isEmpty()
                && selectedProductsForDeletion.isEmpty()) {
            if (productSaveStatus != null) {
                productSaveStatus.setForeground(theme.success());
                productSaveStatus.setText("Changes saved successfully!");
            }
            return;
        }
        if (commitProductCatalogChanges() && productSaveStatus != null) {
            productSaveStatus.setForeground(theme.success());
            productSaveStatus.setText("Changes saved successfully!");
        }
    }

    private boolean commitProductCatalogChanges() {
        Map<String, Integer> currentProducts = new LinkedHashMap<>(finishedGoods);
        currentProducts.putAll(stagedProductAdditions);
        Map<String, Integer> updatedProducts = new LinkedHashMap<>();
        Map<String, Boolean> updatedExpansion = new LinkedHashMap<>();
        Map<String, List<BOMItem>> updatedBOM = new LinkedHashMap<>();
        Map<String, Integer> updatedOpeningBalances = new LinkedHashMap<>();
        Map<String, Long> updatedLifetimeUnits = new LinkedHashMap<>();
        Map<String, Integer> updatedActiveUnits = new LinkedHashMap<>();
        Map<String, String> renamedProducts = new LinkedHashMap<>();
        Set<String> uniqueNames = new HashSet<>();

        for (Map.Entry<String, Integer> entry : currentProducts.entrySet()) {
            String oldName = entry.getKey();
            if (selectedProductsForDeletion.contains(oldName)) {
                continue;
            }
            JTextField editField = productNameEdits.get(oldName);
            String newName = editField == null ? oldName : editField.getText().trim();
            if (!isValidCatalogName(newName)) {
                showCatalogError("Enter a product name without tabs or line breaks.");
                if (editField != null) {
                    editField.requestFocusInWindow();
                }
                return false;
            }
            if (!uniqueNames.add(newName.toLowerCase(Locale.ROOT))) {
                showCatalogError("A product with the name \"" + newName + "\" already exists.");
                if (editField != null) {
                    editField.requestFocusInWindow();
                }
                return false;
            }
            updatedProducts.put(newName, entry.getValue());
            updatedExpansion.put(newName,
                    productExpansionMap.getOrDefault(oldName, Boolean.FALSE));
            updatedBOM.put(newName, billOfMaterials.getOrDefault(oldName, Collections.emptyList()));
            updatedOpeningBalances.put(newName,
                    productionOpeningBalances.getOrDefault(oldName, 0));
            updatedLifetimeUnits.put(newName,
                    lifetimeProductionUnits.getOrDefault(oldName, 0L));
            if (activeProductionUnits.containsKey(oldName)) {
                updatedActiveUnits.put(newName, activeProductionUnits.get(oldName));
            }
            if (!oldName.equals(newName)) {
                renamedProducts.put(oldName, newName);
            }
        }

        Map<String, Integer> previousFinishedGoods = new LinkedHashMap<>(finishedGoods);
        Map<String, Boolean> previousExpansion = new LinkedHashMap<>(productExpansionMap);
        Map<String, List<BOMItem>> previousBOM = new LinkedHashMap<>(billOfMaterials);
        Map<String, Integer> previousOpeningBalances =
                new LinkedHashMap<>(productionOpeningBalances);
        Map<String, Long> previousLifetimeUnits = new LinkedHashMap<>(lifetimeProductionUnits);
        Map<String, Integer> previousActiveUnits = new LinkedHashMap<>(activeProductionUnits);
        List<String> sortedNames = new ArrayList<>(updatedProducts.keySet());
        sortedNames.sort(String.CASE_INSENSITIVE_ORDER);
        finishedGoods.clear();
        for (String name : sortedNames) {
            finishedGoods.put(name, updatedProducts.get(name));
        }
        productExpansionMap.clear();
        productExpansionMap.putAll(updatedExpansion);
        billOfMaterials.clear();
        billOfMaterials.putAll(updatedBOM);
        productionOpeningBalances.clear();
        productionOpeningBalances.putAll(updatedOpeningBalances);
        lifetimeProductionUnits.clear();
        lifetimeProductionUnits.putAll(updatedLifetimeUnits);
        activeProductionUnits.clear();
        activeProductionUnits.putAll(updatedActiveUnits);
        try {
            saveInventory();
        } catch (IOException e) {
            finishedGoods.clear();
            finishedGoods.putAll(previousFinishedGoods);
            productExpansionMap.clear();
            productExpansionMap.putAll(previousExpansion);
            billOfMaterials.clear();
            billOfMaterials.putAll(previousBOM);
            productionOpeningBalances.clear();
            productionOpeningBalances.putAll(previousOpeningBalances);
            lifetimeProductionUnits.clear();
            lifetimeProductionUnits.putAll(previousLifetimeUnits);
            activeProductionUnits.clear();
            activeProductionUnits.putAll(previousActiveUnits);
            showCatalogError("Unable to save product changes to inventory_data.txt:\n"
                    + e.getMessage());
            return false;
        }
        if (!renamedProducts.isEmpty()) {
            productAliasUpdateInProgress = true;
            productionBackend.execute(() -> applyProductAliases(renamedProducts));
        }
        stagedProductAdditions.clear();
        productNameEdits.clear();
        selectedProductsForDeletion.clear();
        pendingProductDraft = null;
        productTextUndoManager.discardAllEdits();
        productCatalogMode = ProductCatalogMode.NORMAL;
        productUndoButton = null;
        showPage("Production");
        return true;
    }

    private void cancelProductCatalogChanges() {
        for (String product : stagedProductAdditions.keySet()) {
            productExpansionMap.remove(product);
        }
        stagedProductAdditions.clear();
        productNameEdits.clear();
        selectedProductsForDeletion.clear();
        pendingProductDraft = null;
        productTextUndoManager.discardAllEdits();
        productCatalogMode = ProductCatalogMode.NORMAL;
        productUndoButton = null;
        showPage("Production");
    }

    private void attachProductTextUndo(JTextField field) {
        field.getDocument().addUndoableEditListener(event -> {
            productTextUndoManager.addEdit(event.getEdit());
            refreshProductUndoButton();
        });
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshProductUndoButton();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshProductUndoButton();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshProductUndoButton();
            }
        });
    }

    private void pushProductActionUndo(Runnable undoAction) {
        productTextUndoManager.addEdit(new javax.swing.undo.AbstractUndoableEdit() {
            private static final long serialVersionUID = 1L;

            @Override
            public void undo() {
                super.undo();
                undoAction.run();
            }

            @Override
            public String getPresentationName() {
                return "Product catalog action";
            }
        });
        refreshProductUndoButton();
    }

    private void undoProductAction() {
        if (productTextUndoManager.canUndo()) {
            productTextUndoManager.undo();
        }
        refreshProductUndoButton();
        applicationRoot.repaint();
    }

    private void refreshProductUndoButton() {
        if (productUndoButton != null) {
            productUndoButton.setEnabled(productTextUndoManager.canUndo());
        }
    }

    private boolean resolvePendingProductChanges() {
        if (productCatalogMode != ProductCatalogMode.NORMAL
                || pendingProductDraft != null || !stagedProductAdditions.isEmpty()
                || !productNameEdits.isEmpty() || !selectedProductsForDeletion.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Save or cancel pending product changes from the Production footer before leaving.",
                    "Pending Product Changes", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean isValidCatalogName(String name) {
        return !name.isEmpty() && name.indexOf('\t') < 0
                && name.indexOf('\n') < 0 && name.indexOf('\r') < 0;
    }

    private void toggleProductionCard(String product) {
        productExpansionMap.put(product,
                !Boolean.TRUE.equals(productExpansionMap.get(product)));
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
        int previous = pendingQuantity == null ? getStockQuantity(itemName) : pendingQuantity;
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
        Integer onHand = pending == null
                ? rawMaterials.getOrDefault(itemName, stagedMaterialAdditions.get(itemName))
                : pending;
        return onHand == null ? 0 : onHand;
    }

    private void stageManualStockOverride(String itemName, JTextField quantityField) {
        String entered = quantityField.getText().trim();
        final int quantity;
        try {
            quantity = Integer.parseInt(entered);
        } catch (NumberFormatException e) {
            quantityField.setText(String.valueOf(getStockQuantity(itemName)));
            return;
        }
        if (quantity < 0) {
            quantityField.setText(String.valueOf(getStockQuantity(itemName)));
            return;
        }
        Integer savedQuantity = stagedMaterialAdditions.containsKey(itemName)
                ? stagedMaterialAdditions.get(itemName) : rawMaterials.get(itemName);
        if (savedQuantity != null && quantity == savedQuantity) {
            pendingStockUpdates.remove(itemName);
        } else {
            pendingStockUpdates.put(itemName, quantity);
        }
        updateNotificationBadge();
    }

    private boolean resolvePendingStockUpdates() {
        if (!pendingStockUpdates.isEmpty() || !pendingMaterialDrafts.isEmpty()
                || !stagedMaterialAdditions.isEmpty()
                || !materialNameEdits.isEmpty() || !selectedMaterialsForDeletion.isEmpty()
                || stockFooterMode != StockFooterMode.NORMAL) {
            JOptionPane.showMessageDialog(frame,
                    "Save or cancel pending stock changes from the Stocks footer before leaving.",
                    "Pending Material Changes", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean commitPendingStockUpdates() {
        if (!stockFooterSaveInProgress
                && (!pendingStockUpdates.isEmpty() || !pendingMaterialDrafts.isEmpty()
                        || !stagedMaterialAdditions.isEmpty()
                        || !materialNameEdits.isEmpty() || !selectedMaterialsForDeletion.isEmpty()
                        || stockFooterMode != StockFooterMode.NORMAL)) {
            showCatalogError("Use the Stocks footer Save button to commit material changes.");
            return false;
        }
        if (pendingStockUpdates.isEmpty() && pendingMaterialDrafts.isEmpty()
                && stagedMaterialAdditions.isEmpty()
                && materialNameEdits.isEmpty()
                && selectedMaterialsForDeletion.isEmpty()) {
            if (stockFooterMode != StockFooterMode.NORMAL) {
                stockFooterMode = StockFooterMode.NORMAL;
                showPage("Stocks");
            }
            return true;
        }
        Map<String, Integer> additions = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> addition : stagedMaterialAdditions.entrySet()) {
            if (!selectedMaterialsForDeletion.contains(addition.getKey())) {
                additions.put(addition.getKey(),
                        pendingStockUpdates.getOrDefault(addition.getKey(), addition.getValue()));
            }
        }
        for (MaterialDraft draft : pendingMaterialDrafts) {
            String name = draft.nameField.getText().trim();
            if (name.isEmpty() || name.indexOf('\t') >= 0
                    || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
                showCatalogError("Enter a material name without tabs or line breaks.");
                draft.nameField.requestFocusInWindow();
                return false;
            }
            if (containsItemIgnoreCase(rawMaterials, name)
                    || containsItemIgnoreCase(additions, name)) {
                showCatalogError("A material with the name \"" + name + "\" already exists.");
                draft.nameField.requestFocusInWindow();
                return false;
            }
            final int quantity;
            try {
                quantity = Integer.parseInt(draft.quantityField.getText().trim());
            } catch (NumberFormatException e) {
                showCatalogError("Enter a whole number from 0 to " + Integer.MAX_VALUE
                        + " for " + name + ".");
                draft.quantityField.requestFocusInWindow();
                return false;
            }
            if (quantity < 0) {
                showCatalogError("Stock quantity cannot be negative for " + name + ".");
                draft.quantityField.requestFocusInWindow();
                return false;
            }
            additions.put(name, quantity);
        }

        Map<String, String> renamedMaterials = new LinkedHashMap<>();
        Set<String> uniqueNames = new HashSet<>();
        for (String oldName : rawMaterials.keySet()) {
            if (selectedMaterialsForDeletion.contains(oldName)) {
                continue;
            }
            JTextField editField = materialNameEdits.get(oldName);
            String newName = editField == null ? oldName : editField.getText().trim();
            if (newName.isEmpty() || newName.indexOf('\t') >= 0
                    || newName.indexOf('\n') >= 0 || newName.indexOf('\r') >= 0) {
                showCatalogError("Enter a material name without tabs or line breaks.");
                if (editField != null) {
                    editField.requestFocusInWindow();
                }
                return false;
            }
            if (!uniqueNames.add(newName.toLowerCase(Locale.ROOT))) {
                showCatalogError("A material with the name \"" + newName + "\" already exists.");
                if (editField != null) {
                    editField.requestFocusInWindow();
                }
                return false;
            }
            if (!oldName.equals(newName)) {
                renamedMaterials.put(oldName, newName);
            }
        }
        for (String newName : additions.keySet()) {
            if (!uniqueNames.add(newName.toLowerCase(Locale.ROOT))) {
                showCatalogError("A material with the name \"" + newName + "\" already exists.");
                return false;
            }
        }

        Map<String, Integer> previousFinishedValues = new LinkedHashMap<>(finishedGoods);
        Map<String, Integer> previousRawValues = new LinkedHashMap<>(rawMaterials);
        Map<String, Integer> previousPendingValues = new LinkedHashMap<>(pendingStockUpdates);
        Map<String, List<BOMItem>> previousBillOfMaterials =
                new LinkedHashMap<>(billOfMaterials);
        Map<String, Integer> updatedRawMaterials = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> material : rawMaterials.entrySet()) {
            String oldName = material.getKey();
            if (selectedMaterialsForDeletion.contains(oldName)) {
                continue;
            }
            String newName = renamedMaterials.getOrDefault(oldName, oldName);
            int quantity = pendingStockUpdates.getOrDefault(oldName, material.getValue());
            updatedRawMaterials.put(newName, quantity);
        }
        for (String product : new ArrayList<>(billOfMaterials.keySet())) {
            List<BOMItem> updatedItems = new ArrayList<>();
            for (BOMItem item : billOfMaterials.get(product)) {
                updatedItems.add(new BOMItem(
                        renamedMaterials.getOrDefault(item.materialName, item.materialName),
                        item.quantityRequired));
            }
            billOfMaterials.put(product, Collections.unmodifiableList(updatedItems));
        }
        rawMaterials.clear();
        rawMaterials.putAll(updatedRawMaterials);
        rawMaterials.putAll(additions);
        try {
            saveInventory();
        } catch (IOException e) {
            finishedGoods.clear();
            finishedGoods.putAll(previousFinishedValues);
            rawMaterials.clear();
            rawMaterials.putAll(previousRawValues);
            pendingStockUpdates.clear();
            pendingStockUpdates.putAll(previousPendingValues);
            billOfMaterials.clear();
            billOfMaterials.putAll(previousBillOfMaterials);
            showSaveError(e);
            return false;
        }
        pendingStockUpdates.clear();
        pendingMaterialDrafts.clear();
        stagedMaterialAdditions.clear();
        materialNameEdits.clear();
        selectedMaterialsForDeletion.clear();
        stockTextUndoManager.discardAllEdits();
        stockActionUndoHistory.clear();
        stockFooterMode = StockFooterMode.NORMAL;
        stockUndoButton = null;
        updateNotificationBadge();
        if ("Stocks".equals(currentPage)) {
            showPage("Stocks");
        }
        return true;
    }

    private void navigateTo(String page) {
        if (page.equals(currentPage)) {
            return;
        }
        if (productionTransactionInProgress) {
            JOptionPane.showMessageDialog(frame,
                    "Please wait for the production transaction to finish.",
                    "Production In Progress", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if ("Stocks".equals(currentPage) && !resolvePendingStockUpdates()) {
            return;
        }
        if ("Production".equals(currentPage) && !resolvePendingProductChanges()) {
            return;
        }
        currentPage = page;
        buildApplication();
    }

    private void produceProduct(String product) {
        if (!productionHistoryLoaded || productionTransactionInProgress
                || productCatalogMode != ProductCatalogMode.NORMAL) {
            return;
        }
        List<String> unavailable = getUnavailableBOMItems(product);
        if (!unavailable.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Not Available for Production: Insufficient Raw Materials\n"
                            + String.join("\n", unavailable),
                    "Production Unavailable", JOptionPane.WARNING_MESSAGE);
            showPage("Production");
            return;
        }
        if (finishedGoods.getOrDefault(product, 0) == Integer.MAX_VALUE) {
            JOptionPane.showMessageDialog(frame, "Finished stock has reached the maximum supported quantity.",
                    "Production Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Instant producedAt = Instant.now();
        List<BOMItem> consumedItems = new ArrayList<>(
                billOfMaterials.getOrDefault(product, Collections.emptyList()));
        Map<String, Integer> updatedRaw = new LinkedHashMap<>(rawMaterials);
        for (BOMItem bomItem : billOfMaterials.getOrDefault(product, Collections.emptyList())) {
            updatedRaw.put(bomItem.materialName,
                    updatedRaw.get(bomItem.materialName) - bomItem.quantityRequired);
        }
        Map<String, Integer> persistedFinished = getPersistedFinishedGoodsSnapshot();
        if (persistedFinished.getOrDefault(product, 0) == Integer.MAX_VALUE) {
            JOptionPane.showMessageDialog(frame,
                    "Production history has reached the maximum inventory quantity.",
                    "Production Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        persistedFinished.put(product, persistedFinished.getOrDefault(product, 0) + 1);
        productionTransactionInProgress = true;
        showPage("Production");
        ProductionBatch batch = new ProductionBatch(producedAt, product, 1);
        productionBackend.execute(() -> {
            IOException inventoryFailure = null;
            IOException auditFailure = null;
            boolean productionRecordWritten = false;
            try {
                saveInventorySnapshot(persistedFinished, updatedRaw);
            } catch (IOException e) {
                inventoryFailure = e;
            }
            if (inventoryFailure == null) {
                try {
                    appendProductionAudit(batch);
                    productionRecordWritten = true;
                    activeProductionBatches.add(batch);
                    scheduleNextProductionExpiry();
                } catch (IOException e) {
                    auditFailure = e;
                }
                try {
                    appendMaterialConsumptionAudit(batch, consumedItems);
                } catch (IOException e) {
                    if (auditFailure == null) {
                        auditFailure = e;
                    } else {
                        auditFailure.addSuppressed(e);
                    }
                }
                try {
                    appendProductionHistory(batch, consumedItems);
                } catch (IOException e) {
                    if (auditFailure == null) {
                        auditFailure = e;
                    } else {
                        auditFailure.addSuppressed(e);
                    }
                }
            }
            final IOException savedInventoryFailure = inventoryFailure;
            final IOException savedAuditFailure = auditFailure;
            final boolean savedProductionRecordWritten = productionRecordWritten;
            SwingUtilities.invokeLater(() -> {
                productionTransactionInProgress = false;
                if (savedInventoryFailure != null) {
                    showSaveError(savedInventoryFailure);
                    showPage("Production");
                    return;
                }
                rawMaterials.clear();
                rawMaterials.putAll(updatedRaw);
                long liveUnits = (long) finishedGoods.getOrDefault(product, 0) + 1;
                finishedGoods.put(product, (int) Math.min(Integer.MAX_VALUE, liveUnits));
                if (savedProductionRecordWritten) {
                    lifetimeProductionUnits.merge(product, 1L, Long::sum);
                    activeProductionUnits.merge(product, 1, (left, right) ->
                            (int) Math.min(Integer.MAX_VALUE, (long) left + right));
                } else {
                    productionOpeningBalances.merge(product, 1, (left, right) ->
                            (int) Math.min(Integer.MAX_VALUE, (long) left + right));
                }
                updateNotificationBadge();
                showPage("Production");
                if (savedAuditFailure != null) {
                    JOptionPane.showMessageDialog(frame,
                            "Production and inventory were saved, but one or more append-only "
                                    + "history records could not be written:\n"
                                    + savedAuditFailure.getMessage(),
                            "Production History Error", JOptionPane.ERROR_MESSAGE);
                }
                applicationRoot.repaint();
            });
        });
    }

    private void initializeHistoryFile() throws IOException {
        for (Path historyFile : Arrays.asList(
                HISTORY_FILE, PRODUCTION_LOG_FILE, MATERIAL_CONSUMPTION_FILE,
                PRODUCT_ALIAS_FILE)) {
            Path absoluteHistoryFile = historyFile.toAbsolutePath();
            Path parent = absoluteHistoryFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(absoluteHistoryFile)) {
                Files.createFile(absoluteHistoryFile);
            }
        }
    }

    private void appendProductionAudit(ProductionBatch batch) throws IOException {
        String record = PRODUCTION_TIMESTAMP_FORMAT.format(batch.producedAt)
                + "\t" + batch.productName + "\t" + batch.quantity;
        appendAuditLine(PRODUCTION_LOG_FILE, record);
    }

    private void appendMaterialConsumptionAudit(ProductionBatch batch, List<BOMItem> items)
            throws IOException {
        if (items.isEmpty()) {
            appendAuditLine(MATERIAL_CONSUMPTION_FILE,
                    PRODUCTION_TIMESTAMP_FORMAT.format(batch.producedAt)
                            + "\t" + batch.productName + "\t-\t0");
            return;
        }
        for (BOMItem item : items) {
            String record = PRODUCTION_TIMESTAMP_FORMAT.format(batch.producedAt)
                    + "\t" + batch.productName + "\t" + item.materialName
                    + "\t" + item.quantityRequired;
            appendAuditLine(MATERIAL_CONSUMPTION_FILE, record);
        }
    }

    private void appendAuditLine(Path path, String record) throws IOException {
        boolean needsLineBreak = false;
        if (Files.exists(path) && Files.size(path) > 0) {
            long fileSize = Files.size(path);
            try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
                channel.position(fileSize - 1);
                ByteBuffer lastByte = ByteBuffer.allocate(1);
                channel.read(lastByte);
                lastByte.flip();
                needsLineBreak = lastByte.get() != (byte) '\n';
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (needsLineBreak) {
                writer.newLine();
            }
            writer.write(record);
            writer.newLine();
        }
    }

    private void applyProductAliases(Map<String, String> aliases) {
        IOException aliasFailure = null;
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            try {
                appendAuditLine(PRODUCT_ALIAS_FILE,
                        PRODUCTION_TIMESTAMP_FORMAT.format(Instant.now())
                                + "\t" + alias.getKey() + "\t" + alias.getValue());
            } catch (IOException e) {
                if (aliasFailure == null) {
                    aliasFailure = e;
                } else {
                    aliasFailure.addSuppressed(e);
                }
            }
        }
        for (int index = 0; index < activeProductionBatches.size(); index++) {
            ProductionBatch batch = activeProductionBatches.get(index);
            String finalName = resolveProductAlias(batch.productName, aliases);
            if (!finalName.equals(batch.productName)) {
                activeProductionBatches.set(index,
                        new ProductionBatch(batch.producedAt, finalName, batch.quantity));
            }
        }
        Map<String, Integer> activeByProduct = aggregateActiveBatches();
        IOException finalAliasFailure = aliasFailure;
        SwingUtilities.invokeLater(() -> {
            productAliasUpdateInProgress = false;
            activeProductionUnits.clear();
            activeProductionUnits.putAll(activeByProduct);
            refreshFinishedGoodsFromActiveBatches(activeByProduct);
            if (finalAliasFailure != null) {
                JOptionPane.showMessageDialog(frame,
                        "Product names were saved, but production history aliases could not "
                                + "be fully archived:\n" + finalAliasFailure.getMessage(),
                        "Production History Error", JOptionPane.ERROR_MESSAGE);
            }
            if (pageHost != null && "Production".equals(currentPage)) {
                showPage("Production");
            }
        });
        scheduleNextProductionExpiry();
    }

    private String resolveProductAlias(String product, Map<String, String> aliases) {
        Set<String> visited = new HashSet<>();
        String current = product;
        while (aliases.containsKey(current) && visited.add(current)) {
            current = aliases.get(current);
        }
        return current;
    }

    private Map<String, Integer> aggregateActiveBatches() {
        Map<String, Integer> activeByProduct = new LinkedHashMap<>();
        for (ProductionBatch batch : activeProductionBatches) {
            activeByProduct.merge(batch.productName, batch.quantity, (left, right) ->
                    (int) Math.min(Integer.MAX_VALUE, (long) left + right));
        }
        return activeByProduct;
    }

    private void appendProductionHistory(ProductionBatch batch, List<BOMItem> items)
            throws IOException {
        String timestamp = "[" + batch.producedAt.atZone(ZoneId.systemDefault())
                .format(HISTORY_TIMESTAMP_FORMAT) + "] ";
        StringJoiner consumedMaterials = new StringJoiner(", ");
        for (BOMItem item : items) {
            consumedMaterials.add(item.materialName + " (" + item.quantityRequired + ")");
        }
        String productRecord = timestamp + "Produced: " + batch.productName
                + " (" + batch.quantity + ")";
        String consumptionRecord = timestamp + "Consumed for " + batch.productName + ": "
                + consumedMaterials;
        appendAuditLine(HISTORY_FILE, productRecord);
        appendAuditLine(HISTORY_FILE, consumptionRecord);
    }

    private void loadProductionLedger(Map<String, Integer> inventoryAtStartup) {
        Map<String, Long> rawTotalsByProduct = new LinkedHashMap<>();
        Map<String, String> aliases = new LinkedHashMap<>();
        Map<String, Long> totalsByProduct = new LinkedHashMap<>();
        Map<String, Integer> activeByProduct = new LinkedHashMap<>();
        List<ProductionBatch> activeBatches = new ArrayList<>();
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofHours(72));
        try (BufferedReader reader = Files.newBufferedReader(
                PRODUCT_ALIAS_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                if (fields.length == 3 && !fields[1].isEmpty() && !fields[2].isEmpty()) {
                    try {
                        Instant.parse(fields[0]);
                        aliases.put(fields[1], fields[2]);
                    } catch (DateTimeParseException e) {
                        System.err.println("Ignoring malformed product alias log row.");
                    }
                }
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                    "Unable to load product alias history:\n" + e.getMessage(),
                    "Production History Error", JOptionPane.ERROR_MESSAGE));
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(
                PRODUCTION_LOG_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 3 || fields[1].isEmpty()) {
                    continue;
                }
                Instant producedAt;
                int quantity;
                try {
                    producedAt = Instant.parse(fields[0]);
                    quantity = Integer.parseInt(fields[2]);
                } catch (DateTimeParseException | NumberFormatException e) {
                    System.err.println("Ignoring malformed production log row.");
                    continue;
                }
                if (quantity <= 0) {
                    System.err.println("Ignoring non-positive production log quantity.");
                    continue;
                }
                if (producedAt.isAfter(now)) {
                    System.err.println("Ignoring production log row with a future timestamp.");
                    continue;
                }
                String product = fields[1];
                rawTotalsByProduct.merge(product, (long) quantity,
                        (left, right) -> left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right);
                if (!producedAt.isBefore(cutoff) && !producedAt.isAfter(now)) {
                    activeBatches.add(new ProductionBatch(producedAt, product, quantity));
                }
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                    "Unable to load production batch history:\n" + e.getMessage(),
                    "Production History Error", JOptionPane.ERROR_MESSAGE));
            return;
        }

        for (Map.Entry<String, Long> entry : rawTotalsByProduct.entrySet()) {
            String resolvedName = resolveProductAlias(entry.getKey(), aliases);
            totalsByProduct.merge(resolvedName, entry.getValue(),
                    (left, right) -> left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right);
        }
        List<ProductionBatch> catalogBatches = new ArrayList<>();
        for (ProductionBatch batch : activeBatches) {
            String resolvedName = resolveProductAlias(batch.productName, aliases);
            if (inventoryAtStartup.containsKey(resolvedName)) {
                ProductionBatch resolvedBatch = new ProductionBatch(
                        batch.producedAt, resolvedName, batch.quantity);
                catalogBatches.add(resolvedBatch);
                activeByProduct.merge(resolvedName, batch.quantity, (left, right) ->
                        (int) Math.min(Integer.MAX_VALUE, (long) left + right));
            }
        }

        SwingUtilities.invokeLater(() -> {
            productionOpeningBalances.clear();
            lifetimeProductionUnits.clear();
            activeProductionUnits.clear();
            activeProductionBatches.clear();
            activeProductionBatches.addAll(catalogBatches);
            lifetimeProductionUnits.putAll(totalsByProduct);
            activeProductionUnits.putAll(activeByProduct);
            for (Map.Entry<String, Integer> item : inventoryAtStartup.entrySet()) {
                long baseline = Math.max(0L,
                        (long) item.getValue() - totalsByProduct.getOrDefault(item.getKey(), 0L));
                productionOpeningBalances.put(item.getKey(),
                        (int) Math.min(Integer.MAX_VALUE, baseline));
            }
            refreshFinishedGoodsFromActiveBatches(activeByProduct);
            productionHistoryLoaded = true;
            if (pageHost != null && ("Production".equals(currentPage)
                    || "Dashboard".equals(currentPage))) {
                showPage(currentPage);
            }
            productionBackend.execute(this::scheduleNextProductionExpiry);
        });
    }

    private void expireProductionBatches() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(72));
        activeProductionBatches.removeIf(batch -> batch.producedAt.isBefore(cutoff));
        Map<String, Integer> activeByProduct = new LinkedHashMap<>();
        for (ProductionBatch batch : activeProductionBatches) {
            activeByProduct.merge(batch.productName, batch.quantity, (left, right) ->
                    (int) Math.min(Integer.MAX_VALUE, (long) left + right));
        }
        SwingUtilities.invokeLater(() -> {
            if (!productionHistoryLoaded) {
                return;
            }
            activeProductionUnits.clear();
            activeProductionUnits.putAll(activeByProduct);
            refreshFinishedGoodsFromActiveBatches(activeByProduct);
            if (pageHost != null && ("Production".equals(currentPage)
                    || "Dashboard".equals(currentPage))) {
                showPage(currentPage);
            }
        });
        scheduleNextProductionExpiry();
    }

    private void scheduleNextProductionExpiry() {
        if (productionExpiryTask != null) {
            productionExpiryTask.cancel(false);
            productionExpiryTask = null;
        }
        if (activeProductionBatches.isEmpty()) {
            return;
        }
        Instant nextExpiry = activeProductionBatches.get(0).producedAt.plus(Duration.ofHours(72));
        for (ProductionBatch batch : activeProductionBatches) {
            Instant expiry = batch.producedAt.plus(Duration.ofHours(72));
            if (expiry.isBefore(nextExpiry)) {
                nextExpiry = expiry;
            }
        }
        long delayMillis = Math.max(1L,
                Duration.between(Instant.now(), nextExpiry).toMillis());
        productionExpiryTask = productionBackend.schedule(
                this::expireProductionBatches, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void refreshFinishedGoodsFromActiveBatches(Map<String, Integer> activeByProduct) {
        for (String product : new ArrayList<>(finishedGoods.keySet())) {
            long count = (long) productionOpeningBalances.getOrDefault(product, 0)
                    + activeByProduct.getOrDefault(product, 0);
            finishedGoods.put(product, (int) Math.min(Integer.MAX_VALUE, count));
        }
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
        for (BOMItem bomItem : billOfMaterials.getOrDefault(product, Collections.emptyList())) {
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
        if (productionTransactionInProgress || productAliasUpdateInProgress) {
            JOptionPane.showMessageDialog(frame,
                    "Please wait for the production records to finish saving.",
                    "Production In Progress", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
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
            boolean hadPendingUpdates = !pendingStockUpdates.isEmpty()
                    || !pendingMaterialDrafts.isEmpty()
                    || !selectedMaterialsForDeletion.isEmpty();
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
        if (productionExpiryTask != null) {
            productionExpiryTask.cancel(false);
        }
        productionBackend.shutdown();
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

    private long getFinishedUnitCount() {
        long total = 0;
        for (int quantity : activeProductionUnits.values()) {
            total += quantity;
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
        finishedGoods.clear();
        rawMaterials.clear();
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
        finishedGoods.clear();
        rawMaterials.clear();
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
                String itemName = fields[1].trim();
                if (itemName.isEmpty() || itemName.indexOf('\t') >= 0
                        || itemName.indexOf('\n') >= 0 || itemName.indexOf('\r') >= 0) {
                    System.err.println("Ignoring invalid inventory item at line " + lineNumber + ".");
                    continue;
                }
                Map<String, Integer> inventory = "F".equals(fields[0]) ? finishedGoods : rawMaterials;
                if (containsItemIgnoreCase(inventory, itemName)) {
                    System.err.println("Ignoring duplicate inventory item at line " + lineNumber + ".");
                    continue;
                }
                try {
                    int quantity = Integer.parseInt(fields[2]);
                    if (quantity < 0) {
                        System.err.println("Ignoring negative quantity at line " + lineNumber + ".");
                        continue;
                    }
                    inventory.put(itemName, quantity);
                } catch (NumberFormatException e) {
                    System.err.println("Ignoring invalid quantity at line " + lineNumber + ".");
                }
            }
        }
        for (String product : finishedGoods.keySet()) {
            billOfMaterials.putIfAbsent(product, Collections.emptyList());
            productExpansionMap.putIfAbsent(product, Boolean.FALSE);
        }
    }

    private void saveInventory() throws IOException {
        Map<String, Integer> persistedFinishedGoods = getPersistedFinishedGoodsSnapshot();
        saveInventorySnapshot(persistedFinishedGoods, rawMaterials);
    }

    private Map<String, Integer> getPersistedFinishedGoodsSnapshot() {
        Map<String, Integer> persistedFinishedGoods = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> item : finishedGoods.entrySet()) {
            long openingBalance = productionOpeningBalances.getOrDefault(
                    item.getKey(), item.getValue());
            long lifetimeUnits = lifetimeProductionUnits.getOrDefault(item.getKey(), 0L);
            long persistedQuantity = openingBalance > Long.MAX_VALUE - lifetimeUnits
                    ? Long.MAX_VALUE : openingBalance + lifetimeUnits;
            persistedFinishedGoods.put(item.getKey(),
                    (int) Math.min(Integer.MAX_VALUE, persistedQuantity));
        }
        return persistedFinishedGoods;
    }

    private void saveInventorySnapshot(Map<String, Integer> finishedSnapshot,
            Map<String, Integer> rawSnapshot) throws IOException {
        Path absoluteDataFile = DATA_FILE.toAbsolutePath();
        Path parent = absoluteDataFile.getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "inventory_data", ".tmp");
        boolean moved = false;
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                writer.write("# Tow Hitch by 5-21 inventory data. Do not edit while the program is running.");
                writer.newLine();
                for (Map.Entry<String, Integer> item : finishedSnapshot.entrySet()) {
                    writer.write("F\t" + item.getKey() + "\t" + item.getValue());
                    writer.newLine();
                }
                List<String> materialNames = new ArrayList<>(rawSnapshot.keySet());
                materialNames.sort(String.CASE_INSENSITIVE_ORDER);
                for (String materialName : materialNames) {
                    writer.write("R\t" + materialName + "\t" + rawSnapshot.get(materialName));
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

    private boolean containsItemIgnoreCase(Map<String, Integer> catalog, String candidate) {
        for (String itemName : catalog.keySet()) {
            if (itemName.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void enterStockToolbarMode() {
        stockFooterMode = StockFooterMode.TOOLBAR;
        showPage("Stocks");
        stockSaveStatus.setForeground(theme.secondaryText());
        stockSaveStatus.setText("Use a Stocks header icon to add, edit, or delete materials.");
        applicationRoot.repaint();
    }

    private void beginMaterialAddition() {
        MaterialDraft draft = new MaterialDraft();
        pendingMaterialDrafts.add(0, draft);
        attachStockTextUndo(draft.nameField);
        attachStockTextUndo(draft.quantityField);
        stockFooterMode = StockFooterMode.ADD;
        pushStockActionUndo(() -> {
            pendingMaterialDrafts.remove(draft);
            stockFooterMode = StockFooterMode.TOOLBAR;
            showPage("Stocks");
        });
        showPage("Stocks");
        stockSaveStatus.setForeground(theme.secondaryText());
        stockSaveStatus.setText("Enter the material name and quantity, then select Add.");
        draft.nameField.requestFocusInWindow();
        applicationRoot.repaint();
    }

    private boolean stageActiveMaterialAddition() {
        if (pendingMaterialDrafts.isEmpty()) {
            showCatalogError("There is no new material row to add.");
            return false;
        }
        MaterialDraft draft = pendingMaterialDrafts.get(0);
        String name = draft.nameField.getText().trim();
        if (name.isEmpty() || name.indexOf('\t') >= 0
                || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
            showCatalogError("Enter a material name without tabs or line breaks.");
            draft.nameField.requestFocusInWindow();
            return false;
        }
        if (containsItemIgnoreCase(rawMaterials, name)
                || containsItemIgnoreCase(stagedMaterialAdditions, name)) {
            showCatalogError("A material with the name \"" + name + "\" already exists.");
            draft.nameField.requestFocusInWindow();
            return false;
        }
        final int quantity;
        try {
            quantity = Integer.parseInt(draft.quantityField.getText().trim());
        } catch (NumberFormatException e) {
            showCatalogError("Enter a whole number from 0 to " + Integer.MAX_VALUE + ".");
            draft.quantityField.requestFocusInWindow();
            return false;
        }
        if (quantity < 0) {
            showCatalogError("Stock quantity cannot be negative.");
            draft.quantityField.requestFocusInWindow();
            return false;
        }

        stagedMaterialAdditions.put(name, quantity);
        pendingMaterialDrafts.remove(draft);
        stockFooterMode = StockFooterMode.TOOLBAR;
        pushStockActionUndo(() -> {
            stagedMaterialAdditions.remove(name);
            pendingMaterialDrafts.add(0, draft);
            stockFooterMode = StockFooterMode.ADD;
            showPage("Stocks");
        });
        return true;
    }

    private void attachStockTextUndo(JTextField field) {
        field.getDocument().addUndoableEditListener(stockTextUndoManager);
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void updateUndoAvailability() {
                if (stockUndoButton != null) {
                    stockUndoButton.setEnabled(stockTextUndoManager.canUndo()
                            || !stockActionUndoHistory.isEmpty());
                }
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                updateUndoAvailability();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateUndoAvailability();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateUndoAvailability();
            }
        });
    }

    private void pushStockActionUndo(Runnable undoAction) {
        stockActionUndoHistory.add(undoAction);
        if (stockUndoButton != null) {
            stockUndoButton.setEnabled(true);
        }
    }

    private void undoStockAction() {
        if (stockTextUndoManager.canUndo()) {
            stockTextUndoManager.undo();
        } else if (!stockActionUndoHistory.isEmpty()) {
            Runnable undoAction = stockActionUndoHistory.remove(stockActionUndoHistory.size() - 1);
            undoAction.run();
        }
        if (stockUndoButton != null) {
            stockUndoButton.setEnabled(stockTextUndoManager.canUndo()
                    || !stockActionUndoHistory.isEmpty());
        }
        applicationRoot.repaint();
    }

    private void beginMaterialDeletion() {
        stockFooterMode = StockFooterMode.DELETE;
        showPage("Stocks");
        stockSaveStatus.setForeground(theme.secondaryText());
        stockSaveStatus.setText("Select materials to delete, then select Save.");
        applicationRoot.repaint();
    }

    private void beginMaterialNameEdit() {
        if (materialNameEdits.isEmpty()) {
            for (String materialName : rawMaterials.keySet()) {
                JTextField nameField = new JTextField(materialName, 24);
                attachStockTextUndo(nameField);
                materialNameEdits.put(materialName, nameField);
            }
        }
        stockFooterMode = StockFooterMode.EDIT_NAMES;
        showPage("Stocks");
        stockSaveStatus.setForeground(theme.secondaryText());
        stockSaveStatus.setText("Edit material names, then select Save.");
        applicationRoot.repaint();
        if (!rawMaterials.isEmpty()) {
            JTextField firstNameField =
                    materialNameEdits.get(rawMaterials.keySet().iterator().next());
            SwingUtilities.invokeLater(() -> {
                firstNameField.requestFocusInWindow();
                firstNameField.setCaretPosition(firstNameField.getText().length());
            });
        }
    }

    private void cancelStockFooterMode() {
        pendingStockUpdates.clear();
        pendingMaterialDrafts.clear();
        stagedMaterialAdditions.clear();
        materialNameEdits.clear();
        selectedMaterialsForDeletion.clear();
        stockTextUndoManager.discardAllEdits();
        stockActionUndoHistory.clear();
        stockFooterMode = StockFooterMode.NORMAL;
        stockUndoButton = null;
        updateNotificationBadge();
        showPage("Stocks");
        stockSaveStatus.setForeground(theme.secondaryText());
        stockSaveStatus.setText(" ");
        applicationRoot.repaint();
    }

    private void addCatalogItem(boolean product) {
        Map<String, Integer> catalog = product ? finishedGoods : rawMaterials;
        if (!product && !pendingStockUpdates.isEmpty() && !resolvePendingStockUpdates()) {
            return;
        }
        String itemName = showAddCatalogItemDialog(product);
        if (itemName == null) {
            return;
        }
        itemName = itemName.trim();
        if (itemName.isEmpty() || itemName.indexOf('\t') >= 0
                || itemName.indexOf('\n') >= 0 || itemName.indexOf('\r') >= 0) {
            showCatalogError("Enter a name that does not contain tabs or line breaks.");
            return;
        }
        if (containsItemIgnoreCase(catalog, itemName)) {
            showCatalogError("An item with that name already exists.");
            return;
        }

        try {
            appendInventoryRecord(product ? "F" : "R", itemName, 0);
        } catch (IOException e) {
            showCatalogError("Unable to add the item to inventory_data.txt:\n" + e.getMessage());
            return;
        }
        catalog.put(itemName, 0);
        if (product) {
            billOfMaterials.putIfAbsent(itemName, Collections.emptyList());
            productExpansionMap.put(itemName, Boolean.FALSE);
        }
        updateNotificationBadge();
        showPage(product ? "Production" : "Stocks");
        applicationRoot.repaint();
    }

    private void deleteCatalogItem(boolean product) {
        Map<String, Integer> catalog = product ? finishedGoods : rawMaterials;
        if (catalog.isEmpty()) {
            showCatalogError(product ? "There are no products to delete."
                    : "There are no materials to delete.");
            return;
        }
        if (!product && !pendingStockUpdates.isEmpty() && !resolvePendingStockUpdates()) {
            return;
        }
        String itemName = showDeleteCatalogItemDialog(
                product ? "Delete Product" : "Delete Material", catalog);
        if (itemName == null || !catalog.containsKey(itemName)) {
            return;
        }

        Integer removedQuantity = catalog.remove(itemName);
        Integer removedPendingQuantity = pendingStockUpdates.remove(itemName);
        Boolean previousExpansion = product ? productExpansionMap.remove(itemName) : null;
        try {
            saveInventory();
        } catch (IOException e) {
            catalog.put(itemName, removedQuantity);
            if (removedPendingQuantity != null) {
                pendingStockUpdates.put(itemName, removedPendingQuantity);
            }
            if (previousExpansion != null) {
                productExpansionMap.put(itemName, previousExpansion);
            }
            showCatalogError("Unable to delete the item from inventory_data.txt:\n" + e.getMessage());
            return;
        }
        updateNotificationBadge();
        showPage(product ? "Production" : "Stocks");
        applicationRoot.repaint();
    }

    private String showAddCatalogItemDialog(boolean product) {
        String itemType = product ? "product" : "material";
        JDialog dialog = createThemedDialog(product ? "Add New Product" : "Add New Material");
        RoundedPanel card = createDialogCard();
        card.setLayout(new BorderLayout(0, 18));
        JLabel heading = new JLabel(product ? "Create a product profile" : "Add a raw material",
                SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(theme.text());
        card.add(heading, BorderLayout.NORTH);

        RoundedPanel fieldShell = new RoundedPanel(12);
        fieldShell.setBackground(theme.input());
        fieldShell.setLayout(new BorderLayout());
        RoundedTextField nameField = new RoundedTextField("Enter " + itemType + " name", 24);
        nameField.setPreferredSize(new Dimension(360, 46));
        fieldShell.add(nameField, BorderLayout.CENTER);
        card.add(fieldShell, BorderLayout.CENTER);

        String[] result = {null};
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        RoundedButton confirmButton = new RoundedButton(
                "Add", theme.primaryButton(), Color.WHITE, 9);
        confirmButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        confirmButton.setPreferredSize(new Dimension(108, 40));
        confirmButton.addActionListener(event -> {
            if (!nameField.getText().trim().isEmpty()) {
                result[0] = nameField.getText().trim();
                dialog.dispose();
            }
        });
        RoundedButton cancelButton = new RoundedButton(
                "Cancel", theme.input(), theme.text(), 9);
        cancelButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(108, 40));
        cancelButton.addActionListener(event -> dialog.dispose());
        nameField.addActionListener(event -> confirmButton.doClick());
        buttons.add(confirmButton);
        buttons.add(cancelButton);
        card.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(card);
        configureFocusablePanels(card);
        dialog.getRootPane().setDefaultButton(confirmButton);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(440, 190));
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        return result[0];
    }

    private String showDeleteCatalogItemDialog(String title, Map<String, Integer> catalog) {
        List<String> itemNames = new ArrayList<>(catalog.keySet());
        itemNames.sort(String.CASE_INSENSITIVE_ORDER);
        JDialog dialog = createThemedDialog(title);
        RoundedPanel card = createDialogCard();
        card.setLayout(new BorderLayout(0, 18));
        JLabel heading = new JLabel("Select an item to remove", SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(theme.text());
        card.add(heading, BorderLayout.NORTH);

        JComboBox<String> itemSelector = new JComboBox<>(itemNames.toArray(new String[0]));
        itemSelector.setFont(BODY_FONT);
        itemSelector.setForeground(theme.text());
        itemSelector.setBackground(theme.input());
        card.add(itemSelector, BorderLayout.CENTER);
        String[] result = {null};
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        RoundedButton deleteButton = new RoundedButton(
                "Delete", theme.danger(), Color.WHITE, 9);
        deleteButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        deleteButton.setPreferredSize(new Dimension(108, 40));
        deleteButton.addActionListener(event -> {
            result[0] = (String) itemSelector.getSelectedItem();
            dialog.dispose();
        });
        RoundedButton cancelButton = new RoundedButton(
                "Cancel", theme.input(), theme.text(), 9);
        cancelButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(108, 40));
        cancelButton.addActionListener(event -> dialog.dispose());
        buttons.add(deleteButton);
        buttons.add(cancelButton);
        card.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(card);
        configureFocusablePanels(card);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(400, 180));
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        return result[0];
    }

    private void showCatalogError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Inventory Management Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private void appendInventoryRecord(String type, String itemName, int quantity)
            throws IOException {
        Path absoluteDataFile = DATA_FILE.toAbsolutePath();
        Path parent = absoluteDataFile.getParent();
        Files.createDirectories(parent);
        if (Files.notExists(absoluteDataFile)) {
            Files.createFile(absoluteDataFile);
        }
        long fileSize = Files.size(absoluteDataFile);
        boolean needsLineBreak = false;
        if (fileSize > 0) {
            try (SeekableByteChannel channel = Files.newByteChannel(absoluteDataFile,
                    StandardOpenOption.READ)) {
                channel.position(fileSize - 1);
                ByteBuffer lastByte = ByteBuffer.allocate(1);
                channel.read(lastByte);
                lastByte.flip();
                needsLineBreak = lastByte.get() != (byte) '\n';
            }
        }
        String record = (needsLineBreak ? System.lineSeparator() : "")
                + type + "\t" + itemName + "\t" + quantity + System.lineSeparator();
        Files.write(absoluteDataFile, record.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE, StandardOpenOption.APPEND);
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
                Insets margin = getMargin();
                int leftPadding = getInsets().left + (margin == null ? 0 : margin.left);
                placeholderGraphics.drawString(placeholder, leftPadding,
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

    private enum StockFooterMode {
        NORMAL,
        TOOLBAR,
        ADD,
        DELETE,
        EDIT_NAMES
    }

    private enum ProductCatalogMode {
        NORMAL,
        TOOLBAR,
        ADD,
        DELETE,
        EDIT_NAMES
    }

    private enum StockHeaderAction {
        ADD,
        EDIT,
        DELETE
    }

    private final class StockActionIconButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final StockHeaderAction action;

        private StockActionIconButton(StockHeaderAction action) {
            this.action = action;
            setToolTipText(null);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(28, 28));
            setMinimumSize(new Dimension(28, 28));
            setMaximumSize(new Dimension(28, 28));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getModel().isRollover() ? theme.primaryButton() : theme.secondaryText());
            g.setStroke(new java.awt.BasicStroke(1.8f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            if (action == StockHeaderAction.ADD) {
                g.drawLine(centerX - 6, centerY, centerX + 6, centerY);
                g.drawLine(centerX, centerY - 6, centerX, centerY + 6);
            } else if (action == StockHeaderAction.EDIT) {
                Graphics2D pencil = (Graphics2D) g.create();
                pencil.translate(centerX, centerY);
                pencil.rotate(Math.PI / 4.0);
                pencil.setColor(new Color(0x5B5753));
                java.awt.geom.Path2D.Double pencilShape = new java.awt.geom.Path2D.Double();
                pencilShape.moveTo(-2.5, -7);
                pencilShape.lineTo(2.5, -7);
                pencilShape.lineTo(2.5, 6);
                pencilShape.lineTo(0, 10);
                pencilShape.lineTo(-2.5, 6);
                pencilShape.closePath();
                pencil.fill(pencilShape);
                pencil.fillRect(-2, -10, 4, 3);
                pencil.dispose();
            } else {
                g.drawLine(centerX - 7, centerY - 6, centerX + 7, centerY - 6);
                g.drawLine(centerX - 3, centerY - 9, centerX + 3, centerY - 9);
                g.drawLine(centerX - 5, centerY - 4, centerX - 4, centerY + 8);
                g.drawLine(centerX + 5, centerY - 4, centerX + 4, centerY + 8);
                g.drawLine(centerX - 4, centerY + 8, centerX + 4, centerY + 8);
                g.drawLine(centerX - 2, centerY - 2, centerX - 2, centerY + 5);
                g.drawLine(centerX + 2, centerY - 2, centerX + 2, centerY + 5);
            }
            g.dispose();
        }
    }

    private final class MaterialDraft {
        private final JTextField nameField = new JTextField("", 24);
        private final JTextField quantityField = new JTextField("0", 5);
    }

    private final class ProductDraft {
        private final JTextField nameField = new JTextField("", 24);
    }

    private final class SelectionCircleButton extends JToggleButton {
        private static final long serialVersionUID = 1L;

        private SelectionCircleButton() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(24, 24));
            setMinimumSize(new Dimension(24, 24));
            setMaximumSize(new Dimension(24, 24));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int diameter = Math.min(getWidth(), getHeight()) - 10;
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;
            g.setColor(isSelected() ? theme.primaryButton() : theme.secondaryText());
            g.setStroke(new java.awt.BasicStroke(1.5f));
            if (isSelected()) {
                g.fillOval(x, y, diameter, diameter);
                g.setColor(Color.WHITE);
                g.setStroke(new java.awt.BasicStroke(1.8f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g.drawLine(x + 5, y + diameter / 2, x + diameter / 2 - 1, y + diameter - 6);
                g.drawLine(x + diameter / 2 - 1, y + diameter - 6,
                        x + diameter - 4, y + 5);
            } else {
                g.drawOval(x, y, diameter, diameter);
            }
            g.dispose();
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

    private static final class ProductionBatch {
        private final Instant producedAt;
        private final String productName;
        private final int quantity;

        private ProductionBatch(Instant producedAt, String productName, int quantity) {
            this.producedAt = producedAt;
            this.productName = productName;
            this.quantity = quantity;
        }
    }

}
