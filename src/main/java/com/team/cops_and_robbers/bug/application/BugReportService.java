package com.team.cops_and_robbers.bug.application;

import com.team.cops_and_robbers.bug.application.dto.command.BugReportCommand;
import com.team.cops_and_robbers.bug.domain.BugReport;
import com.team.cops_and_robbers.bug.repository.BugReportRepository;
import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BugReportService {

    private final BugReportRepository bugReportRepository;
    private final UserRepository userRepository;
    private final DiscordNotifier discordNotifier;

    @Transactional
    public void createBugReport(BugReportCommand command) {
        bugReportRepository.save(BugReport.createBugReport(command));

        User user = userRepository.getByUserId(command.userId());
        discordNotifier.sendBugReport(command.content(), user.getNickname());
    }
}
