package org.eclipse.thingweb.directory.util;

import com.github.jsonldjava.core.Context;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.eclipse.rdf4j.model.Model;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * org.eclipse.thingweb.directory.util
 * <p>
 * TODO: Add class description
 * <p>
 * Author:  Anh Le-Tuan
 * <p>
 * Email:   anh.letuan@insight-centre.org
 * <p>
 * Date:  27/06/18.
 */
public class Frame2SPARQLTest {


  static String td1_panasonic_air_con =
      "/home/anhlt185/projects/wot/plugfest/2018-bundang/TDs/Panasonic/airConditioner_p1.jsonld";
  static String td2_fujitsu_wifi_agen =
      "/home/anhlt185/projects/wot/plugfest/2018-bundang/TDs/Fujitsu/Fujitsu-WiFiAgent240AC4114764.jsonld";
  static String td3_siemens_festolive =
      "/home/anhlt185/projects/wot/plugfest/2018-bundang/TDs/Siemens/FestoLive.jsonld";
  static String td4_intel_light =
      "/home/anhlt185/projects/wot/plugfest/2018-bundang/TDs/Intel/intel-light.jsonld";
  static String td5_intel_button =
      "/home/anhlt185/projects/wot/plugfest/2018-bundang/TDs/Intel/intel-button.jsonld";

  static String input1 =
      "/home/anhlt185/projects/thingweb-directory/input1.jsonld";

  static String frame1 =
      "/home/anhlt185/projects/thingweb-directory/frame1.jsonld";

  static String frame2 =
      "/home/anhlt185/projects/thingweb-directory/frame2.jsonld";

  static String frame3 =
      "/home/anhlt185/projects/thingweb-directory/frame3.jsonld";

  static String frame4 =
      "/home/anhlt185/projects/thingweb-directory/frame4.jsonld";

  static String frame5 =
      "/home/anhlt185/projects/thingweb-directory/frame5.jsonld";

  static String frame6 =
      "/home/anhlt185/projects/thingweb-directory/frame6.jsonld";
  static String frame7 =
      "/home/anhlt185/projects/thingweb-directory/frame7.jsonld";

  Repository repository;

  public Frame2SPARQLTest(){
    repository = new SailRepository(new MemoryStore());
    repository.initialize();

  }

  private void inputData(String input, boolean isView) throws IOException {
    TDTransform transform = new TDTransform(new ByteArrayInputStream(input.getBytes()));
                    input = transform.asJsonLd10();

    RepositoryConnection repositoryConnection = repository.getConnection();
    repositoryConnection.add(new StringReader(input), "", RDFFormat.JSONLD);
    repositoryConnection.commit();

    if (isView){
      System.out.println(JsonUtils.toPrettyString(JsonUtils.fromString(input)));
      Model model = Rio.parse(new StringReader(input), "", RDFFormat.JSONLD);
      printNTripleFromModel(model);
    }
  }

  private void inputDataByFilePath(String filePath, boolean isView) throws IOException {
    inputData(readStringFromFile(filePath), isView);
  }

  private static String readStringFromFile(String filePath){
    StringBuilder stringBuilder = new StringBuilder();

    try (Stream<String> stream = Files.lines(Paths.get(filePath))){
      stream.forEach(s -> stringBuilder.append(s).append("\n"));
    } catch (IOException e){
      e.printStackTrace();
    }

    return stringBuilder.toString();
  }

  private static String makeFrameObjectFromFile(String filePath){
    String frame = readStringFromFile(filePath);

    try{
      //nomarlise frame object as
      TDTransform tdTransform = new TDTransform(new ByteArrayInputStream(frame.getBytes()));
      frame = tdTransform.asJsonLd10();
    }catch (Exception e) {
      ThingDirectory.LOG.warn("Warn---> " + frame);
      ThingDirectory.LOG.warn("Can not convert json frame to Jsonld10");
    }

    return frame;
  }


  public static void printJsonFromModel(Model model) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Rio.write(model, byteArrayOutputStream, RDFFormat.JSONLD);
    Object object = null;
    try {
      object = JsonUtils.fromString(byteArrayOutputStream.toString());
      //object = JsonLdProcessor.compact(object, Frame2SPARQL.context, new JsonLdOptions());
      //System.out.println(JsonUtils.toPrettyString(object));
      ThingDirectory.LOG.info(JsonUtils.toPrettyString(object));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printNTripleFromModel(Model model) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Rio.write(model, byteArrayOutputStream, RDFFormat.NTRIPLES );
    ThingDirectory.LOG.info(byteArrayOutputStream.toString());
  }


  static int level = 0;
  public void printObject(Object object){
    level++;

    if (object instanceof List){
      List<Object> list = (List<Object>) object;

      for (Object item:list){
        printNspaces(level);
        System.out.println("[");
        printObject(item);
        printNspaces(level);
        System.out.println("]");
      }
    }

    if (object instanceof Map){
      Map<String, Object> map  = (Map<String, Object>) object;

      for (Map.Entry<String, Object> entry: map.entrySet()){
        Object value = entry.getValue();
        String key   = entry.getKey();

        if (value instanceof String){
          printNspaces(level);
          System.out.println("{" + key + " : " + value + "}");
        }
        else {
          printNspaces(level);
          System.out.println("{" + key + " : ");
          printObject(value);
          printNspaces(level);
          System.out.println("}");
        }
      }
    }

    if (object instanceof String){
      printNspaces(level);
      System.out.println(object);
    }

    level--;
  }

  private void printNspaces(int level){
    for (int i = 0; i < level*2; i++){
      System.out.print(" ");
    }
  }

  private void testFrameFromFile(String inputPath, String framePath) throws IOException {
    String input = readStringFromFile(inputPath);
    String frame = readStringFromFile(framePath);

    TDTransform transform = new TDTransform(new ByteArrayInputStream(input.getBytes()));
                    input = transform.asJsonLd10();
    testFrame(input, frame);

  }

  private void testFrame(String input, String frame) throws IOException {
    Object input__ = JsonUtils.fromString(input);
    Object frame__ = JsonUtils.fromString(frame);

    ThingDirectory.LOG.warn(frame__);

    JsonLdOptions jsonLdOptions = new JsonLdOptions();
                  jsonLdOptions.setPruneBlankNodeIdentifiers(true);

    Object framed = JsonLdProcessor.frame(input__, frame__, jsonLdOptions);
           framed = JsonLdProcessor.compact(framed, Frame2SPARQL.context, jsonLdOptions);

    System.out.println(JsonUtils.toPrettyString(framed));
  }

  //=============================================================================================================
  //=============================================================================================================
  public static void main(String[] args){
    Frame2SPARQLTest test = new Frame2SPARQLTest();
    Frame2SPARQL frame2SPARQL = new Frame2SPARQL();

    try {
      test.inputDataByFilePath(td1_panasonic_air_con, false);
      test.inputDataByFilePath(td2_fujitsu_wifi_agen, true);
      test.inputDataByFilePath(td3_siemens_festolive, false);
//      test.inputDataByFilePath(td5_intel_button, true);

//      System.out.println("Frame 1 ===================================================================================");
//      Object result1 = (new Frame2SPARQL()).frame_(test.repository.getConnection(), makeFrameObjectFromFile(frame1));
//      ThingDirectory.LOG.info("final result1: --> " + JsonUtils.toPrettyString(result1));

//      System.out.println("Frame 2 ===================================================================================");
//      Object result2 = (new Frame2SPARQL()).frame_(test.repository.getConnection(), makeFrameObjectFromFile(frame2));
//      ThingDirectory.LOG.info("final result2: --> " + JsonUtils.toPrettyString(result2));

//      System.out.println("Frame 3 ===================================================================================");
//      Object result3 = (new Frame2SPARQL()).frame_(test.repository.getConnection(), makeFrameObjectFromFile(frame3));
//      ThingDirectory.LOG.info("final result3: --> " + JsonUtils.toPrettyString(result3));

      System.out.println("Frame 4 ===================================================================================");
      Object result4 = (new Frame2SPARQL()).frame_(test.repository.getConnection(), makeFrameObjectFromFile(frame4));
      ThingDirectory.LOG.info("final result4: --> " + JsonUtils.toPrettyString(result4));

//      System.out.println("Frame 5 ===================================================================================");
//      Object result5 = (new Frame2SPARQL()).frame_(test.repository.getConnection(), makeFrameObjectFromFile(frame5));
//      ThingDirectory.LOG.info("final result5: --> " + JsonUtils.toPrettyString(result5));

//      System.out.println("Frame 6 ===================================================================================");
//      Object result6 = (new Frame2SPARQL()).frame_(test.repository.getConnection(), makeFrameObjectFromFile(frame6));
//      ThingDirectory.LOG.info("final result6: --> " + JsonUtils.toPrettyString(result6));

//      System.out.println("Frame 7 ===================================================================================");
//      Object result7 = (new Frame2SPARQL()).frame_(test.repository.getConnection(), makeFrameObjectFromFile(frame7));
//      ThingDirectory.LOG.info("final result7: --> " + JsonUtils.toPrettyString(result7));


    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}





