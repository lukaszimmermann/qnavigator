/*******************************************************************************
 * QBiC Project qNavigator enables users to manage their projects.
 * Copyright (C) "2016”  Christopher Mohr, David Wojnar, Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package qbic.vaadincomponents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import logging.Log4j2Logger;

import org.apache.commons.lang.NotImplementedException;

import submitter.Workflow;
import submitter.parameters.FileListParameter;
import submitter.parameters.FileParameter;
import submitter.parameters.InputList;
import submitter.parameters.Parameter;
import submitter.parameters.ParameterSet;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Grid.SingleSelectionModel;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
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

  private logging.Logger LOGGER = new Log4j2Logger(InputFilesComponent.class);
  private Map<String, Parameter> wfmap = new HashMap<String, Parameter>();;


  public InputFilesComponent(Map<String, Parameter> parameters) {
    super();
    wfmap = parameters;
  }

  public InputFilesComponent() {
    this.setCaption(String.format("<font color=#FF0000>  Select input file(s) </font>"));
    this.setCaptionAsHtml(true);
    inputFileForm.setHeight(100.0f, Unit.PERCENTAGE);
    inputFileForm.addStyleName(ValoTheme.TABSHEET_FRAMED);
    inputFileForm.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);
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

  /**
   * DAVID
   * 
   * @param wfparameters
   * @param datasets
   */
  public void buildLayout(Map<String, Parameter> wfparameters,
      BeanItemContainer<DatasetBean> datasets) {
    wfmap = wfparameters;
    for (Map.Entry<String, Parameter> entry : wfmap.entrySet()) {
      GeneratedPropertyContainer gpcontainer = null;
      Grid newGrid = new Grid(gpcontainer);
      if (entry.getValue() instanceof FileParameter
          || entry.getValue() instanceof FileListParameter) {

        // TODO also do filtering on filetype level
        List<String> range = getRange(entry.getValue());

        if (range.contains("fasta") || range.contains("gtf")) {
          gpcontainer = fastaContainer();
          // show only bwaIndex references for bwa
        } else if (range.contains("bwaIndex")) {
          gpcontainer = fastaContainerFiltered("bwa");
        } else {
          gpcontainer = filter(datasets, range);
        }
        newGrid.setContainerDataSource(gpcontainer);
        newGrid.setSelectionMode(getSelectionMode(entry.getValue()));
      } else {
        // showError(String.format("Invalid Inputfile Parameter!", entry.getKey()));
        helpers.Utils.Notification("Invalid Inputfile Parameter",
            "Invalid value for inputfile parameter has been provided." + entry.getKey(), "error");
      }
      HorizontalLayout layout = new HorizontalLayout();
      layout.setMargin(new MarginInfo(true, true, true, true));
      layout.setSizeFull();

      newGrid.setWidth("100%");
      layout.addComponent(newGrid);

      if (newGrid.getContainerDataSource().size() == 0) {
        // Notification.show(
        // String.format("No dataset of type %s available in this project!", entry.getKey()),
        // Type.WARNING_MESSAGE);
        helpers.Utils
            .Notification(
                "Missing Dataset Type",
                String
                    .format(
                        "Workflow submission might not be possible because no dataset of type %s is available in this project",
                        entry.getKey()), "warning");
        layout.addComponent(newGrid);
      }

      helpers.GridFunctions.addColumnFilters(newGrid, gpcontainer);
      inputFileForm.addTab(layout, entry.getKey());
    }
  }

  private SelectionMode getSelectionMode(Parameter param) {
    if (param instanceof FileParameter) {
      return SelectionMode.SINGLE;
    } else if (param instanceof FileListParameter) {
      return SelectionMode.MULTI;
    } else {
      return SelectionMode.NONE;
    }
  }

  private List<String> getRange(Parameter param) {
    if (param instanceof FileParameter) {
      return ((FileParameter) param).getRange();
    } else if (param instanceof FileListParameter) {
      return ((FileListParameter) param).getRange();
    } else {
      return new ArrayList<String>();
    }

  }

  public GeneratedPropertyContainer fastaContainer() {
    BeanItemContainer<FastaBean> subContainer = new BeanItemContainer<FastaBean>(FastaBean.class);
    FastaDB db = new FastaDB();
    subContainer.addAll(db.getAll());
    GeneratedPropertyContainer gpcontainer = new GeneratedPropertyContainer(subContainer);
    gpcontainer.removeContainerProperty("path");
    return gpcontainer;
  }

  // Function to retrieve only specific references, e.g. bwa for BWA indices, could be used for
  // proteomics as well
  public GeneratedPropertyContainer fastaContainerFiltered(String filter) {
    BeanItemContainer<FastaBean> subContainer = new BeanItemContainer<FastaBean>(FastaBean.class);
    FastaDB db = new FastaDB();

    if (filter.equals("bwa")) {
      subContainer.addAll(db.getBWAIndices());
    }

    GeneratedPropertyContainer gpcontainer = new GeneratedPropertyContainer(subContainer);
    gpcontainer.removeContainerProperty("path");
    return gpcontainer;
  }

  /**
   * filters all DataSetBeans which are NOT in the filter and returns a new Container
   * 
   * @param datasets
   * @param filter
   * @return
   */
  public GeneratedPropertyContainer filter(BeanItemContainer<DatasetBean> datasets,
      List<String> filter) {
    BeanItemContainer<DatasetBean> subContainer =
        new BeanItemContainer<DatasetBean>(DatasetBean.class);

    for (java.util.Iterator<DatasetBean> i = datasets.getItemIds().iterator(); i.hasNext();) {
      DatasetBean dataset = i.next();

      // We dont' want to show html and zip files as workflow input (for now). In general we should
      // use the filter[1] which is the filetype.
      // However it has to be specified in the corresponding CTD.
      // For now, do workflow specific filtering additionally
      if (((filter.contains(dataset.getFileType().toLowerCase()) | filter.contains(dataset
          .getFileType()))) & dataset.getFileType().equals("Q_WF_NGS_HLATYPING_RESULTS")) {
        if (dataset.getFileName().endsWith("alleles")) {
          subContainer.addBean(dataset);
        }
      } else if (filter.contains(dataset.getFileType().toLowerCase())
          | filter.contains(dataset.getFileType())
          & !(dataset.getFileName().endsWith(".html") | dataset.getFileName().endsWith(".zip")
              | dataset.getFileName().endsWith(".pdf")
              | dataset.getFileName().endsWith(".origlabfilename")
              | dataset.getFileName().endsWith(".sha256sum") | dataset.getFileName().contains(
              "source_dropbox"))) {
        subContainer.addBean(dataset);
      }
    }

    GeneratedPropertyContainer gpcontainer = new GeneratedPropertyContainer(subContainer);
    gpcontainer.removeContainerProperty("fullPath");
    gpcontainer.removeContainerProperty("openbisCode");
    gpcontainer.removeContainerProperty("properties");

    gpcontainer.addGeneratedProperty("Additional Info", new PropertyValueGenerator<String>() {

      @Override
      public Class<String> getType() {
        return String.class;
      }

      @Override
      public String getValue(Item item, Object itemId, Object propertyId) {
        Map<String, String> properties =
            (Map<String, String>) item.getItemProperty("properties").getValue();

        String additionalInfo = "";
        if (properties != null) {
          if (properties.containsKey("Q_ADDITIONAL_INFO")) {
            additionalInfo = properties.get("Q_ADDITIONAL_INFO");
          }
        }
        return additionalInfo;
      }
    });
    return gpcontainer;
  }


  /*
   * public void buildLayout(Set<Entry<String, Parameter>> wfparameters,
   * BeanItemContainer<DatasetBean> datasets) { this.wfparameters = wfparameters;
   * buildForm(wfparameters, datasets); }
   */
  public void buildForm(Set<Entry<String, Parameter>> wfparameters,
      BeanItemContainer<DatasetBean> datasets) {

    inputFileForm.removeAllComponents();
    inputFileForm.setSizeFull();

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
        helpers.Utils.Notification("Invalid Inputfile Parameter",
            "Invalid inputfile parameter has been selected: " + entry.getKey(), "error");
      }

      HorizontalLayout layout = new HorizontalLayout();
      layout.setMargin(new MarginInfo(true, true, true, true));
      layout.setSizeFull();

      newGrid.setWidth("100%");
      layout.addComponent(newGrid);

      if (newGrid.getContainerDataSource().size() == 0) {
        helpers.Utils
            .Notification(
                "Missing Dataset Type",
                String
                    .format(
                        "Workflow submission might not be possible because no dataset of type %s is available in this project",
                        entry.getKey()), "warning");
        // Notification.show(
        // String.format("No dataset of type %s available in this project!", entry.getKey()),
        // Type.WARNING_MESSAGE);
        layout.addComponent(newGrid);
      }

      inputFileForm.addTab(layout, entry.getKey());
    }
  }

  // TODO
  public void resetInputList() {
    throw new NotImplementedException();
    /*
     * Collection<Field<?>> registeredFields = inputFileFieldGroup.getFields();
     * 
     * for (Field<?> field : registeredFields) { TextField fieldToReset = (TextField) field;
     * fieldToReset.setValue(wfmap.get(field.getCaption()).getValue().toString()); }
     */
  }

  /**
   * returns the currently selected datasets by the user. If no datasets are selected, the list is
   * simply empty Note that no db selections are returned.
   * 
   * @return
   */
  public List<DatasetBean> getSelectedDatasets() {
    List<DatasetBean> selectedDatasets = new ArrayList<DatasetBean>();

    java.util.Iterator<Component> tabs = inputFileForm.iterator();
    while (tabs.hasNext()) {
      Tab tab = inputFileForm.getTab(tabs.next());
      HorizontalLayout current = (HorizontalLayout) tab.getComponent();
      java.util.Iterator<Component> grids = current.iterator();
      while (grids.hasNext()) {
        Grid currentGrid = (Grid) grids.next();
        // returns one (in single-selection mode) or all (in multi-selection mode) selected items
        Collection<Object> selected = currentGrid.getSelectedRows();
        for (Object o : selected) {
          if (o instanceof DatasetBean) {
            DatasetBean selectedBean = (DatasetBean) o;
            selectedDatasets.add(selectedBean);
          }
        }
      }
    }
    if (selectedDatasets.size() == 0) {
      helpers.Utils.Notification("No dataset selected", "Please select at least one dataset.",
          "error");
      // showError("Please select at least one dataset.");
    }
    return selectedDatasets;
  }

  /**
   * returns true if at least one dataset was selected for each tab
   * 
   * @return
   */
  boolean hasDastasetSelected() {
    java.util.Iterator<Component> tabs = inputFileForm.iterator();
    while (tabs.hasNext()) {
      Tab tab = inputFileForm.getTab(tabs.next());
      HorizontalLayout current = (HorizontalLayout) tab.getComponent();
      java.util.Iterator<Component> grids = current.iterator();
      while (grids.hasNext()) {
        Grid currentGrid = (Grid) grids.next();
        // getSelectedRows returns one (in single-selection mode) or all (in multi-selection mode)
        // selected items
        if (currentGrid.getSelectedRows().size() == 0)
          return false;
      }
    }
    return true;
  }

  /**
   * updates workflow parameters with the currently selected datasets and databases. Be aware that
   * it is not checked, whether the correct workflow is given as parameter
   * 
   * @param wf
   * @return false if nothing is selected for some tabs or wf is null or wf is empty
   */
  public boolean updateWorkflow(Workflow wf, WorkflowViewController controller) {
    if (wf == null || wf.getData() == null || wf.getData().getData() == null
        || wf.getData().getData().isEmpty())
      return false;

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
          // Quick solution
          // TODO
          if (selectionSingle == null) {
            if (caption.equals("InputFiles.1.Germline Mutations")
                || caption.equals("InputFiles.1.bam")) {
              continue;
            } else {
              helpers.Utils.Notification("Missing single input parameter",
                  "Nothing selected for single input parameter " + caption, "warning");
              // showError("Warning: Nothing selected for single input parameter " + caption);
              return false;
            }
          }
          if (selectionSingle instanceof FastaBean) {
            FastaBean selectedBean = (FastaBean) selectionSingle;
            inpList.getData().get(caption).setValue(selectedBean.getPath());
          } else {
            DatasetBean selectedBean = (DatasetBean) selectionSingle;
            try {
              inpList.getData().get(caption).setValue(controller.getDatasetsNfsPath(selectedBean));
            } catch (Exception e) {
              LOGGER.error("could not retrieve nfs path. Using datasetbeans getfullpath instead. "
                  + e.getMessage(), e.getStackTrace());
              inpList.getData().get(caption).setValue(selectedBean.getFullPath());
            }
          }

        } else {
          Collection<Object> selectionMulti = currentGrid.getSelectedRows();
          if ((selectionMulti == null || selectionMulti.isEmpty())
              && (!caption.equals("InputFiles.1.fastq"))) {
            helpers.Utils.Notification("Missing single input parameter",
                "Nothing selected for single input parameter " + caption, "warning");
            // showError("Warning: Nothing selected for multi input parameter " + caption);
            return false;
          }
          List<String> selectedPaths = new ArrayList<String>();

          for (Object o : selectionMulti) {
            DatasetBean selectedBean = (DatasetBean) o;
            try {
              selectedPaths.add(controller.getDatasetsNfsPath(selectedBean));
            } catch (Exception e) {
              LOGGER.error("could not retrieve nfs path. Using datasetbeans getfullpath instead. "
                  + e.getMessage(), e.getStackTrace());
              selectedPaths.add(selectedBean.getFullPath());
            }
          }
          inpList.getData().get(caption).setValue(selectedPaths);
        }
      }
    }
    return true;
  }



  /**
   * returns the number of file parameters
   * 
   * @return
   */
  public int size() {
    return this.wfmap.size();
  }

  @Override
  public void buildLayout() {
    // TODO Auto-generated method stub

  }


  public void showError(String message) {
    LOGGER.warn(message);
    Notification.show(message, Type.WARNING_MESSAGE);
  }

  @Override
  public void buildLayout(Workflow wf) {
    // TODO Auto-generated method stub

  }
}
