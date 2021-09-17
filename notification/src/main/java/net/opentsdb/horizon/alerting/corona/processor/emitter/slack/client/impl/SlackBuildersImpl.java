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

import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackBuilders;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackRequest;

public final class SlackBuildersImpl implements SlackBuilders {

    public static SlackBuilders FACTORY = new SlackBuildersImpl();

    public static SlackBuilders instance() {
        return FACTORY;
    }

    private SlackBuildersImpl() { }

    @Override
    public SlackRequest.Builder<?> createRequestBuilder()
    {
        return new SlackRequestImpl.Builder();
    }

    @Override
    public SlackRequest.Attachment.Builder<?> createAttachmentBuilder()
    {
        return new SlackRequestImpl.Attachment.Builder();
    }

    @Override
    public SlackRequest.Field.Builder<?> createFieldBuilder()
    {
        return new SlackRequestImpl.Field.Builder();
    }
}
