package ca.odell.glazedlists.impl.swing;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.ThresholdList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThresholdRangeModelsJavaInteropTest {
    @Test
    void rangeModelsRemainSubclassableFromJava() {
        ThresholdList<Integer> target = new ThresholdList<>(new BasicEventList<>(), value -> value);
        target.setLowerThreshold(10);
        target.setUpperThreshold(20);
        JavaLowerModel lower = new JavaLowerModel(target);
        JavaUpperModel upper = new JavaUpperModel(target);

        lower.setRangeProperties(12, 0, 0, 20, false);
        upper.setRangeProperties(18, 0, 12, 30, false);

        assertEquals(12, lower.getValue());
        assertEquals(18, upper.getValue());
        target.dispose();
    }

    private static final class JavaLowerModel extends LowerThresholdRangeModel {
        private JavaLowerModel(ThresholdList<?> target) {
            super(target);
        }

        @Override
        public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
            super.setRangeProperties(value, extent, min, max, adjusting);
        }
    }

    private static final class JavaUpperModel extends UpperThresholdRangeModel {
        private JavaUpperModel(ThresholdList<?> target) {
            super(target);
        }

        @Override
        public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
            super.setRangeProperties(value, extent, min, max, adjusting);
        }
    }
}
