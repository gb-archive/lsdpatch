// Copyright (C) 2020, Johan Kotlinski

package lsdpatch;

import Document.*;
import fontEditor.FontEditor;
import kitEditor.KitEditor;
import net.miginfocom.swing.MigLayout;
import paletteEditor.PaletteEditor;
import songManager.SongManager;
import utils.EditorPreferences;
import utils.FileDialogLauncher;
import utils.RomUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;

public class MainWindow extends JFrame implements IDocumentListener {
    JTextField romTextField = new JTextField();
    JTextField savTextField = new JTextField();

    JButton upgradeRomButton = new JButton("Upgrade ROM");
    JButton songManagerButton = new JButton("Songs");
    JButton editKitsButton = new JButton("Sample Kits");
    JButton editFontsButton = new JButton("Fonts");
    JButton editPalettesButton = new JButton("Palettes");
    JButton saveButton = new JButton("Save...");

    MainWindow() {
        document.subscribe(this);

        updateTitle();
        JPanel panel = new JPanel();
        getContentPane().add(panel);
        MigLayout rootLayout = new MigLayout("wrap 6");
        panel.setLayout(rootLayout);

        addSelectors(panel);

        panel.add(new JSeparator(), "span 5");

        upgradeRomButton.addActionListener(e -> openRomUpgradeTool());
        panel.add(upgradeRomButton);

        songManagerButton.addActionListener(e -> openSongManager());
        panel.add(songManagerButton);

        editKitsButton.addActionListener(e -> new KitEditor(document).setLocationRelativeTo(this));
        panel.add(editKitsButton);
        editFontsButton.addActionListener(e -> openKitEditor());
        panel.add(editFontsButton);

        editPalettesButton.addActionListener(e -> openPaletteEditor());
        panel.add(editPalettesButton, "grow x");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!document.isDirty() || JOptionPane.showConfirmDialog(null,
                        "Quit without saving changes?",
                        "Unsaved changes",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
                    setDefaultCloseOperation(EXIT_ON_CLOSE);
                }
                super.windowClosing(e);
            }
        });
    }

    private void openRomUpgradeTool() {
        RomUpgradeTool romUpgradeTool = new RomUpgradeTool(document);
        romUpgradeTool.setLocationRelativeTo(this);
        romUpgradeTool.setVisible(true);
    }

    private void openSongManager() {
        SongManager savManager = new SongManager(document);
        savManager.setLocationRelativeTo(this);
        savManager.setVisible(true);
    }

    private void openPaletteEditor() {
        PaletteEditor editor = new PaletteEditor(document);
        editor.setLocationRelativeTo(this);
        editor.setVisible(true);
    }

    private void openKitEditor() {
        FontEditor fontEditor = new FontEditor(document);
        fontEditor.setLocationRelativeTo(this);
        fontEditor.setVisible(true);
    }

    final Document document = new Document();

    private void addSelectors(JPanel panel) {
        romTextField.setMinimumSize(new Dimension(300, 0));
        romTextField.setText(EditorPreferences.lastPath("gb"));
        romTextField.setEditable(false);
        panel.add(romTextField, "span 4, grow x");

        JButton browseRomButton = new JButton("Browse...");
        browseRomButton.addActionListener(e -> onBrowseRomButtonPress());
        panel.add(browseRomButton);

        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> onSave());
        panel.add(saveButton, "span 1 4, grow y");

        savTextField.setMinimumSize(new Dimension(300, 0));
        savTextField.setEditable(false);
        savTextField.setText(EditorPreferences.lastPath("sav"));
        panel.add(savTextField, "span 4, grow x");

        JButton browseSavButton = new JButton("Browse...");
        browseSavButton.addActionListener(e -> onBrowseSavButtonPress());
        panel.add(browseSavButton);

        document.loadRomImage(EditorPreferences.lastPath("gb"));
        document.loadSavFile(EditorPreferences.lastPath("sav"));
        updateButtonsFromTextFields();
    }

    private void onBrowseRomButtonPress() {
        if (document.isDirty() && JOptionPane.showConfirmDialog(null,
                "Load new ROM without saving changes?",
                "Unsaved changes",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
            return;
        }

        File romFile = FileDialogLauncher.load(this, "Select LSDj ROM Image", "gb");
        if (romFile == null) {
            return;
        }
        String romPath = romFile.getAbsolutePath();
        romTextField.setText(romPath);
        document.loadRomImage(romPath);
        String savPath = romPath.replaceFirst(".gb", ".sav");
        document.loadSavFile(savPath);
        EditorPreferences.setLastPath("sav", savPath);
        savTextField.setText(savPath);
        updateButtonsFromTextFields();
    }

    private void onBrowseSavButtonPress() {
        if (document.isSavDirty() && JOptionPane.showConfirmDialog(null,
                "Load new .sav without saving changes?",
                "Unsaved changes",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
            return;
        }

        File savFile = FileDialogLauncher.load(this, "Load Save File", "sav");
        if (savFile == null) {
            return;
        }
        savTextField.setText(savFile.getAbsolutePath());
        updateButtonsFromTextFields();
    }

    void updateButtonsFromTextFields() {
        byte[] romImage = document.romImage();
        boolean romOk = romImage != null;
        String savPath = savTextField.getText();
        boolean savPathOk = savPath.endsWith(".sav") && new File(savPath).exists();
        boolean foundPalettes = romOk && RomUtilities.getNumberOfPalettes(romImage) != -1;

        romTextField.setBackground(romOk ? Color.white : Color.pink);
        savTextField.setBackground(savPathOk ? Color.white : Color.pink);

        editKitsButton.setEnabled(romOk);
        editFontsButton.setEnabled(romOk && foundPalettes);
        editPalettesButton.setEnabled(romOk && foundPalettes);
        upgradeRomButton.setEnabled(romOk);
        songManagerButton.setEnabled(savPathOk && romOk);
    }

    public void onRomDirty(boolean dirty) {
        updateTitle();
        if (dirty) {
            saveButton.setEnabled(true);
        }
    }

    public void onSavDirty(boolean dirty) {
        updateTitle();
        if (dirty) {
            saveButton.setEnabled(true);
        }
    }

    private void updateTitle() {
        String title = "LSDPatcher v" + LSDPatcher.getVersion();
        if (document.romImage() != null) {
            title = title + " - " + document.romFile().getName();
            if (document.isDirty()) {
                title = title + '*';
            }
        }
        setTitle(title);
    }

    private void onSave() {
        File f = FileDialogLauncher.save(this, "Save ROM Image", "gb");
        if (f == null) {
            return;
        }
        String romPath = f.getAbsolutePath();

        try (FileOutputStream fileOutputStream = new FileOutputStream(romPath)) {
            fileOutputStream.write(document.romImage());
            fileOutputStream.close();
            String savPath = romPath.replace(".gb", ".sav");
            document.savFile().saveAs(savPath);
            romTextField.setText(romPath);
            savTextField.setText(savPath);
            document.setRomFile(new File(romPath));
            document.loadSavFile(savPath);
            document.clearDirty();
            EditorPreferences.setLastPath("gb", romPath);
            EditorPreferences.setLastPath("sav", savPath);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    e.getLocalizedMessage(),
                    "File save failed!",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
