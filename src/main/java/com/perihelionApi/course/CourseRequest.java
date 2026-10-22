/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.course;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or replacing a course.
 *
 * @param code      optional course code such as "COMS W4156"
 * @param title     the course title
 * @param termStart the first day of the term
 * @param termEnd   the last day of the term
 */
public record CourseRequest(
        @Size(max = 32)
        @Pattern(regexp = "^$|^[A-Z]{4} [A-Z]\\d{4}$",
                message = "code must look like COMS W4156")
        String code,

        @NotBlank @Size(max = 255) String title,

        @NotNull LocalDate termStart,

        @NotNull LocalDate termEnd) {

    /**
     * Checks that the term starts before it ends. Null dates are treated as
     * valid here so that the not-null constraints report them instead.
     *
     * @return true if the range is valid or either date is null
     */
    @AssertTrue(message = "termStart must be earlier than termEnd")
    public boolean isTermRangeValid() {
        if (termStart == null || termEnd == null) {
            return true;
        }
        return termStart.isBefore(termEnd);
    }

}
