package rars.tools;

import org.jetbrains.annotations.NotNull;
import rars.assembler.DataTypes;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.util.BinaryUtilsKt;
import rars.venus.VenusUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Random;

/*
Copyright (c) 2003-2011,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * A data cache simulator. It can be run either as a stand-alone Java
 * application having
 * access to the com.chrisps.rars package, or through RARS as an item in its
 * Tools menu. It makes
 * maximum use of methods inherited from its abstract superclass
 * AbstractToolAndApplication.
 * Pete Sanderson, v 1.0: 16-18 October 2006, v 1.1: 7 November 2006. v 1.2: 23
 * December 2010.
 * <p>
 * Version 1.2 fixes a bug in the hit/miss animator under full or N-way set
 * associative. It was
 * animating the block of initial access (first block of set). Now it animates
 * the block
 * of final access (where address found or stored). Also added log display to
 * GUI (previously System.out).
 * </p>
 */
public final class CacheSimulator extends AbstractTool {
    private static final String version = "Version 1.2";
    private static final String heading = "Simulate and illustrate data cache performance";
    private static final String[] cacheBlockSizeChoices = {
        "1", "2", "4", "8", "16", "32", "64", "128", "256", "512",
        "1024", "2048"
    };
    private static final String[] cacheBlockCountChoices = {
        "1", "2", "4", "8", "16", "32", "64", "128", "256", "512",
        "1024", "2048"
    };
    private static final String[] placementPolicyChoices = {
        "Direct Mapping", "Fully Associative",
        "N-way Set Associative"
    };
    private static final int DIRECT = 0, FULL = 1, SET = 2; // NOTE: these have to match placementPolicyChoices order!
    private static final String[] replacementPolicyChoices = {"LRU", "Random"};
    private static final int LRU = 0, RANDOM = 1; // NOTE: these have to match replacementPolicyChoices order!
    private static final int defaultCacheBlockSizeIndex = 2;
    private static final int defaultCacheBlockCountIndex = 3;
    private static final int defaultPlacementPolicyIndex = CacheSimulator.DIRECT;
    private static final int defaultReplacementPolicyIndex = CacheSimulator.LRU;
    private static final int defaultCacheSetSizeIndex = 0;
    private static boolean debug = false; // controls display of debugging info
    // Some GUI settings
    private final EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
    private final Font countFonts = new Font("Times", Font.BOLD, 12);
    private final Color backgroundColor = Color.WHITE;
    // RNG used for random replacement policy. For testing, set seed for
    // reproducible stream
    private final Random randu = new Random(0);
    // Major GUI components
    private JComboBox<String> cacheBlockSizeSelector, cacheBlockCountSelector,
        cachePlacementSelector, cacheReplacementSelector,
        cacheSetSizeSelector;
    private JTextField memoryAccessCountDisplay, cacheHitCountDisplay, cacheMissCountDisplay,
        cacheSizeDisplay;
    private JProgressBar cacheHitRateDisplay;
    private Animation animations;
    private JPanel logPanel;
    private JTextArea logText;
    // Values for Combo Boxes
    private int[] cacheBlockSizeChoicesInt, cacheBlockCountChoicesInt;
    private String[] cacheSetSizeChoices; // will change dynamically based on the other selections
    // Cache-related data structures
    private AbstractCache theCache;
    private int memoryAccessCount, cacheHitCount, cacheMissCount;
    private double cacheHitRate;

    /**
     * Simple constructor, likely used by the RARS Tools menu mechanism
     */
    public CacheSimulator(final @NotNull VenusUI mainUI) {
        super("Data Cache Simulation Tool, " + CacheSimulator.version, CacheSimulator.heading, mainUI);
    }

    // Will determine range of choices for "set size in blocks", which is determined
    // both by
    // the number of blocks in the cache and by placement policy.
    private static String[] determineSetSizeChoices(final int cacheBlockCountIndex, final int placementPolicyIndex) {
        final String[] choices;
        final int firstBlockCountIndex = 0;
        switch (placementPolicyIndex) {
            case CacheSimulator.DIRECT:
                choices = new String[1];
                choices[0] = CacheSimulator.cacheBlockCountChoices[firstBlockCountIndex]; // set size fixed at 1
                break;
            case CacheSimulator.SET:
                choices = new String[cacheBlockCountIndex - firstBlockCountIndex + 1];
                System.arraycopy(
                    CacheSimulator.cacheBlockCountChoices, firstBlockCountIndex, choices, 0,
                    choices.length
                );
                break;
            case CacheSimulator.FULL: // 1 set total, so set size fixed at current number of blocks
            default:
                choices = new String[1];
                choices[0] = CacheSimulator.cacheBlockCountChoices[cacheBlockCountIndex];
        }
        return choices;
    }

    private static JPanel getPanelWithBorderLayout() {
        return new JPanel(new BorderLayout(2, 2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Data Cache Simulator";
    }

    /**
     * Method that constructs the main cache simulator display area. It is organized
     * vertically
     * into three major components: the cache configuration which an be modified
     * using combo boxes, the cache performance which is updated as the
     * attached program executes, and the runtime log which is optionally used
     * to display log of each cache access.
     *
     * @return the GUI component containing these three areas
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        // OVERALL STRUCTURE OF MAIN UI (CENTER)
        final Box results = Box.createVerticalBox();
        results.add(this.buildOrganizationArea());
        results.add(this.buildPerformanceArea());
        results.add(this.buildLogArea());
        return results;
    }

    private JComponent buildLogArea() {
        this.logPanel = new JPanel();
        final TitledBorder ltb = new TitledBorder("Runtime Log");
        ltb.setTitleJustification(TitledBorder.CENTER);
        this.logPanel.setBorder(ltb);
        final JCheckBox logShow = new JCheckBox("Enabled", CacheSimulator.debug);
        logShow.addItemListener(
            e -> {
                CacheSimulator.debug = e.getStateChange() == ItemEvent.SELECTED;
                this.resetLogDisplay();
                this.logText.setEnabled(CacheSimulator.debug);
                this.logText.setBackground(CacheSimulator.debug ? Color.WHITE : this.logPanel.getBackground());
            });
        this.logPanel.add(logShow);
        this.logText = new JTextArea(5, 70);
        this.logText.setEnabled(CacheSimulator.debug);
        this.logText.setBackground(CacheSimulator.debug ? Color.WHITE : this.logPanel.getBackground());
        this.logText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        this.logText.setToolTipText("Displays cache activity log if enabled");
        final JScrollPane logScroll = new JScrollPane(
            this.logText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        this.logPanel.add(logScroll);
        return this.logPanel;
    }

    // Rest of the protected methods. These override do-nothing methods inherited

    /// /////////////////////////////////////////////////////////////////////////////////// from
    // the abstract superclass.
    private JComponent buildOrganizationArea() {
        final JPanel organization = new JPanel(new GridLayout(3, 2));
        final TitledBorder otb = new TitledBorder("Cache Organization");
        otb.setTitleJustification(TitledBorder.CENTER);
        organization.setBorder(otb);
        this.cachePlacementSelector = new JComboBox<>(CacheSimulator.placementPolicyChoices);
        this.cachePlacementSelector.setEditable(false);
        this.cachePlacementSelector.setBackground(this.backgroundColor);
        this.cachePlacementSelector.setSelectedIndex(CacheSimulator.defaultPlacementPolicyIndex);
        this.cachePlacementSelector.addActionListener(
            e -> {
                this.updateCacheSetSizeSelector();
                this.reset();
            });

        this.cacheReplacementSelector = new JComboBox<>(CacheSimulator.replacementPolicyChoices);
        this.cacheReplacementSelector.setEditable(false);
        this.cacheReplacementSelector.setBackground(this.backgroundColor);
        this.cacheReplacementSelector.setSelectedIndex(CacheSimulator.defaultReplacementPolicyIndex);

        this.cacheBlockSizeSelector = new JComboBox<>(CacheSimulator.cacheBlockSizeChoices);
        this.cacheBlockSizeSelector.setEditable(false);
        this.cacheBlockSizeSelector.setBackground(this.backgroundColor);
        this.cacheBlockSizeSelector.setSelectedIndex(CacheSimulator.defaultCacheBlockSizeIndex);
        this.cacheBlockSizeSelector.addActionListener(
            e -> {
                this.updateCacheSizeDisplay();
                this.reset();
            });
        this.cacheBlockCountSelector = new JComboBox<>(CacheSimulator.cacheBlockCountChoices);
        this.cacheBlockCountSelector.setEditable(false);
        this.cacheBlockCountSelector.setBackground(this.backgroundColor);
        this.cacheBlockCountSelector.setSelectedIndex(CacheSimulator.defaultCacheBlockCountIndex);
        this.cacheBlockCountSelector.addActionListener(
            e -> {
                this.updateCacheSetSizeSelector();
                this.theCache = this.createNewCache();
                this.resetCounts();
                this.updateDisplay();
                this.updateCacheSizeDisplay();
                this.animations.fillAnimationBoxWithCacheBlocks();
            });

        this.cacheSetSizeSelector = new JComboBox<>(this.cacheSetSizeChoices);
        this.cacheSetSizeSelector.setEditable(false);
        this.cacheSetSizeSelector.setBackground(this.backgroundColor);
        this.cacheSetSizeSelector.setSelectedIndex(CacheSimulator.defaultCacheSetSizeIndex);
        this.cacheSetSizeSelector.addActionListener(
            e -> this.reset());

        // ALL COMPONENTS FOR "CACHE ORGANIZATION" SECTION
        final JPanel placementPolicyRow = CacheSimulator.getPanelWithBorderLayout();
        placementPolicyRow.setBorder(this.emptyBorder);
        placementPolicyRow.add(new JLabel("Placement Policy "), BorderLayout.WEST);
        placementPolicyRow.add(this.cachePlacementSelector, BorderLayout.EAST);

        final JPanel replacementPolicyRow = CacheSimulator.getPanelWithBorderLayout();
        replacementPolicyRow.setBorder(this.emptyBorder);
        replacementPolicyRow.add(new JLabel("Block Replacement Policy "), BorderLayout.WEST);
        /*
         * replacementPolicyDisplay = new JTextField("N/A",6);
         * replacementPolicyDisplay.setEditable(false);
         * replacementPolicyDisplay.setBackground(backgroundColor);
         * replacementPolicyDisplay.setHorizontalAlignment(JTextField.RIGHT);
         */
        replacementPolicyRow.add(this.cacheReplacementSelector, BorderLayout.EAST);

        final JPanel cacheSetSizeRow = CacheSimulator.getPanelWithBorderLayout();
        cacheSetSizeRow.setBorder(this.emptyBorder);
        cacheSetSizeRow.add(new JLabel("Set size (blocks) "), BorderLayout.WEST);
        cacheSetSizeRow.add(this.cacheSetSizeSelector, BorderLayout.EAST);

        // Cachable address range "selection" removed for now...
        /*
         * JPanel cachableAddressesRow = getPanelWithBorderLayout();
         * cachableAddressesRow.setBorder(emptyBorder);
         * cachableAddressesRow.add(new
         * JLabel("Cachable Addresses "),BorderLayout.WEST);
         * cachableAddressesDisplay = new JTextField("all data segment");
         * cachableAddressesDisplay.setEditable(false);
         * cachableAddressesDisplay.setBackground(backgroundColor);
         * cachableAddressesDisplay.setHorizontalAlignment(JTextField.RIGHT);
         * cachableAddressesRow.add(cachableAddressesDisplay, BorderLayout.EAST);
         */
        final JPanel cacheNumberBlocksRow = CacheSimulator.getPanelWithBorderLayout();
        cacheNumberBlocksRow.setBorder(this.emptyBorder);
        cacheNumberBlocksRow.add(new JLabel("Number of blocks "), BorderLayout.WEST);
        cacheNumberBlocksRow.add(this.cacheBlockCountSelector, BorderLayout.EAST);

        final JPanel cacheBlockSizeRow = CacheSimulator.getPanelWithBorderLayout();
        cacheBlockSizeRow.setBorder(this.emptyBorder);
        cacheBlockSizeRow.add(new JLabel("Cache block size (words) "), BorderLayout.WEST);
        cacheBlockSizeRow.add(this.cacheBlockSizeSelector, BorderLayout.EAST);

        final JPanel cacheTotalSizeRow = CacheSimulator.getPanelWithBorderLayout();
        cacheTotalSizeRow.setBorder(this.emptyBorder);
        cacheTotalSizeRow.add(new JLabel("Cache size (bytes) "), BorderLayout.WEST);
        this.cacheSizeDisplay = new JTextField(8);
        this.cacheSizeDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.cacheSizeDisplay.setEditable(false);
        this.cacheSizeDisplay.setBackground(this.backgroundColor);
        this.cacheSizeDisplay.setFont(this.countFonts);
        cacheTotalSizeRow.add(this.cacheSizeDisplay, BorderLayout.EAST);
        this.updateCacheSizeDisplay();

        // Lay 'em out in the grid...
        organization.add(placementPolicyRow);
        organization.add(cacheNumberBlocksRow);
        organization.add(replacementPolicyRow);
        organization.add(cacheBlockSizeRow);
        // organization.add(cachableAddressesRow);
        organization.add(cacheSetSizeRow);
        organization.add(cacheTotalSizeRow);
        return organization;
    }

    private JComponent buildPerformanceArea() {
        final JPanel performance = new JPanel(new GridLayout(1, 2));
        final TitledBorder ptb = new TitledBorder("Cache Performance");
        ptb.setTitleJustification(TitledBorder.CENTER);
        performance.setBorder(ptb);
        final JPanel memoryAccessCountRow = CacheSimulator.getPanelWithBorderLayout();
        memoryAccessCountRow.setBorder(this.emptyBorder);
        memoryAccessCountRow.add(new JLabel("Memory Access Count "), BorderLayout.WEST);
        this.memoryAccessCountDisplay = new JTextField(10);
        this.memoryAccessCountDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.memoryAccessCountDisplay.setEditable(false);
        this.memoryAccessCountDisplay.setBackground(this.backgroundColor);
        this.memoryAccessCountDisplay.setFont(this.countFonts);
        memoryAccessCountRow.add(this.memoryAccessCountDisplay, BorderLayout.EAST);

        final JPanel cacheHitCountRow = CacheSimulator.getPanelWithBorderLayout();
        cacheHitCountRow.setBorder(this.emptyBorder);
        cacheHitCountRow.add(new JLabel("Cache Hit Count "), BorderLayout.WEST);
        this.cacheHitCountDisplay = new JTextField(10);
        this.cacheHitCountDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.cacheHitCountDisplay.setEditable(false);
        this.cacheHitCountDisplay.setBackground(this.backgroundColor);
        this.cacheHitCountDisplay.setFont(this.countFonts);
        cacheHitCountRow.add(this.cacheHitCountDisplay, BorderLayout.EAST);

        final JPanel cacheMissCountRow = CacheSimulator.getPanelWithBorderLayout();
        cacheMissCountRow.setBorder(this.emptyBorder);
        cacheMissCountRow.add(new JLabel("Cache Miss Count "), BorderLayout.WEST);
        this.cacheMissCountDisplay = new JTextField(10);
        this.cacheMissCountDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.cacheMissCountDisplay.setEditable(false);
        this.cacheMissCountDisplay.setBackground(this.backgroundColor);
        this.cacheMissCountDisplay.setFont(this.countFonts);
        cacheMissCountRow.add(this.cacheMissCountDisplay, BorderLayout.EAST);

        final JPanel cacheHitRateRow = CacheSimulator.getPanelWithBorderLayout();
        cacheHitRateRow.setBorder(this.emptyBorder);
        cacheHitRateRow.add(new JLabel("Cache Hit Rate "), BorderLayout.WEST);
        this.cacheHitRateDisplay = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        this.cacheHitRateDisplay.setStringPainted(true);
        this.cacheHitRateDisplay.setForeground(Color.BLUE);
        this.cacheHitRateDisplay.setBackground(this.backgroundColor);
        this.cacheHitRateDisplay.setFont(this.countFonts);
        cacheHitRateRow.add(this.cacheHitRateDisplay, BorderLayout.EAST);

        this.resetCounts();
        this.updateDisplay();

        // Vertically align these 4 measures in a grid, then add to left column of main
        // grid.
        final JPanel performanceMeasures = new JPanel(new GridLayout(4, 1));
        performanceMeasures.add(memoryAccessCountRow);
        performanceMeasures.add(cacheHitCountRow);
        performanceMeasures.add(cacheMissCountRow);
        performanceMeasures.add(cacheHitRateRow);
        performance.add(performanceMeasures);

        // LET'S TRY SOME ANIMATION ON THE RIGHT SIDE...
        this.animations = new Animation();
        this.animations.fillAnimationBoxWithCacheBlocks();
        final JPanel animationsPanel = new JPanel(new GridLayout(1, 2));
        final Box animationsLabel = Box.createVerticalBox();
        final JPanel tableTitle1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JPanel tableTitle2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableTitle1.add(new JLabel("Cache Block Table"));
        tableTitle2.add(new JLabel("(block 0 at top)"));
        animationsLabel.add(tableTitle1);
        animationsLabel.add(tableTitle2);
        final Dimension colorKeyBoxSize = new Dimension(8, 8);

        final JPanel emptyKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JPanel emptyBox = new JPanel();
        emptyBox.setSize(colorKeyBoxSize);
        emptyBox.setBackground(this.animations.defaultColor);
        emptyBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        emptyKey.add(emptyBox);
        emptyKey.add(new JLabel(" = empty"));

        final JPanel missBox = new JPanel();
        final JPanel missKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
        missBox.setSize(colorKeyBoxSize);
        missBox.setBackground(this.animations.missColor);
        missBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        missKey.add(missBox);
        missKey.add(new JLabel(" = miss"));

        final JPanel hitKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JPanel hitBox = new JPanel();
        hitBox.setSize(colorKeyBoxSize);
        hitBox.setBackground(this.animations.hitColor);
        hitBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        hitKey.add(hitBox);
        hitKey.add(new JLabel(" = hit"));

        animationsLabel.add(emptyKey);
        animationsLabel.add(hitKey);
        animationsLabel.add(missKey);
        animationsLabel.add(Box.createVerticalGlue());
        animationsPanel.add(animationsLabel);
        animationsPanel.add(this.animations.getAnimationBox());

        performance.add(animationsPanel);
        return performance;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Apply caching policies and update display when connected program accesses
     * (data) memory.
     */
    @Override
    protected void processRISCVUpdate(final AccessNotice accessNotice) {
        final MemoryAccessNotice notice = (MemoryAccessNotice) accessNotice;
        this.memoryAccessCount++;
        final CacheAccessResult cacheAccessResult = this.theCache.isItAHitThenReadOnMiss(notice.address);
        if (cacheAccessResult.isHit()) {
            this.cacheHitCount++;
            this.animations.showHit(cacheAccessResult.getBlock());
        } else {
            this.cacheMissCount++;
            this.animations.showMiss(cacheAccessResult.getBlock());
        }
        this.cacheHitRate = this.cacheHitCount / (double) this.memoryAccessCount;
    }

    /**
     * Initialize all JComboBox choice structures not already initialized at
     * declaration.
     * Also creates initial default cache object. Overrides inherited method that
     * does nothing.
     */
    @Override
    protected void initializePreGUI() {
        this.cacheBlockSizeChoicesInt = new int[CacheSimulator.cacheBlockSizeChoices.length];
        for (int i = 0; i < CacheSimulator.cacheBlockSizeChoices.length; i++) {
            try {
                this.cacheBlockSizeChoicesInt[i] = Integer.parseInt(CacheSimulator.cacheBlockSizeChoices[i]);
            } catch (final NumberFormatException nfe) {
                this.cacheBlockSizeChoicesInt[i] = 1;
            }
        }
        this.cacheBlockCountChoicesInt = new int[CacheSimulator.cacheBlockCountChoices.length];
        for (int i = 0; i < CacheSimulator.cacheBlockCountChoices.length; i++) {
            try {
                this.cacheBlockCountChoicesInt[i] = Integer.parseInt(CacheSimulator.cacheBlockCountChoices[i]);
            } catch (final NumberFormatException nfe) {
                this.cacheBlockCountChoicesInt[i] = 1;
            }
        }
        this.cacheSetSizeChoices = CacheSimulator.determineSetSizeChoices(
            CacheSimulator.defaultCacheBlockCountIndex,
            CacheSimulator.defaultPlacementPolicyIndex
        );
    }

    /**
     * The only post-GUI initialization is to create the initial cache object based
     * on the default settings
     * of the various combo boxes. Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePostGUI() {
        this.theCache = this.createNewCache();
    }

    // Private methods defined to support the above.

    /**
     * Method to reset cache, counters and display when the Reset button selected.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void reset() {
        this.theCache = this.createNewCache();
        this.resetCounts();
        this.updateDisplay();
        this.animations.reset();
        this.resetLogDisplay();
    }

    /**
     * Updates display immediately after each update (AccessNotice) is processed,
     * after
     * cache configuration changes as needed, and after each execution step when
     * Rars
     * is running in timed mode. Overrides inherited method that does nothing.
     */
    @Override
    protected void updateDisplay() {
        this.updateMemoryAccessCountDisplay();
        this.updateCacheHitCountDisplay();
        this.updateCacheMissCountDisplay();
        this.updateCacheHitRateDisplay();
    }

    // Update the Set Size combo box selection in response to other selections..
    private void updateCacheSetSizeSelector() {
        this.cacheSetSizeSelector.setModel(
            new DefaultComboBoxModel<>(CacheSimulator.determineSetSizeChoices(
                this.cacheBlockCountSelector.getSelectedIndex(),
                this.cachePlacementSelector.getSelectedIndex()
            )));
    }

    // create and return a new cache object based on current specs
    private AbstractCache createNewCache() {
        int setSize = 1;
        try {
            setSize = Integer.parseInt((String) this.cacheSetSizeSelector.getSelectedItem());
        } catch (final
        NumberFormatException nfe) { // if this happens its my fault!
        }
        final AbstractCache theNewCache = new AnyCache(
            this.cacheBlockCountChoicesInt[this.cacheBlockCountSelector.getSelectedIndex()],
            this.cacheBlockSizeChoicesInt[this.cacheBlockSizeSelector.getSelectedIndex()],
            setSize
        );
        return theNewCache;
    }

    private void resetCounts() {
        this.memoryAccessCount = 0;
        this.cacheHitCount = 0;
        this.cacheMissCount = 0;
        this.cacheHitRate = 0.0;
    }

    private void updateMemoryAccessCountDisplay() {
        this.memoryAccessCountDisplay.setText(Integer.toString(this.memoryAccessCount));
    }

    private void updateCacheHitCountDisplay() {
        this.cacheHitCountDisplay.setText(Integer.toString(this.cacheHitCount));
    }

    private void updateCacheMissCountDisplay() {
        this.cacheMissCountDisplay.setText(Integer.toString(this.cacheMissCount));
    }

    private void updateCacheHitRateDisplay() {
        this.cacheHitRateDisplay.setValue((int) Math.round(this.cacheHitRate * 100));
    }

    private void updateCacheSizeDisplay() {
        final int cacheSize = this.cacheBlockSizeChoicesInt[this.cacheBlockSizeSelector.getSelectedIndex()] *
            this.cacheBlockCountChoicesInt[this.cacheBlockCountSelector.getSelectedIndex()] *
            DataTypes.WORD_SIZE;
        this.cacheSizeDisplay.setText(Integer.toString(cacheSize));
    }

    private void resetLogDisplay() {
        this.logText.setText("");
    }

    private void writeLog(final String text) {
        this.logText.append(text);
        this.logText.setCaretPosition(this.logText.getDocument().getLength());
    }

    // Specialized inner classes for cache modeling and animation.

    // Represents a block in the cache. Since we are only simulating
    // cache performance, there's no need to actually store memory contents.
    private static class CacheBlock {
        private boolean valid;
        private int tag;
        private int mostRecentAccessTime;

        public CacheBlock(final int sizeInWords) {
            this.valid = false;
            this.tag = 0;
            this.mostRecentAccessTime = -1;
        }
    }

    // Represents the outcome of a cache access. There are two parts:
    // whether it was a hit or not, and in which block is the value stored.
    // In the case of a hit, the block associated with address. In the case of
    // a miss, the block where new association is made. DPS 23-Dec-2010
    private record CacheAccessResult(boolean hitOrMiss, int blockNumber) {

        public boolean isHit() {
            return this.hitOrMiss;
        }

        public int getBlock() {
            return this.blockNumber;
        }
    }

    // Abstract Cache class. Subclasses will implement specific policies.
    private abstract static class AbstractCache {
        protected final CacheBlock[] blocks;
        private final int numberOfBlocks, blockSizeInWords, setSizeInBlocks, numberOfSets;

        protected AbstractCache(final int numberOfBlocks, final int blockSizeInWords, final int setSizeInBlocks) {
            this.numberOfBlocks = numberOfBlocks;
            this.blockSizeInWords = blockSizeInWords;
            this.setSizeInBlocks = setSizeInBlocks;
            this.numberOfSets = numberOfBlocks / setSizeInBlocks;
            this.blocks = new CacheBlock[numberOfBlocks];
            this.reset();
        }

        public int getNumberOfBlocks() {
            return this.numberOfBlocks;
        }

        public int getNumberOfSets() {
            return this.numberOfSets;
        }

        public int getSetSizeInBlocks() {
            return this.setSizeInBlocks;
        }

        public int getBlockSizeInWords() {
            return this.blockSizeInWords;
        }

        public int getCacheSizeInWords() {
            return this.numberOfBlocks * this.blockSizeInWords;
        }

        public int getCacheSizeInBytes() {
            return this.numberOfBlocks * this.blockSizeInWords * DataTypes.WORD_SIZE;
        }

        // This will work regardless of placement.
        // For direct map, #sets==#blocks
        // For full assoc, #sets==1 so anything % #sets == 0
        // For n-way assoc, it extracts the set bits in address.
        public int getSetNumber(final int address) {
            return address / DataTypes.WORD_SIZE / this.blockSizeInWords % this.numberOfSets;
        }

        // This will work regardless of placement policy (direct map, n-way or full
        // assoc)
        public int getTag(final int address) {
            return address / DataTypes.WORD_SIZE / this.blockSizeInWords / this.numberOfSets;
        }

        // This will work regardless of placement policy (direct map, n-way or full
        // assoc)
        // Returns absolute block offset into the cache.
        public int getFirstBlockToSearch(final int address) {
            return this.getSetNumber(address) * this.setSizeInBlocks;
        }

        // This will work regardless of placement policy (direct map, n-way or full
        // assoc)
        // Returns absolute block offset into the cache.
        public int getLastBlockToSearch(final int address) {
            return this.getFirstBlockToSearch(address) + this.setSizeInBlocks - 1;
        }

        /* Reset the cache contents. */
        public void reset() {
            for (int i = 0; i < this.numberOfBlocks; i++) {
                this.blocks[i] = new CacheBlock(this.blockSizeInWords);
            }
            System.gc(); // scoop 'em up now
        }

        // Subclass must implement this according to its policies
        public abstract CacheAccessResult isItAHitThenReadOnMiss(int address);
    }

    // Implements any of the well-known cache organizations. Physical memory
    // address is partitioned depending on organization:
    // Direct Mapping: [ tag | block | word | byte ]
    // Fully Associative: [ tag | word | byte ]
    // Set Associative: [ tag | set | word | byte ]
    // Bit lengths of each part are determined as follows:
    // Direct Mapping:
    // byte = log2 of #bytes in a word (typically 4)
    // word = log2 of #words in a block
    // block = log2 of #blocks in the cache
    // tag = #bytes in address - (byte+word+block)
    // Fully Associative:
    // byte = log2 of #bytes in a word (typically 4)
    // word = log2 of #words in a block
    // tag = #bytes in address - (byte+word)
    // Set Associative:
    // byte = log2 of #bytes in a word (typically 4)
    // word = log2 of #words in a block
    // set = log2 of #sets in the cache
    // tag = #bytes in address - (byte+word+set)
    // Direct Mapping (1 way set associative):
    // The block value for a given address identifies its block index into the
    ////////////////////////////////////////////////////////////////////////////// cache.
    // That's why its called "direct mapped." This is the only cache block it can
    // occupy. If that cache block is empty or if it is occupied by a different tag,
    // this is a MISS. If that cache block is occupied by the same tag, this is a
    ////////////////////////////////////////////////////////////////////////////// HIT.
    // There is no replacement policy: upon a cache miss of an occupied block, the
    ////////////////////////////////////////////////////////////////////////////// old
    // block is written out (unless write-through) and the new one read in.
    // Those actions are not simulated here.
    // Fully Associative:
    // There is one set, and very tag has to be searched before determining hit or
    ////////////////////////////////////////////////////////////////////////////// miss.
    // If tag is matched, it is a hit. If tag is not matched and there is at least
    ////////////////////////////////////////////////////////////////////////////// one
    // empty block, it is a miss and the new tag will occupy it. If tag is not
    ////////////////////////////////////////////////////////////////////////////// matched
    // and every block is occupied, it is a miss and one of the occupied blocks will
    ////////////////////////////////////////////////////////////////////////////// be
    // selected for removal and the new tag will replace it.
    // n-way Set Associative:
    // Each set consists of n blocks, and the number of sets in the cache is total
    ////////////////////////////////////////////////////////////////////////////// number
    // of blocks divided by n. The set bits in the address will identify which set
    ////////////////////////////////////////////////////////////////////////////// to
    // search, and every tag in that set has to be searched before determining hit
    ////////////////////////////////////////////////////////////////////////////// or
    ////////////////////////////////////////////////////////////////////////////// miss.
    // If tag is matched, it is a hit. If tag is not matched and there is at least
    ////////////////////////////////////////////////////////////////////////////// one
    // empty block, it is a miss and the new tag will occupy it. If tag is not
    ////////////////////////////////////////////////////////////////////////////// matched
    // and every block is occupied, it is a miss and one of the occupied blocks will

    /// /////////////////////////////////////////////////////////////////////////// be
    // selected for removal and the new tag will replace it.
    private class AnyCache extends AbstractCache {
        public AnyCache(final int numberOfBlocks, final int blockSizeInWords, final int setSizeInBlocks) {
            super(numberOfBlocks, blockSizeInWords, setSizeInBlocks);
        }

        // This method works for any of the placement policies:
        // direct mapped, full associative or n-way set associative.
        @Override
        public CacheAccessResult isItAHitThenReadOnMiss(final int address) {
            final int SET_FULL = 0;
            final int firstBlock = this.getFirstBlockToSearch(address);
            final int lastBlock = this.getLastBlockToSearch(address);
            if (CacheSimulator.debug) // System.out.print
            {
                CacheSimulator.this.writeLog("(" + CacheSimulator.this.memoryAccessCount + ") address: " + BinaryUtilsKt.intToHexStringWithPrefix(
                    address) + " (tag "
                    + BinaryUtilsKt.intToHexStringWithPrefix(this.getTag(address)) + ") " + " block range: " + firstBlock + "-"
                    + lastBlock + "\n");
            }
            CacheBlock block;
            int blockNumber;
            // Will do a sequential instead of associative search!
            final int HIT = 1;
            int result = SET_FULL;
            for (blockNumber = firstBlock; blockNumber <= lastBlock; blockNumber++) {
                block = this.blocks[blockNumber];
                if (CacheSimulator.debug) // System.out.print
                {
                    CacheSimulator.this.writeLog("   trying block " + blockNumber
                        + ((block.valid) ? " tag " + BinaryUtilsKt.intToHexStringWithPrefix(block.tag) : " empty"));
                }
                if (block.valid && block.tag == this.getTag(address)) {// it's a hit!
                    if (CacheSimulator.debug) // System.out.print
                    {
                        CacheSimulator.this.writeLog(" -- HIT\n");
                    }
                    result = HIT;
                    block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
                    break;
                }
                if (!block.valid) {// it's a miss but I got it now because it is empty!
                    if (CacheSimulator.debug) // System.out.print
                    {
                        CacheSimulator.this.writeLog(" -- MISS\n");
                    }
                    result = 2; // miss value
                    block.valid = true;
                    block.tag = this.getTag(address);
                    block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
                    break;
                }
                if (CacheSimulator.debug) // System.out.print
                {
                    CacheSimulator.this.writeLog(" -- OCCUPIED\n");
                }
            }
            if (result == SET_FULL) {
                // select one to replace and replace it...
                if (CacheSimulator.debug) // System.out.print
                {
                    CacheSimulator.this.writeLog("   MISS due to FULL SET");
                }
                final int blockToReplace = this.selectBlockToReplace(firstBlock, lastBlock);
                block = this.blocks[blockToReplace];
                block.tag = this.getTag(address);
                block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
                blockNumber = blockToReplace;
            }
            return new CacheAccessResult(result == HIT, blockNumber);
        }

        // call this if all blocks in the set are full. If the set contains more than
        // one block,
        // It will pick on to replace based on selected replacement policy.
        private int selectBlockToReplace(final int first, final int last) {
            int replaceBlock = first;
            if (first != last) {
                switch (CacheSimulator.this.cacheReplacementSelector.getSelectedIndex()) {
                    case CacheSimulator.RANDOM:
                        replaceBlock = first + CacheSimulator.this.randu.nextInt(last - first + 1);
                        if (CacheSimulator.debug) // System.out.print
                        {
                            CacheSimulator.this.writeLog(" -- Random replace block " + replaceBlock + "\n");
                        }
                        break;
                    case CacheSimulator.LRU:
                    default:
                        int leastRecentAccessTime = CacheSimulator.this.memoryAccessCount; // all of them have to be 
                        // less than this
                        for (int block = first; block <= last; block++) {
                            if (this.blocks[block].mostRecentAccessTime < leastRecentAccessTime) {
                                leastRecentAccessTime = this.blocks[block].mostRecentAccessTime;
                                replaceBlock = block;
                            }
                        }
                        if (CacheSimulator.debug) // System.out.print
                        {
                            CacheSimulator.this.writeLog(" -- LRU replace block " + replaceBlock + "; unused since ("
                                + leastRecentAccessTime + ")\n");
                        }
                        break;
                }
            }
            return replaceBlock;
        }
    }

    // Class to display animated cache
    private class Animation {

        public final Color hitColor = Color.GREEN;
        public final Color missColor = Color.RED;
        public final Color defaultColor = Color.WHITE;
        private final Box animation;
        private JTextField[] blocks;

        public Animation() {
            this.animation = Box.createVerticalBox();
        }

        private Box getAnimationBox() {
            return this.animation;
        }

        public int getNumberOfBlocks() {
            return (this.blocks == null) ? 0 : this.blocks.length;
        }

        public void showHit(final int blockNum) {
            this.blocks[blockNum].setBackground(this.hitColor);
        }

        public void showMiss(final int blockNum) {
            this.blocks[blockNum].setBackground(this.missColor);
        }

        public void reset() {
            for (final JTextField block : this.blocks) {
                block.setBackground(this.defaultColor);
            }
        }

        // initialize animation of cache blocks
        private void fillAnimationBoxWithCacheBlocks() {
            this.animation.setVisible(false);
            this.animation.removeAll();
            final int numberOfBlocks =
                CacheSimulator.this.cacheBlockCountChoicesInt[CacheSimulator.this.cacheBlockCountSelector.getSelectedIndex()];
            final int totalVerticalPixels = 128;
            final int blockPixelHeight = (numberOfBlocks > totalVerticalPixels) ? 1 :
                totalVerticalPixels / numberOfBlocks;
            final int blockPixelWidth = 40;
            final Dimension blockDimension = new Dimension(blockPixelWidth, blockPixelHeight);
            this.blocks = new JTextField[numberOfBlocks];
            for (int i = 0; i < numberOfBlocks; i++) {
                this.blocks[i] = new JTextField();
                this.blocks[i].setEditable(false);
                this.blocks[i].setBackground(this.defaultColor);
                this.blocks[i].setSize(blockDimension);
                this.blocks[i].setPreferredSize(blockDimension);
                this.animation.add(this.blocks[i]);
            }
            this.animation.repaint();
            this.animation.setVisible(true);
        }

    }
}
