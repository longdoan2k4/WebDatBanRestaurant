package com.example.restaurantpro.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.restaurantpro.dto.TableAdminResponseDto;
import com.example.restaurantpro.dto.TableGroupDto;
import com.example.restaurantpro.dto.TableStatusItemDto;
import com.example.restaurantpro.dto.TableStatusResponseDto;
import com.example.restaurantpro.exception.TableInUseException;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.BookingStatus;
import com.example.restaurantpro.model.DiningTable;
import com.example.restaurantpro.repository.BookingRepository;
import com.example.restaurantpro.repository.DiningTableRepository;

@Service
public class TableService {

    private final DiningTableRepository diningTableRepository;
    private final BookingRepository bookingRepository;

    public TableService(DiningTableRepository diningTableRepository,
                        BookingRepository bookingRepository) {
        this.diningTableRepository = diningTableRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<DiningTable> getActiveTables() {
        return diningTableRepository.findByActiveTrueOrderByCapacityAsc();
    }

    public List<DiningTable> getAllTables() {
        return diningTableRepository.findAll().stream()
                .sorted((a, b) -> a.getCapacity().compareTo(b.getCapacity()))
                .toList();
    }

    public DiningTable getTableById(Long id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bàn."));
    }

    public List<DiningTable> findSuitableTables(Integer guestCount) {
        return diningTableRepository.findByActiveTrueAndCapacityGreaterThanEqualOrderByCapacityAsc(guestCount);
    }

    public List<DiningTable> findExactCapacityTables(Integer guestCount) {
        return diningTableRepository.findByActiveTrueAndCapacityOrderByNameAsc(guestCount);
    }

    public AvailableTablesResult findAvailableExactCapacityTables(Integer guestCount,
                                                                  LocalDateTime bookingDateTime,
                                                                  Integer durationHours) {
        List<DiningTable> exactCapacityTables = findExactCapacityTables(guestCount);
        if (exactCapacityTables.isEmpty()) {
            return new AvailableTablesResult(List.of(), 0, 0);
        }

        int normalizedDuration = (durationHours == null || durationHours < 1) ? 2 : durationHours;
        LocalDateTime fromTime = bookingDateTime;
        LocalDateTime toTime = bookingDateTime.plusHours(normalizedDuration).plusMinutes(30);

        List<DiningTable> availableTables = new ArrayList<>();
        for (DiningTable table : exactCapacityTables) {
            long conflictCount = bookingRepository.countConflictingBookings(table.getId(), fromTime, toTime);
            if (conflictCount == 0) {
                availableTables.add(table);
            }
        }

        return new AvailableTablesResult(availableTables, exactCapacityTables.size(), availableTables.size());
    }

    public DiningTable save(DiningTable diningTable) {
        return diningTableRepository.save(diningTable);
    }

    public void delete(Long id) {
        deleteTable(id);
    }

    public void deleteTable(Long tableId) {
        boolean hasActiveBooking = bookingRepository.existsActiveBookingByTableId(
                tableId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED),
                LocalDateTime.now()
        );

        if (hasActiveBooking) {
            throw new TableInUseException("Không thể xóa! Bàn này đang có khách đặt lịch hoặc đang sử dụng.");
        }

        try {
            diningTableRepository.deleteById(tableId);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Không thể xóa! Bàn này đã có lịch sử booking trong hệ thống.");
        }
    }

    public long countTables() {
        return diningTableRepository.count();
    }

    public List<DiningTable> findAll() {
        return getAllTables();
    }

    public DiningTable saveOrUpdate(Long id,
                                    String name,
                                    String tableNumber,
                                    String floor,
                                    String roomType,
                                    String areaPosition,
                                    Integer capacity,
                                    String style,
                                    String chairType,
                                    String description,
                                    boolean active) {
        if (tableNumber == null || tableNumber.isBlank()) {
            throw new IllegalArgumentException("So ban (tableNumber) khong duoc de trong.");
        }

        String normalizedTableNumber = tableNumber.trim();
        if (id == null && diningTableRepository.existsByTableNumberIgnoreCase(normalizedTableNumber)) {
            throw new IllegalArgumentException("So ban da ton tai. Vui long dung ma ban khac.");
        }

        DiningTable table;
        if (id == null) {
            table = new DiningTable();
        } else {
            table = getTableById(id);
            if (!normalizedTableNumber.equalsIgnoreCase(table.getTableNumber())
                    && diningTableRepository.existsByTableNumberIgnoreCase(normalizedTableNumber)) {
                throw new IllegalArgumentException("So ban da ton tai. Vui long dung ma ban khac.");
            }
        }
        table.setName(name);
        table.setTableNumber(normalizedTableNumber);
        table.setFloor(floor);
        table.setRoomType(roomType);
        table.setAreaPosition(areaPosition);
        table.setLocation(table.getLocationDisplay());
        table.setCapacity(capacity);
        table.setTableType(style);
        table.setChairType(chairType);
        table.setDescription(description);
        table.setActive(active);
        return save(table);
    }

    public List<TableGroupDto> getTableGroups(LocalDateTime atTime) {
        LocalDateTime monitorTime = atTime == null ? LocalDateTime.now() : atTime;

        record TableGroupKey(String floor, String roomType, Integer capacity) {
        }

        Map<TableGroupKey, List<DiningTable>> grouped = getAllTables().stream()
                .filter(DiningTable::isActive)
                .collect(Collectors.groupingBy(table -> new TableGroupKey(
                        table.getFloor(),
                        table.getRoomType(),
                        table.getCapacity())));

        return grouped.entrySet().stream()
                .map(entry -> {
                    int totalQuantity = entry.getValue().size();
                    int availableQuantity = (int) entry.getValue().stream()
                            .filter(table -> bookingRepository.countActiveBookingsAtTime(table.getId(), monitorTime) == 0)
                            .count();

                    return new TableGroupDto(
                            entry.getKey().floor(),
                            entry.getKey().roomType(),
                            entry.getKey().capacity(),
                            totalQuantity,
                            availableQuantity
                    );
                })
                .sorted(Comparator.comparing(TableGroupDto::getFloor, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(TableGroupDto::getRoomType, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(TableGroupDto::getCapacity, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    public List<TableAdminResponseDto> getAdminTableResponses() {
        LocalDateTime now = LocalDateTime.now();
        return getAllTables().stream()
                .filter(DiningTable::isActive)
                .map(table -> {
                    long activeBookings = bookingRepository.countActiveBookingsAtTime(table.getId(), now);
                boolean availableNow = activeBookings == 0;

                    return new TableAdminResponseDto(
                            table.getId(),
                            table.getName(),
                    table.getTableNumber(),
                            table.getTableType(),
                            table.getFloor(),
                            table.getRoomType(),
                            table.getAreaPosition(),
                            table.getLocationDisplay(),
                            table.getCapacity(),
                    availableNow,
                            table.isActive()
                    );
                })
                .toList();
    }

    public TableMonitoringData getTableMonitoringData(LocalDateTime dateTime) {
        LocalDateTime selected = dateTime == null ? LocalDateTime.now() : dateTime;
        List<DiningTable> allTables = getAllTables();
        List<DiningTable> activeTables = allTables.stream().filter(DiningTable::isActive).toList();

        if (activeTables.isEmpty()) {
            return new TableMonitoringData(selected, List.of(), List.of(), 0, 0);
        }

        List<Long> tableIds = activeTables.stream().map(DiningTable::getId).toList();
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                tableIds,
            selected,
            selected.plusMinutes(1),
                List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        Map<Long, Booking> bookingByTableId = overlapping.stream()
                .collect(Collectors.toMap(
                        booking -> booking.getDiningTable().getId(),
                        booking -> booking,
                        (existing, ignored) -> existing
                ));

        List<TableMonitoringRow> monitoringRows = new ArrayList<>();
        for (DiningTable table : activeTables) {
            Booking booking = bookingByTableId.get(table.getId());
            long activeBookingCount = bookingRepository.countActiveBookingsAtTime(table.getId(), selected);
            int occupiedQuantity = activeBookingCount > 0 ? 1 : 0;
            int availableQuantity = activeBookingCount > 0 ? 0 : 1;

            String statusText;
            if (availableQuantity == 1) {
                statusText = "Còn trống";
            } else {
                statusText = "Hết bàn";
            }

            if (booking == null) {
                monitoringRows.add(new TableMonitoringRow(
                        table,
                        statusText,
                        null,
                        null,
                        1,
                        availableQuantity,
                        occupiedQuantity
                ));
            } else {
                monitoringRows.add(new TableMonitoringRow(
                        table,
                        statusText,
                        booking.getBookingDateTime(),
                        booking.getStatus().getDisplayName(),
                        1,
                        availableQuantity,
                        occupiedQuantity
                ));
            }
        }

        List<TableZoneSummary> zoneSummaries = monitoringRows.stream()
                .collect(Collectors.groupingBy(row -> new ZoneKey(
                        row.table().getTableType(),
                        row.table().getFloor(),
                        row.table().getRoomType(),
                        row.table().getAreaPosition())))
                .entrySet().stream()
                .map(entry -> {
                    int total = entry.getValue().stream().mapToInt(TableMonitoringRow::totalQuantity).sum();
                    int available = entry.getValue().stream().mapToInt(TableMonitoringRow::availableQuantity).sum();
                    ZoneKey key = entry.getKey();
                String sampleTableName = entry.getValue().stream().map(row -> row.table().getName()).findFirst().orElse("N/A");
                return new TableZoneSummary(sampleTableName, key.tableType(), key.floor(), key.roomType(), key.areaPosition(), total, available);
                })
                .sorted(Comparator.comparing(TableZoneSummary::tableName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        int totalActive = monitoringRows.stream().mapToInt(TableMonitoringRow::totalQuantity).sum();
        int totalAvailable = monitoringRows.stream().mapToInt(TableMonitoringRow::availableQuantity).sum();

        return new TableMonitoringData(selected, monitoringRows, zoneSummaries, totalActive, totalAvailable);
    }

    public TableStatusResponseDto getTableStatusAt(LocalDateTime checkTime) {
        LocalDateTime effectiveCheckTime = checkTime == null ? LocalDateTime.now() : checkTime;
        LocalDateTime lookAheadEnd = effectiveCheckTime.plusHours(2);

        List<DiningTable> activeTables = getAllTables().stream()
                .filter(DiningTable::isActive)
                .sorted(Comparator.comparing(DiningTable::getFloor, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(DiningTable::getRoomType, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(DiningTable::getTableNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        if (activeTables.isEmpty()) {
            return new TableStatusResponseDto(effectiveCheckTime, 0, 0, 0, 0, List.of(), List.of(), List.of());
        }

        List<Long> tableIds = activeTables.stream().map(DiningTable::getId).toList();

        List<Booking> inUseBookings = bookingRepository.findOverlappingBookings(
                tableIds,
                effectiveCheckTime,
                effectiveCheckTime.plusMinutes(1),
                List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        List<Booking> lookAheadBookings = bookingRepository.findOverlappingBookings(
                tableIds,
                effectiveCheckTime,
                lookAheadEnd,
                List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        Map<Long, Booking> inUseByTable = inUseBookings.stream()
                .collect(Collectors.toMap(
                        booking -> booking.getDiningTable().getId(),
                        booking -> booking,
                        (existing, ignored) -> existing
                ));

        Map<Long, LocalDateTime> nextBookingStartByTable = new HashMap<>();
        for (Booking booking : lookAheadBookings) {
            Long tableId = booking.getDiningTable() == null ? null : booking.getDiningTable().getId();
            if (tableId == null) {
                continue;
            }
            if (booking.getBookingDateTime() != null && !booking.getBookingDateTime().isBefore(effectiveCheckTime)) {
                LocalDateTime current = nextBookingStartByTable.get(tableId);
                if (current == null || booking.getBookingDateTime().isBefore(current)) {
                    nextBookingStartByTable.put(tableId, booking.getBookingDateTime());
                }
            }
        }

        List<TableStatusItemDto> freeTables = new ArrayList<>();
        List<TableStatusItemDto> inUseTables = new ArrayList<>();
        List<TableStatusItemDto> soonTables = new ArrayList<>();

        for (DiningTable table : activeTables) {
            Long tableId = table.getId();
            Booking usingBooking = inUseByTable.get(tableId);
            if (usingBooking != null) {
                inUseTables.add(toStatusItem(table, usingBooking.getEndTime()));
                continue;
            }

            LocalDateTime nextStart = nextBookingStartByTable.get(tableId);
            if (nextStart != null) {
                soonTables.add(toStatusItem(table, nextStart));
            } else {
                freeTables.add(toStatusItem(table, null));
            }
        }

        return new TableStatusResponseDto(
                effectiveCheckTime,
                activeTables.size(),
                freeTables.size(),
                inUseTables.size(),
                soonTables.size(),
                freeTables,
                inUseTables,
                soonTables
        );
    }

    private TableStatusItemDto toStatusItem(DiningTable table, LocalDateTime availableUntil) {
        return new TableStatusItemDto(
                table.getId(),
                table.getTableNumber(),
                table.getName(),
                table.getFloor(),
                table.getRoomType(),
                table.getCapacity(),
                table.getTableType(),
                availableUntil
        );
    }

    public record AvailableTablesResult(List<DiningTable> tables, int totalTables, int availableTables) {
    }

    public record TableMonitoringData(LocalDateTime selectedDateTime,
                                      List<TableMonitoringRow> rows,
                                      List<TableZoneSummary> zoneSummaries,
                                      int totalActiveTables,
                                      int totalAvailableTables) {
    }

    public record TableMonitoringRow(DiningTable table,
                                     String statusText,
                                     LocalDateTime bookingDateTime,
                                     String bookingStatus,
                                     int totalQuantity,
                                     int availableQuantity,
                                     int occupiedQuantity) {
    }

    public record TableZoneSummary(String tableName,
                                   String tableType,
                                   String floor,
                                   String roomType,
                                   String areaPosition,
                                   int totalTables,
                                   int availableTables) {
    }

    private record ZoneKey(String tableType, String floor, String roomType, String areaPosition) {
    }

}