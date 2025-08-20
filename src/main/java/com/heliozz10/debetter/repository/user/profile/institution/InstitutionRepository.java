package com.heliozz10.debetter.repository.user.profile.institution;

import com.heliozz10.debetter.content.user.profile.institution.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

}
