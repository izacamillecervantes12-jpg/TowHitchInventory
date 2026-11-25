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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private TowHitchCore.UserAccount activeUser;
    private LoginBackgroundPanel loginBackground;
    private JLabel loginTitle;
    private JLabel loginSubtitle;
    private JLabel loginMessage;
    private JLabel loginFooter;
    private JPopupMenu notificationPopup;
    private JPopupMenu settingsPopup;
    private TowHitchCore.FlatIconButton notificationButton;
    private String currentPage = "Dashboard";
    private String searchHighlightName = "";
    private boolean sidebarExpanded = true;
    private final String[] navigationItems = {
            "Dashboard",
            "Unfinished Goods",
            "Finished Goods",
            "Stock Requests",
            "Customer Orders",
            "History / Archive"
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
        showLogin();
        frame.setVisible(true);
    }

    private void showLogin() {
        LoginBackgroundPanel root = new LoginBackgroundPanel();
        root.setFocusable(true);
        loginBackground = root;

        GlassLoginCard card = new GlassLoginCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(0, 61, 12, 61));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
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

        loginSubtitle = new JLabel("Access your owner dashboard", SwingConstants.CENTER);
        loginSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 20));
        loginSubtitle.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x625E5C));
        loginSubtitle.setPreferredSize(new Dimension(394, 25));
        loginSubtitle.setMaximumSize(new Dimension(394, 25));
        addLoginGridRow(content, loginSubtitle, 2, 0, 5, 0, GridBagConstraints.CENTER);

        JLabel usernameLabel = loginLabel("Username");
        LoginTextField username = new LoginTextField("Enter your username");
        JLabel passwordLabel = loginLabel("Password");
        LoginPasswordField password = new LoginPasswordField("Enter password");
        addLoginGridRow(content, Box.createVerticalStrut(6), 3, 0, 0, 0,
                GridBagConstraints.CENTER);
        addLoginGridRow(content, usernameLabel, 4, 31, 5, 0, GridBagConstraints.WEST);
        addLoginGridRow(content, username, 5, 0, 15, 16, GridBagConstraints.WEST);
        addLoginGridRow(content, passwordLabel, 6, 5, 5, 0, GridBagConstraints.WEST);
        addLoginGridRow(content, password, 7, 0, 0, 16, GridBagConstraints.WEST);

        loginMessage = new JLabel(" ");
        loginMessage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        loginMessage.setForeground(theme.danger());
        loginMessage.setPreferredSize(new Dimension(394, 25));
        loginMessage.setMaximumSize(new Dimension(394, 25));
        addLoginGridRow(content, loginMessage, 8, 0, 0, 0, GridBagConstraints.WEST);

        LoginButton signIn = new LoginButton("Log in");
        signIn.setMaximumSize(new Dimension(394, 72));
        signIn.setPreferredSize(new Dimension(394, 52));
        signIn.addActionListener(event -> authenticateOwner(username, password));
        password.addActionListener(event -> authenticateOwner(username, password));
        addLoginGridRow(content, signIn, 9, 35, 10, 20, GridBagConstraints.CENTER);

        Dimension contentSize = content.getPreferredSize();
        contentSize.width = 394;
        content.setPreferredSize(contentSize);
        content.setMaximumSize(contentSize);
        loginFooter = new JLabel("Tow Hitch by 5-21 | Owner Portal", SwingConstants.CENTER);
        loginFooter.setFont(new Font("SansSerif", Font.PLAIN, 16));
        loginFooter.setForeground(theme.isDark() ? new Color(0xC6BFBA) : new Color(0x686361));
        loginFooter.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        loginFooter.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        loginFooter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        card.add(content);
        card.add(Box.createVerticalGlue());
        card.add(loginFooter);
        card.setPreferredSize(new Dimension(516, 570));
        root.setLoginComponents(card, new ThemeSwitchButton());
        frame.setContentPane(root);
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

    private void addLoginGridRow(JPanel panel, Component component, int row,
                                 int topInset, int bottomInset, int verticalPadding,
                                 int anchor) {
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
                loginMessage.setText("Invalid username or password.");
            } else if (!"OWNER".equals(found.role)) {
                loginMessage.setText(
                        "Access denied: This portal is restricted to Owner accounts only.");
            } else {
                activeUser = found;
                buildApplication();
            }
        } catch (IOException e) {
            loginMessage.setText("Unable to read users.txt: " + e.getMessage());
        }
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
            Color fieldColor = theme.isDark()
                    ? new Color(0xB6B1AA) : new Color(0xF4EFE6);
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
            setPreferredSize(new Dimension(394, 51));
            setMaximumSize(new Dimension(394, 67));
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
            int width = getWidth();
            int height = getHeight();
            g.setColor(theme.isDark() ? new Color(0x34312F) : theme.sidebar());
            g.fillRoundRect(0, 1, width, height - 2, height - 2, height - 2);

            int knobSize = height - 10;
            int travel = width - knobSize - 10;
            int knobX = 5 + Math.round(travel * position);
            g.setColor(theme.isDark() ? new Color(0x303856) : Color.WHITE);
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

    private void showSettingsPopup(Component invoker) {
        if (settingsPopup != null && settingsPopup.isVisible()) {
            settingsPopup.setVisible(false);
            return;
        }
        JPopupMenu popup = new RoundedNotificationPopup();
        popup.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel content = popupContent();
        JMenuItem backup = new JMenuItem("Data Backup");
        JMenuItem logout = new JMenuItem("Logout");
        stylePopupItem(backup);
        stylePopupItem(logout);
        backup.addActionListener(event -> createBackup());
        logout.addActionListener(event -> logout());
        content.add(backup);
        content.add(logout);
        popup.add(content);
        settingsPopup = popup;
        popup.show(invoker, 0, invoker.getHeight());
    }

    private void showNotificationsPopup(Component invoker) {
        if (notificationPopup != null && notificationPopup.isVisible()) {
            notificationPopup.setVisible(false);
            return;
        }
        JPopupMenu popup = new RoundedNotificationPopup();
        popup.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel content = popupContent();
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        heading.setOpaque(false);
        JLabel warningDot = new JLabel("\u25CF");
        warningDot.setForeground(theme.danger());
        warningDot.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel headingText = new JLabel("Low Stocks Alert");
        headingText.setFont(new Font("SansSerif", Font.BOLD, 14));
        headingText.setForeground(theme.text());
        heading.add(warningDot);
        heading.add(headingText);
        heading.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        content.add(heading);

        JPanel items = new JPanel();
        items.setOpaque(false);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        int count = 0;
        for (TowHitchCore.InventoryItem item : loadInventorySafely()) {
            if (item.quantity <= item.minAlert) {
                String category = "F".equals(item.type) ? "Finished Goods" : "Unfinished Goods";
                TowHitchCore.RoundedPanel alert = new TowHitchCore.RoundedPanel(10);
                alert.setLayout(new BorderLayout(8, 0));
                alert.setBackground(theme.row());
                alert.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                JPanel labels = new JPanel();
                labels.setOpaque(false);
                labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
                JLabel itemName = new JLabel(item.itemName);
                itemName.setFont(new Font("SansSerif", Font.BOLD, 13));
                itemName.setForeground(theme.text());
                JLabel categoryLabel = new JLabel(category);
                categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
                categoryLabel.setForeground(theme.secondaryText());
                labels.add(itemName);
                labels.add(categoryLabel);
                JLabel quantity = new JLabel(item.quantity + " on hand (min alert: "
                        + item.minAlert + ")");
                quantity.setForeground(theme.danger());
                quantity.setFont(new Font("SansSerif", Font.BOLD, 11));
                alert.add(labels, BorderLayout.CENTER);
                alert.add(quantity, BorderLayout.EAST);
                alert.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
                items.add(alert);
                items.add(Box.createVerticalStrut(5));
                count++;
            }
        }
        if (count == 0) {
            JLabel clear = new JLabel("No low-stock items.");
            clear.setForeground(theme.success());
            clear.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            items.add(clear);
        }
        JScrollPane scroll = createInventoryScroll(items);
        scroll.setPreferredSize(new Dimension(330, Math.min(320, Math.max(60, count * 58 + 16))));
        content.add(scroll);
        popup.add(content);
        notificationPopup = popup;
        popup.show(invoker, 0, invoker.getHeight());
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
        }

        @Override
        protected void paintComponent(Graphics graphics) {
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
            g.setColor(theme.input());
            g.fillRoundRect(0, 0, getWidth(), getHeight(),
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
        applicationRoot.setLayout(new BorderLayout(14, 0));
        applicationRoot.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel mainArea = new JPanel(new BorderLayout(0, 16));
        mainArea.setOpaque(false);
        mainArea.add(buildTopBar(), BorderLayout.NORTH);

        pageHost = new JPanel(new CardLayout());
        pageHost.setOpaque(false);
        for (String page : navigationItems) {
            JPanel pagePanel;
            if ("Dashboard".equals(page)) {
                pagePanel = buildDashboard();
            } else if ("Unfinished Goods".equals(page)) {
                pagePanel = buildUnfinishedGoodsPage();
            } else if ("Finished Goods".equals(page)) {
                pagePanel = buildFinishedGoodsPage();
            } else if ("Stock Requests".equals(page)) {
                pagePanel = buildStockRequestsPage();
            } else if ("Customer Orders".equals(page)) {
                pagePanel = buildCustomerOrdersPage();
            } else if ("History / Archive".equals(page)) {
                pagePanel = buildHistoryPage("");
            } else {
                pagePanel = buildPlaceholderPage(page);
            }
            pageHost.add(pagePanel, page);
        }
        mainArea.add(pageHost, BorderLayout.CENTER);

        if (sidebarExpanded) {
            applicationRoot.add(buildSidebar(), BorderLayout.WEST);
        }
        applicationRoot.add(mainArea, BorderLayout.CENTER);
        frame.setContentPane(applicationRoot);
        showPage(currentPage);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel buildTopBar() {
        TowHitchCore.RoundedPanel top = new TowHitchCore.RoundedPanel(20);
        top.setLayout(new BorderLayout(12, 0));
        top.setBackground(theme.panel());
        top.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JPanel searchArea = new JPanel(new BorderLayout(10, 0));
        searchArea.setOpaque(false);
        TowHitchCore.FlatIconButton menuButton = new TowHitchCore.FlatIconButton(
                "\u2630", "Navigation", theme);
        menuButton.setIconButtonSize(30);
        menuButton.addActionListener(event -> {
            sidebarExpanded = !sidebarExpanded;
            buildApplication();
        });
        searchArea.add(menuButton, BorderLayout.WEST);
        RoundedSearchPanel searchPanel = new RoundedSearchPanel();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.setPreferredSize(new Dimension(360, 42));
        TowHitchCore.RoundedTextField search = new TowHitchCore.RoundedTextField(
                "Search", 26, theme);
        search.setText(searchHighlightName);
        search.addActionListener(event -> performInventorySearch(search.getText().trim()));
        searchPanel.add(buildSearchLeading(), BorderLayout.WEST);
        searchPanel.add(search, BorderLayout.CENTER);
        searchArea.add(searchPanel, BorderLayout.CENTER);
        top.add(searchArea, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        actions.setOpaque(false);

        JLabel roleBadge = new JLabel("OWNER \u2022 " + activeUser.fullName);
        roleBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        roleBadge.setForeground(theme.success());
        actions.add(roleBadge);

        ThemeSwitchButton themeButton = new ThemeSwitchButton();
        themeButton.setToolTipText("Toggle light and dark themes");
        actions.add(themeButton);

        notificationButton = new TowHitchCore.FlatIconButton(
                TowHitchCore.loadTintedIcon(notificationIcon, 24, theme.text()),
                "Inventory low-stock notifications", theme);
        notificationButton.setIconButtonSize(24);
        notificationButton.setUnreadCount(getUnreadLowStockCount());
        notificationButton.addActionListener(event -> showNotificationsPopup(notificationButton));

        TowHitchCore.FlatIconButton settings = new TowHitchCore.FlatIconButton(
                TowHitchCore.loadTintedIcon(settingsIcon, 24, theme.text()),
                "Settings", theme);
        settings.setIconButtonSize(24);
        settings.addActionListener(event -> showSettingsPopup(settings));

        actions.add(notificationButton);
        actions.add(settings);
        top.add(actions, BorderLayout.EAST);
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
        TowHitchCore.RoundedPanel sidebar = new TowHitchCore.RoundedPanel(24);
        sidebar.setPreferredSize(new Dimension(220, 100));
        sidebar.setLayout(new BorderLayout(0, 20));
        sidebar.setBackground(theme.sidebar());
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 18, 16));

        JPanel top = new JPanel(new BorderLayout(0, 24));
        top.setOpaque(false);
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setOpaque(false);
        ImageIcon sidebarLogo = theme.isDark() ? sidebarLogoDark : sidebarLogoLight;
        if (sidebarLogo != null) {
            brand.add(new JLabel(TowHitchCore.scaleToFitIcon(sidebarLogo, 172, 112)));
        }
        top.add(brand, BorderLayout.NORTH);

        JPanel navigation = new JPanel(new GridLayout(0, 1, 0, 8));
        navigation.setOpaque(false);
        for (String item : navigationItems) {
            ImageIcon icon = navigationIcon(item);
            boolean selected = item.equals(currentPage);
            Color foreground = selected ? Color.WHITE : theme.secondaryText();
            TowHitchCore.RoundedButton button = new TowHitchCore.RoundedButton(item,
                    selected ? theme.primaryButton() : theme.sidebar(), foreground, 12, theme);
            button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 14));
            button.setPreferredSize(new Dimension(184, 42));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            if (icon != null) {
                button.setIcon(TowHitchCore.loadTintedIcon(icon, 20, foreground));
                button.setIconTextGap(10);
            }
            button.addActionListener(event -> {
                currentPage = item;
                buildApplication();
            });
            navigation.add(button);
        }
        top.add(navigation, BorderLayout.CENTER);
        sidebar.add(top, BorderLayout.CENTER);

        JPanel logoutArea = new JPanel(new GridBagLayout());
        logoutArea.setOpaque(false);
        TowHitchCore.RoundedButton logout = new TowHitchCore.RoundedButton(
                "Logout", theme.sidebar(), theme.secondaryText(), 12, theme);
        logout.setFont(new Font("SansSerif", Font.PLAIN, 14));
        logout.setPreferredSize(new Dimension(184, 42));
        logout.addActionListener(event -> logout());
        logoutArea.add(logout);
        sidebar.add(logoutArea, BorderLayout.SOUTH);
        return sidebar;
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

    private void showPage(String page) {
        currentPage = page;
        CardLayout layout = (CardLayout) pageHost.getLayout();
        layout.show(pageHost, page);
    }

    private JPanel buildDashboard() {
        JPanel dashboard = new JPanel(new BorderLayout(0, 16));
        dashboard.setOpaque(false);

        JLabel heading = new JLabel("Owner Dashboard");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(theme.text());
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
        cards.add(summaryCard("Unfinished Goods",
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
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new TowHitchCore.MinimalScrollBarUI(theme));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAlertRow(TowHitchCore.InventoryItem item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        String category = "F".equals(item.type) ? "Finished" : "Unfinished";
        JLabel name = new JLabel(item.itemName + "  ");
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        TowHitchCore.StatusPillBadge categoryBadge =
                new TowHitchCore.StatusPillBadge(category, theme);
        JPanel itemLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        itemLabel.setOpaque(false);
        itemLabel.add(name);
        itemLabel.add(categoryBadge);
        JLabel quantity = new JLabel(item.quantity + " stocks (Min: " + item.minAlert + ")");
        quantity.setFont(new Font("SansSerif", Font.PLAIN, 13));
        quantity.setForeground(theme.danger());
        row.add(itemLabel, BorderLayout.WEST);
        row.add(quantity, BorderLayout.EAST);
        row.add(new javax.swing.JSeparator(), BorderLayout.SOUTH);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return row;
    }

    private JPanel buildUnfinishedGoodsPage() {
        TowHitchCore.RoundedPanel panel = new TowHitchCore.RoundedPanel(18);
        panel.setBackground(theme.panel());
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Unfinished Goods");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        header.add(title, BorderLayout.WEST);
        TowHitchCore.RoundedButton add = new TowHitchCore.RoundedButton(
                "+ Add Material", theme.primaryButton(), Color.WHITE, 12, theme);
        add.setPreferredSize(new Dimension(170, 42));
        add.addActionListener(event -> openMaterialDialog(null));
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerActions.add(add);
        header.add(headerActions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (TowHitchCore.InventoryItem item : loadInventorySafely()) {
            if ("U".equals(item.type) || "R".equals(item.type)) {
                rows.add(buildMaterialRow(item));
                rows.add(Box.createVerticalStrut(8));
            }
        }
        if (rows.getComponentCount() == 0) {
            JLabel empty = new JLabel("No unfinished goods have been added.");
            empty.setForeground(theme.secondaryText());
            empty.setBorder(BorderFactory.createEmptyBorder(14, 4, 14, 4));
            rows.add(empty);
        }
        JScrollPane scroll = createInventoryScroll(rows);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMaterialRow(TowHitchCore.InventoryItem item) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setName(item.itemName);
        row.setBackground(matchesHighlight(item.itemName) ? theme.accent() : theme.row());
        row.setLayout(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JLabel name = new JLabel(item.itemName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());

        JLabel quantities = new JLabel("Qty: " + item.quantity + "    Min Alert: " + item.minAlert);
        quantities.setFont(new Font("SansSerif", Font.PLAIN, 12));
        quantities.setForeground(theme.secondaryText());
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(name);
        details.add(quantities);
        row.add(details, BorderLayout.CENTER);

        boolean low = item.quantity <= item.minAlert;
        row.add(new TowHitchCore.StatusPillBadge(
                low ? "LOW STOCK" : "IN STOCK", theme), BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        JButton minus = compactButton("-");
        JButton plus = compactButton("+");
        JButton edit = compactButton("Edit");
        JButton delete = compactButton("Delete");
        minus.addActionListener(event -> adjustQuantity(item, -1));
        plus.addActionListener(event -> adjustQuantity(item, 1));
        edit.addActionListener(event -> openMaterialDialog(item));
        delete.addActionListener(event -> confirmDeleteMaterial(item));
        controls.add(minus);
        controls.add(plus);
        controls.add(edit);
        controls.add(delete);
        row.add(controls, BorderLayout.EAST);
        return row;
    }

    private JPanel buildFinishedGoodsPage() {
        TowHitchCore.RoundedPanel panel = new TowHitchCore.RoundedPanel(18);
        panel.setBackground(theme.panel());
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Finished Goods");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        header.add(title, BorderLayout.WEST);
        TowHitchCore.RoundedButton add = new TowHitchCore.RoundedButton(
                "+ Add Product", theme.primaryButton(), Color.WHITE, 12, theme);
        add.setPreferredSize(new Dimension(170, 42));
        add.addActionListener(event -> openProductDialog(null));
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerActions.add(add);
        header.add(headerActions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

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
        panel.add(createInventoryScroll(rows), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFinishedRow(TowHitchCore.InventoryItem item) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setName(item.itemName);
        row.setBackground(matchesHighlight(item.itemName) ? theme.accent() : theme.row());
        row.setLayout(new BorderLayout(10, 6));
        row.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        JLabel name = new JLabel(item.itemName);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(theme.text());
        JLabel quantities = new JLabel("On Hand: " + item.quantity
                + "    Min Alert: " + item.minAlert);
        quantities.setFont(new Font("SansSerif", Font.PLAIN, 12));
        quantities.setForeground(theme.secondaryText());
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(name);
        details.add(quantities);
        row.add(details, BorderLayout.CENTER);

        boolean low = item.quantity <= item.minAlert;
        row.add(new TowHitchCore.StatusPillBadge(
                low ? "LOW STOCK" : "IN STOCK", theme), BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        JButton edit = compactButton("Edit");
        JButton delete = compactButton("Delete");
        JButton bom = compactButton("Bill of Materials");
        edit.addActionListener(event -> openProductDialog(item));
        delete.addActionListener(event -> confirmDeleteProduct(item));
        bom.addActionListener(event -> toggleBOMPreview(row, item));
        controls.add(bom);
        controls.add(edit);
        controls.add(delete);
        row.add(controls, BorderLayout.EAST);
        return row;
    }

    private JScrollPane createInventoryScroll(JPanel rows) {
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setUI(new TowHitchCore.MinimalScrollBarUI(theme));
        return scroll;
    }

    private TowHitchCore.RoundedButton compactButton(String text) {
        Color fill = theme.input();
        Color foreground = theme.text();
        if ("Approve".equals(text) || "Confirm & Release".equals(text)) {
            fill = new Color(0x16A34A);
            foreground = Color.WHITE;
        } else if ("Reject".equals(text) || "Cancel Order".equals(text)
                || "Delete".equals(text)) {
            fill = new Color(0xDC2626);
            foreground = Color.WHITE;
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
        panel.add(title, BorderLayout.NORTH);

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
        JPanel list = new JPanel(new BorderLayout(0, 6));
        list.setOpaque(false);
        list.add(transactionHeader(new String[] {
                "Status", "Material", "Qty", "Requested By", "Date", "Actions"
        }), BorderLayout.NORTH);
        list.add(createInventoryScroll(rows), BorderLayout.CENTER);
        panel.add(list, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStockRequestRow(TowHitchCore.StockRequest request) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setBackground(theme.row());
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        addTransactionCell(row, statusPill(request.status), 0, 0.15);
        addTransactionCell(row, tableCell(request.materialName), 1, 0.22);
        addTransactionCell(row, tableCell(Integer.toString(request.quantity)), 2, 0.07);
        addTransactionCell(row, tableCell(request.requestedBy), 3, 0.17);
        addTransactionCell(row, tableCell(formatTransactionDate(request.timestamp)), 4, 0.15);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setOpaque(false);
        if ("PENDING".equals(request.status)) {
            TowHitchCore.RoundedButton approve = compactButton("Approve");
            TowHitchCore.RoundedButton reject = compactButton("Reject");
            approve.addActionListener(event -> approveStockRequest(request));
            reject.addActionListener(event -> rejectStockRequest(request));
            controls.add(approve);
            controls.add(reject);
        }
        addTransactionCell(row, controls, 5, 0.24);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private JPanel transactionHeader(String[] labels) {
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(true);
        header.setBackground(theme.sidebar());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        double[] weights = {0.15, 0.22, 0.07, 0.17, 0.15, 0.24};
        for (int i = 0; i < labels.length; i++) {
            JLabel label = tableCell(labels[i]);
            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setForeground(theme.secondaryText());
            addTransactionCell(header, label, i, weights[i]);
        }
        return header;
    }

    private JLabel tableCell(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(theme.text());
        return label;
    }

    private void addTransactionCell(JPanel row, Component component, int column, double weight) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = weight;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 4, 0, 4);
        if (column == 0) {
            constraints.anchor = GridBagConstraints.CENTER;
        } else if (column == 2) {
            constraints.anchor = GridBagConstraints.CENTER;
        } else if (column == 5) {
            constraints.anchor = GridBagConstraints.EAST;
        }
        row.add(component, constraints);
    }

    private String formatTransactionDate(String timestamp) {
        if (timestamp == null) {
            return "";
        }
        int space = timestamp.indexOf(' ');
        return space > 0 ? timestamp.substring(0, space) : timestamp;
    }

    private void approveStockRequest(TowHitchCore.StockRequest request) {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Approve restock of " + request.quantity + " " + request.materialName
                        + " requested by " + request.requestedBy + "?",
                "Approve Stock Request", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            boolean found = false;
            List<TowHitchCore.InventoryItem> updated = new ArrayList<>();
            for (TowHitchCore.InventoryItem item : inventory) {
                if (("U".equals(item.type) || "R".equals(item.type))
                        && item.itemName.equalsIgnoreCase(request.materialName)) {
                    updated.add(new TowHitchCore.InventoryItem(
                            "R", item.itemName, item.quantity + request.quantity,
                            item.minAlert));
                    found = true;
                } else {
                    updated.add(item);
                }
            }
            if (!found) {
                JOptionPane.showMessageDialog(frame,
                        "The requested material does not exist in inventory.",
                        "Inventory Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<TowHitchCore.StockRequest> requests = TowHitchCore.loadStockRequests();
            for (TowHitchCore.StockRequest current : requests) {
                if (current.requestId.equals(request.requestId)) {
                    current.setStatus("APPROVED");
                }
            }
            TowHitchCore.saveInventory(updated);
            TowHitchCore.saveStockRequests(requests);
            TowHitchCore.appendHistory("Approved stock request: " + request.quantity
                    + " " + request.materialName + " requested by " + request.requestedBy
                    + " - Approved by Owner " + activeUser.fullName);
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
        panel.add(title, BorderLayout.NORTH);

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
        list.add(transactionHeader(new String[] {
                "Status", "Product", "Qty", "Customer", "Order Date", "Actions"
        }), BorderLayout.NORTH);
        list.add(createInventoryScroll(rows), BorderLayout.CENTER);
        panel.add(list, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCustomerOrderRow(TowHitchCore.CustomerOrder order) {
        TowHitchCore.RoundedPanel row = new TowHitchCore.RoundedPanel(12);
        row.setBackground(theme.row());
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        addTransactionCell(row, statusPill(order.status), 0, 0.15);
        addTransactionCell(row, tableCell(order.productName), 1, 0.22);
        addTransactionCell(row, tableCell(Integer.toString(order.quantity)), 2, 0.07);
        addTransactionCell(row, tableCell(order.customerName), 3, 0.17);
        addTransactionCell(row, tableCell(formatTransactionDate(order.timestamp)), 4, 0.15);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setOpaque(false);
        if ("PENDING".equals(order.status)) {
            TowHitchCore.RoundedButton confirm = compactButton("Confirm & Release");
            TowHitchCore.RoundedButton cancel = compactButton("Cancel Order");
            confirm.addActionListener(event -> confirmCustomerOrder(order));
            cancel.addActionListener(event -> cancelCustomerOrder(order));
            controls.add(confirm);
            controls.add(cancel);
        }
        addTransactionCell(row, controls, 5, 0.24);
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
                    + " - Confirmed by Owner " + activeUser.fullName);
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

    private JPanel buildHistoryPage(String initialFilter) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("History / Archive");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(theme.text());
        JLabel subtitle = new JLabel(
                "Production, material consumption, restock, and order records.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(theme.secondaryText());
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);
        header.add(heading, BorderLayout.CENTER);

        TowHitchCore.RoundedTextField search =
                new TowHitchCore.RoundedTextField("Search audit log", 20, theme);
        search.setPreferredSize(new Dimension(250, 42));
        search.setText(initialFilter);
        header.add(search, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);
        panel.add(cards, BorderLayout.CENTER);
        Runnable refresh = () -> {
            cards.removeAll();
            List<HistoryLogRecord> productAndOrder = new ArrayList<>();
            List<HistoryLogRecord> materialAndAudit = new ArrayList<>();
            for (HistoryLogRecord record : readHistoryRecords()) {
                if (!search.getText().trim().isEmpty()
                        && !record.message.toLowerCase().contains(
                                search.getText().trim().toLowerCase())) {
                    continue;
                }
                if (isProductOrOrderRecord(record.message)) {
                    productAndOrder.add(record);
                } else {
                    materialAndAudit.add(record);
                }
            }
            cards.add(buildHistoryLogCard("Product & Order Logs", productAndOrder));
            cards.add(buildHistoryLogCard("Material Consumption & Audit", materialAndAudit));
            cards.revalidate();
            cards.repaint();
        };
        search.addActionListener(event -> refresh.run());
        refresh.run();
        return panel;
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
            int quantityStart = produced.lastIndexOf(" (");
            if (quantityStart > 0 && produced.endsWith(")")) {
                String quantity = produced.substring(quantityStart + 2, produced.length() - 1);
                String productName = produced.substring(0, quantityStart).trim();
                records.add(new HistoryLogRecord(message,
                        quantity + " " + productName + " produced", date, time));
                return records;
            }
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

    private boolean isProductOrOrderRecord(String message) {
        String normalized = message.toLowerCase();
        return normalized.contains("produced:")
                || normalized.contains("approved stock request")
                || normalized.contains("completed customer order");
    }

    private TowHitchCore.RoundedPanel buildHistoryLogCard(
            String titleText, List<HistoryLogRecord> records) {
        TowHitchCore.RoundedPanel card = new TowHitchCore.RoundedPanel(24);
        card.setBackground(theme.panel());
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
        row.setBackground(theme.row());
        row.setLayout(new GridBagLayout());
        row.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel description = new JLabel(record.display);
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        description.setForeground(theme.text());
        JLabel timestamp = new JLabel(record.date + " | " + record.time);
        timestamp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
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

    private void openMaterialDialog(TowHitchCore.InventoryItem existing) {
        JDialog dialog = createThemedDialog(
                existing == null ? "Add Material" : "Edit Material", 460, 360);
        JPanel form = (JPanel) dialog.getContentPane();

        JTextField name = new JTextField(existing == null ? "" : existing.itemName, 24);
        JTextField quantity = new JTextField(existing == null ? "0"
                : Integer.toString(existing.quantity), 24);
        JTextField minAlert = new JTextField(existing == null ? "5"
                : Integer.toString(existing.minAlert), 24);

        form.add(dialogFieldLabel("Material Name"));
        form.add(name);
        form.add(dialogFieldLabel("Initial Quantity"));
        form.add(quantity);
        form.add(dialogFieldLabel("Minimum Alert Level"));
        form.add(minAlert);

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
            Integer parsedQuantity;
            Integer parsedMin;
            try {
                parsedQuantity = Integer.parseInt(quantity.getText().trim());
                parsedMin = Integer.parseInt(minAlert.getText().trim());
                if (parsedQuantity < 0 || parsedMin < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                showDialogError(dialog, "Quantity and minimum alert must be non-negative integers.");
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
        form.add(actions);
        dialog.setVisible(true);
    }

    private JDialog createThemedDialog(String title, int width, int height) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(frame);
        JPanel content = new JPanel();
        content.setOpaque(true);
        content.setBackground(theme.panel());
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
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

    private void adjustQuantity(TowHitchCore.InventoryItem item, int change) {
        int next = item.quantity + change;
        if (next < 0) {
            next = 0;
        }
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            List<TowHitchCore.InventoryItem> updated = new ArrayList<>();
            for (TowHitchCore.InventoryItem current : inventory) {
                if (current.itemName.equalsIgnoreCase(item.itemName)
                        && ("U".equals(current.type) || "R".equals(current.type))) {
                    updated.add(new TowHitchCore.InventoryItem(
                            "R", current.itemName, next, current.minAlert));
                } else {
                    updated.add(current);
                }
            }
            TowHitchCore.saveInventory(updated);
            TowHitchCore.appendHistory("Adjusted material: " + item.itemName
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
                "Are you sure you want to delete '" + item.itemName
                        + "'? This action cannot be undone.",
                "Delete Material", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            inventory.removeIf(current -> current.itemName.equalsIgnoreCase(item.itemName)
                    && ("U".equals(current.type) || "R".equals(current.type)));
            TowHitchCore.saveInventory(inventory);
            Map<String, List<TowHitchCore.BOMItem>> bom = TowHitchCore.loadBOM();
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
        JTextField quantity = new JTextField(existing == null ? "0"
                : Integer.toString(existing.quantity), 24);
        JTextField minAlert = new JTextField(existing == null ? "3"
                : Integer.toString(existing.minAlert), 24);

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
        List<JTextField> requiredQuantities = new ArrayList<>();
        for (TowHitchCore.InventoryItem material : loadInventorySafely()) {
            if ("U".equals(material.type) || "R".equals(material.type)) {
                JPanel recipeRow = new JPanel(new BorderLayout(8, 0));
                recipeRow.setOpaque(false);
                Integer required = existingRecipe.get(material.itemName);
                JCheckBox check = new JCheckBox(material.itemName,
                        required != null);
                check.setForeground(theme.text());
                check.setOpaque(false);
                JTextField requiredField = new JTextField(
                        required == null ? "1" : Integer.toString(required), 5);
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
            Integer parsedQuantity;
            Integer parsedMin;
            try {
                parsedQuantity = Integer.parseInt(quantity.getText().trim());
                parsedMin = Integer.parseInt(minAlert.getText().trim());
                if (parsedQuantity < 0 || parsedMin < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                showDialogError(dialog,
                        "Quantity and minimum alert must be non-negative integers.");
                return;
            }
            List<TowHitchCore.BOMItem> recipe = new ArrayList<>();
            try {
                for (int i = 0; i < picks.size(); i++) {
                    if (picks.get(i).isSelected()) {
                        int required = Integer.parseInt(requiredQuantities.get(i).getText().trim());
                        if (required < 0) {
                            throw new NumberFormatException();
                        }
                        recipe.add(new TowHitchCore.BOMItem(
                                picks.get(i).getText(), required));
                    }
                }
            } catch (NumberFormatException e) {
                showDialogError(dialog,
                        "BOM required quantities must be non-negative integers.");
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
                "Are you sure you want to delete '" + item.itemName
                        + "'? This action cannot be undone.",
                "Delete Product", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            List<TowHitchCore.InventoryItem> inventory = TowHitchCore.loadInventory();
            inventory.removeIf(current -> "F".equals(current.type)
                    && current.itemName.equalsIgnoreCase(item.itemName));
            Map<String, List<TowHitchCore.BOMItem>> bom = TowHitchCore.loadBOM();
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
                                  TowHitchCore.InventoryItem item) {
        Component south = ((BorderLayout) row.getLayout()).getLayoutComponent(
                BorderLayout.SOUTH);
        if (south != null) {
            row.remove(south);
            row.revalidate();
            row.repaint();
            return;
        }
        JPanel preview = new JPanel();
        preview.setOpaque(false);
        preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
        List<TowHitchCore.BOMItem> recipe;
        try {
            recipe = TowHitchCore.loadBOM().getOrDefault(
                    item.itemName, new ArrayList<TowHitchCore.BOMItem>());
        } catch (IOException e) {
            recipe = new ArrayList<>();
        }
        if (recipe.isEmpty()) {
            JLabel empty = new JLabel("No BOM recipe configured.");
            empty.setForeground(theme.secondaryText());
            preview.add(empty);
        } else {
            JLabel heading = new JLabel("Bill of Materials");
            heading.setFont(new Font("SansSerif", Font.BOLD, 12));
            heading.setForeground(theme.text());
            preview.add(heading);
            for (TowHitchCore.BOMItem bomItem : recipe) {
                JLabel line = new JLabel(bomItem.materialName
                        + " - " + bomItem.quantityRequired + " per unit");
                line.setForeground(theme.secondaryText());
                preview.add(line);
            }
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
                ? "Finished Goods" : "Unfinished Goods";
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

    private int getUnreadLowStockCount() {
        int count = 0;
        for (TowHitchCore.InventoryItem item : loadInventorySafely()) {
            if (item.quantity <= item.minAlert) {
                count++;
            }
        }
        return count;
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
