package edu.nd.crepe.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CollectorTest {

    private static final long HOUR = 60L * 60 * 1000;

    private static Collector collector(long start, long end, String status) {
        return new Collector("c1", "u1", "App", "com.example.app", "description", "mode", start, end, status);
    }

    @Test
    public void autoSetStatus_beforeStart_isNotYetStarted() {
        long now = System.currentTimeMillis();
        Collector collector = collector(now + HOUR, now + 2 * HOUR, null);

        assertTrue(collector.autoSetCollectorStatus());
        assertEquals(Collector.NOTYETSTARTED, collector.getCollectorStatus());
    }

    @Test
    public void autoSetStatus_insideTimeFrame_isActive() {
        long now = System.currentTimeMillis();
        Collector collector = collector(now - HOUR, now + HOUR, null);

        assertTrue(collector.autoSetCollectorStatus());
        assertEquals(Collector.ACTIVE, collector.getCollectorStatus());
    }

    @Test
    public void autoSetStatus_afterEnd_isExpired() {
        long now = System.currentTimeMillis();
        Collector collector = collector(now - 2 * HOUR, now - HOUR, Collector.ACTIVE);

        assertTrue(collector.autoSetCollectorStatus());
        assertEquals(Collector.EXPIRED, collector.getCollectorStatus());
    }

    @Test
    public void autoSetStatus_reportsWhetherTheStatusChanged() {
        long now = System.currentTimeMillis();
        Collector collector = collector(now - HOUR, now + HOUR, Collector.ACTIVE);

        assertFalse(collector.autoSetCollectorStatus());
    }

    @Test
    public void isDeleted_withoutStatus_isFalse() {
        // used to throw a NullPointerException for collectors that were never given a status
        assertFalse(new Collector("c1").isDeleted());
    }

    @Test
    public void isDeleted_withDeletedStatus_isTrue() {
        assertTrue(collector(0, 1, Collector.DELETED).isDeleted());
    }

    @Test
    public void compareWith_identicalCollectors_hasNoChange() {
        assertEquals(Collector.ChangeStatus.NO_CHANGE,
                collector(0, 10, Collector.ACTIVE).compareWith(collector(0, 10, Collector.ACTIVE)));
    }

    @Test
    public void compareWith_reportsTheFirstDifference() {
        Collector original = collector(0, 10, Collector.ACTIVE);

        Collector newDescription = collector(0, 10, Collector.ACTIVE);
        newDescription.setDescription("something else");
        assertEquals(Collector.ChangeStatus.DESCRIPTION_CHANGE, original.compareWith(newDescription));

        assertEquals(Collector.ChangeStatus.COLLECTOR_START_TIME_CHANGE, original.compareWith(collector(1, 10, Collector.ACTIVE)));
        assertEquals(Collector.ChangeStatus.COLLECTOR_END_TIME_CHANGE, original.compareWith(collector(0, 11, Collector.ACTIVE)));
        assertEquals(Collector.ChangeStatus.COLLECTOR_STATUS_CHANGE, original.compareWith(collector(0, 10, Collector.EXPIRED)));
    }

    @Test
    public void compareWith_toleratesMissingFields() {
        // collectors read from firebase can lack a description or a status
        Collector first = new Collector("c1");
        Collector second = new Collector("c1");

        assertEquals(Collector.ChangeStatus.NO_CHANGE, first.compareWith(second));
    }

    @Test
    public void compareWith_differentCollector_returnsNull() {
        Collector other = new Collector("c2", "u1", "App", "com.example.app", "description", "mode", 0, 10, Collector.ACTIVE);

        assertNull(collector(0, 10, Collector.ACTIVE).compareWith(other));
    }

    @Test
    public void setCollectorStatus_ignoresUnknownAndNullValues() {
        Collector collector = collector(0, 10, Collector.ACTIVE);

        collector.setCollectorStatus("not-a-status");
        collector.setCollectorStatus(null);

        assertEquals(Collector.ACTIVE, collector.getCollectorStatus());
    }

    @Test
    public void dateStrings_areFormattedInUtc() {
        Collector collector = collector(0, 24 * HOUR, Collector.ACTIVE);

        assertEquals("01/01/1970", collector.getCollectorStartTimeString());
        assertEquals("01/02/1970", collector.getCollectorEndTimeString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringTimeConstructor_rejectsNonNumericTimes() {
        new Collector("c1", "u1", "App", "com.example.app", "description", "mode", null, "tomorrow", "10");
    }
}
