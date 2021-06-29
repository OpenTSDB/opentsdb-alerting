/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.processor.notification;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class EmailClient {

    private static final String DEFAULT_FROM = "opentsdb-do-not-reply@opentsdb.net";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String smtpHost;

    private String from;

    private String alias;

    public EmailClient(String smtpHost) {
        this(smtpHost, DEFAULT_FROM);
    }

    public EmailClient(String smtpHost, String from) {
        this(smtpHost, from, null);
    }

    public EmailClient(String smtpHost, String from, String alias) {
        this.smtpHost = smtpHost;
        this.from = from;
        this.alias = alias;
    }

    public void send(String subject, String htmlBody, List<String> addresses) throws EmailException {
        send(subject, htmlBody, (String[]) addresses.toArray(new String[addresses.size()]));
    }

    public void send(String subject, String htmlBody, String... addresses) throws EmailException {
        MultiPartEmail multiPartEmail = createMultipartEmail();
        multiPartEmail.addTo(addresses);
        multiPartEmail.setFrom(from, alias);

        logger.debug("Email html body: " + htmlBody);

        String textBody = Jsoup.parse(htmlBody).text();
        logger.debug("Email text body: " + textBody);

        MimeMultipart mainMultipart = new MimeMultipart("related");

        try {

            MimeBodyPart htmlBodyPart = new MimeBodyPart();
            htmlBodyPart.setContent(htmlBody, "text/html; charset=utf-8");

            MimeBodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setText(textBody);

            Multipart htmlAndTextMultipart = new MimeMultipart("alternative");
            htmlAndTextMultipart.addBodyPart(textBodyPart);
            htmlAndTextMultipart.addBodyPart(htmlBodyPart);

            MimeBodyPart htmlAndTextBodyPart = new MimeBodyPart();
            htmlAndTextBodyPart.setContent(htmlAndTextMultipart);

            mainMultipart.addBodyPart(htmlAndTextBodyPart);
        } catch (MessagingException exception) {
            throw new EmailException("Error setting email body parts for " + addresses, exception);
        }

        multiPartEmail.addPart(mainMultipart);
        multiPartEmail.setSubject(subject);
        multiPartEmail.send();
    }

    @VisibleForTesting
    MultiPartEmail createMultipartEmail() throws EmailException {
        MultiPartEmail multiPartEmail = new MultiPartEmail();
        multiPartEmail.setHostName(smtpHost);
        multiPartEmail.setFrom(from);
        return multiPartEmail;
    }

}
