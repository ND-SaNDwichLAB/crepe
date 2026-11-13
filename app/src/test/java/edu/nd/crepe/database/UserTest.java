package edu.nd.crepe.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class UserTest {

    private static User user() {
        return new User("u1", "Name", "", 1L, 2L);
    }

    @Test
    public void constructor_withoutCollectorList_startsEmpty() {
        assertTrue(user().getCollectorsForCurrentUser().isEmpty());
    }

    @Test
    public void constructor_withNullCollectorList_startsEmpty() {
        // used to throw a NullPointerException when the stored list was missing
        User user = new User("u1", "Name", "", 1L, 2L, null);

        assertNotNull(user.getCollectorsForCurrentUser());
        assertTrue(user.getCollectorsForCurrentUser().isEmpty());
    }

    @Test
    public void setCollectors_withNull_keepsAnEmptyList() {
        User user = user();

        user.setCollectorsForCurrentUser(null);

        assertNotNull(user.getCollectorsForCurrentUser());
        assertTrue(user.getCollectorsForCurrentUser().isEmpty());
    }

    @Test
    public void addCollector_ignoresDuplicatesAndNull() {
        User user = user();

        user.addCollectorForCurrentUser("c1");
        user.addCollectorForCurrentUser("c1");
        user.addCollectorForCurrentUser(null);

        assertEquals(new ArrayList<>(Arrays.asList("c1")), user.getCollectorsForCurrentUser());
    }

    @Test
    public void removeCollector_removesItByCollectorId() {
        User user = user();
        user.addCollectorForCurrentUser("c1");
        user.addCollectorForCurrentUser("c2");

        // the list holds ids, this used to try (and fail) to remove the Collector object itself
        user.removeCollectorForCurrentUser(new Collector("c1"));

        assertEquals(new ArrayList<>(Arrays.asList("c2")), user.getCollectorsForCurrentUser());
    }
}
