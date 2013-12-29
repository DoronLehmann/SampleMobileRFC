package com.sap.portal.demo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sap.security.api.IPrincipal;
import com.sap.security.api.UMException;
import com.sapportals.connector.ConnectorException;
import com.sapportals.connector.connection.IConnection;
import com.sapportals.connector.execution.functions.IInteraction;
import com.sapportals.connector.execution.functions.IInteractionSpec;
import com.sapportals.connector.execution.structures.IRecordSet;
import com.sapportals.portal.ivs.cg.ConnectionProperties;
import com.sapportals.portal.ivs.cg.IConnectorGatewayService;
import com.sapportals.portal.prt.component.AbstractPortalComponent;
import com.sapportals.portal.prt.component.IPortalComponentRequest;
import com.sapportals.portal.prt.component.IPortalComponentResponse;
import com.sapportals.portal.prt.resource.IResource;
import com.sapportals.portal.prt.resource.ResourceException;
import com.sapportals.portal.prt.runtime.PortalRuntime;

public class SampleMobileRFCApp extends AbstractPortalComponent
{

	public void doContent(IPortalComponentRequest request, IPortalComponentResponse response)
	{
		//Get the "action" parameter from the request
		String action = request.getParameter("action"); 
		if (action != null)
		{
			//In case that the parameter was passed - handle the action
			handleAction(request, response, action);
		}
		//include the client side resources
		includeResources(request, response);
	}

	/*
	 * The method includes the client side resources such as JavaScript files, CSS, HTML to the application
	 */
	private void includeResources(IPortalComponentRequest request, IPortalComponentResponse response)
	{
		IResource jQuery = request.getResource(IResource.SCRIPT, "scripts/jquery-1.7.2.js");
		response.include(request,jQuery);

		IResource jqm = request.getResource(IResource.SCRIPT, "scripts/jquery.mobile-1.1.1.js");
		response.include(request,jqm);

		IResource appJs = request.getResource(IResource.SCRIPT, "scripts/app.js");
		response.include(request,appJs);

		IResource jqmCss = request.getResource(IResource.CSS, "css/jquery.mobile-1.1.1.css");
		response.include(request,jqmCss);
		
		IResource mobileCss = request.getResource(IResource.CSS, "css/mobile.css");
		response.include(request,mobileCss);

		IResource appHTML = request.getResource(IResource.STATIC_PAGE, "html/app.html");
		response.include(request,appHTML);
	}


	private void handleAction(IPortalComponentRequest request, IPortalComponentResponse response, String action)
	{
		try
		{
			IConnection connection = getConnection(request, response);
			if (connection != null)
			{
				// Get the Interaction interface for executing the command
				IInteraction ix = connection.createInteractionEx();
				// Get interaction spec and set the name of the command to run
				IInteractionSpec ixspec = ix.getInteractionSpec();
				IRecordSet result = null;
				MappedRecord mr = null;
				boolean getFullList = true;
				//In case that the passed action is "search"
				if (action.equalsIgnoreCase("search"))
				{
					String term = request.getParameter("term");
					//we search the employee list for all of the employees that their last name starts with the given term
					mr = searchForEmployees(ix, ixspec, term);
					result = getRecordSet(mr, "EMPLOYEE_LIST");
				}
				//In case we want to get specific employee data
				else if (action.equalsIgnoreCase("getemploye"))
				{
					String id = request.getParameter("id");
					mr = searchEmployeeByID(ix, ixspec, id);
					result = getRecordSet(mr, "PERSONAL_DATA");
					getFullList = false;
				}			
				result.beforeFirst();
				//create a JSON array of the results
				JSONArray retVal = new JSONArray();
				int countlimit = 40;
				while (result.next() && countlimit > 0 )
				{
					JSONObject record = new JSONObject();
					//in case we are searching for all of the employees
					if (getFullList)
					{
						record.put("id", result.getString("PERNR"));
						record.put("name", result.getString("ENAME"));
						record.put("org", result.getString("ORG_TEXT"));
					}
					//getting a specific employee data
					else
					{
						record.put("LAST_NAME", result.getString("LAST_NAME"));
						record.put("FIRSTNAME", result.getString("FIRSTNAME"));
						record.put("BIRTHDATE", result.getString("BIRTHDATE"));
						IRecordSet res2 = getRecordSet(mr, "COMMUNICATION");
						res2.beforeFirst();
						while (res2.next())
						{
							if (res2.getString("USRID_LONG") != null)
							{
								record.put("USRID_LONG", res2.getString("USRID_LONG") );
							}
						}
						res2.close();
					}
					retVal.put(record);
					countlimit--;
				}
				//writing the JSON response to the client
				writeJsonResponse(request, retVal.toString());
				result.close();
			}
			else
			{
				writeJsonResponse(request, "");
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error writing to response", e);
		}
	}

	private MappedRecord searchForEmployees(IInteraction ix, IInteractionSpec ixspec, String term) throws ResourceException, javax.resource.ResourceException
	{
		ixspec.setPropertyValue("Name", "BAPI_EMPLOYEE_GETLIST");
		RecordFactory rf = ix.getRecordFactory();
		MappedRecord input = rf.createMappedRecord("input");
		input.put("OBJECT_ID", new String("00000000"));
		input.put("SEARCH_DATE", new String("10.06.2012"));
		input.put("LST_NAME_SEARK", new String(term.toUpperCase() + "*"));
		return (MappedRecord) ix.execute(ixspec, input);
	}

	private MappedRecord searchEmployeeByID(IInteraction ix, IInteractionSpec ixspec, String id) throws ResourceException, javax.resource.ResourceException
	{
		ixspec.setPropertyValue("Name", "BAPI_EMPLOYEE_GETDATA");
		RecordFactory rf = ix.getRecordFactory();
		MappedRecord input = rf.createMappedRecord("input");
		input.put("EMPLOYEE_ID", new String(id));
		input.put("SEARCH_DATE", new String("10.06.2012"));
		input.put("READDB", new String("0"));
		return (MappedRecord) ix.execute(ixspec, input);
	}

	private IRecordSet getRecordSet(MappedRecord output, String function)
	{
		return (IRecordSet) output.get(function);
	}

	/*
	 * Getting the connector gateway service
	 */
	private IConnection getConnection(IPortalComponentRequest request, IPortalComponentResponse response) throws ConnectorException, IOException, UMException
	{
		IConnectorGatewayService cgService = (IConnectorGatewayService)PortalRuntime.getRuntimeResources().getService(IConnectorGatewayService.KEY); 
		ConnectionProperties prop = new ConnectionProperties(request.getLocale(), (IPrincipal) request.getUser());
		if (cgService == null) 
		{
			writeJsonResponse(request, "");
		}
		//Get the system alias from the configurable Portal component property
		private String BACKEND_SYSTEM_ALIAS = request.getComponentContext().getProfile().getProperty("com.sap.portal.demo.SystemAlias");
		return cgService.getConnection(BACKEND_SYSTEM_ALIAS, prop);
	}

	/*
	 * Writing the JSON response to the client
	 */
	private void writeJsonResponse(IPortalComponentRequest request, String results) throws IOException
	{
		HttpServletResponse servletRes = request.getServletResponse(true);
		servletRes.setCharacterEncoding("UTF-8");
		//Set the response content type to JSON
		servletRes.setHeader("Content-Type", "application/json; charset=UTF-8");
		String utfString = results;
		if (utfString != null) {
			try 
			{
				utfString = new String(results.getBytes("UTF-8"),"UTF-8");
			}
			catch (UnsupportedEncodingException e) 
			{
				throw new RuntimeException("Error writing to response", e);
			}
		}
		servletRes.getWriter().write(utfString);
	}
}

