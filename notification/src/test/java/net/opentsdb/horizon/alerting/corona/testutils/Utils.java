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

package net.opentsdb.horizon.alerting.corona.testutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class Utils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static File load(final String resource)
    {
        final ClassLoader classLoader = Utils.class.getClassLoader();
        return new File(classLoader.getResource(resource).getFile());
    }

    public static JsonNode parseJsonTree(final File file)
    {
        try {
            return OBJECT_MAPPER.readTree(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseJsonTree(final String resource)
    {
        return parseJsonTree(load(resource));
    }

    public static JsonNode parseJsonTreeFromString(final String content)
    {
        try {
            return OBJECT_MAPPER.readTree(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
