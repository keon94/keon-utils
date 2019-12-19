package com.keon.projects.differ;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Differ {

    public static <T> Diff<?> diff(final T obj1, final T obj2) throws Exception {
        return diff0(obj1, obj2);
    }

    private static <T, U> Diff<?> diff0(final T o1, final T o2) throws Exception {
        if (o1 == o2)
            return Diff.EMPTY;
        if ((o1 == null && o2 != null) || (o1 != null && o2 == null)) {
            return new Diff<>(o1, o2);
        }
        //neither is null
        if(o1.equals(o2)) {
            return Diff.EMPTY;
        } else if (o1.getClass().isArray()) {
            return diffArray0((Object[])o1, (Object[])o2);
        } else {
            final Diff<?> totalDiff = new Diff<>(o1, o2);
            for (final Field f : o1.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                final U of1 = (U) f.get(o1);
                final U of2 = (U) f.get(o2);
                final Diff<?> diff = diff0(of1, of2);
                if (diff != Diff.EMPTY)
                    totalDiff.add(diff.forField(f));
            }
            return totalDiff.getEffective();
        }
    }

    private static <T> Diff<?> diffArray0(final T[] a1, final T[] a2) throws Exception {
        if (a1.length < a2.length) {
            return diffArray0(a2, a1);
        }
        //a1 will be the larger one
        final Diff<?> total = new Diff<>(a1, a2);
        int i = 0;
        for (; i < a2.length; ++i) {
            final Diff<?> diff = diff0(a1[i], a2[i]);
            if (diff != Diff.EMPTY) {
                total.add(diff);
            }
        }
        for (; i < a1.length; ++i) {
            if (a1[i] != null) {
                total.add(new Diff<T>(a1[i], null));
            }
        }
        return total;
    }


    public static class Diff<T> {

        static final Diff<?> EMPTY = null;

        private final Set<Diff<?>> diffs = new HashSet<>();

        private String field;
        private final T obj1;
        private final T obj2;

        private Diff(final T obj1, final T obj2) {
            this.obj1 = obj1;
            this.obj2 = obj2;
            field = tag(obj1 != null ? obj1.getClass().getSimpleName() : obj2 != null ? obj2.getClass().getSimpleName() : "null", null);
        }

        private Diff<T> forField(final Field field) {
            this.field = tag(field.getType().getSimpleName(), field.getName());
            return this;
        }

        private Diff<T> add(final Diff<?> diff) {
            diffs.add(diff);
            return this;
        }

        private Diff<?> getEffective() {
            return diffs.isEmpty() ? EMPTY : this;
        }

        //====================================== Printing functions ===========================================

        @Override
        public String toString() {
            final String string = "<root>" + toString0() + "</root>";
            try {
                final Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                final StreamResult result = new StreamResult(new StringWriter());
                transformer.transform(new StreamSource(new StringReader(string)), result);
                return result.getWriter().toString().replace("Xequals", "=").replace("Xbracket", "[]").replace("Xspace", " ");
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String toString0() {
            final StringBuilder builder = new StringBuilder();
            if (diffs.isEmpty()) {
                final String fieldName = field == null ? "null" : field;
                if (!obj1.equals(obj2)) {
                    final String s1 = obj1.getClass().isArray() ? Arrays.toString(toArray(obj1)) : obj1.toString();
                    final String s2 = obj2 == null ? null : obj2.getClass().isArray() ? Arrays.toString(toArray(obj2)) : obj2.toString();
                    builder.append(wrap(fieldName, wrap("o1", s1) + wrap("o2", s2)));
                }
            } else {
                final StringBuilder subdiffs = new StringBuilder();
                for (final Diff<?> d : diffs) {
                    subdiffs.append(d.toString0());
                }
                if (subdiffs.length() > 0) {
                    builder.append(wrap(field, subdiffs.toString()));
                }
            }
            return builder.toString();
        }

        private static String wrap(final String tag, final String s) {
            return "<" + tag + ">" + s + "</" + tag + ">";
        }

        private static Object[] toArray(final Object o) {
            final Class<?> ofArray = o.getClass().getComponentType();
            if (ofArray.isPrimitive()) {
                final List<Object> ar = new ArrayList<>();
                int length = Array.getLength(o);
                for (int i = 0; i < length; i++) {
                    ar.add(Array.get(o, i));
                }
                return ar.toArray();
            } else {
                return (Object[]) o;
            }
        }

        private static String tag(final String type, final String name) {
            return "typeXequals" + type.replace("[]", "Xbracket") + "Xspace" + "nameXequals" + name;
        }
    }
}