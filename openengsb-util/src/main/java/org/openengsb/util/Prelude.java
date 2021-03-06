/**

   Copyright 2009 OpenEngSB Division, Vienna University of Technology

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   
 */
package org.openengsb.util;

import org.openengsb.util.tuple.Pair;
import org.openengsb.util.tuple.Singleton;
import org.openengsb.util.tuple.Triple;
import org.openengsb.util.tuple.Unit;

public class Prelude {
    /**
     * Returns a {@code Unit} object.
     * 
     * @see Unit
     */
    public static Unit unit() {
        return Unit.UNIT;
    }

    /**
     * Creates a Singleton with the specified parameter.
     * 
     * @see Singleton
     */
    public static <A> Singleton<A> singleton(final A fst) {
        return new Singleton<A>(fst);
    }

    /**
     * Creates a Pair with the specified parameters.
     * 
     * @see Pair
     */
    public static <A, B> Pair<A, B> pair(final A fst, final B snd) {
        return new Pair<A, B>(fst, snd);
    }

    /**
     * Creates a Triple with the specified parameters.
     * 
     * @see Triple
     */
    public static <A, B, C> Triple<A, B, C> triple(final A fst, final B snd, final C trd) {
        return new Triple<A, B, C>(fst, snd, trd);
    }

    /**
     * Breaks the specified string into an array of strings at newline
     * characters. The resulting strings do not contain newlines. This method
     * should work cross-platform, it breaks at the following line separators:
     * {@code \n, \r, \r\n, \n\r}.
     * 
     * @param input a string with line separators
     * @return array of lines
     */
    public static String[] lines(final String input) {
        return input.split("(\r\n)|(\n\r)|(\n)|(\r)");
    }

    /**
     * Concatenates the elements of a String array together placing '/' between
     * them.
     * 
     * @param elems an array of strings to combine
     * @return the array elements combined to one 'path'
     */
    public static String pathize(final String[] elems) {
        final StringBuilder builder = new StringBuilder();
        for (final String elem : elems) {
            builder.append(elem).append("/");
        }
        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    /**
     * Reverse operation of {@link #pathize(String[])}
     * 
     * @param a 'path' to split in single elements
     * @return an array of strings representing path elements
     */
    public static String[] dePathize(final String elem) {
        String tmp = elem;
        while (tmp.startsWith("/")) {
            tmp = tmp.substring(1);
        }
        while (tmp.endsWith("/")) {
            tmp = tmp.substring(0, tmp.length() - 1);
        }

        return tmp.split("/");
    }

    private Prelude() {
        throw new AssertionError();
    }
}
