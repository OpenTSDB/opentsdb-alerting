/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.config.impl;

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFetcher;
import io.undertow.util.FileUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.nio.file.Files;

import java.nio.file.Paths;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


public class FileConfigFetcher implements AlertConfigFetcher {

    private String dir = null;
    private int mirrorSetId;
    private int mirrorid;
    private int daemonid;
    private EnvironmentConfig environmentConfig = new EnvironmentConfig();

    public FileConfigFetcher(int daemonid) {
        this.dir = environmentConfig.getAlertConfigFilePath();
        this.mirrorSetId = environmentConfig.getMirrorSetId();
        this.mirrorid = environmentConfig.getMirrorId();
        this.daemonid = daemonid;
    }

    @Override
    public Map<Long, AlertConfig> getAlertConfig() {

        try {
            return Files.list(Paths.get(dir))
                    .map(path -> {
                        try {
                            return FileUtils.readFile(new FileInputStream(path.toFile()));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }).filter(Objects::nonNull).map(json -> {
                        try {
                            return AlertUtils.loadConfig(json);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toMap(AlertConfig::getAlertId,Function.identity()));


        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    public static void main(String args[]) {

        FileConfigFetcher fileConfigFetcher = new FileConfigFetcher(0);

        System.out.println(fileConfigFetcher.getAlertConfig());

    }
}
