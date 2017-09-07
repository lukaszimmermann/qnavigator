/*******************************************************************************
 * QBiC Project qNavigator enables users to manage their projects. Copyright (C) "2016” Christopher
 * Mohr, David Wojnar, Andreas Friedrich
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.uni_tuebingen.qbic.qbicmainportlet;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.tepi.filtertable.FilterTable;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomTable.RowHeaderMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import logging.Log4j2Logger;
import model.SearchResultsExperimentBean;
import model.SearchResultsProjectBean;
import model.SearchResultsSampleBean;

public class SearchResultsView extends VerticalLayout implements View {

  /**
   * 
   */
  private static final long serialVersionUID = -9100320125534037596L;

  /**
   * 
   */

  public final static String navigateToLabel = "searchresults";

  private logging.Logger LOGGER = new Log4j2Logger(SearchResultsView.class);

  String caption;
  FilterTable table;

  DataHandler datahandler;

  VerticalLayout searchResultsLayout;

  BeanItemContainer<SearchResultsSampleBean> sampleBeanContainer;
  BeanItemContainer<SearchResultsExperimentBean> expBeanContainer;
  BeanItemContainer<SearchResultsProjectBean> projBeanContainer;

  String queryString = new String();
  Grid projectGrid = new Grid();
  Grid sampleGrid = new Grid();
  Grid expGrid = new Grid();
  // Boolean includePatientCreation = false;
  ToolBar toolBar;
  State state;
  String resourceUrl;
  String header;

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }


  private Button export = new Button("Export as TSV");

  private int numberOfProjects = 0;

  private String user;

  public SearchResultsView(DataHandler datahandler, String caption, String user, State state,
      String resUrl) {
    searchResultsLayout = new VerticalLayout();
    this.table = buildFilterTable();
    this.datahandler = datahandler;

    this.state = state;
    this.resourceUrl = resUrl;

    this.user = user;
  }


  public void setSizeFull() {
    searchResultsLayout.setSizeFull();
    super.setSizeFull();
    this.table.setSizeFull();
    searchResultsLayout.setSpacing(true);
    // homeview_content.setMargin(true);
  }

  /**
   * sets the ContainerDataSource of this view. Should usually contains project information. Caption
   * is caption.
   * 
   * @param homeViewInformation
   * @param caption
   */
  public void setContainerDataSource(String caption) {

    this.caption = caption;
    // this.currentBean = spaceBean;
    // this.numberOfProjects = currentBean.getProjects().size();
    //
    // setExportButton();
    //
    // this.table.setContainerDataSource(spaceBean.getProjects());
    // this.table.setVisibleColumns(new Object[] {"code", "space", "description"});
    // this.table.setColumnHeader("code", "Name");
    // this.table.setColumnHeader("space", "Project");
    // this.table.setColumnHeader("description", "Description");
    // this.table.setColumnExpandRatio("Name", 1);
    // this.table.setColumnExpandRatio("Description", 3);


    List<Sample> sampleResults = datahandler.getSampleResults();
    List<Experiment> expResults = datahandler.getExpResults();
    List<Project> projResults = datahandler.getProjResults();


    queryString = datahandler.getLastQueryString();

    // get the project search result data
    Collection<SearchResultsProjectBean> projCollection = new ArrayList<SearchResultsProjectBean>();
    SearchResultsProjectBean tmpProjectBean;

    for (Project p : projResults) {
      tmpProjectBean = new SearchResultsProjectBean(p, queryString);
      projCollection.add(tmpProjectBean);
    }

    projBeanContainer = new BeanItemContainer<SearchResultsProjectBean>(
        SearchResultsProjectBean.class, projCollection);


    // get the experiment search result data
    Collection<SearchResultsExperimentBean> expCollection =
        new ArrayList<SearchResultsExperimentBean>();

    SearchResultsExperimentBean tmpExperimentBean;

    for (Experiment i : expResults) {
      // System.out.println(i);
      // Label sampleLabel = new Label(i.toString());
      // SearchResultsSampleItem sampleItem = new SearchResultsSampleItem(i, rowNumber);

      // LOGGER.info(i.getIdentifier());
      tmpExperimentBean = new SearchResultsExperimentBean(i, queryString);
      expCollection.add(tmpExperimentBean);

    }

    expBeanContainer = new BeanItemContainer<SearchResultsExperimentBean>(
        SearchResultsExperimentBean.class, expCollection);

    // get the sample search result data
    Collection<SearchResultsSampleBean> sampleCollection = new ArrayList<SearchResultsSampleBean>();
    SearchResultsSampleBean tmpSearchBean;

    for (Sample i : sampleResults) {
      // System.out.println(i);
      // Label sampleLabel = new Label(i.toString());
      // SearchResultsSampleItem sampleItem = new SearchResultsSampleItem(i, rowNumber);

      tmpSearchBean = new SearchResultsSampleBean(i, queryString);
      sampleCollection.add(tmpSearchBean);

    }

    sampleBeanContainer = new BeanItemContainer<SearchResultsSampleBean>(
        SearchResultsSampleBean.class, sampleCollection);

  }

  private void setExportButton() {
    // buttonLayoutSection.removeAllComponents();
    // HorizontalLayout buttonLayout = new HorizontalLayout();
    // buttonLayout.setHeight(null);
    // buttonLayout.setWidth("100%");
    // buttonLayoutSection.addComponent(buttonLayout);
    //
    // buttonLayout.addComponent(this.export);
    //
    // StreamResource sr =
    // Utils.getTSVStream(Utils.containerToString(currentBean.getProjects()), this.caption);
    // FileDownloader fileDownloader = new FileDownloader(sr);
    // fileDownloader.extend(this.export);
  }

  /**
   * updates view, if height, width or the browser changes.
   * 
   * @param browserHeight
   * @param browserWidth
   * @param browser
   */
  public void updateView(int browserHeight, int browserWidth, WebBrowser browser) {
    setWidth((browserWidth * 0.85f), Unit.PIXELS);
  }

  /**
   * 
   * @return
   */
  ToolBar initToolBar() {
    // SearchBarView searchBarView = new SearchBarView(datahandler);
    SearchEngineView searchEngineView = new SearchEngineView(datahandler);

    toolBar = new ToolBar(resourceUrl, state, searchEngineView);
    toolBar.init();
    return toolBar;
  }

  /**
   * updates the menu bar based on the new content (currentbean was changed)
   */
  void updateContentToolBar() {
    toolBar.setDownload(false);
    toolBar.setWorkflow(false);
    toolBar.update("", "");
  }

  void buildLayout(int browserHeight, int browserWidth, WebBrowser browser) {
    // this.setMargin(new MarginInfo(true, true, false, false));
    // clean up first
    searchResultsLayout.removeAllComponents();
    searchResultsLayout.setWidth("100%");
    // searchResultsLayout.setSpacing(true);

    searchResultsLayout.setCaption("Search results for query '" + queryString + "'");
    // Label header = new Label("Search results for query '" + queryString + "':");
    // searchResultsLayout.addComponent(header);

    // updateView(browserWidth, browserWidth, browser);

    VerticalLayout viewContent = new VerticalLayout();
    viewContent.setWidth("100%");
    viewContent.setSpacing(true);
    viewContent.setMargin(new MarginInfo(true, false, false, false));

    List<String> showOptions = datahandler.getShowOptions();

    if (showOptions.contains("Projects")) {
      projectGrid = new Grid(projBeanContainer);
      projectGrid.setCaption("Found Projects");
      projectGrid.setColumnOrder("projectID", "description");
      projectGrid.setSizeFull();

      projectGrid.setHeightMode(HeightMode.ROW);
      projectGrid.setHeightByRows(5);
      projectGrid.setSelectionMode(SelectionMode.SINGLE);

      projectGrid.addItemClickListener(new ItemClickListener() {

        @Override
        public void itemClick(ItemClickEvent event) {
          // TODO Auto-generated method stub
          String cellType = new String(event.getPropertyId().toString());

          if (cellType.equals("projectID")) {
            String cellContent = new String(
                projBeanContainer.getContainerProperty(event.getItemId(), event.getPropertyId())
                    .getValue().toString());

            State state = (State) UI.getCurrent().getSession().getAttribute("state");
            ArrayList<String> message = new ArrayList<String>();
            message.add("clicked");
            message.add(cellContent);
            message.add("project");
            state.notifyObservers(message);
          }
        }
      });

      if (projBeanContainer.size() == 0) {
        Label noSamples = new Label("no projects were found");
        noSamples.setCaption("Found Projects");
        viewContent.addComponent(noSamples);
      } else {
        viewContent.addComponent(projectGrid);
      }
    }

    if (showOptions.contains("Experiments")) {
      // expGrid = new Grid(expBeanContainer);
      expGrid = new Grid(expBeanContainer);
      expGrid.setCaption("Found Experiments");
      expGrid.setColumnOrder("experimentID", "experimentName", "matchedField");
      expGrid.setSizeFull();

      expGrid.getColumn("experimentID").setExpandRatio(0);
      expGrid.getColumn("experimentName").setExpandRatio(1);
      expGrid.getColumn("matchedField").setExpandRatio(1);

      expGrid.setHeightMode(HeightMode.ROW);
      expGrid.setHeightByRows(5);
      expGrid.setSelectionMode(SelectionMode.SINGLE);



      expGrid.addItemClickListener(new ItemClickListener() {
        @Override
        public void itemClick(ItemClickEvent event) {
          String cellType = new String(event.getPropertyId().toString());

          if (cellType.equals("experimentID")) {
            String cellContent = new String(
                expBeanContainer.getContainerProperty(event.getItemId(), event.getPropertyId())
                    .getValue().toString());


            State state = (State) UI.getCurrent().getSession().getAttribute("state");
            ArrayList<String> message = new ArrayList<String>();
            message.add("clicked");
            message.add(cellContent);
            message.add("experiment");
            state.notifyObservers(message);
          }
        }
      });

      if (expBeanContainer.size() == 0) {
        Label noExps = new Label("no experiments were found");
        noExps.setCaption("Found Experiments");
        viewContent.addComponent(noExps);
      } else {
        viewContent.addComponent(expGrid);
      }

    }

    if (showOptions.contains("Samples")) {
      sampleGrid = new Grid(sampleBeanContainer);
      sampleGrid.setCaption("Found Samples");
      sampleGrid.setColumnOrder("sampleID", "sampleName", "matchedField");
      sampleGrid.setSizeFull();

      sampleGrid.getColumn("sampleID").setExpandRatio(0);
      sampleGrid.getColumn("sampleName").setExpandRatio(1);
      sampleGrid.getColumn("matchedField").setExpandRatio(1);

      sampleGrid.setHeightMode(HeightMode.ROW);
      sampleGrid.setHeightByRows(5);
      sampleGrid.setSelectionMode(SelectionMode.SINGLE);

      sampleGrid.addItemClickListener(new ItemClickListener() {
        @Override
        public void itemClick(ItemClickEvent event) {
          String cellType = new String(event.getPropertyId().toString());

          if (cellType.equals("sampleID")) {
            String cellContent = new String(
                sampleBeanContainer.getContainerProperty(event.getItemId(), event.getPropertyId())
                    .getValue().toString());

            State state = (State) UI.getCurrent().getSession().getAttribute("state");
            ArrayList<String> message = new ArrayList<String>();
            message.add("clicked");
            message.add(cellContent);
            message.add("sample");
            state.notifyObservers(message);
          }
        }
      });

      if (sampleBeanContainer.size() == 0) {
        Label noSamples = new Label("no samples were found");
        noSamples.setCaption("Found Samples");
        viewContent.addComponent(noSamples);
      } else {
        viewContent.addComponent(sampleGrid);
      }
    }


    searchResultsLayout.addComponent(viewContent);

    this.addComponent(searchResultsLayout);
  }


  private void updateUI() {
    expGrid.setContainerDataSource(expBeanContainer);
    sampleGrid.setContainerDataSource(sampleBeanContainer);

  }

  private void tableClickChangeTreeView() {
    table.setSelectable(true);
    table.setImmediate(true);
    this.table
        .addValueChangeListener(new ViewTablesClickListener(table, ProjectView.navigateToLabel));
  }


  private FilterTable buildFilterTable() {
    FilterTable filterTable = new FilterTable();

    filterTable.setFilterDecorator(new DatasetViewFilterDecorator());
    filterTable.setFilterGenerator(new DatasetViewFilterGenerator());

    filterTable.setFilterBarVisible(true);

    filterTable.setSelectable(true);
    filterTable.setImmediate(true);

    filterTable.setRowHeaderMode(RowHeaderMode.INDEX);

    filterTable.setColumnCollapsingAllowed(false);

    filterTable.setColumnReorderingAllowed(true);
    return filterTable;
  }

  @Override
  public void enter(ViewChangeEvent event) {
    setContainerDataSource("test");
    int height = event.getNavigator().getUI().getPage().getBrowserWindowHeight();
    int width = event.getNavigator().getUI().getPage().getBrowserWindowWidth();
    buildLayout(height, width, event.getNavigator().getUI().getPage().getWebBrowser());
    updateUI();
  }

  /**
   * Enables or disables the component. The user can not interact disabled components, which are
   * shown with a style that indicates the status, usually shaded in light gray color. Components
   * are enabled by default.
   */
  public void setEnabled(boolean enabled) {
    this.export.setEnabled(enabled);
    this.table.setEnabled(enabled);
  }


}
