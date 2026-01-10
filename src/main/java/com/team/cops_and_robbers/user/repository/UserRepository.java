package com.team.cops_and_robbers.user.repository;

import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNickname(String nickname);

    Optional<User> findBySocialIdAndSocialType(String socialId, SocialType socialType);
}
