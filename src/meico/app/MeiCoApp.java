package meico.app;

/**
 * This is a standalone application that runs all the conversions.
 * @author Axel Berndt.
 */

import meico.mei.Mei;
import meico.midi.Midi;
import meico.msm.Msm;

import net.miginfocom.swing.MigLayout;
import nu.xom.ParsingException;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MeiCoApp extends JFrame {

    private List<Mei4Gui> music;
    private Sequencer sequencer;

    private final JLabel statusMessage;             // the message component of the statusbar
    private final JLabel fileListPanel;             // a container for the list of loaded and generated files, here, the main interactions take place
    private final JPanel backgroundPanel;           // the conainer of everything that happens in the work area of the window
    private final JPanel statusPanel;               // the container of the statusbar components
    private final JLabel dropLabel;                 // a text label that tells the user to drop files
    private final JLabel meilabel;                  // mei logo
    private final JLabel msmlabel;                  // msm logo
    private final JLabel midilabel;                 // midi logo
    private final JLabel loadIcon;                  // a clickable icon in the statusbar to start the filo open dialog
    private final JLabel closeAllIcon;              // a clickable icon to close all loaded data in the work area, it is placed in the statusbar


    /**
     * the main method to run meico
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0)               // if meico.jar is called without command line arguments
            new MeiCoApp();                 // start meico in window mode
        else                                // in case of command line arguments
            commandLineMode(args);          // run the command line mode
    }

    /**
     * call this method to start the program in command line mode
     * it shows you all you need if you want to use meico in your application
     *
     * @param args The following parameter strings are used
     *             - "?" or "help": for this command line help text. If you use this, any other arguments are skipped.
     *             - "addIds": to add xml:ids to note, rest and chord elements in mei, as far as they do not have an id; meico will output a revised mei file
     *             - "resolveCopyOfs": mei elements with a copyOf attribute are resolved into selfcontained elements with an own xml:id; meico will output a revised mei file
     *             - "msm": converts mei to msm; meico will write an msm file to the path of the mei
     *             - "midi": converts mei to msm to midi; meico will output a midi file to the path of the mei
     *             - "debug": to write debug versions of mei and msm
     *             - Path tho the mei file (e.g., D:\Arbeit\Software\Java\MEI Converter\test files\Hummel_Concerto_for_trumpet.mei), this should always be the last parameter -  always in quotes!
     */
    public static void commandLineMode(String[] args) {
        for (String arg : args) {
            if (arg.equals("?") || arg.equals("help")) {
                System.out.println("Meico requires the following arguments:\n");
                System.out.println("[?] or [help]    for this command line help text. If you use this, any other arguments are skipped.");
                System.out.println("[addIds]         to add xml:ids to note, rest and chord elements in mei, as far as they do not have an id; meico will output a revised mei file");
                System.out.println("[resolveCopyOfs] mei elements with a copyOf attribute are resolved into selfcontained elements with an own xml:id; meico will output a revised mei file");
                System.out.println("[msm]            converts mei to msm; meico will write an msm file to the path of the mei");
                System.out.println("[midi]           converts mei (to msm, internally) to midi; meico will output a midi file to the path of the mei");
                System.out.println("[debug]          to write debug versions of the mei and msm files  to the path");
                System.out.println("\nThe final argument should always be a path to a valid mei file (e.g., \"C:\\myMeiCollection\\test.mei\"); always in quotes! This is the only mandatory argument if you want to convert something.");
                return;
            }
        }

        // load the file
        File meiFile;
        try {
            System.out.println("Loading file: " + args[args.length-1]);
            meiFile = new File(args[args.length-1]);                    // load mei file
        } catch (NullPointerException error) {
            error.printStackTrace();                                    // print error to console
            return;
        }
        Mei mei = null;                              // read an mei file (without validation, hence, false)
        try {
            mei = new Mei(meiFile, false);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (ParsingException e) {
            e.printStackTrace();
            return;
        }
        if (mei.isEmpty()) {
            System.out.println("Failed.");
            return;
        }

        // what does the user want meico to do with the file just loaded?
        boolean addIds = false;
        boolean resolveCopyOfs = false;
        boolean msm = false;
        boolean midi = false;
        boolean debug = false;
        for (int i = 0; i < args.length-1; ++i) {
            if (args[i].equals("addIds")) { addIds = true; continue; }
            if (args[i].equals("resolveCopyOfs")) { resolveCopyOfs = true; continue; }
            if (args[i].equals("msm")) { msm = true; continue; }
            if (args[i].equals("midi")) { midi = true; continue; }
            if (args[i].equals("debug")) { debug = true; }
        }

        // optional mei processing functions
        if (resolveCopyOfs) {
            System.out.println("Processing mei: resolving copyOfs.");
            mei.resolveCopyofs();                       // this call is part of the exportMsm() method but can also be called alone to expand the mei source and write it to the file system
        }
        if (addIds) {
            System.out.println("Processing mei: adding xml:ids.");
            mei.addIds();                               // generate ids for note, rest, mRest, multiRest, and chord elements that have no xml:id attribute
        }
        if (resolveCopyOfs || addIds) {
            mei.writeMei();                             // this outputs an expanded mei file with more xml:id attributes and resolved copyof's
        }

        if (!(msm || midi)) return;                     // if no conversion is required, we are done here

        // convert mei -> msm -> midi
        System.out.println("Converting mei to msm.");
        List<Msm> msms = mei.exportMsm(720, !debug);    // usually, the application should use mei.exportMsm(720); the cleanup flag is just for debugging (in debug mode no cleanup is done)
        if (msms.isEmpty()) {
            System.out.println("No msm data created.");
            return;
        }

        if (debug)
            mei.writeMei(args[args.length-1].substring(0, args[args.length-1].length()-4) + "-debug.mei"); // After the msm export, there is some new stuff in the mei ... mainly the date and dur attribute at measure elements (handy to check for numeric problems that occured during conversion), some ids and expanded copyofs. This was required for the conversion and can be output with this function call. It is, however, mainly interesting for debugging.

        if (msm) {
            System.out.println("Writing msm to file system: ");
            for (int i = 0; i < msms.size(); ++i) {
                if (!debug) msms.get(i).removeRests();  // purge the data (some applications may keep the rests from the mei; these should not call this function)
                msms.get(i).writeMsm();                 // write the msm file to the file system
                System.out.println("\t" + msms.get(i).getFile().getPath());
            }
        }

        if (midi) {
            System.out.println("Converting msm to midi and writing midi to file system: ");
            List<Midi> midis = new ArrayList<Midi>();
            for (int i = 0; i < msms.size(); ++i) {
                midis.add(msms.get(i).exportMidi());    // convert msm to midi
                try {
                    midis.get(i).writeMidi();           // write midi file to the file system
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("\t" + midis.get(i).getFile().getPath());
            }
        }
    }

    /**
     * The constructor method which starts meico in window mode with a default title.
     * This allows other applications to start the meico window simply by instantiation.
     * <tt>MeiCoApp m = new MeiCoApp();</tt>
     */
    public MeiCoApp() {
        this("meico - MEI Converter");
    }

    /**
     * The constructor method which starts meico in window mode with a user defined title.
     * @param title the title of the meico window
     */
    public MeiCoApp(String title) {
        super(title);

        this.music = new ArrayList<Mei4Gui>();

        // initialize the midi sequencer for midi playback
        try {
            this.sequencer = MidiSystem.getSequencer();
            this.sequencer.open();
        } catch (MidiUnavailableException e) {
            this.setStatusMessage(e.toString());
            this.sequencer = null;
        }
        if (sequencer != null) {
            // TODO: load a higher quality soundbank

            this.sequencer.addMetaEventListener(new MetaEventListener() {               // Add a listener for meta message events to detect when ...
                public void meta(MetaMessage event) {
                    if (event.getType() == 47) {                                        // ... the sequencer is done playing
                        stopPlayback();                                                 // switch all playMaidi buttons to triangle
                    }
                }
            });
        }

        // set the OS' look and feel (this is mainly relevant for the JFileChooser that is used later)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());        // try to use the system's look and feel
        }
        catch (Throwable ex) {                                                          // if failed
                                                                                        // it hopefully keeps the old look and feel (not tested)
        }

        // keyboard input via key binding
        InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Exit");            // close the window when ESC pressed
        this.getRootPane().getActionMap().put("Exit", new AbstractAction(){             // define the "Exit" action
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();                                                              // close the window (if this is the only window, this will terminate the JVM)
                System.exit(0);                                                         // the program may still run, enforce exit
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "Load");                // start file open dialog to load a file when F1 is pressed
        this.getRootPane().getActionMap().put("Load", new AbstractAction(){             // define the "Load" action
            @Override
            public void actionPerformed(ActionEvent e) {
                fileOpenDialog();                                                       // start file open dialog
            }
        });

        // add the filedrop function to this JFrame
        new FileDrop(null, this.getRootPane(), new FileDrop.Listener() {                // for debugging information replace the null argument by System.out
            public void filesDropped(java.io.File[] files) {                            // this is the fileDrop listener
                setStatusMessage("");

                for (File file : files)                                                 // for each file that has been dropped
                    loadFile(file);                                                     // load it

                doRepaint();
                toFront();                                                              // after the file drop force this window to have the focus
            }
        });

        // some general window settings
        //this.setBounds(100, 100, 1000, 400);                                          // set window size and position
        this.setSize(1000, 500);                                                        // set window size
        this.setResizable(true);                                                        // don't allow resizing
        this.setLocationRelativeTo(null);                                               // set window position
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);                   // what happens when the X is clicked?
        this.setLayout(new BorderLayout());                                             // the layout manager of the main window frame

        // initialize the gui elements
        this.statusPanel = new JPanel();                                                // the container of the statusbar label
        this.backgroundPanel = new JPanel();                                            // a background panel
        this.fileListPanel = new JLabel();                                              // the container to display loaded files
        this.statusMessage = new JLabel();                                              // the statusbar message
        this.loadIcon = new JLabel(new ImageIcon(getClass().getResource("/resources/open-small.png")));             // the file load icon that opens the file chooser dialog
        this.closeAllIcon = new JLabel("\u2716");                                            // the icon to close all loaded data
        this.dropLabel = new JLabel("Drop your mei, msm and midi files here.", JLabel.CENTER);   // the file drop label
//        this.dropLabel = new JLabel("Drop your mei, msm and midi files here.", new ImageIcon(getClass().getResource("/resources/drop-inverse.png")), JLabel.CENTER);   // the file drop label
        this.meilabel = new JLabel(new ImageIcon(getClass().getResource("/resources/mei-inverse.png")), JLabel.CENTER);     // mei icon label
        this.msmlabel = new JLabel(new ImageIcon(getClass().getResource("/resources/msm-inverse.png")), JLabel.CENTER);     // msm icon label
        this.midilabel = new JLabel(new ImageIcon(getClass().getResource("/resources/midi-inverse.png")), JLabel.CENTER);   // midi icon label

        // prepare the components
        this.makeStatusbar();                                                           // compile the statusbar
        this.getContentPane().add(this.statusPanel, BorderLayout.SOUTH);                // display it
        this.makeMainPanel();
        this.getContentPane().add(this.backgroundPanel, BorderLayout.CENTER);           // display it

        this.setVisible(true);                                                          // show the frame
    }

    /**
     * load a file
     * @param file this is the file to be loaded
     */
    private void loadFile(File file) {
        try {
//            this.music.add(new MeiCoMusicObject(file));
            this.music.add(new Mei4Gui(file, this));
        } catch (InvalidFileTypeException e) {
            this.setStatusMessage(e.toString());            // if it is neither of the above file formats, output a statusbar message
        } catch (ParsingException e) {
            this.setStatusMessage(e.toString());            // if it is neither of the above file formats, output a statusbar message
        } catch (IOException e) {
            this.setStatusMessage(e.toString());            // if it is neither of the above file formats, output a statusbar message
        }
    }

    /**
     * this method starts the file chooser to open/load a file into meico
     */
    private void fileOpenDialog() {
        JFileChooser chooser = new JFileChooser();                              // open the fileopen dialog
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("all supported files (*.mei, *.msm, *.mid)", "mei", "msm", "mid"));  // make only suitable file types choosable
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("digital music edition (*.mei)", "mei"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("musical sequence markup (*.msm)", "msm"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("midi file (*.mid)", "mid"));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {      // file open has been approved
            loadFile(chooser.getSelectedFile());                                // load it
            this.doRepaint();
        }
    }

    /**
     * When the content of backgroundPanel changes, it needs to be repainted. That is done with this method.
     */
    private void doRepaint() {
        this.makeMainPanel();                                               // update the working area
        this.getContentPane().validate();                                   // and
        this.getContentPane().repaint();                                    // repaint
    }

    /**
     * this is  called to output a line in the status message
     * @param message the message text
     */
    private void setStatusMessage(String message) {
        this.statusMessage.setText(message);
    }

    /**
     * a helper method that sets up the statusbar
     */
    private void makeStatusbar() {
        // the statusbar
        this.statusPanel.setLayout(new MigLayout(/*Layout Constraints*/ "", /*Column constraints*/ "", /*Row constraints*/ "0[]0"));
        this.statusMessage.setForeground(Color.DARK_GRAY);                              // the text color
        this.statusMessage.setHorizontalAlignment(SwingConstants.LEFT);                 // text alignment
        // TODO: use System.setOut() and System.setErr() to redirect the output stream to statusMessage

        // add a file load icon to the statusbar
        this.loadIcon.setPreferredSize(new Dimension(16, 16));                          // make the icon very small
        this.loadIcon.setHorizontalAlignment(JLabel.CENTER);
        //this.loadIcon.setToolTipText("<html>open file load dialog</html>");
        this.loadIcon.addMouseListener(new MouseAdapter() {                             // add a mouse listener to the button
            @Override
            public void mouseClicked(MouseEvent e) {                                    // when clicked
                fileOpenDialog();                                                       // open the file load dialog
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setStatusMessage("File open dialog");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setStatusMessage("");
            }
        });

        // add a close all icon to the statusbar
        this.closeAllIcon.setPreferredSize(new Dimension(16, 16));
        this.closeAllIcon.setHorizontalAlignment(JLabel.CENTER);
        this.closeAllIcon.setForeground(Color.DARK_GRAY);                               // the text color
        this.closeAllIcon.setFont(new Font("default", Font.PLAIN, 15));                 // font type, style and size
        //this.closeAllIcon.setToolTipText("<html>clear the workspace</html>");
        this.closeAllIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((sequencer != null) && sequencer.isOpen())
                    sequencer.stop();
                music.clear();
                doRepaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setStatusMessage("Clear the workspace");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setStatusMessage("");
            }
        });

        this.statusPanel.add(this.statusMessage, "alignx left, aligny center, push");   // add the status label, i.e. the text message, to the status panel
        this.statusPanel.add(this.loadIcon, "right");                                   // add the load icon to the status panel
        this.statusPanel.add(this.closeAllIcon, "right");                               // add the closeAll icon to the status panel
    }

    /**
     * method to setup the main workspace
     */
    private void makeMainPanel() {
        this.backgroundPanel.removeAll();                                                   // clear background panel to make it anew

        // set the background panel
        this.backgroundPanel.setBackground(Color.DARK_GRAY);
        this.backgroundPanel.setLayout(new MigLayout(/*Layout Constraints*/ "", /*Column constraints*/ "", /*Row constraints*/ "")); // use the MigLayout manager also within the background panel

        // add the file format logos
        this.backgroundPanel.add(meilabel, "center, pushx, gap 20 20 30 30");
        this.backgroundPanel.add(msmlabel, "center, pushx, gap 20 20 30 30");
        this.backgroundPanel.add(midilabel, "center, pushx, gap 20 20 30 30, wrap");

        if (this.music.isEmpty()) {                                                         // if no files are loaded
            // create a file drop label
            this.dropLabel.setForeground(Color.GRAY);                                       // text color
            this.dropLabel.setHorizontalTextPosition(JLabel.CENTER);                        // center the label text within the label
            this.dropLabel.setFont(new Font("default", Font.PLAIN, 20));                    // font type, style and size
            this.backgroundPanel.add(dropLabel, "span, grow, push");                        // add it to the background panel
//            this.dropLabel.addMouseListener(new MouseAdapter() {                             // add a mouse listener to the button
//                @Override
//                public void mouseClicked(MouseEvent e) {                                    // when clicked
//                    fileOpenDialog();                                                       // open the file load dialog
//                }
//            });
        }
        else {                                                                              // otherwise there are files loaded with which we want to interact
            this.fileListPanel.removeAll();
            this.fileListPanel.setOpaque(false);                                            // set the file list panel transparent
            this.fileListPanel.setLayout(new BoxLayout(this.fileListPanel, BoxLayout.PAGE_AXIS));

            for (Mei4Gui m : this.music) {
                this.fileListPanel.add(m.getPanel());
            }

            this.backgroundPanel.add(this.fileListPanel, "span, grow, push");               // add the file list panel to the background panel
        }
    }

    private void stopPlayback() {
        if ((this.sequencer != null) && this.sequencer.isOpen()) {
            this.sequencer.stop();
            this.sequencer.setMicrosecondPosition(0);
        }
        for (Mei4Gui mei : music) {
            for (Mei4Gui.Msm4Gui msm : mei.msm) {
                if (msm.midi != null) {
                    msm.midi.panel[2].setText("\u25BA");
                }
            }
        }
    }

    /**
     * an Mei extension for the windowed meico app
     */
    private class Mei4Gui extends Mei {
        private final JPanel panel;             // the actual gui extension
        private List<Msm4Gui> msm;              // corresponding msm objects
        private MeiCoApp app;                   // reference to the meico app
        private boolean idsAdded;
        private boolean copyofsResolved;
        private int ppq;

        /**
         * constructor
         * @param file
         */
        public Mei4Gui(File file, MeiCoApp app) throws InvalidFileTypeException, IOException, ParsingException {
            this.msm = new ArrayList<Msm4Gui>();

            if (file.getName().substring(file.getName().length()-4).equals(".mei")) {           // if it is an mei file
                this.readMeiFile(file, false);                                                  // load it
            }
            else {                                                                              // otherwise try loading it as an msm (or midi) object
                this.msm.add(new Msm4Gui(file, app));                                                // load it into the msms list
            }

            this.panel = new JPanel();                                                          // initialize the gui component
            this.app = app;                                                                     // store the reference to the meico app
            this.idsAdded = false;
            this.copyofsResolved = false;
            this.ppq = 720;
        }

        /**
         * This method draws and returns the panel that the MeiCoApp displays.
         * @return
         */
        public JPanel getPanel() {
            // create the panel component and its content
            this.panel.removeAll();
            this.panel.setOpaque(false);
            this.panel.setLayout(new MigLayout(/*Layout Constraints*/ "wrap 9", /*Column constraints*/ "[left, 23%:23%:23%][right, 4%:4%:4%][right, 4%:4%:4%][left, 23%:23%:23%][right, 4%:4%:4%][right, 4%:4%:4%][left, 23%:23%:23%][right, 4%:4%:4%][right, 4%:4%:4%]", /*Row constraints*/ ""));

            int skip = 0;

            // mei components
            if (this.isEmpty()) {
                skip = 3;
            }
            else {
                JPopupMenu meiNamePop = new JPopupMenu("MEI Processing");
                meiNamePop.setEnabled(true);
                JMenuItem addIDs = new JMenuItem(new AbstractAction("Add IDs") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        addIds();
                        idsAdded = true;
                        this.setEnabled(false);
                    }
                });
                addIDs.setEnabled(!this.idsAdded);

                JMenuItem resolveCopyofs = new JMenuItem(new AbstractAction("Resolve Copyofs") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        resolveCopyofs();
                        copyofsResolved = true;
                        this.setEnabled(false);
                    }
                });
                resolveCopyofs.setEnabled(!this.copyofsResolved);

                JMenuItem reload = new JMenuItem(new AbstractAction("Reload Original MEI") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        msm = new ArrayList<Msm4Gui>();
                        try {
                            readMeiFile(getFile(), false);
                        } catch (IOException err) {
                            app.setStatusMessage(err.toString());
                        } catch (ParsingException err) {
                            app.setStatusMessage(err.toString());
                        }
                        idsAdded = false;
                        copyofsResolved = false;
                        app.doRepaint();
                    }
                });
                reload.setEnabled(true);

                meiNamePop.add(addIDs);
                meiNamePop.add(resolveCopyofs);
                meiNamePop.add(reload);

                JLabel meiName = new JLabel(this.getFile().getName());
                meiName.setOpaque(true);
                meiName.setBackground(Color.LIGHT_GRAY);
                meiName.setForeground(Color.DARK_GRAY);
                meiName.setBorder(new EmptyBorder(0, 4, 0, 0));
                meiName.setFont(new Font("default", Font.PLAIN, 18));
                //meiName.setToolTipText("<html>" + this.getFile().getPath() + "</html>");
                meiName.setComponentPopupMenu(meiNamePop);
                meiName.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        app.setStatusMessage(getFile().getPath() + ", RIGHT CLICK: mei processing functions");
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        app.setStatusMessage("");
                    }
                });

                final JLabel saveMei = new JLabel(new ImageIcon(getClass().getResource("/resources/save-gray.png")), JLabel.CENTER);
                saveMei.setOpaque(true);
                saveMei.setBackground(Color.LIGHT_GRAY);
                saveMei.setForeground(Color.DARK_GRAY);
                //saveMei.setToolTipText("<html>LEFT CLICK: quick save with default filename <br> RIGHT CLICK: open save dialog</html>");
                saveMei.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        saveMei.setBackground(new Color(232, 232, 232));
                        app.setStatusMessage("LEFT CLICK: quick save with default filename, RIGHT CLICK: open save dialog");
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (saveMei.getBackground() != Color.GRAY)
                            saveMei.setBackground(Color.LIGHT_GRAY);
                        app.setStatusMessage("");
                    }
                    @Override
                    public void mousePressed(MouseEvent e) {
                        saveMei.setBackground(Color.GRAY);
                    }
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (saveMei.contains(e.getPoint())) {                                           // right click for file save dialog
                            if (e.isPopupTrigger()) {
                                JFileChooser chooser = new JFileChooser();                              // open the file save dialog
                                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {      // file save has been approved
                                    writeMei(chooser.getSelectedFile().getAbsolutePath());              // save it
                                }
                            }
                            else {                                                                      // quick save with default filename with left mouse button
                                writeMei();
                            }
                            saveMei.setBackground(new Color(232, 232, 232));
                        }
                        else
                            saveMei.setBackground(Color.LIGHT_GRAY);
                    }
                });

                final JTextField ppqField = new JTextField(Integer.toString(this.ppq), 10);
                ppqField.setHorizontalAlignment(SwingConstants.RIGHT);
                ppqField.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        String input = ppqField.getText().trim();
                        String clean = "";
                        for (int i = 0; i < input.length(); ++i) {
                            if (Character.isDigit(input.charAt(i))) {
                                clean = clean + input.charAt(i);
                            }
                        }
                        if (clean.isEmpty())
                            clean = "720";
                        ppq = Integer.parseInt(clean);
                        ppqField.setText(Integer.toString(ppq));
                    }
                });
                ppqField.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ppqField.getParent().getParent().setVisible(false);
                    }
                });

                JLabel ppqLabel = new JLabel("ppq");
                JPanel ppqPanel = new JPanel();
                ppqPanel.add(ppqField);
                ppqPanel.add(ppqLabel);
                JLabel ppqSetup = new JLabel("Set Time Resolution");
                JPopupMenu mei2msmPop = new JPopupMenu("Conversion Options");
                mei2msmPop.setEnabled(true);
                mei2msmPop.add(ppqSetup);
                mei2msmPop.add(ppqPanel);

                final JLabel mei2msm = new JLabel(new ImageIcon(getClass().getResource("/resources/convert-gray.png")), JLabel.CENTER);
                mei2msm.setOpaque(true);
                mei2msm.setBackground(Color.LIGHT_GRAY);
                mei2msm.setForeground(Color.DARK_GRAY);
                //mei2msm.setToolTipText("<html>convert to msm</html>");
                mei2msm.setComponentPopupMenu(mei2msmPop);
                mei2msm.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        mei2msm.setBackground(new Color(232, 232, 232));
                        app.setStatusMessage("Convert to msm, RIGHT CLICK: set time resolution (currently " + ppq + " ppq)");
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (mei2msm.getBackground() != Color.GRAY)
                            mei2msm.setBackground(Color.LIGHT_GRAY);
                        app.setStatusMessage("");
                    }
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e))
                            mei2msm.setBackground(Color.GRAY);
                    }
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (mei2msm.contains(e.getPoint())) {
                                msm.clear();
                                for (Msm m : exportMsm(ppq)) {
                                    msm.add(new Msm4Gui(m, app));
                                }
                                mei2msm.setBackground(new Color(232, 232, 232));
                                app.doRepaint();
                            }
                            else
                                mei2msm.setBackground(Color.LIGHT_GRAY);
                        }
                    }
                });

                this.panel.add(meiName, "pushx, height 35px!, width 23%!");
                this.panel.add(saveMei, "pushx, height 35px!, width 4%!");
                this.panel.add(mei2msm, "pushx, height 35px!, width 4%!");
            }

            // msm and midi components
            for (Msm4Gui m : this.msm) {
                // the msm components
                if (m.isEmpty()) {
                    skip += 3;
                }
                else {
                    this.panel.add(m.getPanel()[0], "pushx, height 35px!, width 23%!, skip " + skip);
                    this.panel.add(m.getPanel()[1], "pushx, height 35px!, width 4%!");
                    this.panel.add(m.getPanel()[2], "pushx, height 35px!, width 4%!");
                    skip = 0;
                }

                // the midi components
                if ((m.midi == null) || m.midi.isEmpty()) {
                    skip = 6;       // skip the 3 midi cells and the 3 mei cells of the next line
                }
                else {
                    this.panel.add(m.midi.getPanel()[0], "pushx, height 35px!, width 23%!, skip " + skip);
                    this.panel.add(m.midi.getPanel()[1], "pushx, height 35px!, width 4%!");
                    this.panel.add(m.midi.getPanel()[2], "pushx, height 35px!, width 4%!");
                    skip = 3;       // all further msm and midi components that belong to this mei instance have 3 skips in the panel so that they are placed in the correct column
                }
            }

            return this.panel;
        }

        /**
         * an Msm extension for the windowed meico app
         */
        private class Msm4Gui extends Msm {
            private final JLabel[] panel;             // the actual gui extension
            private Midi4Gui midi;
            private MeiCoApp app;
            private boolean restsRemoved;
            private double bpm;

            public Msm4Gui(Msm msm, MeiCoApp app) {
                this.setFile(msm.getFile().getPath());
                this.setDocument(msm.getDocument());
                this.panel = new JLabel[3];         // the name label, save label and msm2midi label
                this.midi = null;
                this.app = app;
                this.restsRemoved = false;
                this.bpm = 120.0;
            }

            /**
             * constructor
             * @param file
             */
            public Msm4Gui(File file, MeiCoApp app) throws InvalidFileTypeException, IOException, ParsingException {
                this.midi = null;

                if (file.getName().substring(file.getName().length()-4).equals(".msm")) {       // if it is an msm file
                    this.readMsmFile(file, false);                                              // load it
                }
                else {                                                                          // otherwise try loading it as a midi file
                    this.midi = new Midi4Gui(file, app);
                }

                this.panel = new JLabel[3];     // the name label, save label and msm2midi label
                this.app = app;
                this.restsRemoved = false;
                this.bpm = 120.0;
            }

            /**
             * This method draws and returns the panel that the MeiCoApp displays.
             * @return
             */
            public JLabel[] getPanel() {
                if (this.isEmpty()) {                   // if no msm data loaded
                    this.panel[0] = new JLabel();       // return
                    this.panel[1] = new JLabel();       // empty
                    this.panel[2] = new JLabel();       // labels
                }
                else {
                    JPopupMenu msmNamePop = new JPopupMenu("MSM Processing");
                    msmNamePop.setEnabled(true);
                    JMenuItem removeRests = new JMenuItem(new AbstractAction("Remove Rests") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeRests();
                            restsRemoved = true;
                            this.setEnabled(false);
                        }
                    });
                    removeRests.setEnabled(!this.restsRemoved);
                    msmNamePop.add(removeRests);


                    JLabel msmName = new JLabel(this.getFile().getName());
                    msmName.setOpaque(true);
                    msmName.setBackground(Color.LIGHT_GRAY);
                    msmName.setForeground(Color.DARK_GRAY);
                    msmName.setBorder(new EmptyBorder(0, 4, 0, 0));
                    msmName.setFont(new Font("default", Font.PLAIN, 18));
                    //msmName.setToolTipText("<html>" + this.getFile().getPath() + "</html>");
                    msmName.setComponentPopupMenu(msmNamePop);
                    msmName.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            app.setStatusMessage(getFile().getPath() + ", RIGHT CLICK: msm processing options");
                        }
                        @Override
                        public void mouseExited(MouseEvent e) {
                            app.setStatusMessage("");
                        }
                    });

                    final JLabel saveMsm = new JLabel(new ImageIcon(getClass().getResource("/resources/save-gray.png")), JLabel.CENTER);
                    saveMsm.setOpaque(true);
                    saveMsm.setBackground(Color.LIGHT_GRAY);
                    saveMsm.setForeground(Color.DARK_GRAY);
                    //saveMsm.setToolTipText("<html>LEFT CLICK: quick save with default filename <br> RIGHT CLICK: open save dialog</html>");
                    saveMsm.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            saveMsm.setBackground(new Color(232, 232, 232));
                            app.setStatusMessage("LEFT CLICK: quick save with default filename, RIGHT CLICK: open save dialog");
                        }
                        @Override
                        public void mouseExited(MouseEvent e) {
                            if (saveMsm.getBackground() != Color.GRAY)
                                saveMsm.setBackground(Color.LIGHT_GRAY);
                            app.setStatusMessage("");
                        }
                        @Override
                        public void mousePressed(MouseEvent e) {
                            saveMsm.setBackground(Color.GRAY);
                        }
                        @Override
                        public void mouseReleased(MouseEvent e) {
                            if (saveMsm.contains(e.getPoint())) {
                                if (e.isPopupTrigger()) {                                                   // right click for file save dialog
                                    JFileChooser chooser = new JFileChooser();                              // open the fileopen dialog
                                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {      // file save has been approved
                                        writeMsm(chooser.getSelectedFile().getAbsolutePath());              // save it
                                    }
                                }
                                else {                                                                      // quick save with default filename with left mouse button
                                    writeMsm();
                                }
                                saveMsm.setBackground(new Color(232, 232, 232));
                            }
                            else
                                saveMsm.setBackground(Color.LIGHT_GRAY);
                        }
                    });

                    final JTextField bpmField = new JTextField(Double.toString(this.bpm), 10);
                    bpmField.setHorizontalAlignment(SwingConstants.RIGHT);
                    bpmField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusLost(FocusEvent e) {
                            String input = bpmField.getText().trim();
                            String clean = "";
                            boolean dec = false;
                            for (int i = 0; i < input.length(); ++i) {
                                if (Character.isDigit(input.charAt(i))) {
                                    clean = clean + input.charAt(i);
                                }
                                if (input.charAt(i) == '.' && !dec) {
                                    dec = true;
                                    clean = clean + input.charAt(i);
                                }
                            }
                            if (clean.isEmpty())
                                clean = "120.0";
                            bpm = Double.parseDouble(clean);
                            bpmField.setText(Double.toString(bpm));
                        }
                    });
                    bpmField.addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            bpmField.getParent().getParent().setVisible(false);
                        }
                    });

                    JLabel ppqLabel = new JLabel("bpm");
                    JPanel bpmPanel = new JPanel();

                    bpmPanel.add(bpmField);
                    bpmPanel.add(ppqLabel);
                    JLabel bpmSetup = new JLabel("Set Tempo");
                    JPopupMenu msm2midiPop = new JPopupMenu("Conversion Options");
                    msm2midiPop.setEnabled(true);
                    msm2midiPop.add(bpmSetup);
                    msm2midiPop.add(bpmPanel);

                    final JLabel msm2midi = new JLabel(new ImageIcon(getClass().getResource("/resources/convert-gray.png")), JLabel.CENTER);
                    msm2midi.setOpaque(true);
                    msm2midi.setBackground(Color.LIGHT_GRAY);
                    msm2midi.setForeground(Color.DARK_GRAY);
                    msm2midi.setComponentPopupMenu(msm2midiPop);
                    //msm2midi.setToolTipText("<html>convert to midi</html>");
                    msm2midi.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            msm2midi.setBackground(new Color(232, 232, 232));
                            app.setStatusMessage("Convert to midi, RIGHT CLICK: set the tempo (currently " + bpm + " bpm)");
                        }
                        @Override
                        public void mouseExited(MouseEvent e) {
                            if (msm2midi.getBackground() != Color.GRAY)
                                msm2midi.setBackground(Color.LIGHT_GRAY);
                            app.setStatusMessage("");
                        }
                        @Override
                        public void mousePressed(MouseEvent e) {
                            if (SwingUtilities.isLeftMouseButton(e))
                                msm2midi.setBackground(Color.GRAY);
                        }
                        @Override
                        public void mouseReleased(MouseEvent e) {
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                if (msm2midi.contains(e.getPoint())) {
                                    midi = new Midi4Gui(exportMidi(bpm), app);
                                    msm2midi.setBackground(new Color(232, 232, 232));
                                    app.doRepaint();
                                }
                                else
                                    msm2midi.setBackground(Color.LIGHT_GRAY);
                            }
                        }
                    });

                    this.panel[0] = msmName;
                    this.panel[1] = saveMsm;
                    this.panel[2] = msm2midi;

                }

                return this.panel;
            }

            /**
             * a Midi extension for the windowed meico app
             */
            private class Midi4Gui extends meico.midi.Midi {
                protected final JLabel[] panel;             // the actual gui extension
                private MeiCoApp app;

                /**
                 * constructor
                 * @param file
                 */
                public Midi4Gui(File file, MeiCoApp app) throws InvalidFileTypeException {
                    if (!file.getName().substring(file.getName().length()-4).equals(".mid")) {      // if it is not a midi file
                        throw new InvalidFileTypeException(file.getName() + " invalid file format!");
                    }

                    try {
                        this.readMidiFile(file);
                    } catch (InvalidMidiDataException e) {
                        throw new InvalidFileTypeException(file.getName() + " invalid midi file!");
                    } catch (IOException e) {
                        throw new InvalidFileTypeException(file.getName() + " invalid midi file!");
                    }

                    this.panel = new JLabel[3];     // the name label, save label and play label
                    this.app = app;
                }

                /**
                 * constructor
                 */
                public Midi4Gui(meico.midi.Midi midi, MeiCoApp app) {
                    super(midi.getSequence(), midi.getFile());
                    this.panel = new JLabel[3];
                    this.app = app;
                }

                /**
                 * This method draws and returns the panel that the MeiCoApp displays.
                 * @return
                 */
                public JLabel[] getPanel() {
                    if (this.isEmpty()) {                   // if no msm data loaded
                        this.panel[0] = new JLabel();       // return
                        this.panel[1] = new JLabel();       // empty
                        this.panel[2] = new JLabel();       // labels
                    }
                    else {
                        JLabel midiName = new JLabel(this.getFile().getName());
                        midiName.setOpaque(true);
                        midiName.setBackground(Color.LIGHT_GRAY);
                        midiName.setForeground(Color.DARK_GRAY);
                        midiName.setBorder(new EmptyBorder(0, 4, 0, 0));
                        midiName.setFont(new Font("default", Font.PLAIN, 18));
                        //midiName.setToolTipText("<html>" + this.getFile().getPath() + "</html>");
                        midiName.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                app.setStatusMessage(getFile().getPath());
                            }
                            @Override
                            public void mouseExited(MouseEvent e) {
                                app.setStatusMessage("");
                            }
                        });

                        final JLabel saveMidi = new JLabel(new ImageIcon(getClass().getResource("/resources/save-gray.png")), JLabel.CENTER);
                        saveMidi.setOpaque(true);
                        saveMidi.setBackground(Color.LIGHT_GRAY);
                        saveMidi.setForeground(Color.DARK_GRAY);
                        //saveMidi.setToolTipText("<html>LEFT CLICK: quick save with default filename <br> RIGHT CLICK: open save dialog</html>");
                        saveMidi.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                saveMidi.setBackground(new Color(232, 232, 232));
                                app.setStatusMessage("LEFT CLICK: quick save with default filename, RIGHT CLICK: open save dialog");
                            }
                            @Override
                            public void mouseExited(MouseEvent e) {
                                if (saveMidi.getBackground() != Color.GRAY)
                                    saveMidi.setBackground(Color.LIGHT_GRAY);
                                app.setStatusMessage("");
                            }
                            @Override
                            public void mousePressed(MouseEvent e) {
                                saveMidi.setBackground(Color.GRAY);
                            }
                            @Override
                            public void mouseReleased(MouseEvent e) {
                                if (saveMidi.contains(e.getPoint())) {
                                    if (SwingUtilities.isLeftMouseButton(e)) {                                  // quick save with default filename with left mouse button
                                        try {
                                            writeMidi();
                                        } catch (IOException err) {
                                            app.setStatusMessage(err.toString());
                                        }
                                    }
                                    else {                                                                      // svae dialog with right mouse button
                                        JFileChooser chooser = new JFileChooser();                              // open the fileopen dialog
                                        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {      // file save has been approved
                                            try {
                                                writeMidi(chooser.getSelectedFile());                               // save it
                                            } catch (IOException err) {
                                                app.setStatusMessage(err.toString());
                                            }
                                        }
                                    }
                                    saveMidi.setBackground(new Color(232, 232, 232));
                                }
                                else
                                    saveMidi.setBackground(Color.LIGHT_GRAY);
                            }
                        });

                        final JLabel playMidi = new JLabel("\u25BA");
                        playMidi.setFont(new Font("default", Font.PLAIN, 18));
                        playMidi.setHorizontalAlignment(JLabel.CENTER);
                        playMidi.setOpaque(true);
                        playMidi.setBackground(Color.LIGHT_GRAY);
                        playMidi.setForeground(Color.DARK_GRAY);
                        //playMidi.setToolTipText("<html>play midi file</html>");
                        playMidi.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                playMidi.setBackground(new Color(232, 232, 232));
                                app.setStatusMessage("Play midi file");
                            }
                            @Override
                            public void mouseExited(MouseEvent e) {
                                if (playMidi.getBackground() != Color.GRAY)
                                    playMidi.setBackground(Color.LIGHT_GRAY);
                                app.setStatusMessage("");
                            }
                            @Override
                            public void mousePressed(MouseEvent e) {
                                playMidi.setBackground(Color.GRAY);
                            }
                            @Override
                            public void mouseReleased(MouseEvent e) {
                                if (playMidi.contains(e.getPoint())) {
                                    if (playMidi.getText().equals("\u25A0")) {
                                        app.stopPlayback();
                                        playMidi.setBackground(new Color(232, 232, 232));
                                        return;
                                    }
                                    app.stopPlayback();
                                    try {
                                        app.sequencer.setSequence(getSequence());
                                        app.sequencer.start();
                                    } catch (InvalidMidiDataException err) {
                                        app.setStatusMessage(err.toString());
                                        playMidi.setBackground(new Color(232, 232, 232));
                                        return;
                                    } catch (NullPointerException err) {
                                        app.setStatusMessage(err.toString());
                                        playMidi.setBackground(new Color(232, 232, 232));
                                        return;
                                    } catch (IllegalStateException err) {
                                        app.setStatusMessage(err.toString());
                                        playMidi.setBackground(new Color(232, 232, 232));
                                        return;
                                    }
                                    playMidi.setText("\u25A0");
                                    playMidi.setBackground(new Color(232, 232, 232));
                                }
                                else
                                    playMidi.setBackground(Color.LIGHT_GRAY);
                            }
                        });

                        this.panel[0] = midiName;
                        this.panel[1] = saveMidi;
                        this.panel[2] = playMidi;
                    }

                    return this.panel;
                }
            }
        }
    }
}