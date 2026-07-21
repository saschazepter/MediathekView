/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * A factory for creating Sequencers.
 *
 * @author James Lemieux
 */
public final class Sequencers {

    /**
     * A dummy constructor to prevent instantiation of this class
     */
    private Sequencers() {
        throw new UnsupportedOperationException();
    }

    // Sequencers // // // // // // // // // // // // // // // // // // // // //

    public static SequenceList.Sequencer<Date> monthSequencer() {
        return new MonthSequencer();
    }

    /**
     * This Sequencer produces a sequence of {@link Date} objects normalized
     * to the first millisecond of each month.
     */
    private static final class MonthSequencer implements SequenceList.Sequencer<Date> {
        private final ZoneId zoneId = ZoneId.systemDefault();

        /**
         * The previous month in the sequence. For example:
         *
         * <ul>
         *   <li> previous(February 15, 2006 3:21:22.234) returns February 1, 2006 0:00:00.000
         *   <li> previous(February 1, 2006 0:00:00.000) returns January 1, 2006 0:00:00.000
         *   <li> previous(January 1, 2006 0:00:00.000) returns December 1, 2005 0:00:00.000
         * </ul>
         */
        @Override
        public Date previous(Date date) {
            if (date == null) {
                throw new IllegalArgumentException("date may not be null");
            }

            ZonedDateTime dateTime = date.toInstant().atZone(zoneId);
            if (dateTime.getDayOfMonth() == 1 && dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
                dateTime = dateTime.minusMonths(1);
            }

            return monthStart(dateTime);
        }

        /**
         * The next month in the sequence. For example:
         *
         * <ul>
         *   <li> next(November 15, 2005 3:21:22.234) returns December 1, 2005 0:00:00.000
         *   <li> next(December 1, 2005 0:00:00.000) returns January 1, 2006 0:00:00.000
         *   <li> next(January 1, 2006 0:00:00.000) returns February 1, 2006 0:00:00.000
         * </ul>
         */
        @Override
        public Date next(Date date) {
            if (date == null) {
                throw new IllegalArgumentException("date may not be null");
            }

            return monthStart(date.toInstant().atZone(zoneId).plusMonths(1));
        }

        private Date monthStart(ZonedDateTime dateTime) {
            final LocalDateTime monthStart = dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay();
            final List<ZoneOffset> validOffsets = zoneId.getRules().getValidOffsets(monthStart);
            final ZoneOffset preferredOffset = validOffsets.isEmpty() ? null : validOffsets.getLast();
            return Date.from(ZonedDateTime.ofLocal(monthStart, zoneId, preferredOffset).toInstant());
        }
    }
}