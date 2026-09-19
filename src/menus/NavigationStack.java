// ============================================
// menus/NavigationStack.java
// ============================================
package menus;

import java.util.Stack;

public class NavigationStack {
    private final Stack<String> history = new Stack<>();
    public void push(String menuName) { history.push(menuName); }
    public String back() { return history.isEmpty() ? "MAIN_MENU" : history.pop(); }
}