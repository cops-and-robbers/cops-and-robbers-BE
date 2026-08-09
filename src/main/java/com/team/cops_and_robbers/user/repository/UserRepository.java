package com.team.cops_and_robbers.user.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying(clearAutomatically = true)
    @Query("delete from User u where u.id = :id")
    void deleteUserByIdDirectly(@Param("id") Long id);

    boolean existsByNickname(String nickname);

    Optional<User> findBySocialIdAndSocialType(String socialId, SocialType socialType);

    @Query("""
            select u from User u
            where (cast(:nickname as string) is null or u.nickname like concat('%', cast(:nickname as string), '%'))
            and (:socialType is null or u.socialType = :socialType)
            and (cast(:fromDate as timestamp) is null or u.createdAt >= :fromDate)
            and (cast(:toDate as timestamp) is null or u.createdAt <= :toDate)
            """)
    Page<User> findAllForAdmin(
            @Param("nickname") String nickname,
            @Param("socialType") SocialType socialType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    default User getByUserId(Long userId) {
        return findById(userId)
                .orElseThrow(() -> new ApplicationException(UserException.USER_NOT_FOUND));
    }
}
