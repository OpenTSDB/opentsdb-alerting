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

package net.opentsdb.horizon.alerting.corona.processor.emitter.email;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.graphics2d.svg.SVGGraphics2D;

public class Plotter {

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
                    2.0f,
                    new float[]{6.0f, 6.0f},
                    0.0f
            );

    private static final Stroke STROKE_DASHED =
            new BasicStroke(
                    1.5f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    1.0f,
                    new float[]{3.0f, 3.0f},
                    3.0f
            );

    private static final Stroke STROKE_SOLID =
            new BasicStroke(
                    2.f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            );

    /**
     * Creates a chart.
     *
     * @param dataset a dataset.
     * @return A chart.
     */
    private static JFreeChart createChart(final XYDataset dataset)
    {
        final JFreeChart chart =
                ChartFactory.createTimeSeriesChart("", null, null, dataset);
        chart.getTitle().setFont(FONT_BOLD_18);

        final XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(false);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.getDomainAxis().setLowerMargin(0.0);
        plot.getDomainAxis().setLabelFont(FONT_BOLD_14);
        plot.getDomainAxis().setTickLabelFont(FONT_PLAIN_12);
        plot.getRangeAxis().setLabelFont(FONT_BOLD_14);
        plot.getRangeAxis().setTickLabelFont(FONT_PLAIN_12);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setBackgroundPaint(Color.WHITE);

        chart.setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(FONT_PLAIN_14);
        chart.getLegend().setFrame(BlockBorder.NONE);
        chart.getLegend().setHorizontalAlignment(HorizontalAlignment.CENTER);
        final XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setDefaultShapesVisible(false);
            renderer.setDrawSeriesLineAsPath(false);
            renderer.setAutoPopulateSeriesStroke(false);
            renderer.setDefaultStroke(STROKE_DEFAULT, false);

            // Values line.
            renderer.setSeriesStroke(0, STROKE_SOLID);
            renderer.setSeriesPaint(0, Color.BLUE);
            renderer.setSeriesShapesFilled(0, true);
            renderer.setSeriesShapesVisible(0, true);
            renderer.setSeriesShape(0, DOT_SHAPE);
            renderer.setSeriesVisible(0, true);

            // Threshold line.
            renderer.setSeriesPaint(1, Color.RED);
            renderer.setSeriesStroke(1, STROKE_DASHED);
        }
        return chart;
    }

    private static XYDataset createDataset(final long[] timestampsSec,
                                           final double[] values,
                                           final double threshold)
    {
        final Second[] ts = new Second[timestampsSec.length];
        for (int i = 0; i < timestampsSec.length; i++) {
            ts[i] = new Second(new Date(1000L * timestampsSec[i]));
        }

        final TimeSeries s1 = new TimeSeries("value");
        for (int i = 0; i < values.length; i++) {
            s1.add(ts[i], values[i]);
        }

        final TimeSeries s2 = new TimeSeries("threshold");
        for (int i = 0; i < ts.length; i++) {
            s2.add(ts[i], threshold);
        }

        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);
        dataset.addSeries(s2);
        return dataset;
    }

    public String plotSVG(long[] timestampsSec, double[] values, double threshold)
    {
        final JFreeChart chart =
                createChart(
                        createDataset(timestampsSec, values, threshold)
                );
        SVGGraphics2D g2 = new SVGGraphics2D(300, 200);
        g2.setRenderingHint(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);
        chart.draw(g2, new Rectangle(0, 0, 300, 200));
        return g2.getSVGElement();
    }

    public byte[] plotPNG(long[] timestampsSec,
                          double[] values,
                          double threshold)
    {
        final JFreeChart chart =
                createChart(
                        createDataset(timestampsSec, values, threshold)
                );
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final BufferedImage bufferedImage =
                    chart.createBufferedImage(598, 200);
            ImageIO.write(bufferedImage, "png", os);
            os.flush();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
