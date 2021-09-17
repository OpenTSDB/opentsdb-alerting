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

package net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = SlackRequest.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface SlackRequest {

    @JsonProperty("text")
    String getText();

    @JsonProperty("attachments")
    List<Attachment> getAttachments();

    @JsonProperty("url")
    String getUrl();

    @JsonProperty("type")
    String getType();

    @JsonProperty("ts")
    String getTimestampSec();

    @JsonProperty("parse")
    String getParse();

    interface Builder<B extends Builder<B>>
            extends net.opentsdb.horizon.alerting.Builder<B, SlackRequest>
    {

        B setText(String text);

        B setAttachments(List<Attachment> attachments);

        B setUrl(String url);

        B setType(String type);

        B setTimestampSec(String tsSec);

        B setParse(String parse);
    }

    @JsonSerialize(as = Attachment.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Attachment {

        @JsonProperty("color")
        SlackColor getColor();

        @JsonProperty("title")
        String getTitle();

        @JsonProperty("title_link")
        String getTitleLink();

        @JsonProperty("id")
        String getId();

        @JsonProperty("pretext")
        String getPretext();

        @JsonProperty("text")
        String getText();

        @JsonProperty("fields")
        List<Field> getFields();

        @JsonProperty("ts")
        long getTimestampSec();

        @JsonProperty("mrkdwn_in")
        String[] getMarkdownIn();

        interface Builder<B extends Builder<B>>
                extends net.opentsdb.horizon.alerting.Builder<B, Attachment>
        {

            B setColor(SlackColor color);

            B setTitle(String title);

            B setTitleLink(String titleLink);

            B setId(String id);

            B setPretext(String pretext);

            B setText(String text);

            B setFields(List<Field> fields);

            B addLongField(final String title, final String value);

            B addShortField(final String title, final String value);

            B setTimestampSec(long tsSec);

            B setMarkdownIn(String ...markdownIn);
        }
    }

    @JsonSerialize(as = Field.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Field {

        @JsonProperty("title")
        String getTitle();

        @JsonProperty("value")
        String getValue();

        @JsonProperty("short")
        boolean isShort();

        interface Builder<B extends Builder<B>>
                extends net.opentsdb.horizon.alerting.Builder<B, Field>
        {

            B setTitle(String title);

            B setValue(String value);

            B setShort(boolean isShort);
        }
    }
}
