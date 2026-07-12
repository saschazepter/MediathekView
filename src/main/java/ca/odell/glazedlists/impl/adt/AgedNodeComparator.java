/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.adt;

// to implement the Comparator interface
import java.util.Comparator;

/**
 * A Comparator for sorting AgedNodes
 *
 * @author <a href="mailto:kevin@swank.ca">Kevin Maltby</a>
 */
public final class AgedNodeComparator implements Comparator<AgedNode> {

    @Override
    public final int compare(AgedNode node1, AgedNode node2) {
        long difference = node1.getTimestamp() - node2.getTimestamp();
        if(difference < 0) {
            return -1;
        } else if(difference > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
