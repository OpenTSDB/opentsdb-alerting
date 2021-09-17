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

package net.opentsdb.horizon.alerting.config;

import net.opentsdb.horizon.alerting.AbstractConfig;
import net.opentsdb.horizon.alerting.ConfigItem;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AbstractConfigTest {

    public static class TestConfig extends AbstractConfig {

        public enum TestItem implements ConfigItem {

            // These values should come from the config only
            NO_DEFAULT_INT(),
            NO_DEFAULT_STRING(),
            NO_DEFAULT_LIST(),

            // There values should come from the defaults, since not provided
            // in the config (constructor)
            USE_DEFAULT_INT(42),
            USE_DEFAULT_STRING("hola"),
            USE_DEFAULT_LIST(Arrays.asList("one", "two", "three")),

            // There values should come from config, even though they have
            // defaults.
            USE_CONFIGURED_INT(-42),
            USE_CONFIGURED_STRING("should not be used"),
            USE_CONFIGURED_LIST(Arrays.asList("should", "not", "use")),

            SHOULD_THROW();

            private final Object defaultValue;

            TestItem(Object defaultValue)
            {
                this.defaultValue = defaultValue;
            }

            TestItem()
            {
                this(null);
            }

            @Override
            public String key()
            {
                return defaultKeyNameGenerator.apply(name());
            }

            @Override
            public Object getDefault()
            {
                return defaultValue;
            }
        }

        public static final Configuration CONFIGURATION;

        static {
            // Configuration should be loaded from a file. Pretend it was.
            // TODO: Load an actual file.
            CONFIGURATION = new PropertiesConfiguration();
            CONFIGURATION.addProperty("no.default.int", "42");
            CONFIGURATION.addProperty("no.default.string", "hello");
            CONFIGURATION.addProperty("no.default.list", "one,two,three");
            CONFIGURATION.addProperty("use.configured.int", "4242");
            CONFIGURATION.addProperty("use.configured.string", "hello again");
            CONFIGURATION.addProperty("use.configured.list", "three,two,one");
        }

        public TestConfig()
        {
            super(CONFIGURATION);
        }

        @Override
        protected ConfigItem[] getConfigValues()
        {
            return TestItem.values();
        }

        public int getNoDefaultInt()
        {
            return getInt(TestItem.NO_DEFAULT_INT);
        }

        public String getNoDefaultString()
        {
            return get(TestItem.NO_DEFAULT_STRING);
        }

        public List<String> getNoDefaultList()
        {
            return getList(TestItem.NO_DEFAULT_LIST);
        }

        public int getUseDefaultInt()
        {
            return getInt(TestItem.USE_DEFAULT_INT);
        }

        public String getUseDefaultString()
        {
            return get(TestItem.USE_DEFAULT_STRING);
        }

        public List<String> getUseDefaultList()
        {
            return getList(TestItem.USE_DEFAULT_LIST);
        }

        public int getUseConfiguredInt()
        {
            return getInt(TestItem.USE_CONFIGURED_INT);
        }

        public String getUseConfiguredString()
        {
            return get(TestItem.USE_CONFIGURED_STRING);
        }

        public List<String> getUseConfiguredList()
        {
            return getList(TestItem.USE_CONFIGURED_LIST);
        }

        public int getShouldThrow()
        {
            return getInt(TestItem.SHOULD_THROW);
        }
    }

    /**
     * To test <t>toString()</t> implementation.
     *
     * <p> Exclude the <t>SHOULD_THROW</t> variable from printables, to avoid
     * a thrown exception.
     */
    public static class TestToStringConfig extends TestConfig {

        @Override
        protected ConfigItem[] getConfigValues()
        {
            final ConfigItem[] original = super.getConfigValues();
            // Exclude the SHOULD_THROW entry.
            return Arrays.copyOfRange(original, 0, original.length - 1);
        }
    }

    /**
     * Test subject.
     */
    public static final TestConfig TEST_CONFIG = new TestConfig();

    @Test
    public void testGetConfigurationsWithNoDefaults()
    {
        assertEquals(TEST_CONFIG.getNoDefaultInt(), 42);
        assertEquals(TEST_CONFIG.getNoDefaultString(), "hello");
        assertEquals(TEST_CONFIG.getNoDefaultList(),
                Arrays.asList("one", "two", "three"));
    }

    @Test
    public void testGetDefaults()
    {
        assertEquals(TEST_CONFIG.getUseDefaultInt(), 42);
        assertEquals(TEST_CONFIG.getUseDefaultString(), "hola");
        assertEquals(TEST_CONFIG.getUseDefaultList(),
                Arrays.asList("one", "two", "three"));
    }

    @Test
    public void testGetConfigured()
    {
        assertEquals(TEST_CONFIG.getUseConfiguredInt(), 4242);
        assertEquals(TEST_CONFIG.getUseConfiguredString(), "hello again");
        assertEquals(TEST_CONFIG.getUseConfiguredList(),
                Arrays.asList("three", "two", "one"));
    }

    @Test
    public void testIntegerValueNotConfigured()
    {
        assertThrows(NoSuchElementException.class,
                TEST_CONFIG::getShouldThrow);
    }

    @Test
    public void testToString()
    {
        final String expected = "no.default.int = 42\n" +
                "no.default.string = hello\n" +
                "no.default.list = [one, two, three]\n" +
                "use.default.int = 42 (default)\n" +
                "use.default.string = hola (default)\n" +
                "use.default.list = [one, two, three] (default)\n" +
                "use.configured.int = 4242\n" +
                "use.configured.string = hello again\n" +
                "use.configured.list = [three, two, one]\n";

        assertEquals(expected, new TestToStringConfig().toString());
    }
}
