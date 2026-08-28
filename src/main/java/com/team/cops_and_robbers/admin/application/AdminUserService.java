package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.command.user.AdminTermsResetCommand;
import com.team.cops_and_robbers.admin.application.dto.command.user.AdminUserListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminTermsResetPreviewResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminTermsResetResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminUserPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminUserResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.GameParticipationResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.UserDeviceResult;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final GameParticipantRepository gameParticipantRepository;

    public AdminUserPageResult getUserList(AdminUserListCommand command) {
        Page<User> userPage = userRepository.findAllForAdmin(
                command.nickname(), command.socialType(), command.fromDate(), command.toDate(),
                command.toPageable());
        return AdminUserPageResult.from(userPage);
    }

    public AdminUserResult getUser(Long userId) {
        User user = userRepository.getByUserId(userId);
        return AdminUserResult.from(user);
    }

    public AdminTermsResetPreviewResult getTermsResetPreview(AdminTermsResetCommand command) {
        long affectedUsers = userRepository.countTermsResetTargets(command.types());
        return new AdminTermsResetPreviewResult(Math.toIntExact(affectedUsers));
    }

    @Transactional
    public AdminTermsResetResult resetTermsAgreement(AdminTermsResetCommand command) {
        long affectedUsers = userRepository.resetTermsAgreement(command.types());
        return new AdminTermsResetResult(Math.toIntExact(affectedUsers));
    }

    public Map<AdminUserResult, UserDeviceResult> getDevicesByUsers(List<AdminUserResult> users) {
        List<Long> userIds = users.stream().map(AdminUserResult::id).toList();
        Map<Long, UserDeviceResult> deviceByUserId = userDeviceRepository.findByUser_IdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        ud -> ud.getUser().getId(),
                        UserDeviceResult::from
                ));
        return users.stream()
                .filter(user -> deviceByUserId.containsKey(user.id()))
                .collect(Collectors.toMap(
                        user -> user,
                        user -> deviceByUserId.get(user.id())
                ));
    }

    public Map<AdminUserResult, List<GameParticipationResult>> getGameParticipationsByUsers(
            List<AdminUserResult> users) {
        List<Long> userIds = users.stream().map(AdminUserResult::id).toList();
        Map<Long, List<GameParticipationResult>> participationsByUserId =
                gameParticipantRepository.findByUserIdInWithGame(userIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                gp -> gp.getUser().getId(),
                                Collectors.mapping(GameParticipationResult::from, Collectors.toList())
                        ));

        return users.stream().collect(Collectors.toMap(
                user -> user,
                user -> participationsByUserId.getOrDefault(user.id(), List.of())
        ));
    }
}
