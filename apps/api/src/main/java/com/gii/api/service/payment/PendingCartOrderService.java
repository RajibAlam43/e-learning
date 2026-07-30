package com.gii.api.service.payment;

import com.gii.api.model.request.payment.CreateCheckoutOrderItemRequest;
import com.gii.api.model.request.payment.CreateCheckoutOrderRequest;
import com.gii.api.model.response.payment.CheckoutOrderItemResponse;
import com.gii.api.model.response.payment.CheckoutOrderResponse;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class PendingCartOrderService {

  private static final long ORDER_EXPIRY_SECONDS = Duration.ofMinutes(30).getSeconds();

  private final CurrentUserService currentUserService;
  private final CourseRepository courseRepository;
  private final CollectionRepository collectionRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;

  public CheckoutOrderResponse execute(CreateCheckoutOrderRequest request, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    List<CreateCheckoutOrderItemRequest> requestedItems = request.items();
    validateNoDuplicateLines(requestedItems);

    List<UUID> requestedCourseIds =
        requestedItems.stream()
            .filter(i -> i.itemType() == OrderItemType.COURSE)
            .map(CreateCheckoutOrderItemRequest::courseId)
            .toList();
    List<UUID> requestedCollectionIds =
        requestedItems.stream()
            .filter(i -> i.itemType() == OrderItemType.COLLECTION)
            .map(CreateCheckoutOrderItemRequest::collectionId)
            .toList();

    Map<UUID, Course> coursesById = fetchPublishedCourses(requestedCourseIds);
    Map<UUID, Collection> collectionsById = fetchPublishedCollections(requestedCollectionIds);

    for (UUID courseId : requestedCourseIds) {
      if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
          user.getId(), courseId, EnrollmentStatus.ACTIVE)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this course");
      }
    }

    List<CollectionCourse> collectionCoursesForCart =
        requestedCollectionIds.isEmpty()
            ? List.of()
            : collectionCourseRepository.findByCollection_IdInWithCourse(requestedCollectionIds);
    Map<UUID, List<CollectionCourse>> collectionCoursesByCollectionId = groupByCollection(collectionCoursesForCart);

    Set<UUID> collectionCourseIdsInCart = new HashSet<>();
    for (CollectionCourse cc : collectionCoursesForCart) {
      collectionCourseIdsInCart.add(cc.getCourse().getId());
    }

    // Case 3: block course+collection overlap inside the same cart.
    for (UUID courseId : requestedCourseIds) {
      if (collectionCourseIdsInCart.contains(courseId)) {
        String courseTitle = coursesById.get(courseId).getTitle();
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Course \"" + courseTitle + "\" is already included in a selected collection. Remove individual course.");
      }
    }

    // Case 2: if user owns collection containing the course, block standalone course purchase.
    Set<UUID> ownedCollectionIds =
        collectionEnrollmentRepository.findByUserIdAndStatus(user.getId(), EnrollmentStatus.ACTIVE).stream()
            .map(CollectionEnrollment::getCollection)
            .map(Collection::getId)
            .collect(java.util.stream.Collectors.toSet());
    if (!ownedCollectionIds.isEmpty() && !requestedCourseIds.isEmpty()) {
      List<CollectionCourse> ownedCollectionCourses =
          collectionCourseRepository.findByCollection_IdInWithCourse(new ArrayList<>(ownedCollectionIds));
      Set<UUID> blockedCourseIds =
          ownedCollectionCourses.stream().map(cc -> cc.getCourse().getId()).collect(java.util.stream.Collectors.toSet());
      for (UUID courseId : requestedCourseIds) {
        if (blockedCourseIds.contains(courseId)) {
          throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "This course is already covered by one of your owned collections.");
        }
      }
    }

    // Case 1: collection price is reduced by already-owned included courses.
    Set<UUID> ownedCourseIds =
        enrollmentRepository.findByUserIdAndStatus(user.getId(), EnrollmentStatus.ACTIVE).stream()
            .map(Enrollment::getCourse)
            .map(Course::getId)
            .collect(java.util.stream.Collectors.toSet());

    Order reusablePendingOrder = findReusablePendingOrder(user.getId(), requestedItems);
    if (reusablePendingOrder != null) {
      return toCheckoutResponse(reusablePendingOrder, user);
    }

    Order order =
        orderRepository.save(
            Order.builder()
                .user(user)
                .amountBdt(BigDecimal.ZERO)
                .currency("BDT")
                .provider(OrderProvider.SSLCOMMERZ)
                .status(OrderStatus.PENDING)
                .build());

    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal totalDiscount = BigDecimal.ZERO;
    List<OrderItem> orderItems = new ArrayList<>();
    List<CheckoutOrderItemResponse> responseItems = new ArrayList<>();

    for (CreateCheckoutOrderItemRequest requestedItem : requestedItems) {
      if (requestedItem.itemType() == OrderItemType.COURSE) {
        Course course = coursesById.get(requestedItem.courseId());
        BigDecimal price = course.getIsFree() != null && course.getIsFree() ? BigDecimal.ZERO : course.getPriceBdt();
        BigDecimal discount = BigDecimal.ZERO;
        orderItems.add(
            OrderItem.builder()
                .order(order)
                .itemType(OrderItemType.COURSE)
                .course(course)
                .titleSnapshot(course.getTitle())
                .titleSnapshotEn(course.getTitleEn())
                .priceBdt(price)
                .discountBdt(discount)
                .build());
        responseItems.add(
            CheckoutOrderItemResponse.builder()
                .itemType(OrderItemType.COURSE)
                .courseId(course.getId())
                .collectionId(null)
                .courseName(localizedContentService.text(course.getTitle(), course.getTitleEn()))
                .courseSlug(course.getSlug())
                .courseThumbnailUrl(assetUrlService.publicUrl(course.getThumbnailObjectKey()))
                .originalPrice(price)
                .discountAmount(discount)
                .finalPrice(price.subtract(discount))
                .discountReason(null)
                .build());
        subtotal = subtotal.add(price);
        totalDiscount = totalDiscount.add(discount);
        continue;
      }

      Collection collection = collectionsById.get(requestedItem.collectionId());
      List<CollectionCourse> includedCourses =
          collectionCoursesByCollectionId.getOrDefault(collection.getId(), List.of());
      BigDecimal discount = BigDecimal.ZERO;
      for (CollectionCourse included : includedCourses) {
        if (ownedCourseIds.contains(included.getCourse().getId())) {
          discount = discount.add(included.getCourse().getPriceBdt());
        }
      }
      if (discount.compareTo(collection.getPriceBdt()) > 0) {
        discount = collection.getPriceBdt();
      }
      BigDecimal price = collection.getPriceBdt();

      orderItems.add(
          OrderItem.builder()
              .order(order)
              .itemType(OrderItemType.COLLECTION)
              .collection(collection)
              .titleSnapshot(collection.getTitle())
              .titleSnapshotEn(collection.getTitleEn())
              .priceBdt(price)
              .discountBdt(discount)
              .build());
      responseItems.add(
          CheckoutOrderItemResponse.builder()
              .itemType(OrderItemType.COLLECTION)
              .courseId(null)
              .collectionId(collection.getId())
              .courseName(
                  localizedContentService.text(collection.getTitle(), collection.getTitleEn()))
              .courseSlug(collection.getSlug())
              .courseThumbnailUrl(null)
              .originalPrice(price)
              .discountAmount(discount)
              .finalPrice(price.subtract(discount))
              .discountReason(discount.signum() > 0 ? "ALREADY_OWNED_INCLUDED_COURSES" : null)
              .build());
      subtotal = subtotal.add(price);
      totalDiscount = totalDiscount.add(discount);
    }

    orderItemRepository.saveAll(orderItems);

    BigDecimal totalAmount = subtotal.subtract(totalDiscount);
    order.setAmountBdt(totalAmount);
    Order savedOrder = orderRepository.save(order);

    Instant expiresAt = savedOrder.getCreatedAt().plusSeconds(ORDER_EXPIRY_SECONDS);
    return CheckoutOrderResponse.builder()
        .orderId(savedOrder.getId())
        .subtotal(subtotal)
        .totalDiscount(totalDiscount)
        .totalAmount(totalAmount)
        .currency(savedOrder.getCurrency())
        .items(responseItems)
        .status(savedOrder.getStatus())
        .expiresAt(expiresAt)
        .isExpired(Instant.now().isAfter(expiresAt))
        .customerEmail(user.getEmail())
        .customerPhone(user.getPhone())
        .nextAction("INITIATE_PAYMENT")
        .build();
  }

  private void validateNoDuplicateLines(List<CreateCheckoutOrderItemRequest> requestedItems) {
    Set<String> seen = new HashSet<>();
    for (CreateCheckoutOrderItemRequest item : requestedItems) {
      if (item.itemType() == OrderItemType.COURSE) {
        if (item.courseId() == null || item.collectionId() != null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid COURSE checkout item payload");
        }
        if (!seen.add("COURSE:" + item.courseId())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate course found in cart");
        }
      } else if (item.itemType() == OrderItemType.COLLECTION) {
        if (item.collectionId() == null || item.courseId() != null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid COLLECTION checkout item payload");
        }
        if (!seen.add("COLLECTION:" + item.collectionId())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate collection found in cart");
        }
      }
    }
  }

  private Map<UUID, Course> fetchPublishedCourses(List<UUID> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    List<Course> courses = courseRepository.findAllById(ids);
    Map<UUID, Course> map = new HashMap<>();
    for (Course course : courses) {
      if (course.getStatus() != PublishStatus.PUBLISHED) {
        continue;
      }
      map.put(course.getId(), course);
    }
    if (map.size() != new HashSet<>(ids).size()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more courses not found");
    }
    return map;
  }

  private Map<UUID, Collection> fetchPublishedCollections(List<UUID> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    List<Collection> collections = collectionRepository.findByIdInAndStatus(ids, PublishStatus.PUBLISHED);
    Map<UUID, Collection> map = collections.stream().collect(java.util.stream.Collectors.toMap(Collection::getId, c -> c));
    if (map.size() != new HashSet<>(ids).size()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more collections not found");
    }
    return map;
  }

  private Map<UUID, List<CollectionCourse>> groupByCollection(List<CollectionCourse> rows) {
    Map<UUID, List<CollectionCourse>> out = new HashMap<>();
    for (CollectionCourse row : rows) {
      out.computeIfAbsent(row.getCollection().getId(), ignored -> new ArrayList<>()).add(row);
    }
    return out;
  }

  private Order findReusablePendingOrder(UUID userId, List<CreateCheckoutOrderItemRequest> requestedItems) {
    Instant now = Instant.now();
    Set<String> requestedKeys = requestedItems.stream().map(this::itemKey).collect(Collectors.toSet());
    int requestedSize = requestedItems.size();
    for (Order order : orderRepository.findByUserIdAndStatus(userId, OrderStatus.PENDING)) {
      if (!order.getCreatedAt().plusSeconds(ORDER_EXPIRY_SECONDS).isAfter(now)) {
        continue;
      }
      List<OrderItem> existingItems = orderItemRepository.findByOrderId(order.getId());
      if (existingItems.size() != requestedSize) {
        continue;
      }
      Set<String> existingKeys = existingItems.stream().map(this::itemKey).collect(Collectors.toSet());
      if (existingKeys.equals(requestedKeys)) {
        return order;
      }
    }
    return null;
  }

  private String itemKey(CreateCheckoutOrderItemRequest item) {
    return item.itemType() == OrderItemType.COURSE
        ? "COURSE:" + item.courseId()
        : "COLLECTION:" + item.collectionId();
  }

  private String itemKey(OrderItem item) {
    return item.getItemType() == OrderItemType.COURSE
        ? "COURSE:" + item.getCourse().getId()
        : "COLLECTION:" + item.getCollection().getId();
  }

  private CheckoutOrderResponse toCheckoutResponse(Order order, User user) {
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    BigDecimal subtotal =
        items.stream().map(OrderItem::getPriceBdt).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalDiscount =
        items.stream()
            .map(OrderItem::getDiscountBdt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAmount = subtotal.subtract(totalDiscount);
    List<CheckoutOrderItemResponse> responseItems =
        items.stream()
            .map(
                item ->
                    CheckoutOrderItemResponse.builder()
                        .itemType(item.getItemType())
                        .courseId(item.getCourse() != null ? item.getCourse().getId() : null)
                        .collectionId(
                            item.getCollection() != null ? item.getCollection().getId() : null)
                        .courseName(item.getTitleSnapshot())
                        .courseSlug(
                            item.getCourse() != null
                                ? item.getCourse().getSlug()
                                : item.getCollection().getSlug())
                        .courseThumbnailUrl(
                            item.getCourse() != null
                                ? assetUrlService.publicUrl(
                                    item.getCourse().getThumbnailObjectKey())
                                : null)
                        .originalPrice(item.getPriceBdt())
                        .discountAmount(item.getDiscountBdt())
                        .finalPrice(item.getPriceBdt().subtract(item.getDiscountBdt()))
                        .discountReason(null)
                        .build())
            .toList();

    Instant expiresAt = order.getCreatedAt().plusSeconds(ORDER_EXPIRY_SECONDS);
    return CheckoutOrderResponse.builder()
        .orderId(order.getId())
        .subtotal(subtotal)
        .totalDiscount(totalDiscount)
        .totalAmount(totalAmount)
        .currency(order.getCurrency())
        .items(responseItems)
        .status(order.getStatus())
        .expiresAt(expiresAt)
        .isExpired(Instant.now().isAfter(expiresAt))
        .customerEmail(user.getEmail())
        .customerPhone(user.getPhone())
        .nextAction("INITIATE_PAYMENT")
        .build();
  }
}
