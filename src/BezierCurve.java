package src;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that draws a Bézier curve with configurable control points.
 * If the curve is larger than the visible area, place this panel in a JScrollPane.
 */
public class BezierCurve extends JPanel {
    private static final int PADDING = 50;
    private static final double CURVE_STEP = 0.01;
    private static final int POINT_SIZE = 8;
    private static final float CURVE_STROKE_WIDTH = 2.0f;

    private final List<Point2D> controlPoints;
    private Path2D cachedPath;
    private int[][] binomialCache;

    // bounding box for the generated curve and control points
    private double minX = 0, minY = 0, maxX = 0, maxY = 0;

    public BezierCurve() {
        controlPoints = new ArrayList<>();
        controlPoints.add(new Point2D.Double(100, 100));
        controlPoints.add(new Point2D.Double(200, 33));
        controlPoints.add(new Point2D.Double(-200, -33));
        controlPoints.add(new Point2D.Double(0, -500));

        initializeBinomialCache();
        generatePath();
    }

    /**
     * Inicializa la caché del coeficiente binomial usando programación dinámica.
     */
    private void initializeBinomialCache() {
        int n = controlPoints.size() - 1;
        binomialCache = new int[n + 1][n + 1];

        for (int i = 0; i <= n; i++) {
            binomialCache[i][0] = 1;
            binomialCache[i][i] = 1;
            for (int j = 1; j < i; j++) {
                binomialCache[i][j] = binomialCache[i - 1][j - 1] + binomialCache[i - 1][j];
            }
        }
    }

    /**
     * Genera el path de la curva de Bézier y calcula su bounding box.
     */
    private void generatePath() {
        cachedPath = new Path2D.Double();

        // initialize bounds
        minX = Double.POSITIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;

        // include control points in bounds
        for (Point2D cp : controlPoints) {
            updateBounds(cp.getX(), cp.getY());
        }

        Point2D firstPoint = calculateBezierPoint(0);
        cachedPath.moveTo(firstPoint.getX(), firstPoint.getY());
        updateBounds(firstPoint.getX(), firstPoint.getY());

        for (double t = CURVE_STEP; t <= 1.0; t += CURVE_STEP) {
            Point2D p = calculateBezierPoint(t);
            cachedPath.lineTo(p.getX(), p.getY());
            updateBounds(p.getX(), p.getY());
        }

        // Ensure endpoint t=1 included
        Point2D lastPoint = calculateBezierPoint(1.0);
        cachedPath.lineTo(lastPoint.getX(), lastPoint.getY());
        updateBounds(lastPoint.getX(), lastPoint.getY());

        // set preferred size so JScrollPane knows the virtual size
        int w = (int) Math.ceil(maxX - minX) + 2 * PADDING;
        int h = (int) Math.ceil(maxY - minY) + 2 * PADDING;
        setPreferredSize(new Dimension(Math.max(100, w), Math.max(100, h)));
        revalidate();
        repaint();
    }

    private void updateBounds(double x, double y) {
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Improve rendering quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Transform so world coordinates (curve coords) are drawn with padding and Y axis up
        g2d.translate(-minX + PADDING, maxY + PADDING);
        g2d.scale(1, -1);

        drawAxes(g2d);
        drawControlLines(g2d);
        drawControlPoints(g2d);
        drawBezierCurve(g2d);
    }

    /**
     * Dibuja los ejes cartesianos dentro del bounding box calculado.
     */
    private void drawAxes(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1.0f));
        int left = (int) Math.floor(minX - PADDING);
        int right = (int) Math.ceil(maxX + PADDING);
        int bottom = (int) Math.floor(minY - PADDING);
        int top = (int) Math.ceil(maxY + PADDING);

        // horizontal axis (y = 0)
        g2d.drawLine(left, 0, right, 0);
        // vertical axis (x = 0)
        g2d.drawLine(0, bottom, 0, top);
    }

    /**
     * Dibuja las líneas entre puntos de control.
     */
    private void drawControlLines(Graphics2D g2d) {
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10.0f, new float[]{5.0f}, 0.0f));
        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Point2D p1 = controlPoints.get(i);
            Point2D p2 = controlPoints.get(i + 1);
            g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
        }
    }

    /**
     * Dibuja los puntos de control.
     */
    private void drawControlPoints(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        for (Point2D p : controlPoints) {
            int x = (int) p.getX() - POINT_SIZE / 2;
            int y = (int) p.getY() - POINT_SIZE / 2;
            g2d.fillOval(x, y, POINT_SIZE, POINT_SIZE);
        }
    }

    /**
     * Dibuja la curva de Bézier.
     */
    private void drawBezierCurve(Graphics2D g2d) {
        g2d.setColor(new Color(0, 102, 204));
        g2d.setStroke(new BasicStroke(CURVE_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(cachedPath);
    }

    /**
     * Calcula un punto en la curva de Bézier para un valor t dado.
     */
    private Point2D calculateBezierPoint(double t) {
        double x = 0, y = 0;
        int n = controlPoints.size() - 1;

        for (int i = 0; i <= n; i++) {
            double factor = binomialCache[n][i] * Math.pow(1 - t, n - i) * Math.pow(t, i);
            x += factor * controlPoints.get(i).getX();
            y += factor * controlPoints.get(i).getY();
        }

        return new Point2D.Double(x, y);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Curva de Bézier");
            BezierCurve panel = new BezierCurve();

            // Wrap the panel in a JScrollPane so it can scroll if larger than the window
            JScrollPane scrollPane = new JScrollPane(panel);
            // Optional: always show scrollbars when needed
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.add(scrollPane);
            frame.setVisible(true);
        });
    }
}
