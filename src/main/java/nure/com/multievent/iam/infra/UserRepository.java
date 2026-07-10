package nure.com.multievent.iam.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select distinct u from User u left join fetch u.roles left join fetch u.profile order by u.email asc")
    List<User> findAllByOrderByEmailAsc();

    @Query("select distinct u from User u left join fetch u.roles left join fetch u.profile where u.id = :id")
    Optional<User> findWithRolesAndProfileById(@Param("id") UUID id);

    @Query("select count(distinct u) from User u join u.roles r where r.name = :roleName")
    long countByRoleName(@Param("roleName") RoleName roleName);

    @Query("select distinct u from User u join u.roles r where r.name = :roleName order by u.email asc")
    List<User> findDistinctByRoleNameOrderByEmailAsc(@Param("roleName") RoleName roleName);
}
