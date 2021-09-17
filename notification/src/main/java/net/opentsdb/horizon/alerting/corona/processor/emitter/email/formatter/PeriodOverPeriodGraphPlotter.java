/*
 *  This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon.alerting.corona.processor.emitter.email.formatter;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.graphics2d.svg.SVGGraphics2D;

public class PeriodOverPeriodGraphPlotter {

    private static final String FONT_NAME = "Palatino";

    private static final Font FONT_BOLD_14 = new Font(FONT_NAME, Font.BOLD, 14);

    private static final Font FONT_BOLD_18 = new Font(FONT_NAME, Font.BOLD, 18);

    private static final Font FONT_PLAIN_12 = new Font(FONT_NAME, Font.PLAIN, 12);

    private static final Font FONT_PLAIN_14 = new Font(FONT_NAME, Font.PLAIN, 14);

    private static final Shape DOT_SHAPE = new Ellipse2D.Double(-.5, -.5, 1., 1.);

    private static final Stroke STROKE_DEFAULT =
            new BasicStroke(
                    2.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    1.0f,
                    new float[]{6.0f, 6.0f},
                    0.0f
            );

    private static final Stroke STROKE_UPPER =
            new BasicStroke(
                    1.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    1.0f,
                    new float[]{2.0f, 2.0f},
                    0.0f
            );

    private static final Stroke STROKE_LOWER =
            new BasicStroke(
                    1.0f,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_ROUND,
                    1.0f,
                    new float[]{4.0f, 2.0f},
                    0.0f
            );

    private static final Stroke STROKE_PREDICTED =
            new BasicStroke(
                    1.f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            );

    private static final Stroke STROKE_SOLID =
            new BasicStroke(
                    2.f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            );

    public String plotSVG(PeriodOverPeriodAlertView view) {
        final JFreeChart chart = createChart(view.getAlert(), createDataset(view));
        SVGGraphics2D g2 = new SVGGraphics2D(300, 200);
        g2.setRenderingHint(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);
        chart.draw(g2, new Rectangle(0, 0, 300, 200));
        return g2.getSVGElement();
    }

    public byte[] plotPNG(PeriodOverPeriodAlertView view) {
        final JFreeChart chart = createChart(view.getAlert(), createDataset(view));
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final BufferedImage bufferedImage = chart.createBufferedImage(598, 200);
            ImageIO.write(bufferedImage, "png", os);
            os.flush();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private XYDataset createDataset(PeriodOverPeriodAlertView view) {
        // Timestamps.
        final long[] timestampsSec = view.getTimestampsSec();
        final Second[] ts = new Second[timestampsSec.length];
        for (int i = 0; i < timestampsSec.length; i++) {
            ts[i] = new Second(new Date(1000L * timestampsSec[i]));
        }

        final PeriodOverPeriodAlert alert = view.getAlert();
        final TimeSeriesCollection dataset = new TimeSeriesCollection();

        dataset.addSeries(
                generateTimeseries("observed", ts, alert.getObservedValues())
        );
        dataset.addSeries(
                generateTimeseries("predicted", ts, alert.getPredictedValues())
        );

        alert.getLowerBadValues().ifPresent(doubles -> dataset.addSeries(
                generateTimeseries("lower bad", ts, doubles)
        ));
        alert.getLowerWarnValues().ifPresent(doubles -> dataset.addSeries(
                generateTimeseries("lower warn", ts, doubles)
        ));
        alert.getUpperBadValues().ifPresent(doubles -> dataset.addSeries(
                generateTimeseries("upper bad", ts, doubles)
        ));
        alert.getUpperWarnValues().ifPresent(doubles -> dataset.addSeries(
                generateTimeseries("uppper warn", ts, doubles)
        ));

        return dataset;
    }

    private TimeSeries generateTimeseries(final String name,
                                          final Second[] timestamps,
                                          final double[] values) {
        final TimeSeries ts = new TimeSeries(name);
        for (int i = 0; i < values.length; i++) {
            ts.add(timestamps[i], values[i]);
        }
        return ts;
    }

    /**
     * Plots a chart.
     *
     * @param dataset timeseries dataset
     * @return plotted chart.
     */
    private static JFreeChart createChart(final PeriodOverPeriodAlert alert,
                                          final XYDataset dataset) {
        final JFreeChart chart =
                ChartFactory.createTimeSeriesChart("", null, null, dataset);
        chart.getTitle().setFont(FONT_BOLD_18);
        chart.setBackgroundPaint(Color.WHITE);

        final XYPlot plot = chart.getXYPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(false);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.getDomainAxis().setLowerMargin(0.0);
        plot.getDomainAxis().setLabelFont(FONT_BOLD_14);
        plot.getDomainAxis().setTickLabelFont(FONT_PLAIN_12);
        plot.getRangeAxis().setLabelFont(FONT_BOLD_14);
        plot.getRangeAxis().setTickLabelFont(FONT_PLAIN_12);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);

        final LegendTitle legend = chart.getLegend();
        legend.setItemFont(FONT_PLAIN_14);
        legend.setFrame(BlockBorder.NONE);
        legend.setHorizontalAlignment(HorizontalAlignment.CENTER);

        final XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setDefaultShapesVisible(false);
            renderer.setDrawSeriesLineAsPath(false);
            renderer.setAutoPopulateSeriesStroke(false);
            renderer.setDefaultStroke(STROKE_DEFAULT, false);

            // Observed.
            renderer.setSeriesPaint(0, Color.BLUE);
            renderer.setSeriesStroke(0, STROKE_SOLID);
            renderer.setSeriesShapesFilled(0, true);
            renderer.setSeriesShapesVisible(0, true);
            renderer.setSeriesShape(0, DOT_SHAPE);
            renderer.setSeriesVisible(0, true);

            // Predicted.
            renderer.setSeriesPaint(1, Color.BLACK);
            renderer.setSeriesStroke(1, STROKE_PREDICTED);

            AtomicInteger idx = new AtomicInteger(2);
            alert.getLowerBadValues().ifPresent(ignored -> {
                int i = idx.getAndIncrement();
                renderer.setSeriesPaint(i, Color.RED);
                renderer.setSeriesStroke(i, STROKE_LOWER);
            });
            alert.getLowerWarnValues().ifPresent(ignored -> {
                int i = idx.getAndIncrement();
                renderer.setSeriesPaint(i, Color.ORANGE);
                renderer.setSeriesStroke(i, STROKE_LOWER);
            });
            alert.getUpperBadValues().ifPresent(ignored -> {
                int i = idx.getAndIncrement();
                renderer.setSeriesPaint(i, Color.RED);
                renderer.setSeriesStroke(i, STROKE_UPPER);
            });
            alert.getUpperWarnValues().ifPresent(ignored -> {
                int i = idx.getAndIncrement();
                renderer.setSeriesPaint(i, Color.ORANGE);
                renderer.setSeriesStroke(i, STROKE_UPPER);
            });
        }

        return chart;
    }
}
