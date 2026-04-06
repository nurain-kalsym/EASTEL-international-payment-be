package com.kalsym.internationalPayment.controller;

import static com.kalsym.internationalPayment.filter.SessionRequestFilter.HEADER_STRING;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.Favourite;
import com.kalsym.internationalPayment.model.FavouriteRequest;
import com.kalsym.internationalPayment.model.Product;
import com.kalsym.internationalPayment.model.ProductVariant;
import com.kalsym.internationalPayment.model.Transaction;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.repositories.FavouriteRepository;
import com.kalsym.internationalPayment.services.FavouriteService;
import com.kalsym.internationalPayment.services.UserService;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/favourite")
public class FavouriteController {

    @Autowired
    FavouriteService favouriteService;

    @Autowired
    UserService userService;

    @Autowired
    FavouriteRepository favouriteRepository;

    @GetMapping("")
    public ResponseEntity<HttpResponse> getFavouriteMessagesByUserId(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "ASC") String sortingOrder,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(required = false) String globalSearch) {

        String logprefix = "getFavouriteMessagesByUserId";
        HttpResponse response = new HttpResponse(request.getRequestURI());
        try {

            User user = userService.getUser(request.getHeader(HEADER_STRING));

            if (user != null) {
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "user id: " + user.getId());
                try {
                    Favourite favourite = new Favourite();
                    favourite.setUserId(user.getId());

                    ExampleMatcher matcher = ExampleMatcher
                            .matchingAll().withIgnoreCase()
                            .withMatcher("userId", new ExampleMatcher.GenericPropertyMatcher().exact())
                            .withIgnoreNullValues().withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
                    Example<Favourite> example = Example.of(favourite, matcher);

                    Pageable pageable = null;
                    if (sortingOrder.equalsIgnoreCase("desc")) {
                        pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());
                    } else {
                        pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
                    }

                    Page<Favourite> messages = favouriteRepository.findAll(getAllFavouriteByUser(example, globalSearch),
                            pageable);
                    List<Favourite> tempResultList = messages.getContent();

                    tempResultList = tempResultList.stream()
                            .map(x -> {
                                List<ProductVariant> filteredVariants = x.getProduct().getProductVariant().stream()
                                        .filter(y -> y.getVariantType().equals(x.getVariantType()))
                                        .collect(Collectors.toList());

                                x.getProduct().setProductVariant(filteredVariants);

                                return x;
                            })
                            .collect(Collectors.toList());

                    response.setStatus(HttpStatus.OK);
                    response.setData(messages);
                } catch (NullPointerException e) {
                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setMessage("Error getting favourite messages: " + e.getMessage());
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "NullPointerException"+ e.getMessage());
                } catch (Exception e) {
                    response.setStatus(HttpStatus.EXPECTATION_FAILED);
                    response.setMessage("Unexpected error getting favourite messages: " + e.getMessage());
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Exception"+ e.getMessage());
                }
            } else {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("User Not Found");
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "User Not Found");
            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setMessage("Invalid User Credential");
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    public Specification<Favourite> getAllFavouriteByUser(Example<Favourite> example, String globalSearch) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Transaction, Product> product = root.join("product");

            predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));
            // predicates.add(builder.not(root.get("status").in(StatusType.DELETED)));

            if (globalSearch != null) {
                // Predicate for Employee Projects data
                predicates.add(builder.or(
                        builder.like(product.get("productName"), "%" + globalSearch + "%"),
                        builder.like(root.get("accountNo"), "%" + globalSearch + "%"),
                        builder.like(root.get("label"), "%" + globalSearch + "%")));
            }

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    @Operation(summary = "create or update fav", description = "dont send id if created new, only send when update only")
    @PostMapping("/createOrUpdate")
    public ResponseEntity<HttpResponse> createOrUpdateFavourite(
            HttpServletRequest request,
            @RequestBody FavouriteRequest favourite) {
        String logprefix = "createOrUpdateFavourite";
        HttpResponse response = new HttpResponse(request.getRequestURI());
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Favourite Id :" + favourite.getId());
        try {
            User user = userService.getUser(request.getHeader(HEADER_STRING));
            if (user == null) {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("User Not Found");
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "User Not Found, User token : " + request.getHeader(HEADER_STRING));

                return ResponseEntity.status(response.getStatus()).body(response);
            }

            Favourite newFavourite = new Favourite();
            newFavourite.setUserId(user.getId());
            newFavourite.setAccountNo(favourite.getAccountNo());
            newFavourite.setProductCode(favourite.getProductCode());
            newFavourite.setLabel(favourite.getLabel());
            newFavourite.setVariantType(favourite.getVariantType());
            newFavourite.setCountryCode(favourite.getCountryCode());
            Date date = new Date();
            newFavourite.setUpdated(date);

            // Check if the favourite already exists
            Favourite existingFavourite = favouriteRepository.findByUserIdAndProductCode(user.getId(),
                    favourite.getProductCode());

            // For updating an existing favourite
            if (favourite.getId() != null) {
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Favourite Id :" + favourite.getId());

                if (existingFavourite == null) {
                    response.setStatus(HttpStatus.NOT_FOUND);
                    response.setMessage("Saved Favourite Not Found");
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Saved Favourite Not Found");
                    return ResponseEntity.status(response.getStatus()).body(response);
                }

                if (!existingFavourite.getId().equals(favourite.getId())) {
                    response.setStatus(HttpStatus.EXPECTATION_FAILED);
                    response.setMessage("Wrong favourite id");
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Wrong Favourite Id Passed, Request Delete Id: " + existingFavourite.getId());
                    return ResponseEntity.status(response.getStatus()).body(response);
                }

                try {
                    Favourite updatedFavourite = favouriteService.updateFavourite(favourite.getId(), newFavourite);
                    response.setStatus(HttpStatus.OK);
                    response.setData(updatedFavourite);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Favourite Updated ");
                } catch (Exception e) {
                    response.setStatus(HttpStatus.EXPECTATION_FAILED);
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Exception " + e.getMessage());
                }

            }
            // For creating a new favourite
            else {

                if (existingFavourite != null) {
                    response.setStatus(HttpStatus.CONFLICT);
                    response.setMessage("Product already exists in favourites");
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Product already exists in favourites");
                    return ResponseEntity.status(response.getStatus()).body(response);
                }

                newFavourite.setCreated(date);

                try {
                    Favourite savedFavourite = favouriteService.createFavourite(newFavourite);
                    response.setStatus(HttpStatus.CREATED);
                    response.setData(savedFavourite);
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Favourite Created ");
                } catch (Exception e) {
                    response.setStatus(HttpStatus.EXPECTATION_FAILED);
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Exception " + e.getMessage());
                }

            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setMessage("Invalid User Credential");

            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "delete fav by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpResponse> deleteFavouriteById(
            HttpServletRequest request,
            @PathVariable Long id) {
        String logprefix = "deleteFavouriteById";
        HttpResponse response = new HttpResponse(request.getRequestURI());
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Request Delete Favourite Id: " + id);
        try {
            User user = userService.getUser(request.getHeader(HEADER_STRING));
            if (user == null) {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("User Not Found");
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "User Not Found, User token : " + request.getHeader(HEADER_STRING));
                return ResponseEntity.status(response.getStatus()).body(response);
            }

            // Check if the favourite exists
            Favourite existingFavourite = favouriteRepository.findById(id).orElse(null);
            if (existingFavourite == null) {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("Favourite Not Found");
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Favourite Not Found");
                return ResponseEntity.status(response.getStatus()).body(response);
            }

            if (!existingFavourite.getUserId().equals(user.getId())) {
                response.setStatus(HttpStatus.UNAUTHORIZED);
                response.setMessage("USER UNAUTHORIZED");
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "USER UNAUTHORIZED");
                return ResponseEntity.status(response.getStatus()).body(response);
            }

            try {
                favouriteRepository.deleteById(id);
                response.setStatus(HttpStatus.OK);
                response.setMessage("Favourite Deleted Successfully");
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Favourite Deleted");

            } catch (Exception e) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                response.setMessage("Unexpected error deleting favourite: " + e.getMessage());
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Exception " + e.getMessage());
            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setMessage("Invalid User Credential");

            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
