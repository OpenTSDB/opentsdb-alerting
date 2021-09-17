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

package net.opentsdb.horizon.alerting.corona.processor.emitter.oc;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.bouncycastle.util.Strings;
import org.jsoup.Jsoup;

public class OcCommand {

    /* ------------ Static Class ------------ */

    @Getter
    public static class Message {

        private static final Logger LOG = LoggerFactory.getLogger(Message.class);

        private static final int MAX_LENGTH = 1600;

        private static final ThreadLocal<StringWriter> LOCAL_WRITER =
                ThreadLocal.withInitial(StringWriter::new);

        private static final PebbleTemplate MESSAGE_TEMPLATE;

        /**
         * It was agreed with the Moog team, the Horizon alerts come with
         * this customer so that they could be distinguished from the old
         * alerts.
         * <p>
         * Otherwise the field is not used on their end.
         */
        private static final String HORIZON_OC_CUSTOMER = "HORIZON";

        static {
            final PebbleEngine engine = new PebbleEngine.Builder().build();
            MESSAGE_TEMPLATE = engine.getLiteralTemplate(
                    "{{ ocCustomer }}" +
                            "|{{ appDescription }}" +
                            "|{{ hostTag }}" +
                            "|{{ ocContext }}" +
                            "|{{ messageSnippet }}" +
                            "|{{ dashboardLink }}" +
                            "|{{ opsDbProperty }}" +
                            "|{{ alertHash }}" +
                            "|{{ recovery }}" +
                            "|{{ runbookIds }}"
            );
        }

        private String appDescriptionBase;

        private String appDescriptionSubject;

        private String appDescriptionBody;

        private String hostTag;

        private String messageSnippet;

        private String dashboardLink;

        private String alertHash;

        /**
         * @return 0 - yes, 1 - no. I smell C...
         */
        private int recovery;

        // ------ From meta ------ //

        private String runbookIds;

        /* ------ From contact

        private String ocCustomer;

        private String opsDbProperty;

        private String ocContext;

        */

        /* ------------ Methods ------------ */

        private String sanitize(final String token)
        {
            if (token == null) {
                return "";
            }

            final String escaped = token
                    .replaceAll("\\|", "&#124;") // Replace pipes
                    .replaceAll("[\r\n]+", ""); // Replace newlines

            return Jsoup.parse(escaped)
                    .text()
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        private int len(final String str)
        {
            if (str == null) {
                return 0;
            }
            return str.length();
        }

        private String normalizeContext(final String context)
        {
            if (context == null || !"live".equalsIgnoreCase(context.trim())) {
                return "analysis";
            }
            return "live";
        }

        /**
         * Render the message for the given contact.
         *
         * @param contact OC contact
         * @return rendered message.
         */
        public String render(final OcContact contact)
        {

            trimToSize();

            final StringWriter writer = LOCAL_WRITER.get();
            // Reset the buffer.
            writer.getBuffer().setLength(0);
            try {
                MESSAGE_TEMPLATE.evaluate(
                        writer,
                        new HashMap<String, Object>() {{
                            put("ocCustomer", HORIZON_OC_CUSTOMER);
                            put("appDescription", buildAppDescription());
                            put("hostTag", hostTag);
                            put("ocContext", normalizeContext(contact.getContext()));
                            put("messageSnippet", messageSnippet);
                            put("dashboardLink", dashboardLink);
                            put("opsDbProperty", sanitize(contact.getOpsdbProperty()));
                            put("alertHash", alertHash);
                            put("recovery", recovery);
                            put("runbookIds", runbookIds);
                        }}
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return writer.toString();
        }

        private String buildAppDescription() {
            final StringBuilder sb = new StringBuilder();
            sb.append(appDescriptionBase);
            if (!appDescriptionSubject.isEmpty()) {
                sb.append(" subject:").append(appDescriptionSubject);
            }
            if (!appDescriptionBody.isEmpty()) {
                sb.append(" body:").append(appDescriptionBody);
            }
            return sb.toString();
        }

        private void trimToSize() {
            if (estimateLength() <= MAX_LENGTH) {
                return;
            }
            // Snip message as not really important.
            messageSnippet = "...";

            final int length = estimateLength();
            if (length <= MAX_LENGTH) {
                return;
            }

            LOG.warn("Dropping subject and body for message: {}", this);
            // Drop body and subject.
            appDescriptionBody = "";
            appDescriptionSubject = "";
        }

        private int estimateLength()
        {
            return len(HORIZON_OC_CUSTOMER)
                    + len(appDescriptionBase)
                    + len(appDescriptionSubject)
                    + len(appDescriptionBody)
                    + len(hostTag)
                    + 8 /* context analysis */
                    + len(messageSnippet)
                    + len(dashboardLink)
                    + 30 /* opsdb property max */
                    + len(alertHash)
                    + len(runbookIds)
                    + 10 /* separators */;
        }

        public Message setAppDescription(final String base,
                                         final String subject,
                                         final String body)
        {
            this.appDescriptionBase = sanitize(base);
            this.appDescriptionSubject = sanitize(subject);
            this.appDescriptionBody = sanitize(body);
            return this;
        }

        public Message setHostTag(final String hostTag)
        {
            this.hostTag = sanitize(hostTag);
            return this;
        }

        public Message setMessageSnippet(final String messageSnippet)
        {
            this.messageSnippet = sanitize(messageSnippet);
            return this;
        }

        public Message setDashboardLink(final String dashboardLink)
        {
            this.dashboardLink = sanitize(dashboardLink);
            return this;
        }

        public Message setAlertHash(final String alertHash)
        {
            this.alertHash = sanitize(alertHash);
            return this;
        }

        public Message setRecovery(final int recovery)
        {
            this.recovery = recovery;
            return this;
        }

        public Message setRunbookIds(final String runbookIds)
        {
            this.runbookIds = "";
            if (runbookIds != null) {
                this.runbookIds = String.join("|", Strings.split(runbookIds, ','));
            }
            return this;
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
            Message message = (Message) o;
            return recovery == message.recovery &&
                    Objects.equals(appDescriptionBase, message.appDescriptionBase) &&
                    Objects.equals(appDescriptionSubject, message.appDescriptionSubject) &&
                    Objects.equals(appDescriptionBody, message.appDescriptionBody) &&
                    Objects.equals(hostTag, message.hostTag) &&
                    Objects.equals(messageSnippet, message.messageSnippet) &&
                    Objects.equals(dashboardLink, message.dashboardLink) &&
                    Objects.equals(alertHash, message.alertHash) &&
                    Objects.equals(runbookIds, message.runbookIds);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(
                    appDescriptionBase,
                    appDescriptionSubject,
                    appDescriptionBody,
                    hostTag,
                    messageSnippet,
                    dashboardLink,
                    alertHash,
                    recovery,
                    runbookIds
            );
        }

        @Override
        public String toString()
        {
            return "Message{" +
                    "appDescriptionBase='" + appDescriptionBase + '\'' +
                    ", appDescriptionSubject='" + appDescriptionSubject + '\'' +
                    ", appDescriptionBody='" + appDescriptionBody + '\'' +
                    ", hostTag='" + hostTag + '\'' +
                    ", messageSnippet='" + messageSnippet + '\'' +
                    ", dashboardLink='" + dashboardLink + '\'' +
                    ", alertHash='" + alertHash + '\'' +
                    ", recovery=" + recovery +
                    ", runbookIds='" + runbookIds + '\'' +
                    '}';
        }
    }

    /* ------------ Fields ------------ */

    /**
     * Colo. [ywrmsg -C colo option]
     */
    private final String colo;

    /**
     * Property. [ywrmsg -P property option]
     */
    private final String property;

    /**
     * Host sending the alert. [ywrmsg -H host option]
     */
    private final String host;

    /**
     * Check id. [ywrmsg -K check id option]
     */
    private final String checkId;

    /**
     * Severity. [ywrmsg -I severity option]
     */
    private final int severity; // Comes from meta

    /**
     * Display count. [ywrmsg -D display count option]
     */
    // private final int displayCount; // Comes from the contact

    /**
     * Message. [ywrmsg -m message option]
     */
    private final Message message;

    /* ------------ Constructors ------------ */

    public OcCommand(final String colo,
                     final String property,
                     final String host,
                     final String checkId,
                     final int severity,
                     final Message message)
    {
        this.colo = colo;
        this.property = property;
        this.host = host;
        this.checkId = checkId;
        this.severity = severity;
        this.message = message;
    }

    public List<String> build(final OcContact contact)
    {
        final List<String> command = new ArrayList<>();
        command.add("/opt/bin/ywmsg");

        if (colo != null) {
            command.add("-C");
            command.add(colo);
        }

        if (property != null) {
            command.add("-P");
            command.add(property);
        }

        if (host != null) {
            command.add("-H");
            command.add(host);
        }

        if (checkId != null) {
            command.add("-K");
            command.add(checkId);
        }

//        TODO: This is not used in Moog.
//        if (checkId != null) {
//            command.add("-D");
//            command.add(contact.getDisplayCount());
//        }

        command.add("-I");
        command.add(String.valueOf(severity));

        command.add("-m");
        command.add(message.render(contact));

        return command;
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
        OcCommand that = (OcCommand) o;
        return severity == that.severity &&
                Objects.equals(colo, that.colo) &&
                Objects.equals(property, that.property) &&
                Objects.equals(host, that.host) &&
                Objects.equals(checkId, that.checkId) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(colo, property, host, checkId, severity, message);
    }

    @Override
    public String toString()
    {
        return "OcCommand{" +
                "colo='" + colo + '\'' +
                ", property='" + property + '\'' +
                ", host='" + host + '\'' +
                ", checkId='" + checkId + '\'' +
                ", severity=" + severity +
                ", message=" + message +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private String colo;

        private String property;

        private String host;

        private String checkId;

        private int severity;

        private Message message;

        public Builder setColo(final String colo)
        {
            this.colo = colo;
            return this;
        }

        public Builder setProperty(final String property)
        {
            this.property = property;
            return this;
        }

        public Builder setHost(final String host)
        {
            this.host = host;
            return this;
        }

        public Builder setCheckId(final String checkId)
        {
            this.checkId = checkId;
            return this;
        }

        public Builder setSeverity(final int severity)
        {
            this.severity = severity;
            return this;
        }

        public Builder setMessage(final Message message)
        {
            this.message = message;
            return this;
        }

        public OcCommand build()
        {
            return new OcCommand(
                    colo,
                    property,
                    host,
                    checkId,
                    severity,
                    message
            );
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
