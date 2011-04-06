package org.molgenis.fieldtypes;

import org.molgenis.framework.db.Database;
import org.molgenis.framework.ui.html.DateInput;
import org.molgenis.framework.ui.html.HtmlInput;
import org.molgenis.model.MolgenisModelException;

public class DateField extends FieldType
{
	@Override
	public String getJavaPropertyType() throws MolgenisModelException
	{
		return "java.util.Date";
	}
	
	@Override
	public String getJavaAssignment(String value)
	{
		if(value == null || value.equals("")) return "null";
		return "java.sql.Date.valueOf(\""+value+"\")";
	}
	
	@Override
	public String getJavaPropertyDefault()
	{
		if(f.isAuto()) return "new java.sql.Date(new java.util.Date().getTime())";
		else return getJavaAssignment(f.getDefaultValue());
	}

	@Override
	public String getMysqlType() throws MolgenisModelException
	{
		return "DATE";
	}
	
	@Override
	public String getJavaSetterType() throws MolgenisModelException
	{
		return "Date";
	}

	@Override
	public String getHsqlType()
	{
		return "DATE";
	}
	@Override
	public String getXsdType()
	{
		return "date";
	}

	@Override
	public String getFormatString()
	{
		return "%s";
	}

	@Override
	public HtmlInput createInput(String name, String xrefEntityClassName,
			Database db) throws InstantiationException, IllegalAccessException
	{
		return new DateInput(name);
	}

	@Override
	public String getCppPropertyType() throws MolgenisModelException
	{
		return "time_t";
	}
	
	@Override
	public String getCppJavaPropertyType()
	{
		return "Ljava/sql/Date;";
	}
}
