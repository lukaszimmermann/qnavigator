package helpers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.WordUtils;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.ControlledVocabularyPropertyType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.EntityType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.PropertyTypeGroup;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SampleType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.VocabularyTerm;


public class OpenBisFunctions {

  /**
   * Returns the 4 or 5 character project prefix used for samples in openBIS.
   * 
   * @param sample sample ID starting with a standard project prefix.
   * @return Project prefix of the sample
   */
  public static String getProjectPrefix(String sample) {
    if (Utils.isInteger("" + sample.charAt(4)))
      return sample.substring(0, 4);
    else
      return sample.substring(0, 5);
  }

  public static double statusToDoubleValue(String status) {

    double value = 0.0;

    switch (status) {
      case "STARTED":
        value = 0.25;
        break;
      case "RUNNING":
        value = 0.5;
        break;
      case "FINISHED":
        value = 1.0;
        break;

    }
    return value;
  }
  
  /**
   * Function to list the vocabulary terms for a given property which has been added to openBIS. The
   * property has to be a Controlled Vocabulary Property.
   * 
   * @param property the property type
   * @return list of the vocabulary terms of the given property
   */
  public static List<String> listVocabularyTermsForProperty(PropertyType property) {
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
  public static String getCVLabelForProperty(PropertyType propertyType, String propertyValue) {
    ControlledVocabularyPropertyType controlled_vocab =
        (ControlledVocabularyPropertyType) propertyType;

    for (VocabularyTerm term : controlled_vocab.getTerms()) {
      if (term.getCode().equals(propertyValue)) {
        return term.getLabel();
      }
    }
    throw new IllegalArgumentException();
  }
  
  /**
   * Function to retrieve all properties which have been assigned to a specific entity type
   * 
   * @param entity_type entitiy type
   * @return list of properties which are assigned to the entity type
   */
  public static List<PropertyType> listPropertiesForType(EntityType entity_type) {
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
   * Function to transform openBIS entity type to human readable text. Performs String replacement
   * and does not query openBIS!
   * 
   * @param entityCode the entity code as string
   * @return entity code as string in human readable text
   */
  public static String openBIScodeToString(String entityCode) {
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
