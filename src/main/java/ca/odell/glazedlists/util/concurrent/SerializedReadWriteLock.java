/* Glazed Lists                                                 (c) 2003-2007 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.util.concurrent;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A ReadWriteLock dummy implementation that's only used for Java object
 * serialization. Lock implementations are represented by this class on the
 * serialization stream and reconstructed as a {@link ReentrantReadWriteLock}
 * when deserialized.
 *
 * @author Holger Brands
 */
public final class SerializedReadWriteLock implements ReadWriteLock, Serializable {

    /** For versioning as a {@link Serializable} */
    private static final long serialVersionUID = -8627867501684280198L;

    /** {@inheritDoc} */
    @Override
    public Lock readLock() {
        throw new UnsupportedOperationException("SerializedReadWriteLock is only used for serialization");
    }

    /** {@inheritDoc} */
    @Override
    public Lock writeLock() {
        throw new UnsupportedOperationException("SerializedReadWriteLock is only used for serialization");
    }

    /** Recreate an appropriate lock implementation when deserialized on the target JVM. */
    private Object readResolve() throws ObjectStreamException {
        return new ReentrantReadWriteLock();
    }
}
