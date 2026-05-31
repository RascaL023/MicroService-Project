package id.rascal.response_kit.template;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetaTemplate(
    PaginationTemplate pagination,
    String timestamp
) {
    public static MetaTemplate now() {
        return new MetaTemplate(
            null,
            OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        );
    }

    public static MetaTemplate paged(PaginationTemplate pagination) {
        return new MetaTemplate(
            pagination,
            OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        );
    }
}
