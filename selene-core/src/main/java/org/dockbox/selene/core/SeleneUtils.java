/*
 *  Copyright (C) 2020 Guus Lieben
 *
 *  This framework is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, either version 2.1 of the
 *  License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 *  the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this library. If not, see {@literal<http://www.gnu.org/licenses/>}.
 */

package org.dockbox.selene.core;

import org.dockbox.selene.core.annotations.extension.Extension;
import org.dockbox.selene.core.objects.entity.Ignore;
import org.dockbox.selene.core.objects.entity.Property;
import org.dockbox.selene.core.objects.events.Event;
import org.dockbox.selene.core.objects.optional.Exceptional;
import org.dockbox.selene.core.objects.tuple.Triad;
import org.dockbox.selene.core.objects.tuple.Tuple;
import org.dockbox.selene.core.objects.tuple.Vector3N;
import org.dockbox.selene.core.server.IntegratedExtension;
import org.dockbox.selene.core.server.Selene;
import org.dockbox.selene.core.server.properties.InjectorProperty;
import org.dockbox.selene.core.extension.ExtensionManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("OverlyComplexClass")
public enum SeleneUtils {
    ;

    public static final int MAXIMUM_DECIMALS = 15;
    public static final UUID EMPTY_UUID = UUID.fromString("00000000-1111-2222-3333-000000000000");

    private static final Random random = new Random();
    private static final Map<Object, Triad<LocalDateTime, Long, TemporalUnit>> activeCooldowns = emptyConcurrentMap();
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap =
            ofEntries(entry(boolean.class, Boolean.class),
                    entry(byte.class, Byte.class),
                    entry(char.class, Character.class),
                    entry(double.class, Double.class),
                    entry(float.class, Float.class),
                    entry(int.class, Integer.class),
                    entry(long.class, Long.class),
                    entry(short.class, Short.class));

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <K, V> Map<K, V> ofEntries(Entry<? extends K, ? extends V>... entries) {
        if (entries.length == 0) { // implicit null check of entries array
            return Collections.emptyMap();
        } else {
            Map<K, V> map = emptyMap();
            for (Entry<? extends K, ? extends V> entry : entries) {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        }
    }

    public static <K, V> Entry<K, V> entry(K k, V v) {
        return new Tuple<>(k, v);
    }

    public static void cooldown(Object o, Long duration, TemporalUnit timeUnit, boolean overwriteExisting) {
        if (isInCooldown(o) && !overwriteExisting) return;
        activeCooldowns.put(o, new Triad<>(LocalDateTime.now(), duration, timeUnit));
    }

    public static void cooldown(Object o, Long duration, TemporalUnit timeUnit) {
        cooldown(o, duration, timeUnit, false);
    }

    public static boolean isInCooldown(Object o) {
        if (activeCooldowns.containsKey(o)) {
            LocalDateTime now = LocalDateTime.now();
            Triad<LocalDateTime, Long, TemporalUnit> cooldown = activeCooldowns.get(o);
            LocalDateTime timeCooledDown = cooldown.getFirst();
            Long duration = cooldown.getSecond();
            TemporalUnit timeUnit = cooldown.getThird();

            LocalDateTime endTime = timeCooledDown.plus(duration, timeUnit);

            return endTime.isAfter(now);

        } else return false;
    }

    public static List<Event> getFiredEvents(Event... events) {
        return Arrays.stream(events)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Contract(pure = true)
    @Nullable
    public static Event getFirstFiredEvent(Event... events) {
        for (Event event : events) {
            if (null != event) {
                return event;
            }
        }
        return null;
    }

    private static final char[] _hex = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    public static final String FOLDER_SEPARATOR = "/";

    @Contract(value = "null -> true", pure = true)
    public static boolean isEmpty(String value) {
        return null == value || value.isEmpty();
    }

    @Contract(value = "null -> false", pure = true)
    public static boolean isNotEmpty(String value) {
        return null != value && !value.isEmpty();
    }

    public static String capitalize(String value) {
        return isEmpty(value) ? value : (value.substring(0, 1).toUpperCase() + value.substring(1));
    }

    public static boolean isEmpty(Object object) {
        if (null == object) return true;
        if (object instanceof String) return isEmpty((String) object);
        else if (object instanceof Collection) return ((Collection<?>) object).isEmpty();
        else if (object instanceof Map) return ((Map<?, ?>) object).isEmpty();
        else if (hasMethod(object, "isEmpty"))
            return getMethodValue(object, "isEmpty", Boolean.class).orElse(false);
        else return false;
    }

    @Contract(pure = true)
    public static boolean equals(@NonNls final String str1, @NonNls final String str2) {
        if (null == str1 || null == str2) {
            //noinspection StringEquality
            return str1 == str2;
        }
        return str1.equals(str2);
    }

    @Contract(pure = true)
    public static boolean equalsIgnoreCase(@NonNls final String s1, @NonNls final String s2) {
        if (null == s1 || null == s2) {
            //noinspection StringEquality
            return s1 == s2;
        }
        return s1.equalsIgnoreCase(s2);
    }

    public static boolean equalsWithTrim(@NonNls final String s1, @NonNls final String s2) {
        if (null == s1 || null == s2) {
            //noinspection StringEquality
            return s1 == s2;
        }
        return s1.trim().equals(s2.trim());
    }

    public static boolean equalsIgnoreCaseWithTrim(@NonNls final String s1, @NonNls final String s2) {
        if (null == s1 || null == s2) {
            //noinspection StringEquality
            return s1 == s2;
        }
        return s1.trim().equalsIgnoreCase(s2.trim());
    }

    public static boolean equal(Object expected, Object actual) {
        if (expected != null || actual != null) {
            return !(expected == null || !expected.equals(actual));
        }
        return false;
    }

    public static boolean same(Object expected, Object actual) {
        return expected == actual;
    }

    public static boolean notEqual(Object expected, Object actual) {
        return !equal(expected, actual);
    }

    public static boolean notSame(Object expected, Object actual) {
        return !same(expected, actual);
    }

    public static boolean hasContent(final String s) {
        return !(0 == trimLength(s));    // faster than returning !isEmpty()
    }

    public static int length(final CharSequence s) {
        return null == s ? 0 : s.length();
    }

    public static int trimLength(final String s) {
        return (null == s) ? 0 : s.trim().length();
    }

    @Contract(pure = true)
    public static int lastIndexOf(String path, char ch) {
        if (null == path) {
            return -1;
        }
        return path.lastIndexOf(ch);
    }

    @NotNull
    @SuppressWarnings("MagicNumber")
    public static byte[] decode(CharSequence s) {
        int len = s.length();
        if (0 != len % 2) {
            return new byte[0];
        }

        byte[] bytes = new byte[len / 2];
        int pos = 0;

        for (int i = 0; i < len; i += 2) {
            byte hi = (byte) Character.digit(s.charAt(i), 16);
            byte lo = (byte) Character.digit(s.charAt(i + 1), 16);
            bytes[pos++] = (byte) (hi * 16 + lo);
        }

        return bytes;
    }

    @NotNull
    @SuppressWarnings("MagicNumber")
    public static String encode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length << 1);
        for (byte aByte : bytes) {
            sb.append(convertDigit(aByte >> 4));
            sb.append(convertDigit(aByte & 0x0f));
        }
        return sb.toString();
    }

    @Contract(pure = true)
    public static int[] range(int max) {
        return range(0, max);
    }

    @NotNull
    @Contract(pure = true)
    public static int[] range(int min, int max) {
        int[] range = new int[(max - min) + 1]; // +1 as both min and max are inclusive
        for (int i = min; i <= max; i++) {
            range[i - min] = i;
        }
        return range;
    }

    @NotNull
    public static String repeat(String string, int amount) {
        StringBuilder sb = new StringBuilder();
        for (int ignored : range(1, amount)) sb.append(string);
        return sb.toString();
    }

    @Contract(pure = true)
    @SuppressWarnings("MagicNumber")
    private static char convertDigit(int value) {
        return _hex[value & 0x0f];
    }

    public static int count(String s, char c) {
        if (isEmpty(s)) {
            return 0;
        }

        int count = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }

        return count;
    }

    @NotNull
    public static String wildcardToRegexString(@NotNull CharSequence wildcard) {
        StringBuilder s = new StringBuilder(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    s.append(".*");
                    break;

                case '?':
                    s.append('.');
                    break;

                // escape special regexp-characters
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '^':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    s.append('\\');
                    s.append(c);
                    break;

                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return s.toString();
    }

    public static int levenshteinDistance(@NonNls CharSequence s, @NonNls CharSequence t) {
        // degenerate cases          s
        if (null == s || "".contentEquals(s)) {
            return null == t || "".contentEquals(t) ? 0 : t.length();
        } else if (null == t || "".contentEquals(t)) {
            return s.length();
        }

        // create two work vectors of integer distances
        int[] v0 = new int[t.length() + 1];
        int[] v1 = new int[t.length() + 1];

        // initialize v0 (the previous row of distances)
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i < v0.length; i++) {
            v0[i] = i;
        }

        int sLen = s.length();
        int tLen = t.length();
        for (int i = 0; i < sLen; i++) {
            // calculate v1 (current row distances) from the previous row v0

            // first element of v1 is A[i+1][0]
            //   edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < tLen; j++) {
                int cost = (s.charAt(i) == t.charAt(j)) ? 0 : 1;
                v1[j + 1] = (int) minimum(v1[j] + 1, v0[j + 1] + 1, v0[j] + cost);
            }

            // copy v1 (current row) to v0 (previous row) for next iteration
            System.arraycopy(v1, 0, v0, 0, v0.length);
        }

        return v1[t.length()];
    }

    public static int damerauLevenshteinDistance(@NonNls CharSequence source, @NonNls CharSequence target) {
        if (null == source || "".contentEquals(source)) {
            return null == target || "".contentEquals(target) ? 0 : target.length();
        } else if (null == target || "".contentEquals(target)) {
            return source.length();
        }

        int srcLen = source.length();
        int targetLen = target.length();
        int[][] distanceMatrix = new int[srcLen + 1][targetLen + 1];

        // We need indexers from 0 to the length of the source string.
        // This sequential set of numbers will be the row "headers"
        // in the matrix.
        for (int srcIndex = 0; srcIndex <= srcLen; srcIndex++) {
            distanceMatrix[srcIndex][0] = srcIndex;
        }

        // We need indexers from 0 to the length of the target string.
        // This sequential set of numbers will be the
        // column "headers" in the matrix.
        for (int targetIndex = 0; targetIndex <= targetLen; targetIndex++) {
            // Set the value of the first cell in the column
            // equivalent to the current value of the iterator
            distanceMatrix[0][targetIndex] = targetIndex;
        }

        for (int srcIndex = 1; srcIndex <= srcLen; srcIndex++) {
            for (int targetIndex = 1; targetIndex <= targetLen; targetIndex++) {
                // If the current characters in both strings are equal
                int cost = source.charAt(srcIndex - 1) == target.charAt(targetIndex - 1) ? 0 : 1;

                // Find the current distance by determining the shortest path to a
                // match (hence the 'minimum' calculation on distances).
                distanceMatrix[srcIndex][targetIndex] = (int) minimum(
                        // Character match between current character in
                        // source string and next character in target
                        distanceMatrix[srcIndex - 1][targetIndex] + 1,
                        // Character match between next character in
                        // source string and current character in target
                        distanceMatrix[srcIndex][targetIndex - 1] + 1,
                        // No match, at current, add cumulative penalty
                        distanceMatrix[srcIndex - 1][targetIndex - 1] + cost);

                // We don't want to do the next series of calculations on
                // the first pass because we would get an index out of bounds
                // exception.
                if (1 == srcIndex || 1 == targetIndex) {
                    continue;
                }

                // transposition check (if the current and previous
                // character are switched around (e.g.: t[se]t and t[es]t)...
                if (source.charAt(srcIndex - 1) == target.charAt(targetIndex - 2) && source.charAt(srcIndex - 2) == target.charAt(targetIndex - 1)) {
                    // What's the minimum cost between the current distance
                    // and a transposition.
                    distanceMatrix[srcIndex][targetIndex] = (int) minimum(
                            // Current cost
                            distanceMatrix[srcIndex][targetIndex],
                            // Transposition
                            distanceMatrix[srcIndex - 2][targetIndex - 2] + cost);
                }
            }
        }

        return distanceMatrix[srcLen][targetLen];
    }

    @NotNull
    public static String getRandomString(int minLen, int maxLen) {
        StringBuilder s = new StringBuilder();
        int length = minLen + random.nextInt(maxLen - minLen + 1);
        for (int i = 0; i < length; i++) {
            s.append(getRandomChar(0 == i));
        }
        return s.toString();
    }

    @NotNull
    @SuppressWarnings({"BooleanParameter", "MagicNumber"})
    public static String getRandomChar(boolean upper) {
        int r = random.nextInt(26);
        return upper ? "" + (char) ((int) 'A' + r) : "" + (char) ((int) 'a' + r);
    }

    @NotNull
    public static byte[] getBytes(String s, String encoding) {
        try {
            return null == s ? new byte[0] : s.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(String.format("Encoding (%s) is not supported by your JVM", encoding), e);
        }
    }


    @NotNull
    public static String createUtf8String(byte[] bytes) {
        return createString(bytes, "UTF-8");
    }

    @NotNull
    public static byte[] getUTF8Bytes(String s) {
        return getBytes(s, "UTF-8");
    }

    @NotNull
    public static String createString(byte[] bytes, String encoding) {
        try {
            return null == bytes ? "" : new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(String.format("Encoding (%s) is not supported by your JVM", encoding), e);
        }
    }

    @NotNull
    public static String createUTF8String(byte[] bytes) {
        return createString(bytes, "UTF-8");
    }

    @SuppressWarnings("MagicNumber")
    public static int hashCodeIgnoreCase(CharSequence s) {
        if (null == s) return 0;
        int hash = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = Character.toLowerCase(s.charAt(i));
            hash = 31 * hash + c;
        }
        return hash;
    }

    @Contract(pure = true)
    public static long minimum(long... values) {
        int len = values.length;
        long current = values[0];
        for (int i = 1; i < len; i++) current = Math.min(values[i], current);
        return current;
    }

    @Contract(pure = true)
    public static long maximum(long... values) {
        int len = values.length;
        long current = values[0];
        for (int i = 1; i < len; i++) current = Math.max(values[i], current);
        return current;
    }

    @Contract(pure = true)
    public static double minimum(double... values) {
        int len = values.length;
        double current = values[0];
        for (int i = 1; i < len; i++) current = Math.min(values[i], current);
        return current;
    }

    @Contract(pure = true)
    public static double maximum(double... values) {
        int len = values.length;
        double current = values[0];
        for (int i = 1; i < len; i++) current = Math.max(values[i], current);
        return current;
    }

    public static void assertContainsIgnoreCase(String source, String... contains) {
        String lowerSource = source.toLowerCase();
        for (String contain : contains) {
            int idx = lowerSource.indexOf(contain.toLowerCase());
            String msg = "'" + contain + "' not found in '" + lowerSource + "'";
            assert 0 <= idx : msg;
            lowerSource = lowerSource.substring(idx);
        }
    }

    public static boolean checkContainsIgnoreCase(String source, String... contains) {
        String lowerSource = source.toLowerCase();
        for (String contain : contains) {
            int idx = lowerSource.indexOf(contain.toLowerCase());
            if (-1 == idx) {
                return false;
            }
            lowerSource = lowerSource.substring(idx);
        }
        return true;
    }

    public static Throwable getDeepestException(Throwable e) {
        while (null != e.getCause())
            e = e.getCause();
        return e;
    }

    @Contract("null -> true")
    public static boolean isEmpty(final Object... array) {
        return null == array || 0 == Array.getLength(array);
    }

    public static int size(final Object... array) {
        return null == array ? 0 : Array.getLength(array);
    }

    @Contract("null -> null")
    public static <T> T @Nullable [] shallowCopy(final T[] array) {
        if (null == array) {
            return null;
        }
        return array.clone();
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] addAll(final T[] array1, final T[] array2) {
        if (null == array1) {
            return shallowCopy(array2);
        } else if (null == array2) {
            return shallowCopy(array1);
        }
        final T[] newArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
        System.arraycopy(array1, 0, newArray, 0, array1.length);
        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeItem(T[] array, int pos) {
        int length = Array.getLength(array);
        T[] dest = (T[]) Array.newInstance(array.getClass().getComponentType(), length - 1);

        System.arraycopy(array, 0, dest, 0, pos);
        System.arraycopy(array, pos + 1, dest, pos, length - pos - 1);
        return dest;
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static <T> T[] getArraySubset(T[] array, int start, int end) {
        return Arrays.copyOfRange(array, start, end);
    }

    @SuppressWarnings({"unchecked", "SuspiciousToArrayCall"})
    public static <T> T[] toArray(Class<T> classToCastTo, Collection<?> c) {
        T[] array = c.toArray((T[]) Array.newInstance(classToCastTo, c.size()));
        Iterator<?> i = c.iterator();
        int idx = 0;
        while (i.hasNext()) {
            Array.set(array, idx++, i.next());
        }
        return array;
    }

    @Contract("null, _ -> false; !null, null -> false")
    public static <T> boolean isGenericInstanceOf(T instance, Class<?> type) {
        return null != instance && null != type && isAssignableFrom(type, instance.getClass());
    }

    public static boolean isFileEmpty(@NotNull Path file) {
        return Files.exists(file) && 0 <= file.toFile().length();
    }

    public static <T> Collection<T> singletonList(T mockWorld) {
        return Collections.singletonList(mockWorld);
    }

    public <T> T[] merge(T[] arrayOne, T[] arrayTwo) {
        Object[] merged = Stream.of(arrayOne, arrayTwo).flatMap(Stream::of).toArray(Object[]::new);
        return this.convertGenericArray(merged);
    }

    public static <T> List<T> emptyConcurrentList() {
        return new CopyOnWriteArrayList<>();
    }

    public static <T> Set<T> emptyConcurrentSet() {
        return ConcurrentHashMap.newKeySet();
    }

    public static <K, V> ConcurrentMap<K, V> emptyConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    public static <T> List<T> emptyList() {
        return new ArrayList<>();
    }

    public static <T> Set<T> emptySet() {
        return new HashSet<>();
    }

    public static <K, V> Map<K, V> emptyMap() {
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private <T> T[] convertGenericArray(Object[] array) {
        try {
            Class<T> type = (Class<T>) array.getClass().getComponentType();
            T[] finalArray = (T[]) Array.newInstance(type, 0);
            return (T[]) addAll(finalArray, array);
        } catch (ClassCastException e) {
            Selene.log().error("Attempted to convert generic array not matching generic type T", e);
        }
        return (T[]) new Object[0];
    }

    public static double round(double value, int decimalPlaces) {
        if (Double.isNaN(value) || Double.isInfinite(value) || MAXIMUM_DECIMALS < decimalPlaces) {
            return value;
        }

        BigDecimal decimal = BigDecimal.valueOf(value);
        decimal = decimal.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return decimal.doubleValue();
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    @SafeVarargs
    public static <T> List<T> asList(T... objects) {
        return asList(Arrays.asList(objects));
    }

    public static <T> List<T> asList(Collection<T> collection) {
        return new ArrayList<>(collection);
    }

    @NotNull
    @Contract("_ -> new")
    @SafeVarargs
    public static <T> Set<T> asSet(T... objects) {
        return new HashSet<>(asList(objects));
    }

    @NotNull
    @Contract(value = "_ -> new")
    public static <T> Set<T> asSet(Collection<T> collection) {
        return new HashSet<>(collection);
    }

    @UnmodifiableView
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    @SafeVarargs
    public static <T> List<T> asUnmodifiableList(T... objects) {
        return Collections.unmodifiableList(asList(objects));
    }

    public static <T> List<T> asUnmodifiableList(Collection<T> collection) {
        return Collections.unmodifiableList(emptyList());
    }

    @UnmodifiableView
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    @SafeVarargs
    public static <T> Set<T> asUnmodifiableSet(T... objects) {
        return Collections.unmodifiableSet(asSet(objects));
    }

    public static <T> Collection<T> asUnmodifiableCollection(T... collection) {
        return Collections.unmodifiableCollection(Arrays.asList(collection));
    }

    public static <T> Collection<T> asUnmodifiableCollection(Collection<T> collection) {
        return Collections.unmodifiableCollection(collection);
    }

    public static <K, V> Map<K, V> asUnmodifiableMap(Map<K, V> map) {
        return Collections.unmodifiableMap(map);
    }

    @UnmodifiableView
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static <T> List<T> asUnmodifiableList(List<T> objects) {
        return Collections.unmodifiableList(objects);
    }

    @UnmodifiableView
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static <T> Set<T> asUnmodifiableSet(Set<T> objects) {
        return Collections.unmodifiableSet(objects);
    }

    public static boolean throwsException(Runnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean throwsException(Runnable runnable, Class<? extends Throwable> exception) {
        try {
            runnable.run();
            return false;
        } catch (Throwable t) {
            return isAssignableFrom(exception, t.getClass());
        }
    }

    public static boolean doesNotThrow(Runnable runnable) {
        return !throwsException(runnable);
    }

    public static boolean doesNotThrow(Runnable runnable, Class<? extends Throwable> exception) {
        return !throwsException(runnable, exception);
    }

    @NotNull
    @Contract("_ -> param1")
    public static Path createPathIfNotExists(@NotNull Path path) {
        if (!path.toFile().exists()) path.toFile().mkdirs();
        return path;
    }

    @Contract("_ -> param1")
    public static Path createFileIfNotExists(@NotNull Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.getParent());
                Files.createFile(file);
            } catch (IOException ex) {
                Selene.getServer().except("Could not create file '" + file.getFileName() + "'", ex);
            }
        }
        return file;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static boolean unwrap(Optional<Boolean> optional) {
        return optional.isPresent() && optional.get();
    }

    @SuppressWarnings("OverlyComplexBooleanExpression")
    @Contract(pure = true)
    public static boolean isInCuboidRegion(int x_min, int x_max, int y_min, int y_max, int z_min, int z_max, int x, int y, int z) {
        return x_min <= x && x <= x_max && y_min <= y && y <= y_max && z_min <= z && z <= z_max;
    }

    public static boolean isInCuboidRegion(Vector3N min, Vector3N max, Vector3N vec) {
        return isInCuboidRegion(
                min.getXi(), max.getXi(),
                min.getYi(), max.getYi(),
                min.getZi(), max.getZi(),
                vec.getXi(), vec.getYi(), vec.getZi());
    }

    @NotNull
    @Contract("_ -> new")
    public static LocalDateTime toLocalDateTime(Instant dt) {
        return LocalDateTime.ofInstant(dt, ZoneId.systemDefault());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Exceptional<LocalDateTime> toLocalDateTime(Optional<Instant> optionalInstant) {
        return Exceptional.of(optionalInstant).map(SeleneUtils::toLocalDateTime);
    }

    @NotNull
    @Unmodifiable
    public static <A extends Annotation> Collection<Method> getAnnotedMethods(Class<?> clazz, Class<A> annotation, Predicate<A> rule, boolean skipParents) {
        List<Method> annotatedMethods = emptyList();
        for (Method method : asList(skipParents ? clazz.getMethods() : clazz.getDeclaredMethods())) {
            if (!method.isAccessible()) method.setAccessible(true);
            if (method.isAnnotationPresent(annotation) && rule.test(method.getAnnotation(annotation))) {
                annotatedMethods.add(method);
            }
        }
        return asUnmodifiableList(annotatedMethods);
    }

    @NotNull
    @Unmodifiable
    public static <A extends Annotation> Collection<Method> getAnnotedMethods(Class<?> clazz, Class<A> annotation, Predicate<A> rule) {
        return getAnnotedMethods(clazz, annotation, rule, false);
    }

    public static <A extends Annotation> Collection<Class<?>> getAnnotatedTypes(String prefix, Class<A> annotation, boolean skipParents) {
        Reflections reflections = new Reflections(prefix);
        Set<Class<?>> types = reflections.getTypesAnnotatedWith(annotation, !skipParents);
        return asList(types);
    }

    public static <A extends Annotation> Collection<Class<?>> getAnnotatedTypes(String prefix, Class<A> annotation) {
        return getAnnotatedTypes(prefix, annotation, false);
    }

    public static <T> T getInstance(Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getConstructor();
            return ctor.newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            return Selene.getInstance(clazz);
        }
    }

    public static boolean hasMethod(Object instance, String method) {
        return hasMethod(instance.getClass(), method);
    }

    public static boolean hasMethod(Class<?> type, @NonNls String method) {
        for (Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(method)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static <T> Exceptional<T> getMethodValue(Object instance, String method, Class<T> expectedType) {
        try {
            Method m = instance.getClass().getDeclaredMethod(method);
            T value = (T) m.invoke(instance);
            return Exceptional.ofNullable(value);
        } catch (ClassCastException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return Exceptional.empty();
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> InjectorProperty<T> getProperty(@NonNls String key, Class<T> expectedType, InjectorProperty<?>... properties) {
        for (InjectorProperty<?> property : properties) {
            if (property.getKey().equals(key)
                    && null != property.getObject()
                    && isAssignableFrom(expectedType, property.getObject().getClass())
            ) {
                return (InjectorProperty<T>) property;
            }
        }
        return null;
    }

    public static <T> Exceptional<T> getPropertyValue(@NonNls String key, Class<T> expectedType, InjectorProperty<?>... properties) {
        InjectorProperty<T> property = getProperty(key, expectedType, properties);
        if (null != property) {
            return Exceptional.of(property::getObject);
        }
        return Exceptional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T extends InjectorProperty<?>> List<T> getSubProperties(Class<T> propertyFilter, InjectorProperty<?>... properties) {
        List<T> values = emptyList();
        for (InjectorProperty<?> property : properties) {
            if (isAssignableFrom(propertyFilter, property.getClass())) values.add((T) property);
        }
        return values;
    }

    public static <T> Exceptional<T> tryCreateFromRaw(Class<T> type, Function<Field, Object> valueCollector, boolean inject) {
        return tryCreate(type, valueCollector, inject, Provision.FIELD);
    }

    public static <T> Exceptional<T> tryCreateFromProcessed(Class<T> type, Function<String, Object> valueCollector, boolean inject) {
        return tryCreate(type, valueCollector, inject, Provision.FIELD_NAME);
    }

    private static <T, A> Exceptional<T> tryCreate(Class<T> type, Function<A, Object> valueCollector, boolean inject, Provision provision) {
        T instance = inject ? Selene.getInstance(type) : getInstance(type);
        if (null != instance)
            try {
                for (Field field : type.getDeclaredFields()) {
                    if (!field.isAccessible()) field.setAccessible(true);
                    if (field.isAnnotationPresent(Ignore.class)) continue;
                    Object value;
                    if (Provision.FIELD == provision) {
                        value = valueCollector.apply((A) field);
                    } else {
                        String fieldName = processFieldName(field);
                        value = valueCollector.apply((A) fieldName);
                    }
                    if (null == value) continue;

                    boolean useFieldDirect = true;
                    if (field.isAnnotationPresent(Property.class)) {
                        Property property = field.getAnnotation(Property.class);

                        //noinspection CallToSuspiciousStringMethod
                        if (!"".equals(property.setter()) && hasMethod(type, property.setter())) {
                            Class<?> parameterType = field.getType();
                            if (isNotVoid(property.accepts())) parameterType = property.accepts();

                            Method method = type.getMethod(property.setter(), parameterType);
                            method.invoke(instance, value);
                            useFieldDirect = false;
                        }
                    }

                    if (useFieldDirect && isAssignableFrom(field.getType(), value.getClass()))
                        field.set(instance, value);
                }
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                return Exceptional.of(e);
            }
        return Exceptional.ofNullable(instance);
    }

    public static <T> Exceptional<T> tryCreateFromMap(Class<T> type, Map<String, Object> map) {
        return tryCreateFromProcessed(type, key -> map.getOrDefault(key, null), true);
    }

    public static String processFieldName(Field field) {
        String fieldName = field.getName();
        if (field.isAnnotationPresent(Property.class))
            fieldName = field.getAnnotation(Property.class).value();
        return fieldName;
    }

    public static boolean isPrimitiveWrapperOf(Class<?> targetClass, Class<?> primitive) {
        if (!primitive.isPrimitive()) {
            throw new IllegalArgumentException("First argument has to be primitive type");
        }
        return primitiveWrapperMap.get(primitive) == targetClass;
    }

    public static boolean isEitherAssignableFrom(Class<?> to, Class<?> from) {
        return isAssignableFrom(from, to) || isAssignableFrom(to, from);
    }

    public static boolean isAssignableFrom(Class<?> to, Class<?> from) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (from.isPrimitive()) {
            return isPrimitiveWrapperOf(to, from);
        }
        if (to.isPrimitive()) {
            return isPrimitiveWrapperOf(from, to);
        }
        return false;
    }

    public static String getFieldPropertyName(Field field) {
        return field.isAnnotationPresent(Property.class)
                ? field.getAnnotation(Property.class).value()
                : field.getName();
    }

    public static boolean isNotVoid(Class<?> type) {
        return !(type.equals(Void.class) || type == Void.TYPE);
    }

    @Nullable
    public static Extension getExtension(Class<?> type) {
        if (null == type) return null;
        if (type.equals(Selene.class)) return getExtension(Selene.getInstance(IntegratedExtension.class).getClass());
        Extension extension = type.getAnnotation(Extension.class);
        extension = null != extension ? extension : getExtension(type.getSuperclass());
        if (null == extension)
            extension = Selene.getInstanceSafe(ExtensionManager.class).map(em -> em.getHeader(type).orNull()).orNull();
        return extension;
    }

    @Nullable
    public static <T> T runWithExtension(Class<?> type, Function<Extension, T> function) {
        Extension extension = getExtension(type);
        if (null != extension) return function.apply(extension);
        return null;
    }

    public static void runWithExtension(Class<?> type, Consumer<Extension> consumer) {
        Extension extension = getExtension(type);
        if (null != extension) consumer.accept(extension);
    }

    public static <T> void runWithInstance(Class<T> type, Consumer<T> consumer) {
        T instance = Selene.getInstance(type);
        if (null != instance) consumer.accept(instance);
    }

    public enum Provision {
        FIELD, FIELD_NAME
    }

    public enum HttpStatus {
        ;
        // 1xx Informational
        public static final Integer CONTINUE = 100;
        public static final Integer SWITCHING_PROTOCOLS = 101;
        public static final Integer PROCESSING = 102;

        // 2xx Success
        public static final Integer OK = 200;
        public static final Integer CREATED = 201;
        public static final Integer ACCEPTED = 202;
        public static final Integer NON_AUTHORITATIVE_INFORMATION = 203;
        public static final Integer NO_CONTENT = 204;
        public static final Integer RESET_CONTENT = 205;
        public static final Integer PARTIAL_CONTENT = 206;
        public static final Integer MULTI_STATUS = 207;
        public static final Integer ALREADY_REPORTED = 208;
        public static final Integer IM_USED = 226;

        // 3xx Redirection
        public static final Integer MULTIPLE_CHOICES = 300;
        public static final Integer MOVED_PERMANENTLY = 301;
        public static final Integer FOUND = 302;
        public static final Integer SEE_OTHER = 303;
        public static final Integer NOT_MODIFIED = 304;
        public static final Integer USE_PROXY = 305;
        public static final Integer TEMPORARY_REDIRECT = 307;
        public static final Integer PERMANENT_REDIRECT = 308;

        // 4xx Client Error
        public static final Integer BAD_REQUEST = 400;
        public static final Integer UNAUTHORIZED = 401;
        public static final Integer PAYMENT_REQUIRED = 402;
        public static final Integer FORBIDDEN = 403;
        public static final Integer NOT_FOUND = 404;
        public static final Integer METHOD_NOT_ALLOWED = 405;
        public static final Integer NOT_ACCEPTABLE = 406;
        public static final Integer PROXY_AUTHENTICATION_REQUIRED = 407;
        public static final Integer REQUEST_TIMEOUT = 408;
        public static final Integer CONFLICT = 409;
        public static final Integer GONE = 410;
        public static final Integer LENGTH_REQUIRED = 411;
        public static final Integer PRECONDITION_FAILED = 412;
        public static final Integer REQUEST_ENTITY_TOO_LARGE = 413;
        public static final Integer REQUEST_URI_TOO_LONG = 414;
        public static final Integer UNSUPPORTED_MEDIA_TYPE = 415;
        public static final Integer REQUESTED_RANGE_NOT_SATISFIABLE = 416;
        public static final Integer EXPECTATION_FAILED = 417;
        public static final Integer IAM_A_TEAPOT = 418;
        public static final Integer ENHANCE_YOUR_CALM = 420;
        public static final Integer UNPROCESSABLE_ENTITY = 422;
        public static final Integer LOCKED = 423;
        public static final Integer FAILED_DEPENDENCY = 424;
        public static final Integer UPGRADE_REQUIRED = 426;
        public static final Integer PRECONDITION_REQUIRED = 428;
        public static final Integer TOO_MANY_REQUESTS = 429;
        public static final Integer REQUEST_HEADER_FIELDS_TOO_LARGE = 431;
        public static final Integer NO_RESPONSE = 444;
        public static final Integer RETRY_WITH = 449;
        public static final Integer UNAVAILABLE_FOR_LEGAL_REASONS = 451;
        public static final Integer CLIENT_CLOSED_REQUEST = 499;

        // 5xx Server Error
        public static final Integer INTERNAL_SERVER_ERROR = 500;
        public static final Integer NOT_IMPLEMENTED_ = 501;
        public static final Integer BAD_GATEWAY = 502;
        public static final Integer SERVICE_UNAVAILABLE = 503;
        public static final Integer GATEWAY_TIMEOUT = 504;
        public static final Integer HTTP_VERSION_NOT_SUPPORTED = 505;
        public static final Integer VARIANT_ALSO_NEGOTIATES = 506;
        public static final Integer INSUFFICIENT_STORAGE = 507;
        public static final Integer LOOP_DETECTED = 508;
        public static final Integer BANDWIDTH_LIMIT_EXCEEDED = 509;
        public static final Integer NOT_EXTENDED = 510;
        public static final Integer NETWORK_AUTHENTICATION_REQUIRED = 511;
        public static final Integer NETWORK_READ_TIMEOUT_ERROR = 598;
        public static final Integer NETWORK_CONNECT_TIMEOUT_ERROR = 599;
    }
}
