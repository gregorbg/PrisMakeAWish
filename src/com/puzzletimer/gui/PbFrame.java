package com.puzzletimer.gui;

import com.puzzletimer.database.SolutionDAO;
import com.puzzletimer.models.Category;
import com.puzzletimer.models.Solution;
import com.puzzletimer.state.CategoryManager;
import com.puzzletimer.state.ConfigurationManager;
import com.puzzletimer.state.SolutionManager;
import com.puzzletimer.state.TimerManager;
import com.puzzletimer.statistics.*;
import com.puzzletimer.util.SolutionUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import static com.puzzletimer.Internationalization.i18n;

public class PbFrame extends JFrame {
    private String nullTime;

    private ConfigurationManager configurationManager;
    private CategoryManager categoryManager;

    private JButton buttonOk;
    private JTable table;

    public PbFrame(
            final TimerManager timerManager,
            final SolutionManager solutionManager,
            final CategoryManager categoryManager,
            final ConfigurationManager configurationManager,
            SolutionDAO solutionDAO) {
        super();

        setMinimumSize(new Dimension(800, 600));

        this.nullTime = "XX:XX:XX";

        this.configurationManager = configurationManager;
        this.categoryManager = categoryManager;

        createComponents();
        pack();

        // title
        categoryManager.addListener(new CategoryManager.Listener() {
            @Override
            public void categoriesUpdated(Category[] categories, Category currentCategory) {
                setTitle(
                        String.format(
                                i18n("pbs.pbs_category"),
                                currentCategory.getDescription()));
            }
        });
        categoryManager.notifyListeners();

        timerManager.addListener(new TimerManager.Listener() {
            @Override
            public void precisionChanged(String timerPrecisionId) {
                if(timerPrecisionId.equals("CENTISECONDS")) {
                    PbFrame.this.nullTime = "XX:XX.XX";
                } else if(timerPrecisionId.equals("MILLISECONDS")) {
                    PbFrame.this.nullTime = "XX:XX.XXX";
                }

                PbFrame.this.updateTable(solutionDAO);
            }
        });

        // statistics, table
        solutionManager.addListener(new SolutionManager.Listener() {
            @Override
            public void solutionsUpdated(Solution[] solutions) {
                updateTable(solutionDAO);
            }
        });
        solutionManager.notifyListeners();

        // close button
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.buttonOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                PbFrame.this.setVisible(false);
            }
        });

        // esc key closes window
        this.getRootPane().registerKeyboardAction(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        PbFrame.this.setVisible(false);
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void createComponents() {
        setLayout(
                new MigLayout(
                        "fill",
                        "[][pref!]",
                        "[pref]16[pref!]"));

        // table
        this.table = new JTable();

        JScrollPane scrollPane = new JScrollPane(this.table);
        this.table.setFillsViewportHeight(true);
        scrollPane.setPreferredSize(new Dimension(0, 0));
        add(scrollPane, "grow, wrap");

        // buttonOk
        this.buttonOk = new JButton(i18n("pbs.ok"));
        add(this.buttonOk, "tag ok, span");
    }

    private void updateTable(SolutionDAO solutionDAO) {
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.addColumn(i18n("pbs.table_category"));
        tableModel.addColumn(i18n("pbs.single"));
        tableModel.addColumn(i18n("pbs.mo3"));
        tableModel.addColumn(i18n("pbs.ao5"));
        tableModel.addColumn(i18n("pbs.ao12"));
        tableModel.addColumn(i18n("pbs.ao50"));
        tableModel.addColumn(i18n("pbs.ao100"));
        tableModel.addColumn(i18n("pbs.avg_global"));

        this.table.setModel(tableModel);

        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        int[] columnsWidth = { 150, 50, 50, 50, 50, 50, 50, 50 };
        for (int i = 0; i < columnsWidth.length; i++) {
            TableColumn indexColumn = this.table.getColumnModel().getColumn(i);
            indexColumn.setPreferredWidth(columnsWidth[i]);
        }

        StatisticalMeasure[] measures = {
                new Best(1, Integer.MAX_VALUE),
                new BestMean(3, Integer.MAX_VALUE),
                new BestAverage(5, Integer.MAX_VALUE),
                new BestAverage(12, Integer.MAX_VALUE),
                new BestAverage(50, Integer.MAX_VALUE),
                new BestAverage(100, Integer.MAX_VALUE),
                new Average(3, Integer.MAX_VALUE)
        };

        for (Category category : this.categoryManager.getCategories()) {
            Object[] tableRow = new Object[1 + measures.length];
            tableRow[0] = category.getDescription();

            for (int i = 0; i < measures.length; i++) {
                StatisticalMeasure measure = measures[i];

                Solution[] catSolutions = solutionDAO.getAll(category);
                if (catSolutions.length < measure.getMinimumWindowSize()) continue;

                measure.setSolutions(catSolutions, this.configurationManager.getConfiguration("TIMER-PRECISION").equals("CENTISECONDS"));
                tableRow[1 + i] = SolutionUtils.formatMinutes(measure.getValue(), this.configurationManager.getConfiguration("TIMER-PRECISION"), measures[i].getRound()).replace("DNF", this.nullTime);
            }

            tableModel.addRow(tableRow);
        }
    }
}
