package fontEditor;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

import Document.Document;
import structures.LSDJFont;
import utils.FileDialogLauncher;
import utils.FontIO;
import utils.RomUtilities;

public class FontEditor extends JFrame implements FontMap.TileSelectListener, TileEditor.TileChangedListener {

    private static final long serialVersionUID = 5296681614787155252L;

    private final JCheckBox displayGfxCharacters = new JCheckBox();
    private final FontMap fontMap;
    private final TileEditor tileEditor;

    private final JComboBox<String> fontSelector;

    private byte[] romImage = null;
    private int fontOffset = -1;
    private int selectedFontOffset = -1;
    private int previousSelectedFont = -1;

    public FontEditor(JFrame parent, Document document) {
        parent.setEnabled(false);

        setTitle("Font Editor");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setBounds(100, 100, 800, 600);
        setResizable(true);
        GridBagConstraints constraints = new GridBagConstraints();

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        createFileMenu(menuBar);

        createEditMenu(menuBar);

        GridBagLayout layout = new GridBagLayout();
        JPanel contentPane = new JPanel();
        contentPane.setLayout(layout);
        setContentPane(contentPane);

        tileEditor = new TileEditor();
        tileEditor.setMinimumSize(new Dimension(240, 240));
        tileEditor.setPreferredSize(new Dimension(240, 240));
        tileEditor.setTileChangedListener(this);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridheight = 6;
        contentPane.add(tileEditor, constraints);

        fontSelector = new JComboBox<>();
        fontSelector.setEditable(true);
        // TODO is there a way to remove the action listener implementation from this class?
        fontSelector.addItemListener(this::fontSelectorItemChanged);
        fontSelector.addActionListener(this::fontSelectorAction);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridheight = 1;
        constraints.gridwidth = 3;
        contentPane.add(fontSelector, constraints);


        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.X_AXIS));
        FontEditorColorSelector colorSelector = new FontEditorColorSelector(colorPanel);
        colorSelector.addChangeEventListener(new ChangeEventListener() {
            @Override
            public void onChange(int color, ChangeEventMouseSide side) {
                if (side == ChangeEventMouseSide.LEFT)
                    setColor(color);
                else
                    setRightColor(color);
            }
        });

        setColor(1);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        contentPane.add(colorPanel, constraints);

        JPanel shiftButtonPanel = new JPanel();
        shiftButtonPanel.setLayout(new BoxLayout(shiftButtonPanel, BoxLayout.X_AXIS));

        addImageButtonToPanel(shiftButtonPanel, "/shift_up.png", "Rotate up", e -> tileEditor.shiftUp(tileEditor.getTile()));
        addImageButtonToPanel(shiftButtonPanel, "/shift_down.png", "Rotate down", e -> tileEditor.shiftDown(tileEditor.getTile()));
        addImageButtonToPanel(shiftButtonPanel, "/shift_left.png", "Rotate left", e -> tileEditor.shiftLeft(tileEditor.getTile()));
        addImageButtonToPanel(shiftButtonPanel, "/shift_right.png", "Rotate right", e -> tileEditor.shiftRight(tileEditor.getTile()));

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.NONE;
        contentPane.add(shiftButtonPanel, constraints);

        displayGfxCharacters.setText("Show graphics characters");
        displayGfxCharacters.setToolTipText("Changes made to graphics characters will apply to all fonts.");
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        contentPane.add(displayGfxCharacters, constraints);

        fontMap = new FontMap();
        fontMap.setMinimumSize(new Dimension(128, 16 * 8 * 2));
        fontMap.setPreferredSize(new Dimension(128, 16 * 8 * 2));
        fontMap.setTileSelectListener(this);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.ipadx = 0;
        constraints.ipady = 0;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        contentPane.add(fontMap, constraints);

        setMinimumSize(layout.preferredLayoutSize(contentPane));
        pack();

        setRomImage(document.romImage());

        displayGfxCharacters.addActionListener(e -> {
            fontMap.setShowGfxCharacters(displayGfxCharacters.isSelected());
            if (!displayGfxCharacters.isSelected()) {
                tileSelected(0);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                document.setRomImage(fontMap.romImage());
                parent.setEnabled(true);
            }
        });
    }

    private void addImageButtonToPanel(JPanel panel, String imagePath, String altText, ActionListener event) {
        BufferedImage buttonImage = loadImage(imagePath);
        JButton button = new JButton();
        setUpButtonIconOrText(button, buttonImage, altText);
        button.addActionListener(event);
        panel.add(button);
    }

    private void addMenuEntry(JMenu destination, String name, int key, ActionListener event) {
        JMenuItem newMenuEntry = new JMenuItem(name);
        newMenuEntry.setMnemonic(key);
        newMenuEntry.addActionListener(event);
        destination.add(newMenuEntry);
    }

    private void createFileMenu(JMenuBar menuBar) {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        addMenuEntry(fileMenu, "Load font...", KeyEvent.VK_L, e -> loadFont());
        addMenuEntry(fileMenu, "Save font...", KeyEvent.VK_S, e -> saveFont());
    }

    private void createEditMenu(JMenuBar menuBar) {
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(editMenu);

        addMenuEntry(editMenu, "Copy Tile", KeyEvent.VK_C, e -> tileEditor.copyTile());
        addMenuEntry(editMenu, "Paste Tile", KeyEvent.VK_V, e -> tileEditor.pasteTile());
    }

    private BufferedImage loadImage(String iconPath) {
        try {
            return javax.imageio.ImageIO.read(getClass().getResource(iconPath));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private void setUpButtonIconOrText(JButton button, BufferedImage image, String altText) {
        if (image != null)
            button.setIcon(new ImageIcon(image));
        else
            button.setText(altText);
    }

    private String getFontName(int i) {
        return RomUtilities.getFontName(romImage, i);
    }

    private void populateFontSelector() {
        fontSelector.removeAllItems();

        for (int i = 0; i < LSDJFont.FONT_COUNT; ++i) {
            char[] name = getFontName(i).toCharArray();

            // Avoids duplicate names by appending incrementing number to duplicates.
            for (int j = 0; j < i; ++j) {
                if (!getFontName(j).equals(new String(name))) {
                    continue;
                }
                char lastChar = name[name.length - 1];
                if (Character.isDigit(lastChar)) {
                    ++lastChar;
                } else {
                    lastChar = '1';
                }
                name[name.length - 1] = lastChar;
                RomUtilities.setFontName(romImage, i, new String(name));
            }

            fontSelector.addItem(new String(name));
        }
    }

    public void setRomImage(byte[] romImage) {
        this.romImage = romImage;
        fontMap.setRomImage(romImage);
        fontMap.setGfxCharOffset(RomUtilities.findGfxFontOffset(romImage));
        tileEditor.setRomImage(romImage);
        tileEditor.setGfxDataOffset(RomUtilities.findGfxFontOffset(romImage));

        fontOffset = RomUtilities.findFontOffset(romImage);
        if (fontOffset == -1) {
            System.err.println("Could not find font offset!");
        }
        int nameOffset = RomUtilities.findFontNameOffset(romImage);
        if (nameOffset == -1) {
            System.err.println("Could not find font name offset!");
        }
        populateFontSelector();
    }

    private void fontSelectorItemChanged(java.awt.event.ItemEvent e) {
        if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
            if (e.getItemSelectable() == fontSelector) {
                // Font changed.
                int index = fontSelector.getSelectedIndex();
                if (index != -1) {
                    previousSelectedFont = index;
                    index = (index + 1) % 3; // Adjusts for fonts being defined in wrong order.
                    selectedFontOffset = fontOffset + index * LSDJFont.FONT_SIZE + LSDJFont.FONT_HEADER_SIZE;
                    fontMap.setFontOffset(selectedFontOffset);
                    tileEditor.setFontOffset(selectedFontOffset);
                }
            }
        }
    }

    private void setColor(int color) {
        assert color >= 1 && color <= 3;
        tileEditor.setColor(color);
    }

    private void setRightColor(int color) {
        assert color >= 1 && color <= 3;
        tileEditor.setRightColor(color);
    }

    public void tileSelected(int tile) {
        tileEditor.setTile(tile);
    }

    public void tileChanged() {
        fontMap.repaint();
    }

    private void fontSelectorAction(java.awt.event.ActionEvent e) {
        switch (e.getActionCommand()) {
            case "comboBoxChanged":
                if (fontSelector.getSelectedIndex() != -1) {
                    previousSelectedFont = fontSelector.getSelectedIndex();
                }
                break;
            case "comboBoxEdited":
                String selectedItem = (String) fontSelector.getSelectedItem();
                if (fontSelector.getSelectedIndex() == -1 && selectedItem != null) {
                    int index = previousSelectedFont;
                    RomUtilities.setFontName(romImage, index, selectedItem);
                    populateFontSelector();
                    fontSelector.setSelectedIndex(index);
                    fontSelector.setSelectedIndex(index);
                } else {
                    previousSelectedFont = fontSelector.getSelectedIndex();
                }
                break;
        }
    }

    private void loadFont() {
        try {
            File f = FileDialogLauncher.load(this, "Open Font", new String[]{ "png", "lsdfnt" });
            if (f == null) {
                return;
            }

            if (f.getName().endsWith("png")) {
                importBitmap(f);
                String fontName = f.getName().replaceFirst(".png$", "").toUpperCase();
                RomUtilities.setFontName(romImage, fontSelector.getSelectedIndex(), fontName);
            } else {
                String fontName = FontIO.loadFnt(f, romImage, selectedFontOffset);
                tileEditor.generateShadedAndInvertedTiles();
                RomUtilities.setFontName(romImage, fontSelector.getSelectedIndex(), fontName);
                tileEditor.tileChanged();
                tileChanged();
            }
            // Refresh the name list.
            int previousIndex = fontSelector.getSelectedIndex();
            populateFontSelector();
            fontSelector.setSelectedIndex(previousIndex);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Couldn't open fnt file.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void importBitmap(File bitmap) {
        try {
            BufferedImage image = ImageIO.read(bitmap);
            if (image.getWidth() != 64 && image.getHeight() != 72) {
                JOptionPane.showMessageDialog(this,
                        "Make sure your picture has the right dimensions (64 * 72 pixels).");
                return;
            }
            tileEditor.readImage(bitmap.getName(), image);
            tileEditor.tileChanged();
            tileChanged();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Couldn't load the given picture.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveFont() {
        File f = FileDialogLauncher.save(this, "Export Font", "png");
        if (f == null) {
            return;
        }
        BufferedImage image = tileEditor.createImage(displayGfxCharacters.isSelected());
        try {
            ImageIO.write(image, "PNG", f);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Couldn't export the font map.\n" + e.getMessage());
            e.printStackTrace();
        }
    }
}
