package com.team.cops_and_robbers.user.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying(clearAutomatically = true)
    @Query("delete from User u where u.id = :id")
    void deleteUserByIdDirectly(@Param("id") Long id);

    boolean existsByNickname(String nickname);

    Optional<User> findBySocialIdAndSocialType(String socialId, SocialType socialType);

    default User getByUserId(Long userId) {
        return findById(userId)
                .orElseThrow(() -> new ApplicationException(UserException.USER_NOT_FOUND));
    }
}
