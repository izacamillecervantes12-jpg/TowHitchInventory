import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class OwnerApp {
    private static final int[] UNFINISHED_COLUMNS = {120, -1, 100, 100, 160};
    private static final int[] REQUEST_COLUMNS = {120, -1, 90, 160, 160, 210};
    private static final int[] ORDER_COLUMNS = {120, -1, 80, 160, 160, 230};
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
    private JFrame frame;
    private JPanel pageHost;
    private JPanel applicationRoot;
    private JPanel sidebarPanel;
    private TowHitchCore.FlatIconButton headerMenuButton;
    private JPanel topBar;
    private javax.swing.Timer sidebarAnimation;
    private int sidebarWidth = 255;
    private TowHitchCore.UserAccount activeUser;
    private LoginBackgroundPanel loginBackground;
    private JLabel loginTitle;
    private JLabel loginSubtitle;
    private JLabel loginMessage;
    private JLabel loginFooter;
    private javax.swing.JWindow notificationPopup;
    private javax.swing.JWindow profilePopup;
    private TowHitchCore.AnchoredPopover anchoredPopover;
    private JPopupMenu settingsPopup;
    private TowHitchCore.FlatIconButton notificationButton;
    private final Set<String> readNotificationIds = new HashSet<>();
    private String currentPage = "Dashboard";
    private String searchHighlightName = "";
    private boolean sidebarExpanded = true;
    private boolean historyExpanded;
    private boolean compactView;
    private boolean notificationAlerts = true;
    private boolean autoRefreshInventory = true;
    private boolean stockRequestSelectionMode;
    private final Set<String> selectedStockRequestIds = new HashSet<>();
    private final String[] navigationItems = {
            "Dashboard",
            "Raw Materials",
            "Finished Goods",
            "Stock Requests",
            "Customer Orders",
            "Crew Accounts",
            "Product Logs",
            "Stock Order Logs",
            "Material Consumption",
            "Trash"
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OwnerApp().start());
    }

    private void start() {
        try {
            TowHitchCore.ensureUsersFile();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to initialize users.txt: " + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        frame = new JFrame("Owner Portal | Tow Hitch by 5-21");
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
        root.setFocusable(true);
        loginBackground = root;

        GlassLoginCard card = new GlassLoginCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(38, 36, 36, 36));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        JLabel badge = new JLabel(loginLogo);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(64, 64));
        badge.setMaximumSize(new Dimension(64, 64));
        badge.setOpaque(false);
        addLoginGridRow(content, badge, 0, 0, 12, 0, GridBagConstraints.CENTER);

        loginTitle = new JLabel("Welcome, Admin", SwingConstants.CENTER);
        loginTitle.setFont(new Font("SansSerif", Font.BOLD, 34));
        loginTitle.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        loginTitle.setPreferredSize(new Dimension(394, 41));
        loginTitle.setMaximumSize(new Dimension(394, 41));
        addLoginGridRow(content, loginTitle, 1, 0, 6, 0, GridBagConstraints.CENTER);

        loginSubtitle = new JLabel(
                "Manage inventory, approvals, and shop operations.", SwingConstants.CENTER);
        loginSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        loginSubtitle.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x625E5C));
        loginSubtitle.setPreferredSize(new Dimension(394, 25));
        loginSubtitle.setMaximumSize(new Dimension(394, 25));
        addLoginGridRow(content, loginSubtitle, 2, 0, 26, 0, GridBagConstraints.CENTER);

        JLabel usernameLabel = loginLabel("Username");
        LoginTextField username = new LoginTextField("Enter your username");
        JLabel passwordLabel = loginLabel("Password");
        LoginPasswordField password = new LoginPasswordField("Enter password");
        addLoginGridRow(content, usernameLabel, 3, 0, 6, 0, GridBagConstraints.WEST);
        addLoginGridRow(content, username, 4, 0, 16, 0, GridBagConstraints.WEST);
        addLoginGridRow(content, passwordLabel, 5, 0, 6, 0, GridBagConstraints.WEST);
        addLoginGridRow(content, password, 6, 0, 26, 0, GridBagConstraints.WEST);

        loginMessage = new JLabel(" ");
        loginMessage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        loginMessage.setForeground(theme.danger());
        loginMessage.setVisible(false);
        loginMessage.setPreferredSize(new Dimension(394, 25));
        loginMessage.setMaximumSize(new Dimension(394, 25));
        addLoginGridRow(content, loginMessage, 7, 0, 0, 0, GridBagConstraints.WEST);

        LoginButton signIn = new LoginButton("Log in");
        signIn.setMaximumSize(new Dimension(395, 44));
        signIn.setPreferredSize(new Dimension(395, 44));
        signIn.setMinimumSize(new Dimension(395, 44));
        signIn.addActionListener(event -> authenticateOwner(username, password));
        password.addActionListener(event -> authenticateOwner(username, password));
        addLoginGridRow(content, signIn, 8, 0, 0, 0, GridBagConstraints.WEST);

        Dimension contentSize = content.getPreferredSize();
        contentSize.width = 395;
        content.setPreferredSize(contentSize);
        content.setMaximumSize(contentSize);
        loginFooter = new JLabel("Tow Hitch by 5-21 | Owner Portal", SwingConstants.CENTER);
        loginFooter.setFont(new Font("SansSerif", Font.PLAIN, 16));
        loginFooter.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x686361));
        loginFooter.setHorizontalAlignment(SwingConstants.CENTER);
        loginFooter.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        loginFooter.setPreferredSize(new Dimension(395, 42));
        loginFooter.setMaximumSize(new Dimension(395, 42));

        JPanel centeredContent = centerLoginContent(content);
        card.add(centeredContent);
        card.add(Box.createVerticalStrut(32));
        card.add(loginFooter);
        card.setPreferredSize(new Dimension(468,
                centeredContent.getPreferredSize().height + 32 + 42 + 38 + 36));
        root.setLoginComponents(card, new ThemeSwitchButton());
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

    private JLabel loginLabel(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setFont(new Font("SansSerif", Font.BOLD, 21));
        label.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        label.setPreferredSize(new Dimension(395, 27));
        label.setMinimumSize(new Dimension(395, 27));
        label.setMaximumSize(new Dimension(395, 27));
        return label;
    }

    private void addLoginGridRow(JPanel panel, Component component, int row,
                                 int topInset, int bottomInset, int verticalPadding,
                                 int anchor) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        boolean input = component instanceof JTextField || component instanceof JPasswordField
                || component instanceof JButton;
        constraints.weightx = input ? 0.0 : 1.0;
        constraints.weighty = 0.0;
        constraints.fill = input ? GridBagConstraints.NONE : verticalPadding > 0
                ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        boolean formLabel = component instanceof JLabel
                && ((JLabel) component).getHorizontalAlignment() == SwingConstants.LEFT;
        if (formLabel) {
            constraints.weightx = 0.0;
            constraints.fill = GridBagConstraints.NONE;
        }
        constraints.anchor = input || formLabel ? GridBagConstraints.WEST : anchor;
        constraints.insets = new Insets(topInset, 0, bottomInset, 0);
        constraints.ipady = verticalPadding;
        panel.add(component, constraints);
    }

    private void authenticateOwner(JTextField username, JPasswordField password) {
        String enteredUsername = username.getText().trim();
        String enteredPassword = new String(password.getPassword());
        try {
            TowHitchCore.UserAccount found = null;
            for (TowHitchCore.UserAccount account : TowHitchCore.loadUsers()) {
                if (account.username.equals(enteredUsername)
                        && account.password.equals(enteredPassword)) {
                    found = account;
                    break;
                }
            }
            if (found == null) {
                showLoginMessage("Invalid username or password.");
            } else if (!"OWNER".equals(found.role)) {
                showLoginMessage(
                        "Access denied: This portal is restricted to Owner accounts only.");
            } else {
                activeUser = found;
                buildApplication();
            }
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

    private void refreshLoginTheme() {
        if (loginBackground == null) {
            return;
        }
        loginTitle.setForeground(theme.isDark() ? new Color(0xF5F1ED) : new Color(0x3A3534));
        loginSubtitle.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x625E5C));
        loginFooter.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x686361));
        loginMessage.setForeground(theme.danger());
        updateLoginLabelColors(loginBackground);
        loginBackground.repaint();
    }

    private void updateLoginLabelColors(Component component) {
        if (component instanceof JLabel && component != loginTitle
                && component != loginSubtitle && component != loginFooter
                && component != loginMessage) {
            component.setForeground(theme.isDark()
                    ? new Color(0xF5F1ED) : new Color(0x3A3534));
        }
        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) {
                updateLoginLabelColors(child);
            }
        }
    }

    @SuppressWarnings("this-escape")
    private class LoginBackgroundPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int CARD_WIDTH = 468;
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
                Color start = theme.isDark()
                        ? new Color(0x1F1C1A) : new Color(0xF2E3CE);
                Color end = theme.isDark()
                        ? new Color(0x4A2E2B) : new Color(0xD9C5B2);
                g.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.dispose();
        }
    }

    @SuppressWarnings("this-escape")
    private class GlassLoginCard extends JPanel {
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

    @SuppressWarnings("this-escape")
    private class LoginTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        private LoginTextField(String placeholder) {
            this.placeholder = placeholder;
            TowHitchCore.configureTextField(this);
            setFont(new Font("SansSerif", Font.PLAIN, 14));
            setOpaque(false);
            setAlignmentX(JComponent.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 16));
            setPreferredSize(new Dimension(395, 42));
            setMinimumSize(new Dimension(395, 42));
            setMaximumSize(new Dimension(395, 42));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fieldColor = theme.isDark()
                    ? new Color(0xB6B1AA) : new Color(0xF4EFE6);
            g.setColor(fieldColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.dispose();
            setForeground(new Color(0x242221));
            setCaretColor(new Color(0x242221));
            super.paintComponent(graphics);
            if (getText().isEmpty()) {
                paintLoginPlaceholder(graphics, placeholder, this);
            }
        }
    }

    @SuppressWarnings("this-escape")
    private class LoginPasswordField extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        private LoginPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setFont(new Font("SansSerif", Font.PLAIN, 14));
            setOpaque(false);
            setAlignmentX(JComponent.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 16));
            setPreferredSize(new Dimension(395, 42));
            setMinimumSize(new Dimension(395, 42));
            setMaximumSize(new Dimension(395, 42));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fieldColor = theme.isDark()
                    ? new Color(0xB6B1AA) : new Color(0xF4EFE6);
            g.setColor(fieldColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.dispose();
            setForeground(new Color(0x242221));
            setCaretColor(new Color(0x242221));
            super.paintComponent(graphics);
            if (getPassword().length == 0) {
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

    @SuppressWarnings("this-escape")
    private class LoginButton extends TowHitchCore.RoundedButton {
        private static final long serialVersionUID = 1L;

        private LoginButton(String text) {
            super(text, theme.primaryButton(), Color.WHITE, 12, theme);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }

    @SuppressWarnings("this-escape")
    private class ThemeSwitchButton extends JButton {
        private static final long serialVersionUID = 1L;
        private float position = theme.isDark() ? 1.0f : 0.0f;
        private Timer animation;

        private ThemeSwitchButton() {
            setPreferredSize(new Dimension(60, 30));
            setMinimumSize(new Dimension(60, 30));
            setMaximumSize(new Dimension(60, 30));
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
            float start = position;
            float target = theme.isDark() ? 0.0f : 1.0f;
            int[] step = {0};
            animation = new Timer(15, event -> {
                step[0]++;
                float fraction = Math.min(1.0f, step[0] / 10.0f);
                position = start + (target - start) * fraction;
                repaint();
                if (fraction >= 1.0f) {
                    animation.stop();
                    theme.toggle();
                    if (activeUser == null) {
                        refreshLoginTheme();
                    } else {
                        buildApplication();
                    }
                }
            });
            animation.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            TowHitchCore.paintThemeSwitch(g, getWidth(), getHeight(), position,
                    theme.isDark());
            g.dispose();
        }
    }

    private void showSettingsPopup(Component invoker) {
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
            addProfileDetail(card, "Role", "Owner");
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
            TowHitchCore.PopoverMenuButton back = profileMenuButton(
                    "\u2190 Display & Accessibility", () -> showProfilePopover(anchor, "main"));
            card.add(back);
            card.add(Box.createVerticalStrut(12));
            JLabel title = new JLabel("Dark Mode");
            title.setFont(new Font("SansSerif", Font.BOLD, 14));
            title.setForeground(theme.text());
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
            JLabel name = new JLabel(activeUser.fullName);
            name.setFont(new Font("SansSerif", Font.BOLD, 13));
            name.setForeground(theme.text());
            JLabel username = new JLabel("Owner \u2022 @" + activeUser.username);
            username.setFont(new Font("SansSerif", Font.PLAIN, 12));
            username.setForeground(theme.secondaryText());
            labels.add(name);
            labels.add(Box.createVerticalStrut(3));
            labels.add(username);
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
        JLabel label = new JLabel(key);
        label.setForeground(theme.secondaryText());
        Component detail;
        if ("Role".equals(key)) {
            TowHitchCore.RoundedPanel badge = new TowHitchCore.RoundedPanel(10);
            badge.setBackground(theme.isDark() ? new Color(0x34312F) : new Color(0xEDE7DD));
            badge.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
            JLabel role = new JLabel(value.toUpperCase(Locale.ROOT));
            role.setFont(new Font("SansSerif", Font.BOLD, 10));
            role.setForeground(theme.text());
            badge.add(role);
            detail = badge;
        } else {
            JLabel text = new JLabel(value);
            text.setForeground(theme.text());
            detail = text;
        }
        row.add(label, BorderLayout.WEST);
        row.add(detail, BorderLayout.EAST);
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
            notificationButton.setUnreadCount(enabled ? getUnreadNotificationCount() : 0);
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
            showLoadError("the account password", e);
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
        Object[] options = {"Data Backup", "Close"};
        int selected = JOptionPane.showOptionDialog(frame,
                "Display name: " + activeUser.fullName + "\nUsername: " + activeUser.username
                        + "\nRole: Administrator",
                "Settings", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[1]);
        if (selected == 0) {
            createBackup();
        }
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

    private JLabel popupMessage(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(theme.secondaryText());
        label.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        return label;
    }

    private List<TowHitchCore.SystemNotification> loadNotifications() {
        try {
            return TowHitchCore.loadSystemNotifications("OWNER", activeUser.fullName);
        } catch (IOException e) {
            showLoadError("system notifications", e);
            return new ArrayList<>();
        }
    }

    private int countUnreadNotifications(List<TowHitchCore.SystemNotification> feed) {
        int unread = 0;
        for (TowHitchCore.SystemNotification notification : feed) {
            if (!readNotificationIds.contains(notificationReadKey(notification))) {
                unread++;
            }
        }
        return unread;
    }

    private int getUnreadNotificationCount() {
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

    private void showNotificationsPopup(Component invoker) {
        if (notificationPopup != null && notificationPopup.isVisible()) {
            anchoredPopover.close();
            return;
        }
        javax.swing.JWindow popup = new javax.swing.JWindow(frame);
        popup.setBackground(new Color(0, 0, 0, 0));
        JPanel content = popupContent();
        List<TowHitchCore.SystemNotification> feed = loadNotifications();
        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        JLabel headingText = new JLabel("Notifications");
        headingText.setFont(new Font("SansSerif", Font.BOLD, 15));
        headingText.setForeground(theme.text());
        JButton markAll = notificationLink("Mark all as read");
        heading.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        heading.add(headingText, BorderLayout.WEST);
        heading.add(markAll, BorderLayout.EAST);
        content.add(heading);

        JPanel items = new JPanel();
        items.setOpaque(false);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
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
            JLabel dot = new JLabel("\u25CF");
            dot.setFont(new Font("SansSerif", Font.BOLD, 8));
            String notificationHtml = notification.text.startsWith("<html>")
                    ? "<html><div style='width:310px'>"
                            + notification.text.substring(6, notification.text.length() - 7)
                            + "</div></html>" : notification.text;
            JLabel label = new JLabel(notificationHtml);
            label.setOpaque(false);
            label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            card.add(dot, BorderLayout.WEST);
            card.add(label, BorderLayout.CENTER);
            styleNotificationCard(notification, card, dot, label);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            label.setToolTipText(notification.text);
            MouseAdapter click = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    readNotificationIds.add(notificationReadKey(notification));
                    styleNotificationCard(notification, card, dot, label);
                    notificationButton.setUnreadCount(notificationAlerts
                            ? countUnreadNotifications(feed) : 0);
                    if (notification.id.startsWith("crew:")) {
                        anchoredPopover.close();
                        currentPage = "Crew Accounts";
                        buildApplication();
                    }
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
        if (feed.isEmpty()) {
            items.add(popupMessage("You're all caught up."));
        }
        markAll.addActionListener(event -> {
            for (TowHitchCore.SystemNotification notification : feed) {
                readNotificationIds.add(notificationReadKey(notification));
            }
            for (int i = 0; i < feed.size(); i++) {
                styleNotificationCard(feed.get(i), cards.get(i), dots.get(i), labels.get(i));
            }
            notificationButton.setUnreadCount(0);
        });
        JScrollPane scroll = createInventoryScroll(items);
        scroll.setPreferredSize(new Dimension(390, Math.min(360,
                Math.max(90, feed.size() * 62 + 20))));
        content.add(scroll);
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

    private JPanel popupContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        return content;
    }

    private void stylePopupItem(JMenuItem item) {
        item.setOpaque(false);
        item.setBackground(theme.dashboardPanel());
        item.setForeground(theme.text());
        item.setFont(new Font("SansSerif", Font.PLAIN, 13));
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private class RoundedNotificationPopup extends JPopupMenu {
        private static final long serialVersionUID = 1L;

        private RoundedNotificationPopup() {
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
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

    @SuppressWarnings("this-escape")
    private class GradientPanel extends JPanel {
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

    @SuppressWarnings("this-escape")
    private class RoundedSearchPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int CORNER_RADIUS = 12;

        private RoundedSearchPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.isDark() ? new Color(0x24272E) : new Color(0xFBF9F5));
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3,
                    CORNER_RADIUS, CORNER_RADIUS);
            g.setColor(theme.isDark() ? new Color(0x383C45) : new Color(0xE2DDD3));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3,
                    CORNER_RADIUS, CORNER_RADIUS);
            g.dispose();
        }
    }

    @SuppressWarnings("this-escape")
    private class SearchGlass extends JComponent {
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
            g.setColor(theme.secondaryText());
            g.setStroke(new java.awt.BasicStroke(1.8f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            int x = (getWidth() - 15) / 2;
            int y = (getHeight() - 15) / 2;
            g.drawOval(x, y, 10, 10);
            g.drawLine(x + 8, y + 8, x + 14, y + 14);
            g.dispose();
        }
    }

    @SuppressWarnings("this-escape")
    private class SearchDivider extends JComponent {
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

    private void buildApplication() {
        applicationRoot = new GradientPanel();
        applicationRoot.setLayout(new BorderLayout());
        applicationRoot.setBorder(BorderFactory.createEmptyBorder());

        sidebarPanel = buildSidebar();
        sidebarPanel.setPreferredSize(new Dimension(sidebarExpanded ? sidebarWidth : 0, 100));
        sidebarPanel.setVisible(sidebarExpanded || sidebarWidth > 0);
        pageHost = new JPanel(new CardLayout());
        pageHost.setOpaque(false);
        for (String page : navigationItems) {
            JPanel pagePanel;
            if ("Dashboard".equals(page)) {
                pagePanel = buildDashboard();
            } else if ("Raw Materials".equals(page)) {
                pagePanel = buildUnfinishedGoodsPage();
            } else if ("Finished Goods".equals(page)) {
                pagePanel = buildFinishedGoodsPage();
            } else if ("Stock Requests".equals(page)) {
                pagePanel = buildStockRequestsPage();
            } else if ("Customer Orders".equals(page)) {
                pagePanel = buildCustomerOrdersPage();
            } else if ("Crew Accounts".equals(page)) {
                pagePanel = buildCrewAccountsPage();
            } else if ("Product Logs".equals(page)
                    || "Stock Order Logs".equals(page)
                    || "Material Consumption".equals(page)) {
                pagePanel = buildHistoryPage("", page);
            } else if ("Trash".equals(page)) {
                pagePanel = buildTrashPage();
            } else {
                pagePanel = buildPlaceholderPage(page);
            }
            pageHost.add(pagePanel, page);
        }
        applicationRoot.add(sidebarPanel, BorderLayout.WEST);
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
        applicationRoot.add(content, BorderLayout.CENTER);
        frame.setContentPane(applicationRoot);
        showPage(currentPage);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel buildTopBar() {
        TowHitchCore.RoundedPanel top = new TowHitchCore.RoundedPanel(0);
        top.setLayout(new GridBagLayout());
        top.setBackground(theme.panel());
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
        search.setText(searchHighlightName);
        search.addActionListener(event -> performInventorySearch(search.getText().trim()));
        searchPanel.add(buildSearchLeading(), BorderLayout.WEST);
        search.setPreferredSize(new Dimension(220, 32));
        search.setMinimumSize(new Dimension(100, 32));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        searchPanel.add(search, BorderLayout.CENTER);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        top.add(searchPanel, constraints);

        notificationButton = new TowHitchCore.FlatIconButton(
                TowHitchCore.loadTintedIcon(notificationIcon, 18, theme.text()),
                "Notifications", theme);
        notificationButton.setIconButtonSize(24);
        notificationButton.setUnreadCount(notificationAlerts ? getUnreadNotificationCount() : 0);
        notificationButton.addActionListener(event -> showNotificationsPopup(notificationButton));
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

    private JPanel buildSearchLeading() {
        JPanel leading = new JPanel(new GridBagLayout());
        leading.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 0, 0, 10);
        leading.add(new SearchGlass(), constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        leading.add(new SearchDivider(), constraints);
        return leading;
    }

    private JPanel buildSidebar() {
        TowHitchCore.RoundedPanel sidebar = new TowHitchCore.RoundedPanel(0);
        sidebar.setPreferredSize(new Dimension(255, 100));
        sidebar.setLayout(new BorderLayout(0, 0));
        sidebar.setBackground(theme.sidebar());
        sidebar.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BorderLayout());
        ImageIcon sidebarLogo = theme.isDark() ? sidebarLogoDark : sidebarLogoLight;
        JPanel logoHero = new JPanel(new GridBagLayout());
        logoHero.setOpaque(false);
        logoHero.setBorder(BorderFactory.createEmptyBorder(36, 16, 36, 16));
        logoHero.setPreferredSize(new Dimension(255, 202));
        logoHero.setMinimumSize(new Dimension(255, 202));
        logoHero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 202));
        if (sidebarLogo != null) {
            JLabel logo = new JLabel(TowHitchCore.loadScaledImageAssetFit(
                    theme.isDark() ? "sidebar_logo_dark.png" : "sidebar_logo_light.png",
                    210, 130));
            logo.setHorizontalAlignment(SwingConstants.CENTER);
            logoHero.add(logo);
        }
        top.add(logoHero, BorderLayout.NORTH);
        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String item : navigationItems) {
            if ("Product Logs".equals(item)) {
                addNavigationButton(navigation, "History / Archive",
                        isHistoryPage(currentPage), historyIcon, 0, () -> {
                            historyExpanded = !historyExpanded;
                            buildApplication();
                        });
                if (historyExpanded) {
                    addNavigationButton(navigation, item, item.equals(currentPage), historyIcon,
                            18, () -> navigate(item));
                }
                continue;
            }
            if ("Stock Order Logs".equals(item) || "Material Consumption".equals(item)) {
                if (historyExpanded) {
                    addNavigationButton(navigation, item, item.equals(currentPage), historyIcon,
                            18, () -> navigate(item));
                }
                continue;
            }
            boolean selected = item.equals(currentPage);
            Color foreground = selected ? Color.WHITE : theme.secondaryText();
            TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(item,
                    selected ? theme.primaryButton() : theme.sidebar(), foreground, 12, theme);
            button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 14));
            button.setPreferredSize(new Dimension(231, 42));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            button.setMargin(new Insets(6, 14, 6, 14));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            if ("Crew Accounts".equals(item)) {
                button.setIcon(new UserGroupIcon(foreground));
                button.setIconTextGap(10);
            } else {
                ImageIcon icon = navigationIcon(item);
                if (icon != null) {
                button.setIcon(TowHitchCore.loadTintedIcon(icon, 20, foreground));
                button.setIconTextGap(10);
                }
            }
            button.addActionListener(event -> navigate(item));
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(6));
        }
        navigation.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(navigation, BorderLayout.CENTER);
        sidebar.add(top, BorderLayout.CENTER);

        JPanel logoutArea = new JPanel(new GridBagLayout());
        logoutArea.setOpaque(false);
        TowHitchCore.RoundedButton logout = new TowHitchCore.RoundedButton(
                "Logout", theme.sidebar(), theme.secondaryText(), 12, theme);
        logout.setFont(new Font("SansSerif", Font.PLAIN, 14));
        logout.setPreferredSize(new Dimension(184, 42));
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        logout.setMargin(new Insets(6, 14, 6, 14));
        logout.setIcon(TowHitchCore.loadTintedIcon("logout_icon.png", 18,
                theme.isDark() ? new Color(0xD1D5DB) : new Color(0x374151)));
        logout.setIconTextGap(7);
        logout.setHorizontalAlignment(SwingConstants.CENTER);
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.addActionListener(event -> logout());
        GridBagConstraints logoutConstraints = new GridBagConstraints();
        logoutConstraints.anchor = GridBagConstraints.CENTER;
        logoutArea.add(logout, logoutConstraints);
        sidebar.add(logoutArea, BorderLayout.SOUTH);
        return sidebar;
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

    private void navigate(String page) {
        currentPage = page;
        if (!isHistoryPage(page)) {
            historyExpanded = false;
        }
        buildApplication();
    }

    private void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        if (sidebarPanel == null) {
            return;
        }
        sidebarPanel.setVisible(true);
        if (sidebarAnimation != null) {
            sidebarAnimation.stop();
        }
        int target = sidebarExpanded ? 255 : 0;
        topBar.revalidate();
        topBar.repaint();
        sidebarAnimation = new javax.swing.Timer(15, event -> {
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

    private ImageIcon navigationIcon(String page) {
        if ("Dashboard".equals(page)) {
            return dashboardIcon;
        }
        if ("History / Archive".equals(page)) {
            return historyIcon;
        }
        if ("Stock Requests".equals(page) || "Customer Orders".equals(page)) {
            return productionIcon;
        }
        return inventoryIcon;
    }

    private static final class UserGroupIcon implements javax.swing.Icon {
        private final Color color;

        private UserGroupIcon(Color color) {
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(color);
            g.setStroke(new java.awt.BasicStroke(1.7f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g.drawOval(x + 7, y + 2, 6, 6);
            g.drawOval(x + 1, y + 6, 5, 5);
            g.drawOval(x + 14, y + 6, 5, 5);
            g.drawArc(x + 4, y + 9, 12, 9, 0, 180);
            g.drawArc(x, y + 11, 8, 7, 0, 150);
            g.drawArc(x + 12, y + 11, 8, 7, 30, 150);
            g.dispose();
        }
    }

    private void showPage(String page) {
        currentPage = page;
        CardLayout layout = (CardLayout) pageHost.getLayout();
        layout.show(pageHost, page);
    }

    private JPanel buildDashboard() {
        JPanel dashboard = new JPanel(new BorderLayout(0, 16));
        dashboard.setOpaque(false);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Owner Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(theme.text());
        heading.add(title);
        heading.add(pageSubtitle(
                "Real-time overview of inventory levels, stock alerts, and active shop transactions."));
        dashboard.add(heading, BorderLayout.NORTH);

        List<TowHitchCore.InventoryItem> inventory = loadInventorySafely();
        int unfinishedTypes = 0;
        int unfinishedLow = 0;
        int finishedUnits = 0;
        int finishedLow = 0;
        for (TowHitchCore.InventoryItem item : inventory) {
            if ("F".equals(item.type)) {
                finishedUnits += item.quantity;
                if (item.quantity <= item.minAlert) {
                    finishedLow++;
                }
            } else if ("U".equals(item.type) || "R".equals(item.type)) {
                unfinishedTypes++;
                if (item.quantity <= item.minAlert) {
                    unfinishedLow++;
                }
            }
        }

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(summaryCard("Raw Materials",
                unfinishedTypes + " types", unfinishedLow + " low stock"));
        cards.add(summaryCard("Finished Goods",
                finishedUnits + " units", finishedLow + " low stock"));
        cards.add(summaryCard("Pending Stock Requests",
                countPendingStockRequests() + " pending", "Stock workflow"));
        cards.add(summaryCard("Pending Customer Orders",
                countPendingCustomerOrders() + " pending", "Customer workflow"));
        center.add(cards, BorderLayout.NORTH);
        center.add(buildLowStockPanel(inventory), BorderLayout.CENTER);
        dashboard.add(center, BorderLayout.CENTER);
        return dashboard;
    }

    private JPanel summaryCard(String title, String primary, String secondary) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(18);
        card.setBackground(theme.panel());
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(theme.secondaryText());
        JLabel primaryLabel = new JLabel(primary);
        primaryLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        primaryLabel.setForeground(theme.text());
        JLabel secondaryLabel = new JLabel(secondary);
        secondaryLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        secondaryLabel.setForeground(theme.danger());

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(primaryLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(secondaryLabel);
        return card;
    }

    private JPanel buildLowStockPanel(List<TowHitchCore.InventoryItem> inventory) {
        TowHitchCore.RoundedPanel panel = new TowHitchCore.RoundedPanel(18);
        panel.setBackground(theme.panel());
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("\u26A0  Low Stocks Alert");
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(theme.text());
        JLabel subtitle = new JLabel(
                "Items at or below their configured minimum alert level");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(theme.secondaryText());
        header.add(heading);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        panel.add(header, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        int rowCount = 0;
        for (TowHitchCore.InventoryItem item : inventory) {
            if (item.quantity <= item.minAlert) {
                rows.add(buildAlertRow(item));
                rows.add(Box.createVerticalStrut(8));
                rowCount++;
            }
        }
        if (rowCount == 0) {
            JLabel clear = new JLabel("No low-stock items.");
            clear.setForeground(theme.success());
            clear.setBorder(BorderFactory.createEmptyBorder(14, 4, 14, 4));
            rows.add(clear);
        }

        JScrollPane scroll = new JScrollPane(rows);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        TowHitchCore.configureSmoothScroll(scroll);
        scroll.getVerticalScrollBar().setUI(new TowHitchCore.MinimalScrollBarUI(theme));
        panel.add(sectionContainer(scroll), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAlertRow(TowHitchCore.InventoryItem item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        Color divider = theme.isDark() ? new Color(0x333740) : new Color(0xE5E2DC);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, divider),
                BorderFactory.createEmptyBorder(12, 4, 12, 4)));
        JLabel name = new JLabel(item.itemName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        JLabel quantity = new JLabel(item.quantity + " stocks");
        quantity.setFont(new Font("SansSerif", Font.BOLD, 14));
        quantity.setForeground(theme.danger());
        row.add(name, BorderLayout.WEST);
        row.add(quantity, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        return row;
    }

    private JPanel buildUnfinishedGoodsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Raw Materials");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(pageSubtitle(
                "Central raw material inventory with direct stock adjustments, threshold controls, and item management."));
        header.add(titleBlock, BorderLayout.WEST);
        TowHitchCore.RoundedButton add = new TowHitchCore.RoundedButton(
                "+ Add Material", theme.primaryButton(), Color.WHITE, 12, theme);
        add.setPreferredSize(new Dimension(170, 42));
        add.addActionListener(event -> openMaterialDialog(null));
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerActions.add(add);
        header.add(headerActions, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        TowHitchCore.RoundedPanel outer = inventoryOuterCard();
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        int materialCount = 0;
        for (TowHitchCore.InventoryItem item : loadInventorySafely()) {
            if ("U".equals(item.type) || "R".equals(item.type)) {
                rows.add(buildMaterialRow(item));
                rows.add(Box.createVerticalStrut(8));
                materialCount++;
            }
        }
        if (materialCount == 0) {
            JLabel empty = new JLabel("No raw materials have been added.");
            empty.setForeground(theme.secondaryText());
            empty.setBorder(BorderFactory.createEmptyBorder(14, 4, 14, 4));
            rows.add(empty);
        }
        JPanel table = new JPanel(new BorderLayout(0, 8));
        table.setOpaque(false);
        table.add(inventoryTableHeader(
                new String[] {"Status", "Material Name", "On Hand", "Min Alert", ""},
                UNFINISHED_COLUMNS), BorderLayout.NORTH);
        table.add(createInventoryScroll(rows), BorderLayout.CENTER);
        outer.add(table, BorderLayout.CENTER);
        page.add(outer, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildMaterialRow(TowHitchCore.InventoryItem item) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setName(item.itemName);
        row.setBackground(matchesHighlight(item.itemName) ? theme.accent()
                : (theme.isDark() ? new Color(0x252832) : Color.WHITE));
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JLabel name = new JLabel(item.itemName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        boolean low = item.quantity <= item.minAlert;
        addAlignedTableCell(row, new TowHitchCore.StatusPillBadge(
                low ? "LOW STOCK" : "IN STOCK", theme), 0, UNFINISHED_COLUMNS,
                SwingConstants.CENTER);
        addAlignedTableCell(row, name, 1, UNFINISHED_COLUMNS, SwingConstants.LEFT);
        addAlignedTableCell(row, tableCell(Integer.toString(item.quantity)), 2,
                UNFINISHED_COLUMNS, SwingConstants.CENTER);
        addAlignedTableCell(row, tableCell(Integer.toString(item.minAlert)), 3,
                UNFINISHED_COLUMNS, SwingConstants.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        controls.setOpaque(false);
        TowHitchCore.MoreOptionsButton actions = new TowHitchCore.MoreOptionsButton(theme);
        actions.addActionListener(event -> showInventoryActions(actions, item));
        controls.add(actions);
        addAlignedTableCell(row, controls, 4, UNFINISHED_COLUMNS, SwingConstants.CENTER);
        return row;
    }

    private JPanel inventoryTableHeader(String[] labels, int[] widths) {
        JPanel header = new TowHitchCore.RoundedPanel(12);
        header.setLayout(new GridBagLayout());
        header.setOpaque(true);
        header.setBackground(theme.isDark() ? new Color(0x1E2026) : Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        for (int i = 0; i < labels.length; i++) {
            JLabel label = tableCell(labels[i]);
            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setForeground(theme.secondaryText());
            addAlignedTableCell(header, label, i, widths,
                    i == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
        }
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
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

    private JPanel buildFinishedGoodsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Finished Goods");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(pageSubtitle(
                "Completed product catalog with live stock counts, Bill of Materials recipes, and threshold settings."));
        header.add(titleBlock, BorderLayout.WEST);
        TowHitchCore.RoundedButton add = new TowHitchCore.RoundedButton(
                "+ Add Product", theme.primaryButton(), Color.WHITE, 12, theme);
        add.setPreferredSize(new Dimension(170, 42));
        add.addActionListener(event -> openProductDialog(null));
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerActions.add(add);
        header.add(headerActions, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        TowHitchCore.RoundedPanel outer = inventoryOuterCard();
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (TowHitchCore.InventoryItem item : loadInventorySafely()) {
            if ("F".equals(item.type)) {
                rows.add(buildFinishedRow(item));
                rows.add(Box.createVerticalStrut(8));
            }
        }
        if (rows.getComponentCount() == 0) {
            JLabel empty = new JLabel("No finished goods have been added.");
            empty.setForeground(theme.secondaryText());
            empty.setBorder(BorderFactory.createEmptyBorder(14, 4, 14, 4));
            rows.add(empty);
        }
        JPanel table = new JPanel(new BorderLayout(0, 8));
        table.setOpaque(false);
        table.add(inventoryTableHeader(
                new String[] {"Status", "Product Name", "On Hand", "Min Alert", "Recipe / Actions"},
                UNFINISHED_COLUMNS), BorderLayout.NORTH);
        table.add(createInventoryScroll(rows), BorderLayout.CENTER);
        outer.add(table, BorderLayout.CENTER);
        page.add(outer, BorderLayout.CENTER);
        return page;
    }

    private TowHitchCore.RoundedPanel inventoryOuterCard() {
        TowHitchCore.RoundedPanel outer = new TowHitchCore.RoundedPanel(20);
        outer.setBackground(theme.isDark() ? new Color(0x1C1F26) : new Color(0xFAF8F4));
        outer.setLayout(new BorderLayout());
        outer.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x2E323B) : new Color(0xE6E2D8), 20),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        return outer;
    }

    private JPanel buildFinishedRow(TowHitchCore.InventoryItem item) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setName(item.itemName);
        row.setBackground(matchesHighlight(item.itemName) ? theme.accent()
                : (theme.isDark() ? new Color(0x252832) : Color.WHITE));
        row.setLayout(new BorderLayout(10, 6));
        row.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        boolean low = item.quantity <= item.minAlert;
        JPanel cells = new JPanel(new GridBagLayout());
        cells.setOpaque(false);
        cells.add(new TowHitchCore.StatusPillBadge(
                low ? "LOW STOCK" : "IN STOCK", theme),
                tableConstraints(0, UNFINISHED_COLUMNS, SwingConstants.CENTER));
        JLabel name = new JLabel(item.itemName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        cells.add(name, tableConstraints(1, UNFINISHED_COLUMNS, SwingConstants.LEFT));
        cells.add(tableCell(Integer.toString(item.quantity)),
                tableConstraints(2, UNFINISHED_COLUMNS, SwingConstants.CENTER));
        cells.add(tableCell(Integer.toString(item.minAlert)),
                tableConstraints(3, UNFINISHED_COLUMNS, SwingConstants.CENTER));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        TowHitchCore.ChevronButton bom = new TowHitchCore.ChevronButton(theme);
        bom.addActionListener(event -> {
            bom.setExpanded(!bom.isExpanded());
            toggleBOMPreview(row, item, bom);
        });
        TowHitchCore.MoreOptionsButton actions = new TowHitchCore.MoreOptionsButton(theme);
        actions.addActionListener(event -> showInventoryActions(actions, item));
        controls.add(bom);
        controls.add(actions);
        cells.add(controls, tableConstraints(4, UNFINISHED_COLUMNS, SwingConstants.CENTER));
        row.add(cells, BorderLayout.CENTER);
        return row;
    }

    private GridBagConstraints tableConstraints(int column, int[] widths, int alignment) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = widths[column] < 0 ? 1.0 : 0.0;
        constraints.fill = widths[column] < 0
                ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        constraints.anchor = alignment == SwingConstants.LEFT
                ? GridBagConstraints.WEST : GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 4, 0, 4);
        return constraints;
    }

    private JScrollPane createInventoryScroll(JPanel rows) {
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        TowHitchCore.configureSmoothScroll(scroll);
        scroll.getVerticalScrollBar().setUI(new TowHitchCore.MinimalScrollBarUI(theme));
        return scroll;
    }

    private TowHitchCore.RoundedButton compactButton(String text) {
        Color fill = theme.input();
        Color foreground = theme.text();
        if ("Approve".equals(text) || "Accept".equals(text)
                || "Confirm & Release".equals(text)) {
            fill = new Color(0x2E6F40);
            foreground = new Color(0xF8FAFC);
        } else if ("Reject".equals(text) || "Cancel".equals(text)
                || "Cancel Order".equals(text)
                || "Delete".equals(text) || "Remove".equals(text)) {
            fill = new Color(0xA63A3A);
            foreground = new Color(0xFAF8F5);
        }
        TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(
                text, fill, foreground, 12, theme);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setMargin(new Insets(4, 8, 4, 8));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel buildStockRequestsPage() {
        TowHitchCore.RoundedPanel panel = new TowHitchCore.RoundedPanel(18);
        panel.setBackground(theme.panel());
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("Stock Requests");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(pageSubtitle(
                "Restock request ledger tracking material replenishment approvals between Staff and Owner."));
        JPanel pageHeader = new JPanel(new BorderLayout(10, 0));
        pageHeader.setOpaque(false);
        pageHeader.add(titleBlock, BorderLayout.CENTER);
        panel.add(pageHeader, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        List<TowHitchCore.StockRequest> requests;
        try {
            requests = TowHitchCore.loadStockRequests();
        } catch (IOException e) {
            requests = new ArrayList<>();
            showLoadError("stock_requests.txt", e);
        }
        requests.sort((left, right) -> {
            int leftRank = "PENDING".equals(left.status) ? 0 : 1;
            int rightRank = "PENDING".equals(right.status) ? 0 : 1;
            return Integer.compare(leftRank, rightRank);
        });
        for (TowHitchCore.StockRequest request : requests) {
            rows.add(buildStockRequestRow(request));
            rows.add(Box.createVerticalStrut(8));
        }
        if (requests.isEmpty()) {
            JLabel empty = new JLabel("No stock requests.");
            empty.setForeground(theme.secondaryText());
            rows.add(empty);
        }
        JPanel table = new JPanel(new BorderLayout(0, 6));
        table.setOpaque(false);
        JPanel header = transactionHeader(new String[] {
                "Status", "Material", "Qty", "Requested By", "Date", ""
        }, REQUEST_COLUMNS);
        JScrollPane scroll = createInventoryScroll(rows);
        alignHeaderToTableViewport(header, scroll);
        table.add(header, BorderLayout.NORTH);
        table.add(scroll, BorderLayout.CENTER);
        JPanel list = new JPanel(new BorderLayout(0, 8));
        list.setOpaque(false);
        JPanel cardHeader = new JPanel(new BorderLayout(8, 0));
        cardHeader.setOpaque(false);
        JButton moreActions = new JButton() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getModel().isRollover()
                        ? (theme.isDark() ? new Color(0x4A4540) : new Color(0xEEE9E0))
                        : (theme.isDark() ? new Color(0x34312F) : Color.WHITE));
                g.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g.setColor(theme.text());
                for (int x : new int[] {10, 16, 22}) g.fillOval(x - 1, 15, 3, 3);
                g.dispose();
            }
        };
        moreActions.setOpaque(false);
        moreActions.setContentAreaFilled(false);
        moreActions.setBorderPainted(false);
        moreActions.setFocusPainted(false);
        moreActions.setPreferredSize(new Dimension(32, 32));
        moreActions.setMinimumSize(new Dimension(32, 32));
        moreActions.setMaximumSize(new Dimension(32, 32));
        moreActions.setBorder(BorderFactory.createEmptyBorder());
        moreActions.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        moreActions.addActionListener(event -> showStockRequestActions(moreActions));
        cardHeader.add(moreActions, BorderLayout.EAST);
        if (stockRequestSelectionMode) {
            JPanel bulkActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            bulkActions.setOpaque(false);
            TowHitchCore.RoundedButton approveSelected = compactButton("Approve Selected");
            TowHitchCore.RoundedButton rejectSelected = compactButton("Reject Selected");
            TowHitchCore.RoundedButton cancelSelection = compactButton("Cancel Selection");
            approveSelected.addActionListener(event -> approveSelectedStockRequests());
            rejectSelected.addActionListener(event -> rejectSelectedStockRequests());
            cancelSelection.addActionListener(event -> clearStockRequestSelection());
            bulkActions.add(approveSelected);
            bulkActions.add(rejectSelected);
            bulkActions.add(cancelSelection);
            cardHeader.add(bulkActions, BorderLayout.CENTER);
        }
        list.add(cardHeader, BorderLayout.NORTH);
        list.add(table, BorderLayout.CENTER);
        panel.add(sectionContainer(list), BorderLayout.CENTER);
        return panel;
    }

    private void showStockRequestActions(Component anchor) {
        javax.swing.JWindow popup = new javax.swing.JWindow(frame);
        popup.setBackground(new Color(0, 0, 0, 0));
        TowHitchCore.RoundedPanel menu = new TowHitchCore.RoundedPanel(14);
        menu.setBackground(theme.panel());
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder.RoundedShadowBorder(
                        theme.isDark() ? new Color(0x383C45) : new Color(0xDCD8CE),
                        14, false),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        TowHitchCore.PopoverMenuButton selectItems = new TowHitchCore.PopoverMenuButton(
                stockRequestSelectionMode ? "Exit Selection" : "Select Items",
                "\u2611", false, false, theme);
        selectItems.addActionListener(event -> {
            anchoredPopover.close();
            stockRequestSelectionMode = !stockRequestSelectionMode;
            if (!stockRequestSelectionMode) selectedStockRequestIds.clear();
            refreshApplication();
        });
        TowHitchCore.PopoverMenuButton selectAll = new TowHitchCore.PopoverMenuButton(
                "Select All", "\u2611", false, false, theme);
        selectAll.addActionListener(event -> {
            anchoredPopover.close();
            stockRequestSelectionMode = true;
            selectAllPendingStockRequests();
        });
        menu.add(selectItems);
        menu.add(Box.createVerticalStrut(4));
        menu.add(selectAll);
        popup.setContentPane(menu);
        popup.pack();
        anchoredPopover.show(anchor, popup, false);
    }

    private void selectAllPendingStockRequests() {
        selectedStockRequestIds.clear();
        try {
            for (TowHitchCore.StockRequest request : TowHitchCore.loadStockRequests()) {
                if ("PENDING".equals(request.status)) {
                    selectedStockRequestIds.add(request.requestId);
                }
            }
        } catch (IOException e) {
            showLoadError("stock_requests.txt", e);
        }
        refreshApplication();
    }

    private void clearStockRequestSelection() {
        selectedStockRequestIds.clear();
        stockRequestSelectionMode = false;
        refreshApplication();
    }

    private JPanel buildStockRequestRow(TowHitchCore.StockRequest request) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        addAlignedTableCell(row, statusPill(request.status), 0, REQUEST_COLUMNS,
                SwingConstants.CENTER);
        addAlignedTableCell(row, tableNameCell(request.materialName), 1, REQUEST_COLUMNS,
                SwingConstants.LEFT);
        addAlignedTableCell(row, tableCell(Integer.toString(request.quantity)), 2, REQUEST_COLUMNS,
                SwingConstants.CENTER);
        addAlignedTableCell(row, tableCell(request.requestedBy), 3, REQUEST_COLUMNS,
                SwingConstants.CENTER);
        addAlignedTableCell(row, tableCell(formatTransactionDate(request.timestamp)), 4,
                REQUEST_COLUMNS, SwingConstants.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        controls.setOpaque(false);
        if ("PENDING".equals(request.status)) {
            if (stockRequestSelectionMode) {
                JCheckBox selected = new JCheckBox();
                selected.setOpaque(false);
                selected.setSelected(selectedStockRequestIds.contains(request.requestId));
                selected.addActionListener(event -> {
                    if (selected.isSelected()) {
                        selectedStockRequestIds.add(request.requestId);
                    } else {
                        selectedStockRequestIds.remove(request.requestId);
                    }
                });
                controls.add(selected);
            }
            TowHitchCore.RoundedButton approve = compactButton("Approve");
            TowHitchCore.RoundedButton reject = compactButton("Reject");
            approve.addActionListener(event -> approveStockRequest(request));
            reject.addActionListener(event -> rejectStockRequest(request));
            controls.add(approve);
            controls.add(reject);
        }
        addAlignedTableCell(row, controls, 5, REQUEST_COLUMNS, SwingConstants.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private JPanel transactionHeader(String[] labels, int[] widths) {
        JPanel header = new TowHitchCore.RoundedPanel(12);
        header.setLayout(new GridBagLayout());
        header.setOpaque(true);
        header.setBackground(theme.isDark() ? new Color(0x1E2026) : Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        for (int i = 0; i < labels.length; i++) {
            JLabel label = tableCell(labels[i]);
            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setForeground(theme.secondaryText());
            Component cell = label;
            if (stockRequestSelectionMode && widths == REQUEST_COLUMNS && i == 5) {
                JCheckBox selectAll = new JCheckBox();
                selectAll.setOpaque(false);
                selectAll.setSelected(allPendingStockRequestsSelected());
                selectAll.addActionListener(event -> {
                    if (selectAll.isSelected()) {
                        selectAllPendingStockRequests();
                    } else {
                        selectedStockRequestIds.clear();
                        refreshApplication();
                    }
                });
                cell = selectAll;
            }
            addAlignedTableCell(header, cell, i, widths, transactionAlignment(i, widths));
        }
        return header;
    }

    private boolean allPendingStockRequestsSelected() {
        try {
            List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
            boolean hasPending = false;
            for (TowHitchCore.StockRequest request : requests) {
                if ("PENDING".equals(request.status)) {
                    hasPending = true;
                    if (!selectedStockRequestIds.contains(request.requestId)) return false;
                }
            }
            return hasPending;
        } catch (IOException e) {
            showLoadError("stock_requests.txt", e);
            return false;
        }
    }

    private int transactionAlignment(int column, int[] widths) {
        if (widths == REQUEST_COLUMNS && (column == 3 || column == 4)) {
            return SwingConstants.CENTER;
        }
        return column == 0 || column == 2 || column == 5
                ? SwingConstants.CENTER : SwingConstants.LEFT;
    }

    private void addAlignedTableCell(JPanel panel, Component component, int column,
                                     int[] widths, int alignment) {
        double[] weights = new double[widths.length];
        for (int i = 0; i < widths.length; i++) {
            weights[i] = widths[i] < 0 ? 1.0 : 0.0;
        }
        TowHitchCore.addAlignedTableCell(panel, component, column, widths, weights, alignment);
    }

    private JLabel tableCell(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(theme.text());
        return label;
    }

    private JLabel tableNameCell(String text) {
        JLabel label = tableCell(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private String formatTransactionDate(String timestamp) {
        return TowHitchCore.formatSmartDate(timestamp);
    }

    private void approveStockRequest(TowHitchCore.StockRequest request) {
        JDialog dialog = createThemedDialog("Approve Stock Request", 420, 230);
        JLabel detail = new JLabel("<html>Approve up to <b>" + request.quantity
                + " " + request.materialName + "</b> requested by "
                + request.requestedBy + ".</html>");
        detail.setForeground(theme.text());
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        TowHitchCore.RoundedBorder.QuantityStepper approvalQuantity =
                new TowHitchCore.RoundedBorder.QuantityStepper(
                        request.quantity, 1, request.quantity, theme);
        approvalQuantity.setPreferredWidth(150);
        JPanel content = (JPanel) dialog.getContentPane();
        content.add(detail);
        content.add(Box.createVerticalStrut(12));
        content.add(approvalQuantity);
        content.add(Box.createVerticalGlue());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        TowHitchCore.RoundedButton cancel = new TowHitchCore.RoundedButton(
                "Cancel", theme.input(), theme.text(), 12, theme);
        TowHitchCore.RoundedButton approve = new TowHitchCore.RoundedButton(
                "Approve", new Color(0x2E6F40), Color.WHITE, 12, theme);
        cancel.addActionListener(event -> dialog.dispose());
        approve.addActionListener(event -> {
            int amount;
            try {
                amount = approvalQuantity.getValue();
            } catch (IllegalArgumentException e) {
                showDialogError(dialog, e.getMessage());
                return;
            }
            dialog.dispose();
            approveStockRequests(java.util.Collections.singletonMap(
                    request.requestId, amount));
        });
        actions.add(cancel);
        actions.add(approve);
        content.add(actions);
        dialog.setVisible(true);
    }

    private void approveSelectedStockRequests() {
        Map<String, Integer> approvals = new LinkedHashMap<>();
        try {
            for (TowHitchCore.StockRequest request : TowHitchCore.loadStockRequests()) {
                if (selectedStockRequestIds.contains(request.requestId)
                        && "PENDING".equals(request.status)) {
                    approvals.put(request.requestId, request.quantity);
                }
            }
        } catch (IOException e) {
            showLoadError("stock_requests.txt", e);
            return;
        }
        if (approvals.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Select at least one pending request.",
                    "Approve Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(frame,
                "Approve the full quantity for " + approvals.size() + " selected request(s)?",
                "Approve Selected Stock Requests", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) approveStockRequests(approvals);
    }

    private void approveStockRequests(Map<String, Integer> approvals) {
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
            List<String> history = new ArrayList<>();
            for (Map.Entry<String, Integer> approval : approvals.entrySet()) {
                TowHitchCore.StockRequest request = null;
                for (TowHitchCore.StockRequest current : requests) {
                    if (current.requestId.equals(approval.getKey())
                            && "PENDING".equals(current.status)) {
                        request = current;
                        break;
                    }
                }
                if (request == null) {
                    throw new IOException("A selected stock request is no longer pending.");
                }
                boolean found = false;
                List<TowHitchCore.InventoryItem> updatedInventory = new ArrayList<>();
                for (TowHitchCore.InventoryItem item : inventory) {
                    if (("U".equals(item.type) || "R".equals(item.type))
                            && item.itemName.equalsIgnoreCase(request.materialName)) {
                        long total = (long) item.quantity + approval.getValue();
                        if (total > Integer.MAX_VALUE) {
                            throw new IOException("Approved quantity exceeds inventory limits for "
                                    + request.materialName + ".");
                        }
                        updatedInventory.add(new TowHitchCore.InventoryItem(
                                "R", item.itemName, (int) total, item.minAlert));
                        found = true;
                    } else {
                        updatedInventory.add(item);
                    }
                }
                if (!found) {
                    throw new IOException("The requested material does not exist in inventory: "
                            + request.materialName + ".");
                }
                inventory = updatedInventory;
                requests = TowHitchCore.withStockRequestApproval(
                        requests, request.requestId, approval.getValue(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                history.add("Approved stock request: " + approval.getValue()
                        + " " + request.materialName + " requested by " + request.requestedBy
                        + " - Approved by Owner " + activeUser.fullName);
            }
            TowHitchCore.saveInventory(inventory);
            TowHitchCore.saveStockRequests(requests);
            for (String message : history) TowHitchCore.appendHistory(message);
            clearStockRequestSelection();
            refreshApplication();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(),
                    "Stock Request Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            showLoadError("the stock request transaction", e);
        }
    }

    private void rejectSelectedStockRequests() {
        List<String> ids = new ArrayList<>(selectedStockRequestIds);
        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Select at least one pending request.",
                    "Reject Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(frame,
                "Reject " + ids.size() + " selected request(s)?",
                "Reject Selected Stock Requests", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
            List<String> history = new ArrayList<>();
            for (TowHitchCore.StockRequest request : requests) {
                if (ids.contains(request.requestId) && "PENDING".equals(request.status)) {
                    request.setStatus("REJECTED");
                    history.add("Rejected stock request: " + request.quantity
                            + " " + request.materialName + " requested by " + request.requestedBy
                            + " - Rejected by Owner " + activeUser.fullName);
                }
            }
            TowHitchCore.saveStockRequests(requests);
            for (String message : history) TowHitchCore.appendHistory(message);
            clearStockRequestSelection();
            refreshApplication();
        } catch (IOException e) {
            showLoadError("the stock request transaction", e);
        }
    }

    private void rejectStockRequest(TowHitchCore.StockRequest request) {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Reject this restock request for " + request.quantity
                        + " " + request.materialName + "?",
                "Reject Stock Request", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
            for (TowHitchCore.StockRequest current : requests) {
                if (current.requestId.equals(request.requestId)) {
                    current.setStatus("REJECTED");
                }
            }
            TowHitchCore.saveStockRequests(requests);
            TowHitchCore.appendHistory("Rejected stock request: " + request.quantity
                    + " " + request.materialName + " requested by " + request.requestedBy
                    + " - Rejected by Owner " + activeUser.fullName);
            refreshApplication();
        } catch (IOException e) {
            showLoadError("the stock request transaction", e);
        }
    }

    private JPanel buildCustomerOrdersPage() {
        TowHitchCore.RoundedPanel panel = new TowHitchCore.RoundedPanel(18);
        panel.setBackground(theme.panel());
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("Customer Orders");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(pageSubtitle(
                "Client order queue for reviewing pending orders and confirming product releases."));
        panel.add(titleBlock, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        List<TowHitchCore.CustomerOrder> orders;
        try {
            orders = TowHitchCore.loadCustomerOrders();
        } catch (IOException e) {
            orders = new ArrayList<>();
            showLoadError("customer_orders.txt", e);
        }
        orders.sort((left, right) -> {
            int leftRank = "PENDING".equals(left.status) ? 0 : 1;
            int rightRank = "PENDING".equals(right.status) ? 0 : 1;
            return Integer.compare(leftRank, rightRank);
        });
        for (TowHitchCore.CustomerOrder order : orders) {
            rows.add(buildCustomerOrderRow(order));
            rows.add(Box.createVerticalStrut(8));
        }
        if (orders.isEmpty()) {
            JLabel empty = new JLabel("No customer orders.");
            empty.setForeground(theme.secondaryText());
            rows.add(empty);
        }
        JPanel list = new JPanel(new BorderLayout(0, 6));
        list.setOpaque(false);
        JPanel header = transactionHeader(new String[] {
                "Status", "Product", "Qty", "Customer", "Order Date", ""
        }, ORDER_COLUMNS);
        JScrollPane scroll = createInventoryScroll(rows);
        alignHeaderToTableViewport(header, scroll);
        list.add(header, BorderLayout.NORTH);
        list.add(scroll, BorderLayout.CENTER);
        panel.add(sectionContainer(list), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCrewAccountsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Crew Accounts");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        heading.add(title);
        heading.add(Box.createVerticalStrut(3));
        heading.add(pageSubtitle(
                "Review Staff account registrations and manage active crew access."));
        page.add(heading, BorderLayout.NORTH);

        int[] widths = {110, -1, 150, 190, 210};
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        try {
            List<TowHitchCore.StaffAccount> accounts = TowHitchCore.loadStaffAccounts();
            accounts.sort((left, right) -> {
                int leftRank = "PENDING".equals(left.status) ? 0
                        : "APPROVED".equals(left.status) ? 1 : 2;
                int rightRank = "PENDING".equals(right.status) ? 0
                        : "APPROVED".equals(right.status) ? 1 : 2;
                return Integer.compare(leftRank, rightRank);
            });
            for (TowHitchCore.StaffAccount account : accounts) {
                rows.add(buildCrewAccountRow(account, widths));
                rows.add(Box.createVerticalStrut(8));
            }
        } catch (IOException e) {
            showLoadError("staff_accounts.txt", e);
        }
        if (rows.getComponentCount() == 0) {
            JLabel empty = new JLabel("No staff account registrations.");
            empty.setForeground(theme.secondaryText());
            rows.add(empty);
        }

        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        JPanel tableHeader = transactionHeader(new String[] {
                "Status", "Crew Name", "Username", "Date Registered", "Actions"
        }, widths);
        JScrollPane scroll = createInventoryScroll(rows);
        alignHeaderToTableViewport(tableHeader, scroll);
        list.add(tableHeader, BorderLayout.NORTH);
        list.add(scroll, BorderLayout.CENTER);
        page.add(sectionContainer(list), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildCrewAccountRow(TowHitchCore.StaffAccount account, int[] widths) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        addAlignedTableCell(row, statusPill(account.status), 0, widths, SwingConstants.CENTER);
        addAlignedTableCell(row, tableNameCell(account.displayName), 1, widths, SwingConstants.LEFT);
        addAlignedTableCell(row, tableCell(account.username), 2, widths, SwingConstants.CENTER);
        addAlignedTableCell(row, tableCell(formatTransactionDate(account.createdAtIso)),
                3, widths, SwingConstants.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        actions.setOpaque(false);
        if ("PENDING".equals(account.status)) {
            TowHitchCore.RoundedButton accept = compactButton("Accept");
            TowHitchCore.RoundedButton cancel = compactButton("Cancel");
            accept.addActionListener(event -> updateCrewAccount(account.id, "APPROVED"));
            cancel.addActionListener(event -> updateCrewAccount(account.id, "REMOVED"));
            actions.add(accept);
            actions.add(cancel);
        } else if ("APPROVED".equals(account.status)) {
            TowHitchCore.RoundedButton remove = compactButton("Remove");
            remove.addActionListener(event -> {
                int choice = JOptionPane.showConfirmDialog(frame,
                        "Remove Staff access for " + account.displayName + "?",
                        "Remove Crew Account", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    updateCrewAccount(account.id, "REMOVED");
                }
            });
            actions.add(remove);
        } else {
            actions.add(tableCell("\u2014"));
        }
        addAlignedTableCell(row, actions, 4, widths, SwingConstants.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        return row;
    }

    private void updateCrewAccount(String accountId, String status) {
        try {
            List<TowHitchCore.StaffAccount> accounts = TowHitchCore.loadStaffAccounts();
            boolean changed = false;
            for (TowHitchCore.StaffAccount account : accounts) {
                if (account.id.equals(accountId)
                        && ("PENDING".equals(account.status) && ("APPROVED".equals(status)
                                || "REMOVED".equals(status))
                                || "APPROVED".equals(account.status)
                                        && "REMOVED".equals(status))) {
                    account.setStatus(status);
                    changed = true;
                    break;
                }
            }
            if (!changed) {
                JOptionPane.showMessageDialog(frame,
                        "This crew account has already changed or no longer exists.",
                        "Crew Account Updated", JOptionPane.INFORMATION_MESSAGE);
                refreshApplication();
                return;
            }
            TowHitchCore.saveStaffAccounts(accounts);
            refreshApplication();
        } catch (IOException e) {
            showLoadError("staff_accounts.txt", e);
        }
    }

    private JPanel buildCustomerOrderRow(TowHitchCore.CustomerOrder order) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        addAlignedTableCell(row, statusPill(order.status), 0, ORDER_COLUMNS,
                SwingConstants.CENTER);
        addAlignedTableCell(row, tableNameCell(order.productName), 1, ORDER_COLUMNS,
                SwingConstants.LEFT);
        addAlignedTableCell(row, tableCell(Integer.toString(order.quantity)), 2, ORDER_COLUMNS,
                SwingConstants.CENTER);
        addAlignedTableCell(row, tableCell(order.customerName), 3, ORDER_COLUMNS,
                SwingConstants.LEFT);
        addAlignedTableCell(row, tableCell(formatTransactionDate(order.timestamp)), 4,
                ORDER_COLUMNS, SwingConstants.LEFT);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        controls.setOpaque(false);
        if ("PENDING".equals(order.status)) {
            TowHitchCore.RoundedButton confirm = compactButton("Confirm & Release");
            TowHitchCore.RoundedButton cancel = compactButton("Cancel Order");
            confirm.setPreferredSize(new Dimension(116, 34));
            cancel.setPreferredSize(new Dimension(92, 34));
            confirm.addActionListener(event -> confirmCustomerOrder(order));
            cancel.addActionListener(event -> cancelCustomerOrder(order));
            controls.add(confirm);
            controls.add(cancel);
        }
        addAlignedTableCell(row, controls, 5, ORDER_COLUMNS, SwingConstants.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private void confirmCustomerOrder(TowHitchCore.CustomerOrder order) {
        try {
            TowHitchCore.InventoryItem product = null;
            for (TowHitchCore.InventoryItem item : TowHitchCore.loadInventory()) {
                if ("F".equals(item.type)
                        && item.itemName.equalsIgnoreCase(order.productName)) {
                    product = item;
                    break;
                }
            }
            if (product == null || product.quantity < order.quantity) {
                JOptionPane.showMessageDialog(frame,
                        "Insufficient Finished Goods stock to fulfill this order.",
                        "Insufficient Stock", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(frame,
                    "Confirm and release " + order.quantity + " " + order.productName
                            + " for " + order.customerName + "?",
                    "Confirm & Release", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            List<TowHitchCore.InventoryItem> updated = new ArrayList<>();
            for (TowHitchCore.InventoryItem item : inventory) {
                if ("F".equals(item.type)
                        && item.itemName.equalsIgnoreCase(order.productName)) {
                    updated.add(new TowHitchCore.InventoryItem(
                            "F", item.itemName, item.quantity - order.quantity,
                            item.minAlert));
                } else {
                    updated.add(item);
                }
            }
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            for (TowHitchCore.CustomerOrder current : orders) {
                if (current.orderId.equals(order.orderId)) {
                    current.setStatus("COMPLETED");
                }
            }
            TowHitchCore.saveInventory(updated);
            TowHitchCore.saveCustomerOrders(orders);
            TowHitchCore.appendHistory("Completed customer order: " + order.orderId
                    + " - " + order.quantity + " " + order.productName
                    + " released to " + order.customerName
                    + " - Confirmed by Admin");
            refreshApplication();
        } catch (IOException e) {
            showLoadError("the customer order transaction", e);
        }
    }

    private void cancelCustomerOrder(TowHitchCore.CustomerOrder order) {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Cancel this order for " + order.quantity + " " + order.productName
                        + " for " + order.customerName + "?",
                "Cancel Customer Order", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            List<TowHitchCore.CustomerOrder> orders = TowHitchCore.loadCustomerOrders();
            for (TowHitchCore.CustomerOrder current : orders) {
                if (current.orderId.equals(order.orderId)) {
                    current.setStatus("CANCELLED");
                }
            }
            TowHitchCore.saveCustomerOrders(orders);
            TowHitchCore.appendHistory("Cancelled customer order: " + order.orderId
                    + " - " + order.quantity + " " + order.productName
                    + " for " + order.customerName
                    + " - Cancelled by Owner " + activeUser.fullName);
            refreshApplication();
        } catch (IOException e) {
            showLoadError("the customer order transaction", e);
        }
    }

    private TowHitchCore.StatusPillBadge statusPill(String status) {
        return new TowHitchCore.StatusPillBadge(status, theme);
    }

    private JPanel buildHistoryPage(String initialFilter, String pageName) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        String titleText = pageName;
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        String subtitleText = "Product Logs".equals(pageName)
                ? "Newest-first completed product assemblies."
                : "Stock Order Logs".equals(pageName)
                        ? "Newest-first approved restocks and released customer orders."
                        : "Newest-first raw material deductions.";
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(theme.secondaryText());
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);
        header.add(heading, BorderLayout.CENTER);

        TowHitchCore.RoundedTextField search =
                new TowHitchCore.RoundedTextField(
                        "Search by item, action, or date...", 28, theme);
        RoundedSearchPanel searchContainer = new RoundedSearchPanel();
        searchContainer.setPreferredSize(new Dimension(350, 38));
        searchContainer.add(buildSearchLeading(), BorderLayout.WEST);
        searchContainer.add(search, BorderLayout.CENTER);
        search.setMargin(new Insets(0, 10, 0, 8));
        search.setText(initialFilter);
        header.add(searchContainer, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel results = new JPanel(new BorderLayout());
        results.setOpaque(false);
        panel.add(sectionContainer(results), BorderLayout.CENTER);
        Runnable refresh = () -> {
            List<HistoryLogRecord> filtered = new ArrayList<>();
            for (HistoryLogRecord record : readHistoryRecords()) {
                String query = search.getText().trim().toLowerCase(Locale.ROOT);
                String formattedDate = TowHitchCore.formatSmartDateOnly(record.date);
                if (!query.isEmpty()
                        && !record.message.toLowerCase(Locale.ROOT).contains(query)
                        && !record.display.toLowerCase(Locale.ROOT).contains(query)
                        && !record.date.toLowerCase(Locale.ROOT).contains(query)
                        && !formattedDate.toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
                boolean matchesSection = "Product Logs".equals(pageName)
                        ? isProductionRecord(record.message)
                        : "Stock Order Logs".equals(pageName)
                                ? isStockOrderRecord(record.message)
                                : isMaterialConsumptionRecord(record.message);
                if (matchesSection) {
                    filtered.add(record);
                }
            }
            results.removeAll();
            results.add(buildHistoryLogCard(titleText, filtered), BorderLayout.CENTER);
            results.revalidate();
            results.repaint();
        };
        search.addActionListener(event -> refresh.run());
        refresh.run();
        return panel;
    }

    private JPanel buildTrashPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false);
        JLabel title = new JLabel("Trash");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(pageSubtitle(
                "30-day recovery archive for recently deleted inventory items and records."));
        page.add(titleBlock, BorderLayout.NORTH);
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        int[] widths = { -1, 130, 60, 130, 160, 90, 230 };
        JPanel header = transactionHeader(new String[] {
                "Item / Record", "Category", "Qty", "Deleted By", "Deleted Date", "Expires In", ""
        }, widths);
        try {
            for (TowHitchCore.TrashEntry entry : TowHitchCore.loadTrash()) {
                rows.add(buildTrashRow(entry, widths));
                rows.add(Box.createVerticalStrut(7));
            }
        } catch (IOException e) {
            showLoadError("trash_bin.txt", e);
        }
        if (rows.getComponentCount() == 0) {
            rows.add(new JLabel("Trash is empty."));
        }
        JPanel list = new JPanel(new BorderLayout(0, 7));
        list.setOpaque(false);
        JScrollPane scroll = createInventoryScroll(rows);
        alignHeaderToTableViewport(header, scroll);
        list.add(header, BorderLayout.NORTH);
        list.add(scroll, BorderLayout.CENTER);
        page.add(sectionContainer(list), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildTrashRow(TowHitchCore.TrashEntry entry, int[] widths) {
        JPanel row = new TowHitchCore.RoundedPanel(12);
        row.setLayout(new GridBagLayout());
        row.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xECEAE4), 12),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        TowHitchCore.addAlignedTableCell(row, tableNameCell(entry.itemName), 0,
                widths, trashWeights(widths), SwingConstants.LEFT);
        TowHitchCore.addAlignedTableCell(row, tableCell(trashCategoryLabel(entry.category)), 1,
                widths, trashWeights(widths), SwingConstants.LEFT);
        TowHitchCore.addAlignedTableCell(row, tableCell(Integer.toString(entry.quantity)), 2,
                widths, trashWeights(widths), SwingConstants.CENTER);
        TowHitchCore.addAlignedTableCell(row, tableCell(entry.deletedBy), 3,
                widths, trashWeights(widths), SwingConstants.LEFT);
        String deletedDate = TowHitchCore.formatSmartDate(entry.deletedAtIso);
        TowHitchCore.addAlignedTableCell(row, tableCell(deletedDate), 4,
                widths, trashWeights(widths), SwingConstants.LEFT);
        long age;
        try {
            age = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDateTime.parse(entry.deletedAtIso),
                    java.time.LocalDateTime.now());
        } catch (RuntimeException e) {
            age = 30;
        }
        long daysRemaining = Math.min(30, Math.max(0, 30 - age));
        TowHitchCore.addAlignedTableCell(row, tableCell(daysRemaining + " days"), 5,
                widths, trashWeights(widths), SwingConstants.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        actions.setOpaque(false);
        TowHitchCore.RoundedButton restore = new TowHitchCore.RoundedButton(
                "Restore", theme.success(), new Color(0xF8FAFC), 10, theme);
        TowHitchCore.RoundedButton delete = new TowHitchCore.RoundedButton(
                "Delete Permanently", new Color(0xA63A3A), new Color(0xFAF8F5), 10, theme);
        restore.addActionListener(event -> restoreTrashEntry(entry));
        delete.addActionListener(event -> permanentlyDeleteTrashEntry(entry));
        actions.add(restore);
        actions.add(delete);
        TowHitchCore.addAlignedTableCell(row, actions, 6, widths,
                trashWeights(widths), SwingConstants.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private double[] trashWeights(int[] widths) {
        double[] weights = new double[widths.length];
        weights[0] = 1.0;
        return weights;
    }

    private String trashCategoryLabel(String category) {
        return "F".equals(category) ? "Finished Goods" : "Raw Materials";
    }

    private void restoreTrashEntry(TowHitchCore.TrashEntry entry) {
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            String type = entry.category.equals("F") ? "F"
                    : entry.category.equals("U") ? "U" : "R";
            for (TowHitchCore.InventoryItem item : inventory) {
                if (item.itemName.equalsIgnoreCase(entry.itemName)
                        && item.type.equals(type)) {
                    JOptionPane.showMessageDialog(frame,
                            "An item with this name already exists in inventory.",
                            "Restore Not Available", JOptionPane.WARNING_MESSAGE);
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
                            recipe.add(new TowHitchCore.BOMItem(
                                    fields[0], Integer.parseInt(fields[1])));
                        }
                    }
                    bom.put(entry.itemName, recipe);
                } else if (lines.length > 0 && "RAW".equals(lines[0])) {
                    for (int i = 1; i < lines.length; i++) {
                        String[] fields = lines[i].split("\\t", -1);
                        if (fields.length == 2) {
                            bom.computeIfAbsent(fields[0], ignored -> new ArrayList<>())
                                    .add(new TowHitchCore.BOMItem(
                                            entry.itemName, Integer.parseInt(fields[1])));
                        }
                    }
                }
                TowHitchCore.saveBOM(bom);
            }
            TowHitchCore.removeFromTrash(entry.id);
            TowHitchCore.appendHistory("Restored from Trash: " + entry.itemName
                    + " by Owner " + activeUser.fullName);
            TowHitchCore.showToast(frame, entry.itemName + " restored", true);
            refreshApplication();
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(frame, "Unable to restore item: " + e.getMessage(),
                    "Trash Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void permanentlyDeleteTrashEntry(TowHitchCore.TrashEntry entry) {
        int answer = JOptionPane.showConfirmDialog(frame,
                "Permanently delete '" + entry.itemName + "'? This cannot be undone.",
                "Delete Permanently", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            TowHitchCore.removeFromTrash(entry.id);
            TowHitchCore.appendHistory("Permanently deleted from Trash: " + entry.itemName
                    + " by Owner " + activeUser.fullName);
            refreshApplication();
        } catch (IOException e) {
            showLoadError("trash_bin.txt", e);
        }
    }

    private List<HistoryLogRecord> readHistoryRecords() {
        List<HistoryLogRecord> records = new ArrayList<>();
        Path historyFile = Paths.get("history_logs.txt").toAbsolutePath();
        if (!Files.isRegularFile(historyFile)) {
            return records;
        }
        try {
            List<String> lines = Files.readAllLines(historyFile, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) {
                records.addAll(parseHistoryLogRecords(lines.get(i)));
            }
        } catch (IOException e) {
            showLoadError("history_logs.txt", e);
        }
        return records;
    }

    private List<HistoryLogRecord> parseHistoryLogRecords(String line) {
        List<HistoryLogRecord> records = new ArrayList<>();
        if (line == null || line.trim().isEmpty()) {
            return records;
        }
        String timestamp;
        String message;
        if (line.startsWith("[") && line.contains("] ")) {
            int end = line.indexOf("] ");
            timestamp = line.substring(1, end).trim();
            message = line.substring(end + 2).trim();
        } else {
            int separator = line.indexOf('\t');
            if (separator > 0) {
                timestamp = line.substring(0, separator).trim();
                message = line.substring(separator + 1).trim();
            } else {
                timestamp = "";
                message = line.trim();
            }
        }
        String date = "";
        String time = "";
        int space = timestamp.indexOf(' ');
        if (space > 0 && space < timestamp.length() - 1) {
            date = timestamp.substring(0, space);
            time = timestamp.substring(space + 1);
        } else {
            date = timestamp;
        }
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
                    records.add(new HistoryLogRecord(message,
                            "Produced " + producedWithActor, date, time));
                    return records;
                }
            }
            int quantityStart = produced.lastIndexOf(" (");
            if (quantityStart > 0 && produced.endsWith(")")) {
                String quantity = produced.substring(quantityStart + 2, produced.length() - 1);
                String productName = produced.substring(0, quantityStart).trim();
                records.add(new HistoryLogRecord(message,
                        quantity + " " + productName + " produced", date, time));
                return records;
            }
        } else if (message.startsWith("Deducted: ")) {
            String deduction = message.substring("Deducted: ".length());
            records.add(new HistoryLogRecord(message, deduction, date, time));
            return records;
        } else if (message.startsWith("Consumed for ")) {
            int separator = message.indexOf(": ");
            if (separator >= 0 && separator + 2 < message.length()) {
                for (String material : message.substring(separator + 2).split(",\\s*")) {
                    String item = material.trim();
                    if (item.endsWith(")") && item.contains(" (")) {
                        int quantityStart = item.lastIndexOf(" (");
                        String quantity = item.substring(quantityStart + 2, item.length() - 1);
                        String materialName = item.substring(0, quantityStart).trim();
                        records.add(new HistoryLogRecord(message,
                                quantity + " " + materialName, date, time));
                    }
                }
                return records;
            }
        }
        records.add(new HistoryLogRecord(message, message, date, time));
        return records;
    }

    private boolean isProductionRecord(String message) {
        return message.startsWith("Produced:");
    }

    private boolean isStockOrderRecord(String message) {
        String normalized = message.toLowerCase();
        return normalized.contains("approved stock request")
                || normalized.contains("completed customer order");
    }

    private boolean isMaterialConsumptionRecord(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.startsWith("deducted:") || normalized.startsWith("consumed for ");
    }

    private TowHitchCore.RoundedPanel buildHistoryLogCard(
            String titleText, List<HistoryLogRecord> records) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(24);
        card.setBackground(theme.isDark() ? new Color(0x1C1F26) : new Color(0xFAF8F4));
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel heading = new JLabel(titleText);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(theme.text());
        card.add(heading, BorderLayout.NORTH);
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (HistoryLogRecord record : records) {
            String dateLabel = TowHitchCore.formatSmartDateOnly(record.date);
            if (rows.getComponentCount() == 0
                    || !dateLabel.equals(rows.getClientProperty("currentHistoryDate"))) {
                JLabel dateHeading = new JLabel(dateLabel.isEmpty() ? "Unknown date" : dateLabel);
                dateHeading.setFont(new Font("SansSerif", Font.BOLD, 13).deriveFont(13.5f));
                dateHeading.setForeground(theme.text());
                dateHeading.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0));
                dateHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
                rows.add(dateHeading);
                rows.putClientProperty("currentHistoryDate", dateLabel);
            }
            rows.add(buildHistoryLogRow(record));
            rows.add(Box.createVerticalStrut(6));
        }
        if (rows.getComponentCount() == 0) {
            JLabel empty = new JLabel("No history records yet.");
            empty.setForeground(theme.secondaryText());
            rows.add(empty);
        }
        JScrollPane scroll = createInventoryScroll(rows);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private TowHitchCore.RoundedPanel buildHistoryLogRow(HistoryLogRecord record) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setBackground(theme.isDark() ? new Color(0x252832) : Color.WHITE);
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel description = new JLabel(record.display);
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        description.setForeground(theme.text());
        JLabel timestamp = new JLabel(TowHitchCore.formatHistoryTime(record.time));
        timestamp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        timestamp.setForeground(theme.secondaryText());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        row.add(description, constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(0, 12, 0, 0);
        constraints.anchor = GridBagConstraints.EAST;
        row.add(timestamp, constraints);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                Math.max(46, row.getPreferredSize().height)));
        return row;
    }

    private void showLoadError(String source, IOException e) {
        JOptionPane.showMessageDialog(frame,
                "Unable to access " + source + ": " + e.getMessage(),
                "Persistence Error", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel buildPlaceholderPage(String titleText) {
        TowHitchCore.RoundedPanel panel = new TowHitchCore.RoundedPanel(18);
        panel.setBackground(theme.panel());
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        panel.add(title, BorderLayout.NORTH);
        JLabel placeholder = new JLabel("This Owner Portal section is ready for implementation.");
        placeholder.setFont(new Font("SansSerif", Font.PLAIN, 14));
        placeholder.setForeground(theme.secondaryText());
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(placeholder, BorderLayout.CENTER);
        return panel;
    }

    private JLabel pageSubtitle(String text) {
        JLabel subtitle = new JLabel(text);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(theme.secondaryText());
        return subtitle;
    }

    private JPanel sectionContainer(Component content) {
        TowHitchCore.RoundedPanel section = TowHitchCore.createSectionPanel(theme);
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    private void openMaterialDialog(TowHitchCore.InventoryItem existing) {
        JDialog dialog = createThemedDialog(
                existing == null ? "Add Material" : "Edit Material", 560, 330);
        JPanel content = (JPanel) dialog.getContentPane();
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField name = new JTextField(existing == null ? "" : existing.itemName, 24);
        TowHitchCore.configureTextField(name);
        TowHitchCore.RoundedBorder.QuantityStepper quantity =
                new TowHitchCore.RoundedBorder.QuantityStepper(
                        existing == null ? 0 : existing.quantity, 0,
                        Integer.MAX_VALUE, theme);
        TowHitchCore.RoundedBorder.QuantityStepper minAlert =
                new TowHitchCore.RoundedBorder.QuantityStepper(
                        existing == null ? 5 : existing.minAlert, 0,
                        Integer.MAX_VALUE, theme);
        styleModalField(name, 240);
        TowHitchCore.RoundedPanel nameBox = new TowHitchCore.RoundedPanel(12);
        nameBox.setBackground(theme.input());
        nameBox.setLayout(new BorderLayout());
        nameBox.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.row(), 12),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));
        name.setOpaque(false);
        name.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        nameBox.add(name, BorderLayout.CENTER);
        nameBox.setPreferredSize(new Dimension(240, 38));
        quantity.setPreferredWidth(140);
        minAlert.setPreferredWidth(140);
        addMaterialFormRow(form, "Material Name", nameBox, 0);
        addMaterialFormRow(form, "Initial Quantity", quantity, 1);
        addMaterialFormRow(form, "Minimum Alert Level", minAlert, 2);
        content.add(form);
        content.add(Box.createVerticalGlue());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        TowHitchCore.RoundedButton cancel = new TowHitchCore.RoundedButton(
                "Cancel", theme.input(), theme.text(), 12, theme);
        TowHitchCore.RoundedButton save = new TowHitchCore.RoundedButton(
                existing == null ? "Add" : "Save", theme.primaryButton(),
                Color.WHITE, 12, theme);
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            String newName = name.getText().trim();
            if (newName.isEmpty()) {
                showDialogError(dialog, "Material name cannot be empty.");
                return;
            }
            if (newName.indexOf('\t') >= 0 || newName.indexOf('\n') >= 0
                    || newName.indexOf('\r') >= 0) {
                showDialogError(dialog, "Material name cannot contain tabs or newlines.");
                return;
            }
            int parsedQuantity;
            int parsedMin;
            try {
                parsedQuantity = quantity.getValue();
                parsedMin = minAlert.getValue();
            } catch (IllegalArgumentException e) {
                showDialogError(dialog, e.getMessage());
                return;
            }
            try {
                List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
                for (TowHitchCore.InventoryItem item : inventory) {
                    boolean isSameExisting = existing != null
                            && item.itemName.equalsIgnoreCase(existing.itemName);
                    if (!isSameExisting && item.itemName.equalsIgnoreCase(newName)
                            && ("U".equals(item.type) || "R".equals(item.type))) {
                        showDialogError(dialog, "A material with this name already exists.");
                        return;
                    }
                }
                if (existing == null) {
                    inventory.add(new TowHitchCore.InventoryItem(
                            "R", newName, parsedQuantity, parsedMin));
                    TowHitchCore.saveInventory(inventory);
                    TowHitchCore.appendHistory("Added material: " + newName
                            + " (Qty: " + parsedQuantity + ", Min Alert: " + parsedMin + ")");
                } else {
                    renameAndReplaceMaterial(inventory, existing, newName, parsedQuantity, parsedMin);
                }
                dialog.dispose();
                refreshApplication();
            } catch (IOException e) {
                showDialogError(dialog, "Unable to save: " + e.getMessage());
            }
        });
        actions.add(cancel);
        actions.add(save);
        content.add(actions);
        dialog.setVisible(true);
    }

    private void addMaterialFormRow(JPanel form, String labelText,
                                    Component control, int row) {
        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0;
        label.gridy = row;
        label.weightx = 0;
        label.fill = GridBagConstraints.NONE;
        label.anchor = GridBagConstraints.WEST;
        label.insets = new Insets(6, 0, 6, 12);
        JLabel fieldLabel = dialogFieldLabel(labelText);
        fieldLabel.setPreferredSize(new Dimension(150, 34));
        form.add(fieldLabel, label);
        GridBagConstraints input = new GridBagConstraints();
        input.gridx = 1;
        input.gridy = row;
        input.weightx = 1;
        input.fill = GridBagConstraints.HORIZONTAL;
        input.anchor = GridBagConstraints.WEST;
        input.insets = new Insets(6, 0, 6, 0);
        form.add(control, input);
    }

    private JDialog createThemedDialog(String title, int width, int height) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(frame);
        TowHitchCore.RoundedPanel content = new TowHitchCore.RoundedPanel(20);
        content.setBackground(theme.panel());
        content.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder.RoundedShadowBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xDCD8CE), 20),
                BorderFactory.createEmptyBorder(12, 18, 18, 18)));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(theme.text());
        header.add(heading, BorderLayout.CENTER);
        JButton close = new JButton("\u2715");
        close.setFocusable(false);
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setForeground(theme.secondaryText());
        close.addActionListener(event -> dialog.dispose());
        header.add(close, BorderLayout.EAST);
        content.add(header);
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
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        dialog.setContentPane(content);
        return dialog;
    }

    private JLabel dialogFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(theme.text());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 5, 0));
        return label;
    }

    private void styleModalField(JTextField field, int width) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(theme.text());
        field.setBackground(theme.input());
        field.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder(theme.row(), 12),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        field.setPreferredSize(new Dimension(width, 38));
    }

    private void showDialogError(JDialog dialog, String message) {
        JOptionPane.showMessageDialog(dialog, message, "Validation Error",
                JOptionPane.WARNING_MESSAGE);
    }

    private void renameAndReplaceMaterial(List<TowHitchCore.InventoryItem> inventory,
                                          TowHitchCore.InventoryItem existing,
                                          String newName, int quantity, int minAlert)
            throws IOException {
        List<TowHitchCore.InventoryItem> updated = new ArrayList<>();
        for (TowHitchCore.InventoryItem item : inventory) {
            if (item.itemName.equals(existing.itemName)
                    && ("U".equals(item.type) || "R".equals(item.type))) {
                updated.add(new TowHitchCore.InventoryItem(
                        "R", newName, quantity, minAlert));
            } else {
                updated.add(item);
            }
        }
        TowHitchCore.saveInventory(updated);
        if (!existing.itemName.equalsIgnoreCase(newName)) {
            Map<String, List<TowHitchCore.BOMItem>> bom = TowHitchCore.loadBOM();
            for (List<TowHitchCore.BOMItem> recipes : bom.values()) {
                for (int i = 0; i < recipes.size(); i++) {
                    TowHitchCore.BOMItem recipe = recipes.get(i);
                    if (recipe.materialName.equals(existing.itemName)) {
                        recipes.set(i, new TowHitchCore.BOMItem(newName,
                                recipe.quantityRequired));
                    }
                }
            }
            TowHitchCore.saveBOM(bom);
        }
        TowHitchCore.appendHistory("Edited material: " + existing.itemName
                + " -> " + newName + " (Qty: " + quantity + ", Min Alert: " + minAlert + ")");
    }

    private void showInventoryActions(Component anchor, TowHitchCore.InventoryItem item) {
        javax.swing.JWindow popup = new javax.swing.JWindow(frame);
        popup.setBackground(new Color(0, 0, 0, 0));
        TowHitchCore.RoundedPanel menu = new TowHitchCore.RoundedPanel(14);
        menu.setBackground(theme.panel());
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createCompoundBorder(
                new TowHitchCore.RoundedBorder.RoundedShadowBorder(
                        theme.isDark() ? new Color(0x383C45) : new Color(0xDCD8CE),
                        14, false),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        Runnable quickAdjustAction = () -> openQuickQuantityDialog(item);
        Runnable editAction = () -> {
            if ("F".equals(item.type)) {
                openProductDialog(item);
            } else {
                openMaterialDialog(item);
            }
        };
        Runnable deleteAction = () -> {
            if ("F".equals(item.type)) {
                confirmDeleteProduct(item);
            } else {
                confirmDeleteMaterial(item);
            }
        };
        menu.add(inventoryActionButton("Quick Adjust / Set Quantity", false,
                popup, quickAdjustAction));
        menu.add(Box.createVerticalStrut(4));
        menu.add(inventoryActionButton("Edit Item / Details", false, popup, editAction));
        JPanel divider = profileDivider();
        divider.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        menu.add(divider);
        menu.add(inventoryActionButton("Delete Item", true, popup, deleteAction));
        popup.setContentPane(menu);
        popup.pack();
        anchoredPopover.show(anchor, popup, false);
    }

    private TowHitchCore.RoundedButton inventoryActionButton(
            String text, boolean danger, javax.swing.JWindow popup, Runnable action) {
        Color fill = danger ? new Color(0xA63A3A) : theme.row();
        Color foreground = danger ? Color.WHITE : theme.text();
        TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(
                text, fill, foreground, 10, theme);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setPreferredSize(new Dimension(252, 38));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> {
            anchoredPopover.close();
            action.run();
        });
        return button;
    }

    private void openQuickQuantityDialog(TowHitchCore.InventoryItem item) {
        JDialog dialog = createThemedDialog("Quick Adjust / Set Quantity", 380, 190);
        JLabel name = new JLabel(item.itemName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        TowHitchCore.RoundedBorder.QuantityStepper quantity =
                new TowHitchCore.RoundedBorder.QuantityStepper(
                        item.quantity, 0, Integer.MAX_VALUE, theme);
        JPanel adjustment = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        adjustment.setOpaque(false);
        adjustment.add(quantity);
        TowHitchCore.RoundedButton cancel = new TowHitchCore.RoundedButton(
                "Cancel", theme.input(), theme.text(), 12, theme);
        TowHitchCore.RoundedButton save = new TowHitchCore.RoundedButton(
                "Save", theme.primaryButton(), Color.WHITE, 12, theme);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            int next;
            try {
                next = quantity.getValue();
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(dialog, e.getMessage(),
                        "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dialog.dispose();
            setInventoryQuantity(item, next);
        });
        actions.add(cancel);
        actions.add(save);
        JPanel content = (JPanel) dialog.getContentPane();
        content.add(name);
        content.add(adjustment);
        content.add(Box.createVerticalGlue());
        content.add(actions);
        dialog.setVisible(true);
    }

    private void setInventoryQuantity(TowHitchCore.InventoryItem item, int next) {
        if (next < 0) {
            JOptionPane.showMessageDialog(frame, "Quantity cannot be negative.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            List<TowHitchCore.InventoryItem> updated = new ArrayList<>();
            boolean found = false;
            for (TowHitchCore.InventoryItem current : inventory) {
                if (current.itemName.equalsIgnoreCase(item.itemName)
                        && current.type.equals(item.type)) {
                    updated.add(new TowHitchCore.InventoryItem(
                            current.type, current.itemName, next, current.minAlert));
                    found = true;
                } else {
                    updated.add(current);
                }
            }
            if (!found) {
                JOptionPane.showMessageDialog(frame, "Inventory item no longer exists.",
                        "Inventory Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            TowHitchCore.saveInventory(updated);
            TowHitchCore.appendHistory("Adjusted inventory: " + item.itemName
                    + " to Qty: " + next);
            refreshApplication();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to update quantity: " + e.getMessage(),
                    "Inventory Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmDeleteMaterial(TowHitchCore.InventoryItem item) {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Move '" + item.itemName + "' to Trash? It can be restored for 30 days.",
                "Move Material to Trash", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            Map<String, List<TowHitchCore.BOMItem>> bom = TowHitchCore.loadBOM();
            StringBuilder linkedRecipes = new StringBuilder("RAW\n");
            for (Map.Entry<String, List<TowHitchCore.BOMItem>> recipe : bom.entrySet()) {
                for (TowHitchCore.BOMItem component : recipe.getValue()) {
                    if (component.materialName.equalsIgnoreCase(item.itemName)) {
                        linkedRecipes.append(recipe.getKey()).append('\t')
                                .append(component.quantityRequired).append('\n');
                    }
                }
            }
            String extraData = java.util.Base64.getEncoder().encodeToString(
                    linkedRecipes.toString().getBytes(StandardCharsets.UTF_8));
            TowHitchCore.addToTrash(item.type, item.itemName,
                    item.quantity, item.minAlert, extraData, activeUser.fullName);
            inventory.removeIf(current -> current.itemName.equalsIgnoreCase(item.itemName)
                    && ("U".equals(current.type) || "R".equals(current.type)));
            TowHitchCore.saveInventory(inventory);
            for (List<TowHitchCore.BOMItem> recipes : bom.values()) {
                recipes.removeIf(recipe -> recipe.materialName.equalsIgnoreCase(item.itemName));
            }
            TowHitchCore.saveBOM(bom);
            TowHitchCore.appendHistory("Deleted material: " + item.itemName);
            refreshApplication();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to delete material: " + e.getMessage(),
                    "Inventory Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openProductDialog(TowHitchCore.InventoryItem existing) {
        JDialog dialog = createThemedDialog(
                existing == null ? "Add Product" : "Edit Product", 560, 560);
        JPanel form = (JPanel) dialog.getContentPane();

        JTextField name = new JTextField(existing == null ? "" : existing.itemName, 24);
        TowHitchCore.configureTextField(name);
        TowHitchCore.RoundedBorder.QuantityStepper quantity =
                new TowHitchCore.RoundedBorder.QuantityStepper(
                        existing == null ? 0 : existing.quantity, 0,
                        Integer.MAX_VALUE, theme);
        TowHitchCore.RoundedBorder.QuantityStepper minAlert =
                new TowHitchCore.RoundedBorder.QuantityStepper(
                        existing == null ? 3 : existing.minAlert, 0,
                        Integer.MAX_VALUE, theme);
        styleModalField(name, 230);

        form.add(dialogFieldLabel("Product Name"));
        form.add(name);
        form.add(dialogFieldLabel("Initial Finished Quantity"));
        form.add(quantity);
        form.add(dialogFieldLabel("Minimum Alert Level"));
        form.add(minAlert);
        form.add(dialogFieldLabel("Optional BOM Recipe"));

        Map<String, List<TowHitchCore.BOMItem>> bom;
        try {
            bom = TowHitchCore.loadBOM();
        } catch (IOException e) {
            bom = new LinkedHashMap<>();
        }
        Map<String, Integer> existingRecipe = new LinkedHashMap<>();
        if (existing != null) {
            for (TowHitchCore.BOMItem recipe : bom.getOrDefault(
                    existing.itemName, new ArrayList<TowHitchCore.BOMItem>())) {
                existingRecipe.put(recipe.materialName, recipe.quantityRequired);
            }
        }

        JPanel recipePanel = new JPanel();
        recipePanel.setOpaque(false);
        recipePanel.setLayout(new BoxLayout(recipePanel, BoxLayout.Y_AXIS));
        List<JCheckBox> picks = new ArrayList<>();
        List<TowHitchCore.RoundedBorder.QuantityStepper> requiredQuantities =
                new ArrayList<>();
        for (TowHitchCore.InventoryItem material : loadInventorySafely()) {
            if ("U".equals(material.type) || "R".equals(material.type)) {
                JPanel recipeRow = new JPanel(new BorderLayout(8, 0));
                recipeRow.setOpaque(false);
                Integer required = existingRecipe.get(material.itemName);
                JCheckBox check = new JCheckBox(material.itemName,
                        required != null);
                check.setForeground(theme.text());
                check.setOpaque(false);
                TowHitchCore.RoundedBorder.QuantityStepper requiredField =
                        new TowHitchCore.RoundedBorder.QuantityStepper(
                                required == null ? 1 : required, 0,
                                Integer.MAX_VALUE, theme);
                JLabel perUnit = new JLabel("per unit");
                perUnit.setForeground(theme.secondaryText());
                recipeRow.add(check, BorderLayout.CENTER);
                JPanel quantityArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
                quantityArea.setOpaque(false);
                quantityArea.add(requiredField);
                quantityArea.add(perUnit);
                recipeRow.add(quantityArea, BorderLayout.EAST);
                recipePanel.add(recipeRow);
                picks.add(check);
                requiredQuantities.add(requiredField);
            }
        }
        JScrollPane recipeScroll = new JScrollPane(recipePanel);
        TowHitchCore.configureSmoothScroll(recipeScroll);
        recipeScroll.setPreferredSize(new Dimension(500, 150));
        recipeScroll.setBorder(BorderFactory.createEmptyBorder());
        recipeScroll.getVerticalScrollBar().setUI(
                new TowHitchCore.MinimalScrollBarUI(theme));
        form.add(recipeScroll);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        TowHitchCore.RoundedButton cancel = new TowHitchCore.RoundedButton(
                "Cancel", theme.input(), theme.text(), 12, theme);
        TowHitchCore.RoundedButton save = new TowHitchCore.RoundedButton(
                existing == null ? "Add" : "Save", theme.primaryButton(),
                Color.WHITE, 12, theme);
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            String newName = name.getText().trim();
            if (newName.isEmpty()) {
                showDialogError(dialog, "Product name cannot be empty.");
                return;
            }
            if (newName.indexOf('\t') >= 0 || newName.indexOf('\n') >= 0
                    || newName.indexOf('\r') >= 0) {
                showDialogError(dialog, "Product name cannot contain tabs or newlines.");
                return;
            }
            int parsedQuantity;
            int parsedMin;
            try {
                parsedQuantity = quantity.getValue();
                parsedMin = minAlert.getValue();
            } catch (IllegalArgumentException e) {
                showDialogError(dialog, e.getMessage());
                return;
            }
            List<TowHitchCore.BOMItem> recipe = new ArrayList<>();
            try {
                for (int i = 0; i < picks.size(); i++) {
                    if (picks.get(i).isSelected()) {
                        int required = requiredQuantities.get(i).getValue();
                        recipe.add(new TowHitchCore.BOMItem(
                                picks.get(i).getText(), required));
                    }
                }
            } catch (IllegalArgumentException e) {
                showDialogError(dialog, e.getMessage());
                return;
            }

            try {
                List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
                for (TowHitchCore.InventoryItem item : inventory) {
                    boolean isSameExisting = existing != null
                            && item.itemName.equalsIgnoreCase(existing.itemName);
                    if ("F".equals(item.type) && !isSameExisting
                            && item.itemName.equalsIgnoreCase(newName)) {
                        showDialogError(dialog,
                                "A finished product with this name already exists.");
                        return;
                    }
                }
                if (existing == null) {
                    inventory.add(new TowHitchCore.InventoryItem(
                            "F", newName, parsedQuantity, parsedMin));
                    Map<String, List<TowHitchCore.BOMItem>> updatedBom =
                            TowHitchCore.loadBOM();
                    updatedBom.put(newName, recipe);
                    TowHitchCore.saveInventory(inventory);
                    TowHitchCore.saveBOM(updatedBom);
                    TowHitchCore.appendHistory("Added product: " + newName
                            + " (Qty: " + parsedQuantity + ", Min Alert: " + parsedMin + ")");
                } else {
                    List<TowHitchCore.InventoryItem> updated = new ArrayList<>();
                    for (TowHitchCore.InventoryItem item : inventory) {
                        if ("F".equals(item.type)
                                && item.itemName.equalsIgnoreCase(existing.itemName)) {
                            updated.add(new TowHitchCore.InventoryItem(
                                    "F", newName, parsedQuantity, parsedMin));
                        } else {
                            updated.add(item);
                        }
                    }
                    Map<String, List<TowHitchCore.BOMItem>> updatedBom =
                            TowHitchCore.loadBOM();
                    updatedBom.remove(existing.itemName);
                    updatedBom.put(newName, recipe);
                    TowHitchCore.saveInventory(updated);
                    TowHitchCore.saveBOM(updatedBom);
                    TowHitchCore.appendHistory("Edited product: " + existing.itemName
                            + " -> " + newName + " (Qty: " + parsedQuantity
                            + ", Min Alert: " + parsedMin + ")");
                }
                dialog.dispose();
                refreshApplication();
            } catch (IOException e) {
                showDialogError(dialog, "Unable to save: " + e.getMessage());
            }
        });
        actions.add(cancel);
        actions.add(save);
        form.add(actions);
        dialog.setVisible(true);
    }

    private void confirmDeleteProduct(TowHitchCore.InventoryItem item) {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Move '" + item.itemName + "' to Trash? It can be restored for 30 days.",
                "Move Product to Trash", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            Map<String, List<TowHitchCore.BOMItem>> bom = TowHitchCore.loadBOM();
            List<TowHitchCore.BOMItem> recipe = bom.getOrDefault(
                    item.itemName, new ArrayList<>());
            StringBuilder serializedRecipe = new StringBuilder("PRODUCT\n");
            for (TowHitchCore.BOMItem component : recipe) {
                serializedRecipe.append(component.materialName).append('\t')
                        .append(component.quantityRequired).append('\n');
            }
            String extraData = java.util.Base64.getEncoder().encodeToString(
                    serializedRecipe.toString().getBytes(StandardCharsets.UTF_8));
            TowHitchCore.addToTrash("F", item.itemName,
                    item.quantity, item.minAlert, extraData, activeUser.fullName);
            inventory.removeIf(current -> "F".equals(current.type)
                    && current.itemName.equalsIgnoreCase(item.itemName));
            bom.remove(item.itemName);
            TowHitchCore.saveInventory(inventory);
            TowHitchCore.saveBOM(bom);
            TowHitchCore.appendHistory("Deleted product: " + item.itemName);
            refreshApplication();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to delete product: " + e.getMessage(),
                    "Inventory Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleBOMPreview(TowHitchCore.RoundedPanel row,
                                  TowHitchCore.InventoryItem item,
                                  TowHitchCore.ChevronButton toggle) {
        Component south = ((BorderLayout) row.getLayout()).getLayoutComponent(
                BorderLayout.SOUTH);
        if (!toggle.isExpanded()) {
            if (south != null) {
                row.remove(south);
            }
            row.revalidate();
            row.repaint();
            return;
        }
        if (south != null) {
            row.remove(south);
        }
        JPanel preview = new JPanel();
        preview.setOpaque(false);
        preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
        List<TowHitchCore.BOMItem> recipe;
        try {
            recipe = TowHitchCore.loadBOM().getOrDefault(
                    item.itemName, new ArrayList<TowHitchCore.BOMItem>());
        } catch (IOException e) {
            showLoadError("the product BOM", e);
            toggle.setExpanded(false);
            return;
        }
        if (recipe.isEmpty()) {
            JLabel empty = new JLabel("No BOM recipe configured.");
            empty.setForeground(theme.secondaryText());
            preview.add(empty);
        } else {
            TowHitchCore.RoundedPanel ingredients = new TowHitchCore.RoundedPanel(12);
            ingredients.setLayout(new GridLayout(0, 3, 8, 4));
            ingredients.setBackground(theme.isDark()
                    ? new Color(0x22252C) : new Color(0xF8F6F1));
            ingredients.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            for (TowHitchCore.BOMItem bomItem : recipe) {
                boolean insufficient = true;
                for (TowHitchCore.InventoryItem material : loadInventorySafely()) {
                    if (material.itemName.equalsIgnoreCase(bomItem.materialName)
                            && material.quantity >= bomItem.quantityRequired) {
                        insufficient = false;
                        break;
                    }
                }
                JLabel line = new JLabel(bomItem.materialName + " \u00D7 "
                        + bomItem.quantityRequired);
                line.setForeground(insufficient ? theme.danger() : theme.secondaryText());
                ingredients.add(line);
            }
            preview.add(ingredients);
        }
        row.add(preview, BorderLayout.SOUTH);
        row.revalidate();
        row.repaint();
    }

    private void performInventorySearch(String query) {
        searchHighlightName = query;
        if (query.isEmpty()) {
            refreshApplication();
            return;
        }
        TowHitchCore.InventoryItem found = null;
        for (TowHitchCore.InventoryItem item : loadInventorySafely()) {
            if (item.itemName.toLowerCase().contains(query.toLowerCase())) {
                found = item;
                break;
            }
        }
        if (found == null) {
            JOptionPane.showMessageDialog(frame,
                    "No inventory item matches \"" + query + "\".",
                    "Search", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        currentPage = "F".equals(found.type)
                ? "Finished Goods" : "Raw Materials";
        refreshApplication();
    }

    private boolean matchesHighlight(String itemName) {
        return !searchHighlightName.isEmpty()
                && itemName.toLowerCase().contains(searchHighlightName.toLowerCase());
    }

    private void refreshApplication() {
        buildApplication();
        SwingUtilities.invokeLater(() -> {
            if (searchHighlightName.isEmpty()) {
                return;
            }
            Component[] components = applicationRoot.getComponents();
            highlightAndScroll(components);
        });
    }

    private void highlightAndScroll(Component[] components) {
        for (Component component : components) {
            if (component instanceof JComponent) {
                JComponent jComponent = (JComponent) component;
                if (jComponent.getName() != null
                        && jComponent.getName().toLowerCase()
                                .contains(searchHighlightName.toLowerCase())) {
                    jComponent.scrollRectToVisible(jComponent.getBounds());
                }
            }
            if (component instanceof java.awt.Container) {
                highlightAndScroll(((java.awt.Container) component).getComponents());
            }
        }
    }

    private void createBackup() {
        try {
            String stamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupDirectory = Paths.get("backups").resolve(stamp).toAbsolutePath();
            Files.createDirectories(backupDirectory);
            String[] files = {
                    "users.txt",
                    "inventory_data.txt",
                    "stock_requests.txt",
                    "customer_orders.txt",
                    "history_logs.txt",
                    "production_logs.txt"
            };
            for (String fileName : files) {
                Path source = Paths.get(fileName).toAbsolutePath();
                if (Files.isRegularFile(source)) {
                    Files.copy(source, backupDirectory.resolve(fileName),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            JOptionPane.showMessageDialog(frame,
                    "Backup created at:\n" + backupDirectory,
                    "Data Backup", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to create backup: " + e.getMessage(),
                    "Data Backup", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        activeUser = null;
        currentPage = "Dashboard";
        showLogin();
    }

    private List<TowHitchCore.InventoryItem> loadInventorySafely() {
        try {
            return TowHitchCore.loadInventory();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to load inventory_data.txt: " + e.getMessage(),
                    "Inventory Error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    private int countPendingStockRequests() {
        try {
            int count = 0;
            for (TowHitchCore.StockRequest request : TowHitchCore.loadStockRequests()) {
                if ("PENDING".equals(request.status)) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    private int countPendingCustomerOrders() {
        try {
            int count = 0;
            for (TowHitchCore.CustomerOrder order : TowHitchCore.loadCustomerOrders()) {
                if ("PENDING".equals(order.status)) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    private int countPendingLines(String fileName) {
        Path file = Paths.get(fileName).toAbsolutePath();
        if (!Files.isRegularFile(file)) {
            return 0;
        }
        try {
            int count = 0;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    private static final class HistoryLogRecord {
        private final String message;
        private final String display;
        private final String date;
        private final String time;

        private HistoryLogRecord(String message, String display, String date, String time) {
            this.message = message;
            this.display = display;
            this.date = date;
            this.time = time;
        }
    }

}
