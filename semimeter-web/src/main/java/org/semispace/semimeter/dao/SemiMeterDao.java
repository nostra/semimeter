/*
 * Copyright 2009 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semispace.semimeter.dao;

import org.semispace.semimeter.bean.GroupedResult;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.TokenizedPathInfo;

import java.util.Collection;
import java.util.List;

public interface SemiMeterDao {

    /**
     * @return size of stalegroup table, or -1 if any errors occur.
     */
    public int size();

    public boolean isAlive();

    public void performInsertion(Collection<Item> items);

    public Long sumItems(long startAt, long endAt, String path);

    public JsonResults[] performParameterizedQuery(long startAt, long endAt, String path);

    /**
     * @param numberOfSamples Presuming number to be positive
     */
    public JsonResults[] createTimeArray(String path, long endAt, long startAt, Integer numberOfSamples);

    public List<GroupedResult> getGroupedSums(long startAt, long endAt, TokenizedPathInfo query, int maxResults)
            throws IllegalArgumentException;

    public List<GroupedResult> getHourlySums(Integer publicationId, Integer sectionId);

    public void deleteEntriesOlderThanMillis(final long millis);
}
