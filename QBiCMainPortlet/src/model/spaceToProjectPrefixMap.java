package model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

  public class spaceToProjectPrefixMap {
    public static final Map<String, String> myMap;
    static {
        Map<String, String> aMap = new HashMap<String,String>();
        aMap.put("IVAC_ALL", "QA");
        aMap.put("IVAC_CEGAT", "QC");
        myMap = Collections.unmodifiableMap(aMap);
    }
}