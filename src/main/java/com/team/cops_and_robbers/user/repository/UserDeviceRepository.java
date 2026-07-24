package com.team.cops_and_robbers.user.repository;

import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUser(User user);

    @Query("select ud from UserDevice ud join fetch ud.user where ud.user.id in :userIds")
    List<UserDevice> findByUser_IdIn(@Param("userIds") List<Long> userIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from UserDevice ud where ud.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

}
