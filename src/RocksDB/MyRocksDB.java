package RocksDB;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.ext.com.google.common.base.Utf8;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.rdfhdt.hdt.hdt.HDT;
import org.rocksdb.IngestExternalFileOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Status;

public class MyRocksDB {

	private static Options OPTIONS = new Options().setCreateIfMissing(true);
	public String dbPath;

	public MyRocksDB(String DBPath)
	{
		this.dbPath = DBPath;
		RocksDB.loadLibrary();
	}

	// get Value from RocksDB
	public  byte[] getValueFromDB(byte[] key, RocksDB DB)
	{
		byte[] result = null;
		try {
			result = DB.get(key);
		} catch (RocksDBException e) {
			e.printStackTrace();
		}
		return result;
	}

	// add to RocksDB
	public  void addListToDB(List<String> list, byte[] value, RocksDB DB)
	{
		try {
			for(String s : list)
			{
				DB.put(serializeObject(s), value);
			}

		} catch (RocksDBException e) {
			e.printStackTrace();
		}		
	}

	// add to RocksDB
	public  void addToDB(byte[] key, byte[] value, RocksDB DB)
	{
		try {
			DB.put(key, value);
		} catch (RocksDBException e) {
			e.printStackTrace();
		}		
	}

	public void deleteRocksDB()
	{
		try {
			FileUtils.deleteDirectory(new File(this.dbPath+"RocksDB"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Serialize an object to a byte array
	public byte[] serializeObject(Object obj)
	{
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bout);
			out.writeObject(obj);
			out.close();
			return bout.toByteArray();
			/*String result = new String(bout.toByteArray());
				//System.out.println("Data Serialized");
				return result.getBytes();*/
		}catch(IOException i) 
		{
			i.printStackTrace();
			return null;
		}
	}

	// De-serialize a byte array to object
	public Object deSerializeObject(byte[] byteValue)
	{
		try {
			Object obj = null;
			ByteArrayInputStream bin = new ByteArrayInputStream(byteValue);
			ObjectInputStream in = new ObjectInputStream(bin);
			try 
			{
				obj = in.readObject();
			} catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
				return null;
			}
			in.close();
			//System.out.println("Data deSerialized");
			return obj;
		}catch(IOException i) {
			i.printStackTrace();
			return null;
		}
	}
	
	public void openDB() 
	{
		// a factory method that returns a RocksDB instance
		// returns pathnames for files and directory
		try {
			FileUtils.forceMkdir(new File(this.dbPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*			ROCKS_DB_ID2Communities = RocksDB.open(OPTIONS, this.dbPath+"RocksDB/ROCKS_DB_ID2Communities.db");
		ROCKS_DB_Community2Terms = RocksDB.open(OPTIONS, this.dbPath+"RocksDB/ROCKS_DB_Community2Terms.db");
		ROCKS_DB_Term2Community = RocksDB.open(OPTIONS, this.dbPath+"RocksDB/ROCKS_DB_Term2Community.db");*/
		// do something
	}


	public void openRocksDB() 
	{
		// a factory method that returns a RocksDB instance
		// returns pathnames for files and directory
		try {
			FileUtils.forceMkdir(new File(this.dbPath+"RocksDB"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*			ROCKS_DB_ID2Communities = RocksDB.open(OPTIONS, this.dbPath+"RocksDB/ROCKS_DB_ID2Communities.db");
		ROCKS_DB_Community2Terms = RocksDB.open(OPTIONS, this.dbPath+"RocksDB/ROCKS_DB_Community2Terms.db");
		ROCKS_DB_Term2Community = RocksDB.open(OPTIONS, this.dbPath+"RocksDB/ROCKS_DB_Term2Community.db");*/
		// do something
	}
	
	
	public RocksDB opendb(String db)
	{
		try {
			// a factory method that returns a RocksDB instance
			// returns pathnames for files and directory
			return RocksDB.open(OPTIONS, this.dbPath+"/RocksDB/"+db+".db");
			// do something
		} catch (RocksDBException e) {
			// do some error handling
			e.printStackTrace();
			return null;
		}
	}
	
	
	/*public RocksDB opendb(String db)
	{
		try {
			// a factory method that returns a RocksDB instance
			// returns pathnames for files and directory
			return RocksDB.open(OPTIONS, db);
			// do something
		} catch (RocksDBException e) {
			// do some error handling
			e.printStackTrace();
			return null;
		}
	}*/


	/*public RocksDB opendb(String db)
	{
		try {
			// a factory method that returns a RocksDB instance
			// returns pathnames for files and directory
			return RocksDB.open(OPTIONS, this.dbPath+"/RocksDB/"+db+".db");
			// do something
		} catch (RocksDBException e) {
			// do some error handling
			e.printStackTrace();
			return null;
		}
	}*/
	
	// [B@7a5d012c [B@73f792cf [B@2ed94a8b [B@38082d64 [B@dfd3711

	public void iterateOverThisDB(RocksDB DB)
	{
		try (final RocksIterator iterator = DB.newIterator()) {			
			iterator.seekToFirst();
			int i=1;
			
			while(iterator.isValid())
			{
				System.out.print(i +". ");
				try {
					System.out.println(iterator.key());
					System.out.print(" --> " );				
					System.out.println(new String(iterator.value(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				i++;
				iterator.next();
			} 
		}
	}
	
}
