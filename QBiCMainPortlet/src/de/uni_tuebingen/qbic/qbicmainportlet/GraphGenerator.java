package de.uni_tuebingen.qbic.qbicmainportlet;

import helpers.OpenBisFunctions;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingConstants;

import main.OpenBisClient;
import model.ExperimentBean;
import model.ProjectBean;
import model.SampleBean;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SampleType;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;

// public class HelloWorld extends JFrame
public class GraphGenerator {

  private String url;
  private StreamResource res;

  public StreamResource getRes() {
    return res;
  }

  public void setRes(StreamResource res) {
    this.res = res;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public GraphGenerator(ProjectBean projectBean, OpenBisClient openBisClient) throws IOException {
    // super("openBIS graph");


    // Project p = dh.openBisClient.getProjectByCode(project);

    // Project p = dh.openBisClient.getProjectByIdentifier(projectBean);
    // System.out.println(p);

    mxGraph graph = new mxGraph();
    Object parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();

    mxStylesheet stylesheet = graph.getStylesheet();
    Hashtable<String, Object> style = new Hashtable<String, Object>();
    style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
    style.put(mxConstants.STYLE_OPACITY, 100);
    style.put(mxConstants.STYLE_FONTCOLOR, "#ffffff");
    style.put(mxConstants.STYLE_DIRECTION, "east");
    stylesheet.putCellStyle("ROUNDED", style);


    Double width = new Double(120.0);
    Double height = new Double(40.0);



    try {
      // Object found_project = graph.insertVertex(parent, null, p.getIdentifier(), 20, 20, 120,
      // height,"ROUNDED;strokeColor=#005FAA;fillColor=#005FAA");
      // Object dummy_node = graph.insertVertex(parent, null, "HALLO", 50,20,120,height, "ROUNDED");

      // could be done more efficiently with SearchCriteria (at least I hope so)
      Map<String, Integer> sample_count = new HashMap<String, Integer>();


      // List<Sample> all_project_samples = dh.openBisClient.getSamplesOfProject(p.getCode());
      // List<Sample> all_project_samples = dh.project_to_samples.get(p.getId());
      List<SampleBean> all_project_samples = new ArrayList<SampleBean>();

      for (Iterator i = projectBean.getExperiments().getItemIds().iterator(); i.hasNext();) {
        // Get the current item identifier, which is an integer.
        ExperimentBean exp = (ExperimentBean) i.next();

        for (Iterator j = exp.getSamples().getItemIds().iterator(); j.hasNext();) {
          SampleBean samp = (SampleBean) j.next();

          all_project_samples.add(samp);
        }
      }


      // count the different sample types
      Integer num_measurement_samples = new Integer(0);

      List<SampleBean> samps = new ArrayList<SampleBean>();

      for (SampleBean s : all_project_samples) {
        // System.out.println(s);
        // String key = s.getSampleTypeCode();
        String key = s.getType();

        if (sample_count.containsKey(key)) {

          sample_count.put(key, sample_count.get(key) + 1);
        } else {
          sample_count.put(key, 1);
        }

        if (key.equals("Q_TEST_SAMPLE")) {
          List<Sample> tmp = s.getParents();
          num_measurement_samples += tmp.size();
        }

        if (key.equals("Q_BIOLOGICAL_ENTITY")) {
          samps.add(s);
        }
      }

      if (sample_count.containsKey("Q_BIOLOGICAL_ENTITY")) {
        Object dummy_node_level1 =
            graph.insertVertex(parent, null, sample_count.get("Q_BIOLOGICAL_ENTITY") + "\n"
                + "biological entities", 20, 20, width, height,
                "NONE;strokeWidth=0;strokeColor=#FFFFFF;fillColor=#FFFFFF");


        if (sample_count.containsKey("Q_BIOLOGICAL_SAMPLE")) {
          Object dummy_node_level2 =
              graph.insertVertex(parent, null, sample_count.get("Q_BIOLOGICAL_SAMPLE") + "\n"
                  + "biological samples", 20, 20, width + 50, height,
                  "NONE;strokeWidth=0;strokeColor=#FFFFFF;fillColor=#FFFFFF");
          graph.insertEdge(parent, null, "", dummy_node_level1, dummy_node_level2,
              "strokeWidth=0;strokeColor=#FFFFFF");

          if (sample_count.containsKey("Q_TEST_SAMPLE")) {
            Object dummy_node_level3 =
                graph.insertVertex(parent, null, sample_count.get("Q_TEST_SAMPLE") + "\n"
                    + "test samples", 20, 20, width + 50, height,
                    "NONE;strokeWidth=0;strokeColor=#FFFFFF;fillColor=#FFFFFF");
            graph.insertEdge(parent, null, "", dummy_node_level2, dummy_node_level3,
                "strokeWidth=0;strokeColor=#FFFFFF");

            if (num_measurement_samples > 0) {
              Object dummy_node_level4 =
                  graph.insertVertex(parent, null, num_measurement_samples + "\n"
                      + "measured samples", 20, 20, width + 50, height,
                      "NONE;strokeWidth=0;strokeColor=#FFFFFF;fillColor=#FFFFFF");

              graph.insertEdge(parent, null, "", dummy_node_level3, dummy_node_level4,
                  "strokeWidth=0;strokeColor=#FFFFFF");

            }
          }
        }
      }



      List<PropertyType> bioSampleProperties =
          openBisClient.listPropertiesForType(openBisClient
              .getSampleTypeByString("Q_BIOLOGICAL_SAMPLE"));
      List<PropertyType> testSampleProperties =
          openBisClient.listPropertiesForType(openBisClient.getSampleTypeByString("Q_TEST_SAMPLE"));


      for (SampleBean s : samps) {
        String species =
            (s.getProperties().containsKey("Q_NCBI_ORGANISM")) ? s.getProperties().get(
                "Q_NCBI_ORGANISM") : "";

        //old get species code
       /* for (Map.Entry<String, String> entry : s.getProperties().entrySet()) {
          if (entry.getKey().equals("Q_NCBI_ORGANISM")) {
            species = entry.getValue();
          }
        }*/

        // TODO get Labels of properties.... for organism
        // List<PropertyType> completeProperties =
        // dh.openBisClient.listPropertiesForType(dh.openBisClient.getSampleTypeByString(s.getType()));


        Object mother_node =
            graph.insertVertex(parent, s.getId(), String.format("%s\n%s", s.getCode(), species),
                20, 20, width, height, "ROUNDED;strokeColor=#ffffff;fillColor=#0365C0");
        // subtree_vertices.add(mother_node);
        List<Sample> children = new ArrayList<Sample>();

        // children.addAll(this.open_client.getFacade().listSamplesOfSample(s.getPermId()));
        // children.addAll(dh.openBisClient.getFacade().listSamplesOfSample(s.getPermId()));

        children.addAll(s.getChildren());


        // Object group_node = graph.insertVertex(parent, null, "Entities", 20, 20, width, height);

        for (Sample c : children) {

          if (c.getSampleTypeCode().equals("Q_BIOLOGICAL_SAMPLE")) {

            String primaryTissue = "";
            String secondaryName = "";
            for (PropertyType pType : bioSampleProperties) {
              if (pType.getCode().equals("Q_PRIMARY_TISSUE")) {
                primaryTissue =
                    openBisClient.getCVLabelForProperty(
                        pType,
                        openBisClient.getSampleByIdentifier(c.getIdentifier()).getProperties()
                            .get("Q_PRIMARY_TISSUE"));
                String secID = openBisClient.getSampleByIdentifier(c.getIdentifier()).getProperties().get("Q_SECONDARY_NAME");
                if (secID != null) {
                  secondaryName = secID.replace("nan", "");
                }
              }
            }

            Object daughter_node =
                graph.insertVertex(parent, c.getPermId(),
                    String.format("%s\n%s\n%s", c.getCode(), primaryTissue, secondaryName), 20, 20, width + 50, height + 20,
                    "ROUNDED;strokeColor=#ffffff;fillColor=#51A7F9");
            graph.insertEdge(parent, null, "", mother_node, daughter_node);
            // graph.groupCells(group_node, 1.0, new Object[] {daughter_node});

            List<Sample> grandchildren = new ArrayList<Sample>();

            // grandchildren.addAll(this.open_client.getFacade().listSamplesOfSample(c.getPermId()));
            grandchildren.addAll(openBisClient.getFacade().listSamplesOfSample(c.getPermId()));

            for (Sample gc : grandchildren) {
              if (gc.getSampleTypeCode().equals("Q_TEST_SAMPLE")) {

                String testSampleType = "";
                String testSecID = "";
                for (PropertyType pType : testSampleProperties) {
                  if (pType.getCode().equals("Q_SAMPLE_TYPE")) {
                    testSampleType =
                        openBisClient.openBIScodeToString(openBisClient.getCVLabelForProperty(
                            pType, openBisClient.getSampleByIdentifier(gc.getIdentifier())
                                .getProperties().get("Q_SAMPLE_TYPE")));
                    testSecID = openBisClient.getSampleByIdentifier(gc.getIdentifier()).getProperties().get("Q_SECONDARY_NAME");
                    if (testSecID != null) {
                      testSecID = testSecID.replace("nan", "");
                    }
                  }
                }

                Object granddaughter_node =
                    graph.insertVertex(parent, gc.getPermId(),
                        String.format("%s\n%s\n%s", gc.getCode(), testSampleType, testSecID), 20, 20,  width + 50, height + 20,
                        "ROUNDED;strokeColor=#ffffff;fillColor=#70BF41");
                graph.insertEdge(parent, null, "", daughter_node, granddaughter_node);

                List<Sample> grandgrandchildren = new ArrayList<Sample>();

                // grandgrandchildren.addAll(this.open_client.getFacade().listSamplesOfSample(gc.getPermId()));
                grandgrandchildren.addAll(openBisClient.getFacade().listSamplesOfSample(
                    gc.getPermId()));

                for (Sample ggc : grandgrandchildren) {

                  String measuredSecID = "";
                  measuredSecID = openBisClient.getSampleByIdentifier(ggc.getIdentifier()).getProperties().get("Q_SECONDARY_NAME");
                  if (measuredSecID != null) {
                    measuredSecID = measuredSecID.replace("nan", "");
                  }
                  Object grandgranddaughter_node =
                      graph.insertVertex(
                          parent,
                          ggc.getPermId(),
                          String.format("%s\n%s\n%s", ggc.getCode(),
                              openBisClient.openBIScodeToString(ggc.getSampleTypeCode()), measuredSecID), 20, 20,
                          width + 50, height + 20, "ROUNDED;strokeColor=#ffffff;fillColor=#F39019");
                  graph.insertEdge(parent, null, "", granddaughter_node, grandgranddaughter_node);

                }
              }
            }
          }
        }
      }

      mxHierarchicalLayout layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
      layout.setDisableEdgeStyle(true);
      // layout.setInterHierarchySpacing(10);
      layout.setInterRankCellSpacing(100);


      // mxIGraphLayout layout = new mxFastOrganicLayout(graph);

      layout.execute(graph.getDefaultParent()); // Run the layout on the facade.\n

      // mxGraphComponent graphComponent = new mxGraphComponent(graph);
      // getContentPane().add(graphComponent);

      BufferedImage image =
          mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null);

      // String url = VaadinService.getCurrent().getBaseDirectory().getAbsolutePath() +
      // "/WEB-INF/images/graph.png";
      // this.url = url;
      final ByteArrayOutputStream bas = new ByteArrayOutputStream();

      if (image != null) {
        ImageIO.write(image, "PNG", bas);
        this.res = showFile(projectBean.getId(), "PNG", bas);
      } else {
        this.res = null;
      }
    }// end try
    finally {
      graph.getModel().endUpdate();
    }
  }


  private StreamResource showFile(final String name, final String type,
      final ByteArrayOutputStream bas) {
    // resource for serving the file contents
    final StreamSource streamSource = new StreamSource() {
      /**
			 * 
			 */
      private static final long serialVersionUID = 6632340984219486654L;

      @Override
      public InputStream getStream() {
        if (bas != null) {
          final byte[] byteArray = bas.toByteArray();
          return new ByteArrayInputStream(byteArray);
        }
        return null;
      }
    };
    StreamResource resource = new StreamResource(streamSource, name);
    return resource;
  }
  
  public GraphGenerator(List<Sample> samples, Map<String, SampleType> types, OpenBisClient openBisClient, String projectId) throws IOException {
    //Map<String, SampleType> types = openBisClient.getSampleTypes();
    // super("openBIS graph");

    // Project p = dh.openBisClient.getProjectByCode(project);

    // Project p = dh.openBisClient.getProjectByIdentifier(projectBean);
    // System.out.println(p);

    mxGraph graph = new mxGraph();
    Object parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();

    mxStylesheet stylesheet = graph.getStylesheet();
    Hashtable<String, Object> style = new Hashtable<String, Object>();
    style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
    style.put(mxConstants.STYLE_OPACITY, 100);
    style.put(mxConstants.STYLE_FONTCOLOR, "#ffffff");
    style.put(mxConstants.STYLE_DIRECTION, "east");
    stylesheet.putCellStyle("ROUNDED", style);


    Double width = new Double(120.0);
    Double height = new Double(40.0);



    try {

      // could be done more efficiently with SearchCriteria (at least I hope so)
      Map<String, Integer> sample_count = new HashMap<String, Integer>();


      // count the different sample types
      Integer num_measurement_samples = new Integer(0);

      List<Sample> samps = new ArrayList<Sample>();

      for (Sample s : samples) {
        // System.out.println(s);
        // String key = s.getSampleTypeCode();
        String key = s.getSampleTypeCode();

        if (sample_count.containsKey(key)) {

          sample_count.put(key, sample_count.get(key) + 1);
        } else {
          sample_count.put(key, 1);
        }

        if (key.equals("Q_TEST_SAMPLE")) {
          List<Sample> tmp = openBisClient.getParents(s.getCode());
          num_measurement_samples += tmp.size();
        }

        if (key.equals("Q_BIOLOGICAL_ENTITY")) {
          samps.add(s);
        }
      }

      if (sample_count.containsKey("Q_BIOLOGICAL_ENTITY")) {
        Object dummy_node_level1 =
            graph.insertVertex(parent, null, sample_count.get("Q_BIOLOGICAL_ENTITY") + "\n"
                + "biological entities", 20, 20, width, height,
                "NONE;strokeWidth=0;strokeColor=#FFFFFF;fillColor=#FFFFFF");


        if (sample_count.containsKey("Q_BIOLOGICAL_SAMPLE")) {
          Object dummy_node_level2 =
              graph.insertVertex(parent, null, sample_count.get("Q_BIOLOGICAL_SAMPLE") + "\n"
                  + "biological samples", 20, 20, width + 75, height,
                  "NONE;strokeWidth=0;strokeColor=#FFFFFF;fillColor=#FFFFFF");
          graph.insertEdge(parent, null, "", dummy_node_level1, dummy_node_level2,
              "strokeWidth=0;strokeColor=#FFFFFF");

          if (sample_count.containsKey("Q_TEST_SAMPLE")) {
            Object dummy_node_level3 =
                graph.insertVertex(parent, null, sample_count.get("Q_TEST_SAMPLE") + "\n"
                    + "test samples", 20, 20, width + 75, height,
                    "NONE;strokeWidth=0;strokeColor=#FFFFFF;fillColor=#FFFFFF");
            graph.insertEdge(parent, null, "", dummy_node_level2, dummy_node_level3,
                "strokeWidth=0;strokeColor=#FFFFFF");

            if (num_measurement_samples > 0) {
              Object dummy_node_level4 =
                  graph.insertVertex(parent, null, num_measurement_samples + "\n"
                      + "measured samples", 20, 20, width + 75, height,
                      "NONE;strokeWidth=0;strokeColor=#FFFFFF;fillColor=#FFFFFF");

              graph.insertEdge(parent, null, "", dummy_node_level3, dummy_node_level4,
                  "strokeWidth=0;strokeColor=#FFFFFF");

            }

          }



        }
      }
      
      

      List<PropertyType> bioSampleProperties =
          OpenBisFunctions.listPropertiesForType(types.get("Q_BIOLOGICAL_SAMPLE"));
      List<PropertyType> testSampleProperties =
          OpenBisFunctions.listPropertiesForType(types.get("Q_TEST_SAMPLE"));


      for (Sample s : samps) {
        String species =
            (s.getProperties().containsKey("Q_NCBI_ORGANISM")) ? s.getProperties().get(
                "Q_NCBI_ORGANISM") : "";

        //old get species code
       /* for (Map.Entry<String, String> entry : s.getProperties().entrySet()) {
          if (entry.getKey().equals("Q_NCBI_ORGANISM")) {
            species = entry.getValue();
          }
        }*/

        // TODO get Labels of properties.... for organism
        // List<PropertyType> completeProperties =
        // dh.openBisClient.listPropertiesForType(dh.openBisClient.getSampleTypeByString(s.getType()));


        Object mother_node =
            graph.insertVertex(parent, s.getIdentifier(), String.format("%s\n%s", s.getCode(), species),
                20, 20, width, height, "ROUNDED;strokeColor=#ffffff;fillColor=#0365C0");
        // subtree_vertices.add(mother_node);
        List<Sample> children = new ArrayList<Sample>();

        // children.addAll(this.open_client.getFacade().listSamplesOfSample(s.getPermId()));
        // children.addAll(dh.openBisClient.getFacade().listSamplesOfSample(s.getPermId()));

        children.addAll(openBisClient.getChildrenSamples(s));


        // Object group_node = graph.insertVertex(parent, null, "Entities", 20, 20, width, height);

        for (Sample c : children) {

          if (c.getSampleTypeCode().equals("Q_BIOLOGICAL_SAMPLE")) {

            String primaryTissue = "";
            String secondaryName = "";
            for (PropertyType pType : bioSampleProperties) {
              if (pType.getCode().equals("Q_PRIMARY_TISSUE")) {
                primaryTissue =
                    OpenBisFunctions.getCVLabelForProperty(
                        pType,
                        openBisClient.getSampleByIdentifier(c.getIdentifier()).getProperties()
                            .get("Q_PRIMARY_TISSUE"));
              }
            }
            
            String secID = openBisClient.getSampleByIdentifier(c.getIdentifier()).getProperties().get("Q_SECONDARY_NAME");
            if (secID != null) {
              secondaryName = secID.replace("nan", "");
            }
            Object daughter_node =
                graph.insertVertex(parent, c.getPermId(),
                    String.format("%s\n%s\n%s", c.getCode(), primaryTissue,secondaryName), 20, 20, width + 75, height + 20,
                    "ROUNDED;strokeColor=#ffffff;fillColor=#51A7F9");
            graph.insertEdge(parent, null, "", mother_node, daughter_node);
            // graph.groupCells(group_node, 1.0, new Object[] {daughter_node});

            List<Sample> grandchildren = new ArrayList<Sample>();

            // grandchildren.addAll(this.open_client.getFacade().listSamplesOfSample(c.getPermId()));
            grandchildren.addAll(openBisClient.getFacade().listSamplesOfSample(c.getPermId()));

            for (Sample gc : grandchildren) {
              if (gc.getSampleTypeCode().equals("Q_TEST_SAMPLE")) {

                String testSampleType = "";
                String testSecID = "";
                for (PropertyType pType : testSampleProperties) {
                  if (pType.getCode().equals("Q_SAMPLE_TYPE")) {
                    testSampleType =
                        OpenBisFunctions.openBIScodeToString(OpenBisFunctions.getCVLabelForProperty(
                            pType, openBisClient.getSampleByIdentifier(gc.getIdentifier())
                                .getProperties().get("Q_SAMPLE_TYPE")));
                  }
                }
                testSecID = openBisClient.getSampleByIdentifier(gc.getIdentifier()).getProperties().get("Q_SECONDARY_NAME");
                if (testSecID != null) {
                  testSecID = testSecID.replace("nan", "");
                }
                Object granddaughter_node =
                    graph.insertVertex(parent, gc.getPermId(),
                        String.format("%s\n%s\n%s", gc.getCode(), testSampleType, testSecID), 20, 20,width + 75, height + 20,
                        "ROUNDED;strokeColor=#ffffff;fillColor=#70BF41");
                graph.insertEdge(parent, null, "", daughter_node, granddaughter_node);

                List<Sample> grandgrandchildren = new ArrayList<Sample>();

                // grandgrandchildren.addAll(this.open_client.getFacade().listSamplesOfSample(gc.getPermId()));
                grandgrandchildren.addAll(openBisClient.getFacade().listSamplesOfSample(
                    gc.getPermId()));

                for (Sample ggc : grandgrandchildren) {

                  String measuredSecID = "";
                  String openbisSecID = openBisClient.getSampleByIdentifier(ggc.getIdentifier()).getProperties().get("Q_SECONDARY_NAME");
                  if (openbisSecID != null) {
                    measuredSecID = openbisSecID.replace("nan", "");
                  }
                  Object grandgranddaughter_node =
                      graph.insertVertex(
                          parent,
                          ggc.getPermId(),
                          String.format("%s\n%s\n%s", ggc.getCode(),
                              OpenBisFunctions.openBIScodeToString(ggc.getSampleTypeCode()),measuredSecID), 20, 20,
                              width + 75, height + 20, "ROUNDED;strokeColor=#ffffff;fillColor=#F39019");
                  graph.insertEdge(parent, null, "", granddaughter_node, grandgranddaughter_node);

                }
              }
            }
          }
        }
      }

      mxHierarchicalLayout layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
      layout.setDisableEdgeStyle(true);
      // layout.setInterHierarchySpacing(10);
      layout.setInterRankCellSpacing(100);


      // mxIGraphLayout layout = new mxFastOrganicLayout(graph);

      layout.execute(graph.getDefaultParent()); // Run the layout on the facade.\n

      // mxGraphComponent graphComponent = new mxGraphComponent(graph);
      // getContentPane().add(graphComponent);

      BufferedImage image =
          mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null);

      // String url = VaadinService.getCurrent().getBaseDirectory().getAbsolutePath() +
      // "/WEB-INF/images/graph.png";
      // this.url = url;
      final ByteArrayOutputStream bas = new ByteArrayOutputStream();

      if (image != null) {
        ImageIO.write(image, "PNG", bas);
        this.res = showFile(projectId, "PNG", bas);
      } else {
        this.res = null;
      }
    }// end try
    finally {
      graph.getModel().endUpdate();
    }
  }
  
  
  
  
  
  
  
  
  
}
