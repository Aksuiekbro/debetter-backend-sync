package com.heliozz10.debetter.repository.user.profile;

import com.heliozz10.debetter.content.user.profile.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

}
