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

package mediathek.mainwindow;

import mediathek.gui.messages.DarkModeChangeEvent;
import mediathek.tool.MessageBus;
import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class MemoryUsagePanel extends JPanel {

    private final TimeSeries total = new TimeSeries("Total Memory");
    private final DateAxis domain = new DateAxis("Time");
    private final NumberAxis range = new NumberAxis("Memory");
    private final JFreeChart chart;

    public MemoryUsagePanel(int maxAge, @NotNull TimeUnit timeUnit) {

        super(new BorderLayout());

        total.setMaximumItemAge(TimeUnit.MILLISECONDS.convert(maxAge, timeUnit));

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(total);

        range.setAutoRange(true);

        var renderer = new XYAreaRenderer();
        renderer.setOutline(true);
        renderer.setSeriesPaint(0, new Color(255, 0, 0, 100));
        renderer.setSeriesOutlinePaint(0, Color.RED);

        var plot = new XYPlot(dataset, domain, range, renderer);
        plot.setBackgroundPaint(Color.BLACK);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        domain.setAutoRange(true);
        domain.setLowerMargin(0.0);
        domain.setUpperMargin(0.0);
        domain.setTickLabelsVisible(true);

        range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        range.setNumberFormatOverride(new DecimalFormat("#######.##"));

        chart = new JFreeChart(plot);
        chart.setAntiAlias(true);
        chart.removeLegend();

        setLabelColors();

        var chartPanel = new ChartPanel(chart);
        chartPanel.setPopupMenu(null);
        chartPanel.setMouseZoomable(false);
        chartPanel.setDomainZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setMouseWheelEnabled(false);
        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.gc();
            }
        });
        add(chartPanel);

        MessageBus.getMessageBus().subscribe(this);
    }

    @Handler
    private void handleDarkModeChange(DarkModeChangeEvent e) {
        SwingUtilities.invokeLater(this::setLabelColors);
    }

    private void setLabelColors() {
        var labelColor = UIManager.getColor("Label.foreground");
        var panelColor = UIManager.getColor("Panel.background");
        if (labelColor == null) {
            labelColor = Color.LIGHT_GRAY;
        }
        if (panelColor == null) {
            panelColor = Color.DARK_GRAY;
        }

        domain.setLabelPaint(labelColor);
        domain.setTickLabelPaint(labelColor);
        domain.setTickMarkPaint(labelColor);
        domain.setAxisLinePaint(labelColor);

        range.setLabelPaint(labelColor);
        range.setTickLabelPaint(labelColor);
        range.setTickMarkPaint(labelColor);
        range.setAxisLinePaint(labelColor);

        chart.setBackgroundPaint(panelColor);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(panelColor);
        plot.setOutlinePaint(labelColor);
        plot.setDomainGridlinePaint(labelColor);
        plot.setRangeGridlinePaint(labelColor);
    }

    private void addTotalObservation(double y) {
        total.add(new Millisecond(), y);
    }

    public class MemoryUsageDataGenerator extends Timer implements ActionListener {

        /**
         * Constructor.
         *
         * @param interval the interval
         */
        public MemoryUsageDataGenerator(int interval, @NotNull TimeUnit timeUnit) {
            super((int) TimeUnit.MILLISECONDS.convert(interval, timeUnit), null);
            addActionListener(this);
        }

        /**
         * Adds a new total memory reading (converted to MByte) to the dataset.
         *
         * @param event the action event.
         */
        public void actionPerformed(ActionEvent event) {
            long t = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            addTotalObservation(t);
        }

    }
}
