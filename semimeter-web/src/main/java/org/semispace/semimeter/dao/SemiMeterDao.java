package org.semispace.semimeter.dao;

import org.semispace.semimeter.bean.GroupedResult;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.TokenizedPathInfo;

import java.util.Collection;
import java.util.List;

public interface SemiMeterDao {
    static final int MAX_PATH_LENGTH = 2048;

    public int size();


    public boolean isAlive();


    public Long sumItems(long startAt, long endAt, String path);

    public JsonResults[] performParameterizedQuery(long startAt, long endAt, String path);


    public JsonResults[] createTimeArray(String path, long endAt, long startAt, Integer numberOfSamples);


    /**
     * @param start Where to start, inclusive
     * @param end   Where to end, inclusive
     */
    public void collate(long start, long end);

    public List<GroupedResult> getGroupedSums(long startAt, long endAt, TokenizedPathInfo query, int maxResults) throws IllegalArgumentException;

    public void performInsertion(Collection<Item> items);
}
