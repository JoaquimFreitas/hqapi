package org.hyperic.hq.hqapi1;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.JAXBException;
import java.util.Map;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.net.SocketException;
import java.lang.reflect.Method;

import org.hyperic.hq.hqapi1.types.ResponseStatus;
import org.hyperic.hq.hqapi1.types.ServiceError;

class HQConnection {

    private static Log _log = LogFactory.getLog(HQConnection.class);

    private String  _host;
    private int     _port;
    private boolean _isSecure;
    private String  _user;
    private String  _password;

    HQConnection(String host, int port, boolean isSecure, String user,
                 String password) {
        _host     = host;
        _port     = port;
        _isSecure = isSecure;
        _user     = user;
        _password = password;

        if (_isSecure) {
            // To allow for self signed certificates
            UntrustedSSLProtocolSocketFactory.register();
        }
    }

    /**
     * Generate an response object with the given Error.  In some cases the
     * HQ server will not give us a result, so we generate one ourselves.
     * XXX: It would be nice here if we could get JAXB to generate an
     *      interface for all response objects so we don't need to use
     *      reflection here.
     * 
     * @param res The return Class
     * @param error The ServiceError to include in the response
     * @return A response object of the given type with the given service error.
     * @throws IOException If an error occurs generating the error object.
     */
    private <T> T getErrorResponse(Class<T> res, ServiceError error)
        throws IOException
    {
        try {
            T ret = res.newInstance();

            Method setResponse = res.getMethod("setStatus", ResponseStatus.class);
            setResponse.invoke(ret, ResponseStatus.FAILURE);

            Method setError = res.getMethod("setError", ServiceError.class);
            setError.invoke(ret, error);

            return ret;
        } catch (Exception e) {
            // This shouldn't happen unless programmer error.  For instance,
            // a result object not containing a Status or Error field.
            if (_log.isDebugEnabled()) {
                _log.debug("Error generating error response", e);
            }

            throw new IOException("Error generating Error response");
        }
    }

    private String urlEncode(String s)
        throws IOException
    {
        return URLEncoder.encode(s, "UTF-8");
    }

    /**
     * Issue a GET against the API.
     *
     * @param path The web service endpoint.
     * @param params A Map of key value pairs that are converted into query
     * arguments.
     * @param resultClass The response object type.
     * @return The response object from the operation.  This response will be of
     * the type given in the resultClass argument.
     * @throws IOException If a network error occurs during the request.
     */
    <T> T doGet(String path, Map<String, String[]> params, Class<T> resultClass)
        throws IOException
    {
        GetMethod method = new GetMethod();
        method.setDoAuthentication(true);

        StringBuffer uri = new StringBuffer(path);
        if (uri.charAt(uri.length() - 1) != '?') {
            uri.append("?");
        }

        int idx = 0;
        for (Iterator i = params.keySet().iterator(); i.hasNext(); idx++) {
            String key = (String)i.next();
            String[] vals = params.get(key);
            for (String val : vals) {
                if (val != null) {
                    if (idx > 0) {
                        uri.append("&");
                    }
                    uri.append(key).append("=").append(urlEncode(val));
                }
            }
        }

        return runMethod(method, uri.toString(), resultClass);
    }

    /**
     * Issue a POST against the API.
     *
     * @param path The web service endpoint
     * @param o The object to POST.  This object will be serialized into XML
     * prior to being sent.
     * @param resultClass The result object type.
     * @return The response object from the operation.  This response will be
     * of the type given in the resultClass argument.
     * @throws IOException If a network error occurs during the request.
     */
    <T> T doPost(String path, Object o, Class<T> resultClass)
        throws IOException
    {
        PostMethod method = new PostMethod();
        method.setDoAuthentication(true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            XmlUtil.serialize(o, bos, Boolean.FALSE);
        } catch (JAXBException e) {
            ServiceError error = new ServiceError();
            error.setErrorCode("UnexpectedError");
            error.setReasonText("Unable to serialize response");
            if (_log.isDebugEnabled()) {
                _log.debug("Unable to serialize response", e);
            }
            return getErrorResponse(resultClass, error);
        }
        
        Part[] parts = {
            new StringPart("postdata", bos.toString())
        };

        method.setRequestEntity(new MultipartRequestEntity(parts,
                                                           method.getParams()));

        return runMethod(method, path, resultClass);
    }

    private <T> T runMethod(HttpMethodBase method, String uri,
                            Class<T> resultClass)
        throws IOException
    {
        String protocol = _isSecure ? "https" : "http";
        ServiceError error;
        URL url = new URL(protocol, _host, _port, uri);
        method.setURI(new URI(url.toString(), true));

        try {
            HttpClient client = new HttpClient();

            // Validate user & password inputs
            if (_user == null || _user.length() == 0) {
                error = new ServiceError();
                error.setErrorCode("LoginFailure");
                error.setReasonText("User name cannot be null or empty");
                return getErrorResponse(resultClass, error);
            }

            if (_password == null || _password.length() == 0) {
                error = new ServiceError();
                error.setErrorCode("LoginFailure");
                error.setReasonText("Password cannot be null or empty");
                return getErrorResponse(resultClass, error);
            }

            // Set Basic auth creds
            client.getParams().setAuthenticationPreemptive(true);
            Credentials defaultcreds = new UsernamePasswordCredentials(_user,
                                                                       _password);
            client.getState().setCredentials(AuthScope.ANY, defaultcreds);

            // Disable re-tries
            DefaultHttpMethodRetryHandler retryhandler =
                    new DefaultHttpMethodRetryHandler(0, true);
            client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                                             retryhandler);
            
            switch (client.executeMethod(method)) {
                case 200:
                    // We only deal with HTTP_OK responses
                    InputStream is = method.getResponseBodyAsStream();
                    try {
                        return XmlUtil.deserialize(resultClass, is);
                    } catch (JAXBException e) {
                        error = new ServiceError();
                        error.setErrorCode("UnexpectedError");
                        error.setReasonText("Unable to deserialize result");
                        if (_log.isDebugEnabled()) {
                            _log.debug("Unable to deserialize result", e);
                        }
                        return getErrorResponse(resultClass, error);
                    }
                case 401:
                    // Unauthorized
                    error = new ServiceError();
                    error.setErrorCode("LoginFailure");
                    error.setReasonText("The given username and password could " +
                                        "not be validated");
                    return getErrorResponse(resultClass, error);
                default:
                    // Some other server blow up.
                    error = new ServiceError();
                    error.setErrorCode("UnexpectedError");
                    error.setReasonText("An unexpected error occured");
                    return getErrorResponse(resultClass, error);
            }
        } catch (SocketException e) {
            throw new HttpException("Error issuing request", e);
        } finally {
            method.releaseConnection();
        }
    }
}
