package id.rascal.response_kit.template;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessPagedTemplate(
    boolean isSuccess,
    String message,
    Object data,
    MetaTemplate meta
) { }
