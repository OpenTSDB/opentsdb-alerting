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

package net.opentsdb.horizon.alerting.corona.processor.emitter;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterpolatorTest {

    @Test
    void interpolate() {
        assertEquals("hello", Interpolator.interpolate(
                "hello", Collections.emptyMap())
        );
        assertEquals("{{hello}}{{bye}}", Interpolator.interpolate(
                "{{hello}}{{bye}}", Collections.emptyMap())
        );
        assertEquals("HELLOWORLD", Interpolator.interpolate(
                "{{hello}}{{stranger}}", new HashMap<String, String>() {{
                    put("hello", "HELLO");
                    put("stranger", "WORLD");
                }})
        );
        assertEquals("{ {hello}} {{ world}} ", Interpolator.interpolate(
                "{ {hello}} {{ world}} ", new HashMap<String, String>() {{
                    put("hello", "HELLO");
                    put("world", "WORLD");
                }})
        );
        assertEquals("BYE", Interpolator.interpolate(
                "{{_hello}}", new HashMap<String, String>() {{
                    put("_hello", "BYE");
                }})
        );
    }

    @Test
    void interpolate_throws() {
        assertThrows(NullPointerException.class, () ->
                Interpolator.interpolate("{{hello}}", null, new String[]{"i am here"})
        );
        assertThrows(NullPointerException.class, () ->
                Interpolator.interpolate("{{hello}}", new String[]{"i am here"}, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
                Interpolator.interpolate("{{hello}}", new String[]{"one"}, new String[]{"i am here", "extra"})
        );
    }

    @Test
    void tryInterpolate_doesNotThrow() {
        assertEquals("{{hello}}", Interpolator.tryInterpolate("{{hello}}", null, new String[]{"i am here"}));
        assertEquals("{{hello}}", Interpolator.tryInterpolate("{{hello}}", new String[]{"i am here"}, null));
        assertEquals("{{hello}}", Interpolator.tryInterpolate("{{hello}}", new String[]{"one"}, new String[]{"i am here", "extra"}));
    }
}