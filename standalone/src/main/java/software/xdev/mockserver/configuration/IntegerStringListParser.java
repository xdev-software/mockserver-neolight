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
package software.xdev.mockserver.configuration;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class IntegerStringListParser {
    
    private static final Logger LOG = LoggerFactory.getLogger(IntegerStringListParser.class);
    
    public Integer[] toArray(String integers) {
        return toList(integers).toArray(new Integer[0]);
    }

    List<Integer> toList(String integers) {
        List<Integer> integerList = new ArrayList<Integer>();
        for (String integer : integers.split(",")) {
            try {
                integerList.add(Integer.parseInt(integer.trim()));
            } catch (NumberFormatException nfe) {
                LOG.error("NumberFormatException converting {} to integer", integer, nfe);
            }
        }
        return integerList;
    }

    public String toString(Integer[] integers) {
        return toString(Arrays.asList(integers));
    }

    public String toString(List<Integer> integers) {
        return integers.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }
}
