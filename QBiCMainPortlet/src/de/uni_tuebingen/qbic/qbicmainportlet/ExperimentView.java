package de.uni_tuebingen.qbic.qbicmainportlet;

import java.util.ArrayList;

import org.tepi.filtertable.FilterTable;

import com.apple.eawt.Application;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.CustomTable.RowHeaderMode;

public class ExperimentView extends Panel {

  /**
   * 
   */
  private static final long serialVersionUID = -9156593640161721690L;
  FilterTable table;
  VerticalLayout vert;

  private String id;

  public ExperimentView(FilterTable table, IndexedContainer datasource, String id) {
    vert = new VerticalLayout();
    this.id = id;

    this.table = buildFilterTable();
    this.table.setSelectable(true);
    this.table.setSizeFull();

    vert.addComponent(this.table);
    vert.setComponentAlignment(this.table, Alignment.TOP_CENTER);
    this.setContent(vert);

    this.table.setContainerDataSource(datasource);
    this.tableClickChangeTreeView();
  }


  public ExperimentView() {
    // execute the above constructor with default settings, in order to have the same settings
    this(new FilterTable(), new IndexedContainer(), "No project selected");
  }

  public void setSizeFull() {
    this.table.setSizeFull();
    vert.setSizeFull();
    super.setSizeFull();
  }

  /**
   * sets the ContainerDataSource for showing it in a table and the id of the current Openbis
   * Experiment. The id is shown in the caption.
   * 
   * @param projectInformation
   * @param id
   */
  public void setContainerDataSource(ExperimentInformation expInformation, String id) {
    this.setStatistics(expInformation);
    this.table.setContainerDataSource(expInformation.samples);
    this.id = id;
    this.updateCaption();
  }

  private void setStatistics(ExperimentInformation expInformation) {
    this.vert.removeAllComponents();

    VerticalLayout statistics = new VerticalLayout();


    statistics.addComponent(new Label(String.format("Experiment Type: %s",
        expInformation.experimentType)));
    statistics.addComponent(new Label(String.format("Number of Samples: %s",
        expInformation.numberOfSamples)));
    statistics.addComponent(new Label(String.format("Number of Datasets: %s",
        expInformation.numberOfDatasets)));

    statistics.addComponent(new Label(expInformation.propertiesFormattedString, ContentMode.HTML));

    if (expInformation.numberOfDatasets > 0) {

      String lastSample = "No Samples available";
      if (expInformation.lastChangedSample != null) {
        lastSample = expInformation.lastChangedSample.split("/")[2];
      }
      statistics.addComponent(new Label(String.format(
          "Last Change: %s",
          String.format("In Sample: %s. Date: %s", lastSample,
              expInformation.lastChangedDataset.toString()))));
    }
    this.vert.setSpacing(true);
    this.vert.setMargin(true);
    this.vert.addComponent(statistics);
    this.vert.addComponent(this.table);
  }


  private void updateCaption() {
    this.setCaption(String.format("Statistics of Experiment: %s", id));
  }

  private void tableClickChangeTreeView() {
    table.setSelectable(true);
    table.setImmediate(true);
    //this.table.addValueChangeListener(new ViewTablesClickListener(table, "Sample"));
  }
  
  private FilterTable buildFilterTable() {
    FilterTable filterTable = new FilterTable();
    filterTable.setSizeFull();

    filterTable.setFilterDecorator(new DatasetViewFilterDecorator());
    filterTable.setFilterGenerator(new DatasetViewFilterGenerator());

    filterTable.setFilterBarVisible(true);

    filterTable.setSelectable(true);
    filterTable.setImmediate(true);
    filterTable.setMultiSelect(true);

    filterTable.setRowHeaderMode(RowHeaderMode.INDEX);

    filterTable.setColumnCollapsingAllowed(true);

    filterTable.setColumnReorderingAllowed(true);

    filterTable.setCaption("Registered Samples");

    return filterTable;
  }

}
