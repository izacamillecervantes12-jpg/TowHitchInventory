import java.awt.Color;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Path2D;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.WindowFocusListener;
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
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.border.CompoundBorder;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.WindowConstants;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;

public final class TowHitchCore {
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    private static final DateTimeFormatter SMART_DATE_CURRENT =
            DateTimeFormatter.ofPattern("MMM d '|' h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter SMART_DATE_PAST =
            DateTimeFormatter.ofPattern("MMM d, yyyy '|' h:mm a", Locale.ENGLISH);

    public static final Path USERS_FILE = Paths.get("users.txt");
    public static final Path STAFF_ACCOUNTS_FILE = Paths.get("staff_accounts.txt");
    public static final Path INVENTORY_FILE = Paths.get("inventory_data.txt");
    public static final Path TRASH_FILE = Paths.get("trash_bin.txt");
    public static final int DEFAULT_MIN_ALERT = 5;
    private static final String[] DEFAULT_RAW_MATERIALS = {
            "Mild steel plates", "Square steel tubing", "Steel flat bars", "Steel rods",
            "Bolts", "Nuts", "Washers", "Welding rods", "Metal primer", "Paint",
            "Round steel tubing", "Rubber straps", "Steel plates", "Trailer axle",
            "Wheels and tires", "Trailer hitch coupler", "Safety chains", "Electrical wires",
            "Trailer signal lights", "Wheel chock", "Tie-down rings", "Steel bars",
            "Steel hitch ball", "Steel mounting plate", "Expanded metal mesh"
    };
    private static boolean defaultCatalogSeeded;
    private static AnchoredPopover activeAnchoredPopover;
    private static boolean globalMouseHandlingInstalled;

    static {
        applyToolTipTheme(false);
        try {
            ensureUsersFile();
            ensureStaffAccountsFile();
            ensureStockRequestsFile();
            ensureCustomerOrdersFile();
            ensureDefaultCatalogData();
            ensureTrashFile();
            loadTrash();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TowHitchCore() {
    }

    public static void configureTextField(JTextField field) {
        if (field instanceof JComponent && Boolean.TRUE.equals(
                ((JComponent) field).getClientProperty("towHitchTextFieldConfigured"))) {
            return;
        }

        public static void selectAllOnFocus(JTextField field) {
            if (Boolean.TRUE.equals(field.getClientProperty("towHitchSelectAllOnFocus"))) {
                return;
            }
            field.putClientProperty("towHitchSelectAllOnFocus", Boolean.TRUE);
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    SwingUtilities.invokeLater(() -> {
                        if (field.isFocusOwner()) {
                            field.selectAll();
                        }
                    });
                }
            });
        }
        if (field instanceof JComponent) {
            ((JComponent) field).putClientProperty(
                    "towHitchTextFieldConfigured", Boolean.TRUE);
        }
        if (!Boolean.TRUE.equals(field.getClientProperty("towHitchPreserveAlignment"))) {
            field.setHorizontalAlignment(SwingConstants.LEFT);
        }
        field.setMargin(new Insets(0, 8, 0, 4));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (field.getText().isEmpty()) {
                    field.setCaretPosition(0);
                }
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (!Boolean.TRUE.equals(
                        field.getClientProperty("towHitchPreserveAlignment"))) {
                    field.setHorizontalAlignment(SwingConstants.LEFT);
                }
                field.setCaretPosition(0);
            }
        });
    }

    public static void installGlobalMouseHandling() {
        if (globalMouseHandlingInstalled) {
            return;
        }
        globalMouseHandlingInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof java.awt.event.MouseEvent)
                    || event.getID() != java.awt.event.MouseEvent.MOUSE_PRESSED) {
                return;
            }
            Object source = event.getSource();
            if (!(source instanceof Component)) {
                return;
            }
            Component clicked = (Component) source;
            boolean textTarget = false;
            for (Component current = clicked; current != null; current = current.getParent()) {
                if (current instanceof JTextField || current instanceof JTextArea) {
                    textTarget = true;
                    if (current instanceof JTextField) {
                        JTextField textField = (JTextField) current;
                        configureTextField(textField);
                    }
                    break;
                }
            }
            if (!textTarget) {
                Component focusOwner = java.awt.KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof JTextField || focusOwner instanceof JTextArea) {
                    java.awt.Window window = SwingUtilities.getWindowAncestor(focusOwner);
                    if (window instanceof JFrame) {
                        javax.swing.JRootPane root = ((JFrame) window).getRootPane();
                        root.setFocusable(true);
                        root.requestFocusInWindow();
                    } else if (window instanceof javax.swing.JDialog) {
                        javax.swing.JRootPane root = ((javax.swing.JDialog) window).getRootPane();
                        root.setFocusable(true);
                        root.requestFocusInWindow();
                    }
                }
            }
            AnchoredPopover active = activeAnchoredPopover;
            if (active != null && !active.contains(clicked)) {
                active.close();
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    public static final class AnchoredPopover {
        private final JFrame parent;
        private Component anchor;
        private JWindow window;
        private boolean alignRight;
        private final ComponentAdapter movementListener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent event) {
                reposition();
            }

            @Override
            public void componentResized(ComponentEvent event) {
                reposition();
            }
        };
        private final WindowFocusListener focusListener = new WindowFocusListener() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent event) {
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent event) {
                if (event.getOppositeWindow() != window) {
                    close();
                }
            }
        };

        public AnchoredPopover(JFrame parent) {
            this.parent = parent;
        }

        public boolean show(Component anchor, JWindow window, boolean alignRight) {
            installGlobalMouseHandling();
            if (activeAnchoredPopover != null) {
                if (activeAnchoredPopover == this && this.anchor == anchor) {
                    window.dispose();
                    close();
                    return false;
                }
                activeAnchoredPopover.close();
            }
            this.anchor = anchor;
            this.window = window;
            this.alignRight = alignRight;
            activeAnchoredPopover = this;
            parent.addComponentListener(movementListener);
            parent.addWindowFocusListener(focusListener);
            reposition();
            window.setVisible(true);
            return true;
        }

        public void close() {
            if (window == null) {
                return;
            }
            parent.removeComponentListener(movementListener);
            parent.removeWindowFocusListener(focusListener);
            JWindow closingWindow = window;
            window = null;
            anchor = null;
            if (activeAnchoredPopover == this) {
                activeAnchoredPopover = null;
            }
            closingWindow.dispose();
        }

        private boolean contains(Component component) {
            return window != null
                    && (SwingUtilities.isDescendingFrom(component, window)
                            || component == window
                            || SwingUtilities.isDescendingFrom(component, anchor)
                            || component == anchor);
        }

        private void reposition() {
            if (window == null || anchor == null || !anchor.isShowing()) {
                return;
            }
            try {
                java.awt.Point point = anchor.getLocationOnScreen();
                java.awt.Rectangle bounds = java.awt.GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getMaximumWindowBounds();
                int x = alignRight ? point.x + anchor.getWidth() - window.getWidth() : point.x;
                x = Math.max(bounds.x + 8,
                        Math.min(x, bounds.x + bounds.width - window.getWidth() - 8));
                int y = point.y + anchor.getHeight() + 6;
                if (y + window.getHeight() > bounds.y + bounds.height - 8) {
                    y = point.y - window.getHeight() - 6;
                }
                window.setLocation(x, y);
            } catch (java.awt.IllegalComponentStateException ignored) {
                close();
            }
        }
    }

    public static void applyToolTipTheme(boolean darkMode) {
        Color background = darkMode ? new Color(0x262930) : new Color(0xFBF9F5);
        Color foreground = darkMode ? new Color(0xF3F4F6) : new Color(0x2B2926);
        Color border = darkMode ? new Color(0x3A3E47) : new Color(0xE2DDD3);
        UIManager.put("ToolTip.background", background);
        UIManager.put("ToolTip.foreground", foreground);
        UIManager.put("ToolTip.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    public static void paintThemeSwitch(Graphics2D graphics, int width, int height,
                                        float position, boolean darkMode) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(darkMode ? new Color(0x34312F) : new Color(0xE8E4DC));
        g.fillRoundRect(0, 1, width, height - 2, height - 2, height - 2);

        double leftCenterX = width / 4.0;
        double rightCenterX = width * 3.0 / 4.0;
        int knobDiameter = height - 10;
        double targetKnobCenterX = leftCenterX + (rightCenterX - leftCenterX) * position;
        int knobX = (int) Math.round(targetKnobCenterX - knobDiameter / 2.0);
        double knobCenterX = knobX + knobDiameter / 2.0;
        double knobCenterY = height / 2.0;
        int knobY = (int) Math.round(knobCenterY - knobDiameter / 2.0);
        g.setColor(darkMode ? new Color(0x303856) : Color.WHITE);
        g.fillOval(knobX, knobY, knobDiameter, knobDiameter);

        boolean sunOnKnob = position < 0.5f;
        String sun = "\u2600";
        String moon = "\u263E";
        Font sunFont = new Font("SansSerif", Font.BOLD, 14);
        Font moonFont = new Font("SansSerif", Font.PLAIN, 14);
        double sunCenterX = sunOnKnob ? knobCenterX : leftCenterX;
        double moonCenterX = sunOnKnob ? rightCenterX : knobCenterX;
        drawCenteredGlyph(g, sun, sunFont, sunOnKnob
                ? (darkMode ? new Color(0x23211F) : new Color(0x23211F))
                : new Color(0x969DAD), sunCenterX, knobCenterY);
        drawCenteredGlyph(g, moon, moonFont, sunOnKnob
                ? new Color(0x8B949E) : new Color(0xF5E9B8), moonCenterX, knobCenterY);
        g.dispose();
    }

    private static void drawCenteredGlyph(Graphics2D graphics, String glyph, Font font,
                                          Color color, double centerX, double centerY) {
        graphics.setFont(font);
        graphics.setColor(color);
        java.awt.FontMetrics metrics = graphics.getFontMetrics(font);
        int x = (int) Math.round(centerX - metrics.stringWidth(glyph) / 2.0);
        int baseline = (int) Math.round(centerY
                + (metrics.getAscent() - metrics.getDescent()) / 2.0);
        graphics.drawString(glyph, x, baseline);
    }

    public static String formatSmartDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.getYear() == LocalDate.now().getYear()
                ? dateTime.format(SMART_DATE_CURRENT) : dateTime.format(SMART_DATE_PAST);
    }

    public static String formatSmartDateOnly(String date) {
        if (date == null || date.trim().isEmpty()) {
            return "";
        }
        try {
            LocalDate parsed = LocalDate.parse(date.trim());
            DateTimeFormatter formatter = parsed.getYear() == LocalDate.now().getYear()
                    ? DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                    : DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
            return parsed.format(formatter);
        } catch (java.time.format.DateTimeParseException e) {
            return date.trim();
        }
    }

    public static String formatSmartDate(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return "";
        }
        String value = timestamp.trim();
        try {
            return formatSmartDate(LocalDateTime.parse(value));
        } catch (java.time.format.DateTimeParseException ignored) {
            try {
                return formatSmartDate(LocalDateTime.parse(value,
                        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm[:ss]", Locale.ROOT)));
            } catch (java.time.format.DateTimeParseException ignoredLegacy) {
                try {
                    return formatSmartDate(LocalDateTime.parse(value,
                            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm[:ss]", Locale.ROOT)));
                } catch (java.time.format.DateTimeParseException e) {
                    try {
                        return formatSmartDate(java.time.OffsetDateTime.parse(value)
                                .toLocalDateTime());
                    } catch (java.time.format.DateTimeParseException ignoredOffset) {
                        try {
                            return formatSmartDate(LocalDateTime.parse(value,
                                    DateTimeFormatter.ofPattern("MMM d, uuuu h:mm a",
                                            Locale.ENGLISH)));
                        } catch (java.time.format.DateTimeParseException parseFailure) {
                            return value;
                        }
                    }
                }
            }
        }
    }

    public static RoundedPanel createSectionPanel(ThemeController theme) {
        RoundedPanel section = new RoundedPanel(20);
        section.setLayout(new BorderLayout());
        section.setBackground(theme.isDark() ? new Color(0x1C1F26) : new Color(0xFAF8F4));
        section.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(theme.isDark()
                        ? new Color(0x2E323B) : new Color(0xE6E2D8), 20),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        return section;
    }

    public static String formatHistoryTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return "";
        }
        try {
            java.time.LocalTime parsed = java.time.LocalTime.parse(time.trim(),
                    DateTimeFormatter.ofPattern("H:mm[:ss]", Locale.ROOT));
            return parsed.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
        } catch (java.time.format.DateTimeParseException e) {
            try {
                java.time.LocalTime parsed = java.time.LocalTime.parse(time.trim(),
                        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
                return parsed.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
            } catch (java.time.format.DateTimeParseException ignored) {
                return time.trim();
            }
        }
    }

    public static final class RoundedBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Color color;
        private final int radius;

        public RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        public static final class RoundedShadowBorder extends AbstractBorder {
            private static final long serialVersionUID = 1L;
            private final Color outline;
            private final int radius;
            private final boolean drawOutline;

            public RoundedShadowBorder(Color outline, int radius) {
                this(outline, radius, true);
            }

            public RoundedShadowBorder(Color outline, int radius, boolean drawOutline) {
                this.outline = outline;
                this.radius = radius;
                this.drawOutline = drawOutline;
            }

            @Override
            public void paintBorder(Component component, Graphics graphics, int x, int y,
                                    int width, int height) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 6; i >= 1; i--) {
                    g.setColor(new Color(0, 0, 0, 5 + (6 - i) * 2));
                    g.drawRoundRect(x + i, y + i + 2, width - i * 2 - 1,
                            height - i * 2 - 3, radius, radius);
                }
                if (drawOutline) {
                    g.setColor(outline);
                    g.drawRoundRect(x + 6, y + 6, width - 13, height - 14, radius, radius);
                }
                g.dispose();
            }

            @Override
            public Insets getBorderInsets(Component component) {
                return new Insets(12, 12, 12, 12);
            }
        }

        public static final class QuantityStepper extends JPanel {
            private static final long serialVersionUID = 1L;
            private final JTextField valueField;
            private final int minimum;
            private final int maximum;

            public QuantityStepper(int initialValue, int minimum, int maximum,
                                   ThemeController theme) {
                if (minimum < 0 || maximum < minimum || initialValue < minimum
                        || initialValue > maximum) {
                    throw new IllegalArgumentException("Invalid quantity stepper bounds.");
                }
                this.minimum = minimum;
                this.maximum = maximum;
                setOpaque(false);
                setLayout(new BorderLayout(2, 0));
                setPreferredSize(new Dimension(120, 34));
                setMinimumSize(new Dimension(120, 34));
                setMaximumSize(new Dimension(120, 34));
                setBorder(new CompoundBorder(new RoundedBorder(theme.isDark()
                        ? new Color(0x383C45) : new Color(0xDCD8CE), 10),
                        BorderFactory.createEmptyBorder(1, 2, 1, 2)));

                JButton decrease = stepButton("\u2212", theme);
                JButton increase = stepButton("+", theme);
                decrease.addActionListener(event -> adjust(-1));
                increase.addActionListener(event -> adjust(1));
                valueField = new JTextField(Integer.toString(initialValue), 3);
                valueField.setHorizontalAlignment(SwingConstants.CENTER);
                valueField.putClientProperty("towHitchPreserveAlignment", Boolean.TRUE);
                valueField.setFont(new Font("SansSerif", Font.PLAIN, 13));
                valueField.setForeground(theme.text());
                valueField.setOpaque(false);
                valueField.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                selectAllOnFocus(valueField);
                valueField.addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        valueField.setHorizontalAlignment(SwingConstants.CENTER);
                    }

                    @Override
                    public void focusLost(FocusEvent event) {
                        valueField.setHorizontalAlignment(SwingConstants.CENTER);
                        valueField.setCaretPosition(0);
                    }
                });
                add(decrease, BorderLayout.WEST);
                add(valueField, BorderLayout.CENTER);
                add(increase, BorderLayout.EAST);
            }

            private JButton stepButton(String label, ThemeController theme) {
                JButton button = new JButton(label);
                button.setFont(new Font("SansSerif", Font.BOLD, 14));
                button.setForeground(theme.text());
                button.setOpaque(false);
                button.setContentAreaFilled(false);
                button.setFocusPainted(false);
                button.setMargin(new Insets(0, 0, 0, 0));
                button.setPreferredSize(new Dimension(34, 34));
                button.setMinimumSize(new Dimension(34, 34));
                button.setMaximumSize(new Dimension(34, 34));
                button.setBorder(BorderFactory.createEmptyBorder());
                return button;
            }

            private void adjust(int amount) {
                try {
                    int current = getValue();
                    long next = (long) current + amount;
                    valueField.setText(Integer.toString((int) Math.max(minimum,
                            Math.min(maximum, next))));
                } catch (IllegalArgumentException e) {
                    valueField.setText(Integer.toString(minimum));
                }
            }

            public int getValue() {
                try {
                    int value = Integer.parseInt(valueField.getText().trim());
                    if (value < minimum || value > maximum) {
                        throw new IllegalArgumentException("Quantity is outside the allowed range.");
                    }
                    return value;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Enter a valid whole-number quantity.", e);
                }
            }

            public void setPreferredWidth(int width) {
                Dimension size = new Dimension(width, 34);
                setPreferredSize(size);
                setMinimumSize(size);
                setMaximumSize(size);
            }

            public JTextField getTextField() {
                return valueField;
            }
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y,
                                int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }

    public static Icon createLogoutIcon(Color color) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }

            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(color);
                g.setStroke(new java.awt.BasicStroke(1.6f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                Path2D door = new Path2D.Float();
                door.moveTo(6, 2.5);
                door.curveTo(4.5, 2.5, 3.5, 3.5, 3.5, 5);
                door.lineTo(3.5, 11);
                door.curveTo(3.5, 12.5, 4.5, 13.5, 6, 13.5);
                g.draw(door);
                g.draw(new java.awt.geom.Line2D.Float(1.5f, 8, 11.5f, 8));
                g.draw(new java.awt.geom.Line2D.Float(8.5f, 5, 11.5f, 8));
                g.draw(new java.awt.geom.Line2D.Float(8.5f, 11, 11.5f, 8));
                g.dispose();
            }
        };
    }

    public static void addAlignedTableCell(JPanel panel, Component component, int column,
                                           int[] columnWidths, double[] columnWeights,
                                           int alignment) {
        if (columnWidths.length != columnWeights.length || column < 0
                || column >= columnWidths.length) {
            throw new IllegalArgumentException("Invalid aligned table column configuration.");
        }
        JPanel cell = new JPanel(new java.awt.GridBagLayout());
        cell.setOpaque(false);
        Dimension componentSize = component.getPreferredSize();
        int width = columnWidths[column] > 0 ? columnWidths[column] : 1;
        int height = Math.max(28, componentSize.height);
        Dimension cellSize = new Dimension(width, height);
        cell.setPreferredSize(cellSize);
        cell.setMinimumSize(cellSize);
        if (columnWidths[column] > 0) {
            cell.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        }

        if (component instanceof JLabel) {
            ((JLabel) component).setHorizontalAlignment(alignment);
        }
        java.awt.GridBagConstraints inner = new java.awt.GridBagConstraints();
        inner.gridx = 0;
        inner.gridy = 0;
        inner.weightx = 1.0;
        inner.fill = alignment == SwingConstants.LEFT || alignment == SwingConstants.RIGHT
                ? java.awt.GridBagConstraints.HORIZONTAL : java.awt.GridBagConstraints.NONE;
        inner.anchor = alignment == SwingConstants.LEFT ? java.awt.GridBagConstraints.WEST
                : alignment == SwingConstants.RIGHT ? java.awt.GridBagConstraints.EAST
                : java.awt.GridBagConstraints.CENTER;
        cell.add(component, inner);

        java.awt.GridBagConstraints constraints = new java.awt.GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = columnWeights[column];
        constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        constraints.anchor = java.awt.GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 4, 0, 4);
        panel.add(cell, constraints);
    }

    public static void configureSmoothScroll(javax.swing.JScrollPane scrollPane) {
        scrollPane.setDoubleBuffered(true);
        scrollPane.getViewport().setDoubleBuffered(true);
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setUnitIncrement(24);
        vertical.setBlockIncrement(96);
        vertical.setOpaque(false);
        vertical.setPreferredSize(new Dimension(6, 6));
        JScrollBar horizontal = scrollPane.getHorizontalScrollBar();
        horizontal.setUnitIncrement(24);
        horizontal.setBlockIncrement(96);
        horizontal.setOpaque(false);
        horizontal.setPreferredSize(new Dimension(6, 6));
    }

    public static void showToast(JFrame parent, String message, boolean isSuccess) {
        if (parent == null || !parent.isShowing()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JWindow toast = new JWindow(parent);
            JPanel surface = new RoundedPanel(12);
            surface.setBackground(new Color(0xFDFBF7));
            surface.setLayout(new java.awt.BorderLayout(10, 0));
            surface.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isSuccess
                            ? new Color(0x2E6F40) : new Color(0xA63A3A)),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            JLabel label = new JLabel(message);
            label.setFont(new Font("SansSerif", Font.BOLD, 13));
            label.setForeground(isSuccess ? new Color(0x2E6F40) : new Color(0xA63A3A));
            surface.add(label, java.awt.BorderLayout.CENTER);
            toast.setContentPane(surface);
            toast.pack();
            java.awt.Point location = parent.getLocationOnScreen();
            toast.setLocation(location.x + parent.getWidth() - toast.getWidth() - 24,
                    location.y + parent.getHeight() - toast.getHeight() - 28);
            toast.setAlwaysOnTop(true);
            toast.setVisible(true);
            Timer timer = new Timer(3500, event -> toast.dispose());
            timer.setRepeats(false);
            timer.start();
        });
    }

    public static final class TrashEntry {
        public final String id;
        public final String category;
        public final String itemName;
        public final int quantity;
        public final int minAlert;
        public final String extraData;
        public final String deletedBy;
        public final String deletedAtIso;

        public TrashEntry(String id, String category, String itemName, int quantity,
                          int minAlert, String extraData, String deletedBy, String deletedAtIso) {
            this.id = id;
            this.category = category;
            this.itemName = itemName;
            this.quantity = quantity;
            this.minAlert = minAlert;
            this.extraData = extraData == null ? "" : extraData;
            this.deletedBy = deletedBy;
            this.deletedAtIso = deletedAtIso;
        }
    }

    public static synchronized List<TrashEntry> loadTrash() throws IOException {
        ensureTrashFile();
        List<TrashEntry> entries = new ArrayList<>();
        List<String> lines = Files.readAllLines(TRASH_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        LocalDateTime now = LocalDateTime.now();
        boolean purged = false;
        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length != 8) {
                System.err.println("Ignoring malformed trash data at line " + (lineNumber + 1) + ".");
                continue;
            }
            try {
                LocalDateTime deletedAt = LocalDateTime.parse(fields[7]);
                if (ChronoUnit.DAYS.between(deletedAt, now) >= 30) {
                    purged = true;
                    continue;
                }
                int quantity = Integer.parseInt(fields[3]);
                int minAlert = Integer.parseInt(fields[4]);
                if (quantity < 0 || minAlert < 0 || fields[0].isEmpty()
                        || fields[1].isEmpty() || fields[2].isEmpty()) {
                    throw new IllegalArgumentException("Invalid trash entry.");
                }
                entries.add(new TrashEntry(fields[0], fields[1], fields[2], quantity, minAlert,
                        fields[5], fields[6], fields[7]));
            } catch (RuntimeException e) {
                System.err.println("Ignoring invalid trash data at line " + (lineNumber + 1) + ".");
            }
        }
        if (purged) {
            saveTrash(entries);
        }
        return entries;
    }

    public static synchronized void saveTrash(List<TrashEntry> entries) throws IOException {
        Path target = TRASH_FILE.toAbsolutePath();
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "trash_bin", ".tmp");
        boolean moved = false;
        try {
            try (java.io.BufferedWriter writer =
                         Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                for (TrashEntry entry : entries) {
                    writer.write(entry.id + "\t" + entry.category + "\t" + entry.itemName + "\t"
                            + entry.quantity + "\t" + entry.minAlert + "\t" + entry.extraData + "\t"
                            + entry.deletedBy + "\t" + entry.deletedAtIso);
                    writer.newLine();
                }
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public static synchronized void addToTrash(String category, String itemName, int quantity,
                                                int minAlert, String extraData, String deletedBy)
            throws IOException {
        List<TrashEntry> entries = loadTrash();
        String timestamp = LocalDateTime.now().toString();
        entries.add(new TrashEntry(UUID.randomUUID().toString(), category, itemName, quantity,
                minAlert, extraData, deletedBy, timestamp));
        saveTrash(entries);
    }

    public static synchronized void removeFromTrash(String id) throws IOException {
        List<TrashEntry> entries = loadTrash();
        entries.removeIf(entry -> entry.id.equals(id));
        saveTrash(entries);
    }

    private static synchronized void ensureTrashFile() throws IOException {
        Path file = TRASH_FILE.toAbsolutePath();
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }
    }

    public static List<SystemNotification> loadSystemNotifications(String role,
                                                                    String customerName)
            throws IOException {
        List<SystemNotification> notifications = new ArrayList<>();
        if ("OWNER".equals(role)) {
            for (StaffAccount account : loadStaffAccounts()) {
                if ("PENDING".equals(account.status)) {
                    notifications.add(new SystemNotification(
                            "crew:" + account.id + ":PENDING",
                            "<html><b>" + notificationEntity(account.displayName)
                                    + "</b> requested a Staff account.</html>",
                            new Color(0x536A86)));
                }
            }
        }
        for (InventoryItem item : loadInventory()) {
            if (item.quantity <= item.minAlert) {
                String name = notificationEntity(item.itemName);
                String text = item.quantity == 0
                        ? "<html><b>" + name + "</b> is out of stock.</html>"
                        : "<html><b>" + name + "</b> is running low on stock (" + item.quantity
                                + " left).</html>";
                String id = "inventory:" + item.itemName.toLowerCase(java.util.Locale.ROOT)
                        + ":" + item.quantity + ":" + item.minAlert;
                notifications.add(new SystemNotification(id, text,
                        item.quantity == 0 ? new Color(0xA63A3A) : new Color(0x9A6B36)));
            }
        }

        List<StockRequest> requests = loadStockRequests();
        if ("OWNER".equals(role)) {
            for (StockRequest request : requests) {
                if ("PENDING".equals(request.status)) {
                    notifications.add(new SystemNotification(
                            "request:" + request.requestId + ":" + request.status,
                            "<html>New restock request for " + request.quantity + " "
                                    + "<b>" + notificationEntity(request.materialName)
                                    + "</b> from " + notificationEntity(request.requestedBy)
                                    + " is pending.</html>",
                            new Color(0x536A86)));
                }
            }
        } else if ("STAFF".equals(role)) {
            for (StockRequest request : requests) {
                if (("APPROVED".equals(request.status) || "REJECTED".equals(request.status))
                        && request.requestedBy.equalsIgnoreCase(customerName)) {
                    notifications.add(new SystemNotification(
                            "request:" + request.requestId + ":" + request.status,
                            "<html>Your restock request for " + request.quantity + " "
                                    + "<b>" + notificationEntity(request.materialName)
                                    + "</b> has been "
                                    + request.status.toLowerCase(Locale.ROOT) + ".</html>",
                            new Color(0x536A86)));
                }
            }
        }

        for (CustomerOrder order : loadCustomerOrders()) {
            if ("OWNER".equals(role) || "STAFF".equals(role)) {
                if ("PENDING".equals(order.status)) {
                    notifications.add(new SystemNotification(
                            "order:" + order.orderId + ":" + order.status,
                            "<html><b>Order #" + notificationEntity(order.orderId)
                                    + "</b> \u2014 " + order.quantity + "x "
                                    + notificationEntity(order.productName)
                                    + " placed.</html>",
                            new Color(0x536A86)));
                }
            } else if ("CUSTOMER".equals(role)
                    && order.customerName.equalsIgnoreCase(customerName)
                    && ("COMPLETED".equals(order.status) || "CANCELLED".equals(order.status))) {
                notifications.add(new SystemNotification(
                        "order:" + order.orderId + ":" + order.status,
                        "<html>Your order for " + order.quantity + "x <b>"
                                + notificationEntity(order.productName) + "</b> was "
                                + ("COMPLETED".equals(order.status) ? "confirmed." : "cancelled.")
                                + "</html>",
                        new Color(0x536A86)));
            }
        }
        return notifications;
    }

    private static String notificationEntity(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static final class SystemNotification {
        public final String id;
        public final String text;
        public final Color indicatorColor;

        private SystemNotification(String id, String text, Color indicatorColor) {
            this.id = id;
            this.text = text;
            this.indicatorColor = indicatorColor;
        }
    }

    private static synchronized void ensureDefaultCatalogData() throws IOException {
        initializeInventoryFile();
        List<InventoryItem> inventory = readInventoryFile();
        boolean inventoryChanged = false;
        for (String material : DEFAULT_RAW_MATERIALS) {
            if (!hasInventoryItem(inventory, material, false)) {
                inventory.add(new InventoryItem("U", material, 0, DEFAULT_MIN_ALERT));
                inventoryChanged = true;
            }
        }

        Map<String, List<BOMItem>> recipes = defaultRecipes();
        for (String product : recipes.keySet()) {
            if (!hasInventoryItem(inventory, product, true)) {
                inventory.add(new InventoryItem("F", product, 0, 3));
                inventoryChanged = true;
            }
        }
        if (inventoryChanged) {
            saveInventory(inventory);
        }

        Map<String, List<BOMItem>> bom = loadBOM();
        boolean bomChanged = false;
        for (Map.Entry<String, List<BOMItem>> recipe : recipes.entrySet()) {
            if (!bom.containsKey(recipe.getKey()) || bom.get(recipe.getKey()).isEmpty()) {
                bom.put(recipe.getKey(), recipe.getValue());
                bomChanged = true;
            }
        }
        if (bomChanged || !Files.isRegularFile(Paths.get("bom_data.txt").toAbsolutePath())) {
            saveBOM(bom);
        }
        defaultCatalogSeeded = true;
    }

    private static boolean hasInventoryItem(List<InventoryItem> inventory, String name,
                                            boolean finished) {
        for (InventoryItem item : inventory) {
            if (("F".equals(item.type)) == finished
                    && item.itemName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, List<BOMItem>> defaultRecipes() {
        Map<String, List<BOMItem>> recipes = new LinkedHashMap<>();
        recipes.put("Hitch Accessories", recipe(
                "Mild steel plates:1", "Square steel tubing:1", "Steel flat bars:1",
                "Steel rods:1", "Bolts:2", "Nuts:2", "Washers:2", "Welding rods:1",
                "Metal primer:1", "Paint:1"));
        recipes.put("Cargo Basket", recipe(
                "Square steel tubing:4", "Steel flat bars:2", "Expanded metal mesh:1",
                "Bolts:4", "Nuts:4", "Washers:4", "Welding rods:1", "Metal primer:1",
                "Paint:1"));
        recipes.put("Bike Rack", recipe(
                "Square steel tubing:3", "Round steel tubing:1", "Steel flat bars:1",
                "Steel rods:1", "Rubber straps:2", "Bolts:4", "Nuts:4", "Washers:4",
                "Welding rods:1", "Metal primer:1", "Paint:1"));
        recipes.put("Trailer", recipe(
                "Square steel tubing:12", "Steel plates:2", "Steel flat bars:3",
                "Trailer axle:1", "Wheels and tires:2", "Trailer hitch coupler:1",
                "Safety chains:2", "Bolts:8", "Nuts:8", "Washers:8", "Welding rods:2",
                "Electrical wires:1", "Trailer signal lights:2", "Metal primer:1", "Paint:1"));
        recipes.put("Motor Rack Carrier", recipe(
                "Square steel tubing:5", "Steel plates:2", "Steel flat bars:2",
                "Steel rods:1", "Expanded metal mesh:1", "Wheel chock:1", "Tie-down rings:4",
                "Bolts:6", "Nuts:6", "Washers:6", "Welding rods:1", "Metal primer:1",
                "Paint:1"));
        recipes.put("Clamp", recipe(
                "Steel plates:2", "Steel bars:1", "Bolts:1", "Nuts:1", "Washers:2",
                "Welding rods:1", "Metal primer:1", "Paint:1"));
        recipes.put("Hitch Ball Assembly", recipe(
                "Steel hitch ball:1", "Steel mounting plate:1", "Bolts:2", "Nuts:2",
                "Washers:2"));
        return recipes;
    }

    private static List<BOMItem> recipe(String... items) {
        List<BOMItem> recipe = new ArrayList<>();
        for (String item : items) {
            int separator = item.lastIndexOf(':');
            recipe.add(new BOMItem(item.substring(0, separator),
                    Integer.parseInt(item.substring(separator + 1))));
        }
        return recipe;
    }

    public static final class ThemeController implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean darkMode;

        public void toggle() {
            setDark(!darkMode);
        }

        public void setDark(boolean dark) {
            darkMode = dark;
            applyToolTipTheme(darkMode);
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
            return darkMode ? new Color(0xA9B7D0) : new Color(0x46566B);
        }

        public Color danger() {
            return darkMode ? new Color(0xE2A09A) : new Color(0xA63A3A);
        }

        public Color success() {
            return darkMode ? new Color(0xA6C3A8) : new Color(0x2E6F40);
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
                return theme.isDark() ? new Color(0xB6D1BB) : new Color(0x275937);
            }
            if ("LOW STOCK".equals(status) || "OUT OF STOCK".equals(status)
                    || "REJECTED".equals(status) || "REMOVED".equals(status)
                    || "CANCELLED".equals(status)) {
                return theme.isDark() ? new Color(0xE0A3A0) : new Color(0x8F3232);
            }
            return theme.isDark() ? new Color(0xD5BD94) : new Color(0x7C5324);
        }

        private Color backgroundColor() {
            if ("APPROVED".equals(status) || "COMPLETED".equals(status)
                    || "IN STOCK".equals(status)) {
                return theme.isDark()
                        ? new Color(0x3D5441) : new Color(0xE6EFE9);
            }
            if ("LOW STOCK".equals(status) || "OUT OF STOCK".equals(status)
                    || "REJECTED".equals(status) || "REMOVED".equals(status)
                    || "CANCELLED".equals(status)) {
                return theme.isDark()
                        ? new Color(0x573B3B) : new Color(0xF5E6E6);
            }
            return theme.isDark()
                    ? new Color(0x51483B) : new Color(0xF4ECE1);
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
                if (fill.equals(new Color(0x2E6F40))) {
                    actualFill = new Color(0x245832);
                } else if (fill.equals(new Color(0xA63A3A))) {
                    actualFill = new Color(0x8C2E2E);
                }
            }
            g.setColor(actualFill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            if (getModel().isRollover() && isEnabled()
                    && !fill.equals(new Color(0x2E6F40))
                    && !fill.equals(new Color(0xA63A3A))) {
                g.setColor(new Color(255, 255, 255, 25));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            }
            g.dispose();
            setForeground(isEnabled() ? foreground : theme.secondaryText());
            super.paintComponent(graphics);
        }
    }

    public static final class LogoutIcon implements Icon {
        private final Color color;
        private final Color background;

        public LogoutIcon(Color color) {
            this(color, null);
        }

        public LogoutIcon(Color color, Color background) {
            this.color = color;
            this.background = background;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.setStroke(new java.awt.BasicStroke(1.8f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.draw(new RoundRectangle2D.Double(3.0, 1.8, 9.0, 14.4, 3.2, 3.2));
            if (background != null) {
                g.setColor(background);
                g.setStroke(new java.awt.BasicStroke(3.0f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g.drawLine(12, 6, 12, 12);
            }
            g.setColor(color);
            g.setStroke(new java.awt.BasicStroke(1.8f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.drawLine(8, 9, 17, 9);
            g.drawLine(13, 5, 17, 9);
            g.drawLine(17, 9, 13, 13);
            g.dispose();
        }
    }

    public static final class MoreOptionsButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final ThemeController theme;

        public MoreOptionsButton(ThemeController theme) {
            this.theme = theme;
            setToolTipText("More actions");
            setPreferredSize(new Dimension(36, 36));
            setMinimumSize(new Dimension(36, 36));
            setMaximumSize(new Dimension(36, 36));
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
            if (getModel().isRollover()) {
                g.setColor(theme.input());
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
            }
            g.setColor(theme.text());
            int centerX = getWidth() / 2;
            for (int centerY = getHeight() / 2 - 7; centerY <= getHeight() / 2 + 7;
                    centerY += 7) {
                g.fillOval(centerX - 1, centerY - 1, 3, 3);
            }
            g.dispose();
        }
    }

    public static final class ChevronButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final ThemeController theme;
        private boolean expanded;

        public ChevronButton(ThemeController theme) {
            this.theme = theme;
            setToolTipText("Show bill of materials");
            setPreferredSize(new Dimension(32, 32));
            setMinimumSize(new Dimension(32, 32));
            setMaximumSize(new Dimension(32, 32));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            setToolTipText(expanded ? "Hide bill of materials" : "Show bill of materials");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover()) {
                g.setColor(theme.input());
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
            }
            g.setColor(theme.secondaryText());
            g.setStroke(new java.awt.BasicStroke(2.0f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            if (expanded) {
                g.drawLine(centerX - 5, centerY + 2, centerX, centerY - 3);
                g.drawLine(centerX, centerY - 3, centerX + 5, centerY + 2);
            } else {
                g.drawLine(centerX - 5, centerY - 2, centerX, centerY + 3);
                g.drawLine(centerX, centerY + 3, centerX + 5, centerY - 2);
            }
            g.dispose();
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
            configureTextField(this);
            putClientProperty("towHitchTextFieldConfigured", Boolean.TRUE);
            setFont(BODY_FONT);
            setForeground(theme.text());
            setCaretColor(theme.text());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            setPreferredSize(new Dimension(220, 38));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            setForeground(theme.text());
            setCaretColor(theme.text());
            super.paintComponent(graphics);
            if (getText().isEmpty()) {
                Graphics2D placeholderGraphics = (Graphics2D) graphics.create();
                placeholderGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                placeholderGraphics.setFont(getFont());
                placeholderGraphics.setColor(theme.secondaryText());
                Insets margin = getMargin();
                int leftPadding = getInsets().left + (margin == null ? 0 : margin.left);
                java.awt.FontMetrics metrics = placeholderGraphics.getFontMetrics();
                int baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                placeholderGraphics.drawString(placeholder, leftPadding, baseline);
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
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            setPreferredSize(new Dimension(220, 40));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(theme.input());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.dispose();
            setForeground(theme.isDark() ? new Color(0x242221) : theme.text());
            setCaretColor(theme.isDark() ? new Color(0x242221) : theme.text());
            super.paintComponent(graphics);
            if (getPassword().length == 0) {
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
            setToolTipText(count == 0 ? "Notifications"
                    : count + " unread notification" + (count == 1 ? "" : "s"));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            setForeground(theme.text());
            super.paintComponent(graphics);
            if (unreadCount > 0) {
                Graphics2D badge = (Graphics2D) graphics.create();
                badge.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String countText = unreadCount > 99 ? "99+" : Integer.toString(unreadCount);
                badge.setFont(new Font("SansSerif", Font.BOLD, 8));
                java.awt.FontMetrics metrics = badge.getFontMetrics();
                int badgeWidth = Math.max(14, metrics.stringWidth(countText) + 6);
                int badgeX = getWidth() - badgeWidth + 1;
                badge.setColor(theme.danger());
                badge.fillRoundRect(badgeX, 2, badgeWidth, 13, 13, 13);
                badge.setColor(Color.WHITE);
                badge.drawString(countText,
                        badgeX + (badgeWidth - metrics.stringWidth(countText)) / 2, 11);
                badge.dispose();
            }
        }
    }

    @SuppressWarnings("this-escape")
    public static class ProfileButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final ThemeController theme;

        public ProfileButton(ThemeController theme) {
            this.theme = theme;
            setToolTipText("Profile");
            setPreferredSize(new Dimension(34, 34));
            setMinimumSize(new Dimension(34, 34));
            setMaximumSize(new Dimension(34, 34));
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
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getModel().isRollover()
                    ? (theme.isDark() ? new Color(0x48433D) : new Color(0xEAE4DA))
                    : (theme.isDark() ? new Color(0x3B3732) : new Color(0xF3EEE6)));
            g.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g.setColor(theme.isDark() ? new Color(0xD6CEC2) : new Color(0x5C554C));
            g.setStroke(new java.awt.BasicStroke(1.7f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            int center = getWidth() / 2;
            g.drawOval(center - 4, 7, 8, 8);
            g.draw(new RoundRectangle2D.Double(center - 8, 17, 16, 10, 8, 8));
            g.dispose();
        }
    }

    @SuppressWarnings("this-escape")
    public static class PopoverMenuButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final String leadingIcon;
        private final boolean showChevron;
        private final ThemeController theme;
        private final boolean danger;

        public PopoverMenuButton(String label, String leadingIcon, boolean showChevron,
                                 boolean danger, ThemeController theme) {
            super(label);
            this.leadingIcon = leadingIcon;
            this.showChevron = showChevron;
            this.danger = danger;
            this.theme = theme;
            setFont(new Font("SansSerif", Font.PLAIN, 13));
            setForeground(danger ? theme.danger() : theme.text());
            setHorizontalAlignment(SwingConstants.LEFT);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            setPreferredSize(new Dimension(286, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover()) {
                g.setColor(theme.isDark() ? new Color(0x34312F) : new Color(0xF2EEE7));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            g.setFont(getFont());
            g.setColor(danger ? theme.danger() : theme.secondaryText());
            java.awt.FontMetrics metrics = g.getFontMetrics();
            int baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            int x = 14;
            if (leadingIcon != null && !leadingIcon.isEmpty()) {
                g.drawString(leadingIcon, x, baseline);
                x += metrics.stringWidth(leadingIcon) + 10;
            }
            g.setColor(danger ? theme.danger() : theme.text());
            g.drawString(getText(), x, baseline);
            if (showChevron) {
                g.setColor(theme.secondaryText());
                g.drawString(">", getWidth() - 24 - metrics.stringWidth(">"), baseline);
            }
            g.dispose();
        }
    }

    @SuppressWarnings("this-escape")
    public static final class PillToggleSwitch extends JToggleButton {
        private static final long serialVersionUID = 1L;
        private final ThemeController theme;

        public PillToggleSwitch(boolean selected, ThemeController theme) {
            this.theme = theme;
            setSelected(selected);
            setPreferredSize(new Dimension(38, 20));
            setMinimumSize(new Dimension(38, 20));
            setMaximumSize(new Dimension(38, 20));
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
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isSelected() ? new Color(0x1C1B19)
                    : theme.isDark() ? new Color(0x55514A) : new Color(0xD6D1C8));
            g.fillRoundRect(0, 0, 38, 20, 20, 20);
            g.setColor(Color.WHITE);
            g.fillOval(isSelected() ? 20 : 2, 2, 16, 16);
            g.dispose();
        }
    }

    @SuppressWarnings("this-escape")
    public static final class ProfileRadioCard extends JToggleButton {
        private static final long serialVersionUID = 1L;
        private final ThemeController theme;

        public ProfileRadioCard(String text, boolean selected, ThemeController theme) {
            super(text);
            this.theme = theme;
            setSelected(selected);
            setFont(FONT_BODY);
            setForeground(theme.text());
            setHorizontalAlignment(SwingConstants.LEFT);
            setPreferredSize(new Dimension(132, 42));
            setMinimumSize(new Dimension(132, 42));
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
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = isSelected()
                    ? (theme.isDark() ? new Color(0x34312F) : new Color(0xF0ECE4))
                    : theme.panel();
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.setColor(isSelected()
                    ? (theme.isDark() ? new Color(0x8B8174) : new Color(0xA69A88))
                    : (theme.isDark() ? new Color(0x44464D) : new Color(0xE2DDD3)));
            g.setStroke(new java.awt.BasicStroke(1.0f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            int centerY = getHeight() / 2;
            g.setColor(isSelected() ? new Color(0x1C1B19) : theme.secondaryText());
            g.setStroke(new java.awt.BasicStroke(1.4f));
            g.drawOval(12, centerY - 7, 14, 14);
            if (isSelected()) {
                g.fillOval(16, centerY - 3, 6, 6);
            }
            g.setFont(getFont());
            g.setColor(theme.text());
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(getText(), 34,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    @SuppressWarnings("this-escape")
    public static final class RoundedCheckBox extends javax.swing.JCheckBox {
        private static final long serialVersionUID = 1L;
        private final ThemeController theme;

        public RoundedCheckBox(String text, boolean selected, ThemeController theme) {
            super(text, selected);
            this.theme = theme;
            setFont(FONT_BODY);
            setForeground(theme.text());
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setPreferredSize(new Dimension(230, 36));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover()) {
                g.setColor(theme.input());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            int y = (getHeight() - 18) / 2;
            g.setStroke(new java.awt.BasicStroke(1.5f));
            if (isSelected()) {
                g.setColor(new Color(0x1C1B19));
                g.fillRoundRect(0, y, 18, 18, 5, 5);
                g.setColor(Color.WHITE);
                g.setStroke(new java.awt.BasicStroke(1.8f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                Path2D check = new Path2D.Double();
                check.moveTo(4.0, y + 9.0);
                check.lineTo(7.5, y + 12.5);
                check.lineTo(14.0, y + 5.5);
                g.draw(check);
            } else {
                g.setColor(theme.secondaryText());
                g.drawRoundRect(0, y, 17, 17, 5, 5);
            }
            g.setFont(getFont());
            g.setColor(getForeground());
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(getText(), 28,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
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
        public Dimension getPreferredSize(JComponent component) {
            return new Dimension(6, 6);
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            scrollbar.setOpaque(false);
            scrollbar.setBackground(new Color(0, 0, 0, 0));
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
                    ? new Color(0x4B505C) : new Color(0xB8B3A8);
            g.setColor(thumb);
            if (scrollBar.getOrientation() == JScrollBar.HORIZONTAL) {
                int height = 5;
                int y = bounds.y + (bounds.height - height) / 2;
                g.fillRoundRect(bounds.x, y, bounds.width, height, 6, 6);
            } else {
                int width = 5;
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

        public static final class StaffAccount {
            public final String id;
            public final String fullName;
            public final String displayName;
            public final String username;
            public final String password;
            public String status;
            public final String createdAtIso;

            public StaffAccount(String id, String fullName, String username, String password,
                                String status, String createdAtIso) {
                this(id, fullName, fullName, username, password, status, createdAtIso);
            }

            public StaffAccount(String id, String fullName, String displayName, String username,
                                String password, String status, String createdAtIso) {
                this.id = id;
                this.fullName = fullName;
                this.displayName = displayName;
                this.username = username;
                this.password = password;
                this.status = status;
                this.createdAtIso = createdAtIso;
            }

            public void setStatus(String status) {
                this.status = status;
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

        public static synchronized void ensureStaffAccountsFile() throws IOException {
            Path file = STAFF_ACCOUNTS_FILE.toAbsolutePath();
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        }

        public static List<StaffAccount> loadStaffAccounts() throws IOException {
            ensureStaffAccountsFile();
            List<StaffAccount> accounts = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(
                    STAFF_ACCOUNTS_FILE.toAbsolutePath(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 6 && fields.length != 7) {
                        continue;
                    }
                    boolean hasDisplayName = fields.length == 7;
                    int statusIndex = hasDisplayName ? 5 : 4;
                    int createdAtIndex = hasDisplayName ? 6 : 5;
                    String status = fields[statusIndex].trim().toUpperCase(Locale.ROOT);
                    if (!"PENDING".equals(status) && !"APPROVED".equals(status)
                            && !"REMOVED".equals(status)) {
                        continue;
                    }
                    if (fields[0].trim().isEmpty() || fields[1].trim().isEmpty()
                            || (hasDisplayName && fields[2].trim().isEmpty())) {
                        continue;
                    }
                    String displayName = hasDisplayName ? fields[2].trim() : fields[1].trim();
                    String username = fields[hasDisplayName ? 3 : 2].trim();
                    String password = fields[hasDisplayName ? 4 : 3];
                    accounts.add(new StaffAccount(fields[0].trim(), fields[1].trim(),
                            displayName, username, password, status, fields[createdAtIndex].trim()));
                }
            }
            return accounts;
        }

        public static synchronized StaffAccount registerStaffAccount(
                String fullName, String username, String password) throws IOException {
            return registerStaffAccount(fullName, fullName, username, password);
        }

        public static synchronized StaffAccount registerStaffAccount(
                String fullName, String displayName, String username, String password)
                throws IOException {
            String name = validateStaffAccountField(fullName, "Full name");
            String display = validateStaffAccountField(displayName, "Display name");
            String login = validateStaffAccountField(username, "Username");
            String secret = validateStaffAccountField(password, "Password");
            ensureUsersFile();
            List<UserAccount> users = loadUsers();
            for (UserAccount user : users) {
                if (user.username.equalsIgnoreCase(login)) {
                    throw new IOException("That username is already in use.");
                }
            }
            List<StaffAccount> accounts = loadStaffAccounts();
            for (StaffAccount account : accounts) {
                if (account.username.equalsIgnoreCase(login)) {
                    throw new IOException("That username is already in use.");
                }
            }
            StaffAccount account = new StaffAccount(UUID.randomUUID().toString(), name, display,
                    login, secret, "PENDING", LocalDateTime.now().toString());
            accounts.add(account);
            saveStaffAccounts(accounts);
            return account;
        }

        private static String validateStaffAccountField(String value, String label)
                throws IOException {
            if (value == null || value.trim().isEmpty()) {
                throw new IOException(label + " is required.");
            }
            String normalized = value.trim();
            if (normalized.indexOf('\t') >= 0 || normalized.indexOf('\n') >= 0
                    || normalized.indexOf('\r') >= 0) {
                throw new IOException(label + " contains an unsupported character.");
            }
            return normalized;
        }

        public static synchronized void saveStaffAccounts(List<StaffAccount> accounts)
                throws IOException {
            Path file = STAFF_ACCOUNTS_FILE.toAbsolutePath();
            Files.createDirectories(file.getParent());
            Path temporaryFile = Files.createTempFile(file.getParent(), "staff_accounts", ".tmp");
            boolean moved = false;
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile,
                    StandardCharsets.UTF_8)) {
                for (StaffAccount account : accounts) {
                    writer.write(account.id + "\t" + account.fullName + "\t"
                            + account.displayName + "\t" + account.username + "\t" + account.password + "\t"
                            + account.status + "\t"
                            + account.createdAtIso);
                    writer.newLine();
                }
            }
            try {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                moved = true;
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            } finally {
                if (!moved) {
                    Files.deleteIfExists(temporaryFile);
                }
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
            if (!defaultCatalogSeeded) {
                ensureDefaultCatalogData();
            } else {
                initializeInventoryFile();
            }
        }

        private static synchronized void initializeInventoryFile() throws IOException {
            if (!Files.exists(INVENTORY_FILE.toAbsolutePath())) {
                saveInventory(new ArrayList<>());
            }
        }

        public static List<InventoryItem> loadInventory() throws IOException {
            initializeInventory();
            return readInventoryFile();
        }

        private static List<InventoryItem> readInventoryFile() throws IOException {
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

        public static List<StockRequest> withStockRequestApproval(
                List<StockRequest> requests, String requestId, int approvedQuantity,
                String timestamp) {
            List<StockRequest> updated = new ArrayList<>();
            boolean matched = false;
            StockRequest pendingRemainder = null;
            for (StockRequest request : requests) {
                if (!request.requestId.equals(requestId)) {
                    updated.add(request);
                    continue;
                }
                if (matched || !"PENDING".equals(request.status)
                        || approvedQuantity < 1 || approvedQuantity > request.quantity) {
                    throw new IllegalArgumentException("Invalid stock request approval quantity.");
                }
                matched = true;
                updated.add(new StockRequest(request.requestId, timestamp,
                        request.requestedBy, request.materialName, approvedQuantity, "APPROVED"));
                int remainder = request.quantity - approvedQuantity;
                if (remainder > 0) {
                    String remainderId = request.requestId + "-R-"
                            + UUID.randomUUID().toString().substring(0, 8);
                    pendingRemainder = new StockRequest(remainderId, request.timestamp,
                            request.requestedBy, request.materialName, remainder, "PENDING");
                }
            }
            if (!matched) {
                throw new IllegalArgumentException("The pending stock request could not be found.");
            }
            if (pendingRemainder != null) {
                updated.add(pendingRemainder);
            }
            return updated;
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

        public static ImageIcon loadScaledImageAssetFit(
                String assetName, int maxWidth, int maxHeight) {
            return scaleToFitIcon(loadImageAsset(assetName), maxWidth, maxHeight);
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

        public static ImageIcon loadTintedIcon(String assetName, int size, Color tint) {
            return loadTintedIcon(loadImageAsset(assetName), size, tint);
        }

        public static void addAssetCandidates(List<Path> candidates, Path directory, String assetName) {
            String lowerName = assetName.toLowerCase(java.util.Locale.ROOT);
            String stem = assetName;
            List<String> names = new ArrayList<>();
            names.add(assetName);
            if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg")
                    || lowerName.endsWith(".jpeg")) {
                int extensionIndex = assetName.lastIndexOf('.');
                stem = assetName.substring(0, extensionIndex);
                names.add(stem + ".png");
                names.add(stem + ".jpg");
                names.add(stem + ".jpeg");
            }
            for (String name : names) {
                Path candidate = directory.resolve(name).normalize();
                if (!candidates.contains(candidate)) {
                    candidates.add(candidate);
                }
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
