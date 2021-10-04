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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.AddAlertNoteRequest;
import com.ifountain.opsgenie.client.swagger.model.CloseAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.GetAlertResponse;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import com.ifountain.opsgenie.client.swagger.model.TeamRecipient;

/**
 * OpsGenie Alert API client wrapper.
 * <p>
 * The class is not thread-safe because the {@link #alertApi} client
 * requires the API key to be set before every API call.
 * <p>
 * TODO: Public API seem to be quite weird. But have no idea how to
 * improve it yet.
 */
@NotThreadSafe
public class OpsGenieClient {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(OpsGenieClient.class);

    private static final int BAD_REQUEST =
            Response.Status.BAD_REQUEST.getStatusCode();

    private static final int NOT_FOUND =
            Response.Status.NOT_FOUND.getStatusCode();

    private static final String BY_ALERT_ALIAS = "alias";

    /* ------------ Fields ------------ */

    /**
     * OpsGenie Alert API client.
     */
    private final AlertApi alertApi;

    /* ------------ Constructors ------------ */

    public OpsGenieClient()
    {
        this.alertApi =
                new com.ifountain.opsgenie.client.OpsGenieClient().alertV2();
        alertApi.getApiClient().setConnectTimeout(5_000);
        alertApi.getApiClient().getHttpClient().setReadTimeout(5_000);
    }

    /* ------------ Methods ------------ */

    /**
     * Build {@link CreateAlertRequest} from {@link OpsGenieAlert} instance.
     *
     * @param alert {@code OpsGenieAlert} instance
     * @return {@link CreateAlertRequest} instance.
     */
    private CreateAlertRequest buildCreateRequest(final OpsGenieAlert alert)
    {
        final CreateAlertRequest.PriorityEnum priority =
                CreateAlertRequest.PriorityEnum.fromValue(alert.getPriority());
        final List<String> tags = alert.getTags() == null ?
                Collections.emptyList() : Arrays.asList(alert.getTags());

        final CreateAlertRequest request = new CreateAlertRequest()
                .alias(alert.getAlias())
                .description(alert.getDescription())
                .message(alert.getMessage())
                .note(alert.getNote())
                .priority(priority)
                .source(alert.getSource())
                .tags(tags)
                .details(alert.getDetails())
                .user(alert.getUser());

        for (String visibleToTeam: alert.getVisibleToTeams()) {
            final TeamRecipient recipient = new TeamRecipient();
            recipient.setName(visibleToTeam);
            request.addVisibleToItem(recipient);
        }

        return request;
    }

    /**
     * Build {@link CloseAlertRequest} for {@link OpsGenieAlert} instance.
     *
     * @param alert {@code OpsGenieAlert} instance
     * @return {@link CloseAlertRequest} instance.
     */
    private CloseAlertRequest buildCloseRequest(final OpsGenieAlert alert)
    {
        return new CloseAlertRequest()
                .note(alert.getNote())
                .source(alert.getSource())
                .user(alert.getUser());
    }

    /**
     * Build {@link AddAlertNoteRequest} for {@link OpsGenieAlert} instance.
     *
     * @param alert {@code OpsGenieAlert} instance
     * @return {@link AddAlertNoteRequest} instance.
     */
    private AddAlertNoteRequest buildAddAlertNoteRequest(
            final OpsGenieAlert alert)
    {
        return new AddAlertNoteRequest()
                .note(alert.getNote())
                .source(alert.getSource())
                .user(alert.getUser());
    }

    /**
     * Get alert by alias.
     *
     * @param apiKey API key
     * @param alias  alert alias
     * @return {@link GetAlertResponse}
     * @throws ApiException if request failed.
     */
    private GetAlertResponse get(final String apiKey, final String alias)
            throws ApiException
    {
        alertApi.getApiClient().setApiKey(apiKey);
        return alertApi.getAlert(alias, BY_ALERT_ALIAS);
    }

    /**
     * Create a new alert.
     *
     * @param apiKey  API key
     * @param request create alert request
     * @return {@link SuccessResponse} if succeeded
     * @throws ApiException if request failed.
     */
    private SuccessResponse create(final String apiKey,
                                   final CreateAlertRequest request)
            throws ApiException
    {
        alertApi.getApiClient().setApiKey(apiKey);
        return alertApi.createAlert(request);
    }

    /**
     * Add a note to the given alert.
     *
     * @param apiKey  API key
     * @param id      OpsGenie alert id
     * @param idType  OpsGenie id type
     * @param request {@link CloseAlertRequest} instance
     * @return {@link SuccessResponse} if succeeded
     * @throws ApiException if request failed.
     */
    private SuccessResponse addNote(final String apiKey,
                                    final String id,
                                    final String idType,
                                    final AddAlertNoteRequest request)
            throws ApiException
    {
        alertApi.getApiClient().setApiKey(apiKey);
        return alertApi.addNote(id, request, idType);
    }

    /**
     * Close the alert.
     *
     * @param apiKey  API key
     * @param id      OpsGenie alert id
     * @param idType  OpsGenie id type
     * @param request {@link CloseAlertRequest} instance
     * @return {@link SuccessResponse} if succeeded
     * @throws ApiException if request failed.
     */
    private SuccessResponse close(final String apiKey,
                                  final String id,
                                  final String idType,
                                  final CloseAlertRequest request)
            throws ApiException
    {
        alertApi.getApiClient().setApiKey(apiKey);
        return alertApi.closeAlert(id, idType, request);
    }

    /**
     * Perform null check.
     *
     * @param apiKey API key
     * @param alert  alert
     */
    private void requireNonNull(final String apiKey, final OpsGenieAlert alert)
    {
        Objects.requireNonNull(apiKey, "apiKey cannot be null");
        Objects.requireNonNull(alert, "alert cannot be null");
    }

    /**
     * Create an alert.
     *
     * @param apiKey API key
     * @param alert  alert to create
     * @return true if success, false otherwise.
     */
    public boolean create(final String apiKey, final OpsGenieAlert alert)
    {
        requireNonNull(apiKey, alert);
        try {
            final SuccessResponse response = create(apiKey, buildCreateRequest(alert));
            LOG.info("Created alert in OpsGenie: alias={}, namespace={}, response={}, message={}",
                alert.getAlias(), alert.getNamespace(), response, alert.getMessage());
            return true;
        } catch (ApiException e) {
            LOG.error("Creating alert failed: alias={}, namespace={}",
                    alert.getAlias(), alert.getNamespace(), e);
            return false;
        }
    }

    /**
     * Add a note to the alert.
     *
     * @param apiKey API key
     * @param alert  alert to add note to
     * @return true if success, false otherwise.
     */
    public boolean addNote(final String apiKey, final OpsGenieAlert alert)
    {
        requireNonNull(apiKey, alert);
        try {
            final AddAlertNoteRequest request = buildAddAlertNoteRequest(alert);
            final SuccessResponse response = addNote(apiKey, alert.getAlias(), BY_ALERT_ALIAS, request);
            LOG.info("Updated notes in OpsGenie: alias={}, namespace={}, response={}, message={}",
                    alert.getAlias(), alert.getNamespace(), response, alert.getMessage());
            return true;
        } catch (ApiException e) {
            LOG.error("Adding note to alert failed: alias={}, namespace={}",
                    alert.getAlias(), alert.getNamespace(), e);
            return false;
        }
    }

    /**
     * Close the alert.
     *
     * @param apiKey API key
     * @param alert  alert to close
     * @return true if success, false otherwise.
     */
    public boolean close(final String apiKey, final OpsGenieAlert alert)
    {
        requireNonNull(apiKey, alert);
        try {
            final CloseAlertRequest request = buildCloseRequest(alert);
            final SuccessResponse response = close(apiKey, alert.getAlias(), BY_ALERT_ALIAS, request);
            LOG.info("Closed alert and updated notes in OpsGenie: alias={}, namespace={}, response={}, message={}",
                    alert.getAlias(), alert.getNamespace(), response, alert.getMessage());
            return true;
        } catch (ApiException e) {
            LOG.error("Closing alert failed: alias={}, namespace={}",
                    alert.getAlias(), alert.getNamespace(), e);
            return false;
        }
    }

    /**
     * Checks if the alert is still active in OpsGenie.
     * According to the API: https://docs.opsgenie.com/docs/response.
     *
     * @param apiKey API key
     * @param alert  alert to check
     * @return {@code Optional} with true if still active, false if closed,
     * empty if could not determine.
     */
    public Optional<Boolean> isActive(final String apiKey,
                                      final OpsGenieAlert alert)
    {
        requireNonNull(apiKey, alert);
        try {
            // Expect ApiException if the alert doesn't exist.
            final GetAlertResponse response = get(apiKey, alert.getAlias());
            LOG.info("Got alert information in OpsGenie: alias={}, namespace={}, response={}",
                    alert.getAlias(), alert.getNamespace(), response);

            // Report closed alerts as inactive.
            if ("closed".equalsIgnoreCase(response.getData().getStatus())) {
                return Optional.of(Boolean.FALSE);
            }

            return Optional.of(Boolean.TRUE);
        } catch (ApiException e) {
            if (e.getCode() == NOT_FOUND) {
                return Optional.of(Boolean.FALSE);
            }
            LOG.error("Failed to get alert info: alias={}", alert.getAlias(), e);
        }
        return Optional.empty();
    }
}
