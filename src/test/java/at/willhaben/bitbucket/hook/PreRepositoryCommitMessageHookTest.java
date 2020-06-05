package at.willhaben.bitbucket.hook;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.repository.*;
import com.atlassian.bitbucket.setting.Settings;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

//@RunWith(MockitoJUnitRunner.class)
public class PreRepositoryCommitMessageHookTest {

    @Mock
    private PreRepositoryHookContext context;

    @Mock
    private RepositoryHookRequest request;

    @Mock
    private Settings settings;

    @Mock
    private Repository repository;

    @Mock
    private Project project;

    @Mock
    private RefChange refChange;

    @Mock
    private MinimalRef minimalRef;

    @Mock
    private Commit commit;

    @Mock
    private CommitAddedDetails commitAddedDetails;

    @BeforeEach
    public void SetupMocks() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(repository.getProject()).thenReturn(project);

        when(context.getSettings()).thenReturn(settings);
        when(context.getRepository()).thenReturn(repository);

        when(request.getRepository()).thenReturn(repository);
        when(request.getRefChanges()).thenReturn(List.of(refChange));

        when(refChange.getRef()).thenReturn(minimalRef);
        when(refChange.getType()).thenReturn(RefChangeType.ADD);

        when(commitAddedDetails.getCommit()).thenReturn(commit);
        when(commitAddedDetails.getRef()).thenReturn(minimalRef);

    }

    //TODO always accepted as there is no commits
    @Test
    public void testHappyPath() {
        //GIVEN
        String given = "WHIAD-1564: this is a correct commit message! ";
        PreRepositoryCommitMessageHook hook = new PreRepositoryCommitMessageHook();
        when(commit.getRepository()).thenReturn(repository);
        when(commit.getId()).thenReturn("random-commit-id");
        when(commit.getMessage()).thenReturn(given);
        //WHEN
        RepositoryHookResult result = hook.preUpdate(context, request);
        //THEN
        Mockito.verify(context, times(1)).registerCommitCallback(any(PreRepositoryHookCommitCallback.class), eq(RepositoryHookCommitFilter.ADDED_TO_REPOSITORY));
        assertEquals(RepositoryHookResult.accepted(), result);
    }

    @Ignore
    public void testRejectedWhenCommitMessageHasNoTicketId() {
        //GIVEN
        String given = "This is a wrong commit message! ";
        PreRepositoryCommitMessageHook hook = new PreRepositoryCommitMessageHook();
        when(commit.getRepository()).thenReturn(repository);
        when(commit.getId()).thenReturn("random-commit-id");
        when(commit.getMessage()).thenReturn(given);
        //WHEN
        RepositoryHookResult result = hook.preUpdate(context, request);
        //THEN
        Mockito.verify(context, times(1)).registerCommitCallback(any(PreRepositoryHookCommitCallback.class), eq(RepositoryHookCommitFilter.ADDED_TO_REPOSITORY));
        assertTrue(result.isRejected());
    }
}
