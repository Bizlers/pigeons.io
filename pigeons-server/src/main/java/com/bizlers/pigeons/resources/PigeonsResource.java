package com.bizlers.pigeons.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bizlers.auth.commons.core.SessionPrincipal;
import com.bizlers.auth.commons.models.Session;
import com.bizlers.pigeons.core.PigeonOperator;
import com.bizlers.pigeons.models.Pigeon;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.bizlers.pigeons.utils.Validator;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;
import com.sun.jersey.multipart.FormDataParam;

@Path("/pigeons")
@Component
@Scope("request")  
public class PigeonsResource {

	@Context
	private SecurityContext securityContext;

	@Autowired
	private PigeonOperator pigeonOperator;
	
	@Autowired
	private Validator validator;
	
	@PUT
	@Path("/activate")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Loggable(value = Loggable.DEBUG)
	public Response activatePigeons(@FormDataParam("file") InputStream uploadedInputStream) {
		Response response = null;
		if (validator.isUserInRole(securityContext, Validator.AGENT_ROLE)) {
			try {
				List<String> clientIds = pigeonOperator.readClientIds(uploadedInputStream);
				if (clientIds != null && !clientIds.isEmpty()) {
					Pigeon pigeon = null;
					for (String clientId : clientIds) {
						pigeon = pigeonOperator.readPigeon(clientId);
						if (pigeon != null)
							break;
					}
					if (pigeon != null) {
						if (validator.isValidRequest(pigeon.getBrokerName(), securityContext)) {
							pigeonOperator.activatePigeons(clientIds);
							response = Response.ok().build();
						} else {
							Logger.warn(this, "Failed to activate pigeons. Unauthorized.");
							response = Response.status(Status.UNAUTHORIZED).build();
						}
					} else {
						Logger.warn(this, "Failed to activate pigeons. No such pigeons exists.");
						response = Response.status(Status.BAD_REQUEST).entity("Pigeons does not exits.").build();
					}
				} else {
					response = Response.status(Status.BAD_REQUEST)
							.entity("Failed to activate pigeons. Provided list is null.").build();
				}
			} catch (IOException e) {
				Logger.error(this, "Failed to activate pigeons. Exception : %[exception]s", e);
				response = Response.serverError().entity(e.getMessage()).build();
			} catch (PigeonServerException e) {
				Logger.error(this, "Failed to activate pigeons. Exception : %[exception]s", e);
				response = Response.serverError().entity(e.getMessage()).build();
			}
		} else {
			response = Response.status(Status.FORBIDDEN).build();
		}
		return response;
	}

	@PUT
	@Path("/delete")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Loggable(value = Loggable.DEBUG)
	public Response deletePigeons(@FormDataParam("file") InputStream uploadedInputStream) {
		Response response = null;
		if (validator.isUserInRole(securityContext, Validator.AGENT_ROLE)) {
			try {
				List<String> clientIds = pigeonOperator.readClientIds(uploadedInputStream);

				if (clientIds != null && !clientIds.isEmpty()) {
					Pigeon pigeon = null;
					for (String clientId : clientIds) {
						pigeon = pigeonOperator.readPigeon(clientId);
						if (pigeon != null)
							break;
					}

					if (pigeon != null) {
						if (validator.isValidRequest(pigeon.getBrokerName(), securityContext)) {
							pigeonOperator.deletePigeons(clientIds);
							response = Response.ok().build();
						} else {
							Logger.warn(this, "Failed to delete pigeons. Unauthorized.");
							response = Response.status(Status.UNAUTHORIZED).build();
						}
					} else {
						Logger.warn(this, "Failed to delete pigeons. Already deleted.");
						response = Response.status(Status.OK).entity("Pigeons already deleted.").build();
					}
				} else {
					response = Response.status(Status.BAD_REQUEST)
							.entity("Failed to delete pigeons. Provided list is null.").build();
				}
			} catch (IOException e) {
				Logger.error(this, "Failed to delete pigeons. Exception : %[exception]s", e);
				response = Response.serverError().entity(e.getMessage()).build();
			} catch (PigeonServerException e) {
				Logger.error(this, "Failed to delete pigeons. Exception : %[exception]s", e);
				response = Response.serverError().entity(e.getMessage()).build();
			}
		} else {
			response = Response.status(Status.FORBIDDEN).build();
		}
		return response;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response getPigeonByRegion(
			@QueryParam("region") @DefaultValue("") String region,
			@QueryParam("publish") @DefaultValue("false") Boolean publish,
			@QueryParam("appId") long appId,
			@QueryParam("port") @DefaultValue("0") int port,
			@QueryParam("replace") @DefaultValue("false") Boolean replace,
			@QueryParam("clientId") @DefaultValue("") String clientId) {
		Response response = null;
		try {
			if (validator.isValidAppRequest(appId, securityContext)) {
				if (publish) {
					List<Pigeon> pigeonList = pigeonOperator
							.getPublishPigeonList(appId);
					if (pigeonList == null) {
						response = Response.status(Status.NOT_FOUND).build();
					} else {
						response = Response.ok().entity(pigeonList).build();
					}
				} else {
					Pigeon pigeon = null;
					if (replace) {
						pigeon = pigeonOperator.replacePigeon(appId, clientId);
						if (pigeon == null) {
							response = Response.status(Status.NOT_FOUND)
									.build();
						} else {
							response = Response.ok().entity(pigeon).build();
						}
					} else {
						if (port >= 0 && port <= 1023) {
							pigeon = pigeonOperator.getPigeon(region, appId,
									port);
							if (pigeon == null) {
								response = Response.status(Status.NOT_FOUND)
										.build();
							} else {
								response = Response.ok().entity(pigeon).build();
							}
						} else {
							response = Response.status(Status.BAD_REQUEST)
									.build();
						}
					}
				}
			} else {
				Logger.warn(this, "The request cannot be authorized.");
				response = Response.status(Status.UNAUTHORIZED).build();
			}
		} catch (PigeonServerException e) {
			Logger.error(
					this,
					"Failed to get pigeons by region. Exception : %[exception]s",
					e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage()).build();
		}
		return response;
	}

	@PUT
	@Path("{clientId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response update(@PathParam("clientId") String clientId, Pigeon pigeon) {
		Response response = null;
		if (validator.isUserInRole(securityContext, Validator.AGENT_ROLE)) {
			try {
				Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
				if (validator.isValidAppRequest(pigeon, session)) {
					if (pigeon != null) {
						pigeonOperator.updatePigeon(pigeon);
						response = Response.ok().build();
					} else {
						response = Response.status(Status.BAD_REQUEST).entity("Input Object is null").build();
					}
				} else {
					response = Response.status(Status.UNAUTHORIZED).build();
				}
			} catch (PigeonServerException e) {
				Logger.warn(this, "Failed to update pigeon. Exception: %[exception]s", e);
				response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			} 
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		return response;
	}

	@GET
	@Path("{clientId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response getPigeon(@PathParam("clientId") String clientId) {
		Response response = null;
		if (validator.isUserInRole(securityContext, Validator.AGENT_ROLE)) {
			try {
				if (clientId != null && !clientId.isEmpty()) {
					clientId = clientId.replaceAll(":", "/");
					Pigeon pigeon = pigeonOperator.readPigeon(clientId);
					if (pigeon != null) {
						Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
						if (validator.isValidAppRequest(pigeon, session)) {
							response = Response.ok().entity(pigeon).build();
						} else {
							response = Response.status(Status.UNAUTHORIZED).build();
						}
					} else {
						response = Response.status(Status.NOT_FOUND).entity("Clientid doesn't exists").build();
					}
				} else {
					response = Response.status(Status.BAD_REQUEST).entity("Kindly provide proper input").build();
				}
			} catch (PigeonServerException e) {
				Logger.warn(this, "Failed to retrieve pigeon. Exception: %[exception]s", e);
				response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			} 
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		return response;
	}

	@DELETE
	@Path("{clientId}")
	@Loggable(value = Loggable.DEBUG)
	public Response delete(@PathParam("clientId") String clientId) {
		Response response = null;
		if (validator.isUserInRole(securityContext, Validator.AGENT_ROLE)) {
			try {
				if (clientId != null && !clientId.isEmpty()) {
					clientId = clientId.replaceAll(":", "/");
					Pigeon pigeon = pigeonOperator.readPigeon(clientId);
					if (pigeon != null) {
						SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
						if (validator.isValidAppRequest(pigeon, sessionPrincipal.getSession())) {
							pigeonOperator.deletePigeon(clientId);
							response = Response.ok().build();
						} else {
							response = Response.status(Status.UNAUTHORIZED).build();
						}
					} else {
						response = Response.status(Status.NOT_FOUND).entity("Clientid doesn't exists").build();
					}
				} else {
					response = Response.status(Status.BAD_REQUEST).entity("clientid is null").build();
				}
			} catch (PigeonServerException e) {
				Logger.warn(this, "Failed to delete pigeon. Exception: %[exception]s", e);
				response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			} 
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		return response;
	}
	
}
