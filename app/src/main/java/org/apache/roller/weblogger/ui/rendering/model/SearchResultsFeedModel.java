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
package org.apache.roller.weblogger.ui.rendering.model;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringEscapeUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.URLStrategy;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.search.FieldConstants;
import org.apache.roller.weblogger.business.search.IndexManager;
import org.apache.roller.weblogger.business.search.operations.SearchOperation;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.wrapper.WeblogCategoryWrapper;
import org.apache.roller.weblogger.pojos.wrapper.WeblogEntryWrapper;
import org.apache.roller.weblogger.pojos.wrapper.WeblogWrapper;
import org.apache.roller.weblogger.ui.rendering.pagers.Pager;
import org.apache.roller.weblogger.ui.rendering.pagers.SearchResultsFeedPager;
import org.apache.roller.weblogger.ui.rendering.util.WeblogFeedRequest;
import org.apache.roller.weblogger.ui.rendering.util.WeblogRequest;
import org.apache.roller.weblogger.util.Utilities;

/**
 * Extends normal page renderer model to represent search results for Atom
 * feeds.
 * 
 * Also adds some new methods which are specific only to search results.
 */
public class SearchResultsFeedModel implements Model {

	private WeblogFeedRequest feedRequest;
	private URLStrategy urlStrategy;
	private Weblog weblog;
    private WeblogEntryManager weblogEntryManager;
    private IndexManager indexManager;

	// the pager used by the 3.0+ rendering system
	private SearchResultsFeedPager pager = null;

	private List<WeblogEntryWrapper> results = new LinkedList<>();

	private Set categories = new TreeSet();

	private boolean websiteSpecificSearch = true;

	private int hits = 0;
	private int offset = 0;
	private int limit = 0;

	private int entryCount = 0;

    public void setUrlStrategy(URLStrategy urlStrategy) {
        this.urlStrategy = urlStrategy;
    }

    public void setWeblogEntryManager(WeblogEntryManager weblogEntryManager) {
        this.weblogEntryManager = weblogEntryManager;
    }

    public void setIndexManager(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    public String getModelName() {
		return "model";
	}

	public void init(Map initData) throws WebloggerException {

		// we expect the init data to contain a weblogRequest object
		WeblogRequest weblogRequest = (WeblogRequest) initData
				.get("parsedRequest");
		if (weblogRequest == null) {
			throw new WebloggerException(
					"expected weblogRequest from init data");
		}

		if (weblogRequest instanceof WeblogFeedRequest) {
			this.feedRequest = (WeblogFeedRequest) weblogRequest;
		} else {
			throw new WebloggerException(
					"weblogRequest is not a WeblogFeedRequest."
							+ "  FeedModel only supports feed requests.");
		}

		// extract weblog object
		weblog = feedRequest.getWeblog();

		String pagerUrl = urlStrategy.getWeblogFeedURL(weblog,
				feedRequest.getType(),
				// cat and term below null but added to URL in pager
                feedRequest.getFormat(), null, null,
				null, false, true);

		// if there is no query, then we are done
		if (feedRequest.getTerm() == null) {
			pager = new SearchResultsFeedPager(urlStrategy, pagerUrl,
					feedRequest.getPage(), feedRequest, results, false);
			return;
		}

		this.entryCount = WebloggerRuntimeConfig
				.getIntProperty("site.newsfeeds.defaultEntries");

		// setup the search
		SearchOperation search = new SearchOperation(indexManager);
		search.setTerm(feedRequest.getTerm());

		if (WebloggerRuntimeConfig.isSiteWideWeblog(feedRequest
				.getWeblogHandle())) {
			this.websiteSpecificSearch = false;
		} else {
			search.setWebsiteHandle(feedRequest.getWeblogHandle());
		}

		if (StringUtils.isNotEmpty(feedRequest.getWeblogCategoryName())) {
			search.setCategory(feedRequest.getWeblogCategoryName());
		}

		// execute search
        indexManager.executeIndexOperationNow(search);

		if (search.getResultsCount() > -1) {

			TopFieldDocs docs = search.getResults();
			ScoreDoc[] hitsArr = docs.scoreDocs;
			this.hits = search.getResultsCount();

			// Convert the Hits into WeblogEntryData instances.
			convertHitsToEntries(hitsArr, search);
		}

		// search completed, setup pager based on results
		pager = new SearchResultsFeedPager(urlStrategy, pagerUrl,
				feedRequest.getPage(), feedRequest, results,
				(hits > (offset + limit)));
	}

	public Pager getSearchResultsPager() {
		return pager;
	}

	/**
	 * Convert hits to entries.
	 * 
	 * @param hits
	 *            the hits
	 * @param search
	 *            the search
	 * @throws WebloggerException
	 *             the weblogger exception
	 */
	private void convertHitsToEntries(ScoreDoc[] hits, SearchOperation search)
			throws WebloggerException {

		// determine offset
		this.offset = feedRequest.getPage() * this.entryCount;
		if (this.offset >= hits.length) {
			this.offset = 0;
		}

		// determine limit
		this.limit = this.entryCount;
		if (this.offset + this.limit > hits.length) {
			this.limit = hits.length - this.offset;
		}

		try {
			TreeSet<String> categorySet = new TreeSet<>();

			WeblogEntry entry;
			Document doc;
			String handle;
			Timestamp now = new Timestamp(new Date().getTime());
			for (int i = offset; i < offset + limit; i++) {
				doc = search.getSearcher().doc(hits[i].doc);
				handle = doc.getField(FieldConstants.WEBSITE_HANDLE)
						.stringValue();

                entry = weblogEntryManager.getWeblogEntry(doc.getField(
                        FieldConstants.ID).stringValue());

				if (!(websiteSpecificSearch && handle.equals(feedRequest.getWeblogHandle()))
                        && doc.getField(FieldConstants.CATEGORY) != null) {
                    categorySet.add(doc.getField(FieldConstants.CATEGORY).stringValue());
				}

				// maybe null if search result returned inactive user
				// or entry's user is not the requested user.
				// but don't return future posts
				if (entry != null && entry.getPubTime().before(now)) {
					results.add(WeblogEntryWrapper.wrap(entry, urlStrategy));
				}
			}

			if (categorySet.size() > 0) {
				this.categories = categorySet;
			}
		} catch (IOException e) {
			throw new WebloggerException(e);
		}
	}

	/**
	 * Get weblog being displayed.
	 */
	public WeblogWrapper getWeblog() {
		return WeblogWrapper.wrap(weblog, urlStrategy);
	}

	public String getTerm() {
		String query =feedRequest.getTerm() ;
		return (query == null) 
			? "" : StringEscapeUtils.escapeXml(Utilities.escapeHTML(query));
	}

	public int getHits() {
		return hits;
	}

	public int getOffset() {
		return offset;
	}

	public int getPage() {
		return feedRequest.getPage();
	}

	public int getLimit() {
		return limit;
	}

	public List getResults() {
		return results;
	}

	public Set getCategories() {
		return categories;
	}

	public String getCategoryName() {
		return feedRequest.getWeblogCategoryName();
	}

	public WeblogCategoryWrapper getWeblogCategory() {
		if (feedRequest.getWeblogCategory() != null) {
			return WeblogCategoryWrapper.wrap(feedRequest.getWeblogCategory(),
					urlStrategy);
		}
		return null;
	}
}
