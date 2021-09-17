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

package net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.concurrent.NotThreadSafe;

import lombok.Getter;

/**
 * Data class that holds information for building/checking/updating
 * actual OpsGenie alerts.
 * <p>
 * Fields comply to https://docs.opsgenie.com/docs/alert-api.
 * <p>
 * Please verify the factual information, e.g. length limits, with
 * the official docs.
 */
@NotThreadSafe
public class OpsGenieAlert {

    /* ------------ Fields ------------ */

    /**
     * Namespace of the alert - for logging.
     *
     * @return alert namespace.
     */
    @Getter
    private final String namespace;

    /**
     * A very short alert message (docs: 130 characters).
     *
     * @return alert message.
     */
    @Getter
    private final String message;

    /**
     * Alias is a user-defined alert identifier.
     * <p>
     * OpsGenie uses it to de-duplicate noisy alerts. We use it not to
     * bother with OpsGenie-generated alert id (we store no state).
     *
     * @return alert alias.
     */
    @Getter
    private final String alias;

    /**
     * Description of the alert (docs: 15000 characters).
     *
     * @return alert description.
     */
    @Getter
    private final String description;

    /**
     * Source of the alert (docs: 100 characters).
     *
     * @return alert source.
     */
    @Getter
    private final String source;

    @Getter
    private final String[] tags;

    @Getter
    private final String user;

    @Getter
    private final String priority;

    @Getter
    final List<String> visibleToTeams;

    private final String generalNote;

    private final String recoveryNote;

    /**
     * True if we know that the alert can be closed.
     * <p>
     * The conditions entail:
     * - Alert group has only one alert
     * - The alert is a recovery alert
     * - Alert tags has the same cardinality as the group key list
     * - Tags match
     * - None of tag values are null.
     * <p>
     * Look at OpsGenieFormatter#canBeClosed(AlertGroup, GroupKey)
     *
     * @return true if the alert can be closed.
     */
    @Getter
    private final boolean closable;

    /**
     * OpsGenie details (docs: 8000 characters in total).
     *
     * @return OpsGenie details.
     */
    @Getter
    private final Map<String, String> details;

    /**
     * A mutable flag; recovery note is included if set to true.
     */
    private boolean includeRecoveryNote;

    /* ------------ Constructors ------------ */

    private OpsGenieAlert(final Builder builder)
    {
        Objects.requireNonNull(builder, "builder cannot be null");
        Objects.requireNonNull(builder.message, "message cannot be null");
        Objects.requireNonNull(builder.alias, "alias cannot be null");
        this.namespace = builder.namespace;
        this.message = builder.message;
        this.alias = builder.alias;
        this.description = builder.description;
        this.source = builder.source;
        this.tags = builder.tags;
        this.user = builder.user;
        this.priority = builder.priority;
        this.visibleToTeams = builder.visibleToTeams == null ?
                Collections.emptyList() : builder.visibleToTeams;
        this.generalNote = builder.generalNote;
        this.recoveryNote = builder.recoveryNote;
        this.includeRecoveryNote = builder.includeRecoveryNote;
        this.closable = builder.canBeClosed;
        this.details = builder.details == null ?
                Collections.emptyMap() : builder.details;
    }

    /* ------------ Methods ------------ */

    public OpsGenieAlert addRecoveryNote()
    {
        this.includeRecoveryNote = true;
        return this;
    }

    public OpsGenieAlert removeRecoveryNote()
    {
        this.includeRecoveryNote = false;
        return this;
    }

    public String getNote()
    {
        if (includeRecoveryNote && recoveryNote != null) {
            return generalNote + recoveryNote;
        }
        return generalNote;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpsGenieAlert that = (OpsGenieAlert) o;

        return closable == that.closable &&
                includeRecoveryNote == that.includeRecoveryNote &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(message, that.message) &&
                Objects.equals(alias, that.alias) &&
                Objects.equals(description, that.description) &&
                Objects.equals(source, that.source) &&
                Arrays.equals(tags, that.tags) &&
                Objects.equals(user, that.user) &&
                Objects.equals(priority, that.priority) &&
                Objects.equals(visibleToTeams, that.visibleToTeams) &&
                Objects.equals(generalNote, that.generalNote) &&
                Objects.equals(recoveryNote, that.recoveryNote) &&
                Objects.equals(details, that.details);
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(
                namespace, message, alias, description, source, user, priority,
                visibleToTeams, generalNote, recoveryNote, closable, details,
                includeRecoveryNote
        );
        result = 31 * result + Arrays.hashCode(tags);
        return result;
    }

    @Override
    public String toString()
    {
        return "OpsGenieAlert{" +
                "namespace='" + namespace + '\'' +
                ", message='" + message + '\'' +
                ", alias='" + alias + '\'' +
                ", description='" + description + '\'' +
                ", source='" + source + '\'' +
                ", tags=" + Arrays.toString(tags) +
                ", user='" + user + '\'' +
                ", priority='" + priority + '\'' +
                ", visibleToTeams=" + visibleToTeams +
                ", generalNote='" + generalNote + '\'' +
                ", recoveryNote='" + recoveryNote + '\'' +
                ", closable=" + closable +
                ", details=" + details +
                ", includeRecoveryNote=" + includeRecoveryNote +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private String namespace;

        private String message;

        private String alias;

        private String description;

        private String source;

        private String[] tags;

        private String user;

        private String priority;

        private List<String> visibleToTeams;

        private String generalNote;

        private String recoveryNote;

        private boolean includeRecoveryNote;

        private boolean canBeClosed;

        private Map<String, String> details;

        public Builder setNamespace(String namespace)
        {
            this.namespace = namespace;
            return this;
        }

        public Builder setMessage(String message)
        {
            this.message = message;
            return this;
        }

        public Builder setAlias(String alias)
        {
            this.alias = alias;
            return this;
        }

        public Builder setDescription(String description)
        {
            this.description = description;
            return this;
        }

        public Builder setSource(String source)
        {
            this.source = source;
            return this;
        }

        public Builder setTags(String... tags)
        {
            this.tags = tags;
            return this;
        }

        public Builder setUser(String user)
        {
            this.user = user;
            return this;
        }

        public Builder setPriority(String priority)
        {
            this.priority = priority;
            return this;
        }

        public Builder addVisibleToTeams(String ...teams)
        {
            if (teams != null && teams.length != 0) {
                this.visibleToTeams = Arrays.asList(teams);
            } else {
                this.visibleToTeams = null;
            }
            return this;
        }

        public Builder setGeneralNote(String generalNote)
        {
            this.generalNote = generalNote;
            return this;
        }

        public Builder setRecoveryNote(String recoveryNote)
        {
            this.recoveryNote = recoveryNote;
            return this;
        }

        public Builder setIncludeRecoveryNote(boolean includeRecoveryNote)
        {
            this.includeRecoveryNote = includeRecoveryNote;
            return this;
        }

        public Builder setCanBeClosed(boolean canBeClosed)
        {
            this.canBeClosed = canBeClosed;
            return this;
        }

        public Builder setDetails(Map<String, String> details)
        {
            this.details = details;
            return this;
        }

        public Builder addDetail(String key, String value)
        {
            if (details == null) {
                details = new HashMap<>();
            }
            details.put(key, value);
            return this;
        }

        public OpsGenieAlert build()
        {
            return new OpsGenieAlert(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
