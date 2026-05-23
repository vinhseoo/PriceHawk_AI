package com.pricehawk.catalog.mapper;

import com.pricehawk.catalog.domain.entity.*;
import com.pricehawk.catalog.dto.response.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    // ── Category ──────────────────────────────────────────────────────────────

    /**
     * Flat DTO — no children. Used inside Product DTO to avoid LAZY-loading the
     * entire category tree. This is the only MapStruct-generated Category→CategoryDTO
     * mapping so there is no ambiguity.
     */
    @Named("flatCategory")
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", ignore = true)
    CategoryDTO toCategoryDTOFlat(Category category);

    /**
     * Full DTO with recursive children. Implemented manually as a default method
     * to avoid MapStruct ambiguity and handle recursion safely.
     */
    default CategoryDTO toCategoryDTO(Category category) {
        if (category == null) return null;
        return new CategoryDTO(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getParent() != null ? category.getParent().getId() : null,
            category.getLevel(),
            category.getSortOrder(),
            category.isActive(),
            category.getChildren() == null ? List.of() :
                category.getChildren().stream().map(this::toCategoryDTO).toList()
        );
    }

    // ── Product ───────────────────────────────────────────────────────────────

    @Mapping(target = "category", source = "category", qualifiedByName = "flatCategory")
    @Mapping(target = "listings", source = "listings")
    ProductDTO toProductDTO(Product product);

    @Mapping(target = "categoryName", source = "category.name")
    ProductSummaryDTO toProductSummaryDTO(Product product);

    List<ProductSummaryDTO> toProductSummaryDTOList(List<Product> products);

    // ── SellerListing ─────────────────────────────────────────────────────────

    SellerListingDTO toSellerListingDTO(SellerListing listing);

    List<SellerListingDTO> toSellerListingDTOList(List<SellerListing> listings);

    // ── PriceHistory ──────────────────────────────────────────────────────────

    PriceHistoryDTO toPriceHistoryDTO(PriceHistory priceHistory);

    List<PriceHistoryDTO> toPriceHistoryDTOList(List<PriceHistory> history);
}
