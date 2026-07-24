import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * GUI for a Life-like cellular automaton on a generic grid.
 * Original version by Peter Taylor, modified and extended by Henrik Schou Guttesen using ChatGPT 5.2
 * See: http://codegolf.stackexchange.com/q/35827/194
 *
* Usage:
 *   javac GenericLifeGui.java GenericLife.java AbstractLattice.java Tiling.java <tiling>.java
 *   java GenericLifeGui <tiling> [<cell-data>...]
 */
class GenericLifeGui<Cell> extends JComponent {
    private static final Color GRIDCOL = new Color(0x37392f);
    private static final Color DEADCOL = new Color(0xffffff);
    private static final Color LIVECOL = new Color(0x00b092);
    private static final Color PENDING_DEADCOL = new Color(0xb2dda9);
    private static final Color PENDING_LIVECOL = new Color(0xececa3);

    private final Tiling<Cell> tiling;
    private Set<Cell> alive = new HashSet<Cell>();
    private Set<Cell> nextGeneration = new HashSet<Cell>();
    private Point centre = new Point(0, 0);
    private List<Cell> bufferIndex;
    private BufferedImage pBuffer;
    // New fields in GenericLifeGui class
    private boolean isDragging = false;
    private Set<Cell> modifiedDuringDrag = new HashSet<>();

    private static void usage() {
        System.out.println("Usage: java GenericLifeGui <tiling> [<cell-data>...]");
        System.out.println("Example: java GenericLifeGui Kagome -6 -4 B -1 -7 C -1 -7 A -5 -5 C -5 -5 B -5 -5 A -4 -6 C -4 -6 B -1 -6 B -3 -7 C 0 -7 C -4 -5 B -3 -6 C -3 -6 B 0 -6 B -2 -7 C 1 -7 A -7 -3 C -7 -3 B -7 -3 A -2 -6 B -6 -4 C");
        System.exit(1);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("--help")) usage();

        Tiling tiling = (Tiling) Class.forName(args[0]).newInstance();
        GenericLifeGui gui = new GenericLifeGui(tiling);

        if (args.length > 1) {
            String[] cellData = Arrays.copyOfRange(args, 1, args.length);
            try {
                gui.alive = tiling.parseCells(cellData);
                gui.nextGeneration = GenericLife.nextGeneration(tiling, gui.alive);
            } catch (Exception e) {
                System.err.println("Error parsing initial configuration: " + e.getMessage());
                usage();
            }
        }

        gui.launch();
    }

    private GenericLifeGui(Tiling<Cell> tiling) {
        this.tiling = tiling;
    }

    private void launch() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final JFrame frame = new JFrame(tiling.getClass().getSimpleName());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(GenericLifeGui.this, BorderLayout.CENTER);

                GenericLifeGui.this.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent evt) {
                        isDragging = true;
                        modifiedDuringDrag.clear();
                        if (pBuffer == null) return;
                        int x = evt.getX(), y = evt.getY();
                        if (x < 0 || x >= pBuffer.getWidth() || y < 0 || y >= pBuffer.getHeight()) return;
                        int idx = pBuffer.getRGB(x, y) & 0xffffff;
                        if (idx > 0 && idx < bufferIndex.size()) {
                            Cell cell = bufferIndex.get(idx);
                            if (!alive.add(cell)) alive.remove(cell);  // Toggle on click
                            modifiedDuringDrag.add(cell);
                            nextGeneration = GenericLife.nextGeneration(tiling, alive);
                            repaint();
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent evt) {
                        isDragging = false;
                    }
                });

                GenericLifeGui.this.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent evt) {
                        if (!isDragging || pBuffer == null) return;
                        int x = evt.getX(), y = evt.getY();
                        if (x < 0 || x >= pBuffer.getWidth() || y < 0 || y >= pBuffer.getHeight()) return;
                        int idx = pBuffer.getRGB(x, y) & 0xffffff;
                        if (idx > 0 && idx < bufferIndex.size()) {
                            Cell cell = bufferIndex.get(idx);
                            if (modifiedDuringDrag.add(cell)) { // Only once per drag
                                alive.add(cell); // Always mark as alive
                                nextGeneration = GenericLife.nextGeneration(tiling, alive);
                                repaint();
                            }
                        }
                    }
                });

                JPanel toolbar = new JPanel();
                toolbar.setLayout(new FlowLayout());
                frame.getContentPane().add(toolbar, BorderLayout.NORTH);

                toolbar.add(new JButton(
                    new AbstractAction("Step") {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            alive = nextGeneration;
                            nextGeneration = GenericLife.nextGeneration(tiling, alive);
                            repaint();
                        }
                    }));

                toolbar.add(new JButton(
                    new AbstractAction("Text") {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            JDialog textDialog = new JDialog(frame, "Text", Dialog.ModalityType.APPLICATION_MODAL);
                            textDialog.getContentPane().setLayout(new BorderLayout());
                            JTextPane textPane = new JTextPane();
                            textDialog.getContentPane().add(textPane, BorderLayout.CENTER);

                            textPane.setText(tiling.format(alive));

                            //textDialog.pack();
                            textDialog.setBounds(frame.getBounds());
                            textDialog.setVisible(true);
                        }
                    }));

                toolbar.add(new JButton(
                    new AbstractAction("Clear") {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            alive.clear();
                            nextGeneration.clear();
                            repaint();
                        }
                    }));

                // Export button: SVG or PDF
                toolbar.add(new JButton(new AbstractAction("Export...") {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setDialogTitle("Export configuration (SVG or PDF)");
                        chooser.setSelectedFile(new File("life-export.svg"));
                        int rc = chooser.showSaveDialog(GenericLifeGui.this);
                        if (rc != JFileChooser.APPROVE_OPTION) return;
                        File file = chooser.getSelectedFile();
                        String fname = file.getName().toLowerCase();
                        try {
                            if (fname.endsWith(".svg") || fname.endsWith(".svgz")) {
                                exportAsSVG(file, getWidth(), getHeight());
                            } else if (fname.endsWith(".pdf")) {
                                exportAsPDF(file, getWidth(), getHeight());
                            } else {
                                File fsvg = new File(file.getParentFile(), file.getName() + ".svg");
                                exportAsSVG(fsvg, getWidth(), getHeight());
                            }
                            JOptionPane.showMessageDialog(GenericLifeGui.this, "Exported to " + file.getAbsolutePath());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(GenericLifeGui.this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }));

                frame.setSize(500, 700);
                frame.setVisible(true);
            }
        });
    }

    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);

        // Use the shared vector-capable renderer for on-screen painting
        Graphics2D g = (Graphics2D) g0.create();
        try {
            renderToGraphics(g, getWidth(), getHeight());
        } finally {
            g.dispose();
        }

        // Build the hit-test buffer (pBuffer) for mouse interaction as before.
        // This buffer encodes polygons with unique integer colors used for hit testing.
        Rectangle bounds = new Rectangle(getSize());
        bounds.x = centre.x - (bounds.width >> 1);
        bounds.y = centre.y - (bounds.height >> 1);

        Set<Cell> visited = new HashSet<Cell>();
        Set<Cell> unvisited = new HashSet<Cell>(alive);
        bufferIndex = new ArrayList<Cell>();
        bufferIndex.add(null);
        if (pBuffer == null || pBuffer.getWidth() != bounds.width || pBuffer.getHeight() != bounds.height) {
            pBuffer = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics g2 = pBuffer.createGraphics();
        try {
            // Clear buffer
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, bounds.width, bounds.height);

            unvisited.add(tiling.initialCell());
            while (!unvisited.isEmpty()) {
                Iterator<Cell> it = unvisited.iterator();
                Cell current = it.next();
                it.remove();
                visited.add(current);

                Rectangle cellBounds = new Rectangle(-1, -1);
                int[][] cellVertices = tiling.bounds(current);
                int[] xs = cellVertices[0], ys = cellVertices[1];
                int[] tx = Arrays.copyOf(xs, xs.length);
                int[] ty = Arrays.copyOf(ys, ys.length);
                for (int i = 0; i < tx.length; i++) {
                    cellBounds.add(tx[i], ty[i]);
                    tx[i] -= bounds.x;
                    ty[i] -= bounds.y;
                }

                if (!bounds.intersects(cellBounds)) continue;

                bufferIndex.add(current);
                int colorIndex = bufferIndex.size() - 1;
                g2.setColor(new Color(colorIndex));
                g2.fillPolygon(tx, ty, tx.length);
                g2.setColor(Color.BLACK);
                g2.drawPolygon(tx, ty, tx.length);

                for (Cell neighbour : tiling.neighbours(current)) {
                    if (!visited.contains(neighbour)) unvisited.add(neighbour);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Shared rendering routine that draws the current configuration into any
     * Graphics2D. This is used both for on-screen painting and for vector export.
     */
    private void renderToGraphics(Graphics2D g, int width, int height) {
        // Enable antialiasing for smooth edges
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle bounds = new Rectangle(width, height);
        bounds.x = centre.x - (bounds.width >> 1);
        bounds.y = centre.y - (bounds.height >> 1);

        Set<Cell> visited = new HashSet<Cell>();
        Set<Cell> unvisited = new HashSet<Cell>(alive);
        unvisited.add(tiling.initialCell());

        // Use a copy of nextGeneration since we don't want export to mutate state
        Set<Cell> ngCopy = new HashSet<>(nextGeneration);

        while (!unvisited.isEmpty()) {
            Iterator<Cell> it = unvisited.iterator();
            Cell current = it.next();
            it.remove();
            visited.add(current);

            Rectangle cellBounds = new Rectangle(-1, -1);
            int[][] cellVertices = tiling.bounds(current);
            int[] xs = Arrays.copyOf(cellVertices[0], cellVertices[0].length);
            int[] ys = Arrays.copyOf(cellVertices[1], cellVertices[1].length);
            for (int i = 0; i < xs.length; i++) {
                cellBounds.add(xs[i], ys[i]);
                xs[i] -= bounds.x;
                ys[i] -= bounds.y;
            }

            if (!bounds.intersects(cellBounds)) continue;

            boolean isAlive = alive.contains(current);
            boolean isChanging = ngCopy.contains(current) != isAlive;
            Color fill = isChanging ? (isAlive ? PENDING_DEADCOL : PENDING_LIVECOL)
                                    : (isAlive ? LIVECOL : DEADCOL);

            g.setPaint(fill);
            g.fillPolygon(xs, ys, xs.length);
            g.setPaint(GRIDCOL);
            g.drawPolygon(xs, ys, xs.length);

            for (Cell neighbour : tiling.neighbours(current)) {
                if (!visited.contains(neighbour)) unvisited.add(neighbour);
            }
        }

        // restore AA
        if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Export the current view to an SVG file using Batik's SVGGraphics2D.
     */
    private void exportAsSVG(File outFile, int width, int height) throws Exception {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                width, height, width, height));

        // Background rectangle using DEADCOL
        sb.append(String.format("<rect width=\"100%%\" height=\"100%%\" fill=\"#%06X\" />\n", DEADCOL.getRGB() & 0xFFFFFF));

        Rectangle bounds = new Rectangle(width, height);
        bounds.x = centre.x - (bounds.width >> 1);
        bounds.y = centre.y - (bounds.height >> 1);

        Set<Cell> visited = new HashSet<Cell>();
        Set<Cell> unvisited = new HashSet<Cell>(alive);
        unvisited.add(tiling.initialCell());
        Set<Cell> ngCopy = new HashSet<Cell>(nextGeneration);

        while (!unvisited.isEmpty()) {
            Iterator<Cell> it = unvisited.iterator();
            Cell current = it.next();
            it.remove();
            visited.add(current);

            int[][] cellVertices = tiling.bounds(current);
            int[] xs = Arrays.copyOf(cellVertices[0], cellVertices[0].length);
            int[] ys = Arrays.copyOf(cellVertices[1], cellVertices[1].length);

            Rectangle cellBounds = new Rectangle(-1, -1);
            for (int i = 0; i < xs.length; i++) {
                cellBounds.add(xs[i], ys[i]);
                xs[i] -= bounds.x;
                ys[i] -= bounds.y;
            }
            if (!bounds.intersects(cellBounds)) continue;

            boolean isAlive = alive.contains(current);
            boolean isChanging = ngCopy.contains(current) != isAlive;
            Color fill = isChanging ? (isAlive ? PENDING_DEADCOL : PENDING_LIVECOL) : (isAlive ? LIVECOL : DEADCOL);

            StringBuilder pts = new StringBuilder();
            for (int i = 0; i < xs.length; i++) {
                if (i > 0) pts.append(' ');
                pts.append(xs[i]).append(',').append(ys[i]);
            }

            String fillHex = String.format("#%06X", fill.getRGB() & 0xFFFFFF);
            String strokeHex = String.format("#%06X", GRIDCOL.getRGB() & 0xFFFFFF);

            sb.append(String.format("<polygon points=\"%s\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1\" />\n",
                    pts.toString(), fillHex, strokeHex));

            for (Cell neighbour : tiling.neighbours(current)) {
                if (!visited.contains(neighbour)) unvisited.add(neighbour);
            }
        }

        sb.append("</svg>\n");

        File parent = outFile.getParentFile();
        if (parent != null) parent.mkdirs();

        try (Writer w = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8")) {
            w.write(sb.toString());
        }
    }

    /**
     * PDF export is not performed inside the JVM in this dependency-free version.
     * We instead write an SVG beside the requested PDF filename and inform the caller.
     */
    private void exportAsPDF(File outFile, int width, int height) throws Exception {
        File svgOut = new File(outFile.getParentFile(), outFile.getName().replaceAll("\\.pdf$", "") + ".svg");
        exportAsSVG(svgOut, width, height);
        throw new UnsupportedOperationException("PDF export not available without external libraries. SVG written to: " + svgOut.getAbsolutePath() + "\\nConvert to PDF using Inkscape or rsvg-convert.");
    }
}
