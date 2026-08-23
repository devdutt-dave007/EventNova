// ============================================
// collections/CustomLinkedList.java
// Change #3: hand-built singly linked list (mandatory custom DS),
// used as the FIFO backing structure for the waiting list — a genuinely
// unbounded, growable structure, unlike a fixed array.
// ============================================
package collections;
import java.util.NoSuchElementException;
public class CustomLinkedList<T>
{

    private static class Node<T>
    {
        T data;
        Node<T> next;
        Node(T data)
        {
            this.data = data;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public void addLast(T value)
    {
        Node<T> newNode = new Node<>(value);
        if (head == null)
        {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    public T removeFirst() {
        if (head == null) {
            throw new NoSuchElementException("CustomLinkedList is empty.");
        }
        T value = head.data;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return value;
    }

    public boolean isEmpty() { return head == null; }
    public int size() { return size; }
}