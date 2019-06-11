package de.fiz.oai.backend.controller;

import de.fiz.oai.backend.Application;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public abstract class AbstractController {

    protected void checkApplicationReady() {
        Application application = Application.getInstance();
        if (!application.isApplicationReady()) {
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }

}
