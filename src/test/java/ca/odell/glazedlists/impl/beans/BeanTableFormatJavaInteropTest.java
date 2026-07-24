/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.odell.glazedlists.impl.beans;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanTableFormatJavaInteropTest {

    @Test
    void subclassRetainsProtectedStateAndDescriptorHook() {
        final ExposedBeanTableFormat format = new ExposedBeanTableFormat();
        final Bean bean = new Bean();

        assertEquals(1, format.loadCount);
        assertNotNull(format.beanPropertiesArray());
        assertEquals("value", format.propertyNamesArray()[0]);
        assertEquals("Value", format.columnLabelsArray()[0]);
        assertNotNull(format.comparatorsArray());
        assertEquals(String.class, format.classesArray()[0]);
        assertEquals("initial", format.getColumnValue(bean, 0));
        assertEquals(bean, format.setColumnValue(bean, "changed", 0));
        assertEquals("changed", bean.getValue());
    }

    @Test
    void nullableEditedValuesRemainSupported() {
        final ExposedBeanTableFormat format = new ExposedBeanTableFormat();
        final Bean bean = new Bean();

        assertEquals(bean, format.setColumnValue(bean, null, 0));
        assertNull(bean.getValue());
    }

    @Test
    void protectedPrimitiveMapRemainsImmutable() {
        final var primitiveMap = ExposedBeanTableFormat.primitiveMap();

        assertEquals(Integer.class, primitiveMap.get(Integer.TYPE));
        assertThrows(UnsupportedOperationException.class, () -> primitiveMap.put(String.class, String.class));
    }

    private static final class ExposedBeanTableFormat extends BeanTableFormat<Bean> {
        private int loadCount;

        private ExposedBeanTableFormat() {
            super(Bean.class, new String[]{"value"}, new String[]{"Value"}, new boolean[]{true});
        }

        @Override
        protected void loadPropertyDescriptors(@NonNull Class<Bean> beanClass) {
            loadCount++;
            super.loadPropertyDescriptors(beanClass);
        }

        private BeanProperty<Bean>[] beanPropertiesArray() {
            return beanProperties;
        }

        private String[] propertyNamesArray() {
            return propertyNames;
        }

        private String[] columnLabelsArray() {
            return columnLabels;
        }

        private java.util.Comparator<?>[] comparatorsArray() {
            return comparators;
        }

        private Class<?>[] classesArray() {
            return classes;
        }

        private static java.util.Map<Class<?>, Class<?>> primitiveMap() {
            return primitiveToObjectMap;
        }
    }

    public static final class Bean {
        private String value = "initial";

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
