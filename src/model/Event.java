// ============================================
// model/Event.java
// Change #1: Category is now a fixed enum
// ============================================
package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Event implements Comparable<Event> {

    public enum Category { FEST, MUSIC, DANCE, HACKATHON, SEMINAR, TECH_FEST, DRAMA, SPORTS }
    public enum ParticipationType { SOLO, TEAM }
    public enum Status { OPEN, CLOSED, COMPLETED, CANCELLED }
    public enum Eligibility { STUDENT_NATIVE, FACULTY_NATIVE, EXTERNAL, ALL }

    private final int eventId;
    private final int instituteId;
    private final String eventName;
    private final Category category;
    private final Eligibility eligibility;
    private final ParticipationType participationType;
    private final String description;
    private final String venue;
    private final int capacity;
    private final double ticketPrice;
    private final LocalDate eventDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final LocalDateTime registrationDeadline;
    private final Status status;

    public Event(int eventId, int instituteId, String eventName, Category category,
                 Eligibility eligibility, ParticipationType participationType, String description,
                 String venue, int capacity, double ticketPrice, LocalDate eventDate,
                 LocalTime startTime, LocalTime endTime, LocalDateTime registrationDeadline,
                 Status status) {
        this.eventId = eventId;
        this.instituteId = instituteId;
        this.eventName = eventName;
        this.category = category;
        this.eligibility = eligibility;
        this.participationType = participationType;
        this.description = description;
        this.venue = venue;
        this.capacity = capacity;
        this.ticketPrice = ticketPrice;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.registrationDeadline = registrationDeadline;
        this.status = status;
    }

    public int getEventId() { return eventId; }
    public int getInstituteId() { return instituteId; }
    public String getEventName() { return eventName; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Eligibility getEligibility() { return eligibility; }
    public ParticipationType getParticipationType() { return participationType; }
    public String getVenue() { return venue; }
    public int getCapacity() { return capacity; }
    public double getTicketPrice() { return ticketPrice; }
    public LocalDate getEventDate() { return eventDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public Status getStatus() { return status; }

    public boolean isFree() { return ticketPrice == 0.0; }

    // Real runtime polymorphism example: enables Collections.sort(events)
    @Override
    public int compareTo(Event other) {
        return this.eventDate.compareTo(other.eventDate);
    }

    @Override
    public String toString() {
        return "Event_ID: " + eventId + " | " + eventName +
                " | Category: " + category + " | Eligibility: " + eligibility +
                " | Venue: " + venue + " | Date: " + eventDate +
                " | Price: " + (isFree() ? "FREE" : "Rs." + ticketPrice) +
                " | Status: " + status;
    }
}
