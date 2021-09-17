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

package net.opentsdb.horizon.alerting.corona.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class AbstractParser<O> implements Parser<O> {

    private static final Logger LOG =
            LoggerFactory.getLogger(AbstractParser.class);

    /**
     * Parse the given JSON node in a list of elements.
     *
     * @param node   JSON node to parse
     * @param parser single list element parser
     * @param quiet  flag to ignore parsing exceptions if true
     * @param <E>    parser result type
     * @return parsed element.
     */
    protected static <E> List<E> parseList(final JsonNode node,
                                           final Parser<E> parser,
                                           final boolean quiet)
    {
        if (!node.isArray()) {
            if (quiet) {
                LOG.error("Expected an array, got: {}", node);
                return Collections.emptyList();
            } else {
                throw new ParserException("Expected an array, got: " + node);
            }
        }

        final List<E> entries = new ArrayList<>();
        for (final JsonNode n : node) {
            final E elem;
            try {
                elem = parser.parse(n);
            } catch (ParserException pe) {
                if (quiet) {
                    LOG.error("Failed to parse node: " + n, pe);
                    continue;
                } else {
                    throw new ParserException(pe);
                }
            }
            entries.add(elem);
        }
        return entries;
    }

    /**
     * Subclasses must override this method for parsing.
     * <p>
     * Any exception thrown from this method will be properly wrapped into
     * {@link ParserException}.
     *
     * @param root {@link JsonNode} to parser
     * @return parsed object
     */
    protected abstract O doParse(JsonNode root);

    @Override
    public final O parse(JsonNode root)
    {
        try {
            return doParse(root);
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException(e);
        }
    }
}
