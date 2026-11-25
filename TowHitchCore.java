import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

public final class TowHitchCore {
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);

    public static final Path USERS_FILE = Paths.get("users.txt");
    public static final Path INVENTORY_FILE = Paths.get("inventory_data.txt");
    public static final int DEFAULT_MIN_ALERT = 5;

    static {
        try {
            ensureUsersFile();
            ensureStockRequestsFile();
            ensureCustomerOrdersFile();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TowHitchCore() {
    }

    public static final class ThemeController implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean darkMode;

        public void toggle() {
            darkMode = !darkMode;
        }

        public boolean isDark() {
            return darkMode;
        }

        public Color canvasStart() {
            return darkMode ? new Color(0x34312F) : new Color(0xF0E5D8);
        }

        public Color canvasEnd() {
            return darkMode ? new Color(0x34312F) : new Color(0xF8F1E9);
        }

        public Color workspaceCanvas() {
            return darkMode ? new Color(0x1F1C1A) : new Color(0xF2E3CE);
        }

        public Color sidebar() {
            return darkMode ? new Color(0x2D2A26) : new Color(0xF4EFE6);
        }

        public Color dashboardPanel() {
            return darkMode ? new Color(0x34312F) : Color.WHITE;
        }

        public Color panel() {
            return darkMode ? new Color(0x4A4643) : new Color(0xFDFBF7);
        }

        public Color row() {
            return darkMode ? new Color(0x292725) : new Color(0xF8F4EE);
        }

        public Color input() {
            return darkMode ? new Color(0xB6B1AA) : new Color(0xF6F0E5);
        }

        public Color text() {
            return darkMode ? new Color(0xF7F4EF) : new Color(0x23211F);
        }

        public Color secondaryText() {
            return darkMode ? new Color(0xB6B1AA) : new Color(0x7F7A75);
        }

        public Color primaryButton() {
            return darkMode ? new Color(0x242221) : new Color(0x23211F);
        }

        public Color accent() {
            return new Color(0x3A63D6);
        }

        public Color danger() {
            return darkMode ? new Color(0xFF9A8F) : new Color(0xB23A32);
        }

        public Color success() {
            return darkMode ? new Color(0x9BD1A5) : new Color(0x37764A);
        }
    }

    @SuppressWarnings("this-escape")
    public static class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int radius;

        public RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    @SuppressWarnings("this-escape")
    public static final class StatusPillBadge extends RoundedPanel {
        private static final long serialVersionUID = 1L;
        private final ThemeController theme;
        private final String status;

        public StatusPillBadge(String status, ThemeController theme) {
            super(24);
            this.status = status;
            this.theme = theme;
            setLayout(new java.awt.GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            JLabel label = new JLabel(status, SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setForeground(textColor());
            add(label, new java.awt.GridBagConstraints());
            setPreferredSize(new Dimension(label.getPreferredSize().width + 24, 24));
            setMinimumSize(getPreferredSize());
            setMaximumSize(new Dimension(getPreferredSize().width, 24));
        }

        private Color textColor() {
            if ("APPROVED".equals(status) || "COMPLETED".equals(status)
                    || "IN STOCK".equals(status)) {
                return theme.isDark() ? new Color(0x9BD1A5) : new Color(0x047857);
            }
            if ("LOW STOCK".equals(status) || "OUT OF STOCK".equals(status)
                    || "REJECTED".equals(status)
                    || "CANCELLED".equals(status)) {
                return theme.isDark() ? new Color(0xFF9A8F) : new Color(0xB91C1C);
            }
            return theme.isDark() ? new Color(0x9BB8FF) : new Color(0x1D4ED8);
        }

        private Color backgroundColor() {
            if ("APPROVED".equals(status) || "COMPLETED".equals(status)
                    || "IN STOCK".equals(status)) {
                return theme.isDark()
                        ? new Color(0xD8193D33, true) : new Color(0xDCECFDF5, true);
            }
            if ("LOW STOCK".equals(status) || "OUT OF STOCK".equals(status)
                    || "REJECTED".equals(status)
                    || "CANCELLED".equals(status)) {
                return theme.isDark()
                        ? new Color(0xD84A2929, true) : new Color(0xDCFEF2F2, true);
            }
            return theme.isDark()
                    ? new Color(0xD8263650, true) : new Color(0xDCEFF6FF, true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(backgroundColor());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            g.setColor(textColor());
            g.setStroke(new java.awt.BasicStroke(1.0f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            g.dispose();
        }
    }

    @SuppressWarnings("this-escape")
    public static class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        private final Color foreground;
        private final int cornerRadius;
        private final ThemeController theme;

        public RoundedButton(String text, Color fill, Color foreground) {
            this(text, fill, foreground, 12);
        }

        public RoundedButton(String text, Color fill, Color foreground, int cornerRadius) {
            this(text, fill, foreground, cornerRadius, new ThemeController());
        }

        public RoundedButton(String text, Color fill, Color foreground, int cornerRadius,
                             ThemeController theme) {
            super(text);
            this.fill = fill;
            this.foreground = foreground;
            this.cornerRadius = cornerRadius;
            this.theme = theme;
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
            if (getModel().isRollover() && isEnabled()) {
                if (fill.equals(new Color(0x16A34A))) {
                    actualFill = new Color(0x15803D);
                } else if (fill.equals(new Color(0xDC2626))) {
                    actualFill = new Color(0xB91C1C);
                }
            }
            g.setColor(actualFill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            if (getModel().isRollover() && isEnabled()
                    && !fill.equals(new Color(0x16A34A))
                    && !fill.equals(new Color(0xDC2626))) {
                g.setColor(new Color(255, 255, 255, 25));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            }
            g.dispose();
            setForeground(isEnabled() ? foreground : theme.secondaryText());
            super.paintComponent(graphics);
        }
    }

    @SuppressWarnings("this-escape")
    public static class RoundedTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;
        private final ThemeController theme;

        public RoundedTextField(String placeholder, int columns) {
            this(placeholder, columns, new ThemeController());
        }

        public RoundedTextField(String placeholder, int columns, ThemeController theme) {
            super(columns);
            this.placeholder = placeholder;
            this.theme = theme;
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

    @SuppressWarnings("this-escape")
    public static class RoundedPasswordField extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;
        private final ThemeController theme;

        public RoundedPasswordField(String placeholder, int columns) {
            this(placeholder, columns, new ThemeController());
        }

        public RoundedPasswordField(String placeholder, int columns, ThemeController theme) {
            super(columns);
            this.placeholder = placeholder;
            this.theme = theme;
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

    @SuppressWarnings("this-escape")
    public static class FlatIconButton extends JButton {
        private static final long serialVersionUID = 1L;
        private int unreadCount;
        private final ThemeController theme;

        public FlatIconButton(String icon, String tooltip) {
            this(icon, tooltip, new ThemeController());
        }

        public FlatIconButton(Icon icon, String tooltip) {
            this(icon, tooltip, new ThemeController());
        }

        public FlatIconButton(String icon, String tooltip, ThemeController theme) {
            super(icon);
            this.theme = theme;
            initialize(tooltip);
        }

        public FlatIconButton(Icon icon, String tooltip, ThemeController theme) {
            super(icon);
            this.theme = theme;
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

        public void setIconButtonSize(int size) {
            Dimension dimensions = new Dimension(size, size);
            setPreferredSize(dimensions);
            setMinimumSize(dimensions);
            setMaximumSize(dimensions);
            setMargin(new Insets(0, 0, 0, 0));
        }

        public void setUnreadCount(int count) {
            unreadCount = count;
            setToolTipText(count == 0 ? "No low-stock alerts"
                    : count + " low-stock alert" + (count == 1 ? "" : "s"));
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

    public static class MinimalScrollBarUI extends BasicScrollBarUI {
        private final ThemeController theme;

        public MinimalScrollBarUI() {
            this(new ThemeController());
        }

        public MinimalScrollBarUI(ThemeController theme) {
            this.theme = theme;
        }

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
            JScrollBar scrollBar = (JScrollBar) component;
            if (bounds.isEmpty() || !scrollBar.isEnabled()) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color thumb = theme.isDark()
                    ? (isThumbRollover() ? new Color(0x85817C) : new Color(0x4A4643))
                    : (isThumbRollover() ? new Color(0x7F7A75) : new Color(0xB6B1AA));
            g.setColor(thumb);
            if (scrollBar.getOrientation() == JScrollBar.HORIZONTAL) {
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

    @SuppressWarnings("this-escape")
    public static class SelectionCircleButton extends JToggleButton {
        private static final long serialVersionUID = 1L;
        private final ThemeController theme;

        public SelectionCircleButton() {
            this(new ThemeController());
        }

        public SelectionCircleButton(ThemeController theme) {
            this.theme = theme;
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

        public static final class BOMItem {
            public final String materialName;
            public final int quantityRequired;

            public BOMItem(String materialName, int quantityRequired) {
                this.materialName = materialName;
                this.quantityRequired = quantityRequired;
            }

            public String getMaterialName() {
                return materialName;
            }

            public int getQuantityRequired() {
                return quantityRequired;
            }
        }

        public static final class ProductionBatch {
            public final Instant producedAt;
            public final String productName;
            public final int quantity;

            public ProductionBatch(Instant producedAt, String productName, int quantity) {
                this.producedAt = producedAt;
                this.productName = productName;
                this.quantity = quantity;
            }

            public Instant getProducedAt() {
                return producedAt;
            }

            public String getProductName() {
                return productName;
            }

            public int getQuantity() {
                return quantity;
            }
        }

        public static final class HistoryEntry {
            public final String itemText;
            public final String date;
            public final String time;

            public HistoryEntry(String itemText, String date, String time) {
                this.itemText = itemText;
                this.date = date;
                this.time = time;
            }

            public String getItemText() {
                return itemText;
            }

            public String getDate() {
                return date;
            }

            public String getTime() {
                return time;
            }
        }

        public static final class UserAccount {
            public String username;
            public String password;
            public String fullName;
            public String role;

            public UserAccount(String username, String password, String fullName, String role) {
                this.username = username;
                this.password = password;
                this.fullName = fullName;
                this.role = role;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getFullName() {
                return fullName;
            }

            public void setFullName(String fullName) {
                this.fullName = fullName;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }
        }

        public static final class InventoryItem {
            public final String type;
            public final String itemName;
            public final int quantity;
            public final int minAlert;

            public InventoryItem(String type, String itemName, int quantity, int minAlert) {
                this.type = normalizeInventoryType(type);
                this.itemName = itemName;
                this.quantity = quantity;
                this.minAlert = minAlert < 0 ? DEFAULT_MIN_ALERT : minAlert;
            }

            public String getType() {
                return type;
            }

            public String getItemName() {
                return itemName;
            }

            public int getQuantity() {
                return quantity;
            }

            public int getMinAlert() {
                return minAlert;
            }
        }

        public static synchronized void ensureUsersFile() throws IOException {
            Path absoluteUsersFile = USERS_FILE.toAbsolutePath();
            Files.createDirectories(absoluteUsersFile.getParent());
            if (!Files.exists(absoluteUsersFile)) {
                List<UserAccount> defaults = new ArrayList<>();
                defaults.add(new UserAccount("owner", "owner123", "Shop Owner", "OWNER"));
                defaults.add(new UserAccount("staff", "staff123", "Production Staff", "STAFF"));
                defaults.add(new UserAccount("customer", "cust123", "Walk-in Client", "CUSTOMER"));
                saveUsers(defaults);
            }
        }

        public static List<UserAccount> loadUsers() throws IOException {
            ensureUsersFile();
            List<UserAccount> users = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(USERS_FILE.toAbsolutePath(),
                    StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 4) {
                        continue;
                    }
                    String role = fields[3].trim().toUpperCase();
                    if (!role.equals("OWNER") && !role.equals("STAFF") && !role.equals("CUSTOMER")) {
                        continue;
                    }
                    users.add(new UserAccount(fields[0].trim(), fields[1].trim(),
                            fields[2].trim(), role));
                }
            }
            return users;
        }

        public static synchronized void saveUsers(List<UserAccount> users) throws IOException {
            Path absoluteUsersFile = USERS_FILE.toAbsolutePath();
            Files.createDirectories(absoluteUsersFile.getParent());
            Path temporaryFile = Files.createTempFile(absoluteUsersFile.getParent(),
                    "users", ".tmp");
            boolean moved = false;
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                for (UserAccount user : users) {
                    writer.write(user.username + "\t" + user.password + "\t"
                            + user.fullName + "\t" + user.role);
                    writer.newLine();
                }
            }
            try {
                Files.move(temporaryFile, absoluteUsersFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                moved = true;
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, absoluteUsersFile, StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            } finally {
                if (!moved) {
                    Files.deleteIfExists(temporaryFile);
                }
            }
        }

        public static synchronized void initializeInventory() throws IOException {
            if (!Files.exists(INVENTORY_FILE.toAbsolutePath())) {
                saveInventory(new ArrayList<>());
            }
        }

        public static List<InventoryItem> loadInventory() throws IOException {
            initializeInventory();
            List<InventoryItem> inventory = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(INVENTORY_FILE.toAbsolutePath(),
                    StandardCharsets.UTF_8)) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 3 && fields.length != 4) {
                        System.err.println("Ignoring malformed inventory data at line " + lineNumber + ".");
                        continue;
                    }
                    String type = fields[0].trim().toUpperCase();
                    if (!type.equals("U") && !type.equals("R") && !type.equals("F")) {
                        System.err.println("Ignoring malformed inventory data at line " + lineNumber + ".");
                        continue;
                    }
                    String itemName = fields[1].trim();
                    if (itemName.isEmpty()) {
                        System.err.println("Ignoring invalid inventory item at line " + lineNumber + ".");
                        continue;
                    }
                    try {
                        int quantity = Integer.parseInt(fields[2].trim());
                        int minAlert = DEFAULT_MIN_ALERT;
                        if (fields.length == 4) {
                            minAlert = Integer.parseInt(fields[3].trim());
                        }
                        if (quantity < 0 || minAlert < 0) {
                            System.err.println("Ignoring negative inventory value at line " + lineNumber + ".");
                            continue;
                        }
                        inventory.add(new InventoryItem(type, itemName, quantity, minAlert));
                    } catch (NumberFormatException e) {
                        System.err.println("Ignoring invalid inventory value at line " + lineNumber + ".");
                    }
                }
            }
            return inventory;
        }

        public static synchronized void saveInventory(List<InventoryItem> inventory) throws IOException {
            Path absoluteDataFile = INVENTORY_FILE.toAbsolutePath();
            Path parent = absoluteDataFile.getParent();
            Files.createDirectories(parent);
            Path temporaryFile = Files.createTempFile(parent, "inventory_data", ".tmp");
            boolean moved = false;
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                writer.write("# Tow Hitch by 5-21 inventory data. Do not edit while the program is running.");
                writer.newLine();
                for (InventoryItem item : inventory) {
                    writer.write(normalizeInventoryType(item.type) + "\t" + item.itemName
                            + "\t" + item.quantity + "\t"
                            + (item.minAlert < 0 ? DEFAULT_MIN_ALERT : item.minAlert));
                    writer.newLine();
                }
            }
            try {
                Files.move(temporaryFile, absoluteDataFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                moved = true;
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, absoluteDataFile, StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            } finally {
                if (!moved) {
                    Files.deleteIfExists(temporaryFile);
                }
            }
        }

        public static void appendHistory(String message) throws IOException {
                if (message == null || message.trim().isEmpty()) {
                    return;
                }
                Path historyFile = Paths.get("history_logs.txt").toAbsolutePath();
                Files.createDirectories(historyFile.getParent());
                String line = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        + "\t" + message + System.lineSeparator();
                Files.write(historyFile, line.getBytes(StandardCharsets.UTF_8),
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            }

            public static Map<String, List<BOMItem>> loadBOM() throws IOException {
                Map<String, List<BOMItem>> bom = new LinkedHashMap<>();
                Path bomFile = Paths.get("bom_data.txt").toAbsolutePath();
                if (!Files.isRegularFile(bomFile)) {
                    return bom;
                }
                for (String line : Files.readAllLines(bomFile, StandardCharsets.UTF_8)) {
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 3) {
                        continue;
                    }
                    try {
                        String product = fields[0].trim();
                        String material = fields[1].trim();
                        int quantity = Integer.parseInt(fields[2].trim());
                        if (!product.isEmpty() && !material.isEmpty() && quantity >= 0) {
                            bom.computeIfAbsent(product, key -> new ArrayList<>())
                                    .add(new BOMItem(material, quantity));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return bom;
            }

            public static synchronized void saveBOM(Map<String, List<BOMItem>> bom) throws IOException {
                Path bomFile = Paths.get("bom_data.txt").toAbsolutePath();
                Files.createDirectories(bomFile.getParent());
                Path temporaryFile = Files.createTempFile(bomFile.getParent(), "bom_data", ".tmp");
                boolean moved = false;
                try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                    writer.write("# Tow Hitch by 5-21 bill of materials.");
                    writer.newLine();
                    for (Map.Entry<String, List<BOMItem>> entry : bom.entrySet()) {
                        for (BOMItem item : entry.getValue()) {
                            writer.write(entry.getKey() + "\t" + item.materialName
                                    + "\t" + item.quantityRequired);
                            writer.newLine();
                        }
                    }
                }
                try {
                    Files.move(temporaryFile, bomFile, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                    moved = true;
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporaryFile, bomFile, StandardCopyOption.REPLACE_EXISTING);
                    moved = true;
                } finally {
                    if (!moved) {
                        Files.deleteIfExists(temporaryFile);
                    }
                }
            }

        public static final class StockRequest {
            public final String requestId;
            public final String timestamp;
            public final String requestedBy;
            public final String materialName;
            public final int quantity;
            public String status;

            public StockRequest(String requestId, String timestamp, String requestedBy,
                                String materialName, int quantity, String status) {
                this.requestId = requestId;
                this.timestamp = timestamp;
                this.requestedBy = requestedBy;
                this.materialName = materialName;
                this.quantity = quantity;
                this.status = normalizeStockStatus(status);
            }

            public String getRequestId() {
                return requestId;
            }

            public String getTimestamp() {
                return timestamp;
            }

            public String getRequestedBy() {
                return requestedBy;
            }

            public String getMaterialName() {
                return materialName;
            }

            public int getQuantity() {
                return quantity;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = normalizeStockStatus(status);
            }
        }

        public static final class CustomerOrder {
            public final String orderId;
            public final String timestamp;
            public final String customerName;
            public final String productName;
            public final int quantity;
            public String status;

            public CustomerOrder(String orderId, String timestamp, String customerName,
                                 String productName, int quantity, String status) {
                this.orderId = orderId;
                this.timestamp = timestamp;
                this.customerName = customerName;
                this.productName = productName;
                this.quantity = quantity;
                this.status = normalizeOrderStatus(status);
            }

            public String getOrderId() {
                return orderId;
            }

            public String getTimestamp() {
                return timestamp;
            }

            public String getCustomerName() {
                return customerName;
            }

            public String getProductName() {
                return productName;
            }

            public int getQuantity() {
                return quantity;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = normalizeOrderStatus(status);
            }
        }

        private static String normalizeStockStatus(String status) {
            if ("APPROVED".equalsIgnoreCase(status)) {
                return "APPROVED";
            }
            if ("REJECTED".equalsIgnoreCase(status)) {
                return "REJECTED";
            }
            return "PENDING";
        }

        private static String normalizeOrderStatus(String status) {
            if ("COMPLETED".equalsIgnoreCase(status)) {
                return "COMPLETED";
            }
            if ("CANCELLED".equalsIgnoreCase(status)) {
                return "CANCELLED";
            }
            return "PENDING";
        }

        public static synchronized void ensureStockRequestsFile() throws IOException {
            Path file = Paths.get("stock_requests.txt").toAbsolutePath();
            if (Files.isRegularFile(file)) {
                return;
            }
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            List<StockRequest> seeded = new ArrayList<>();
            seeded.add(new StockRequest("SR-001", timestamp, "Production Staff",
                    "Bolts", 20, "PENDING"));
            seeded.add(new StockRequest("SR-002", timestamp, "Production Staff",
                    "Paint", 10, "PENDING"));
            saveStockRequests(seeded);
        }

        public static List<StockRequest> loadStockRequests() throws IOException {
            ensureStockRequestsFile();
            List<StockRequest> requests = new ArrayList<>();
            Path file = Paths.get("stock_requests.txt").toAbsolutePath();
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 6) {
                        continue;
                    }
                    try {
                        int quantity = Integer.parseInt(fields[4].trim());
                        if (quantity >= 0) {
                            requests.add(new StockRequest(fields[0].trim(), fields[1].trim(),
                                    fields[2].trim(), fields[3].trim(), quantity, fields[5].trim()));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return requests;
        }

        public static synchronized void saveStockRequests(List<StockRequest> requests)
                throws IOException {
            Path file = Paths.get("stock_requests.txt").toAbsolutePath();
            atomicMoveTextFile(file, "stock_requests", writer -> {
                for (StockRequest request : requests) {
                    writer.write(request.requestId + "\t" + request.timestamp + "\t"
                            + request.requestedBy + "\t" + request.materialName + "\t"
                            + request.quantity + "\t" + request.status);
                    writer.newLine();
                }
            });
        }

        public static synchronized void ensureCustomerOrdersFile() throws IOException {
            Path file = Paths.get("customer_orders.txt").toAbsolutePath();
            if (Files.isRegularFile(file)) {
                return;
            }
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            List<CustomerOrder> seeded = new ArrayList<>();
            seeded.add(new CustomerOrder("CO-001", timestamp, "Walk-in Customer",
                    "Cargo Basket", 1, "PENDING"));
            seeded.add(new CustomerOrder("CO-002", timestamp, "Walk-in Customer",
                    "Bike Rack", 1, "PENDING"));
            saveCustomerOrders(seeded);
        }

        public static List<CustomerOrder> loadCustomerOrders() throws IOException {
            ensureCustomerOrdersFile();
            List<CustomerOrder> orders = new ArrayList<>();
            Path file = Paths.get("customer_orders.txt").toAbsolutePath();
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 6) {
                        continue;
                    }
                    try {
                        int quantity = Integer.parseInt(fields[4].trim());
                        if (quantity >= 0) {
                            orders.add(new CustomerOrder(fields[0].trim(), fields[1].trim(),
                                    fields[2].trim(), fields[3].trim(), quantity, fields[5].trim()));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return orders;
        }

        public static synchronized void saveCustomerOrders(List<CustomerOrder> orders)
                throws IOException {
            Path file = Paths.get("customer_orders.txt").toAbsolutePath();
            atomicMoveTextFile(file, "customer_orders", writer -> {
                for (CustomerOrder order : orders) {
                    writer.write(order.orderId + "\t" + order.timestamp + "\t"
                            + order.customerName + "\t" + order.productName + "\t"
                            + order.quantity + "\t" + order.status);
                    writer.newLine();
                }
            });
        }

        @FunctionalInterface
        private interface ThrowingBufferedWriterConsumer {
            void accept(BufferedWriter writer) throws IOException;
        }

        private static void atomicMoveTextFile(Path target, String prefix,
                                               ThrowingBufferedWriterConsumer writerAction)
                throws IOException {
            Path parent = target.getParent();
            Files.createDirectories(parent);
            Path temporaryFile = Files.createTempFile(parent, prefix, ".tmp");
            boolean moved = false;
            try (BufferedWriter writer = Files.newBufferedWriter(
                    temporaryFile, StandardCharsets.UTF_8)) {
                writerAction.accept(writer);
            }
            try {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                moved = true;
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            } finally {
                if (!moved) {
                    Files.deleteIfExists(temporaryFile);
                }
            }
        }

            private static String normalizeInventoryType(String type) {
            if (type == null) {
                return "R";
            }
            String normalized = type.trim().toUpperCase();
            return normalized.equals("U") || normalized.equals("F")
                    ? normalized : "R";
        }

        public static ImageIcon loadImageAsset(String assetName) {
            List<Path> candidates = new ArrayList<>();
            Path workspacePath = Paths.get("").toAbsolutePath();
            addAssetCandidates(candidates, workspacePath, assetName);
            try {
                Path codePath = Paths.get(TowHitchCore.class.getProtectionDomain()
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

        public static ImageIcon loadScaledImageAsset(String assetName, int width, int height) {
            ImageIcon original = loadImageAsset(assetName);
            if (original == null) {
                return null;
            }
            Image scaled = original.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }

        public static ImageIcon scaleToFitIcon(ImageIcon source, int maxWidth, int maxHeight) {
            if (source == null) {
                return null;
            }
            double scale = Math.min((double) maxWidth / source.getIconWidth(),
                    (double) maxHeight / source.getIconHeight());
            int width = Math.max(1, (int) Math.round(source.getIconWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getIconHeight() * scale));
            return new ImageIcon(source.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        }

        public static ImageIcon loadTintedIcon(ImageIcon source, int size, Color tint) {
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

        public static void addAssetCandidates(List<Path> candidates, Path directory, String assetName) {
            Path candidate = directory.resolve(assetName).normalize();
            if (!candidates.contains(candidate)) {
                candidates.add(candidate);
            }
        }

        public static ImageIcon loadScaledLoginLogo(String assetName, int size) {
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
                Image scaled = original.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
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
    }
