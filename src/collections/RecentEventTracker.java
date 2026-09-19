// ============================================
// collections/RecentEventTracker.java
// ============================================
package collections;

import model.Event;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class RecentEventTracker {
    private static final int MAX_RECENT = 5;
    private final Deque<Event> recentEvents = new ArrayDeque<>();

    public void trackNewEvent(Event event) {
        recentEvents.addFirst(event);
        if (recentEvents.size() > MAX_RECENT) recentEvents.removeLast();
    }

    public void printRecent() {
        if (recentEvents.isEmpty()) {
            System.out.println("No recent events.");
            return;
        }
        System.out.println("---- Recently Published Events ----");
        Iterator<Event> it = recentEvents.iterator();
        while (it.hasNext()) System.out.println(it.next());
    }
}