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

package net.opentsdb.horizon.alerts.model;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.ToString;

import net.opentsdb.horizon.alerts.serde.SnoozeDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.opentsdb.query.filter.QueryFilter;

@Getter
@ToString
public class Snooze {

    public final static int NOT_FOUND = -1;

    private long id = NOT_FOUND;
    private String namespace;
    private List<Long> alertIds = Collections.emptyList();
    private List<String> labels = Collections.emptyList();
    private long startTime;
    private long endTime;

    @JsonDeserialize(using = SnoozeDeserializer.class)
    private QueryFilter filter;
    private String reason;

    private long createdTime;
    private String createdBy;

    private long updatedTime;
    private String updatedBy;


    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        final Snooze snooze = objectMapper.readValue(
                new File("src/main/resources/snoozes/snooze_config.json"),
                Snooze.class);

        System.out.println(snooze.toString());
    }

    @VisibleForTesting
    public void overrideEndTime(long endTime) {
        this.endTime = endTime;
    }

    @VisibleForTesting
    public void overrideStartTime(long startTime) {
        this.startTime = startTime;
    }

}
//
