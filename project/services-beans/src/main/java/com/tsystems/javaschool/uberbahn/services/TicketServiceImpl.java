package com.tsystems.javaschool.uberbahn.services;


import com.tsystems.javaschool.uberbahn.entities.*;
import com.tsystems.javaschool.uberbahn.repositories.*;
import com.tsystems.javaschool.uberbahn.services.errors.BusinessLogicException;
import com.tsystems.javaschool.uberbahn.services.errors.DatabaseException;
import com.tsystems.javaschool.uberbahn.transports.TicketInfo;
import com.tsystems.javaschool.uberbahn.transports.TicketReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TrainRepository trainRepository;
    private final AccountRepository accountRepository;
    private final TicketRepository ticketRepository;
    private final PresenceRepository presenceRepository;
    private final SpotRepository spotRepository;

    @Autowired
    public TicketServiceImpl(TrainRepository trainRepository, AccountRepository accountRepository, TicketRepository ticketRepository, PresenceRepository presenceRepository, SpotRepository spotRepository) {
        this.trainRepository = trainRepository;
        this.accountRepository = accountRepository;
        this.ticketRepository = ticketRepository;
        this.presenceRepository = presenceRepository;
        this.spotRepository = spotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public int countTicketsAvailable(int trainId, int stationOfDepartureId, int stationOfArrivalId) {
        Train train = trainRepository.findOne(trainId);
        Collection<Presence> presences = train.getPresences();
        int ticketsAvailable = train.getNumberOfSeats();
        boolean isDeparturePassed = false;
        boolean isArrivalNotPassed = true;
        for (Presence presence : presences) {
            if (presence.getSpot().getStation().getId() == stationOfDepartureId) {
                isDeparturePassed = true;
            }
            if (presence.getSpot().getStation().getId() == stationOfArrivalId) {
                isArrivalNotPassed = false;
            }
            if (isDeparturePassed && isArrivalNotPassed) {
                ticketsAvailable = Math.min(train.getNumberOfSeats() - presence.getNumberOfTicketsPurchased(), ticketsAvailable);
            }
        }
        return ticketsAvailable;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketInfo> getTicketInfos(int accountId, LocalDateTime since, LocalDateTime until) {
        List<TicketInfo> ticketInfos = ticketRepository.getByAccountIdSinceAndUntil(accountId, since, until).stream().map(ticket -> getTicketInfo(ticket)).collect(Collectors.toList());
        Collections.sort(ticketInfos, (TicketInfo t1, TicketInfo t2) ->
                t2.getDateOfDeparture().atTime(t2.getTimeOfDeparture())
                        .compareTo(t1.getDateOfDeparture().atTime(t1.getTimeOfDeparture())));
        return ticketInfos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketReport> getTicketInfos(LocalDateTime since, LocalDateTime until) {
        return ticketRepository.getBySinceAndUntil(since, until).stream()
                .map(ticket -> getTicketReport(ticket)).collect(Collectors.toList());
    }

    @Override
    public TicketInfo create(int trainId, int stationOfDepartureId, int stationOfArrivalId, String firstName, String lastName, LocalDate dateOfBirth, String accountLogin) {

        checkFields(trainId, stationOfDepartureId, stationOfArrivalId, firstName, lastName, dateOfBirth);

        Train train = trainRepository.findOne(trainId);
        Presence presenceDeparture = presenceRepository.findByTrainIdAndStationId(trainId, stationOfDepartureId);
        Presence presenceArrival = presenceRepository.findByTrainIdAndStationId(trainId, stationOfArrivalId);
        Instant datetimeOfPurchase = LocalDateTime.now().toInstant(ZoneOffset.ofHours(presenceDeparture.getSpot().getStation().getTimezone()));
        int minutesDeparture = presenceDeparture.getSpot().getMinutesSinceDeparture();
        int minutesArrival = presenceArrival.getSpot().getMinutesSinceDeparture();

        Account account = accountRepository.findByLogin(accountLogin);

        Ticket ticket = new Ticket();
        ticket.setTrain(train);
        ticket.setFirstName(firstName);
        ticket.setLastName(lastName);
        ticket.setDateOfBirth(dateOfBirth);
        ticket.setStationOfDeparture(presenceDeparture.getSpot().getStation());
        ticket.setStationOfArrival(presenceArrival.getSpot().getStation());
        ticket.setDatetimeOfPurchase(LocalDateTime.ofInstant(datetimeOfPurchase, ZoneOffset.ofHours(presenceDeparture.getSpot().getStation().getTimezone())));
        ticket.setAccount(account);
        BigDecimal price = (train.getRoute().getPricePerMinute()
                .multiply(BigDecimal.valueOf(train.getPriceCoefficient()))
                .multiply(new BigDecimal(minutesArrival - minutesDeparture))).setScale(2, BigDecimal.ROUND_HALF_UP);
        ticket.setPrice(price);

        try {
            ticketRepository.save(ticket);
        } catch (PersistenceException ex) {
            throw new DatabaseException("Error occurred", ex);
        }
        savePresences(trainId, stationOfDepartureId, stationOfArrivalId);

        return getTicketInfo(ticket);
    }


    @Override
    @Transactional(readOnly = true)
    public TicketInfo getTicketInfo(int ticketId) {
        Ticket ticket = ticketRepository.findOne(ticketId);
        return getTicketInfo(ticket);
    }

    @Transactional(readOnly = true)
    public TicketInfo getTicketInfo(Ticket ticket) {
        TicketInfo ticketInfo = new TicketInfo();
        ticketInfo.setId(ticket.getId());
        ticketInfo.setTrainId(ticket.getTrain().getId());
        ticketInfo.setStationOfDeparture(ticket.getStationOfDeparture().getTitle());
        Spot spotDeparture = spotRepository.findByStationIdAndRouteId(ticket.getStationOfDeparture().getId(), ticket.getTrain().getRoute().getId());
        Spot spotArrival = spotRepository.findByStationIdAndRouteId(ticket.getStationOfArrival().getId(), ticket.getTrain().getRoute().getId());
        LocalDateTime datetimeDeparture = ticket.getTrain().getDateOfDeparture()
                .atTime(ticket.getTrain().getRoute().getTimeOfDeparture())
                .plus(spotDeparture.getMinutesSinceDeparture(), ChronoUnit.MINUTES);
        LocalDateTime datetimeArrival = datetimeDeparture.plus((long)spotArrival.getMinutesSinceDeparture() - spotDeparture.getMinutesSinceDeparture(), ChronoUnit.MINUTES);
        ticketInfo.setDateOfDeparture(datetimeDeparture.toLocalDate());
        ticketInfo.setTimeOfDeparture(datetimeDeparture.toLocalTime());
        ticketInfo.setStationOfArrival(ticket.getStationOfArrival().getTitle());
        ticketInfo.setDateOfArrival(datetimeArrival.toLocalDate());
        ticketInfo.setTimeOfArrival(datetimeArrival.toLocalTime());
        ticketInfo.setFirstName(ticket.getFirstName());
        ticketInfo.setLastName(ticket.getLastName());
        ticketInfo.setDateOfBirth(ticket.getDateOfBirth());
        ticketInfo.setDateOfPurchase(ticket.getDatetimeOfPurchase().toLocalDate());
        ticketInfo.setTimeOfPurchase(ticket.getDatetimeOfPurchase().toLocalTime());
        ticketInfo.setPrice(ticket.getPrice());
        ticketInfo.setRouteTitle(ticket.getTrain().getRoute().getTitle());
        ticketInfo.setLogin(ticket.getAccount().getLogin());
        return ticketInfo;
    }

    @Transactional(readOnly = true)
    public TicketReport getTicketReport(Ticket ticket) {
        TicketReport ticketReport = new TicketReport();
        ticketReport.setId(ticket.getId());
        ticketReport.setTrainId(ticket.getTrain().getId());
        ticketReport.setStationOfDeparture(ticket.getStationOfDeparture().getTitle());
        Spot spotDeparture = spotRepository.findByStationIdAndRouteId(ticket.getStationOfDeparture().getId(), ticket.getTrain().getRoute().getId());
        Spot spotArrival = spotRepository.findByStationIdAndRouteId(ticket.getStationOfArrival().getId(), ticket.getTrain().getRoute().getId());
        LocalDateTime datetimeDeparture = ticket.getTrain().getDateOfDeparture()
                .atTime(ticket.getTrain().getRoute().getTimeOfDeparture())
                .plus(spotDeparture.getMinutesSinceDeparture(), ChronoUnit.MINUTES);
        LocalDateTime datetimeArrival = datetimeDeparture.plus((long)spotArrival.getMinutesSinceDeparture() - spotDeparture.getMinutesSinceDeparture(), ChronoUnit.MINUTES);
        ticketReport.setDateOfDeparture(datetimeDeparture.toLocalDate().toString());
        ticketReport.setTimeOfDeparture(datetimeDeparture.toLocalTime().toString());
        ticketReport.setStationOfArrival(ticket.getStationOfArrival().getTitle());
        ticketReport.setDateOfArrival(datetimeArrival.toLocalDate().toString());
        ticketReport.setTimeOfArrival(datetimeArrival.toLocalTime().toString());
        ticketReport.setFirstName(ticket.getFirstName());
        ticketReport.setLastName(ticket.getLastName());
        ticketReport.setDateOfBirth(ticket.getDateOfBirth().toString());
        ticketReport.setDateOfPurchase(ticket.getDatetimeOfPurchase().toLocalDate().toString());
        ticketReport.setTimeOfPurchase(ticket.getDatetimeOfPurchase().toLocalTime().toString());
        ticketReport.setPrice(ticket.getPrice());
        ticketReport.setRouteTitle(ticket.getTrain().getRoute().getTitle());
        ticketReport.setLogin(ticket.getAccount().getLogin());
        return ticketReport;
    }

    @Transactional(readOnly = true)
    public void checkFields(int trainId, int stationOfDepartureId, int stationOfArrivalId, String firstName, String lastName, LocalDate dateOfBirth) {
        if (firstName == null || lastName == null || dateOfBirth == null) {
            throw new BusinessLogicException("All fields are required");
        }
        if (stationOfDepartureId == stationOfArrivalId) {
            throw new BusinessLogicException("Stations of departure and arrival should be different");
        }
        if (!allLetters(firstName)) {
            throw new BusinessLogicException("Invalid first name");
        }
        if (!allLetters(lastName)) {
            throw new BusinessLogicException("Invalid last name");
        }
        if (LocalDate.now().isBefore(dateOfBirth)) {
            throw new BusinessLogicException("Invalid Date of Birth");
        }
        Train train = trainRepository.findOne(trainId);
        Presence presenceDeparture = presenceRepository.findByTrainIdAndStationId(train.getId(), stationOfDepartureId);
        int ticketsAvailable = countTicketsAvailable(trainId, stationOfDepartureId, stationOfArrivalId);
        if (ticketsAvailable == 0) {
            throw new BusinessLogicException("No tickets available");
        }
        train.getTickets().forEach(ticket -> {
            if (ticket.getFirstName().equals(firstName) && ticket.getLastName().equals(lastName) && ticket.getDateOfBirth().isEqual(dateOfBirth)) {
                throw new BusinessLogicException("Passenger is already registered");
            }
        });
        Instant datetimeOfPurchase = LocalDateTime.now().toInstant(ZoneOffset.ofHours(presenceDeparture.getSpot().getStation().getTimezone()));
        if (ChronoUnit.MINUTES.between(datetimeOfPurchase, presenceDeparture.getInstant()) < 10) {
            throw new BusinessLogicException("Less than 10 minutes before departure");
        }
    }

    private boolean allLetters(String string) {
        return string.chars().allMatch(x -> Character.isLetter(x));
    }


    private void savePresences(int trainId, int stationOfDepartureId, int stationOfArrivalId) {
        Collection<Presence> presencesPassed = new ArrayList<>();
        boolean isDeparturePassed = false;
        boolean isArrivalNotPassed = true;
        Collection<Presence> presences = presenceRepository.findByTrainId(trainId);
        for (Presence presence : presences) {
            if (presence.getSpot().getStation().getId() == stationOfDepartureId) {
                isDeparturePassed = true;
            }
            if (presence.getSpot().getStation().getId() == stationOfArrivalId) {
                isArrivalNotPassed = false;
            }
            if (isDeparturePassed && isArrivalNotPassed) {
                presencesPassed.add(presence);
            }
        }
        presencesPassed.forEach(presence -> {
            int ticketsPurchased = presence.getNumberOfTicketsPurchased();
            ticketsPurchased++;
            presence.setNumberOfTicketsPurchased(ticketsPurchased);
        });
        try {
            presenceRepository.save(presencesPassed);
        } catch (PersistenceException ex) {
            throw new DatabaseException("Error occurred", ex);
        }
    }

}
