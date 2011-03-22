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

package org.semispace.semimeter.bean;

/**
 */
public class ParameterizedQuery {
    private Long startAt;
    private Long endAt;
    private String path;
    private String key;

    public ParameterizedQuery(String resolution, long startAt, long endAt, String path) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.path = path;
        this.key = path + "_" + resolution;
    }

    public ParameterizedQuery() {
        // Intentionally empty
    }

    public Long getStartAt() {
        return startAt;
    }

    public Long getEndAt() {
        return endAt;
    }

    public String getPath() {
        return path;
    }

    public String getKey() {
        return key;
    }

}
