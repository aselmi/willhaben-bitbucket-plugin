package at.willhaben.bitbucket.hook;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.hook.repository.*;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

public class PreRepositoryCommitMessageHook implements PreRepositoryHook<RepositoryHookRequest> {
    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext preRepositoryHookContext,
                                          @Nonnull RepositoryHookRequest repositoryHookRequest) {

        preRepositoryHookContext.registerCommitCallback(
                new CompliantCommitMessageCallback(),
                RepositoryHookCommitFilter.ADDED_TO_REPOSITORY
        );
        return RepositoryHookResult.accepted();
    }

    /**
     * A call back class that checks if the commit message of last added commit(s) meet certain standards.
     * This class is registered to the hook contexts so that it accepts or rejects pushing the added commit(s)
     *
     */
    private static class CompliantCommitMessageCallback implements PreRepositoryHookCommitCallback {

        private RepositoryHookResult result = RepositoryHookResult.accepted();

        @Nonnull
        @Override
        public RepositoryHookResult getResult() {
            return result;
        }

        @Override
        public boolean onCommitAdded(@Nonnull CommitAddedDetails commitDetails) {
            Commit commit = commitDetails.getCommit();
            if (!commitMessageMeetsStandard(commit)) {
                result = RepositoryHookResult.rejected(
                        "Don't push commits with a message that does not meet the standards!",
                        "Offending commit " + commit.getId() + " on " + commitDetails.getRef().getDisplayId()
                );

                return false;
            }
            return true;
        }

        /**
         * checks whether the message of the given commit meets the standards i.e. it contains Jira ticket id at the beginning
         * or No-ticket if the commit is not linked to a ticket.
         *
         * @param commit
         * @return
         */
        private boolean commitMessageMeetsStandard(Commit commit) {
            String ticketIdRegex = "\\D+-\\d+:";
            String noTicketString = "(?i)no-ticket:";
            String[] commitMessage = commit.getMessage().trim().split("\\s+");
            return Pattern.matches(ticketIdRegex, commitMessage[0]) || Pattern.matches(noTicketString, commitMessage[0]);
        }
    }
}


