package org.hyperic.hq.hqapi1.test;

import junit.framework.TestCase;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.hqapi1.ErrorCode;
import org.hyperic.hq.hqapi1.types.ResponseStatus;
import org.hyperic.hq.hqapi1.types.ServiceError;
import org.hyperic.hq.hqapi1.types.Response;
import org.apache.log4j.PropertyConfigurator;

import java.util.Properties;

public class HQApiTestBase  extends TestCase {

    private static boolean _logConfigured = false;

    private static final String  HOST        = "localhost";
    private static final int     PORT        = 7080;
    private static final boolean IS_SECURE   = false;
    private static final String  USER        = "hqadmin";
    private static final String  PASSWORD    = "hqadmin";

    public HQApiTestBase(String name) {
        super(name);
    }

    // Ripped out from PluginMain.java
    private static final String[][] LOG_PROPS = {
        { "log4j.appender.R", "org.apache.log4j.ConsoleAppender" },
        { "log4j.appender.R.layout.ConversionPattern", "%-5p [%t] [%c{1}] %m%n" },
        { "log4j.appender.R.layout", "org.apache.log4j.PatternLayout" }
    };

    private void configureLogging(String level) {
        Properties props = new Properties();
        props.setProperty("log4j.rootLogger", level.toUpperCase() + ", R");
        props.setProperty("log4j.logger.httpclient.wire", level.toUpperCase());
        props.setProperty("log4j.logger.org.apache.commons.httpclient",
                          level.toUpperCase());

        for (String[] PROPS : LOG_PROPS) {
            props.setProperty(PROPS[0], PROPS[1]);
        }

        props.putAll(System.getProperties());
        PropertyConfigurator.configure(props);
    }

    public void setUp() throws Exception {
        super.setUp();

        if (!_logConfigured) {
            String level = System.getProperty("log");
            if (level != null) {
                configureLogging(level);
            } else {
                configureLogging("INFO");
            }
            _logConfigured = true;
        }
    }

    HQApi getApi() {
        return new HQApi(HOST, PORT, IS_SECURE, USER, PASSWORD);
    }

    HQApi getApi(String user, String password) {
        return new HQApi(HOST, PORT, IS_SECURE, user, password);
    }

    // Assert SUCCESS

    void hqAssertSuccess(Response response) {
        String error = (response.getError() != null) ?
            response.getError().getReasonText() : "";
        assertEquals(error, ResponseStatus.SUCCESS, response.getStatus());
    }

    // Assert a particular FAILURE
    
    void hqAssertFailureLoginFailure(Response response) {
        assertEquals(ResponseStatus.FAILURE, response.getStatus());
        assertEquals(response.getError().getReasonText(),
                     ErrorCode.LOGIN_FAILURE.getErrorCode(),
                     response.getError().getErrorCode());
    }

    void hqAssertFailureObjectNotFound(Response response) {
        assertEquals(ResponseStatus.FAILURE, response.getStatus());
        assertEquals(response.getError().getReasonText(),
                     ErrorCode.OBJECT_NOT_FOUND.getErrorCode(),
                     response.getError().getErrorCode());
    }

    void hqAssertFailureObjectExists(Response response) {
        assertEquals(ResponseStatus.FAILURE, response.getStatus());
        assertEquals(response.getError().getReasonText(),
                     ErrorCode.OBJECT_EXISTS.getErrorCode(),
                     response.getError().getErrorCode());
    }

    void hqAssertFailureInvalidParameters(Response response) {
        assertEquals(ResponseStatus.FAILURE, response.getStatus());
        assertEquals(response.getError().getReasonText(),
                     ErrorCode.INVALID_PARAMETERS.getErrorCode(),
                     response.getError().getErrorCode());
    }

    // Unlikely, but here for completeness.
    void hqAssertFailureUnexpectedError(Response response) {
        assertEquals(ResponseStatus.FAILURE, response.getStatus());
        assertEquals(response.getError().getReasonText(),
                     ErrorCode.UNEXPECTED_ERROR.getErrorCode(),
                     response.getError().getErrorCode());
    }

    void hqAssertFailurePermissionDenied(Response response) {
        assertEquals(ResponseStatus.FAILURE, response.getStatus());
        assertEquals(response.getError().getReasonText(),
                     ErrorCode.PERMISSION_DENIED.getErrorCode(),
                     response.getError().getErrorCode());
    }
}