package de.fiz.oai.backend.controller;

import de.fiz.oai.backend.FizOAIBackendApplication;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public abstract class AbstractController {

    protected void checkApplicationReady() {
        FizOAIBackendApplication application = FizOAIBackendApplication.getInstance();
        if (!application.isApplicationReady()) {
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }

}
