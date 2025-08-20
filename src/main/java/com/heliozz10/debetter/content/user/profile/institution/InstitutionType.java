package com.heliozz10.debetter.content.user.profile.institution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum InstitutionType {
    SCHOOL,
    UNIVERSITY;

    @JsonCreator
    public static InstitutionType fromString(String value) {
        return InstitutionType.valueOf(value.toUpperCase());
    }
}
