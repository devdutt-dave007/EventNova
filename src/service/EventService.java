// ============================================
// service/EventService.java
// ============================================
package service;

import collections.EventCache;
import collections.RecentEventTracker;
import dao.EventDAO;
import exception.InvalidDateException;
import model.Event;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventService {

    private final EventDAO eventDAO = new EventDAO();
    private final EventCache eventCache = new EventCache();
    private final RecentEventTracker recentEventTracker = new RecentEventTracker();

    public int createEvent(Event event) throws SQLException, InvalidDateException {
        validateEventDates(event);
        int id = eventDAO.createEvent(event);
        eventCache.add(event);
        recentEventTracker.trackNewEvent(event);
        return id;
    }

    private void validateEventDates(Event event) throws InvalidDateException {
        LocalDate deadlineDate = event.getRegistrationDeadline().toLocalDate();
        if (!event.getEventDate().isAfter(deadlineDate)) {
            throw new InvalidDateException("Event Date (" + event.getEventDate() +
                    ") must be AFTER the Registration Deadline date (" + deadlineDate + ").");
        }
    }

    public boolean updateEvent(Event event) throws SQLException, InvalidDateException {
        validateEventDates(event);
        return eventDAO.updateEvent(event);
    }

    public boolean deleteEvent(int eventId) throws SQLException {
        return eventDAO.deleteEvent(eventId);
    }

    public boolean closeRegistration(int eventId) throws SQLException {
        return eventDAO.updateStatus(eventId, Event.Status.CLOSED);
    }

    public Event getById(int eventId) throws SQLException {
        Event cached = eventCache.getById(eventId);
        if (cached != null) return cached;
        return eventDAO.getById(eventId);
    }

    public List<Event> searchByName(String keyword) throws SQLException { return eventDAO.searchByName(keyword); }
    public List<Event> searchByCategory(Event.Category category) throws SQLException { return eventDAO.searchByCategory(category); }
    public List<Event> searchByDate(LocalDate date) throws SQLException { return eventDAO.searchByDate(date); }
    public List<Event> searchByInstitute(int instituteId) throws SQLException { return eventDAO.searchByInstitute(instituteId); }
    public List<Event> searchByFeeStatus(boolean free) throws SQLException { return eventDAO.searchByFeeStatus(free); }
    public List<Event> getByInstitute(int instituteId) throws SQLException { return eventDAO.searchByInstitute(instituteId); }

    public List<Event> getSortedByDate() throws SQLException {
        List<Event> events = new ArrayList<>(eventDAO.getAll());
        Collections.sort(events); // Event.compareTo — real polymorphism in action
        return events;
    }

    public int getConfirmedCount(int eventId) throws SQLException {
        return eventDAO.getConfirmedRegistrationCount(eventId);
    }

    public void printRecentEvents() {
        recentEventTracker.printRecent();
    }
    // service/EventService.java — add this method
    public int getActiveRegistrationCount(int eventId) throws SQLException {
        return eventDAO.getActiveRegistrationCount(eventId);
    }
}