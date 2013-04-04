package org.fcrepo.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.ExtraPropertiesStore;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * ExtraPropertiesStore implementation which stores properties in a BagIt bag's
 * bag-info.txt.
 * @see https://tools.ietf.org/html/draft-kunze-bagit-08#section-2.2.2
**/
public class BagInfoExtraPropertyStore implements ExtraPropertiesStore
{

	private Connector connector;
	private ValueFactories factories;
	private ValueFactory<String> stringFactory;
	private PropertyFactory propertyFactory;

	/* change to BagConnector when avail. */
	protected BagInfoExtraPropertyStore( Connector connector )
	{
		this.connector = connector;
		this.factories = this.connector.getContext().getValueFactories();
        this.stringFactory = factories.getStringFactory();
		this.propertyFactory = this.connector.getContext().getPropertyFactory();
	}
	public Map<Name,Property> getProperties( String id )
	{
		Map<Name,Property> properties = new HashMap<Name,Property>();
		BufferedReader buf = null;
		try
		{
			try
			{
				File bagInfo = bagInfoFile(id);
        		if (!bagInfo.exists() ) { return NO_PROPERTIES; }
				else if ( !bagInfo.canRead() )
				{
					throw new DocumentStoreException(
						"id", "Can't read " + bagInfo.getAbsolutePath()
					);
				}
				buf = new BufferedReader( new FileReader(bagInfo) );
				String key = null;
				String val = null;
				for ( String line = null; (line=buf.readLine()) != null; )
				{
					if ( key != null &&
						(line.startsWith(" ") || line.startsWith("\t")) )
					{
						// continuation of previous line
						if ( val == null )
						{
							val = line.trim();
						}
						else
						{
							val += " " + line.trim();
						}
					}
					else
					{
						// process completed value
						if ( key != null && val != null )
						{
							addProperty( properties, key, val );
						}
						key = null;
						val = null;
	
						// start new value
						if ( line.indexOf(":") != -1 )
						{
							key = line.substring(0,line.indexOf(":") ).trim();
							val = line.substring(line.indexOf(":") + 1 ).trim();
						}
					}
				}
				if ( key != null && val != null )
				{
					addProperty( properties, key, val );
				}
			}
			finally
			{
		    	buf.close();
			}
		}
		catch ( Exception ex )
		{
			throw new DocumentStoreException(id,ex);
		}
		return properties;
	}
	public void storeProperties( String id, Map<Name, Property> properties )
	{
        PrintWriter out = null;
       	try
       	{
        	try
        	{
            	File bagInfo = bagInfoFile(id);
            	out = new PrintWriter( new FileWriter(bagInfo) );
            	for ( Map.Entry<Name,Property> entry : properties.entrySet() )
            	{
                	Name name = entry.getKey();
                	Property prop = entry.getValue();
					String line = stringFactory.create( name ) + ": "
						+ stringFactory.create( prop );
                	out.println( wrapLine(line) );
            	}
        	}
        	finally
        	{
            	out.close();
        	}
		}
        catch ( Exception ex )
        {
            throw new DocumentStoreException(id,ex);
        }
    }
	public void updateProperties( String id, Map<Name,Property> properties )
	{
		Map<Name,Property> existing = getProperties(id);
		for ( Map.Entry<Name,Property> entry : properties.entrySet() )
		{
			Name name = entry.getKey();
			Property prop = entry.getValue();
			if ( prop == null )
			{
				existing.remove(name);
			}
			else
			{
				existing.put(name, prop);
			}
		}
	}
	public boolean removeProperties( String id )
	{
		File bagInfo = bagInfoFile(id);
		if ( !bagInfo.exists() )
		{
			return false;
		}
		else
		{
			return bagInfo.delete();
		}
	}

	private File bagInfoFile( String id )
	{
		/* need BagItConnector.fileFor(id) impl. */
		return new File("/users/escowles/desktop/bag-info.txt");
	}
	private void addProperty( Map<Name,Property> properties, String key,
		String val )
	{
		Name name = factories.getNameFactory().create(key);
		Property prop = propertyFactory.create(name, val);
		properties.put( name, prop );
	}
    private static String wrapLine(String value)
    {
        if ( value == null || value.length() < 79 )
        {
            return value;
        }
        StringBuffer wrapped = new StringBuffer();
        String[] words = value.split(" ");
        StringBuffer line = new StringBuffer(words[0]);
        for ( int i = 1; i < words.length; i++ )
        {
            if ( words[i].length() + line.length() < 79 )
            {
                line.append(" " + words[i]);
            }
            else
            {
                wrapped.append( line.toString() + "\n");
                line.setLength(0);
                line.append("     "+words[i]);
            }
        }
        if ( line.length() > 0 ) { wrapped.append(line.toString()); }
        return wrapped.toString();
    }
}
