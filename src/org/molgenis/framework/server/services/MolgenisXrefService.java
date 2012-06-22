package org.molgenis.framework.server.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.Query;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.server.MolgenisContext;
import org.molgenis.framework.server.MolgenisRequest;
import org.molgenis.framework.server.MolgenisResponse;
import org.molgenis.framework.server.MolgenisService;
import org.molgenis.framework.ui.html.AbstractRefInput;
import org.molgenis.util.Entity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MolgenisXrefService implements MolgenisService
{
	//private MolgenisContext mc;

	public MolgenisXrefService(MolgenisContext mc)
	{
	//	this.mc = mc;
	}

	/**
	 * Handle use of the XREF API.
	 * 
	 * 
	 * @param request
	 * @param response
	 */
	
	// side caching
	// .../xref/find?xref_entity=xxx&xref_field=yyyy&xref_label=zzzz&
	// filter=aaa
	// .../xref/find?xref_entity=xgap.data.types.Marker&xref_field=id&
	// xref_label=name&xref_filter=PV

	// alternatief => here the 'field' is the xref input itself
	// .../xref/find?entity=xxx&field=zzz&filter=aaaa
	@Override
	public void handleRequest(final MolgenisRequest req, final MolgenisResponse res)
			throws ParseException, DatabaseException, IOException
	{	
		handleXrefRequest(req.getDatabase(), req, res);
	}

	public static void handleXrefRequest(final Database db,			
			final MolgenisRequest req, final MolgenisResponse res) throws DatabaseException
	{
		final Logger logger = Logger.getLogger(MolgenisXrefService.class);
		try
		{
			logger.debug("handling XREF request " + req);

			//xrefField='id' term='GEW' xrefEntity='org.molgenis.pheno.Measur..' xrefLabels='name' filters={}
			final String searchTerm = req.getString(AbstractRefInput.SEARCH_TERM); 
			final String xrefField = req.getString(AbstractRefInput.XREF_FIELD);
			final Class<? extends Entity> xrefEntity = getClassForName(req.getString(AbstractRefInput.XREF_ENTITY));
			final String xrefLabel = req.getString(AbstractRefInput.XREF_LABELS);
			final boolean nillable = req.getBool(AbstractRefInput.NILLABLE);
			
			if(searchTerm == null || xrefField == null || xrefEntity == null || xrefLabel == null) {
				throw new IllegalArgumentException(String.format("parameters are invalid in req: %s", req));
			}			
			
			final String xref_filters = req.getString(AbstractRefInput.FILTERS);
			Collection<QueryRule> filters = null;
			final boolean hasFilters = StringUtils.isNotEmpty(xref_filters);
			if(hasFilters) {
				final Type collectionType = new TypeToken<Collection<QueryRule>>(){}.getType();
				final Gson gson = new Gson();
				filters = gson.fromJson(xref_filters, collectionType);
			}			 
			
			// get the xref labels from the string
			final List<String> xref_labels = new ArrayList<String>();
			for (String label : xrefLabel.split(","))
			{
				xref_labels.add(label.toString());
			}
			
			final HttpServletResponse response = res.getResponse();
			response.setHeader("Cache-Control", "max-age=0"); // disable cache so every request is new
			response.setContentType("application/json");

			final List<? extends Entity> records = 
				getRecords(db, searchTerm, xrefEntity, filters, xref_labels);
			
			final String json = toJSon(xrefField, xref_labels, records, nillable);
			
			logger.debug(json);

			// write out
			PrintWriter out = response.getWriter();
			out.print(json);
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new DatabaseException(e);
		}
	}


	
	private static String toJSon(final String xrefField, final List<String> xref_labels, final List<? extends Entity> records, boolean nillable)
			throws JSONException
	{		
		// transform in JSON (JavaScript Object Notation)
		Map<String, String> values = new TreeMap<String, String>(new sortStringInt());
		
		if(nillable) {
			values.put("&nbsp;", "&nbsp;");
		}		
		
		for (int i = 0; i < records.size(); i++)
		{
			String key = records.get(i).get(xrefField).toString();
			String value = "";
			for (int j = 0; j < xref_labels.size(); j++)
			{
				// hack
				if (j > 0) value += "|";
				value += records.get(i).get(xref_labels.get(j)).toString();
			}
			values.put(key, value);
		}

		//still not sorted due to JSON toString?
		return new JSONObject(values).toString();
	}

	public static List<? extends Entity> getRecords(final Database db, final String searchTerm,
			final Class<? extends Entity> xrefEntity, Collection<QueryRule> filters,
			final List<String> searchFields) throws DatabaseException
	{
		// Login login = molgenis.getApplicationController().getLogin();
		// db.setLogin(login);
		Query<?> q = db.query(xrefEntity);

		// create a CustomQuery
		// JoinQuery q = getDatabase().query(queryFields);
		if(filters != null && filters.size() > 0) {
			q.addRules(filters.toArray(new QueryRule[filters.size()]));
		}
		
		// q.addRules(xref_filters);
		if (searchTerm != null && searchTerm != "")
		{
			for (String xref_label : searchFields)
			{
				q.like(xref_label, "%" + searchTerm + "%");
				q.or();
				q.sortASC(xref_label);
			}
		}
		q.limit(100);

		List<? extends Entity> result = q.find();
		db.close();
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private static Class<? extends Entity> getClassForName(String entityName)
			throws ClassNotFoundException
	{
		return (Class<? extends Entity>) Class.forName(entityName);
	}

}

/**
 * Needed to sort the string-integer IDs in the result.
 * Using Integer would be much better, but then you'd need to
 * put a -1 ID in the map for '&nbsp;' which then fails when saved
 *
 */
class sortStringInt implements Comparator<String>
{
	public int compare(String a, String b)
	{
		if(a.equals("&nbsp;") || b.equals("&nbsp;"))
		{
			return 1; //so it gets on top
		}
		if (Integer.parseInt(a) < Integer.parseInt(b))
		{
			return 1;
		}
		else if (Integer.parseInt(a) == Integer.parseInt(b))
		{
			return 0;
		}
		else
		{
			return -1;
		}
	}
}