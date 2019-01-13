package com.bizlers.pigeons.resources;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bizlers.auth.commons.core.SessionPrincipal;
import com.bizlers.auth.commons.models.Session;
import com.bizlers.pigeons.core.AgentOperator;
import com.bizlers.pigeons.core.PigeonGenerator;
import com.bizlers.pigeons.models.Agent;
import com.bizlers.pigeons.models.Pigeon;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.bizlers.pigeons.utils.Validator;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

@Path("/agents")
@Component
@Scope("request")  
public class AgentsResource {
	
	@Context
	private SecurityContext securityContext;

	@Context
	private ServletContext servletContext;
	
	@Context
	private UriInfo uriInfo;
	
	@Autowired
	private Validator validator;
	
	@Autowired
	private AgentOperator agentOperator;
	
	@Autowired
	private PigeonGenerator pigeonGenerator;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response create(Agent agent) {
		Response response = null;
		int agentPort = Integer.parseInt((String) servletContext.getAttribute("agentPort"));
		int port = uriInfo.getRequestUri().getPort();
		if (port == agentPort) {
			if (agent != null) {
				SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
				if (sessionPrincipal.getSession().getAccountId() == agent.getAccountId()) {
					String agentName = agent.getAgentName();
					String agentIp = agent.getIp();
					if ((agentName != null && !agentName.isEmpty()) && (agentIp != null && !agentIp.isEmpty())) {
						try {
							Agent saved = agentOperator.readAgent(agentName, agentIp);
							if (saved != null) {
								response = Response.status(Status.BAD_REQUEST).entity("Agent already exists.").build();
							} else {
								agentOperator.createAgent(agent);
								response = Response.status(Status.CREATED).entity(agent.getAgentId()).build();
							}
						} catch (PigeonServerException e) {
							Logger.error(this, "Failed to create agent. Exception: %[exception]s", e);
							response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
						} 
					} else {
						response = Response.status(Status.BAD_REQUEST).entity("Agent name/ip cannot be empty.").build();
					}
				} else {
					response = Response.status(Status.UNAUTHORIZED).build();
				}
			} else {
				response = Response.status(Status.BAD_REQUEST).entity("The agent object in request is null.").build();
			}
		} else {
			response = Response.status(Status.UNAUTHORIZED).entity("Unauthorized access on port " + port).build();
		}
		return response;
	}

	private boolean isValidRequest(String agentId) {
		Response res = null;
		int agentPort = Integer.parseInt((String) servletContext.getAttribute("agentPort"));
		int port = uriInfo.getRequestUri().getPort();
		if (port == agentPort) {
			SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
			if (sessionPrincipal != null) {
				return true;
			} else {
				res = Response.status(Status.UNAUTHORIZED).build();
			}
		} else {
			res = Response.status(Status.UNAUTHORIZED).entity("Unauthorized access on port " + port).build();
		}
		Logger.warn(this, res.getEntity().toString());
		throw new WebApplicationException(res);
	}
	
	private String[] agentParams(String agentId) {
		Response res = null;
		if (agentId != null) {
			String array[] = agentId.split(":");
			String agentName = null, ip = null;
			if (array.length == 2) {
				agentName = array[0];
				ip = array[1];
			} else {
				res = Response.status(Status.BAD_REQUEST).entity("Invalid agentId").build();
			}
			if (agentName != null && !agentName.isEmpty() && ip != null && !ip.isEmpty()) {
				return array;
			} else {
				res = Response.status(Status.BAD_REQUEST).entity("Agent name/ip cannot be empty.").build();
			}
		} else {
			res = Response.status(Status.BAD_REQUEST).entity("Invalid agentId").build();
		}
		Logger.warn(this, res.getEntity().toString());
		throw new WebApplicationException(res);
	}
	
	@PUT
	@Path("{agentId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response update(@PathParam("agentId") String agentId, Agent agent) {
		Response response = null;
		try {
			if (agent.getAgentName() != null && !agent.getAgentName().isEmpty() && agent.getIp() != null
					&& !agent.getIp().isEmpty()) {
				Agent oldAgent = agentOperator.readAgent(agent.getAgentName(), agent.getIp());
				SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
				if (validator.isValidRequest(agent, oldAgent, sessionPrincipal.getSession())) {
					agentOperator.updateAgent(agent);
					response = Response.ok().build();
				} else {
					response = Response.status(Status.UNAUTHORIZED).build();
				}
			} else {
				response = Response.status(Status.BAD_REQUEST).entity("Input parameters are null").build();
			}
		} catch (PigeonServerException e) {
			Logger.error(this, "Failed to update agent. Exception: %[exception]s", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		return response;
	}

	@GET
	@Path("{agentId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response getAgent(@PathParam("agentId") String agentId) {
		Response response = null;
		if(isValidRequest(agentId)) {
		try {
			String[] params = agentParams(agentId);
			Agent agent = agentOperator.readAgent(params[0], params[1]);
			Logger.debug(this, "Agent: %s", agent);
			if (agent != null) {
				SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
				if (agent.getAccountId() == sessionPrincipal.getSession().getAccountId()) {
					response = Response.ok().entity(agent).build();
				} else {
					Logger.warn(this, "Unauthorized! Agent's account id doesn't match with session account id.");
					response = Response.status(Status.UNAUTHORIZED).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).build();
			}
		} catch (PigeonServerException e) {
			Logger.warn(this, "Failed to retrieve agent. Exception: %[exception]s", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		}
		return response;
	}

	@GET
	@Path("/{agentId}/generate")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response generateAgentPigeons(@PathParam("agentId") String agentId) {
		Response response = null;
		try {
			String[] params = agentParams(agentId);
			Agent agent = agentOperator.readAgent(params[0], params[1]);
			if (agent != null) {
				SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
				if (agent.getAccountId() == sessionPrincipal.getSession().getAccountId()) {
					pigeonGenerator.generateAgentPigeons(agent.getAgentId(), Pigeon.PIGEONCOUNT);
					response = Response.ok().build();
				} else {
					response = Response.status(Status.UNAUTHORIZED).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).build();
			}
		} catch (PigeonServerException e) {
			Logger.error(this, "Failed to generate pigeons for agent. Exception: %[exception]s", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		return response;
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response delete(@PathParam("agentId") String agentId) {
		Response response = null;
		try {
			String[] params = agentParams(agentId);
			String agentName = params[0];
			String agentIp = params[1];
			Agent agent = agentOperator.readAgent(params[0], params[1]);
			if (agent != null) {
				Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
				if (agent.getAccountId() == session.getAccountId()) {
					agentOperator.removeAgent(agentName, agentIp);
					response = Response.ok().build();
				} else {
					response = Response.status(Status.UNAUTHORIZED).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).build();
			}
		} catch (PigeonServerException e) {
			Logger.error(this, "Failed to delete agent. Exception: %[exception]s", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		} 
		return response;
	}
}
