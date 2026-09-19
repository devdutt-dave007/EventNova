// ============================================
// collections/EventCache.java
// ============================================
package collections;

import model.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventCache {
    private final List<Event> eventList = new ArrayList<>();
    private final Map<Integer, Event> eventMap = new HashMap<>();

    public void load(List<Event> events) {
        eventList.clear();
        eventMap.clear();
        for (Event e : events) {
            eventList.add(e);
            eventMap.put(e.getEventId(), e);
        }
    }

    public void add(Event event) {
        eventList.add(event);
        eventMap.put(event.getEventId(), event);
    }

    public Event getById(int eventId) { return eventMap.get(eventId); }
}