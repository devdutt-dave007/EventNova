// ============================================
// collections/WaitingListManager.java
// Now backed by CustomLinkedList instead of java.util.LinkedList
// ============================================
package collections;

import java.util.HashMap;
import java.util.Map;

public class WaitingListManager {

    private final Map<Integer, CustomLinkedList<Integer>> waitingLists = new HashMap<>();

    public void addToWaitingList(int eventId, int userId) {
        waitingLists.putIfAbsent(eventId, new CustomLinkedList<>());
        waitingLists.get(eventId).addLast(userId);
    }

    public Integer promoteNext(int eventId) {
        CustomLinkedList<Integer> queue = waitingLists.get(eventId);
        if (queue == null || queue.isEmpty()) return null;
        return queue.removeFirst();
    }

    public boolean hasWaitingUsers(int eventId) {
        CustomLinkedList<Integer> queue = waitingLists.get(eventId);
        return queue != null && !queue.isEmpty();
    }
}