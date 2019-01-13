package com.bizlers.pigeons.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bizlers.auth.commons.core.SessionPrincipal;
import com.bizlers.auth.commons.dao.UserRoleDAO;
import com.bizlers.auth.commons.models.Session;
import com.bizlers.auth.commons.utils.AuthorizationException;
import com.bizlers.pigeons.dao.ApplicationDAO;
import com.bizlers.pigeons.models.Application;
import com.bizlers.pigeons.utils.ErrorState;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.bizlers.pigeons.utils.Validator;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

@Path("/applications")
@Component
@Scope("request")  
public class ApplicationsResource {

	@Context
	private SecurityContext securityContext;

	private static final String APPLICATION_ROLE = "application";

	private static final String USER_ROLE = "user";

	@Autowired
	private Validator validator;
	
	@Autowired
	private UserRoleDAO userRoleDao;
	
	@Autowired
	private ApplicationDAO applicationDAO;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response register(Application application) {
		Response response = null;
		Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
		if (validator.isValidApplication(application, session)) {
			try {
				if (applicationDAO.get(application.getEmailId()) != null) {
					response = Response.status(Response.Status.BAD_REQUEST)
							.entity(ErrorState.DUPLICATE_APPLICATION).build();
				} else {
					long accountId = session.getAccountId();
					if(!userRoleDao.isUserInRole(accountId, APPLICATION_ROLE)) {
						userRoleDao.insertUserRole(session.getAccountId(),
							APPLICATION_ROLE);
					}
					if(!userRoleDao.isUserInRole(accountId, USER_ROLE)) {
						userRoleDao.insertUserRole(session.getAccountId(),
							USER_ROLE);
					}
					applicationDAO.create(application);
					/*GenerateCertificates.INSTANCE.generate(application,
							CERTIFICATE_DURATION_IN_DAYS);*/
					response = Response.status(Response.Status.CREATED)
							.entity(application.getAppId()).build();
				}
			} catch (PigeonServerException e) {
				Logger.error(
						this,
						"Failed to register application. Exception: %[exception]s",
						e);
				response = Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(e.getMessage()).build();
			} catch (AuthorizationException e) {
				Logger.error(
						this,
						"Failed to register application. Exception: %[exception]s",
						e);
				response = Response.status(Status.UNAUTHORIZED)
						.entity(e.getMessage()).build();
			}
		} else {
			response = Response.status(Response.Status.BAD_REQUEST).build();
		}
		return response;
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response getApplicationsForUser(@QueryParam("userId") long userId) {
		Response response = null;
		Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
		if (userId == session.getAccountId()) {
			Application application = applicationDAO.getByAccountId(userId);
			if (application != null) {
				response = Response.ok().entity(application).build();
			} else {
				response = Response.status(Response.Status.NOT_FOUND).build();
			}
		} else {
			Logger.warn(this, "Unauthorized request to access application. userId = %d", userId);
			response = Response.status(Response.Status.UNAUTHORIZED).build();
		}
		return response;
	}

	
	@PUT
	@Path("{applicationId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response register(
			@PathParam("applicationId") long applicationId, Application application) {
		Response response = null;
		try {
			Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
			if (validator.isValidApplication(application, applicationId,
					session)
					&& (applicationDAO.get(application.getEmailId()) != null)) {
				applicationDAO.update(application);
				response = Response.ok().build();
			} else {
				response = Response.status(Response.Status.BAD_REQUEST).build();
			}
		} catch (PigeonServerException e) {
			Logger.error(this,
					"Failed to register application. Exception: %[exception]s",
					e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage()).build();
		}
		return response;
	}

	@GET
	@Path("{applicationId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response getApplication(
			@PathParam("applicationId") long applicationId) {
		Response response = null;
		try {
			Application application = applicationDAO.get(applicationId);
			if (application != null) {
				Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
				if (session.getAccountId() == application.getAccountId()) {
					response = Response.ok().entity(application).build();
				} else {
					response = Response.status(Response.Status.UNAUTHORIZED)
							.build();
				}
			} else {
				response = Response.status(Response.Status.NOT_FOUND).build();
			}
		} catch (PigeonServerException e) {
			Logger.warn(this,
					"Failed to retrieve application. Exception: %[exception]s",
					e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage()).build();
		}
		return response;
	}

	@DELETE
	@Path("{applicationId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response unRegister(@PathParam("applicationId") long applicationId) {
		Response response = null;
		try {
			Application application = applicationDAO.get(applicationId);
			if (application != null) {
				Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
				if (session.getAccountId() == application.getAccountId()) {
					userRoleDao.deleteUserRole(session.getAccountId(),
							APPLICATION_ROLE);
					userRoleDao.deleteUserRole(session.getAccountId(),
							USER_ROLE);
					applicationDAO.delete(application);
					response = Response.ok().build();
				} else {
					response = Response.status(Response.Status.UNAUTHORIZED)
							.build();
				}
			} else {
				response = Response.status(Response.Status.NOT_FOUND).build();
			}
		} catch (PigeonServerException e) {
			Logger.error(
					this,
					"Failed to unregister application. Exception: %[exception]s",
					e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage()).build();
		} catch (AuthorizationException e) {
			Logger.error(
					this,
					"Failed to unregister application. Exception: %[exception]s",
					e);
			response = Response.status(Status.UNAUTHORIZED)
					.entity(e.getMessage()).build();
		}
		return response;
	}
	
}
