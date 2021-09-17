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

package net.opentsdb.horizon.alerting;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.app.AlertProcessorConfig;
import org.slf4j.Logger;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Clean configuration model.
 * <p>
 * See {@link AlertProcessorConfig} for reference.
 *
 * @author skhegay
 */
public abstract class AbstractConfig {

    @Getter
    private Configuration configuration;

    public AbstractConfig(Configuration configuration)
    {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        this.configuration = configuration;
    }

    private Integer getInt(final String key)
    {
        final String value = configuration.getString(key);
        if (value == null || value.trim().isEmpty()) {
            throw new NoSuchElementException("No value for key=" + key);
        }
        return Integer.valueOf(value);
    }

    private Integer getInt(final String key, final int defaultValue)
    {
        final String value = configuration.getString(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.valueOf(value);
    }

    private String get(final String key)
    {
        return configuration.getString(key);
    }

    private String get(final String key, final String defaultValue)
    {
        final String value = configuration.getString(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private List<String> getList(final String key)
    {
        final List<Object> list = configuration.getList(key);
        return Lists.transform(list, Functions.toStringFunction());
    }

    private List<String> getList(final String key,
                                 final List<String> defaultValue)
    {
        final List<Object> list = configuration.getList(key, defaultValue);
        return Lists.transform(list, Functions.toStringFunction());
    }

    protected Integer getInt(final ConfigItem configItem)
    {
        final Integer result;
        if (configItem.getDefault() == null) {
            result = getInt(configItem.key());
        } else {
            result = getInt(configItem.key(), (Integer) configItem.getDefault());
        }
        return result;
    }

    protected String get(final ConfigItem configItem)
    {
        final String result;
        if (configItem.getDefault() == null) {
            result = get(configItem.key());
        } else {
            result = get(configItem.key(), (String) configItem.getDefault());
        }
        return result;
    }

    protected List<String> getList(final ConfigItem configItem)
    {
        final List<String> result;
        if (configItem.getDefault() == null) {
            result = getList(configItem.key());
        } else {
            @SuppressWarnings("unchecked")
            final List<String> defaultList =
                    (List<String>) configItem.getDefault();
            result = getList(configItem.key(), defaultList);
        }
        return result;
    }

    protected String getMethodName(final String configName)
    {
        final String[] parts = configName.split("_");
        return "get" + Arrays.stream(parts)
                .map(part -> part.substring(0, 1).toUpperCase() +
                        part.substring(1).toLowerCase())
                .collect(Collectors.joining());
    }

    protected Method getMethod(final String methodName)
            throws NoSuchMethodException
    {
        try {
            return getClass().getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            return getClass().getMethod(methodName);
        }
    }

    protected Object getValue(final String configName)
    {
        final String methodName = getMethodName(configName);
        try {
            final Method method = getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract ConfigItem[] getConfigValues();

    @Override
    public final String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final ConfigItem item : getConfigValues()) {
            final Object value = getValue(item.name());
            final boolean isDefault = item.getDefault() != null
                    && item.getDefault().equals(value);

            stringBuilder
                    .append(item.key())
                    .append(" = ")
                    .append(value)
                    .append(isDefault ? " (default)" : "")
                    .append("\n");
        }
        return stringBuilder.toString();
    }

    /* ------------ Builder ------------ */

    protected abstract static class Builder<
            C extends AbstractConfig,
            B extends Builder<C, B>
            >
    {

        /* ------------ Fields ------------ */

        private String defaultPath;

        private String envVariableName;

        private String systemPropertyName;

        private String fromApplicationArgs;

        private boolean verbose = false;

        /* ------------ Constructors ------------ */

        protected abstract B self();

        protected abstract C build(Configuration configuration);

        protected abstract Logger getLogger();

        /* ------------ Methods ------------ */

        public B setDefaultPath(final String defaultPath)
        {
            this.defaultPath = defaultPath;
            return self();
        }

        public B tryEnvVariable(final String envVariableName)
        {
            this.envVariableName = envVariableName;
            return self();
        }

        public B trySystemProperty(final String systemPropertyName)
        {
            this.systemPropertyName = systemPropertyName;
            return self();
        }

        public B tryFromArgs(final String[] args, final int zeroBasedPosition)
        {
            if (zeroBasedPosition < 0) {
                throw new IllegalArgumentException(
                        "zeroBasedPosition cannot be < 0: given=" +
                                zeroBasedPosition);
            }
            if (args != null && zeroBasedPosition < args.length) {
                this.fromApplicationArgs = args[zeroBasedPosition];
            }
            return self();
        }

        public B setVerbose(boolean versobe)
        {
            this.verbose = versobe;
            return self();
        }

        private void log(String format, Object... args)
        {
            if (verbose) {
                getLogger().info(format, args);
            }
        }

        public C build()
        {
            // Default
            String configPath = defaultPath;
            log("Default config: {}", configPath);

            // Override with system property
            if (systemPropertyName != null) {
                final String fromSysProperty =
                        System.getProperty(systemPropertyName);
                if (fromSysProperty != null && !fromSysProperty.isEmpty()) {
                    configPath = fromSysProperty;
                    log("Overridden from system properties: {}", configPath);
                }
            }

            // Override with env variable
            if (envVariableName != null) {
                final String fromEnv = System.getenv().get(envVariableName);
                if (fromEnv != null && !fromEnv.isEmpty()) {
                    configPath = fromEnv;
                    log("Overridden from env variable: {}", configPath);
                }
            }

            // From argument
            if (fromApplicationArgs != null && !fromApplicationArgs.isEmpty()) {
                configPath = fromApplicationArgs;
                log("Overridden from arguments: {}", configPath);
            }

            final Configuration configuration;
            try {
                log("Loading configuration from: {}", configPath);
                configuration = new PropertiesConfiguration(configPath);
            } catch (ConfigurationException e) {
                throw new RuntimeException(
                        "Failed to load configuration from " + configPath, e);
            }

            return build(configuration);
        }
    }
}
