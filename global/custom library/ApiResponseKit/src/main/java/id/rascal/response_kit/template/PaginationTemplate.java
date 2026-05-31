package id.rascal.response_kit.template;

public record PaginationTemplate(
    int currentPage,
    int perPage,
    long totalItems,
    int totalPages,
    boolean hasNextPage,
    boolean hasPrevPage
) { }
