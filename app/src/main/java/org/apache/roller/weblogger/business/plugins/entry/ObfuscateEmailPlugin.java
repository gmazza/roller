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

package org.apache.roller.weblogger.business.plugins.entry;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.Weblog;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Obfuscate email addresses in entry text.
 */
public class ObfuscateEmailPlugin implements WeblogEntryPlugin {

    private static Pattern MAILTO_PATTERN =
            Pattern.compile("mailto:([a-zA-Z0-9\\.\\-]+@[a-zA-Z0-9\\.\\-]+\\.[a-zA-Z0-9]+)");

    private static Pattern EMAIL_PATTERN =
            Pattern.compile("\\b[a-zA-Z0-9\\.\\-]+(@)([a-zA-Z0-9\\.\\-]+)(\\.)([a-zA-Z0-9]+)\\b");

    private static Log mLogger = LogFactory.getLog(ObfuscateEmailPlugin.class);
    
    protected String name = "Email Scrambler";
    
    protected String description = "Automatically converts email addresses " +
            "to me-AT-mail-DOT-com format.  Also &quot;scrambles&quot; mailto: links.";
    
    
    public ObfuscateEmailPlugin() {
        mLogger.debug("ObfuscateEmailPlugin instantiated.");
    }
    
    
    public String getName() {
        return name;
    }
    
    
    public String getDescription() {
        return StringEscapeUtils.escapeEcmaScript(description);
    }
    
    
    public void init(Weblog website) throws WebloggerException {}
    
    
    public String render(WeblogEntry entry, String str) {
        return encodeEmail(str);
    }

    protected String encodeEmail(String str) {
        // obfuscate mailto's: turns them into hex encoded,
        // so that browsers can still understand the mailto link
        Matcher mailtoMatch = MAILTO_PATTERN.matcher(str);
        while (mailtoMatch.find()) {
            String email = mailtoMatch.group(1);
            //System.out.println("email=" + email);
            String hexed = encode(email);
            str = str.replaceFirst("mailto:"+email, "mailto:"+hexed);
        }

        return obfuscateEmail(str);
    }


    /**
     * obfuscate plaintext emails: makes them
     * "human-readable" - still too easy for
     * machines to parse however.
     */
    private String obfuscateEmail(String str) {
        Matcher emailMatch = EMAIL_PATTERN.matcher(str);
        while (emailMatch.find()) {
            String at = emailMatch.group(1);
            //System.out.println("at=" + at);
            str = str.replaceFirst(at, "-AT-");

            String dot = emailMatch.group(2) + emailMatch.group(3) + emailMatch.group(4);
            String newDot = emailMatch.group(2) + "-DOT-" + emailMatch.group(4);
            //System.out.println("dot=" + dot);
            str = str.replaceFirst(dot, newDot);
        }
        return str;
    }


    /**
     * Thanks to the folks at Blojsom (http://sf.net/projects/blojsom)
     * for showing me what I was doing wrong with the Hex class.
     *
     * @param email email to encode
     * @return encoded email string
     */
    private String encode(String email) {
        StringBuilder result = new StringBuilder();
        try {
            char[] hexString = Hex.encodeHex(email.getBytes("UTF-8"));
            for (int i = 0; i < hexString.length; i++) {
                if (i % 2 == 0) {
                    result.append("%");
                }
                result.append(hexString[i]);
            }
        } catch (UnsupportedEncodingException e) {
            return email;
        }

        return result.toString();
    }
}
