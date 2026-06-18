package com.heliozz10.debetter.dto.user.profile.out;

import com.heliozz10.debetter.content.user.profile.City;
import com.heliozz10.debetter.content.user.profile.institution.Institution;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class ParticipantProfileView {
    private City city;
    private Institution institution;
    private Integer rating;
}
