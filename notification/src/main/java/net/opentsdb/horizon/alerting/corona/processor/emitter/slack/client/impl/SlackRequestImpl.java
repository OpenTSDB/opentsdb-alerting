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

package net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackColor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackRequest;

@Getter
final class SlackRequestImpl implements SlackRequest {

    private final String text;

    private final List<SlackRequest.Attachment> attachments;

    private final String url;

    private final String type;

    private final String timestampSec;

    private final String parse;

    private SlackRequestImpl(final Builder builder)
    {
        this.text = builder.text;
        this.attachments = builder.attachments;
        this.url = builder.url;
        this.type = builder.type;
        this.timestampSec = builder.timestampSec;
        this.parse = builder.parse;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SlackRequestImpl that = (SlackRequestImpl) o;
        return Objects.equals(text, that.text) &&
                Objects.equals(attachments, that.attachments) &&
                Objects.equals(url, that.url) &&
                Objects.equals(type, that.type) &&
                Objects.equals(timestampSec, that.timestampSec) &&
                Objects.equals(parse, that.parse);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(text, attachments, url, type, timestampSec, parse);
    }

    @Override
    public String toString()
    {
        return "SlackRequestImpl{" +
                "text='" + text + '\'' +
                ", attachments=" + attachments +
                ", url='" + url + '\'' +
                ", type='" + type + '\'' +
                ", timestampSec='" + timestampSec + '\'' +
                ", parse='" + parse + '\'' +
                '}';
    }

    static final class Builder implements SlackRequest.Builder<Builder> {

        private String text;

        private List<SlackRequest.Attachment> attachments;

        private String url;

        private String type;

        private String timestampSec;

        private String parse;

        Builder() { }

        @Override
        public Builder setText(final String text)
        {
            this.text = text;
            return this;
        }

        @Override
        public Builder setAttachments(final List<SlackRequest.Attachment> attachments)
        {
            this.attachments = attachments;
            return this;
        }

        @Override
        public Builder setUrl(final String url)
        {
            this.url = url;
            return this;
        }

        @Override
        public Builder setType(final String type)
        {
            this.type = type;
            return this;
        }

        @Override
        public Builder setTimestampSec(final String tsSec)
        {
            this.timestampSec = tsSec;
            return this;
        }

        @Override
        public Builder setParse(final String parse)
        {
            this.parse = parse;
            return this;
        }

        @Override
        public SlackRequest build()
        {
            return new SlackRequestImpl(this);
        }
    }

    /* ------------ Static Classes ------------ */

    @Getter
    static final class Field implements SlackRequest.Field {

        private final String title;

        private final String value;

        private final boolean isShort;

        private Field(final Builder builder)
        {
            this.title = builder.title;
            this.value = builder.value;
            this.isShort = builder.isShort;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Field that = (Field) o;
            return isShort == that.isShort &&
                    Objects.equals(title, that.title) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(title, value, isShort);
        }

        @Override
        public String toString()
        {
            return "Field{" +
                    "title='" + title + '\'' +
                    ", value='" + value + '\'' +
                    ", isShort=" + isShort +
                    '}';
        }

        static final class Builder
                implements SlackRequest.Field.Builder<Builder>
        {

            private String title;

            private String value;

            private boolean isShort;

            Builder() { }

            @Override
            public Builder setTitle(final String title)
            {
                this.title = title;
                return this;
            }

            @Override
            public Builder setValue(final String value)
            {
                this.value = value;
                return this;
            }

            @Override
            public Builder setShort(final boolean aShort)
            {
                isShort = aShort;
                return this;
            }

            @Override
            public SlackRequest.Field build()
            {
                return new Field(this);
            }
        }
    }

    @Getter
    final static class Attachment implements SlackRequest.Attachment {

        private final SlackColor color;

        private final String title;

        private final String titleLink;

        private final String id;

        private final String pretext;

        private final String text;

        private final List<SlackRequest.Field> fields;

        private final long timestampSec;

        private final String[] markdownIn;

        private Attachment(final Builder builder)
        {
            this.color = builder.color;
            this.title = builder.title;
            this.titleLink = builder.titleLink;
            this.id = builder.id;
            this.pretext = builder.pretext;
            this.text = builder.text;
            this.fields = builder.fields;
            this.timestampSec = builder.timestampSec;
            this.markdownIn = builder.markdownIn;
        }


        @Override
        public boolean equals(final Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Attachment that = (Attachment) o;
            return timestampSec == that.timestampSec &&
                    Objects.equals(color, that.color) &&
                    Objects.equals(title, that.title) &&
                    Objects.equals(titleLink, that.titleLink) &&
                    Objects.equals(id, that.id) &&
                    Objects.equals(pretext, that.pretext) &&
                    Objects.equals(text, that.text) &&
                    Objects.equals(fields, that.fields) &&
                    Arrays.equals(markdownIn, that.markdownIn);
        }

        @Override
        public int hashCode()
        {
            int result = Objects.hash(color,
                    title,
                    titleLink,
                    id,
                    pretext,
                    text,
                    fields,
                    timestampSec);
            result = 31 * result + Arrays.hashCode(markdownIn);
            return result;
        }

        @Override
        public String toString()
        {
            return "Attachment{" +
                    "color='" + color + '\'' +
                    ", title='" + title + '\'' +
                    ", titleLink='" + titleLink + '\'' +
                    ", id='" + id + '\'' +
                    ", pretext='" + pretext + '\'' +
                    ", text='" + text + '\'' +
                    ", fields=" + fields +
                    ", timestampSec=" + timestampSec +
                    ", markdownIn=" + Arrays.toString(markdownIn) +
                    '}';
        }

        static final class Builder
                implements SlackRequest.Attachment.Builder<Builder>
        {

            private SlackColor color;

            private String title;

            private String titleLink;

            private String id;

            private String pretext;

            private String text;

            private List<SlackRequest.Field> fields;

            private long timestampSec;

            private String[] markdownIn;

            Builder() { }

            @Override
            public Builder setColor(final SlackColor color)
            {
                this.color = color;
                return this;
            }

            @Override
            public Builder setTitle(final String title)
            {
                this.title = title;
                return this;
            }

            @Override
            public Builder setTitleLink(final String titleLink)
            {
                this.titleLink = titleLink;
                return this;
            }

            @Override
            public Builder setId(final String id)
            {
                this.id = id;
                return this;
            }

            @Override
            public Builder setPretext(final String pretext)
            {
                this.pretext = pretext;
                return this;
            }

            @Override
            public Builder setText(final String text)
            {
                this.text = text;
                return this;
            }

            @Override
            public Builder setFields(final List<SlackRequest.Field> fields)
            {
                this.fields = fields;
                return this;
            }

            @Override
            public Builder setTimestampSec(final long timestampSec)
            {
                this.timestampSec = timestampSec;
                return this;
            }

            @Override
            public Builder addLongField(final String title,
                                        final String value)
            {
                return addField(title, value, false);
            }

            @Override
            public Builder addShortField(final String title,
                                         final String value)
            {
                return addField(title, value, true);
            }

            private Builder addField(final String title,
                                     final String value,
                                     final boolean isShort)
            {
                if (fields == null) {
                    fields = new ArrayList<>();
                }

                fields.add(
                        new Field.Builder()
                                .setTitle(title)
                                .setValue(value)
                                .setShort(isShort)

                                .build()
                );
                return this;
            }

            @Override
            public Builder setMarkdownIn(final String[] markdownIn)
            {
                this.markdownIn = markdownIn;
                return this;
            }

            @Override
            public SlackRequest.Attachment build()
            {
                return new Attachment(this);
            }
        }
    }
}


