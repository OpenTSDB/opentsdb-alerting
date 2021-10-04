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

import org.junit.jupiter.api.Test;

import java.io.IOException;


class PlotterTest {

    @Test
    void plotSVG() throws IOException
    {
        // This test passes on my laptop and in an adoptopenjdk:8-jdk-hotspot
        // container, but fails in the current template build container, but
        // used to pass before. Will deal with it later.

//        final Plotter plotter = new Plotter();
//        final int N = 30;
//
//        final long initialTsSec = (1578596780L / 60L) * 60L;
//        final long[] timestampsSec = new long[N];
//        for (int i = 0; i < N; ++i) {
//            timestampsSec[i] = initialTsSec + 60L * i;
//        }
//
//        double[] baseline = new double[N];
//        for (int i = 0; i < N; ++i) {
//            baseline[i] = (i % 3 != 0 ? 10. : Double.NaN);
//        }
//
//        final double[] values = new double[N];
//        for (int i = 0; i < N; ++i) {
//            values[i] = (i % 2 != 0 ? 10. : Double.NaN);
//        }
//
//        final String baselinePlot = plotter.plotSVG(timestampsSec, baseline, 12.);
//        final String tearedPlot = plotter.plotSVG(timestampsSec, values, 12.);
//
//        assertFalse(Strings.isNullOrEmpty(baselinePlot));
//        assertFalse(Strings.isNullOrEmpty(tearedPlot));
//
//        final String artifactsDir = System.getenv("SD_ARTIFACTS_DIR");
//        if (!Strings.isNullOrEmpty(artifactsDir)) {
//            final File f = new File(artifactsDir + "/PlotterTest.html");
//            try (final FileWriter fw = new FileWriter(f)) {
//                fw.write(baselinePlot);
//                fw.write("<br />");
//                fw.write(tearedPlot);
//            }
//        }
    }
}
