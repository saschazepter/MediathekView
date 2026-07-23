package ca.odell.glazedlists;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdListJavaInteropTest {
    @Test
    void retainsConstructorsPropertiesAndNestedEvaluatorSurface() throws Exception {
        final Constructor<?> beanConstructor = ThresholdList.class.getConstructor(EventList.class, String.class);
        final Constructor<?> evaluatorConstructor = ThresholdList.class.getConstructor(EventList.class, ThresholdList.Evaluator.class);
        assertTrue(Modifier.isPublic(beanConstructor.getModifiers()));
        assertTrue(Modifier.isPublic(evaluatorConstructor.getModifiers()));
        assertEquals(2, Arrays.stream(ThresholdList.class.getDeclaredConstructors()).filter(c -> Modifier.isPublic(c.getModifiers())).count());

        final Method evaluate = ThresholdList.Evaluator.class.getDeclaredMethod("evaluate", Object.class);
        final Method applyAsInt = ThresholdList.Evaluator.class.getDeclaredMethod("applyAsInt", Object.class);
        assertEquals(int.class, evaluate.getReturnType());
        assertEquals(int.class, applyAsInt.getReturnType());
        assertTrue(Modifier.isAbstract(evaluate.getModifiers()));
        assertFalse(Modifier.isAbstract(applyAsInt.getModifiers()));
        assertSame(ThresholdList.class, ThresholdList.Evaluator.class.getEnclosingClass());
        assertTrue(Modifier.isPublic(ThresholdList.Evaluator.class.getModifiers()));
        assertTrue(Modifier.isInterface(ThresholdList.Evaluator.class.getModifiers()));
    }

    @Test
    void evaluatorRemainsAJavaSamWithToIntFunctionDelegation() {
        final ThresholdList.Evaluator<String> evaluator = String::length;

        assertEquals(4, evaluator.evaluate("four"));
        assertEquals(5, apply(evaluator));
    }

    @Test
    void propertyAndEvaluatorConstructorsRemainUsableFromJava() {
        final BasicEventList<BeanScore> source = new BasicEventList<>();
        source.addAll(Arrays.asList(new BeanScore(30), new BeanScore(10), new BeanScore(20)));

        try (source;
             ThresholdList<BeanScore> beanThreshold = new ThresholdList<>(source, "score")) {
            beanThreshold.setLowerThreshold(15);
            beanThreshold.setUpperThreshold(25);
            assertEquals(List.of(20), beanThreshold.stream().map(BeanScore::getScore).toList());
        }

        final BasicEventList<Integer> integerSource = new BasicEventList<>();
        integerSource.addAll(Arrays.asList(30, 10, 20));
        final ThresholdList.Evaluator<Integer> evaluator = value -> value;
        try (integerSource;
             ThresholdList<Integer> threshold = new ThresholdList<>(integerSource, evaluator)) {
            assertSame(evaluator, threshold.getEvaluator());
            threshold.setLowerThreshold(10);
            threshold.setUpperThreshold(20);
            assertEquals(10, threshold.getLowerThreshold());
            assertEquals(20, threshold.getUpperThreshold());
            assertEquals(Arrays.asList(10, 20), threshold);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void packageComparatorRetainsDualModeEqualityAndOverflowSafeOrdering() {
        final ThresholdList.Evaluator<BeanScore> evaluator = BeanScore::getScore;
        final Comparator comparator = new ThresholdList.ThresholdComparator<>(evaluator);
        final Comparator equalComparator = new ThresholdList.ThresholdComparator<>(evaluator);

        assertTrue(comparator.compare(Integer.MIN_VALUE, Integer.MAX_VALUE) < 0);
        assertTrue(comparator.compare(new BeanScore(5), 10) < 0);
        assertTrue(comparator.compare(10, new BeanScore(5)) > 0);
        assertEquals(comparator, equalComparator);
        assertEquals(comparator.hashCode(), equalComparator.hashCode());
        assertTrue(Modifier.isFinal(ThresholdList.ThresholdComparator.class.getModifiers()));
        assertSame(ThresholdList.class, ThresholdList.ThresholdComparator.class.getEnclosingClass());
    }

    public static final class BeanScore {
        private final int score;

        BeanScore(int score) {
            this.score = score;
        }

        public int getScore() {
            return score;
        }
    }

    private static int apply(ToIntFunction<String> function) {
        return function.applyAsInt("three");
    }
}
