package com.tsystems.javaschool.uberbahn.webmain.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

@Entity
@Table(name = "station")
public class Station extends BaseEntity {


    @OneToMany (cascade = CascadeType.ALL, mappedBy = "station")
    private Collection<Spot> spots;

    @OneToMany (cascade = CascadeType.ALL, mappedBy = "stationOfDeparture")
    private Collection<Ticket> departures;

    @OneToMany (cascade = CascadeType.ALL, mappedBy = "stationOfArrival")
    private Collection<Ticket> arrivals;

    @Column(name = "title")
    @OrderBy("title ASC")
    private String title;

    @Column(name = "timeZone")
    private double timeZone;

    public Collection<Spot> getSpots() {
        return spots;
    }

    public Collection<Ticket> getDepartures() {
        return departures;
    }

    public Collection<Ticket> getArrivals() {
        return arrivals;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSpots(Collection<Spot> spots) {
        this.spots = spots;
    }

    public void setDepartures(Collection<Ticket> departures) {
        this.departures = departures;
    }

    public void setArrivals(Collection<Ticket> arrivals) {
        this.arrivals = arrivals;
    }

    public double getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(double timeZone) {
        this.timeZone = timeZone;
    }
}
