package de.uni_tuebingen.qbic.qbicmainportlet;

  import java.util.List;
import java.util.Map;

import logging.Log4j2Logger;
import logging.Logger;
import model.BiologicalEntitySampleBean;
import model.BiologicalSampleBean;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.SelectionEvent;
import com.vaadin.event.SelectionEvent.SelectionListener;
import com.vaadin.server.Page;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickListener;
import com.vaadin.ui.renderers.HtmlRenderer;


public class BiologicalSamplesComponent extends CustomComponent{
  
    private enum sampleTypes {
      Q_BIOLOGICAL_ENTITY,
      Q_BIOLOGICAL_SAMPLE
    };
    
    private enum propertyTypes {
      Q_ADDIIONAL_INFO,
      Q_EXTERNALDB_ID,
      Q_SECONDARY_NAME,
     Q_NCBI_ORGANISM
    };
  
    private static final long serialVersionUID = 8672873911284888801L;

    private VerticalLayout mainLayout;
    private static Logger LOGGER = new Log4j2Logger(BiologicalSamplesComponent.class);
    private Grid sampleBioGrid;
    private Grid sampleEntityGrid;
    
    VerticalLayout vert;

    private DataHandler datahandler;
    private State state;
    private String resourceUrl;
   
    private int numberOfBioSamples;

    private int numberOfEntitySamples;

    private BeanItemContainer<BiologicalSampleBean> samplesBio;

    private BeanItemContainer<BiologicalEntitySampleBean> samplesEntity;
    
    public BiologicalSamplesComponent(DataHandler dh, State state, String resourceurl, String caption) {
      this.datahandler = dh;
      this.resourceUrl = resourceurl;
      this.state = state;
      
      this.setCaption(caption);
      
      this.initUI();
    }

    private void initUI() {
      vert = new VerticalLayout();
      sampleBioGrid = new Grid();
      sampleEntityGrid = new Grid();
      
      sampleEntityGrid.addSelectionListener(new SelectionListener() {

        @Override
        public void select(SelectionEvent event) {
          BeanItem<BiologicalEntitySampleBean> selectedBean = samplesEntity.getItem(sampleEntityGrid.getSelectedRow());
          
          if(selectedBean == null) {
            TextField filterField = (TextField) sampleBioGrid.getHeaderRow(1).getCell("biologicalEntity").getComponent();
            filterField.setValue("");
          }
          else {
            TextField filterField = (TextField) sampleBioGrid.getHeaderRow(1).getCell("biologicalEntity").getComponent();
            filterField.setValue(selectedBean.getBean().getCode());
          //samplesBio.addContainerFilter("biologicalEntity", selectedBean.getBean().getSecondaryName(), false, false);  
          }
        }
        
      });
      
      mainLayout = new VerticalLayout(vert);
      
      this.setWidth(Page.getCurrent().getBrowserWindowWidth() * 0.8f, Unit.PIXELS);
      this.setCompositionRoot(mainLayout);
    }
    
    public void updateUI(String id) {
      sampleBioGrid = new Grid();
      sampleEntityGrid = new Grid();
      
      sampleEntityGrid.addSelectionListener(new SelectionListener() {

        @Override
        public void select(SelectionEvent event) {
          BeanItem<BiologicalEntitySampleBean> selectedBean = samplesEntity.getItem(sampleEntityGrid.getSelectedRow());
          
          if(selectedBean == null) {
            TextField filterField = (TextField) sampleBioGrid.getHeaderRow(1).getCell("biologicalEntity").getComponent();
            filterField.setValue("");
          }
          else {
            TextField filterField = (TextField) sampleBioGrid.getHeaderRow(1).getCell("biologicalEntity").getComponent();
            filterField.setValue(selectedBean.getBean().getCode());
          //samplesBio.addContainerFilter("biologicalEntity", selectedBean.getBean().getSecondaryName(), false, false);  
          }
        }
        
      });
      
      if(id == null) return;
            
              BeanItemContainer<BiologicalSampleBean> samplesBioContainer = new BeanItemContainer<BiologicalSampleBean>(BiologicalSampleBean.class);
              BeanItemContainer<BiologicalEntitySampleBean> samplesEntityContainer = new BeanItemContainer<BiologicalEntitySampleBean>(BiologicalEntitySampleBean.class);

              List<Sample> allSamples =
                  datahandler.getOpenBisClient().getSamplesOfProject(id);

              for (Sample sample : allSamples) {
                
                if (sample.getSampleTypeCode().equals(sampleTypes.Q_BIOLOGICAL_ENTITY.toString())) {
                  
                  Map<String, String> sampleProperties =  sample.getProperties();
                  
                  BiologicalEntitySampleBean newEntityBean = new BiologicalEntitySampleBean();
                  newEntityBean.setCode(sample.getCode());
                  newEntityBean.setId(sample.getIdentifier());
                  newEntityBean.setType(sample.getSampleTypeCode());
                  newEntityBean.setAdditionalInfo(sampleProperties.get("Q_ADDIIONAL_INFO"));
                  newEntityBean.setExternalDB(sampleProperties.get("Q_EXTERNALDB_ID"));
                  newEntityBean.setSecondaryName(sampleProperties.get("Q_SECONDARY_NAME"));
                  
                  String organismID = sampleProperties.get("Q_NCBI_ORGANISM");
                  newEntityBean.setOrganism(organismID);
                  
                  List<PropertyType> bioSampleProperties =
                      datahandler.getOpenBisClient().listPropertiesForType(datahandler.getOpenBisClient()
                          .getSampleTypeByString(sampleTypes.Q_BIOLOGICAL_ENTITY.toString()));
                  
                  for (PropertyType pType : bioSampleProperties) {
                    if (pType.getCode().equals("Q_NCBI_ORGANISM")) {
                      newEntityBean.setOrganismName(datahandler.getOpenBisClient().getCVLabelForProperty(pType,organismID));
                    }
                  }
                  newEntityBean.setProperties(sampleProperties);
                  newEntityBean.setGender(sampleProperties.get("Q_GENDER"));
                  
                  samplesEntityContainer.addBean(newEntityBean);
                  
                  for(Sample child: datahandler.getOpenBisClient().getChildrenSamples(sample))
                  {
                   
                    if(child.getSampleTypeCode().equals(sampleTypes.Q_BIOLOGICAL_SAMPLE.toString())) {
                      Sample realChild = datahandler.getOpenBisClient().getSampleByIdentifier(child.getIdentifier());
                      
                  Map<String, String> sampleBioProperties = realChild.getProperties();
                    
                  BiologicalSampleBean  newBean = new BiologicalSampleBean();
                  newBean.setCode(realChild.getCode());
                  newBean.setId(realChild.getIdentifier());
                  newBean.setType(realChild.getSampleTypeCode());
                  newBean.setPrimaryTissue(sampleBioProperties.get("Q_PRIMARY_TISSUE"));
                  newBean.setTissueDetailed(sampleBioProperties.get("Q_TISSUE_DETAILED"));
                  newBean.setBiologicalEntity(sample.getCode() );

                  newBean.setAdditionalInfo(sampleBioProperties.get("Q_ADDIIONAL_INFO"));
                  newBean.setExternalDB(sampleBioProperties.get("Q_EXTERNALDB_ID"));
                  newBean.setSecondaryName(sampleBioProperties.get("Q_SECONDARY_NAME"));
                  newBean.setProperties(sampleBioProperties);
                  
                  samplesBioContainer.addBean(newBean);
                    }
                  }
                }
              }
              numberOfBioSamples = samplesBioContainer.size();
              numberOfEntitySamples = samplesEntityContainer.size();
              
              samplesBio = samplesBioContainer;
              samplesEntity = samplesEntityContainer;
              
              sampleEntityGrid.removeAllColumns();
              
              final GeneratedPropertyContainer gpcEntity = new GeneratedPropertyContainer(samplesEntity);
              gpcEntity.removeContainerProperty("id");
              gpcEntity.removeContainerProperty("type");
              gpcEntity.removeContainerProperty("organismName");


              sampleEntityGrid.setContainerDataSource(gpcEntity);
              sampleEntityGrid.setColumnReorderingAllowed(true);
              
              gpcEntity.addGeneratedProperty("Organism", new PropertyValueGenerator<String>() {

                @Override
                public Class<String> getType() {
                  return String.class;
                }

                @Override
                public String getValue(Item item, Object itemId, Object propertyId) {
                  String ncbi = String.format("http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?mode=Undef&name=%s&lvl=0&srchmode=1&keep=1&unlock' target='_blank'>%s</a>", item.getItemProperty("organism").getValue(),item.getItemProperty("organismName").getValue());
                  String link = String.format("<a href='%s", ncbi);
   
                  return link;
                }
              });
              
              sampleEntityGrid.getColumn("Organism").setRenderer(new HtmlRenderer());
              sampleEntityGrid.setColumnOrder("secondaryName","code", "Organism");

              final GeneratedPropertyContainer gpcBio = new GeneratedPropertyContainer(samplesBio);
              gpcBio.removeContainerProperty("id");
              gpcBio.removeContainerProperty("type");

              sampleBioGrid.setContainerDataSource(gpcBio);
              sampleBioGrid.setColumnReorderingAllowed(true);
              sampleBioGrid.setColumnOrder("secondaryName", "code");
              
              helpers.GridFunctions.addColumnFilters(sampleBioGrid, gpcBio);
              helpers.GridFunctions.addColumnFilters(sampleEntityGrid, gpcEntity);
              this.buildLayout();        
    }    
    
    /**
     * Precondition: {DatasetView#table} has to be initialized. e.g. with
     * {DatasetView#buildFilterTable} If it is not, strange behaviour has to be expected. builds the
     * Layout of this view.
     */
    private void buildLayout() {
      this.vert.removeAllComponents();
      this.vert.setWidth("100%");

      // Table (containing datasets) section
      VerticalLayout tableSection = new VerticalLayout();
      HorizontalLayout tableSectionContent = new HorizontalLayout();
      HorizontalLayout sampletableSectionContent = new HorizontalLayout();
      
      tableSectionContent.setMargin(new MarginInfo(true, false, true, false));
      sampletableSectionContent.setMargin(new MarginInfo(true, false, false, false));
      
      //tableSectionContent.setCaption("Datasets");
      //tableSectionContent.setIcon(FontAwesome.FLASK);
      tableSection.addComponent(new Label(String.format("This view shows the sample sources (e.g., human, mouse) to be studied and the corresponding extracted samples. With sample sources, information specific to the subject (e.g., age or BMI in the case of patient data) can be stored. The extracted sample is a sample which has been extracted from the corresponding sample source. This is the raw sample material that can be later prepared for specific analytical methods such as MS or NGS. "
          + "\n\n There are %s extracted  samples coming from %s distinct sample sources in this study.", numberOfBioSamples, numberOfEntitySamples), Label.CONTENT_PREFORMATTED));
      
      tableSectionContent.addComponent(sampleBioGrid);
      sampletableSectionContent.addComponent(sampleEntityGrid);
      
      sampleEntityGrid.setCaption("Sample Sources");
      sampleBioGrid.setCaption("Extracted Samples");
      
      tableSection.setMargin(new MarginInfo(true, false, false, true));
      tableSection.setSpacing(true);

      tableSection.addComponent(sampletableSectionContent);
      tableSection.addComponent(tableSectionContent);
      this.vert.addComponent(tableSection);

      sampleBioGrid.setWidth("100%");
      sampleEntityGrid.setWidth("100%");
      
      tableSection.setWidth("100%");
      sampletableSectionContent.setWidth("100%");
      tableSectionContent.setWidth("100%");

      // this.table.setSizeFull();

      HorizontalLayout buttonLayout = new HorizontalLayout();
      //buttonLayout.setMargin(new MarginInfo(false, false, false, true));
      buttonLayout.setHeight(null);
      //buttonLayout.setWidth("100%");
      buttonLayout.setSpacing(true);

    }
}

