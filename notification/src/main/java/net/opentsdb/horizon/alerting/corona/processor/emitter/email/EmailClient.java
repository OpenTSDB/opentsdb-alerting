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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jsoup.Jsoup;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;

public class EmailClient {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(EmailClient.class);

    /* ------------ Fields ------------ */

    private final String smtpHost;

    private final int connectionTimeoutMs;

    /* ------------ Constructor ------------ */

    public EmailClient(final String smtpHost, final int connectionTimeoutMs)
    {
        this.smtpHost = smtpHost;
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    /* ------------ Methods ------------ */

    private List<MimeBodyPart> getImageMimes(final Map<String, byte[]> images)
    {
        return images.entrySet().stream()
                .map(entry -> {
                    final MimeBodyPart img = new MimeBodyPart();
                    final ByteArrayDataSource ds =
                            new ByteArrayDataSource(entry.getValue(),
                                    "image/png"
                            );
                    try {
                        img.setDataHandler(new DataHandler(ds));
                        img.setContentID("<" + entry.getKey() + ">");
                        img.setDisposition(Part.INLINE);
                        return img;
                    } catch (Exception e) {
                        LOG.error("Couldn't attach image {}", entry.getKey());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MultiPartEmail buildEmail(final EmailMessage message,
                                      final String... to)
            throws EmailException
    {
        final MultiPartEmail email = new MultiPartEmail();
        email.setHostName(smtpHost);
        email.setSubject(message.getSubject())
                .setFrom(message.getFrom(), message.getFromAlias())
                .addTo(to);

        final String htmlBody = message.getBody();
        final String textBody = Jsoup.parse(htmlBody).text();

        final MimeMultipart mainPart = new MimeMultipart("related");
        try {
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=utf-8");

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(textBody);

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(multipart);

            mainPart.addBodyPart(mimeBodyPart);

            List<MimeBodyPart> mimes = getImageMimes(message.getImages());
            for (MimeBodyPart img : mimes) {
                mainPart.addBodyPart(img);
            }
        } catch (MessagingException e) {
            throw new EmailException(
                    "Failed to build email for " + Arrays.toString(to), e);
        }

        email.addPart(mainPart);
        return email;
    }

    public void send(final EmailMessage message, final String... to)
            throws EmailException
    {
        final MultiPartEmail email = buildEmail(message, to);
        email.setSocketConnectionTimeout(connectionTimeoutMs);

        // TODO: Look up if the connection is reestablished on every send.
        // https://stackoverflow.com/questions/6694975/apache-commons-email-and-reusing-smtp-connections
        email.send();
    }
}

