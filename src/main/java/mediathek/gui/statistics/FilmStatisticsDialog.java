/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.gui.statistics;

import mediathek.config.Daten;
import mediathek.daten.Country;
import mediathek.daten.DatenFilm;
import mediathek.tool.ApplicationConfiguration;
import mediathek.tool.EscapeKeyHandler;
import mediathek.tool.GermanStringSorter;
import mediathek.tool.datum.DatumFilm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FilmStatisticsDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger();
    private static final int[] INTERVAL_DAYS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 25, 30, 60, 90, 180, 365};
    private static final java.util.EnumSet<Country> EU_COUNTRY_LIST = java.util.EnumSet.of(Country.DE, Country.AT, Country.FR);

    private final AbstractAction action;
    private final SenderStatisticsTableModel senderTableModel = new SenderStatisticsTableModel();
    private final DefaultCategoryDataset intervalDataset = new DefaultCategoryDataset();
    private final JFreeChart intervalChart = ChartFactory.createBarChart(
            "Filme nach Zeitraum",
            "Zeitraum in Tagen",
            "Anzahl Filme",
            intervalDataset
    );

    private JTable tblSenderStats;
    private JLabel lblTotalFilms;
    private JLabel lblHighQualityFilms;
    private JLabel lblRegularQualityFilms;
    private JLabel lblSubtitleDownloads;
    private JLabel lblBurnedInSubtitles;
    private JLabel lblSignLanguageFilms;
    private JLabel lblTrailerFilms;
    private JLabel lblThemaCount;
    private JLabel lblLivestreams;
    private JLabel lblGeoBlockedFilms;
    private JLabel lblStatus;
    private JProgressBar progressBar;

    public FilmStatisticsDialog(@NotNull Window owner, @NotNull AbstractAction action) {
        super(owner);
        this.action = action;

        initComponents();
        EscapeKeyHandler.installHandler(this, this::dispose);
        tblSenderStats.setModel(senderTableModel);
        applyChartTheme();

        action.setEnabled(false);

        loadStatistics();
    }

    @Override
    public void dispose() {
        action.setEnabled(true);
        super.dispose();
    }

    private void loadStatistics() {
        lblStatus.setText("Berechne Statistik aus der ungefilterten Filmliste...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<ComputedStatistics, Void> worker = new SwingWorker<>() {
            @Override
            protected ComputedStatistics doInBackground() {
                return computeStatistics();
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);

                try {
                    applyStatistics(get());
                    lblStatus.setText("Statistik basiert auf der ungefilterten Filmliste ohne Livestreams.");
                }
                catch (Exception ex) {
                    logger.error("Failed to compute film statistics", ex);
                    lblStatus.setText("Statistik konnte nicht berechnet werden.");
                }
            }
        };
        worker.execute();
    }

    private @NotNull ComputedStatistics computeStatistics() {
        final var sorter = GermanStringSorter.getInstance();
        final var today = LocalDate.now();
        final var zoneId = ZoneId.systemDefault();
        final var currentGeoLocation = ApplicationConfiguration.getInstance().getGeographicLocation();

        List<DatenFilm> allEntries = Daten.getInstance().getListeFilme().parallelStream().toList();
        long livestreams = allEntries.parallelStream().filter(DatenFilm::isLivestream).count();

        List<DatenFilm> films = allEntries.parallelStream()
                .filter(film -> !film.isLivestream())
                .toList();

        long totalFilms = films.size();
        long highQualityFilms = films.parallelStream().filter(DatenFilm::isHighQuality).count();
        long regularQualityFilms = totalFilms - highQualityFilms;
        long subtitleDownloads = films.parallelStream().filter(DatenFilm::hasSubtitle).count();
        long burnedInSubtitles = films.parallelStream().filter(DatenFilm::hasBurnedInSubtitles).count();
        long signLanguageFilms = films.parallelStream().filter(DatenFilm::isSignLanguage).count();
        long trailerFilms = films.parallelStream().filter(DatenFilm::isTrailerTeaser).count();
        long geoBlockedFilms = films.parallelStream()
                .filter(film -> isGeoBlockedForLocation(film, currentGeoLocation))
                .count();
        long themaCount = films.parallelStream()
                .map(DatenFilm::getThema)
                .map(String::trim)
                .filter(thema -> !thema.isEmpty())
                .collect(Collectors.toCollection(() -> new TreeSet<>(sorter)))
                .size();

        Map<String, Long> senderCounts = films.parallelStream()
                .collect(Collectors.groupingBy(film -> normalizeLabel(film.getSender()), Collectors.counting()));

        List<SenderStatistic> sortedSenderStatistics = senderCounts.entrySet().stream()
                .map(entry -> new SenderStatistic(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(SenderStatistic::count).reversed()
                        .thenComparing(SenderStatistic::sender, sorter))
                .toList();

        List<Long> filmAgesInDays = films.parallelStream()
                .map(film -> getFilmAgeInDays(film, today, zoneId))
                .filter(age -> age >= 0)
                .toList();

        Map<Integer, Long> intervalCounts = new LinkedHashMap<>();
        for (int intervalDay : INTERVAL_DAYS) {
            long count = filmAgesInDays.parallelStream()
                    .filter(age -> age <= intervalDay)
                    .count();
            intervalCounts.put(intervalDay, count);
        }

        return new ComputedStatistics(
                totalFilms,
                sortedSenderStatistics,
                highQualityFilms,
                regularQualityFilms,
                subtitleDownloads,
                burnedInSubtitles,
                signLanguageFilms,
                trailerFilms,
                themaCount,
                livestreams,
                geoBlockedFilms,
                intervalCounts
        );
    }

    private boolean isGeoBlockedForLocation(@NotNull DatenFilm film, @NotNull Country currentGeoLocation) {
        if (!film.hasCountries()) {
            return false;
        }
        if (film.hasCountry(Country.EU)) {
            return !(film.hasCountry(currentGeoLocation) || EU_COUNTRY_LIST.contains(currentGeoLocation));
        }
        return !film.hasCountry(currentGeoLocation);
    }

    private long getFilmAgeInDays(@NotNull DatenFilm film, @NotNull LocalDate today, @NotNull ZoneId zoneId) {
        var filmDate = film.getDatumFilm();
        if (filmDate.equals(DatumFilm.UNDEFINED_FILM_DATE)) {
            return -1;
        }

        var localFilmDate = filmDate.toInstant().atZone(zoneId).toLocalDate();
        return Math.max(0, ChronoUnit.DAYS.between(localFilmDate, today));
    }

    private void applyStatistics(@NotNull ComputedStatistics statistics) {
        lblTotalFilms.setText(formatCount(statistics.totalFilms()));
        lblHighQualityFilms.setText(formatCount(statistics.highQualityFilms()));
        lblRegularQualityFilms.setText(formatCount(statistics.regularQualityFilms()));
        lblSubtitleDownloads.setText(formatCount(statistics.subtitleDownloads()));
        lblBurnedInSubtitles.setText(formatCount(statistics.burnedInSubtitles()));
        lblSignLanguageFilms.setText(formatCount(statistics.signLanguageFilms()));
        lblTrailerFilms.setText(formatCount(statistics.trailerFilms()));
        lblThemaCount.setText(formatCount(statistics.themaCount()));
        lblLivestreams.setText(formatCount(statistics.livestreams()));
        lblGeoBlockedFilms.setText(formatCount(statistics.geoBlockedFilms()));

        senderTableModel.setRows(statistics.senderStatistics());
        resizeSenderColumnWidth(tblSenderStats);

        intervalDataset.clear();
        statistics.intervalCounts().forEach((interval, count) ->
                intervalDataset.addValue(count, "Filme", String.valueOf(interval)));
    }

    private void resizeSenderColumnWidth(@NotNull JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        int width = 120;
        for (int row = 0; row < table.getRowCount(); row++) {
            TableCellRenderer renderer = table.getCellRenderer(row, 0);
            Component comp = table.prepareRenderer(renderer, row, 0);
            width = Math.max(comp.getPreferredSize().width + 10, width);
        }
        columnModel.getColumn(0).setPreferredWidth(width);
        columnModel.getColumn(1).setPreferredWidth(100);
    }

    private void applyChartTheme() {
        var labelColor = UIManager.getColor("Label.foreground");
        var panelColor = UIManager.getColor("Panel.background");
        if (labelColor == null) {
            labelColor = Color.DARK_GRAY;
        }
        if (panelColor == null) {
            panelColor = Color.WHITE;
        }

        intervalChart.setBackgroundPaint(panelColor);
        intervalChart.removeLegend();
        intervalChart.setTitle((String) null);

        CategoryPlot plot = intervalChart.getCategoryPlot();
        plot.setBackgroundPaint(panelColor);
        plot.setOutlinePaint(labelColor);
        plot.setDomainGridlinePaint(labelColor);
        plot.setRangeGridlinePaint(labelColor);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        var domainAxis = plot.getDomainAxis();
        domainAxis.setLabelPaint(labelColor);
        domainAxis.setTickLabelPaint(labelColor);
        domainAxis.setTickMarkPaint(labelColor);

        var rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLabelPaint(labelColor);
        rangeAxis.setTickLabelPaint(labelColor);
        rangeAxis.setTickMarkPaint(labelColor);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        BarRenderer renderer = new BarRenderer() {
            @Override
            protected void drawItemLabel(Graphics2D g2, org.jfree.data.category.CategoryDataset data,
                                         int row, int column, CategoryPlot plot,
                                         CategoryItemLabelGenerator generator, Rectangle2D bar, boolean negative) {
                if (generator == null) {
                    return;
                }

                String label = generator.generateLabel(data, row, column);
                if (label == null || label.isEmpty()) {
                    return;
                }

                g2.setFont(getItemLabelFont(row, column));
                g2.setPaint(getItemLabelPaint(row, column));

                Rectangle2D bounds = g2.getFontMetrics().getStringBounds(label, g2);
                double rotatedHeight = bounds.getWidth();
                double centerX = bar.getCenterX();
                double centerY;
                double padding = 4.0;
                boolean drawInsideBar;

                if (rotatedHeight + padding <= bar.getHeight()) {
                    centerY = bar.getCenterY();
                    drawInsideBar = true;
                }
                else {
                    centerY = bar.getMinY() - padding - rotatedHeight / 2.0;
                    drawInsideBar = false;
                }

                g2.setPaint(drawInsideBar ? Color.WHITE : getItemLabelPaint(row, column));
                var oldTransform = g2.getTransform();
                g2.translate(centerX, centerY);
                g2.rotate(-Math.PI / 2);
                g2.drawString(label, (float) -bounds.getCenterX(), (float) -bounds.getCenterY());
                g2.setTransform(oldTransform);
            }
        };
        plot.setRenderer(renderer);
        renderer.setSeriesPaint(0, new Color(0x4C, 0x78, 0xA8));
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(false);
        renderer.setMaximumBarWidth(0.08);
        renderer.setDefaultItemLabelGenerator(new CategoryItemLabelGenerator() {
            @Override
            public String generateRowLabel(org.jfree.data.category.CategoryDataset dataset, int row) {
                return dataset.getRowKey(row).toString();
            }

            @Override
            public String generateColumnLabel(org.jfree.data.category.CategoryDataset dataset, int column) {
                return dataset.getColumnKey(column).toString();
            }

            @Override
            public String generateLabel(org.jfree.data.category.CategoryDataset dataset, int row, int column) {
                Number value = dataset.getValue(row, column);
                return value == null ? "" : Long.toString(value.longValue());
            }
        });
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(labelColor);
    }

    private String formatCount(long count) {
        return Long.toString(count);
    }

    private String normalizeLabel(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "<Unbekannt>" : trimmed;
    }

    private JPanel createSummaryRow(String description, JLabel valueLabel) {
        var panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.add(new JLabel(description), BorderLayout.CENTER);
        valueLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        valueLabel.setFont(valueLabel.getFont().deriveFont(valueLabel.getFont().getStyle() | Font.BOLD));
        panel.add(valueLabel, BorderLayout.EAST);
        return panel;
    }

    private void initComponents() {
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var summaryPanel = new JPanel();
        var splitPane = new JSplitPane();
        var senderPanel = new JPanel();
        var senderScrollPane = new JScrollPane();
        tblSenderStats = new JTable();
        var chartPanel = new JPanel();
        var chartComponent = new ChartPanel(intervalChart);
        var statusPanel = new JPanel();
        progressBar = new JProgressBar();
        lblStatus = new JLabel();
        var buttonBar = new JPanel();
        var closeButton = new JButton();

        lblTotalFilms = new JLabel("-");
        lblHighQualityFilms = new JLabel("-");
        lblRegularQualityFilms = new JLabel("-");
        lblSubtitleDownloads = new JLabel("-");
        lblBurnedInSubtitles = new JLabel("-");
        lblSignLanguageFilms = new JLabel("-");
        lblTrailerFilms = new JLabel("-");
        lblThemaCount = new JLabel("-");
        lblLivestreams = new JLabel("-");
        lblGeoBlockedFilms = new JLabel("-");

        setTitle("Filmlisten-Statistik");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setPreferredSize(new Dimension(1220, 720));

        closeButton.setText("Schließen");
        closeButton.addActionListener(_ -> dispose());
        getRootPane().setDefaultButton(closeButton);

        tblSenderStats.setFillsViewportHeight(true);
        tblSenderStats.setAutoCreateRowSorter(true);
        tblSenderStats.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSenderStats.setShowVerticalLines(false);
        tblSenderStats.setPreferredScrollableViewportSize(new Dimension(250, 420));
        senderScrollPane.setViewportView(tblSenderStats);

        senderPanel.setBorder(BorderFactory.createTitledBorder("Filme pro Sender"));
        senderPanel.setLayout(new BorderLayout());
        senderPanel.add(senderScrollPane, BorderLayout.CENTER);

        chartComponent.setPopupMenu(null);
        chartComponent.setMouseZoomable(false);
        chartComponent.setDomainZoomable(false);
        chartComponent.setRangeZoomable(false);
        chartComponent.setMouseWheelEnabled(false);
        chartPanel.setBorder(BorderFactory.createTitledBorder("Maximale Anzahl verfügbarer Filme"));
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(chartComponent, BorderLayout.CENTER);

        splitPane.setResizeWeight(0.28);
        splitPane.setLeftComponent(senderPanel);
        splitPane.setRightComponent(chartPanel);

        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Übersicht"),
                new EmptyBorder(5, 5, 5, 5)));
        summaryPanel.setLayout(new GridLayout(0, 2, 18, 8));
        summaryPanel.add(createSummaryRow("Gesamtzahl Filme", lblTotalFilms));
        summaryPanel.add(createSummaryRow("Anzahl unterschiedlicher Themen", lblThemaCount));
        summaryPanel.add(createSummaryRow("Filme in hoher Qualität", lblHighQualityFilms));
        summaryPanel.add(createSummaryRow("Filme in normaler Qualität", lblRegularQualityFilms));
        summaryPanel.add(createSummaryRow("Filme mit separatem Untertitel-Download", lblSubtitleDownloads));
        summaryPanel.add(createSummaryRow("Filme mit eingebrannten Untertiteln", lblBurnedInSubtitles));
        summaryPanel.add(createSummaryRow("Filme mit Gebärdensprache", lblSignLanguageFilms));
        summaryPanel.add(createSummaryRow("Trailer / Teaser / Vorschau", lblTrailerFilms));
        summaryPanel.add(createSummaryRow("Livestreams", lblLivestreams));
        summaryPanel.add(createSummaryRow("Geo-blockierte Filme für den aktuellen Standort", lblGeoBlockedFilms));

        progressBar.setVisible(false);

        statusPanel.setLayout(new BorderLayout(10, 0));
        statusPanel.add(progressBar, BorderLayout.WEST);
        statusPanel.add(lblStatus, BorderLayout.CENTER);

        buttonBar.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonBar.add(closeButton);

        contentPanel.setLayout(new BorderLayout(0, 12));
        contentPanel.add(summaryPanel, BorderLayout.NORTH);
        contentPanel.add(splitPane, BorderLayout.CENTER);
        contentPanel.add(statusPanel, BorderLayout.SOUTH);

        dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        dialogPane.setLayout(new BorderLayout(0, 12));
        dialogPane.add(contentPanel, BorderLayout.CENTER);
        dialogPane.add(buttonBar, BorderLayout.SOUTH);

        setContentPane(dialogPane);
        pack();
        setLocationRelativeTo(null);
    }

    private record SenderStatistic(String sender, long count) {
    }

    private record ComputedStatistics(
            long totalFilms,
            List<SenderStatistic> senderStatistics,
            long highQualityFilms,
            long regularQualityFilms,
            long subtitleDownloads,
            long burnedInSubtitles,
            long signLanguageFilms,
            long trailerFilms,
            long themaCount,
            long livestreams,
            long geoBlockedFilms,
            Map<Integer, Long> intervalCounts
    ) {
    }

    private static final class SenderStatisticsTableModel extends AbstractTableModel {
        private final List<SenderStatistic> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Sender";
                case 1 -> "Anzahl";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> String.class;
                case 1 -> Long.class;
                default -> Object.class;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SenderStatistic row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.sender();
                case 1 -> row.count();
                default -> null;
            };
        }

        void setRows(@NotNull List<SenderStatistic> senderStatistics) {
            rows.clear();
            rows.addAll(senderStatistics);
            fireTableDataChanged();
        }
    }
}
