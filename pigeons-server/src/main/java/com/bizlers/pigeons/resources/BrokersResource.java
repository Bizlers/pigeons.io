package com.bizlers.pigeons.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import com.bizlers.pigeons.core.BrokerOperator;
import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.utils.ErrorState;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.bizlers.pigeons.utils.Validator;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

@Path("/brokers")
@Component
@Scope("request")  
public class BrokersResource {

	@Context
	private SecurityContext securityContext;

	@Autowired
	private BrokerOperator brokerOperator;
	
	@Autowired
	private Validator validator;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response create(Broker broker) {
		Response response = null;
		try {
			if (broker != null) {
				if (broker.getBrokerName() == null
						|| broker.getBrokerName().isEmpty()
						|| brokerOperator.readBroker(broker.getBrokerName()) == null) {
					if (validator.isValidRequest(broker, securityContext)) {
						brokerOperator.createBroker(broker);
						response = Response.status(Status.CREATED)
								.entity(broker.getBrokerName()).build();
					} else {
						response = Response.status(Status.UNAUTHORIZED).build();
					}
				} else {
					response = Response.status(Status.BAD_REQUEST)
							.entity(ErrorState.DUPLICATE_BROKER).build();
				}
			} else {
				response = Response.status(Status.BAD_REQUEST)
						.entity("Input Object is null").build();
			}
		} catch (PigeonServerException e) {
			Logger.error(this,
					"Failed to create broker. Exception: %[exception]s", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage()).build();
		}
		return response;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response getBrokerByRegion(
			@QueryParam("brokerType") @DefaultValue("") String brokerType,
			@QueryParam("region") @DefaultValue("") String region,
			@QueryParam("ip") @DefaultValue("") String ip,
			@QueryParam("port") @DefaultValue("0") int port,
			@QueryParam("status") @DefaultValue("ALL") String status,
			@QueryParam("list") @DefaultValue("false") boolean list,
			@QueryParam("queryBroker") @DefaultValue("false") boolean queryBroker) {
		Response response = null;
		if (validator.isUserInRole(securityContext, Validator.AGENT_ROLE)) {
			Broker broker = null;
			List<Broker> brokerList = null;
			try {
				if (queryBroker) {
					if (ip != null && !ip.isEmpty() && port != 0) {
						broker = brokerOperator.readBroker(ip, port);
						if (broker != null) {
							response = Response.ok().entity(broker).build();
						} else {
							response = Response.status(Status.NOT_FOUND).build();
						}
					} else {
						response = Response.status(Status.BAD_REQUEST).entity("IP  is null or empty").build();
					}

				} else {
					if (brokerType != null && !brokerType.isEmpty()) {
						if (region != null && !region.isEmpty()) {
							if (list) {
								brokerList = brokerOperator.getBrokerList(brokerType, region, false, status);
							} else {
								broker = brokerOperator.getBroker(brokerType, region, status);
							}
						} else {
							if (list) {
								brokerList = brokerOperator.getBrokerList(brokerType, null, false, status);
							} else {
								broker = brokerOperator.getBroker(brokerType, status);
							}
						}
						if (brokerList != null && list) {
							response = Response.ok().entity(brokerList).build();
						} else if (broker != null) {
							response = Response.ok().entity(broker).build();
						} else {
							response = Response.status(Status.NOT_FOUND).build();
						}
					} else {
						response = Response.status(Status.BAD_REQUEST).entity("Input  is null").build();
					}
				}
			} catch (PigeonServerException e) {
				Logger.error(this, "Faile to retrieve broker by region. Exception: %[exception]s", e);
				response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			}
		} else {
			response = Response.status(Status.FORBIDDEN).build();
		}
		return response;
	}
	
	@PUT
	@Path("{brokerName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response update(@PathParam("brokerName") String brokerName, Broker broker) {
		Response response = null;
		try {
			if (broker != null) {
				if (broker.getBrokerName() != null
						&& !broker.getBrokerName().isEmpty()) {
					SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
					if (validator.isValidRequest(broker, sessionPrincipal.getSession())) {
						brokerOperator.updateBroker(broker);
						response = Response.ok().build();
					} else {
						response = Response.status(Status.UNAUTHORIZED).build();
					}
				}
			} else {
				response = Response.status(Status.BAD_REQUEST)
						.entity("Input Object is null").build();
			}
		} catch (PigeonServerException e) {
			Logger.warn(this,
					"Failed to update broker. Exception: %[exception]s", e);
			response = Response.status(Status.BAD_REQUEST)
					.entity(e.getMessage()).build();
		}
		return response;
	}

	@GET
	@Path("{brokerName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response getBrokerByBrokerName(@PathParam("brokerName") String brokerName) {
		Response response = null;
		try {
			if (brokerName != null && !brokerName.isEmpty()) {
				Broker broker = brokerOperator.readBroker(brokerName);
				if (broker != null) {
					SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
					if (validator.isValidRequest(broker, sessionPrincipal.getSession())) {
						response = Response.ok().entity(broker).build();
					} else {
						response = Response.status(Status.UNAUTHORIZED).build();
					}
				} else {
					response = Response.status(Status.NOT_FOUND).build();
				}
			} else
				response = Response.status(Status.BAD_REQUEST)
						.entity("brokername is null").build();
		} catch (PigeonServerException e) {
			Logger.warn(this,
					"Failed to retrieve broker. Exception: %[exception]s", e);
			response = Response.status(Status.BAD_REQUEST)
					.entity(e.getMessage()).build();
		}
		return response;
	}

	@DELETE
	@Path("{brokerName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Loggable(value = Loggable.DEBUG)
	public Response deleteBroker(@PathParam("brokerName") String brokerName) {
		Response response = null;
		try {
			if (brokerName != null && !brokerName.isEmpty()) {
				SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
				if (validator.isValidRequest(
						brokerOperator.readBroker(brokerName), sessionPrincipal.getSession())) {
					brokerOperator.removeBroker(brokerName);
					response = Response.ok().build();
				} else {
					response = Response.status(Status.UNAUTHORIZED).build();
				}
			} else {
				response = Response.status(Status.BAD_REQUEST)
						.entity("Input Object is null").build();
			}
		} catch (PigeonServerException e) {
			Logger.error(this,
					"Failed to delete broker. Exception: %[exception]s", e);
			response = Response.status(Status.BAD_REQUEST)
					.entity(e.getMessage()).build();
		}
		return response;
	}
}
