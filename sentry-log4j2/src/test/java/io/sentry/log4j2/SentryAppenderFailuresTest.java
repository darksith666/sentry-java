package io.sentry.log4j2;

import mockit.Injectable;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import io.sentry.Sentry;
import io.sentry.SentryFactory;
import io.sentry.dsn.Dsn;
import io.sentry.environment.SentryEnvironment;
import io.sentry.event.Event;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SentryAppenderFailuresTest {
    private SentryAppender sentryAppender;
    private MockUpErrorHandler mockUpErrorHandler;
    @Injectable
    private Sentry mockSentry = null;
    @SuppressWarnings("unused")
    @Mocked("sentryInstance")
    private SentryFactory mockSentryFactory = null;

    @BeforeMethod
    public void setUp() throws Exception {
        sentryAppender = new SentryAppender(mockSentry);
        mockUpErrorHandler = new MockUpErrorHandler();
        sentryAppender.setHandler(mockUpErrorHandler.getMockInstance());
    }

    @Test
    public void testSentryFailureDoesNotPropagate() throws Exception {
        new NonStrictExpectations() {{
            mockSentry.sendEvent((Event) any);
            result = new UnsupportedOperationException();
        }};

        sentryAppender.append(new Log4jLogEvent(null, null, null, Level.INFO, new SimpleMessage(""), null));

        assertThat(mockUpErrorHandler.getErrorCount(), is(1));
    }

    @Test
    public void testSentryFactoryFailureDoesNotPropagate() throws Exception {
        new NonStrictExpectations() {{
            SentryFactory.sentryInstance((Dsn) any, anyString);
            result = new UnsupportedOperationException();
        }};
        SentryAppender sentryAppender = new SentryAppender();
        sentryAppender.setHandler(mockUpErrorHandler.getMockInstance());
        sentryAppender.setDsn("protocol://public:private@host/1");

        sentryAppender.initSentry();

        assertThat(mockUpErrorHandler.getErrorCount(), is(1));
    }

    @Test
    public void testAppendFailIfCurrentThreadSpawnedBySentry() throws Exception {
        SentryEnvironment.startManagingThread();
        try {
            sentryAppender.append(new Log4jLogEvent(null, null, null, Level.INFO, new SimpleMessage(""), null));

            new Verifications() {{
                mockSentry.sendEvent((Event) any);
                times = 0;
            }};
            assertThat(mockUpErrorHandler.getErrorCount(), is(0));
        } finally {
            SentryEnvironment.stopManagingThread();
        }
    }
}