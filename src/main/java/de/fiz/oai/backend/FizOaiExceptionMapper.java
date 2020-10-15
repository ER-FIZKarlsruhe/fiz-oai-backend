package de.fiz.oai.backend;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.server.spi.ResponseErrorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.exceptions.AlreadyExistsException;
import de.fiz.oai.backend.exceptions.NotFoundException;

@Provider
public class FizOaiExceptionMapper implements ExceptionMapper<Throwable>, ResponseErrorMapper {

    private Logger LOGGER = LoggerFactory.getLogger(FizOaiExceptionMapper.class);

    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(Throwable t) {
        String errorMessage = buildErrorMessage(request, t);
        LOGGER.error(errorMessage);
        if (t instanceof WebApplicationException) {
            return ((WebApplicationException) t).getResponse();
        }
        else if (t instanceof NotFoundException) {
            return Response.status(404).entity(ExceptionUtils.getRootCauseMessage(t)).type("text/plain").build();
        }
        else if (t instanceof AlreadyExistsException) {
            return Response.status(409).entity(ExceptionUtils.getRootCauseMessage(t)).type("text/plain").build();
        }
        else {
            return Response.serverError().entity(ExceptionUtils.getRootCauseMessage(t)).type("text/plain").build();
        }
    }

    private String buildErrorMessage(HttpServletRequest req, Throwable t) {
        StringBuilder message = new StringBuilder();
        message.append("URL: ").append(getOriginalURL(req));
        message.append(", Method: ").append(req.getMethod());
        message.append(", Message: ").append(ExceptionUtils.getRootCauseMessage(t));
        return message.toString();
    }

    private String getOriginalURL(HttpServletRequest req) {
        String scheme = req.getScheme();
        String serverName = req.getServerName();
        int serverPort = req.getServerPort();
        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        String queryString = req.getQueryString();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath);

        if (pathInfo != null) {
            url.append(pathInfo);
        }

        if (queryString != null) {
            url.append("?").append(queryString);
        }

        return url.toString();
    }
    
}
