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
package org.tightblog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.tightblog.pojos.Weblog;
import org.tightblog.pojos.WeblogEntry;

import java.util.List;

@Repository
@Transactional("transactionManager")
public interface WeblogEntryRepository extends JpaRepository<WeblogEntry, String> {

    List<WeblogEntry> findByWeblog(Weblog e);

    Long deleteByWeblog(Weblog weblog);

    WeblogEntry findByWeblogAndAnchor(Weblog weblog, String anchor);

    default WeblogEntry findByIdOrNull(String id) {
        return findById(id).orElse(null);
    }
}
