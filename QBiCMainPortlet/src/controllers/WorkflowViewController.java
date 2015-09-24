package controllers;

import helpers.Utils;

import java.io.Serializable;
import java.net.ConnectException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import logging.Log4j2Logger;
import main.OpenBisClient;

import org.apache.commons.lang.NotImplementedException;

import parser.Parser;
import properties.Factor;

import submitter.SubmitFailedException;
import submitter.Submitter;
import submitter.Workflow;
import submitter.parameters.Parameter;
import ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.FileInfoDssDTO;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.plugin.query.shared.api.v1.dto.QueryTableModel;

import com.vaadin.data.Container;
import com.vaadin.data.util.BeanItemContainer;

import de.uni_tuebingen.qbic.beans.DatasetBean;
import de.uni_tuebingen.qbic.qbicmainportlet.DatasetView;
import de.uni_tuebingen.qbic.qbicmainportlet.ExperimentView;
import de.uni_tuebingen.qbic.qbicmainportlet.PatientView;
import de.uni_tuebingen.qbic.qbicmainportlet.ProjectView;
import de.uni_tuebingen.qbic.qbicmainportlet.SampleView;

public class WorkflowViewController {
  private OpenBisClient openbis;
  private Submitter submitter;

  private logging.Logger LOGGER = new Log4j2Logger(WorkflowViewController.class);
  private String user;
  private final String wf_id = "Q_WF_ID";
  private final String wf_version = "Q_WF_VERSION";
  private final String wf_executer = "Q_WF_EXECUTED_BY";
  private final String wf_started = "Q_WF_STARTED_AT";
  private final String wf_status = "Q_WF_STATUS";
  private final String wf_name = "Q_WF_NAME";
  private final String openbis_dss = "DSS1";

  // used by Microarray QC Workflow. See function fetchExperimentalProperties
  private String expPropTSV;
  private Set<String> expFactors;

  private enum workflow_statuses {
    RUNNING
  };

  public WorkflowViewController(Submitter submitter, OpenBisClient openbis, String user) {
    LOGGER.debug("controller 10:31");
    this.openbis = openbis;
    this.submitter = submitter;
    this.user = user;
  }

  /**
   * Returns a Container with the informations of de.uni_tuebingen.qbic.beans.DatasetBean.
   * 
   * @param datasets
   * @return
   */
  public Container fillTable(List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> datasets,
      String projectID) {
    HashMap<String, DataSet> dataMap =
        new HashMap<String, ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet>();
    BeanItemContainer<DatasetBean> container =
        new BeanItemContainer<DatasetBean>(DatasetBean.class);
    Map<String, List<String>> params = new HashMap<String, List<String>>();
    List<String> dsCodes = new ArrayList<String>();

    // if value is true, filter out
    HashMap<String, Boolean> fileInfo = new HashMap<String, Boolean>();


    for (ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet ds : datasets) {
      FileInfoDssDTO[] filelist = ds.listFiles("original", true);

      // determine if folder or checksum file which should not displayed for workflows
      for (FileInfoDssDTO f : filelist) {
        fileInfo.put(f.getPathInDataSet(),
            f.isDirectory() | f.getPathInDataSet().endsWith("sha256sum"));
      }

      dsCodes.add(ds.getCode());
      dataMap.put(ds.getCode(), ds);
    }

    params.put("codes", dsCodes);
    QueryTableModel res = openbis.queryFileInformation(params);
    List<String> fileNames = new ArrayList<String>();// these can be used to map to external
                                                     // ids/secondary names
    for (Serializable[] ss : res.getRows()) {
      String dsCode = (String) ss[0];
      fileNames.add((String) ss[2]);
      // when tryGetInternalPathInDataStore is used here for project like qmari it takes over a
      // minute. without 0.02s
      String path =
      /* dataMap.get(dsCode).getDataSetDss().tryGetInternalPathInDataStore() + */(String) ss[1];
      // path = path.replace("/mnt/DSS1", "/mnt/nfs/qbic");


      if (!fileInfo.get(path)) {
        DatasetBean bean =
            new DatasetBean((String) ss[2], dataMap.get(dsCode).getDataSetTypeCode(), dsCode, path,
                dataMap.get(dsCode).getSampleIdentifierOrNull());
        container.addBean(bean);
      }
    }
    if (true) // TODO only do this for some workflows?
      fetchExperimentalProperties(projectID, fileNames);

    return container;
  }

  /**
   * Register a new Workflow experiment in openBIS. Should be done when starting the workflow.
   * Experiment name is automatically created from the project name and the number of existing
   * experiments in that project. Standard workflow experiment fields are also initialized.
   * 
   * @param space space code
   * @param project project code
   * @param typecode openbis type code of the workflow
   * @param wfName name of the workflow
   * @param wfVersion version of the workflow
   * @param userID the user that starts the workflow
   * @return Code of the newly registered experiment
   */
  public String registerWFExperiment(String space, String project, String typecode, String wfName,
      String wfVersion, String userID) {
    int last = 0;
    for (Experiment e : openbis.getExperimentsOfProjectByIdentifier((new StringBuilder("/"))
        .append(space).append("/").append(project).toString())) {
      String[] codeSplit = e.getCode().split("E");
      String number = codeSplit[codeSplit.length - 1];
      int num = 0;
      try {
        num = Integer.parseInt(number);
      } catch (NumberFormatException ex) {
      }
      last = Math.max(num, last);
    }

    LOGGER.debug("Space: " + space);
    LOGGER.debug("Project: " + project);
    LOGGER.debug("StringBuilder: "
        + new StringBuilder("/").append(space).append("/").append(project).toString());

    String code = project + "E" + Integer.toString(last + 1);

    LOGGER.debug("Code: " + code);

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("code", code);
    params.put("type", typecode);
    params.put("project", project);
    params.put("space", space);

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put(wf_name, wfName);
    properties.put(wf_version, wfVersion);
    properties.put(wf_executer, userID);
    properties.put(wf_started, Utils.getTime());
    properties.put(wf_status, workflow_statuses.RUNNING.toString());
    params.put("properties", properties);

    openbis.ingest(openbis_dss, "register-exp", params);
    return code;
  }


  public List<String> getConnectedSamples(List<DatasetBean> datasetBeans) {
    List<String> sampleIDs = new ArrayList<String>();
    for (DatasetBean bean : datasetBeans) {
      sampleIDs.add(bean.getSampleIdentifier());
    }
    return sampleIDs;
  }

  public String registerWFSample(String space, String project, String experiment, String typecode,
      List<String> parents) {
    int last = 0;
    for (Sample s : openbis.getSamplesofExperiment((new StringBuilder("/")).append(space)
        .append("/").append(project).append("/").append(experiment).toString())) {
      String[] codeSplit = s.getCode().split("R");
      String number = codeSplit[codeSplit.length - 1];
      int num = 0;
      try {
        num = Integer.parseInt(number);
      } catch (NumberFormatException ex) {
      }
      last = Math.max(num, last);
    }

    String code =
        (new StringBuilder(experiment)).append("R").append(Integer.toString(last + 1)).toString();
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("code", code);
    params.put("type", typecode);

    params.put("sample_class", "");
    params.put("parents", parents);

    params.put("project", project);
    params.put("space", space);
    params.put("experiment", experiment);

    Map<String, Object> properties = new HashMap<String, Object>();
    // TODO fill properties
    params.put("properties", properties);

    openbis.ingest(openbis_dss, "register-samp", params);
    return code;
  }


  /**
   * Set the workflow ID for a workflow experiment. This must be the experiment whose code has been
   * given to the submitter to ensure correct registration of the results and log files.
   * 
   * @param space space code
   * @param project project code
   * @param experiment experiment code
   * @param wfID workflow ID created by the submitter for this workflow experiment
   */
  public void setWorkflowID(String space, String project, String experiment, String wfID) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("identifier", "/" + space + "/" + project + "/" + experiment);
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put(wf_id, wfID);
    params.put("properties", properties);
    openbis.ingest(openbis_dss, "notify-user", params);
  }

  /**
   * returns all known workflows, that can be executed with the given filetype
   * 
   * @param fileType
   * @return
   */
  public BeanItemContainer<Workflow> suitableWorkflows(String fileType) {
    try {
      return submitter.getAvailableSuitableWorkflows(fileType);
    } catch (Exception e) {
      e.printStackTrace();
      return new BeanItemContainer<Workflow>(Workflow.class);
    }
  }

  /**
   * returns all known workflows, that can be executed with one of the given filetypes
   * 
   * @param fileType
   * @return
   */
  public BeanItemContainer<Workflow> suitableWorkflows(List<String> fileType) {
    try {
      return submitter.getAvailableSuitableWorkflows(fileType);
    } catch (Exception e) {
      // e.printStackTrace();
      LOGGER.debug("No suitable workflows founds.");
      return new BeanItemContainer<Workflow>(Workflow.class);
    }
  }

  /**
   * returns all known workflows, that can be executed with one of the given filetypes
   * 
   * @param fileType
   * @return
   */
  public BeanItemContainer<Workflow> suitableWorkflowsByExperimentType(String experimentType) {
    try {
      return submitter.getWorkflowsByExperimentType(experimentType);
    } catch (Exception e) {
      e.printStackTrace();
      return new BeanItemContainer<Workflow>(Workflow.class);
    }
  }


  public BeanItemContainer<DatasetBean> getcontainer(String type, String id) {
    List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> datasets =
        new ArrayList<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet>();

    switch (type) {
      case "project":
        datasets = openbis.getClientDatasetsOfProjectByIdentifierWithSearchCriteria(id);
        break;

      case "experiment":
        // TODO
        break;

      case "sample":
        // TODO
        break;

      default:
        break;
    }
    return (BeanItemContainer<DatasetBean>) fillTable(datasets, id);
  }

  private void fetchExperimentalProperties(String id, List<String> fileNames) {
    Map<String, Object> params = new HashMap<String, Object>();
    List<String> codes = new ArrayList<String>();
    for (Sample s : openbis.getSamplesOfProject(id))
      codes.add(s.getCode());
    params.put("codes", codes);
    QueryTableModel res = openbis.getAggregationService("get-property-tsv", params);

    Set<String> factorNames = new HashSet<String>();
    StringBuilder tsv = new StringBuilder();
    String header = "file\tinternal_ID";

    // XML Parser
    Parser p = new Parser();

    for (Serializable[] ss : res.getRows()) {
      String xml = (String) ss[3];
      String code = (String) ss[0];
      String match = getMatchingStrings(fileNames, code);
      if (!xml.isEmpty() && !match.isEmpty()) {
        String extID = (String) ss[1];// how to use this if it is preferred over secondary name?
        String secondaryName = (String) ss[2];
        tsv.append(match + "\t" + secondaryName);
        List<Factor> factors = new ArrayList<Factor>();
        try {
          factors = p.getFactorsFromXML(xml);
        } catch (JAXBException e) {
          e.printStackTrace();
        }
        for (Factor f : factors) {
          factorNames.add(f.getLabel());
          String val = f.getValue();
          if (f.hasUnit())
            val += f.getUnit();
          tsv.append("\t" + val);
        }
        tsv.append("\n");
      }
    }
    for (String f : factorNames)
      header = header + "\t" + f;
    this.expPropTSV = header + "\n" + tsv;
    this.expFactors = factorNames;
    LOGGER.debug("experimental properties: " + expFactors);
    LOGGER.debug("pheno file:\n" + expPropTSV);
  }

  /**
   * Finds the the first matching string in the list
   * 
   * @param list The list of strings to check
   * @param regex The regular expression to use
   * @return first matching String
   */
  static String getMatchingStrings(List<String> list, String substring) {

    for (String s : list) {
      if (s.contains(substring)) {
        return s;
      }
    }
    return "";
  }

  public Submitter getSubmitter() {
    return submitter;
  }

  public String submitAndRegisterWf(String type, String id, Workflow workflow,
      List<DatasetBean> selectedDatasets) throws ConnectException, IllegalArgumentException,
      SubmitFailedException {

    SpaceAndProjectCodes spaceandproject = getSpaceAndProjects(type, id);

    String spaceCode = spaceandproject.space;
    String projectCode = spaceandproject.project;


    String experimentCode =
        registerWFExperiment(spaceCode, projectCode, workflow.getExperimentType(),
            workflow.getID(), workflow.getVersion(), user);


    List<String> parents = getConnectedSamples(selectedDatasets);
    String sampleType = workflow.getSampleType();

    String sampleCode =
        registerWFSample(spaceCode, projectCode, experimentCode, sampleType, parents);

    String openbisId =
        String.format("%s-%s-%s-%s", spaceCode, projectCode, experimentCode, sampleCode);

    LOGGER.info("User: " + user + " is submitting workflow " + workflow.getID() + " openbis id is:"
       + openbisId);
   String submit_id = submitter.submit(workflow, openbisId, user);
    LOGGER.info("Workflow has guse id: " + submit_id);

    setWorkflowID(spaceCode, projectCode, experimentCode, submit_id);
    return openbisId;
  }

  private SpaceAndProjectCodes getSpaceAndProjects(String type, String id) {
    String[] split = id.split("/");

    if (split.length == 0)
      return null;
    switch (type) {
      case PatientView.navigateToLabel:
      case ProjectView.navigateToLabel:
      case ExperimentView.navigateToLabel:
      case "workflowExperimentType":
        return new SpaceAndProjectCodes(split[1], split[2]);
      case SampleView.navigateToLabel:
        String expId =
            openbis.getSampleByIdentifier(String.format("%s/%s", split[1], split[2]))
                .getExperimentIdentifierOrNull();
        if (expId == null)
          return null;
        return getSpaceAndProjects(ExperimentView.navigateToLabel, expId);
      case DatasetView.navigateToLabel:
        throw new NotImplementedException("Dataset view is not ready for workflows!");
      default:
        LOGGER.debug(String.format("Problem with id %s, type %s", id, type));
        return null;
    }
  }

  class SpaceAndProjectCodes {
    public String space;
    public String project;

    public SpaceAndProjectCodes(String spaceCode, String projectCode) {
      this.space = spaceCode;
      this.project = projectCode;
    }

  }

  /**
   * get for a {@link de.uni_tuebingen.qbic.beans.DatasetBean} file or directory the path it has on
   * the data store server.
   * 
   * @param bean
   * @return full path of dataset
   * @throws IllegalArgumentException
   */
  public String getDatasetsNfsPath(DatasetBean bean) throws IllegalArgumentException {
    try {
      DataSet dataset = openbis.getFacade().getDataSet(bean.getOpenbisCode());
      String path = dataset.getDataSetDss().tryGetInternalPathInDataStore();
      if (bean.getFullPath().startsWith("original")) {
        path = Paths.get(path, bean.getFullPath()).toString();
      } else {
        FileInfoDssDTO[] filelist = dataset.listFiles("original", false);
        path = path + "/original/" + filelist[0].getPathInListing();
      }
      path = path.replaceFirst("/mnt/" + openbis_dss, "/mnt/nfs/qbic");
      return path;
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not retrieve nfs path for dataset " + bean);
    }
  }


  public OpenBisClient getOpenbis() {
    return this.openbis;
  }


  /**
   * Returns experimental factor names parsed from the properties of samples in this project
   * 
   * @return Unique set of all experimental factors that are saved in Q_Properties of this project
   */
  public Set<String> getExperimentalFactors() {
    LOGGER.debug("factors requested: "+expFactors);
    return expFactors;
  }

  public String getExperimentalFactorsTSV() {
    return expPropTSV;
  }

}
