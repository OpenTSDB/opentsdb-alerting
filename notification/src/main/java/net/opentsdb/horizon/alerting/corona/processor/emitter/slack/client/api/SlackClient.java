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

public interface SlackClient {

    /**
     * Sends request to the given endpoint and returns a response.
     *
     * All expected errors should be returned as part of response.
     * Expected errors are those which are specified in
     * https://api.slack.com/messaging/webhooks#handling_errors
     *
     * Unexpected error should be turned into {@code SlackException}.
     * E.g. deserialization exception.
     *
     * @param request Slack request to send
     * @param endpoint URL to post the payload to
     * @return Slack response
     *
     * @throws SlackException on unexpected error occurrence.
     */
    SlackResponse send(final SlackRequest request, final String endpoint);
}
