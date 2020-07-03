package at.willhaben.bitbucket.hook;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.hook.repository.*;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreRepositoryCommitMessageHook implements PreRepositoryHook<RepositoryHookRequest> {
    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext preRepositoryHookContext,
                                          @Nonnull RepositoryHookRequest repositoryHookRequest) {

        preRepositoryHookContext.registerCommitCallback(
                new CommitMessageValidationCallback(),
                RepositoryHookCommitFilter.ADDED_TO_REPOSITORY
        );
        return RepositoryHookResult.accepted();
    }

    /**
     * A call back class that checks if the commit message of last added commit(s) meet certain standards.
     * This class is registered to the hook contexts so that it accepts or rejects pushing the added commit(s)
     */
    private static class CommitMessageValidationCallback implements PreRepositoryHookCommitCallback {

        private RepositoryHookResult result = RepositoryHookResult.accepted();

        @Nonnull
        @Override
        public RepositoryHookResult getResult() {
            return result;
        }

        @Override
        public boolean onCommitAdded(@Nonnull CommitAddedDetails commitDetails) {
            Commit commit = commitDetails.getCommit();
            String branchName = commitDetails.getRef().getDisplayId();
            int validationCode = validateCommitMessage(commit, branchName);

            if(validationCode > 0) {
                return true;
            } else {
                if (validationCode == 0) {
                    result = RepositoryHookResult.rejected(
                            "JIRA ticket reference in commit message does not match the one in branch name. Please amend commit message with the correct JIRA ticket reference." +
                                    "\nExamples: \"XXX-1234: message here\", \"XXX-1234 message here\", \"No-ticket, message here\"\n",
                            "Offending commit " + commit.getId() + " on " + branchName
                    );
                    return false;
                } else if (validationCode < 0) {
                    result = RepositoryHookResult.rejected(
                            "Commit message does not meet the standards. Please amend commit message with the JIRA ticket reference." +
                                    "\nExamples: \"XXX-1234: message here\", \"XXX-1234 message here\", \"No-ticket, message here\"\n",
                            "Offending commit " + commit.getId() + " on " + branchName
                    );
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks whether the message of the given commit meets the standards i.e. it contains Jira ticket id at the beginning
         * or No-ticket (not case sensitive), if the commit is not linked to a ticket. The id is then
         * followed by a white space or a special character as a separator from the rest of the commit message.
         *
         * @param commit
         * @param branchName
         * @return 1 if the commit message is valid and reflects the branch name. 0 if the commit message is valid but the ticket id is not the one
         * mentioned in branch name. -1 if the commit message is not valid.
         */
        private int validateCommitMessage(Commit commit, String branchName) {
            String ticketIdRegex = "(\\w+[-_][0-9]+)(\\D*)?";
            String noTicketRefRegex = "((?i)no-ticket)(\\D*)?";
            String branchNameRegex = "^(\\w+\\/)?(\\w+[-_]?[0-9]+)(.*)";

            //String[] commitMessage = commit.getMessage().split("\\s");

            if (Pattern.matches(ticketIdRegex, commit.getMessage())) {
                Pattern branchNamePattern = Pattern.compile(branchNameRegex);
                Matcher branchNameMatcher = branchNamePattern.matcher(branchName);
                String ticketIdFromBranchName = null;
                if(branchNameMatcher.find()) {
                    ticketIdFromBranchName = branchNameMatcher.group(2);
                }

                Pattern ticketIdPattern = Pattern.compile(ticketIdRegex);
                Matcher ticketIdMatcher = ticketIdPattern.matcher(commit.getMessage());
                String ticketIdFromCommitMessage = null;
                if (ticketIdMatcher.find()) {
                    ticketIdFromCommitMessage = ticketIdMatcher.group(1);
                }
                //return code for saying ticket id ref in commit does not match id in branch name
                if(ticketIdFromBranchName != null && ticketIdFromCommitMessage != null) {
                    if (ticketIdFromBranchName.equalsIgnoreCase(ticketIdFromCommitMessage)) {
                        return 1;
                    } else return 0;
                } else {
                    if (ticketIdFromBranchName == null) {
                        return 0;
                    } else return -1;
                }
            } else if (Pattern.matches(noTicketRefRegex, commit.getMessage())) {
                if (Pattern.matches(branchNameRegex, branchName)) {
                    //the branch name contains a ticket id so the no ticket ref is not correct
                    return 0;
                } else {
                    return 1;
                }
            } else {
                return -1;
            }
        }
    }
}


