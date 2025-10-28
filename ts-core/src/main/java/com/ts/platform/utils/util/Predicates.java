package com.ts.platform.utils.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Predicate;

public class Predicates {
    public static Predicate ALWAYS_TRUE_PREDICATE = new AlwaysTruePredicate();

    private Predicates() {
    }

    public static Predicate alwaysTrue() {
        return ALWAYS_TRUE_PREDICATE;
    }

    public static Predicate and(final Predicate[] predicates) {
        return new Predicate() {
            public boolean test(Object t) {
                for(Predicate predicate : predicates) {
                    if (!predicate.test(t)) {
                        return false;
                    }
                }

                return true;
            }
        };
    }

    public static <T> Predicate<T> in(Collection<? extends T> target) {
        return new InPredicate<T>(target);
    }

    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return new NotPredicate<T>(predicate);
    }

    private static class InPredicate<T> implements Predicate<T>, Serializable {
        private static final long serialVersionUID = -8049890389593544847L;
        private final Collection<?> target;

        private InPredicate(Collection<?> target) {
            this.target = target;
        }

        public boolean test(T t) {
            try {
                return this.target.contains(t);
            } catch (ClassCastException | NullPointerException var3) {
                return false;
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof InPredicate) {
                InPredicate<?> that = (InPredicate)obj;
                return this.target.equals(that.target);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return this.target.hashCode();
        }

        public String toString() {
            return "Predicates.in(" + this.target + ")";
        }
    }

    private static class NotPredicate<T> implements Predicate<T>, Serializable {
        private static final long serialVersionUID = 8132922606124731479L;
        private final Predicate<T> predicate;

        private NotPredicate(Predicate<T> predicate) {
            this.predicate = predicate;
        }

        public boolean test(T t) {
            return !this.predicate.test(t);
        }

        public int hashCode() {
            return ~this.predicate.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof NotPredicate) {
                NotPredicate<?> that = (NotPredicate)obj;
                return this.predicate.equals(that.predicate);
            } else {
                return false;
            }
        }

        public String toString() {
            return "Predicates.not(" + this.predicate + ")";
        }
    }

    private static class AlwaysTruePredicate<T> implements Predicate<T> {
        private AlwaysTruePredicate() {
        }

        public boolean test(T t) {
            return true;
        }
    }
}
