/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tightblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Instant;

@ConfigurationProperties(prefix = "site")
public class DynamicProperties {

    /**
     * System-wide holder for our Application URL
     * e.g., https://www.mycompany.com/tightblog
     * If not explicitly defined, as a fallback can
     * be calculated from first incoming HTTP request.
     */
    private String absoluteUrl;

    /**
     * An indicator of whether the TightBlog database tables
     * have been created and are ready for read/write usage.
     * Helpful for startup tasks that require a functioning
     * database before they may proceed.
     */
    private boolean databaseReady;

    /**
     * Indicates the latest time that any blog incurred a change.
     * Primarily useful for caching of any site-wide (cross-blog) data
     * to indicate when that data may need refreshing.
     */
    private Instant lastSitewideChange = Instant.now();

    public String getAbsoluteUrl() {
        return absoluteUrl;
    }

    public void setAbsoluteUrl(String absoluteUrl) {
        this.absoluteUrl = absoluteUrl;
    }

    public boolean isDatabaseReady() {
        return databaseReady;
    }

    public void setDatabaseReady(boolean databaseReady) {
        this.databaseReady = databaseReady;
    }

    public Instant getLastSitewideChange() {
        return lastSitewideChange;
    }

    public void setLastSitewideChange(Instant lastSitewideChange) {
        this.lastSitewideChange = lastSitewideChange;
    }

    public void updateLastSitewideChange() {
        setLastSitewideChange(Instant.now());
    }
}
