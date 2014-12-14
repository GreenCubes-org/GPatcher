package org.greencubes.patcher;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.greencubes.util.Util;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Main {
	
	public static void main(String[] args) {
		if(args.length < 1)
			throw new IllegalArgumentException("You must specify path to path file");
		JSONObject jo = null;
		FileReader fr = null;
		try {
			fr = new FileReader(args[0]);
			jo = new JSONObject(new JSONTokener(fr));
		} catch(FileNotFoundException e) {
			throw new IllegalArgumentException("File '" + args[0] + "' not found!");
		} catch(JSONException e) {
			throw new IllegalArgumentException("File '" + args[0] + "' is not JSON file", e);
		} finally {
			Util.close(fr);
		}
		
	}
}