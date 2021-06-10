/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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
package org.tightblog.bloggerui.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.tightblog.rendering.service.CommentSpamChecker;
import org.tightblog.service.LuceneIndexer;
import org.tightblog.domain.Weblog;
import org.tightblog.domain.WebloggerProperties;
import org.tightblog.rendering.cache.LazyExpiringCache;
import org.tightblog.dao.WeblogDao;
import org.tightblog.dao.WebloggerPropertiesDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Controller for weblogger backend tasks, e.g., cache and system runtime configuration.
 * For message resolution, Locale is not used for Admin actions to force messages to English
 * (Only sysadmins have access to this functionality and keeping message strings to one
 * language helps in searching issues on the Web.)
 */
@RestController
@RequestMapping(path = "/tb-ui/admin/rest/server")
public class AdminController {

    private final static Logger log = LoggerFactory.getLogger(AdminController.class);

    private final Set<LazyExpiringCache> cacheSet;
    private final LuceneIndexer luceneIndexer;
    private final CommentSpamChecker commentValidator;
    private final WeblogDao weblogDao;
    private final WebloggerPropertiesDao webloggerPropertiesDao;
    private final boolean searchEnabled;

    @Autowired
    public AdminController(Set<LazyExpiringCache> cacheSet, LuceneIndexer luceneIndexer,
                           CommentSpamChecker commentValidator, WeblogDao weblogDao,
                           @Value("${search.enabled:false}") boolean searchEnabled,
                           WebloggerPropertiesDao webloggerPropertiesDao) {
        this.cacheSet = cacheSet;
        this.luceneIndexer = luceneIndexer;
        this.commentValidator = commentValidator;
        this.weblogDao = weblogDao;
        this.webloggerPropertiesDao = webloggerPropertiesDao;
        this.searchEnabled = searchEnabled;
    }

    @GetMapping(value = "/caches")
    public List<LazyExpiringCache> getCacheData() {
        return new ArrayList<>(cacheSet);
    }

    @PostMapping(value = "/cache/{cacheName}/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void emptyOneCache(@PathVariable String cacheName) {
        Optional<LazyExpiringCache> maybeCache = cacheSet.stream()
                .filter(c -> c.getCacheHandlerId().equalsIgnoreCase(cacheName)).findFirst();
        maybeCache.ifPresent(LazyExpiringCache::invalidateAll);
    }

    @PostMapping(value = "/resethitcount")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetHitCount() {
        weblogDao.resetDailyHitCounts();
        log.info("daily hit counts manually reset by administrator");
    }

    @GetMapping(value = "/visibleWeblogHandles")
    public List<String> getVisibleWeblogHandles() {
        List<String> weblogHandles = new ArrayList<>();
        List<Weblog> weblogs = weblogDao.findByVisibleTrueOrderByHandle(Pageable.unpaged());
        for (Weblog weblog : weblogs) {
            weblogHandles.add(weblog.getHandle());
        }
        return weblogHandles;
    }

    @GetMapping(value = "/searchenabled")
    public boolean getSearchEnabled() {
        return searchEnabled;
    }

    @PostMapping(value = "/weblog/{handle}/rebuildindex")
    public ResponseEntity<?> rebuildIndex(@PathVariable String handle) {
        Weblog weblog = weblogDao.findByHandle(handle);
        if (weblog != null) {
            luceneIndexer.updateIndex(weblog, false);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/webloggerproperties")
    public WebloggerProperties getWebloggerProperties() {
        WebloggerProperties wp = webloggerPropertiesDao.findOrNull();
        if (wp.getMainBlog() != null) {
            wp.setMainBlogId(wp.getMainBlog().getId());
        }
        return wp;
    }

    @PostMapping(value = "/webloggerproperties")
    public void updateProperties(@Valid @RequestBody WebloggerProperties properties) {
        Weblog mainBlog = Optional.ofNullable(properties.getMainBlogId()).map(weblogDao::findByIdOrNull).orElse(null);
        properties.setMainBlog(mainBlog);
        webloggerPropertiesDao.saveAndFlush(properties);
        commentValidator.refreshGlobalBlacklist();
    }

    @GetMapping(value = "/weblogmap")
    public Map<String, String> getWeblogIdToHandleMap() {
        Page<Weblog> weblogs = weblogDao.findAll(Pageable.unpaged());

        Map<String, String> weblogIdToHandleMap = new HashMap<>();
        weblogs.forEach(w -> weblogIdToHandleMap.put(w.getId(), w.getHandle()));

        return weblogIdToHandleMap;
    }

}
