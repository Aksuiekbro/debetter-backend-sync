package com.heliozz10.debetter.content.user.role;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTournamentKey implements Serializable {
    private Long userId;
    private Long tournamentId;
}