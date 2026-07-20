/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.beans;

import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;

/**
 * Models a getter and setter for an abstract property.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public class BeanProperty<T> {

    /**
     * the target class
     */
    private final Class<T> beanClass;
    /**
     * the property name
     */
    private final String propertyName;

    /**
     * <tt>true</tt> indicates the getter should simply reflect the value it is given
     */
    private final boolean identityProperty;

    /**
     * the value class
     */
    private Class<?> valueClass;

    /**
     * the chain of methods for the getter
     */
    private List<Method> getterChain;

    /**
     * the chain of methods for the setter
     */
    private List<Method> setterChain;

    /**
     * commonly used arguments
     */
    private static final Object[] EMPTY_ARGUMENTS = {};

    /**
     * Creates a new {@link BeanProperty} that gets the specified property from the
     * specified class.
     */
    public BeanProperty(Class<T> beanClass, String propertyName, boolean readable, boolean writable) {
        if (beanClass == null)
            throw new IllegalArgumentException("beanClass may not be null");
        if (propertyName == null)
            throw new IllegalArgumentException("propertyName may not be null");
        if (propertyName.isEmpty())
            throw new IllegalArgumentException("propertyName may not be empty");

        this.beanClass = beanClass;
        this.propertyName = propertyName;
        this.identityProperty = "this".equals(propertyName);

        if (identityProperty && writable)
            throw new IllegalArgumentException("The identity property name (this) cannot be writable");

        // look up the common chain
        final String[] propertyParts = propertyName.split("\\.");
        final List<Method> commonChain = new ArrayList<>(propertyParts.length);
        Class<?> currentClass = beanClass;
        for (int p = 0; p < propertyParts.length - 1; p++) {
            Method partGetter = findGetterMethod(currentClass, propertyParts[p]);
            commonChain.add(partGetter);
            currentClass = resolveType(currentClass, partGetter.getGenericReturnType(), partGetter.getReturnType());
        }

        // look up the final getter
        if (readable) {
            if (identityProperty) {
                valueClass = beanClass;
            }
            else {
                getterChain = new ArrayList<>();
                getterChain.addAll(commonChain);
                Method lastGetter = findGetterMethod(currentClass, propertyParts[propertyParts.length - 1]);
                getterChain.add(lastGetter);
                valueClass = resolveType(currentClass, lastGetter.getGenericReturnType(), lastGetter.getReturnType());
            }
        }

        // look up the final setter
        if (writable) {
            setterChain = new ArrayList<>();
            setterChain.addAll(commonChain);
            Method lastSetter = findSetterMethod(currentClass, propertyParts[propertyParts.length - 1]);
            setterChain.add(lastSetter);
            if (valueClass == null)
                valueClass = resolveType(currentClass, lastSetter.getGenericParameterTypes()[0], lastSetter.getParameterTypes()[0]);
        }
    }

    /**
     * Finds a getter of the specified property on the specified class.
     */
    private Method findGetterMethod(Class<?> targetClass, String property) {
        Method result;

        Class<?> currentClass = targetClass;
        while (currentClass != null) {
            String getProperty = "get" + capitalize(property);
            result = getMethod(currentClass, getProperty);
            if (result != null) {
                validateGetter(result);
                return result;
            }

            String isProperty = "is" + capitalize(property);
            result = getMethod(currentClass, isProperty);
            if (result != null) {
                validateGetter(result);
                return result;
            }
            currentClass = currentClass.getSuperclass();
        }

        throw new IllegalArgumentException("Failed to find getter for property \"" + property + "\" of " + targetClass);
    }

    /**
     * Finds a setter of the specified property on the specified class.
     */
    private Method findSetterMethod(Class<?> targetClass, String property) {
        String setProperty = "set" + capitalize(property);

        // loop through the class and its superclasses
        Class<?> currentClass = targetClass;
        while (currentClass != null) {

            // loop through this class' methods
            Method[] classMethods = currentClass.getMethods();
            for (Method classMethod : classMethods) {
                if (!classMethod.getName().equals(setProperty))
                    continue;
                if (classMethod.getParameterTypes().length != 1)
                    continue;
                validateSetter(classMethod);
                return classMethod;
            }
            currentClass = currentClass.getSuperclass();
        }

        throw new IllegalArgumentException("Failed to find setter for property \"" + property + "\" of " + targetClass);
    }

    /**
     * Validates that the specified method is okay for reflection. This throws an
     * exception if the method is invalid.
     */
    private void validateGetter(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException("Getter \"" + method + "\" is not public");
        }

        if (Void.TYPE.equals(method.getReturnType())) {
            throw new IllegalArgumentException("Getter \"" + method + "\" returns void");
        }

        if (method.getParameterTypes().length != 0) {
            throw new IllegalArgumentException("Getter \"" + method + "\" has too many parameters; expected 0 but found " + method.getParameterTypes().length);
        }
    }

    /**
     * Validates that the specified method is okay for reflection. This throws an
     * exception if the method is invalid.
     */
    private void validateSetter(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException("Setter \"" + method + "\" is not public");
        }

        if (method.getParameterTypes().length != 1) {
            throw new IllegalArgumentException("Setter \"" + method + "\" takes the wrong number of parameters; expected 1 but found " + method.getParameterTypes().length);
        }
    }


    /**
     * Returns the specified property with a capitalized first character.
     */
    private String capitalize(String property) {
        return Character.toUpperCase(property.charAt(0)) + property.substring(1);
    }

    /**
     * Gets the method with the specified name and arguments.
     */
    private Method getMethod(Class<?> targetClass, String methodName) {
        try {
            return targetClass.getMethod(methodName);
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Gets the base class that this getter accesses.
     */
    public Class<T> getBeanClass() {
        return beanClass;
    }

    /**
     * Gets the name of the property that this getter extracts.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Gets the class of the property's value. This is the return type and not
     * necessarily the runtime type of the class.
     */
    public Class<?> getValueClass() {
        return valueClass;
    }

    private static Class<?> resolveType(Class<?> context, Type genericType, Class<?> erasedType) {
        final Class<?> resolvedType = TypeUtils.getRawType(genericType, context);
        return resolvedType != null ? resolvedType : erasedType;
    }

    /**
     * Gets whether this property can get get.
     */
    public boolean isReadable() {
        return getterChain != null || identityProperty;
    }

    /**
     * Gets whether this property can be set.
     */
    public boolean isWritable() {
        return setterChain != null;
    }

    /**
     * Gets the value of this property for the specified Object.
     */
    public Object get(T member) {
        if (!isReadable())
            throw new IllegalStateException("Property " + propertyName + " of " + beanClass + " not readable");

        // the identity property simply reflects the member back unchanged
        if (identityProperty)
            return member;

        try {
            // do all the getters in sequence
            Object currentMember = member;
            for (Method currentMethod : getterChain) {
                currentMember = currentMethod.invoke(currentMember, EMPTY_ARGUMENTS);
                if (currentMember == null)
                    return null;
            }

            // return the result of the last getter
            return currentMember;
        }
        catch (IllegalAccessException e) {
            throw new SecurityException(null, e);
        }
        catch (InvocationTargetException e) {
            throw new UndeclaredThrowableException(e.getCause());
        }
    }

    /**
     * Gets the value of this property for the specified Object.
     */
    public Object set(T member, Object newValue) {
        if (!isWritable())
            throw new IllegalStateException("Property " + propertyName + " of " + beanClass + " not writable");

        Method setterMethod = null;
        try {
            // everything except the last setter chain element is a getter
            Object currentMember = member;
            for (int i = 0, n = setterChain.size() - 1; i < n; i++) {
                Method currentMethod = setterChain.get(i);
                currentMember = currentMethod.invoke(currentMember, EMPTY_ARGUMENTS);
                if (currentMember == null)
                    return null;
            }

            // do the remaining setter
            setterMethod = setterChain.getLast();
            return setterMethod.invoke(currentMember, newValue);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(getSetterErrorMessage(e, setterMethod, newValue));
        }
        catch (IllegalAccessException e) {
            throw new SecurityException(null, e);
        }
        catch (InvocationTargetException e) {
            throw new UndeclaredThrowableException(e.getCause());
        }
        catch (RuntimeException e) {
            throw new RuntimeException("Failed to set property \"" + propertyName + "\" of " + beanClass + " to " + (newValue == null ? "null" : "instance of " + newValue.getClass()), e);
        }
    }

    private static String getSetterErrorMessage(IllegalArgumentException exception, Method setterMethod, Object newValue) {
        final String message = exception.getMessage();
        if (!"argument type mismatch".equals(message) || setterMethod == null)
            return message;

        // improve the message into something like:
        // "MyClass.someMethod(SomeClassType) cannot be called with an instance of WrongClassType"
        return getSimpleName(setterMethod.getDeclaringClass()) + "." + setterMethod.getName()
                + "(" + getSimpleName(setterMethod.getParameterTypes()[0])
                + ") cannot be called with an instance of " + getSimpleName(newValue.getClass());
    }

    /**
     * Returns the simple name of the given class as given in the
     * source code. Returns an empty string if the underlying class is
     * anonymous.
     *
     * @return the simple name of the given class
     */
    private static String getSimpleName(Class<?> clazz) {
        return clazz.getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final BeanProperty<?> that = (BeanProperty<?>) o;

        if (!beanClass.equals(that.beanClass))
            return false;
        return propertyName.equals(that.propertyName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result;
        result = beanClass.hashCode();
        result = 29 * result + propertyName.hashCode();
        return result;
    }
}
