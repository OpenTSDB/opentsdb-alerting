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

package net.opentsdb.horizon.alerts.query.eventdb;

import java.util.HashMap;
import java.util.TreeMap;

import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.model.Event;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class QueryResponseTest {

    @Test
    public void forEach() {
        final String content =
                TestUtil.loadResource(
                        "data/QueryResponseTest-payload.json"
                );
        final QueryResponse response = new QueryResponse(content);

        response.forEach(dp -> {
            // There is only one data point.
            assertEquals(dp.getHits(), 575);
            assertEquals(dp.getTags(),
                    new TreeMap<String, String>() {{
                        put("app", "n/a");
                    }});
            final Event event = dp.getEvent().get();
            assertEquals(event.getNamespace(), "NS");
            assertEquals(event.getPriority(), "low");
            assertEquals(event.getSource(), "jenkins");
            assertEquals(event.getTitle(),
                    "Job build # 1 on jenkins failed");
            assertEquals(event.getTimestamp(), 1567616466L);
            assertEquals(event.getTags(),
                    new HashMap<String, String>() {{
                        put("host", "localhost");
                    }});
        });
    }
}
