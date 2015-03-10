package de.uni_tuebingen.qbic.qbicmainportlet;

import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.WordUtils;

import com.google.gwt.user.server.rpc.UnexpectedException;
import com.vaadin.server.StreamResource;

import ch.systemsx.cisd.common.api.client.ServiceFinder;
import ch.systemsx.cisd.common.exceptions.InvalidAuthenticationException;
import ch.systemsx.cisd.common.exceptions.InvalidSessionException;
import ch.systemsx.cisd.openbis.dss.client.api.v1.IOpenbisServiceFacade;
import ch.systemsx.cisd.openbis.dss.client.api.v1.OpenbisServiceFacadeFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.FileInfoDssDTO;
import ch.systemsx.cisd.openbis.generic.shared.IServiceForDataStoreServer;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.IGeneralInformationService;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Attachment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.ControlledVocabularyPropertyType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.ControlledVocabularyPropertyType.VocabularyTerm;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.DataSet;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.EntityType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.PropertyTypeGroup;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SampleType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria.MatchClause;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria.MatchClauseAttribute;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchSubCriteria;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SpaceWithProjectsAndRoleAssignments;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.id.project.ProjectIdentifierId;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.id.sample.SampleIdentifierId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ExperimentType;
import ch.systemsx.cisd.openbis.plugin.query.shared.api.v1.IQueryApiServer;



/**
 * dss client, a proxy to the generic openbis api This client is based on DSS Client for openBIS
 * written by Emanuel Schmid and the OpenbisConnector written by Bela Hullar
 * 
 * @author wojnar
 */
public class OpenBisClient {// implements Serializable {
  /**
   * 
   */
  // private static final long serialVersionUID = 3926210649301601498L;
  int timeout = 120; // 2 minutes
  int tolimit = 600;
  IOpenbisServiceFacade facade;
  private IGeneralInformationService openbisInfoService;
  IQueryApiServer openbisDssService;
  String sessionToken;
  String userId;
  String password;
  String serverURL;
  boolean verbose;


  public OpenBisClient(String loginid, String password, String serverURL, boolean verbose) {
    this.userId = loginid;
    this.password = password;
    this.serverURL = serverURL;
    this.verbose = verbose;
    this.facade = null;
    this.login();
  }

  public boolean loggedin() {
    if (this.facade == null)
      return false;
    try {
      this.facade.checkSession();
    } catch (InvalidSessionException e) {
      return false;
    }
    return true;
  }

  /**
   * logs out of the OpenBIS server
   */
  public void logout() {
    try {
      this.facade.logout();
    } catch (Exception e) {
      // Nothing todo here
    } finally {
      this.facade = null;
    }
  }

  /**
   * logs in to the OpenBIS server with the system userid after calling this function, the user has
   * to provide the password
   */
  public void login() {
    if (this.loggedin())
      this.logout();

    int trialno = 3;
    int notrial = 0;
    int timeoutStep = this.timeout;
    while (true) {
      try {
        facade =
            OpenbisServiceFacadeFactory.tryCreate(this.userId, this.password, this.serverURL,
                this.timeout * 1000);
        ServiceFinder serviceFinder =
            new ServiceFinder("openbis", IGeneralInformationService.SERVICE_URL);
        ServiceFinder serviceFinder2 =
            new ServiceFinder("openbis", IQueryApiServer.QUERY_PLUGIN_SERVER_URL);

        this.setOpenbisInfoService(serviceFinder.createService(IGeneralInformationService.class, this.serverURL));
        this.openbisDssService =
            serviceFinder2.createService(IQueryApiServer.class, this.serverURL);
        this.sessionToken =
            this.getOpenbisInfoService().tryToAuthenticateForAllServices(this.userId, this.password);
        break;
      } catch (Exception e) {
        if (e.getMessage().contains("Read timed out")) {
          if (this.timeout >= this.tolimit)
            throw new InvalidAuthenticationException("Login failed because of read timeout.");
          this.timeout += timeoutStep;

        } else {
          notrial++;
          if (notrial >= trialno)
            throw new InvalidAuthenticationException(String.format("Login failed after %d trials.",
                trialno));
          try {
            Thread.sleep(10);
          } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
          if (facade == null) {
        	  throw new UnexpectedException("OpenBis facade is not available. Check connection, password and user.", e);
          }
        }
      }
    }
  }

  /**
   * Get session token of current openBIS session
   * 
   * @return session token as string
   */
  public String getSessionToken() {
    if (!this.getOpenbisInfoService().isSessionActive(this.sessionToken)) {
      this.logout();
      this.login();
    }
    return this.sessionToken;
  }

  /**
   * Get a openBIS service facade when logged in to get functionality to retrieve data for example.
   * 
   * @return a IOpenbisServiceFacade which provides various functions
   */
  public IOpenbisServiceFacade getFacade() {
    ensureLoggedIn();
    return facade;
  }

  /**
   * Logs out before garbage is collected
   */
  protected void finalize() throws Throwable {
    //check whether it is really logged in. Otherwise, if executed from a portlet, which was
    //already closed one gets stupid java.lang.IllegalStateException, which pollute the log file.
  //  if(this.loggedin()){
   //   this.logout();
   //   this.facade = null;
   // }
    super.finalize();
  }

  /**
   * Checks if logged in, reconnects if not
   */
  public void ensureLoggedIn() {
    if (!this.loggedin()) {
      this.login();
    }
  }

  /**
   * Function to get all spaces which are registered in this openBIS instance
   * 
   * @return list with the identifiers of all available spaces
   */
  public List<String> listSpaces() {
    List<SpaceWithProjectsAndRoleAssignments> spaces_roles = facade.getSpacesWithProjects();
    List<String> spaces = new ArrayList<String>();
    for (SpaceWithProjectsAndRoleAssignments sp : spaces_roles) {
      spaces.add(sp.getCode());
    }
    return spaces;
  }

  /**
   * Function to get all projects which are registered in this openBIS instance
   * 
   * @return list with all projects which are registered in this openBIS instance
   */
  public List<Project> listProjects() {
    return this.getFacade().listProjects();
  }

  /**
   * Function to list all Experiments which are registered in the openBIS instance.
   * 
   * @return list with all experiments registered in this openBIS instance
   */
  public List<Experiment> listExperiments() {
    ensureLoggedIn();
    List<String> spaces = this.listSpaces();
    List<Experiment> foundExps = new ArrayList<Experiment>();

    for (String s : spaces) {
      SearchCriteria sc = new SearchCriteria();
      sc.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.SPACE, s));
      foundExps.addAll(this.getOpenbisInfoService().searchForExperiments(this.sessionToken, sc));
    }
    return foundExps;
  }

  /**
   * Function to retrieve all samples of a given experiment
   * 
   * @param experimentIdentifier identifier/code (both should work) of the openBIS experiment
   * @return list with all samples of the given experiment
   */
  public List<Sample> getSamplesofExperiment(String experimentIdentifier) {
    ensureLoggedIn();
    SearchCriteria sc = new SearchCriteria();
    SearchCriteria ec = new SearchCriteria();
    ec.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.CODE,
        experimentIdentifier));
    sc.addSubCriteria(SearchSubCriteria.createExperimentCriteria(ec));
    List<Sample> foundSamples = this.getOpenbisInfoService().searchForSamples(sessionToken, sc);
    return foundSamples;
  }

  /**
   * Function to retrieve all samples of a given space
   * 
   * @param spaceIdentifier identifier of the openBIS space
   * @return list with all samples of the given space
   */
  public List<Sample> getSamplesofSpace(String spaceIdentifier) {
    ensureLoggedIn();
    SearchCriteria sc = new SearchCriteria();
    sc.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.SPACE, spaceIdentifier));
    List<Sample> foundSamples = this.getOpenbisInfoService().searchForSamples(sessionToken, sc);
    return foundSamples;
  }

  /**
   * Function to retrieve a sample by it's identifier or code
   * 
   * @param sampleIdentifier identifier or code of the sample
   * @return the sample with the given identifier
   */
  public Sample getSampleByIdentifier(String sampleIdentifier) {
    ensureLoggedIn();
    SearchCriteria sc = new SearchCriteria();
    sc.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.CODE, sampleIdentifier));
    List<Sample> foundSamples = this.getOpenbisInfoService().searchForSamples(sessionToken, sc);
    return foundSamples.get(0);
  }

  /**
   * Function to get all samples of a specific project
   * 
   * @param projectIdentifier identifier or code of the openBIS project
   * @return list with all samples connected to the given project
   */
  public List<Sample> getSamplesOfProject(String projectIdentifier) {
    ensureLoggedIn();
    List<String> projects = new ArrayList<String>();
    List<Project> foundProjects = facade.listProjects();
    List<Sample> foundSamples = new ArrayList<Sample>();

    for (Project proj : foundProjects) {
      if (projectIdentifier.equals(proj.getCode())) {
        projects.add(proj.getIdentifier());
      }
    }

    if (projects.size() > 0) {
      List<Experiment> foundExp = facade.listExperimentsForProjects(projects);
      for (Experiment exp : foundExp) {
        SearchCriteria sc = new SearchCriteria();
        SearchCriteria ec = new SearchCriteria();
        ec.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.CODE,
            exp.getIdentifier()));
        sc.addSubCriteria(SearchSubCriteria.createExperimentCriteria(ec));
        foundSamples.addAll(this.getOpenbisInfoService().searchForSamples(sessionToken, sc));
      }
    }

    return foundSamples;
  }

  /**
   * returns a list of all Experiments connected to the project with the identifier from openBis
   * 
   * @param projectIdentifier identifier of the given openBIS project
   * @return list of all experiments of the given project
   */
  public List<Experiment> getExperimentsOfProjectByIdentifier(String projectIdentifier) {
    List<String> projects = new ArrayList<String>();
    projects.add(projectIdentifier);
    List<Experiment> foundExps = this.getFacade().listExperimentsForProjects(projects);
    return foundExps;
  }

  /**
   * Function to list all Experiments for a specific project which are registered in the openBIS
   * instance.
   * 
   * @param project the project for which the experiments should be listed
   * @return list with all experiments registered in this openBIS instance
   */
  public List<Experiment> getExperimentsForProject(Project project) {
    return this.getExperimentsOfProjectByIdentifier(project.getIdentifier());
  }

  /**
   * returns a list of all Experiments connected to a Project code in openBIS
   * 
   * @param projectCode code of the given openBIS project
   * @return list of all experiments of the given project
   */
  public List<Experiment> getExperimentsOfProjectByCode(String projectCode) {
    String projID = "";
    List<Project> projects = this.getFacade().listProjects();
    for (Project p : projects) {
      if (p.getCode().equals(projectCode))
        projID = "/" + p.getSpaceCode() + "/" + p.getCode();
    }
    List<Experiment> foundExps = getExperimentsOfProjectByIdentifier(projID);
    return foundExps;
  }

  /**
   * Function to get all experiments for a given space and the information to which project the
   * corresponding experiment belongs to
   * 
   * @param spaceIdentifier identifier of a openBIS space
   * @return map containing all projects (keys) and lists with all connected experiments (values)
   */
  public Map<String, List<Experiment>> getProjectExperimentMapping(String spaceIdentifier) {
    Map<String, List<Experiment>> mapping = new HashMap<String, List<Experiment>>();

    for (Experiment e : this.getExperimentsOfSpace(spaceIdentifier)) {
      String[] splitted = e.getIdentifier().split("/");
      String key = "/" + splitted[1] + "/" + splitted[2];
      List<Experiment> value = mapping.get(key);

      if (value != null) {
        mapping.get(key).add(e);
      } else {
        List<Experiment> exps = new ArrayList<Experiment>();
        exps.add(e);
        mapping.put(key, exps);
      }
    }

    return mapping;
  }

  /**
   * Function to retrieve all experiments of a given space
   * 
   * @param spaceIdentifier identifier of the openBIS space
   * @return list with all experiments connected to this space
   */
  public List<Experiment> getExperimentsOfSpace(String spaceIdentifier) {
    ensureLoggedIn();
    if (spaceIdentifier.isEmpty()) {
      List<Experiment> foundExps = this.listExperiments();
      return foundExps;
    } else {
      SearchCriteria sc = new SearchCriteria();
      sc.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.SPACE,
          spaceIdentifier));
      List<Experiment> foundExps =
          this.getOpenbisInfoService().searchForExperiments(this.sessionToken, sc);
      return foundExps;
    }
  }

  /**
   * Function to retrieve all experiments of a specific given type
   * 
   * @param type identifier of the openBIS experiment type
   * @return list with all experiments of this given type
   */
  public List<Experiment> getExperimentsOfType(String type) {
    ensureLoggedIn();
    SearchCriteria sc = new SearchCriteria();
    sc.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.TYPE, type));
    List<Experiment> foundExps =
        this.getOpenbisInfoService().searchForExperiments(this.sessionToken, sc);
    return foundExps;
  }

  /**
   * Function to retrieve all samples of a specific given type
   * 
   * @param type identifier of the openBIS sample type
   * @return list with all samples of this given type
   */
  public List<Sample> getSamplesOfType(String type) {
    ensureLoggedIn();
    SearchCriteria sc = new SearchCriteria();
    sc.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.TYPE, type));
    List<Sample> foundSamples = this.getOpenbisInfoService().searchForSamples(this.sessionToken, sc);
    return foundSamples;
  }

  /**
   * Function to retrieve all projects of a given space from openBIS.
   * 
   * @param space identifier of the openBIS space
   * @return a list with all projects objects for the given space
   */
  public List<Project> getProjectsOfSpace(String space) {
    List<Project> projects = new ArrayList<Project>();
    List<Project> foundProjects = facade.listProjects();

    for (Project proj : foundProjects) {
      if (space.equals(proj.getSpaceCode())) {
        projects.add(proj);
      }
    }
    return projects;
  }

  /**
   * Function to retrieve a project from openBIS by the identifier of the project.
   * 
   * @param projectIdentifier identifier of the openBIS project
   * @return project with the given id
   */
  public Project getProjectByIdentifier(String projectIdentifier) {
    List<Project> projects = this.listProjects();
    Project project = null;
    for (Project p : projects) {
      if (p.getIdentifier().equals(projectIdentifier)) {
        project = p;
      }
    }
    return project;
  }

  /**
   * Function to retrieve a project from openBIS by the code of the project.
   * 
   * @param projectCode code of the openBIS project
   * @return project with the given code
   */
  public Project getProjectByCode(String projectCode) {
    List<Project> projects = this.listProjects();
    Project project = null;
    for (Project p : projects) {
      if (p.getCode().equals(projectCode)) {
        project = p;
        break;
      }
    }
    return project;
  }

  /**
   * Function to retrieve a experiment from openBIS by the code of the experiment.
   * 
   * @param experimentCode code of the openBIS experiment
   * @return experiment with the given code
   */
  public Experiment getExperimentByCode(String experimentCode) {
    List<Experiment> experiments = this.listExperiments();
    Experiment experiment = null;
    for (Experiment e : experiments) {
      if (e.getCode().equals(experimentCode)) {
        experiment = e;
        break;
      }
    }
    return experiment;
  }
  
  /**
   * Function to retrieve a experiment from openBIS by the code of the experiment.
   * 
   * @param experimentCode id of the openBIS experiment
   * @return experiment with the given code
   */
  public Experiment getExperimentById(String experimentId) {
    List<Experiment> experiments = this.listExperiments();
    Experiment experiment = null;
    for (Experiment e : experiments) {
      if (e.getIdentifier().equals(experimentId)) {
        experiment = e;
        break;
      }
    }
    return experiment;
  }


  /**
   * Function to retrieve the project of an experiment from openBIS
   * 
   * @param experimentIdentifier identifier of the openBIS experiment
   * @return project connected to the given experiment
   */
  public Project getProjectOfExperimentByIdentifier(String experimentIdentifier) {
    List<Project> projects = this.facade.listProjects();
    String project = experimentIdentifier.split(("/"))[2];
    Project found_proj = null;

    for (Project proj : projects) {
      if (project.equals(proj.getIdentifier().split("/")[2])) {
        found_proj = proj;
      }
    }
    return found_proj;
  }

  /**
   * Function to list all datasets of a specific sample (watch out there are different dataset
   * classes)
   * 
   * @param sampleIdentifier identifier of the openBIS sample
   * @return list with all datasets of the given sample
   */
  public List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> getDataSetsOfSampleByIdentifier(
      String sampleIdentifier) {
    List<String> identifier = new ArrayList<String>();
    identifier.add(sampleIdentifier);
    return this.facade.listDataSetsForSamples(identifier);
  }

  /**
   * Function to list all datasets of a specific sample (watch out there are different dataset
   * classes)
   * 
   * @param sampleCode code or identifier of the openBIS sample
   * @return list with all datasets of the given sample
   */
  public List<ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.DataSet> getDataSetsOfSample(
      String sampleCode) {
    ensureLoggedIn();
    SearchCriteria ec = new SearchCriteria();
    ec.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.CODE, sampleCode));
    SearchCriteria sc = new SearchCriteria();
    sc.addSubCriteria(SearchSubCriteria.createSampleCriteria(ec));
    return getOpenbisInfoService().searchForDataSetsOnBehalfOfUser(sessionToken, sc, userId);
  }

  /**
   * Function to list all datasets of a specific experiment (watch out there are different dataset
   * classes)
   * 
   * @param experimentPermID permId of the openBIS experiment
   * @return list with all datasets of the given experiment
   */
  public List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> getDataSetsOfExperiment(
      String experimentPermID) {
    return this.getFacade().listDataSetsForExperiment(experimentPermID);
  }

  /**
   * Returns all datasets of a given experiment. The new version should run smoother NOT
   * 
   * @param experimentIdentifier identifier or code of the openbis experiment
   * @return list of all datasets of the given experiment
   */
  public List<ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.DataSet> getDataSetsOfExperimentByIdentifier(
      String experimentIdentifier) {
    ensureLoggedIn();
    SearchCriteria ec = new SearchCriteria();
    ec.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.CODE,
        experimentIdentifier));
    SearchCriteria sc = new SearchCriteria();
    sc.addSubCriteria(SearchSubCriteria.createExperimentCriteria(ec));
    return getOpenbisInfoService().searchForDataSetsOnBehalfOfUser(sessionToken, sc, userId);
  }

  /**
   * Function to list all datasets of a specific openBIS space
   * 
   * @param spaceIdentifier identifier of the openBIS space
   * @return list with all datasets of the given space
   */
  public List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> getDataSetsOfSpaceByIdentifier(
      String spaceIdentifier) {
    List<Sample> samples = getSamplesofSpace(spaceIdentifier);
    ArrayList<String> ids = new ArrayList<String>();
    for (Iterator<Sample> iterator = samples.iterator(); iterator.hasNext();) {
      Sample s = (Sample) iterator.next();
      ids.add(s.getIdentifier());
    }
    if (ids.isEmpty()) {
      return new ArrayList<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet>();
    }
    return this.getFacade().listDataSetsForSamples(ids);
  }

  /**
   * Function to list all datasets of a specific openBIS project
   * 
   * @param projectIdentifier identifier of the openBIS project
   * @return list with all datasets of the given project
   */
  public List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> getDataSetsOfProjectByIdentifier(
      String projectIdentifier) {
    List<Sample> samps = getSamplesOfProject(projectIdentifier);
    List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> res =
        new ArrayList<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet>();
    
    for (Iterator<Sample> iterator = samps.iterator(); iterator.hasNext();) {
      Sample sample = (Sample) iterator.next();
      res.addAll(getDataSetsOfSampleByIdentifier(sample.getIdentifier()));
    }
    return res;
  }

  /**
   * Function to list all datasets of a specific openBIS project
   * 
   * @param projectCode code of the openBIS project
   * @return list with all datasets of the given project
   */
  public List<ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.DataSet> getDataSetsOfProjectByCode(
      String projectCode) {
    List<Sample> samps = getSamplesOfProject(projectCode);
    List<DataSet> res = new ArrayList<DataSet>();
    for (Iterator<Sample> iterator = samps.iterator(); iterator.hasNext();) {
      Sample sample = (Sample) iterator.next();
      res.addAll(getDataSetsOfSample(sample.getCode()));
    }
    return res;
  }

  /**
   * Function to list all datasets of a specific type
   * 
   * @param type identifier of the openBIS type
   * @return list with all datasets of the given type
   */
  public List<DataSet> getDataSetsByType(String type) {
    ensureLoggedIn();
    SearchCriteria sc = new SearchCriteria();
    sc.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.TYPE, type));
    return getOpenbisInfoService().searchForDataSetsOnBehalfOfUser(sessionToken, sc, userId);
  }

  /**
   * Function to list all attachments of a sample
   * 
   * @param sampleIdentifier identifier of the openBIS sample
   * @return list with all attachments connected to the given sample
   */
  public List<Attachment> listAttachmentsForSampleByIdentifier(String sampleIdentifier) {
    ensureLoggedIn();
    return getOpenbisInfoService().listAttachmentsForSample(this.sessionToken, new SampleIdentifierId(
        sampleIdentifier), true);
  }

  /**
   * Function to list all attachments of a project
   * 
   * @param projectIdentifier identifier of the openBIS project
   * @return list with all attachments connected to the given project
   */
  public List<Attachment> listAttachmentsForProjectByIdentifier(String projectIdentifier) {
    ensureLoggedIn();
    return this.getOpenbisInfoService().listAttachmentsForProject(this.sessionToken,
        new ProjectIdentifierId(projectIdentifier), true);
  }

  // TODO specify which parameters have to be there
  /**
   * Function to add an attachment to a existing project in openBIS by calling the corresponding
   * ingestion service of openBIS.
   * 
   * @param parameter map with needed information for registration process by ingestion service
   */
  public void addAttachmentToProject(Map<String, Object> parameter) {
    ensureLoggedIn();
    System.out.println(this.openbisDssService.createReportFromAggregationService(this.sessionToken,
        "DSS1", "add-attachment", parameter));
  }

  /**
   * Returns all users of a Space.
   * 
   * @param spaceCode code of the openBIS space
   * @return set of user names as string
   */
  public Set<String> getSpaceMembers(String spaceCode) {
    List<SpaceWithProjectsAndRoleAssignments> spaces = this.getFacade().getSpacesWithProjects();
    for (SpaceWithProjectsAndRoleAssignments space : spaces) {
      if (space.getCode().equals(spaceCode)) {
        return space.getUsers();
      }
    }
    return null;
  }

  /**
   * Function to retrieve all properties which have been assigned to a specific entity type
   * 
   * @param entity_type entitiy type
   * @return list of properties which are assigned to the entity type
   */
  public List<PropertyType> listPropertiesForType(EntityType entity_type) {
    List<PropertyType> property_types = new ArrayList<PropertyType>();
    List<PropertyTypeGroup> props = entity_type.getPropertyTypeGroups();
    for (PropertyTypeGroup pg : props) {
      for (PropertyType prop_type : pg.getPropertyTypes()) {
        property_types.add(prop_type);
      }
    }
    return property_types;
  }

  /**
   * Function to list the vocabulary terms for a given property which has been added to openBIS. The
   * property has to be a Controlled Vocabulary Property.
   * 
   * @param property the property type
   * @return list of the vocabulary terms of the given property
   */
  public List<String> listVocabularyTermsForProperty(PropertyType property) {
    List<String> terms = new ArrayList<String>();
    ControlledVocabularyPropertyType controlled_vocab = (ControlledVocabularyPropertyType) property;
    for (VocabularyTerm term : controlled_vocab.getTerms()) {
      terms.add(term.getLabel().toString());
    }
    return terms;
  }
  
  /**
   * Function to get the label of a CV item for some property
   * 
   * @param propertyType the property type
   * @param propertyValue the property value
   * @return Label of CV item
   */
  public String getCVLabelForProperty(PropertyType propertyType, String propertyValue) {
    ControlledVocabularyPropertyType controlled_vocab = (ControlledVocabularyPropertyType) propertyType;
    
    for (VocabularyTerm term : controlled_vocab.getTerms()) {
      if(term.getCode().equals(propertyValue)) {
        return term.getLabel();
      }
    }
    return "";
    }
  

  /**
   * Function to get a SampleType object of a sample type
   * 
   * @param sampleType the sample type as string
   * @return the SampleType object of the corresponding sample type
   */
  public SampleType getSampleTypeByString(String sampleType) {
    List<SampleType> types = this.getFacade().listSampleTypes();
    SampleType st = null;
    for (SampleType t : types) {
      if (t.getCode().equals(sampleType)) {
        st = t;
      }
    }
    return st;
  }
  
  /**
   * Function to get a ExperimentType object of a experiment type
   * 
   * @param experimentType the experiment type as string
   * @return the ExperimentType object of the corresponding experiment type
   */
  public ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.ExperimentType getExperimentTypeByString(String experimentType) {
    List<ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.ExperimentType> types = this.getFacade().listExperimentTypes();
    ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.ExperimentType st = null;
    for (ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.ExperimentType t : types) {
      if (t.getCode().equals(experimentType)) {
        st = t;
      }
    }
    return st;
  }

  /**
   * Function to get the labels of all property types of a specific instance type
   * 
   * @param entityType the instance type
   * @return map with types as keys and labels as values
   */
  public Map<String, String> getLabelsofProperties(EntityType entityType) {
    List<PropertyType> prop_types = this.listPropertiesForType(entityType);
    Map<String, String> prop_types_labels = new HashMap<String, String>();

    for (PropertyType pt : prop_types) {
      prop_types_labels.put(pt.getCode(), pt.getLabel());
    }
    return prop_types_labels;
  }
  
  /**
   * Function to trigger ingestion services registered in openBIS
   * 
   * @param serviceName name of the ingestion service which should be triggered
   * @param parameters map with needed information for registration process
   * @return object name of the QueryTableModel which is returned by the aggregation service
   */
  public String triggerIngestionService(String serviceName, Map<String, Object> parameters) {
    return this.openbisDssService.createReportFromAggregationService(this.sessionToken, "DSS1",
        serviceName, parameters).toString();
  }

  // TODO specify parameters needed for ingestion service
  /**
   * Function to add children samples to a sample (parent) using the corresponding ingestition
   * service
   * 
   * @param parameters map with needed information for registration process
   * @return object name of the QueryTableModel which is returned by the aggregation service
   */
  public String addParentChildConnection(Map<String, Object> parameters) {
    return this.openbisDssService.createReportFromAggregationService(this.sessionToken, "DSS1",
        "create-parent-child", parameters).toString();
  }

  /**
   * Function to trigger the registration of new openBIS instances like projects, experiments and
   * samples. This function also triggers the barcode generation for samples.
   * 
   * @param params map with needed information for registration process
   * @param name name of the service for the corresponding registration
   * @param number_of_samples_offset offset to generate correct barcodes (depending on number of
   *        samples) by accounting for delay of registration process
   * @return object name of the QueryTableModel which is returned by the aggregation service
   */
  public String addNewInstance(Map<String, Object> params, String service,
      int number_of_samples_offset) {
    @SuppressWarnings("unchecked")
    Map<String, String> properties = (Map<String, String>) params.get("properties");
    if ((params.get("properties") != null) && properties.get("QBIC_BARCODE") != null) {
      String barcode =
          generateBarcode("/" + params.get("space").toString() + "/"
              + params.get("project").toString(), number_of_samples_offset);
      properties.put("QBIC_BARCODE", barcode);
      params.put("code", barcode);
    }
    // System.out.println(params);
    return this.openbisDssService.createReportFromAggregationService(this.sessionToken, "DSS1",
        service, params).toString();
  }

  /**
   * Function to create a QBiC barcode string for a sample based on the project ID. QBiC barcode
   * format: Q + project_ID + sample number + X + checksum
   * 
   * @param proj ID of the project
   * @return the QBiC barcode as string
   */
  // TODO check if it works for all cases
  // TODO check for null ?
  public String generateBarcode(String proj, int number_of_samples_offset) {
    Project project = this.getProjectByIdentifier(proj);
    // Project project = getProjectofExperiment(exp);
    int number_of_samples = getSamplesOfProject(project.getCode()).size();
    // System.out.println(number_of_samples);

    String barcode = project.getCode() + String.format("%03d", (number_of_samples + 1)) + "S";
    // String barcode = project.getCode() + String.format("%03d", Math.max(1, number_of_samples +
    // number_of_samples_offset)) + "S";
    barcode += checksum(barcode);
    return barcode;
  }

  /**
   * Function map an integer value to a char
   * 
   * @param i the integer value which should be mapped
   * @return the resulting char value
   */
  public char mapToChar(int i) {
    i += 48;
    if (i > 57) {
      i += 7;
    }
    return (char) i;
  }

  /**
   * Function to generate the checksum for the given barcode string
   * 
   * @param s the barcode string
   * @return the checksum for the given barcode
   */
  public char checksum(String s) {
    int i = 1;
    int sum = 0;
    for (int idx = 0; idx <= s.length() - 1; idx++) {
      sum += (((int) s.charAt(idx))) * i;
      i += 1;
    }
    return mapToChar(sum % 34);
  }

  /**
   * Function to transform openBIS entity type to human readable text
   * 
   * @param entityCode the entity code as string
   * @return entity code as string in human readable text
   */
  public String openBIScodeToString(String entityCode) {
    if(entityCode == null) {
      return "";
    }
    else {
    entityCode = WordUtils.capitalizeFully(entityCode.replace("_", " ").toLowerCase());
    String edit_string =
        entityCode.replace("Ngs", "NGS").replace("Hla", "HLA").replace("Rna", "RNA")
            .replace("Dna", "DNA").replace("Ms", "MS");
    if (edit_string.startsWith("Q ")) {
      edit_string = edit_string.replace("Q ", "");
    }
    return edit_string;
    }
  }

  /**
   * Function to get the download url for a file stored in the openBIS datastore server
   * 
   * @throws MalformedURLException Returns an download url for the openbis dataset with the given
   *         code and dataset_type. Throughs MalformedURLException if a url can not be created from
   *         the given parameters. NOTE: datastoreURL differs from serverURL only by the port ->
   *         quick hack used
   * @param dataSetCode code of the openBIS dataset
   * @param openbisFilename name of the file stored in the given dataset
   * @return URL object of the download url for the given file
   */
  public URL getDataStoreDownloadURL(String dataSetCode, String openbisFilename)
      throws MalformedURLException {
    String downloadURL = this.serverURL.substring(0, this.serverURL.length() - 1);
    downloadURL += "4";
    downloadURL += "/datastore_server/";

    downloadURL += dataSetCode;
    downloadURL += "/original/";
    downloadURL += openbisFilename;
    downloadURL += "?mode=simpleHtml&sessionID=";
    downloadURL += this.getSessionToken();
    return new URL(downloadURL);
  }

  /**
   * Returns a Map that maps samples to a list of samples of their parent samples
   * 
   * @param samples A list of openBIS samples
   * @return Map<Sample, List<Sample>> containing a mapping between children and parents of samples
   */
  public Map<Sample, List<Sample>> getParentMap(List<Sample> samples) {
    Map<Sample, List<Sample>> results = new HashMap<Sample, List<Sample>>();
    for (Sample p : samples) {
      String permID = p.getPermId();
      List<Sample> children = getFacade().listSamplesOfSample(permID);
      for (Sample c : children) {
        if (results.containsKey(c))
          results.get(c).add(p);
        else
          results.put(c, new ArrayList<Sample>(Arrays.asList(p)));
      }
    }
    return results;
  }

  /**
   * Checks if a space object of a certain code exists in openBIS
   * @param spaceCode the code of an openBIS space
   * @return true, if the space exists, false otherwise
   */
  public boolean spaceExists(String name) {
    for (SpaceWithProjectsAndRoleAssignments s : this.facade.getSpacesWithProjects()) {
      if (s.getCode().equals(name))
        return true;
    }
    return false;
  }

  /**
   * Checks if a project object of a certain code exists under a given space in openBIS
   * @param spaceCode the code of an openBIS space
   * @param projectCode the code of a project
   * @return true, if the project exists under this space, false otherwise
   */
  public boolean projectExists(String spaceCode, String projectCode) {
    for (Project p : getProjectsOfSpace(spaceCode)) {
      if (p.getCode().equals(projectCode))
        return true;
    }
    return false;
  }

  /**
   * Checks if an experiment of a certain code exists under a given space and project in openBIS
   * @param spaceCode the code of an openBIS space
   * @param projectCode the code of a project in openBIS
   * @param experimentCode the code of an experiment
   * @return true, if the experiment exists under this project and space, false otherwise
   */
  public boolean expExists(String spaceCode, String projectCode, String experimentCode) {
    if (projectExists(spaceCode, projectCode)) {
      for (Experiment e : getExperimentsOfProjectByCode(projectCode)) {
        if (e.getCode().equals(experimentCode))
          return true;
      }
    }
    return false;
  }

  /**
   * Checks if a sample of a given code exists in openBIS
   * @param sampleCode the code of a sample
   * @return true, if the sample exists, false otherwise
   */
  public boolean sampleExists(String name) {
    SearchCriteria sc = new SearchCriteria();
    sc.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.CODE, name));
    for (Sample x : this.getOpenbisInfoService().searchForSamples(sessionToken, sc)) {
      if (x.getCode().equals(name))
        return true;
    }
    return false;
  }
  
  /**
   * Compute status of project by checking status of the contained experiments
   * 
   * @param project the Project object
   * @return ratio of finished experiments in this project
   */
  public float computeProjectStatus(Project project) {
    float finishedExperiments = 0f;

    List<Experiment> experiments = this.getExperimentsOfProjectByCode(project.getCode());
    float numberExperiments = experiments.size();

    for (Experiment e : experiments) {
      if (e.getProperties().keySet().contains("Q_CURRENT_STATUS")) {
        if (e.getProperties().get("Q_CURRENT_STATUS").equals("FINISHED")) {
          finishedExperiments += 1.0;
        };
      }
    }
    if (numberExperiments > 0) {
      return finishedExperiments / experiments.size();
    } else {
      return 0f;
    }
  }
  
  /**
   * Function to retrieve all samples of a given experiment
   * 
   * @param experimentIdentifier identifier/code (both should work) of the openBIS experiment
   * @return list with all samples of the given experiment
   */
  public List<Sample> getParents(String sampleCode) {
    ensureLoggedIn();
    SearchCriteria sc = new SearchCriteria();
    sc.addMatchClause(SearchCriteria.MatchClause.createAttributeMatch(SearchCriteria.MatchClauseAttribute.CODE, sampleCode));
    List<Sample> foundSample = this.facade.searchForSamples(sc);

    SearchCriteria sampleSc = new SearchCriteria();
    sampleSc.addSubCriteria(SearchSubCriteria.createSampleChildCriteria(sc));
    List<Sample> foundParentSamples = this.facade.searchForSamples(sampleSc);

    return foundParentSamples;
    }

  public IGeneralInformationService getOpenbisInfoService() {
    return openbisInfoService;
  }

  public void setOpenbisInfoService(IGeneralInformationService openbisInfoService) {
    this.openbisInfoService = openbisInfoService;
  }
  
  
  public List<Experiment> listExperimentsOfProjects(List<Project> tmp_list) {
    return this.getOpenbisInfoService().listExperiments(
        this.getSessionToken(), tmp_list, null);

  }

  //public static String openBIScodeToString(String experimentTypeCode) {
  //  return openBisClient.openBIScodeToString(experimentTypeCode);
  //}
  
  public List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> listDataSetsForExperiments(List<String> experimentIdentifiers) {
    return this.getFacade().listDataSetsForExperiments(experimentIdentifiers);
  }

  public List<Sample> listSamplesForProjects(List<String> projectIdentifiers) {
    return this.getFacade().listSamplesForProjects(projectIdentifiers);
  }

  //public List<Sample> getSamplesofSpace(String spaceName) {
  //  return this.getSamplesofSpace(spaceName);
  //}

  public List<ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet> listDataSetsForSamples(List<String> sampleIdentifier) {
    return this.getFacade().listDataSetsForSamples(sampleIdentifier);
  }
  
  //public void callIngestionService(String serviceName, Map<String, Object> parameters) {
  //  System.out.println(serviceName);
   // System.out.println(parameters);
   // openBisClient.triggerIngestionService("notify-user", parameters);
  //}
  
  public URL getUrlForDataset(String datasetCode, String datasetName) throws MalformedURLException {

    return this.getDataStoreDownloadURL(datasetCode, datasetName);
  }

  public InputStream getDatasetStream(String datasetCode) {

    IOpenbisServiceFacade facade = this.getFacade();
    ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet dataSet = facade.getDataSet(datasetCode);
    FileInfoDssDTO[] filelist = dataSet.listFiles("original", false);
    return dataSet.getFile(filelist[0].getPathInDataSet());
  }

  public InputStream getDatasetStream(String datasetCode, String folder) {

    IOpenbisServiceFacade facade = this.getFacade();
    ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet dataSet = facade.getDataSet(datasetCode);
    FileInfoDssDTO[] filelist = dataSet.listFiles("original/" + folder, false);
    return dataSet.getFile(filelist[0].getPathInDataSet());
  }
  
}
