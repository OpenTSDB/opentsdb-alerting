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

import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackResponse;

final class SlackResponceImpl implements SlackResponse {

    private static final SlackResponse OK = new SlackResponceImpl("ok", true);

    static SlackResponse ok()
    {
        return OK;
    }

    static SlackResponse error(String reason)
    {
        return new SlackResponceImpl(reason, false);
    }

    private final String message;

    private final boolean ok;

    private SlackResponceImpl(final String message,
                              final boolean ok)
    {
        this.message = message;
        this.ok = ok;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    @Override
    public boolean isOk()
    {
        return ok;
    }
}
