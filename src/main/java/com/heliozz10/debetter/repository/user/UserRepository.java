package com.heliozz10.debetter.repository.user;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.util.media.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @EntityGraph(value = "User.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Optional<User> findById(Long aLong);

    @EntityGraph(value = "User.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    Optional<User> findByUsername(String username);

    @EntityGraph(value = "User.forSecurity", type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> findForSecurityByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsernameOrEmail(String username, String email);
    List<User> findByRole(Role role);

    @Modifying
    @Query("UPDATE User u SET u.imageUrl = :url WHERE u.id = :userId")
    int updateImageUrl(@Param("userId") Long userId, @Param("url") Url url);
}
