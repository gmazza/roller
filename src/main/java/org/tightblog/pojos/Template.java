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
package org.tightblog.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Instant;
import java.util.Map;

/**
 * The Template interface represents the abstract concept of a single unit
 * of templated or non-rendered content.  For TightBlog we mainly think of
 * templates as Thymeleaf templates which are meant to be fed into the
 * Thymeleaf rendering engine.
 */
public interface Template {

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    enum ComponentType {
        WEBLOG("Weblog", "text/html", true, true, true, "template.weblog.description"),
        PERMALINK("Permalink", "text/html", true, true, true, "template.permalink.description"),
        SEARCH_RESULTS("Search Results", "text/html", true, true, true,
                "template.search.description"),
        STYLESHEET("Stylesheet", "text/css", false, false, true, "template.stylesheet.description"),
        JAVASCRIPT("JavaScript file", "application/javascript", false, false,
                true, "template.javascript.description"),
        ATOMFEED("Atom Feed", "application/atom+xml; charset=utf-8", false, true,
                false, "template.atomFeed.description"),
        CUSTOM_INTERNAL("Custom internal", "text/html", false, false,
                true, "template.customInternal.description"),
        CUSTOM_EXTERNAL("Custom external", "text/html", false, true,
                true, "template.customExternal.description");

        // fromObject() allows for enum deserialization (used with front-end template saves)
        // see https://github.com/FasterXML/jackson-databind/issues/158#issuecomment-13092598
        @JsonCreator
        public static ComponentType fromObject(Map<String, Object> data) {
            return ComponentType.valueOf((String) data.get("name"));
        }

        private final String readableName;

        private final String contentType;

        private final boolean singleton;

        private final String descriptionProperty;

        private final boolean incrementsHitCount;

        private final boolean blogComponent;

        ComponentType(String readableName, String contentType, boolean singleton, boolean incrementsHitCount,
                      boolean blogComponent, String descriptionProperty) {
            this.readableName = readableName;
            this.contentType = contentType;
            this.singleton = singleton;
            this.incrementsHitCount = incrementsHitCount;
            this.blogComponent = blogComponent;
            this.descriptionProperty = descriptionProperty;
        }

        public String getReadableName() {
            return readableName;
        }

        public String getContentType() {
            return contentType;
        }

        public boolean isSingleton() {
            return singleton;
        }

        public boolean isAccessibleViaUrl() {
            return !singleton && !CUSTOM_INTERNAL.equals(this);
        }

        public String getDescriptionProperty() {
            return descriptionProperty;
        }

        public boolean isIncrementsHitCount() {
            return incrementsHitCount;
        }

        public boolean isBlogComponent() {
            return blogComponent;
        }

        // so JSON will serialize name
        public String getName() {
            return name();
        }

    }

    /**
     * The template derivation provides the background for this template, useful for doing validation
     * during template customization.  Enum values:
     * SHARED - file-based only, the template came from a shared theme and was not overridden by the user
     * during template customization.
     * OVERRIDDEN - A database-stored template that overrides one provided by a shared theme.
     * SPECIFICBLOG - A database-stored template that does not override a shared template.  It is defined
     * for a single blog only.
     */
    enum TemplateDerivation {
        SHARED("Default"),
        OVERRIDDEN("Override"),
        SPECIFICBLOG("Blog-Only");

        private final String readableName;

        TemplateDerivation(String readableName) {
            this.readableName = readableName;
        }

        @JsonValue
        public String getReadableName() {
            return readableName;
        }
    }

    /**
     * The unique identifier for this Template.
     */
    String getId();

    /**
     * A simple name for this Template.
     */
    String getName();

    /**
     * A description of the contents of this Template.
     */
    String getDescription();

    /**
     * The last time the template was modified.
     */
    Instant getLastModified();

    /**
     * The role this template performs.
     */
    ComponentType getRole();

    /**
     * The derivation of this template.
     */
    TemplateDerivation getDerivation();

    /**
     * The relative path for this Template to add to the default page URL
     * to view the template from the browser providing it is not hidden.
     * Can be null or empty if hidden.
     */
    String getRelativePath();

    String getTemplate();

    void setTemplate(String template);

}
