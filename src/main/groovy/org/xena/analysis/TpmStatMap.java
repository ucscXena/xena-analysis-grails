package org.xena.analysis;

import grails.converters.JSON;
import org.grails.web.json.JSONElement;
import org.grails.web.json.JSONObject;

import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;


public class TpmStatMap extends TreeMap<String,TpmStat> {


  @Override
  public String toString() {
    JSONObject jsonObject = new JSONObject();

    Set<Entry<String,TpmStat>> entrySet = this.entrySet();
    for(Entry<String,TpmStat> entry : entrySet){
      JSONElement jsonValue = JSON.parse(entry.getValue().toString());
      jsonObject.put(entry.getKey(),jsonValue);

    }

    return jsonObject.toString();
  }
}
