package org.eclipse.thingweb.directory.util;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdUtils;
import com.github.jsonldjava.utils.JsonUtils;
import com.github.jsonldjava.utils.Obj;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.utils.TDTransform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.List;


/**
 * org.eclipse.thingweb.digrectory.util
 * <p>
 * TODO: Add class description
 * <p>
 * Author:  Anh Le-Tuan
 * <p>
 * Email:   anh.letuan@insight-centre.org
 * <p>
 * Date:  22/06/18.
 */
public class Frame2SPARQL
{
  public static ArrayList<Object> context;

  static {
    //TODO context should be extract from the input and combination with the frame
    context = new ArrayList<>();
    context.add("https://w3c.github.io/wot/w3c-wot-td-context.jsonld");
    context.add("https://w3c.github.io/wot/w3c-wot-common-context.jsonld");
    HashMap<String, Object> hashMap = new HashMap<>();
    hashMap.put("iot", "http://iotschema.org/");
    context.add(hashMap);
  }

  public static JsonLdOptions opts;

  static {
    opts = new JsonLdOptions();
    opts.setPruneBlankNodeIdentifiers(true);
//    opts.setCompactArrays(false);
//    opts.setOmitDefault(false);
  }

  private static Set<String> KEYWORDS = Sets.newHashSet(
      "@context", "@graph", "@id", "@embed", "@type" , "@default", "@explicit", "@null"
  );

  private int variableCount = 0;
  private List<String> listOfResource;

  private String getNextVar(){
    return "?x" + (++variableCount);
  }

  private String getSubject(Map<String, Object> object){
    String id = (String) object.get("@id");

    if (id == null){
      String var = getNextVar();
      listOfResource.add(var);
      return var;
    } else {
      if (!(id instanceof String)){
        throw new IllegalArgumentException("ID must be a string");
      } else {
        if (((String) id).startsWith("_:")){
          //TODO should be ignored this situation
          return getNextVar();
        } else{
          listOfResource.add("<" + id + ">");
          return "<" + id + ">";
        }
      }
    }
  }

  private String getPredicate(String predicate){
    return predicate.equals("@type") ? " a " : " <" + predicate + "> ";
  }

  //int level = 0;
  private boolean removeOptionalProperties(Object input){
    //level ++;

    if (input instanceof List){
//      if (level == 1){
//        throw new IllegalArgumentException("Frame object must be a single object ");
//      }

      List<Object> listObject = (List<Object>) input;
      List<Object> tobeRemove = new ArrayList<>();

      for (Object object:listObject){
         boolean isRemove = removeOptionalProperties(object);
         if (isRemove) tobeRemove.add(object);
      }

      listObject.removeAll(tobeRemove);

//      level--;
      return listObject.isEmpty();
    }

    if (input instanceof Map) {
      LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) input;
      LinkedHashMap<String, Object> remove = new LinkedHashMap<>();

      for (Map.Entry<String, Object> entry:map.entrySet()){
        String key    = entry.getKey();
        Object value  = entry.getValue();

        if (KEYWORDS.contains(key)) continue;

        if (value instanceof String){
          remove.put(key, value);
        }
        else
        {
          boolean removable =  removeOptionalProperties(value);// && (level!=1);

          if (removable) {
            remove.put(key, value);
          }
        }
      }
      remove.forEach((String key, Object value)-> map.remove(key));

//      level--;
      return map.isEmpty();
    }

//    level--;
    return true;
  }

  private String createQueryPatternForRootObject(String rootPattern, String subject, String predicate, Object in){

    if (in instanceof List){
      List<Object> listObject = (List<Object>) in;
      for (Object object:listObject){
        rootPattern = rootPattern + createQueryPatternForRootObject("", subject, predicate, object);
      }
    }

    if (in instanceof Map){
      Map<String, Object> map = (Map<String, Object>)in;
      String nextSub          = getSubject(map);

      if ((subject != null)&&(predicate != null)){
        rootPattern = rootPattern + subject + getPredicate(predicate) + nextSub + ". \n";
      }

      for (Map.Entry<String, Object> entry:map.entrySet()) {
        String key    = entry.getKey();

        if (key.equals("@id")) continue;

        Object value  = entry.getValue();
        rootPattern = rootPattern + createQueryPatternForRootObject("", nextSub, key, value);
      }
    }

    if (in instanceof String){
      rootPattern = rootPattern + subject + getPredicate(predicate) + "<" + in + ">. \n";
    }

    return rootPattern;
  }

  private String createDescribeQueryForRootObject(Object frameObject){
    String head  = "DESCRIBE " ;

    if (!(frameObject instanceof Map)){
      throw new IllegalArgumentException("frame object has wrong syntax " + frameObject.toString());
    }
    String rootPattern;

    listOfResource = new ArrayList<>();
    rootPattern = createQueryPatternForRootObject("",null, null, frameObject);

    Map<String, Object> map = (Map<String, Object>) frameObject;
    Object id = map.get("@id");

    if (id != null){
      head =  head + " <" + id + ">";
    }
    else
    {
      String resources = "";
      for (String resource:listOfResource){
        resources = resources + " " + resource;
      }
      head = head + resources;
    }

    return head  + "{ \n" + rootPattern + "}";
  }

  public Model describeRootObject(RepositoryConnection connection, Object frameJsonLdObject) throws IOException {

    ThingDirectory.LOG.info("before removing "  + JsonUtils.toPrettyString(frameJsonLdObject));


//    if (frameJsonLdObject instanceof Map){
//      //TODO quick fix --> unstable code
//      Object graph = ((Map<String, Object>) frameJsonLdObject).get("@graph");
//      if (graph!=null){
//        frameJsonLdObject = ((List<Object>)graph).get(0);
//      }
//    }
    //level = 0;//TODO : reset level [quick fix]

    //remove the optional properties from the frame
    removeOptionalProperties(frameJsonLdObject);

//    ThingDirectory.LOG.info("after removing "  + JsonUtils.toPrettyString(frameJsonLdObject));

    //create describe query to get the Root Object
    String query = createDescribeQueryForRootObject(frameJsonLdObject);

    ThingDirectory.LOG.info(query);

    Model model = doDescribeQuery(connection, query);

    return model;
  }

  public Model extractRootObjectAsModel(RepositoryConnection connection, Object frameJsonldObject) throws IOException {

    Object compact = JsonLdProcessor.compact(frameJsonldObject, new HashMap<>(), opts);

    Model model   = describeRootObject(connection, compact);

    return model;
  }

  public Object frame_(RepositoryConnection connection, String jsonldFrame) throws IOException {

    Object jsonldFrameObject  = JsonUtils.fromString(jsonldFrame);
           jsonldFrameObject  =  BNodeRemover.removePollutedBnode(jsonldFrameObject);

    Model model = extractRootObjectAsModel(connection, jsonldFrameObject);

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Rio.write(model, buffer, RDFFormat.JSONLD);

    Object jsonldInput  = JsonUtils.fromString(buffer.toString());
           jsonldInput = JsonLdProcessor.compact(jsonldInput, context, opts);

    try {
      ThingDirectory.LOG.info("JsonLD Input Object -->:" + JsonUtils.toPrettyString(jsonldInput));
      ThingDirectory.LOG.info("JsonLD Frame Object -->:" + JsonUtils.toPrettyString(jsonldFrameObject));

      Object framedJsonLdObject = JsonLdProcessor.frame(jsonldInput, jsonldFrameObject, opts);
             framedJsonLdObject = JsonLdProcessor.compact(framedJsonLdObject, context, opts);

      return framedJsonLdObject;

    } catch (NullPointerException e){
      //If the model is empty.
      return new HashMap<>();
    }
  }














































  //=====================================================================================
  private Model subGraphModel;
  public Object frame(RepositoryConnection connection, String frame) throws IOException {

    Object frameObject = JsonUtils.fromString(frame);

    //expanded frame object
    Object expandedFrameObject = JsonLdProcessor.expand(frameObject);

    //Get subGraph from the repository by the frame object
    if (expandedFrameObject instanceof  List){
      expandedFrameObject = ((List) expandedFrameObject).get(0);
    }

    Object subGraph = getSubGraph(connection, expandedFrameObject);

    //Perform framing to from the framed json-ld
    JsonLdOptions opts = new JsonLdOptions();
    opts.setPruneBlankNodeIdentifiers(true);
    opts.setUseNativeTypes(true);
    opts.setCompactArrays(true);

    Object framed = JsonLdProcessor.frame(subGraph, frameObject, opts);
    return framed;
  }

  private Object getSubGraph(RepositoryConnection connection, Object frame) throws IOException {

    subGraphModel = new TreeModel();

    Model parentLayer = processParentLayer(connection, (Map<String, Object>) frame);

    //seen set to prevent describing duplicated resource
    Set<String> seen = new HashSet<>();

    for (Statement statement:parentLayer){

      seen.add(statement.getSubject().toString());

      Value value = statement.getObject();

      //if the object is an resource
      if (value instanceof Resource)
      {
        //if the resource has not been described yet.
        if (!seen.contains(value.toString()))
        {
          //describe resource in further
          processChildrenLayers(connection, value.toString(), seen);
        }
      }
    }

    subGraphModel.addAll(parentLayer);

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Rio.write(subGraphModel, buffer, RDFFormat.JSONLD);

    return JsonUtils.fromString(buffer.toString());
  }

  private Model processParentLayer(RepositoryConnection connection, Map<String, Object> object){
    Object id = object.get("@id");
    Object type = object.get("@type");

    List<String> properties = new ArrayList<>();

    object.forEach((String key, Object value)->{
      if (!key.equals("@id")){
        if (!key.equals("@context")) {
          if (!key.equals("@type")) {
            properties.add(key);
          }
        }
      }
    });

    String query = generateDescribeQuery(id, properties, type);

    Model model = doDescribeQuery(connection, query);

    return model;
  }

  private void processChildrenLayers(RepositoryConnection connection, String resource, Set<String> seen){
    Model model__ = queryResource(connection, resource);
    seen.add(resource);

    for (Statement statement : model__) {
      Value value = statement.getObject();
      if (!value.equals(resource)) {
        if (value instanceof Resource) {
          if (!seen.contains(value.toString())) {
            processChildrenLayers(connection, value.toString(), seen);
          }
        }
      }
    }

    subGraphModel.addAll(model__);
  }

  private String generateDescribeQuery(Object resource, List<String> properties, Object type){
    String head = resource == null? "?x" : "<" + resource + ">";

    String query = "DESCRIBE " + head;
    query += " WHERE{ \n";

    int i = 0;
    for (String property:properties){
      query += head + " <" + property + "> " + "?o" + (++i) + ". \n";
    }

    if (type != null){
      query += generateType(head, type);
    }

    query += "}";

    return query;
  }

  private String generateType(String head, Object type){
    if (type instanceof Map){
      Map<String, Object> map = (Map<String, Object>) type;
      Object type_ = map.get("@id");
      return head + " a <" + type_ + ">.\n";
    }

    if (type instanceof List){
      List<Object> list = (List<Object>) type;

      String s = "";
      for (Object object:list){
        if (object instanceof String){
          s += head + " a <" + object + ">.\n";
        }
      }
      return s;
    }

    if (type instanceof String)
    {
      return head + " a <" + type + "> .\n";
    }

    return "";
  }

  private Model queryResourceWithFilter(RepositoryConnection connection, String subject, String type ){
    if (subject == null) { subject = "?x"; }
    else {subject = "<" + subject + ">"; }
    type = "<" + type + ">";
    String describeQuery = "DESCRIBE " + subject + " " + "WHERE { " + subject + " a " + type + ".}";
    return doDescribeQuery(connection, describeQuery);
  }

  private Model queryResource(RepositoryConnection connection, String subject){
    String describeQuery  = "DESCRIBE <" + subject + "> ";
    return doDescribeQuery(connection, describeQuery);
  }

  public Model doDescribeQuery(RepositoryConnection connection, String describeQuery){
    GraphQueryResult graphQueryResult = connection.prepareGraphQuery(describeQuery).evaluate();
    Model model = QueryResults.asModel(graphQueryResult);
    return model;
  }
}
