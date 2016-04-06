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

package org.apache.roller.weblogger.business;

import java.util.Collection;
import java.util.Date;

import org.apache.roller.weblogger.pojos.Planet;
import org.apache.roller.weblogger.pojos.Subscription;
import org.apache.roller.weblogger.WebloggerException;


/**
 * A FeedManager is responsible for actually pulling subscriptions from
 * their source and transforming them into Planet Subscriptions and Entries,
 * as well as saving them to the DB.
 */
public interface FeedManager {
    
    /**
     * Fetch a single subscription.
     *
     * This method takes in a feed url and is expected to fetch that feed and
     * return a transient instance of a Subscription representing the
     * given feed.
     *
     * It is important to understand that this method will *NOT* return a 
     * persistent version of an existing Subscription if it happens to
     * exist.  This method is only here to pull feeds from their source 
     * so that they may be used in any way desired by the rest of the system.
     *
     * @param feedURL The feed url to use when fetching the subscription.
     * @return Subscription The fetched subscription.
     * @throws WebloggerException If there is an error fetching the subscription.
     */
    Subscription fetchSubscription(String feedURL) throws WebloggerException;
    
    
    /**
     * Conditionally fetch a single subscription.
     *
     * This method takes in a feed url and its known last modified date and should
     * return a transient Subscription for the feed only if the given feed has
     * been updated since the lastModified date.  This method is meant provide
     * a more efficient way to fetch subscriptions which are being updated so
     * subscriptions are not continually fetched when unnecessary.
     *
     * It is important to understand that this method will *NOT* return a 
     * persistent version of an existing Subscription if it happens to
     * exist.  This method is only here to pull feeds from their source 
     * so that they may be used in any way desired by the rest of the system.
     *
     * @param feedURL The feed url to use when fetching the subscription.
     * @return Subscription The fetched subscription.
     * @throws WebloggerException If there is an error fetching the subscription.
     */
    Subscription fetchSubscription(String feedURL, Date lastModified) throws WebloggerException;

    /**
     * Update a set of subscriptions in the system
     *
     * This method takes in an set of Subscriptions and updates each one
     * with the data from the its source after fetching an updated version
     * of the subscription.
     *
     * @param subscriptions A set of subscriptions to be updated
     */
    void updateSubscriptions(Collection<Subscription> subscriptions);

    /**
     * Update all Subscriptions that are part of the specified planet.
     *
     * @throws WebloggerException If there is an error during the update and the operation cannot continue.
     */
    void updateSubscriptions(Planet planet) throws WebloggerException;
}