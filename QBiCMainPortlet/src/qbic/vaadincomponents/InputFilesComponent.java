package qbic.vaadincomponents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import logging.Log4j2Logger;
import main.OpenBisClient;
import submitter.Workflow;
import submitter.parameters.FileListParameter;
import submitter.parameters.FileParameter;
import submitter.parameters.InputList;
import submitter.parameters.Parameter;
import submitter.parameters.ParameterSet;
import ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Grid.SingleSelectionModel;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import controllers.WorkflowViewController;
import de.uni_tuebingen.qbic.beans.DatasetBean;
import fasta.FastaBean;
import fasta.FastaDB;

public class InputFilesComponent extends WorkflowParameterComponent {


  /**
   * 
   */
  private static final long serialVersionUID = -675703070595329585L;
  private TabSheet inputFileForm = new TabSheet();
  private FieldGroup inputFileFieldGroup;
  
  private logging.Logger LOGGER = new Log4j2Logger(InputFilesComponent.class);
  private Set<Entry<String, Parameter>> wfparameters;
  private HashMap<String, Parameter> wfmap = new HashMap<String, Parameter>();;
  
  
  public InputFilesComponent(Set<Entry<String, Parameter>> wfparameters) {
    this.wfparameters = wfparameters;
    setCompositionRoot(inputFileForm);
  }

  public InputFilesComponent() {
    setCompositionRoot(inputFileForm);
  }

  @Override
  public Workflow getWorkflow() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ParameterSet getParameters() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void resetParameters() {
    // TODO Auto-generated method stub
  }


  public void buildLayout(Set<Entry<String, Parameter>> wfparameters, BeanItemContainer<DatasetBean> datasets) {
    this.wfparameters = wfparameters;
    this.setCaption(String.format("<font color=#FF0000>  Select input file(s) </font>"));
    this.setCaptionAsHtml(true);
    buildForm(wfparameters, datasets);
  }

  public void buildForm(Set<Entry<String, Parameter>> wfparameters, BeanItemContainer<DatasetBean> datasets) {

    inputFileForm.setHeight(100.0f, Unit.PERCENTAGE);
    inputFileForm.addStyleName(ValoTheme.TABSHEET_FRAMED);
    inputFileForm.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);

    inputFileForm.removeAllComponents();
    inputFileForm.setSizeFull();
    inputFileFieldGroup = new FieldGroup();

    wfmap.clear();
    for (Map.Entry<String, Parameter> entry : wfparameters) {
      wfmap.put(entry.getKey(), entry.getValue());
      GeneratedPropertyContainer gpcontainer = null;
      Grid newGrid = new Grid(gpcontainer);

      if (entry.getValue() instanceof FileParameter) {
        FileParameter fileParam = (FileParameter) entry.getValue();
        List<String> associatedDataTypes = fileParam.getRange();
        // String associatedDataType = fileParam.getTitle();

        if (associatedDataTypes.contains("fasta") || associatedDataTypes.contains("gtf")) {
          // if (associatedDataType.toLowerCase().equals("fasta")) {
          BeanItemContainer<FastaBean> subContainer =
              new BeanItemContainer<FastaBean>(FastaBean.class);
          FastaDB db = new FastaDB();
          subContainer.addAll(db.getAll());
          gpcontainer = new GeneratedPropertyContainer(subContainer);
          gpcontainer.removeContainerProperty("path");
        }

        else {
          BeanItemContainer<DatasetBean> subContainer =
              new BeanItemContainer<DatasetBean>(DatasetBean.class);

          for (java.util.Iterator<DatasetBean> i = datasets.getItemIds().iterator(); i.hasNext();) {
            DatasetBean dataset = i.next();

            if (associatedDataTypes.contains(dataset.getFileType().toLowerCase())) {
              // if (associatedDataType.toLowerCase().equals(dataset.getFileType().toLowerCase())) {
              subContainer.addBean(dataset);
            }
          }

          gpcontainer = new GeneratedPropertyContainer(subContainer);
          gpcontainer.removeContainerProperty("fullPath");
          gpcontainer.removeContainerProperty("openbisCode");

        }
        newGrid.setContainerDataSource(gpcontainer);
        newGrid.setSelectionMode(SelectionMode.SINGLE);
      }

      else if (entry.getValue() instanceof FileListParameter) {
        FileListParameter fileParam = (FileListParameter) entry.getValue();
        List<String> associatedDataTypes = fileParam.getRange();

        BeanItemContainer<DatasetBean> subContainer =
            new BeanItemContainer<DatasetBean>(DatasetBean.class);

        for (java.util.Iterator<DatasetBean> i = datasets.getItemIds().iterator(); i.hasNext();) {
          DatasetBean dataset = i.next();

          if (associatedDataTypes.contains(dataset.getFileType().toLowerCase())) {
            subContainer.addBean(dataset);
          }
        }

        gpcontainer = new GeneratedPropertyContainer(subContainer);
        gpcontainer.removeContainerProperty("fullPath");
        gpcontainer.removeContainerProperty("openbisCode");

        newGrid.setContainerDataSource(gpcontainer);
        newGrid.setSelectionMode(SelectionMode.MULTI);
      }

      else {
        Notification.show(String.format("Invalid Inputfile Parameter!", entry.getKey()),
            Type.ERROR_MESSAGE);
      }

      HorizontalLayout layout = new HorizontalLayout();
      layout.setMargin(new MarginInfo(true, true, true, true));
      layout.setSizeFull();

      newGrid.setWidth("100%");
      layout.addComponent(newGrid);

      if (newGrid.getContainerDataSource().size() == 0) {
        Notification.show(
            String.format("No dataset of type %s available in this project!", entry.getKey()),
            Type.WARNING_MESSAGE);
        layout.addComponent(newGrid);
      }

      inputFileForm.addTab(layout, entry.getKey());
    }
  }

  // TODO
  public void resetInputList() {
    Collection<Field<?>> registeredFields = inputFileFieldGroup.getFields();

    for (Field<?> field : registeredFields) {
      TextField fieldToReset = (TextField) field;
      fieldToReset.setValue(wfmap.get(field.getCaption()).getValue().toString());
    }
  }

  /**
   * returns the currently selected datasets by the user. If no datasets are selected, the list is simply empty
   * Note that no db selections are returned.
   * @return
   */
  public List<DatasetBean> getSelectedDatasets() {
    List<DatasetBean> selectedDatasets = new ArrayList<DatasetBean>();
    
    java.util.Iterator<Component> tabs = inputFileForm.iterator();
    while (tabs.hasNext()) {
      Tab tab = inputFileForm.getTab(tabs.next());
      HorizontalLayout current = (HorizontalLayout) tab.getComponent();
      java.util.Iterator<Component> grids = current.iterator();
      while(grids.hasNext()){
        Grid currentGrid = (Grid) grids.next();
        //returns one (in single-selection mode) or all (in multi-selection mode) selected items
        Collection<Object> selected = currentGrid.getSelectedRows();
        for (Object o : selected) {
          if(o instanceof DatasetBean){
            DatasetBean selectedBean = (DatasetBean) o;
            selectedDatasets.add(selectedBean);
          }
        }      
      }
    }
    if(selectedDatasets.size() == 0){
      showError("Please selected some datasets");
    }
    return selectedDatasets;
  }
  
  /**
   * updates workflow parameters with the currently selected datasets and databases.
   * Be aware that it is not checked, whether the correct workflow is given as parameter
   * 
   * @param wf
   * @return false if nothing is selected for some tabs or wf is null or wf is empty
   */
  public boolean updateWorkflow(Workflow wf, WorkflowViewController controller){
    if(wf == null || wf.getData() == null || wf.getData().getData() == null || wf.getData().getData().isEmpty()) return false;
    
    java.util.Iterator<Component> i = inputFileForm.iterator();
    InputList inpList = wf.getData();
    while (i.hasNext()) {
      Tab tab = inputFileForm.getTab(i.next());

      HorizontalLayout current = (HorizontalLayout) tab.getComponent();
      java.util.Iterator<Component> j = current.iterator();
      while (j.hasNext()) {
        Grid currentGrid = (Grid) j.next();

        String caption = tab.getCaption();

        if (currentGrid.getSelectionModel() instanceof SingleSelectionModel) {
          Object selectionSingle = currentGrid.getSelectedRow();
          if (selectionSingle == null) {
            showError("Warning: Nothing selected for single input parameter " + caption);
            return false;
          }
          if (selectionSingle instanceof FastaBean) {
            FastaBean selectedBean = (FastaBean) selectionSingle;
            inpList.getData().get(caption).setValue(selectedBean.getPath());
          } else {
            DatasetBean selectedBean = (DatasetBean) selectionSingle;
            try{
              inpList.getData().get(caption).setValue(controller.getDatasetsNfsPath(selectedBean));
            }catch(Exception e){
              LOGGER.error("could not retrieve nfs path. Using datasetbeans getfullpath instead. "+ e.getMessage(),e.getStackTrace());
              inpList.getData().get(caption).setValue(selectedBean.getFullPath());
            }
          }

        } else {
          Collection<Object> selectionMulti = currentGrid.getSelectedRows();
          if (selectionMulti == null || selectionMulti.isEmpty()) {
            showError("Warning: Nothing selected for multi input parameter " + caption);
            return false;
          }
          List<String> selectedPaths = new ArrayList<String>();

          for (Object o : selectionMulti) {
            DatasetBean selectedBean = (DatasetBean) o;
            try{
              selectedPaths.add(controller.getDatasetsNfsPath(selectedBean));
            }catch(Exception e){
              LOGGER.error("could not retrieve nfs path. Using datasetbeans getfullpath instead. "+ e.getMessage(),e.getStackTrace());
              selectedPaths.add(selectedBean.getFullPath());
            }
          }
          inpList.getData().get(caption).setValue(selectedPaths);
        }
      }
    }
    return true;
  }

  @Override
  public void buildLayout() {
    // TODO Auto-generated method stub

  }
  
  
  public void showError(String message){
    LOGGER.warn(message);
    Notification.show(message, Type.WARNING_MESSAGE);
  }

  @Override
  public void buildLayout(Workflow wf) {
    // TODO Auto-generated method stub
    
  }
}