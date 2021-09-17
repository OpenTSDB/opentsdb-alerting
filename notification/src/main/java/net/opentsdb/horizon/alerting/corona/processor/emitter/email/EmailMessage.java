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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

public class EmailMessage {

    /* ------------ Fields ------------ */

    @Getter
    private final String subject;

    @Getter
    private final String from;

    @Getter
    private final String fromAlias;

    @Getter
    private final String body;

    @Getter
    private final Map<String, byte[]> images;

    /* ------------ Constructors ------------ */

    private EmailMessage(final Builder builder)
    {
        this.subject = builder.subject;
        this.from = builder.from;
        this.fromAlias = builder.fromAlias;
        this.body = builder.body;
        this.images = builder.images == null ?
                Collections.emptyMap() : builder.images;

    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmailMessage that = (EmailMessage) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(from, that.from) &&
                Objects.equals(fromAlias, that.fromAlias) &&
                Objects.equals(body, that.body);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(subject, from, fromAlias, body);
    }

    @Override
    public String toString()
    {
        return "EmailMessage{" +
                "subject='" + subject + '\'' +
                ", from='" + from + '\'' +
                ", fromAlias='" + fromAlias + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private String subject;

        private String from;

        private String fromAlias;

        private String body;

        private Map<String, byte[]> images;

        private Builder() { }

        public Builder setSubject(final String subject)
        {
            this.subject = subject;
            return this;
        }

        public Builder setFrom(final String from)
        {
            this.from = from;
            return this;
        }

        public Builder setFromAlias(final String fromAlias)
        {
            this.fromAlias = fromAlias;
            return this;
        }

        public Builder setBody(final String body)
        {
            this.body = body;
            return this;
        }

        public Builder setImages(final Map<String, byte[]> images)
        {
            this.images = images;
            return this;
        }

        public EmailMessage build()
        {
            return new EmailMessage(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
