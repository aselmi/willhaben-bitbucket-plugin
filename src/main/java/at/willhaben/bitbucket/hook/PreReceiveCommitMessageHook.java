package at.willhaben.bitbucket.hook;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsBetweenRequest;
import com.atlassian.bitbucket.hook.*;
import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.repository.*;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.sal.api.component.ComponentLocator;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * This is not used but just in case the PreRepository hook did not work then this could be an alternative
 * TODO remove once the other hook worked
 */
public class PreReceiveCommitMessageHook implements PreReceiveRepositoryHook {

    @Override
    public boolean onReceive(RepositoryHookContext context, Collection<RefChange> refChanges, HookResponse hookResponse) {

        for (RefChange refChange : refChanges) {
            CommitsBetweenRequest.Builder commitsBuilder = new CommitsBetweenRequest.Builder(context.getRepository());
            commitsBuilder.exclude(refChange.getFromHash());
            commitsBuilder.include(refChange.getToHash());
            PageRequest pageRequest = new PageRequestImpl(0, 10); //max 10 commits per page
            Page<Commit> commits = ComponentLocator.getComponent(CommitService.class).getCommitsBetween(commitsBuilder.build(), pageRequest);

            for (Commit currentCommit : commits.getValues()) {
                if (!commitMessageMeetsStandard(currentCommit)) {
                    hookResponse.err().println("Please set the ticket iad at the beginning of the commit message");
                    return false;
                }
            }
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
