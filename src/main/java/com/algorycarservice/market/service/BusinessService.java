package com.algorycarservice.market.service;

import com.algorycarservice.appointment.dto.SlotsResponse;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.common.schedule.SlotCalendar;
import com.algorycarservice.market.dto.BusinessRequest;
import com.algorycarservice.market.dto.BusinessResponse;
import com.algorycarservice.market.model.Booking;
import com.algorycarservice.market.model.BookingStatus;
import com.algorycarservice.market.model.Business;
import com.algorycarservice.market.model.ServiceCategory;
import com.algorycarservice.market.repository.BookingRepository;
import com.algorycarservice.market.repository.BusinessRepository;
import com.algorycarservice.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessService {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private final BusinessRepository businessRepository;
    private final BookingRepository bookingRepository;
    private final SlotCalendar slotCalendar;

    /**
     * Active businesses, optionally narrowed to a category, a city and a name or district query. Matching is done
     * in memory with Turkish case rules ("İ"/"ı"), which the database's {@code lower()} gets wrong.
     */
    public List<BusinessResponse> search(ServiceCategory category, String city, String query) {
        String cityKey = key(city);
        String queryKey = key(query);
        return businessRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(business -> category == null || business.getCategories().contains(category))
                .filter(business -> cityKey == null || key(business.getCity()).equals(cityKey))
                .filter(business -> queryKey == null
                        || key(business.getName()).contains(queryKey)
                        || key(business.getDistrict()).contains(queryKey))
                .map(BusinessResponse::from)
                .toList();
    }

    public BusinessResponse get(Long id) {
        return BusinessResponse.from(getActive(id));
    }

    /** Bookable slots of a business on one day, using its own capacity. */
    public SlotsResponse slots(Long id, LocalDate date) {
        Business business = getActive(id);
        int capacity = business.getSlotCapacity();
        if (slotCalendar.isClosed(date)) {
            return new SlotsResponse(date, true, capacity, List.of());
        }
        List<Booking> booked = bookingRepository.findAllByBusinessIdAndDateAndStatusIn(id, date, BookingStatus.ACTIVE);
        List<SlotsResponse.Slot> slots = slotCalendar.slotTimes().stream().map(time -> {
            int count = (int) booked.stream().filter(booking -> booking.getStartTime().equals(time)).count();
            return new SlotsResponse.Slot(time, count, count < capacity && slotCalendar.isUpcoming(date, time));
        }).toList();
        return new SlotsResponse(date, false, capacity, slots);
    }

    @Transactional
    public Business create(User owner, BusinessRequest request) {
        Business business = Business.builder().uuid(owner.getBusinessUuid()).owner(owner).build();
        apply(business, request);
        return businessRepository.save(business);
    }

    public BusinessResponse mine(Long ownerId) {
        return BusinessResponse.from(getOwned(ownerId));
    }

    @Transactional
    public BusinessResponse update(Long ownerId, BusinessRequest request) {
        Business business = getOwned(ownerId);
        apply(business, request);
        return BusinessResponse.from(business);
    }

    public Business getOwned(Long ownerId) {
        return businessRepository.findByOwnerId(ownerId).orElseThrow(() -> new ResourceNotFoundException("business"));
    }

    public Business getActive(Long id) {
        return businessRepository.findById(id)
                .filter(Business::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("business"));
    }

    private static void apply(Business business, BusinessRequest request) {
        business.setName(request.name().trim());
        business.setDescription(clean(request.description()));
        business.setPhone(request.phone().trim());
        business.setCity(request.city().trim());
        business.setDistrict(request.district().trim());
        business.setAddress(request.address().trim());
        business.getCategories().clear();
        business.getCategories().addAll(EnumSet.copyOf(request.categories()));
        if (request.slotCapacity() != null) {
            business.setSlotCapacity(request.slotCapacity());
        }
    }

    private static String key(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(TURKISH) : null;
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
