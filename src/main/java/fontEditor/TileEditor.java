package fontEditor;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import structures.LSDJFont;

class TileEditor extends JPanel implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener {

    private static final long serialVersionUID = 4048727729255703626L;

    public interface TileChangedListener {
        void tileChanged();
    }

    private final LSDJFont font;
    private int selectedTile = 0;
    private int color = 3;
    private int rightColor = 3;

    private int[][] clipboard = null;

    private TileChangedListener tileChangedListener;

    TileEditor() {
        font = new LSDJFont();
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    void setRomImage(byte[] romImage) {
        font.setRomImage(romImage);
    }

    void setFontOffset(int offset) {
        font.setDataOffset(offset);
        repaint();
    }

    void setGfxDataOffset(int offset) {
        font.setGfxDataOffset(offset);
        repaint();
    }

    void setTile(int tile) {
        selectedTile = tile;
        repaint();
    }

    int getTile() {
        return selectedTile;
    }

    void shiftUp(int tile) {
        font.rotateTileUp(tile);
        tileChanged();
    }

    void shiftDown(int tile) {
        font.rotateTileDown(tile);
        tileChanged();
    }

    void shiftRight(int tile) {
        font.rotateTileRight(tile);
        tileChanged();
    }

    void shiftLeft(int tile) {
        font.rotateTileLeft(tile);
        tileChanged();
    }

    private int getColor(int tile, int x, int y) {
        return font.getTilePixel(tile, x, y);
    }

    private void switchColor(Graphics g, int c) {
        switch (c & 3) {
            case 0:
                g.setColor(Color.white);
                break;
            case 1:
                g.setColor(Color.lightGray);
                break;
            case 2:
                g.setColor(Color.darkGray); // Not used.
                break;
            case 3:
                g.setColor(Color.black);
                break;
        }
    }

    private int getMinimumDimension() {
        return getWidth() < getHeight() ? getWidth() : getHeight();
    }

    private void paintGrid(Graphics g) {
        g.setColor(java.awt.Color.gray);
        int minimumDimension = getMinimumDimension();
        int offsetX = (getWidth() - minimumDimension) / 2;
        int offsetY = (getHeight() - minimumDimension) / 2;
        int dx = minimumDimension / 8;
        int minimumDimensionSquare = (minimumDimension / 8) * 8;
        for (int x = dx + offsetX; x < minimumDimensionSquare + offsetX; x += dx) {
            g.drawLine(x, offsetY, x, minimumDimensionSquare + offsetY);
        }

        int dy = minimumDimension / 8;
        for (int y = dy + offsetY; y < minimumDimensionSquare + offsetY; y += dy) {
            g.drawLine(offsetX, y, offsetX + minimumDimensionSquare, y);
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int minimumDimension = getMinimumDimension();
        int offsetX = (getWidth() - minimumDimension) / 2;
        int offsetY = (getHeight() - minimumDimension) / 2;
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                int color = getColor(selectedTile, x, y);
                switchColor(g, color);
                int pixelWidth = minimumDimension / 8;
                int pixelHeight = minimumDimension / 8;
                g.fillRect(offsetX + x * pixelWidth, offsetY + y * pixelHeight, pixelWidth, pixelHeight);
            }
        }

        paintGrid(g);
    }

    private void doMousePaint(java.awt.event.MouseEvent e) {
        int minimumDimension = getMinimumDimension();
        int offsetX = (getWidth() - minimumDimension) / 2;
        int offsetY = (getHeight() - minimumDimension) / 2;

        int x = ((e.getX() - offsetX) * 8) / minimumDimension;
        int y = ((e.getY() - offsetY) * 8) / minimumDimension;
        if (x < 0 || x >= 8 || y < 0 || y >= 8)
            return;
        if (SwingUtilities.isLeftMouseButton(e))
            setColor(x, y, color);
        else if (SwingUtilities.isRightMouseButton(e))
            setColor(x, y, rightColor);
        tileChanged();
    }

    private void setColor(int x, int y, int color) {
        font.setTilePixel(selectedTile, x, y, color);
    }

    public void mouseEntered(java.awt.event.MouseEvent e) {
    }

    public void mouseExited(java.awt.event.MouseEvent e) {
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
    }

    public void mousePressed(java.awt.event.MouseEvent e) {
    }

    public void mouseClicked(java.awt.event.MouseEvent e) {
        doMousePaint(e);
    }

    public void mouseMoved(java.awt.event.MouseEvent e) {
    }

    public void mouseDragged(java.awt.event.MouseEvent e) {
        doMousePaint(e);
    }

    void setColor(int color) {
        assert color >= 1 && color <= 3;
        this.color = color;
    }

    void setRightColor(int color) {
        assert color >= 1 && color <= 3;
        this.rightColor = color;
    }

    void setTileChangedListener(TileChangedListener l) {
        tileChangedListener = l;
    }

    void copyTile() {
        if (clipboard == null) {
            clipboard = new int[8][8];
        }
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                clipboard[x][y] = getColor(selectedTile, x, y);
            }
        }
    }

    void generateShadedAndInvertedTiles() {
        font.generateShadedAndInvertedTiles();
    }

    void pasteTile() {
        if (clipboard == null) {
            return;
        }
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                int c = clipboard[x][y];
                if (c < 3) {
                    ++c; // Adjusts from Game Boy Color to editor color.
                }
                setColor(x, y, c);
            }
        }
        tileChanged();
    }

    void tileChanged() {
        repaint();
        generateShadedAndInvertedTiles();
        tileChangedListener.tileChanged();
    }

    void readImage(String name, BufferedImage image) {
        font.loadImageData(name, image);

    }

    BufferedImage createImage(boolean includeGfxCharacters) {
        return font.saveDataToImage(includeGfxCharacters);
    }
}
