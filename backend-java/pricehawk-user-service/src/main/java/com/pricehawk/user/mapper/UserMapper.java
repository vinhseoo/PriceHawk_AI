package com.pricehawk.user.mapper;

import com.pricehawk.user.domain.entity.Role;
import com.pricehawk.user.domain.entity.SearchHistory;
import com.pricehawk.user.domain.entity.SubscriptionPlan;
import com.pricehawk.user.domain.entity.User;
import com.pricehawk.user.domain.entity.WishlistItem;
import com.pricehawk.user.dto.response.SearchHistoryDTO;
import com.pricehawk.user.dto.response.UserDTO;
import com.pricehawk.user.dto.response.WishlistItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    @Mapping(target = "dailySearchRemaining", source = ".", qualifiedByName = "calcRemaining")
    UserDTO toDTO(User user);

    WishlistItemDTO toDTO(WishlistItem item);

    SearchHistoryDTO toDTO(SearchHistory history);

    @Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }

    @Named("calcRemaining")
    default int calcRemaining(User user) {
        if (user.getSubscriptionPlan() == SubscriptionPlan.PREMIUM) return Integer.MAX_VALUE;
        return Math.max(0, 5 - user.getDailySearchCount());
    }
}
