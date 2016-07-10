/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.apache.roller.weblogger.business.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comment plugin which turns plain text URLs into hyperlinks using
 * the html anchor (&lt;a&gt;) tag.
 * 
 * Based on work originally from Matthew Montgomery.
 */
public class LinkMarkupPlugin implements WeblogEntryCommentPlugin {

    private static Logger log = LoggerFactory.getLogger(LinkMarkupPlugin.class);

    private static final Pattern PATTERN = Pattern.compile(
            "http[s]?://[^/][\\S]+", Pattern.CASE_INSENSITIVE);
    
    public LinkMarkupPlugin() {
        log.debug("Instantiating LinkMarkupPlugin");
    }
    
    /**
     * Unique identifier.  This should never change. 
     */
    public String getId() {
        return "LinkMarkup";
    }
    
    /** Returns the display name of this Plugin. */
    public String getName() {
        return "Link Markup";
    }

    /** Briefly describes the function of the Plugin.  May contain HTML. */
    public String getDescription() {
        return "Automatically creates hyperlinks out of embedded URLs.";
    }

    /**
     * Apply plugin to the specified text.
     *
     * @param comment The comment being rendered.
     * @param text String to which plugin should be applied.
     *
     * @return Results of applying plugin to string.
     */
    public String render(final WeblogEntryComment comment, String text) {

        // only do this if comment is plain text (html option has anchor tags already)
        if ("text/plain".equals(comment.getContentType())) {
            StringBuilder result = new StringBuilder();

            if (text != null) {
                Matcher matcher;
                matcher = PATTERN.matcher(text);

                int start = 0;
                int end = text.length();

                while (start < end) {
                    if (matcher.find()) {
                        // Copy up to the match
                        result.append(text.substring(start, (matcher.start())));

                        // Copy the URL and create the hyperlink
                        // Unescape HTML as we don't know if that setting is on
                        String url;
                        url = StringEscapeUtils.unescapeHtml4(text.substring(
                                matcher.start(), matcher.end()));

                        // Build the anchor tag and escape HTML in the URL output
                        result.append("<a href=\"");
                        result.append(StringEscapeUtils.escapeHtml4(url));
                        result.append("\">");
                        result.append(StringEscapeUtils.escapeHtml4(url));
                        result.append("</a>");

                        // Increment the starting index
                        start = matcher.end();
                    }
                    else {
                        // Copy the remainder
                        result.append(text.substring(start, end));

                        // Increment the starting index to exit the loop
                        start = end;
                    }
                }
            }
            return result.toString();
        } else {
            return text;
        }
    }
    
}