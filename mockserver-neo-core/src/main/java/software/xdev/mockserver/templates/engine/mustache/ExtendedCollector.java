/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.mockserver.templates.engine.mustache;

import com.samskivert.mustache.DefaultCollector;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.util.Map;

public class ExtendedCollector extends DefaultCollector {

    @Override
    public Mustache.VariableFetcher createFetcher(Object ctx, String name) {
        if (ctx instanceof Map<?, ?>) {
            return EXTENDED_MAP_FETCHER;
        }
        return super.createFetcher(ctx, name);
    }

    protected static final Mustache.VariableFetcher EXTENDED_MAP_FETCHER = new Mustache.VariableFetcher() {
        public Object get(Object ctx, String name) {
            if (ctx instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) ctx;
                if (map.containsKey(name)) {
                    return map.get(name);
                }
                // map entries, values and keys to be iterated over
                if ("entrySet".equals(name)) {
                    return map.entrySet();
                }
                if ("values".equals(name)) {
                    return map.values();
                }
                if ("keySet".equals(name)) {
                    return map.keySet();
                }
            }
            return Template.NO_FETCHER_FOUND;
        }

        @Override
        public String toString() {
            return "EXTENDED_MAP_FETCHER";
        }
    };

}
